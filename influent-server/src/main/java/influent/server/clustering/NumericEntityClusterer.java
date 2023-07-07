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

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.oculusinfo.ml.DataSet;
import com.oculusinfo.ml.Instance;
import com.oculusinfo.ml.feature.numeric.NumericVectorFeature;
import com.oculusinfo.ml.feature.numeric.centroid.MeanNumericVectorCentroid;
import com.oculusinfo.ml.feature.numeric.distance.EuclideanDistance;
import com.oculusinfo.ml.unsupervised.cluster.BaseClusterer;
import com.oculusinfo.ml.unsupervised.cluster.Cluster;
import com.oculusinfo.ml.unsupervised.cluster.ClusterResult;
import com.oculusinfo.ml.unsupervised.cluster.dpmeans.DPMeans;
import influent.idl.FL_Cluster;
import influent.idl.FL_Entity;
import influent.idlhelper.ClusterHelper;
import influent.idlhelper.PropertyHelper;
import influent.server.clustering.utils.EntityClusterFactory;

public class NumericEntityClusterer extends BaseEntityClusterer {

	private double K = 100.0;
	
	@Override
	public void init(Object[] args) {
		try {
			clusterFactory = (EntityClusterFactory)args[0];
			clusterField = (String)args[1];
			K = (Double)args[2];
		 }
		 catch (Exception e) {
			 throw new IllegalArgumentException("Invalid initialization parameters.", e);
		 }
	}
	
	private Instance createInstance(String id, Double value) {
		Instance inst = new Instance(id);
		
		NumericVectorFeature feature = new NumericVectorFeature("num");
		feature.setValue(new double[]{value});
		inst.addFeature( feature );
		
		return inst;
	}
	
	private Double getDoubleValue(PropertyHelper property) {
		Double val = null;
		
		// try to read the double value of the entity
		if (property != null && property.getRange() != null) {
			Object v = property.getValue();
			if (v instanceof Double) {
				val = (Double)v;
			}
			else if (v instanceof Float) {
				val = (double)(Float)v;
			}
			else if (v instanceof Long) {
				val = (double)(Long)v;
			}
			else if (v instanceof Integer) {
				val = (double)(Integer)v;
			}
		}
		return val;
	}
	
	private DataSet createDataSet(Collection<FL_Entity> entities, Collection<FL_Cluster> immutableClusters, ClusterContext context) {
		DataSet ds = new DataSet();
		
		for (FL_Entity entity : entities) {			
			PropertyHelper prop = getFirstProperty(entity, clusterField);
			if (prop != null) {
				Double val = getDoubleValue(prop);
				
				if (val != null) {
					Instance inst = createInstance(entity.getUid(), val);
					ds.add(inst);
				}
			}
			else {
				// No valid clusterField property found default to -1
				Instance inst = createInstance(entity.getUid(), -1.0);
				ds.add(inst);
			}
		}
		for (FL_Cluster immutableCluster : immutableClusters) {
			PropertyHelper prop = getFirstProperty(immutableCluster, toClusterPropertyName(clusterField));
			if (prop == null) {
				FL_Entity firstChild = getFirstChildEntity(immutableCluster, context);
				if (firstChild != null) {
					prop = getFirstProperty(firstChild, clusterField);
				}
			}
			if (prop != null) {
				Double val = getDoubleValue(prop);
				
				if (val != null) {
					Instance inst = createInstance(immutableCluster.getUid(), val);
					ds.add(inst);
				}
			}
			else {
				// No valid clusterField property found default to -1
				Instance inst = createInstance(immutableCluster.getUid(), -1.0);
				ds.add(inst);
			}
		}
		return ds;
	}
	
	private BaseClusterer createNumericClusterer() {
		DPMeans clusterer = new DPMeans(3, true);
		clusterer.setThreshold(K);
//		KMeans clusterer = new KMeans(K, 3, true); 
		clusterer.registerFeatureType("num", MeanNumericVectorCentroid.class, new EuclideanDistance(1.0));
		return clusterer;
	}
	
	@Override
	public ClusterContext clusterEntities(Collection<FL_Entity> entities, 
										  Collection<FL_Cluster> immutableClusters, 
										  Collection<FL_Cluster> clusters, 
										  ClusterContext context) {
		
		Map<String, FL_Entity> entityIndex = createEntityIndex(entities);
		Map<String, FL_Cluster> immutableClusterIndex = createClusterIndex(immutableClusters);
		Map<String, FL_Cluster> clusterIndex = createClusterIndex(clusters);
		
		DataSet ds = createDataSet(entities, immutableClusters, context);
		
		BaseClusterer clusterer = createNumericClusterer();
		
		List<Cluster> existingClusters = new LinkedList<Cluster>();
		
		for (FL_Cluster cluster : clusters) {			
			double val = 0;
			PropertyHelper prop = getFirstProperty(cluster, toClusterPropertyName(clusterField));
			if (prop != null) {
				val = getDoubleValue(prop);
			}
			Cluster c = clusterer.createCluster();
			c.setId(cluster.getUid());
			NumericVectorFeature feature = new NumericVectorFeature("num");
			feature.setValue(new double[]{val});
			c.addFeature(feature);
			existingClusters.add(c);
		}
		
		ClusterResult rs = null; 
		if (existingClusters.isEmpty()) {
			rs = clusterer.doCluster(ds);
		}
		else {
			rs = clusterer.doIncrementalCluster(ds, existingClusters);
		}
		// clean up
		clusterer.terminate();
		
		Map<String, FL_Cluster> modifiedClusters = new HashMap<String, FL_Cluster>();
		
		for (Cluster c : rs) {
			List<FL_Cluster> subClusters = new LinkedList<FL_Cluster>();
			List<FL_Entity> members = new LinkedList<FL_Entity>();
			
			for (Instance inst : c.getMembers()) {
				String id = inst.getId();
				if ( entityIndex.containsKey(id) ) {
					members.add( entityIndex.get(id) );
				}
				else if ( immutableClusterIndex.containsKey(id) ){
					subClusters.add( immutableClusterIndex.get(id) );
				}
			}
			
			FL_Cluster cluster = clusterIndex.get(c.getId());
			if (cluster == null) {
				cluster = clusterFactory.toCluster(members, subClusters);
				// cache the cluster property
				NumericVectorFeature feature = (NumericVectorFeature)c.getFeature("num");
				double value = feature.getValue()[0];
				addClusterProperty(cluster, clusterField, value);
			}
			else {
				ClusterHelper.addMembers(cluster, members);
				EntityClusterFactory.setEntityCluster(members, cluster);
				
				for (FL_Cluster subCluster : subClusters) {
					ClusterHelper.addSubCluster(cluster, subCluster);
					subCluster.setParent(cluster.getUid());
				}	
				
				// cache the cluster property
				NumericVectorFeature feature = (NumericVectorFeature)c.getFeature("num");
				double value = feature.getValue()[0];
				addClusterProperty(cluster, clusterField, value);
			}
			modifiedClusters.put(cluster.getUid(), cluster);
		}
		
		ClusterContext result = new ClusterContext();
		result.roots.putAll(modifiedClusters);
		result.clusters.putAll(modifiedClusters);
		
		return result;
	}
}
