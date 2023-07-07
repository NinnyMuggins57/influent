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
package influent.server.rest;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Statistics;
import oculus.aperture.common.rest.ApertureServerResource;

import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.data.CacheDirective;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class CacheStatsResource extends ApertureServerResource{	
	
	private static CacheManager cacheManager;
	
	@Inject
	public CacheStatsResource(
		@Named("influent.midtier.ehcache.config") String ehCacheConfig
	) {
		cacheManager = CacheManager.create(ehCacheConfig);
	}
	
	
	@Get
	public StringRepresentation getCacheStats() throws ResourceException {
		try {
				
			JSONObject result = new JSONObject();
			
			for (String cacheName : cacheManager.getCacheNames()) {
				Ehcache cache = cacheManager.getEhcache(cacheName);
				long inMemorySize = cache.calculateInMemorySize();
				long onDiskSize = cache.calculateOnDiskSize();
				Statistics stats = cache.getStatistics();
				
				Map<String, Object> cacheStats = new HashMap<String, Object>();
				cacheStats.put("inMemorySize", inMemorySize);
				cacheStats.put("onDiskSize", onDiskSize);
				cacheStats.put("inMemoryObjectCount", stats.getMemoryStoreObjectCount());
				cacheStats.put("onDiskObjectCount", stats.getDiskStoreObjectCount());
				cacheStats.put("cacheHits", stats.getCacheHits());
				cacheStats.put("cacheMisses", stats.getCacheMisses());
				cacheStats.put("inMemoryHits", stats.getInMemoryHits());
				cacheStats.put("inMemoryMissses", stats.getInMemoryMisses());
				cacheStats.put("onDiskHits", stats.getOnDiskHits());
				cacheStats.put("onDiskMissses", stats.getOnDiskMisses());
				
				result.put(cacheName, cacheStats);
			}
			
			getResponse().setCacheDirectives(
				Collections.singletonList(
					CacheDirective.noCache()
				)
			);

			return new StringRepresentation(result.toString(), MediaType.APPLICATION_JSON);
			
		} catch (JSONException e) {
			throw new ResourceException(
				Status.CLIENT_ERROR_BAD_REQUEST,
				"Unable to create JSON object from caches statistics data",
				e
			);
		} 
	}	
}
