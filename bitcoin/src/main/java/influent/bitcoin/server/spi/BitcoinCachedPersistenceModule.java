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
package influent.bitcoin.server.spi;

import influent.idl.FL_Persistence;
import influent.server.dataaccess.CachedPersistenceAccess;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;


/**
 * This class is used by the Bitcoin *server* implementation.
 * It binds the new FL_ implementation for that server.
 *
 */
public class BitcoinCachedPersistenceModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(FL_Persistence.class).to(CachedPersistenceAccess.class);
	}
	
	
	
	
	/*
	 * Provide the cached service
	 */
	@Provides @Singleton
	public CachedPersistenceAccess connectToCachedPersistenceAccess(
		@Named("influent.midtier.ehcache.config") String ehCacheConfig,
		@Named("influent.persistence.cache.name") String persistenceCacheName,
		@Named("influent.dynamic.clustering.cache.name") String clusteringCacheName
	) {
		return new CachedPersistenceAccess(ehCacheConfig, persistenceCacheName, clusteringCacheName);
	}
}
