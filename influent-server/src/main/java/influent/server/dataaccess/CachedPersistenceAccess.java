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

 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.

 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package influent.server.dataaccess;

import influent.idl.FL_Persistence;
import influent.idl.FL_PersistenceState;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

import org.apache.avro.AvroRemoteException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class CachedPersistenceAccess implements FL_Persistence {

	private static Logger s_logger = LoggerFactory.getLogger(CachedPersistenceAccess.class);
	
	private final Ehcache persistenceCache;
	private final Ehcache clusteringCache;
	
	
	public CachedPersistenceAccess(
		String ehCacheConfig,
		String persistenceCacheName,
		String dynamicClusteringCacheName
	) {
		CacheManager cacheManager = (ehCacheConfig != null) ? CacheManager.create(ehCacheConfig) : null;
		if (cacheManager == null) {
			s_logger.warn("ehcache property not set, persistence data won't be cached");
		}
		
		this.persistenceCache = cacheManager.getEhcache(persistenceCacheName);
		this.clusteringCache = cacheManager.getEhcache(dynamicClusteringCacheName);
	}
	
	
	
	
	@Override
	public FL_PersistenceState persistData(String sessionId, String data) {
		
		if (persistenceCache == null) {
			return FL_PersistenceState.NONE;
		}
		
		FL_PersistenceState state = (!persistenceCache.isKeyInCache(sessionId)) ? FL_PersistenceState.NEW : FL_PersistenceState.MODIFIED;
		
		Element element = new Element(sessionId, data);
		persistenceCache.put(element);
		
		return state;
	}
	
	
	
	
	@Override
	public String getData(String sessionId) throws AvroRemoteException {
		
		String data = null;
		
		if (persistenceCache == null) {
			return data;
		}
		
		Element element = persistenceCache.get(sessionId);
		if (element != null) {
			data = element.getObjectValue().toString();
			Collection<String> contextIds = getContextIdsFromData(data);
			
			if (contextIds.size() > 3) {
			
				Map<Object, Element> contexts = clusteringCache.getAll(contextIds);
				
				boolean containsContext = false;
				for (Element e : contexts.values()) {
					if (e != null) {
						containsContext = true;
						break;
					}
				}
				
				if (!containsContext) {
					data = null;
				}
			}
		}
		
		return data;
	}




	private Collection<String> getContextIdsFromData(String data) {
		
		Set<String> ids = new HashSet<String>();
		
		String regex = "column_[^\"]*";
		Matcher m = Pattern.compile(regex).matcher(data);
		while (m.find()) {
			ids.add(m.group());
		}
		
		return ids;
	}
}
