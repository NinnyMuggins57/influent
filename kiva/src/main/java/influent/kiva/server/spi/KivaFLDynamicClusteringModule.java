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
package influent.kiva.server.spi;

import influent.idl.FL_Clustering;
import influent.idl.FL_ClusteringDataAccess;
import influent.idl.FL_DataAccess;
import influent.idl.FL_Geocoding;
import influent.server.clustering.EntityClusterer;
import influent.server.clustering.GeneralEntityClusterer;
import influent.server.clustering.utils.ClusterContextCache;
import influent.server.clustering.utils.EntityClusterFactory;
import influent.server.clustering.utils.PropertyManager;
import influent.server.dataaccess.DataNamespaceHandler;
import influent.server.dataaccess.DynamicClustering;
import influent.server.utilities.IdGenerator;
import influent.server.utilities.SQLConnectionPool;
import influent.server.utilities.TypedId;

import java.io.InputStream;
import java.util.UUID;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;


/**
 *
 */
public class KivaFLDynamicClusteringModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(EntityClusterer.class).to(GeneralEntityClusterer.class);
		bind(FL_Clustering.class).to(DynamicClustering.class);
		bind(FL_ClusteringDataAccess.class).to(DynamicClustering.class);
	}
	
	@Provides @Singleton
	public PropertyManager clusterPropertiesManager() {
		try {
			InputStream configStream = KivaFLDynamicClusteringModule.class.getResourceAsStream("/clusterer.properties");
			return new PropertyManager(configStream);
		} catch (Exception e) {
			addError("Failed to load Clustering Properties", e);
			return null;
		}
	}
	
	@Provides @Singleton
	public IdGenerator clusterIdGenerator() {
		return new IdGenerator() {
			@Override
			public String nextId() {
				return TypedId.fromNativeId(TypedId.CLUSTER, UUID.randomUUID().toString()).getTypedId();
			}
		};
	}
	
	@Provides @Singleton
	public EntityClusterFactory clusterFactory(
			IdGenerator idGen,
			PropertyManager pMgr,
			FL_Geocoding geocoding
	) {
		try {		
			return new EntityClusterFactory(idGen, geocoding, pMgr);
		} catch (Exception e) {
			addError("Failed to load Clustering Properties", e);
			return null;
		}
	}
	
	/*
	 * Provide the clustering service
	 */
	@Provides @Singleton
	public DynamicClustering clustering (
			FL_DataAccess dataAccess,
			SQLConnectionPool connectionPool,
			DataNamespaceHandler namespaceHandler,
			FL_Geocoding geocoding,
			EntityClusterer clusterer,
			EntityClusterFactory clusterFactory,
			PropertyManager pMgr,
			ClusterContextCache cache
	) {

		try {
			return new DynamicClustering(
				connectionPool, 
				namespaceHandler,
				dataAccess, 
				geocoding, 
				clusterer, 
				clusterFactory,
				pMgr,
				cache
			);
		} catch (Exception e) {
			addError("Failed to load Clustering", e);
			return null;
		}
	}
	
}
