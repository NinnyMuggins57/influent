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
package influent.server.clustering;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.avro.AvroRemoteException;

import com.oculusinfo.ml.DataSet;
import com.oculusinfo.ml.Instance;
import com.oculusinfo.ml.feature.spatial.GeoSpatialFeature;
import com.oculusinfo.ml.feature.spatial.centroid.GeoSpatialCentroid;
import com.oculusinfo.ml.feature.spatial.distance.HaversineDistance;
import com.oculusinfo.ml.unsupervised.cluster.BaseClusterer;
import com.oculusinfo.ml.unsupervised.cluster.Cluster;
import com.oculusinfo.ml.unsupervised.cluster.ClusterResult;
import com.oculusinfo.ml.unsupervised.cluster.dpmeans.DPMeans;

import influent.idl.FL_Cluster;
import influent.idl.FL_ContinentCode;
import influent.idl.FL_Country;
import influent.idl.FL_Entity;
import influent.idl.FL_Frequency;
import influent.idl.FL_GeoData;
import influent.idl.FL_Geocoding;
import influent.idl.FL_PropertyType;
import influent.idlhelper.PropertyHelper;
import influent.server.clustering.utils.EntityClusterFactory;

public class GeoEntityClusterer extends BaseEntityClusterer {
	private FL_Geocoding geoCoder;
	private GEO_LEVEL level;
	
	public enum GEO_LEVEL {
		Continent,
		Region,
		Country,
		LatLon
	}
	
	@Override
	public void init(Object[] args) {
		try {
			clusterFactory = (EntityClusterFactory)args[0];
			clusterField = (String)args[1];
			geoCoder = (FL_Geocoding)args[2];
			level = (GEO_LEVEL)args[3];
		 }
		 catch (Exception e) {
			 throw new IllegalArgumentException("Invalid initialization parameters.", e);
		 }
	}
	
	private Map<String, FL_Country> createGeoCache(Collection<FL_Entity> entities) {
		Map<String, FL_Country> geoCache = new HashMap<String, FL_Country>();
		
		// create geo Cache
		for (FL_Entity entity : entities) {
			PropertyHelper prop = getFirstProperty(entity, clusterField);
			if (prop != null) {
				try {
					FL_GeoData geo = (FL_GeoData)prop.getValue();
					List<FL_Country> countries = geoCoder.getCountries(Collections.singletonList(geo));
					if (!countries.isEmpty()) {
						FL_Country country = countries.get(0);
						geoCache.put(entity.getUid(), country);
					}
				}
				catch (Exception e) {
					log.error("Geo field is not an FL_GeoData object! Ignoring.");
				}
			}
		}
		return geoCache;
	}
	
	private DataSet createDataSet(Collection<FL_Entity> entities, Map<String, FL_Country> geoCache) {
		DataSet ds = new DataSet();
		
		for (FL_Entity entity : entities) {			
			FL_Country country = geoCache.get(entity.getUid());
			
			Double lat = 0.0, lon = 0.0;
			
			if (country != null) {
				FL_GeoData geo = country.getCountry();
				
				if (geo.getLat() != null) {
					lat = geo.getLat();
				}
				if (geo.getLon() != null) {
					lon = geo.getLon();
				}
			}
			
			Instance inst = new Instance(entity.getUid());
			GeoSpatialFeature feature = new GeoSpatialFeature("geo");
			feature.setValue(lat, lon);
			inst.addFeature( feature );
			ds.add(inst);
		}
		return ds;
	}
	
	private BaseClusterer createGeoClusterer() {
		DPMeans clusterer = new DPMeans(3, true);
		clusterer.setThreshold(0.2);
		clusterer.registerFeatureType("geo", GeoSpatialCentroid.class, new HaversineDistance(1.0));
		return clusterer;
	}
	
	@SuppressWarnings("unchecked")
	private Map<String, FL_Cluster> clusterByLatLon(Collection<FL_Entity> entities, Collection<FL_Cluster> clusters, Map<String, FL_Country> geoCache) {
		// Haversine clustering
		Map<String, FL_Entity> entityIndex = createEntityIndex(entities);
		Map<String, FL_Cluster> clusterIndex = createClusterIndex(clusters);
					
		DataSet ds = createDataSet(entities, geoCache);
					
		BaseClusterer clusterer = createGeoClusterer();
		
		List<Cluster> existingClusters = new LinkedList<Cluster>();
	
		for (FL_Cluster cluster : clusters) {
			PropertyHelper prop = getFirstProperty(cluster, FL_PropertyType.GEO.name());
			if (prop != null) {
				List<FL_Frequency> dist = (List<FL_Frequency>)prop.getValue();
				
				double lat = 0, lon = 0;
				for (FL_Frequency freq : dist) {
					FL_GeoData geo = (FL_GeoData)freq.getRange();
					lat += geo.getLat();
					lon += geo.getLon();
				}
				lat /= dist.size();
				lon /= dist.size();
				
				Cluster c = clusterer.createCluster();
				c.setId(cluster.getUid());
				GeoSpatialFeature feature = new GeoSpatialFeature("geo");
				feature.setValue(lat, lon);
				c.addFeature(feature);
				existingClusters.add(c);
			}
		}
		
		ClusterResult rs = clusterer.doIncrementalCluster(ds, existingClusters);
		
		Map<String, FL_Cluster> modifiedClusters = new HashMap<String, FL_Cluster>();
		
		for (Cluster c : rs) {
			List<FL_Entity> members = new LinkedList<FL_Entity>();
			
			for (Instance inst : c.getMembers()) {
				members.add( entityIndex.get(inst.getId()) );
			}
			FL_Cluster cluster = clusterIndex.get(c.getId());
			if (cluster == null) {
				cluster = this.clusterFactory.toCluster(members, null);
			}
			else {
				for (FL_Entity entity : members) {
					cluster.getMembers().add(entity.getUid());
				}
			}
			modifiedClusters.put(cluster.getUid(), cluster);
		}
		return modifiedClusters;
	}
	
	private Map<String, List<FL_Entity>> bucketByGeo(Collection<FL_Entity> entities, Map<String, FL_Country> geoCache, GEO_LEVEL level) {
		Map<String, List<FL_Entity>> buckets = new HashMap<String, List<FL_Entity>>();
		
		// bucket by geo level
		for (FL_Entity entity : entities) {
			String value = "Unknown";
			FL_Country country = geoCache.get(entity.getUid());
			if (country != null) {
				switch (level) {
				case Continent:
					value = country.getContinent().name();
					break;
				case Region:
					value = country.getRegion();
					break;
				case Country:
					value = country.getCountry().getCc();
					break;
				default:
					break;
				}
			}
			if (!buckets.containsKey(value)) {
				buckets.put(value, new LinkedList<FL_Entity>());
			}
			buckets.get(value).add(entity);
		}
		return buckets;
	}
	
	@SuppressWarnings("unchecked")
	private Map<String, FL_Cluster> retrieveClusterBuckets(Collection<FL_Cluster> clusters) {
		Map<String, FL_Cluster> buckets = new HashMap<String, FL_Cluster>();
		
		for (FL_Cluster cluster : clusters) {
			String value = "Unknown";
			
			PropertyHelper prop = getFirstProperty(cluster, FL_PropertyType.GEO.name());
			if (prop != null) {
				List<FL_Frequency> dist = (List<FL_Frequency>)prop.getValue();
				if (!dist.isEmpty()) {
					try {
						FL_Frequency freq = dist.get(0);
						FL_GeoData geo = (FL_GeoData)freq.getRange();
						List<FL_Country> country = null;
						switch (level) {
						case Continent:
							country = geoCoder.getCountries(Collections.singletonList(geo));
							if (country != null && !country.isEmpty() && country.get(0) != null) {
								FL_ContinentCode continent = country.get(0).getContinent();
								if (continent != null && !continent.name().isEmpty()) {
									value = continent.name();
								}
							}
							break;
						case Region:
							country = geoCoder.getCountries(Collections.singletonList(geo));
							if (country != null && !country.isEmpty() && country.get(0) != null) {
								String region = country.get(0).getRegion();
								if (region != null && !region.isEmpty()) {
									value = country.get(0).getRegion();
								}
							}
							break;
						case Country:
							String cc = geo.getCc();
							if (cc != null && !cc.isEmpty()) {
								value = cc;
							}
							break;
						default:
							break;
						}
					} catch (AvroRemoteException e) {
						log.error("Cluster geo field is not an FL_GeoData object! Ignoring.");
					}
				}
			}
			buckets.put(value, cluster);
		}
		return buckets;
	}
	
	@Override
	public ClusterContext clusterEntities(Collection<FL_Entity> entities) {
		return this.clusterEntities(entities, new ArrayList<FL_Cluster>(0), new ClusterContext());
	}
	
	@Override
	public ClusterContext clusterEntities(Collection<FL_Entity> entities, ClusterContext context) {	
		throw new UnsupportedOperationException();
	}
	
	@Override
	public ClusterContext clusterEntities(Collection<FL_Entity> entities, Collection<FL_Cluster> clusters, ClusterContext context) {		
		Map<String, FL_Cluster> modifiedClusters = null;
		
		Map<String, FL_Country> geoCache = createGeoCache(entities);
		
		if (level == GEO_LEVEL.LatLon) {
			modifiedClusters = clusterByLatLon(entities, clusters, geoCache);
		}
		else {
			Map<String, FL_Cluster> prevBuckets = retrieveClusterBuckets(clusters);
			
			// bucket by geo
			Map<String, List<FL_Entity>> buckets = bucketByGeo(entities, geoCache, level);
			
			// merge the buckets with the previous Buckets
			modifiedClusters = mergeBuckets(buckets, prevBuckets);
		}
		
		ClusterContext result = new ClusterContext();
		result.roots.putAll(modifiedClusters);
		result.clusters.putAll(modifiedClusters);
		
		return result;
	}
}
