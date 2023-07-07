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
import com.oculusinfo.ml.unsupervised.cluster.kmeans.KMeans;

import influent.idl.FL_Cluster;
import influent.idl.FL_Entity;
import influent.idlhelper.PropertyHelper;
import influent.server.clustering.utils.EntityClusterFactory;

public class NumericEntityClusterer extends BaseEntityClusterer {

	private int K = 5;
	
	@Override
	public void init(Object[] args) {
		try {
			clusterFactory = (EntityClusterFactory)args[0];
			clusterField = (String)args[1];
			K = (Integer)args[2];
		 }
		 catch (Exception e) {
			 throw new IllegalArgumentException("Invalid initialization parameters.", e);
		 }
	}
	
	private DataSet createDataSet(Collection<FL_Entity> entities) {
		DataSet ds = new DataSet();
		
		for (FL_Entity entity : entities) {			
			PropertyHelper prop = getFirstProperty(entity, clusterField);
			if (prop != null) {
				Double val = null;
				Object v = prop.getValue();
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
				
				if (val != null) {
					Instance inst = new Instance(entity.getUid());
					NumericVectorFeature feature = new NumericVectorFeature("num");
					feature.setValue(new double[]{val});
					inst.addFeature( feature );
					ds.add(inst);
				}
			}
			else {
				// TODO what to do with entities that can't be clustered?
			}
		}
		return ds;
	}
	
	private BaseClusterer createNumericClusterer() {
		KMeans clusterer = new KMeans(K, 3, true); 
		clusterer.registerFeatureType("num", MeanNumericVectorCentroid.class, new EuclideanDistance(1.0));
		return clusterer;
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
		Map<String, FL_Entity> entityIndex = createEntityIndex(entities);
		Map<String, FL_Cluster> clusterIndex = createClusterIndex(clusters);
		
		DataSet ds = createDataSet(entities);
		
		BaseClusterer clusterer = createNumericClusterer();
		
		List<Cluster> existingClusters = new LinkedList<Cluster>();
		
		for (FL_Cluster cluster : clusters) {
			List<FL_Entity> children = this.getChildEntities(cluster, context);
			
			int count = 0;
			float mean = 0;
			
			for (FL_Entity child : children) {
				PropertyHelper prop = getFirstProperty(child, clusterField);
		
				if (prop != null) {
					Object rawVal = prop.getValue();
					
					if (rawVal == null) continue;
					
					if (rawVal instanceof Float) {
						mean += (Float) rawVal;
						count++;
					}
					else if (rawVal instanceof Integer) {
						mean += (Integer) rawVal;
						count++;
					}
					else if (rawVal instanceof Long) {
						mean += (Long) rawVal;
						count++;
					}
				}
			}
			if (count > 0) mean /= count;
			Cluster c = clusterer.createCluster();
			c.setId(cluster.getUid());
			NumericVectorFeature feature = new NumericVectorFeature("num");
			feature.setValue(new double[]{mean});
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
		
		ClusterContext result = new ClusterContext();
		result.roots.putAll(modifiedClusters);
		result.clusters.putAll(modifiedClusters);
		
		return result;
	}
}
