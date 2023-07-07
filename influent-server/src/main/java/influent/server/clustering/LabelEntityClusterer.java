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

import influent.idl.FL_Cluster;
import influent.idl.FL_Entity;
import influent.idlhelper.ClusterHelper;
import influent.idlhelper.PropertyHelper;
import influent.server.clustering.utils.EntityClusterFactory;
import influent.server.clustering.utils.ClustererProperties;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oculus.aperture.spi.common.Properties;

import com.oculusinfo.ml.DataSet;
import com.oculusinfo.ml.Instance;
import com.oculusinfo.ml.feature.string.StringFeature;
import com.oculusinfo.ml.feature.string.centroid.StringMedianCentroid;
import com.oculusinfo.ml.feature.string.distance.EditDistance;
import com.oculusinfo.ml.unsupervised.cluster.BaseClusterer;
import com.oculusinfo.ml.unsupervised.cluster.Cluster;
import com.oculusinfo.ml.unsupervised.cluster.ClusterResult;
import com.oculusinfo.ml.unsupervised.cluster.dpmeans.DPMeans;
import com.oculusinfo.ml.utils.StringTools;

public class LabelEntityClusterer extends BaseEntityClusterer {
	private static Set<String> stopwords;
	private Properties pMgr;
	private String clusterType;
	
	private Set<String> getStopWords() {
		Set<String> stopwords = new HashSet<String>();
		
		try {
			// retrieve the stopwords property
			String stopwordsProp = pMgr.getString(ClustererProperties.STOP_WORDS, "");
			
			for (String word : stopwordsProp.split(",")) {
				stopwords.add(word.toLowerCase());
			}
			
		} catch (Exception e) {
			log.error("Failed to load stop words from clusterer.properties.", e);
		}
		
		return stopwords;
	}
	
	protected List<String> tokenizeString(String label) {
		List<String> candidates = new LinkedList<String>();

		String cleaned = label; //label.replaceAll("[.,!@#$%^&*()-_=+}{;:'\"?/<>\\[\\]\\\\]", " ");

		Pattern regex = Pattern.compile("\\p{L}[\\p{L}\\p{P}]+\\p{L}");
		
		//String[] tokens = regex.split(label); //label.split(" ");
		Matcher lexer = regex.matcher(cleaned);
		
		while (lexer.find()) {
			String token = lexer.group().toLowerCase();
			if (stopwords.contains(token) == false) candidates.add(token);
		}

	    return candidates;
	}

	@Override
	public void init(Object[] args) {
		try {
			clusterFactory = (EntityClusterFactory)args[0];
			clusterField = (String)args[1];
			clusterType = (String)args[2];
			pMgr = (Properties)args[3];
			stopwords = getStopWords();
		 }
		 catch (Exception e) {
			 throw new IllegalArgumentException("Invalid initialization parameters.", e);
		 }
	}
	
	private String cleanString(String label) {
		String cleanStr = label.trim();
		
		// first strip out stop words from label
		if (pMgr.getBoolean(ClustererProperties.ENABLE_STOPWORDS, false)) {
			List<String> tokens = tokenizeString(cleanStr);
			StringBuilder str = new StringBuilder();
			for (String token : tokens) {
				str.append(token);
			}
			cleanStr = str.toString();
		}
		return cleanStr;
	}
	
	private String toLabelKey(String key) {
		if (clusterType.equalsIgnoreCase("alpha")) {
			key = key.isEmpty() ? "_" : key.substring(0, 1).toLowerCase();  // grab the first character - we are bucketing by alpha
		}
		else {  // use fingerprint
			// first clean the string
			key = cleanString(key);
			// compute a hash fingerprint for string to use for label clustering
			key = StringTools.fingerPrint(key);
		} 
		return key;
	}
	
	@Override
	protected String getBucketKey(FL_Cluster cluster, ClusterContext context) {
		return toLabelKey( super.getBucketKey(cluster, context) );
	}

	@Override
	protected String getBucketKey(FL_Entity entity) {
		return toLabelKey( super.getBucketKey(entity) );
	}
	
	private String getStringValue(PropertyHelper property) {
		String val = "Unknown";  // default if no valid property value found
		
		// try to read the double value of the entity
		if (property != null && property.getRange() != null) {
			Object v = property.getValue();
			if (v instanceof String) {
				val = (String)v;
			}
		}
		return val;
	}
	
	private Instance createInstance(String id, String label) {
		Instance inst = new Instance(id);
				
		StringFeature feature = new StringFeature("label");
		feature.setValue( cleanString(label) );
		inst.addFeature( feature );
		
		return inst;
	}
	
	private DataSet createDataSet(Collection<FL_Entity> entities, Collection<FL_Cluster> immutableClusters, ClusterContext context) {
		DataSet ds = new DataSet();
		
		for (FL_Entity entity : entities) {			
			PropertyHelper prop = getFirstProperty(entity, clusterField);
			if (prop != null) {
				String val = getStringValue(prop);
				Instance inst = createInstance(entity.getUid(), val);
				ds.add(inst);
			}
			else {
				// No valid clusterField property found default to "Unknown"
				Instance inst = createInstance(entity.getUid(), "Unknown");
				ds.add(inst);
			}
		}
		for (FL_Cluster immutableCluster : immutableClusters) {
			PropertyHelper prop = getFirstProperty(immutableCluster, clusterField);
			if (prop == null) {
				FL_Entity firstChild = getFirstChildEntity(immutableCluster, context);
				if (firstChild != null) {
					prop = getFirstProperty(firstChild, clusterField);
				}
			}
			if (prop != null) {
				String val = getStringValue(prop);
				Instance inst = createInstance(immutableCluster.getUid(), val);
				ds.add(inst);
			}
			else {
				// No valid clusterField property found default to "Unknown"
				Instance inst = createInstance(immutableCluster.getUid(), "Unknown");
				ds.add(inst);
			}
		}
		return ds;
	}
	
	private BaseClusterer createEditDistanceClusterer() {
		DPMeans clusterer = new DPMeans(3, true);
		clusterer.setThreshold(0.6);
		clusterer.registerFeatureType("label", StringMedianCentroid.class, new EditDistance(1.0));
		return clusterer;
	}
	
	private ClusterContext clusterByEditDistance(Collection<FL_Entity> entities,
												 Collection<FL_Cluster> immutableClusters,
												 Collection<FL_Cluster> clusters, 
												 ClusterContext context) {
		
		Map<String, FL_Entity> entityIndex = createEntityIndex(entities);
		Map<String, FL_Cluster> immutableClusterIndex = createClusterIndex(immutableClusters);
		Map<String, FL_Cluster> clusterIndex = createClusterIndex(clusters);
		
		DataSet ds = createDataSet(entities, immutableClusters, context);
		
		BaseClusterer clusterer = createEditDistanceClusterer();
		
		List<Cluster> existingClusters = new LinkedList<Cluster>();
		
		for (FL_Cluster cluster : clusters) {			
			String val = "Unknown";
			PropertyHelper prop = getFirstProperty(cluster, toClusterPropertyName(clusterField));
			if (prop != null) {
				val = getStringValue(prop);
			}
			Cluster c = clusterer.createCluster();
			c.setId(cluster.getUid());
			StringFeature feature = new StringFeature("label");
			feature.setValue(val);
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
				StringFeature feature = (StringFeature)c.getFeature("label");
				String value = feature.getValue();
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
				StringFeature feature = (StringFeature)c.getFeature("label");
				String value = feature.getValue();
				addClusterProperty(cluster, clusterField, value);
			}
			modifiedClusters.put(cluster.getUid(), cluster);
		}
		
		ClusterContext result = new ClusterContext();
		result.roots.putAll(modifiedClusters);
		result.clusters.putAll(modifiedClusters);
	
		return result;
	}

	@Override
	public ClusterContext clusterEntities(Collection<FL_Entity> entities,
										  Collection<FL_Cluster> immutableClusters,
										  Collection<FL_Cluster> clusters, 
										  ClusterContext context) {
		
		if (clusterType.equalsIgnoreCase("edit")) {
			return clusterByEditDistance(entities, immutableClusters, clusters, context);
		}
		
		return super.clusterEntities(entities, immutableClusters, clusters, context);
	}
}
