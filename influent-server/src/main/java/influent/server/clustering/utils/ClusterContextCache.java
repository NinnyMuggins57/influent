/**
 * Copyright (c) 2013-2014 Oculus Info Inc.
 * http://www.oculusinfo.com/
 *
 * Released under the MIT License.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package influent.server.clustering.utils;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClusterContextCache {
	
	private static final Logger s_logger = LoggerFactory.getLogger(ClusterContextCache.class);

	/**
	 * A set of permits to contexts, which MUST be released after use.
	 * 
	 * @author djonker
	 */
	public static class PermitSet {
		private List<Permit> permits;

		public PermitSet() {
			permits = new LinkedList<ClusterContextCache.Permit>();
		}
		
		/**
		 * Release all permits
		 */
		public void revoke() {
			for (Permit permit : permits) {
				permit.revoke();
			}
			
			permits.clear();
		}
		
		/**
		 * Release the specified permit and remove it from this set.
		 */
		public void revoke(String contextId) {
			Iterator<Permit> i = permits.iterator();

			while (i.hasNext()) {
				Permit permit = i.next();
				
				if (permit.id().equals(contextId)) {
					permit.revoke();
					i.remove();
				}
			}
		}

		// add a new permit
		private Permit add(Permit permit) {
			permits.add(permit);
			
			return permit;
		}
		
	}
	
	/**
	 * Returns a permit to a context, which MUST be released after use.
	 * 
	 * @author djonker
	 */
	private interface Permit {
		/**
		 * context id
		 */
		public String id();
		
		/**
		 * The context which we have a permit to
		 */
		public ColumnContext context();

		/**
		 * Must call release when done with context
		 */
		public void revoke();
	}
	
	
	private class WriteLockedColumn {
		ColumnContext context;
		
		int lockCount = 1;
		
		public WriteLockedColumn(ColumnContext c) {
			context = c;
		}
	}
	
	private final Ehcache cache;

	// context edits in progress.
	private final Map<String, WriteLockedColumn> contextEdits;
	
	
	private static int LOCK_TIMEOUT = /*30*/60*1000;
	
	/**
	 * A permit holds a context lock within the execution of one client thread.
	 * 
	 * @author djonker
	 */
	private class ReadPermit implements Permit {
		final String contextId;
		ColumnContext context;
		boolean locked;
		
		/**
		 * Retrieve the associated context
		 */
		public ColumnContext context() {
			return context;
		}
		
		/**
		 * Release all permits
		 */
		public void revoke() {
			if (locked) {
				locked = false;
				
				cache.releaseReadLockOnKey(contextId);
			}
		}
		
		// construct
		private ReadPermit(String contextId) {
			this.contextId = contextId;
			
			try {
				if (cache.tryReadLockOnKey(contextId, LOCK_TIMEOUT)) {
					locked = true;
					
					// look first for a write copy in progress.
					WriteLockedColumn wlc = contextEdits.get(contextId);
					
					if (wlc != null) {
						context = wlc.context;
						
					} else {
						Element element = cache.get(contextId);
						
						if (element != null) {
							context = (ColumnContext)element.getObjectValue();
						}
					}
					
				} else {
					s_logger.warn("Timed out waiting for context read lock " + contextId);
				}
				
			} catch (InterruptedException e) {
				s_logger.warn("Interrupted waiting for context read lock " + contextId);
			}
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#finalize()
		 */
		@Override
		protected void finalize() throws Throwable {
			
			// can't release from finalizer thread to resolve, so just log the issue.
			if (locked) {
				s_logger.warn("Read permit was not released and has gone out of scope!!! "+ contextId);
			}
			
			super.finalize();
		}

		@Override
		public String id() {
			return contextId;
		}

	}
	
	/**
	 * A permit holds a context lock within the execution of one client thread.
	 * 
	 * @author djonker
	 */
	private class WritePermit implements Permit {
		String contextId;
		ColumnContext context;
		boolean locked;
		
		/**
		 * Retrieve the associated context
		 */
		public ColumnContext context() {
			return context;
		}
		
		/**
		 * Release all permits
		 */
		public void revoke() {
			if (locked) {
				locked = false;

				WriteLockedColumn wlc = contextEdits.get(contextId);

				if (--wlc.lockCount == 0) {
					// update the cache
					try {
						contextEdits.remove(contextId);
						
						cache.put(new Element(contextId, context));
					} finally {
						cache.releaseWriteLockOnKey(contextId);
					}
				}
			}
		}
		
		// construct
		private WritePermit(String contextId) {
			this.contextId = contextId;
			
			try {
				if (!cache.isWriteLockedByCurrentThread(contextId)) {
					if (cache.tryWriteLockOnKey(contextId, LOCK_TIMEOUT)) {
						locked = true;
						
						//System.err.println(contextId);
	
						// take the element for editing out of the cache and temporarily replace with a token.
						Element element = cache.get(contextId);
						
						if (element != null) {
							context = (ColumnContext)element.getObjectValue();
							cache.remove(contextId);
						}
						
						if (context == null) {
							context = new ColumnContext();
						}
	
						// put in local edit map
						contextEdits.put(contextId, new WriteLockedColumn(context));
						
					} else {
						s_logger.warn("Timed out waiting for context write lock " + contextId);
					}
				} else {
					WriteLockedColumn wlc = contextEdits.get(contextId);
					wlc.lockCount++;
					
					context = wlc.context;
					
					locked = true;
				}
				
			} catch (InterruptedException e) {
				s_logger.warn("Interrupted waiting for context write lock " + contextId);
			}
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#finalize()
		 */
		@Override
		protected void finalize() throws Throwable {
			
			// can't release from finalizer thread to resolve, so just log the issue.
			if (locked) {
				s_logger.warn("Write permit was not released and has gone out of scope!!! "+ contextId);
			}
			
			super.finalize();
		}

		@Override
		public String id() {
			return contextId;
		}
	}
	
	/**
	 * (self construct).
	 */
	public ClusterContextCache(
			String ehCacheConfig,
			String cacheName) {
		
		CacheManager cacheManager = (ehCacheConfig != null) ? CacheManager.create(ehCacheConfig) : null;
		if (cacheManager == null) {
			throw new RuntimeException("Failed to initialize ehcache with specified config");
		}
		
		cache = cacheManager.getEhcache(cacheName);
		contextEdits = new ConcurrentHashMap<String, WriteLockedColumn>();
	}

	/**
	 * Fetch a context for ready only access. Permit will be added to permit set. PERMITS MUST BE RELEASED.
	 */
	public ContextRead getReadOnly(String contextId, PermitSet permits) {
		return permits.add(new ReadPermit(contextId)).context();
	}
	
	/**
	 * Fetch a read write context permit. PERMITS MUST BE RELEASED.
	 */
	public ContextReadWrite getReadWrite(String contextId, PermitSet permits) {
		return permits.add(new WritePermit(contextId)).context();
	}

	/**
	 * Remove a context
	 */
	public boolean remove(String contextId) {
		boolean success = false;
		
		try {
			if (cache.tryWriteLockOnKey(contextId, LOCK_TIMEOUT)) {
				try {
					success = cache.remove(contextId);
					
				} finally {
					cache.releaseWriteLockOnKey(contextId);
				}
				
			} else {
				s_logger.warn("Timed out waiting for context write lock to remove. " + contextId);
			}
			
		} catch (InterruptedException e) {
			s_logger.warn("Timed out waiting for context write lock to remove. "+ contextId);
		}
		
		return success;
	}
	
	
}
