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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.oculusinfo.ml.utils.StringTools;

import influent.idl.FL_Cluster;
import influent.idl.FL_Entity;
import influent.idlhelper.PropertyHelper;
import influent.server.clustering.utils.EntityClusterFactory;
import influent.server.clustering.utils.PropertyManager;

public class LabelEntityClusterer extends BaseEntityClusterer {
	private static Set<String> stopwords;
	
	private PropertyManager pMgr;
	private String clusterType;
	
	private Set<String> getStopWords() {
		Set<String> stopwords = new HashSet<String>();
		
		try {
			// retrieve the stopwords property
			String stopwordsProp = pMgr.getProperty(PropertyManager.STOP_WORDS);
			
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
			pMgr = (PropertyManager)args[3];
			stopwords = getStopWords();
		 }
		 catch (Exception e) {
			 throw new IllegalArgumentException("Invalid initialization parameters.", e);
		 }
	}
	
	private Map<String, FL_Cluster> retrieveClusterBuckets(Collection<FL_Cluster> clusters, ClusterContext context) {
		Map<String, FL_Cluster> buckets = new HashMap<String, FL_Cluster>();
		
		for (FL_Cluster cluster : clusters) {
			String value = "Unknown";
			
			FL_Entity firstChild = this.getFirstChildEntity(cluster, context);
			PropertyHelper prop = getFirstProperty(firstChild, clusterField);
			if (prop != null) {
				try {
					value = ((String)prop.getValue()).trim();
					
					if (clusterType.equalsIgnoreCase("alpha")) {
						value = value.substring(0, 1).toLowerCase();  // grab the first character - we are bucketing by alpha
					}
					else {
						// first strip out stop words from label
						if (Boolean.parseBoolean(pMgr.getProperty(PropertyManager.ENABLE_STOPWORDS, "false"))) {
							List<String> tokens = tokenizeString(value);
							StringBuilder str = new StringBuilder();
							for (String token : tokens) {
								str.append(token);
							}
							value = str.toString();
						}
						// compute a hash fingerprint for string to use for label clustering
						value = StringTools.fingerPrint(value);
					} 
				}
				catch (Exception e) {
					log.error("Label field is not a string! Ignoring.");
				}
				
				buckets.put(value, cluster);
			}
		}
		return buckets;
	}
	
	private Map<String, List<FL_Entity>> bucketByLabel(Collection<FL_Entity> entities) {
		Map<String, List<FL_Entity>> buckets = new HashMap<String, List<FL_Entity>>();
		
		for (FL_Entity entity : entities) {
			String value = "UNKNOWN";
			PropertyHelper prop = getFirstProperty(entity, clusterField);
			if (prop != null && prop.getValue() != null && !((String)prop.getValue()).isEmpty()) {
				try {
					value = (String)prop.getValue();
					
					if (clusterType.equalsIgnoreCase("alpha")) {
						value = value.substring(0, 1).toLowerCase();  // grab the first character - we are bucketing by alpha
					}
					else {
						// first strip out stop words from label
						if (Boolean.parseBoolean(pMgr.getProperty(PropertyManager.ENABLE_STOPWORDS, "false"))) {
							List<String> tokens = tokenizeString(value);
							StringBuilder str = new StringBuilder();
							for (String token : tokens) {
								str.append(token);
							}
							value = str.toString();
						}
					
						// compute a hash fingerprint for string to use for label clustering
						value = StringTools.fingerPrint(value);
					}
				}
				catch (Exception e) {
					log.error("Label field is not a string! Ignoring.");
				}
			}
			if (!buckets.containsKey(value)) {
				buckets.put(value, new LinkedList<FL_Entity>());
			}
			buckets.get(value).add(entity);
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
		Map<String, FL_Cluster> prevBuckets = retrieveClusterBuckets(clusters, context);
		
		Map<String, List<FL_Entity>> buckets = bucketByLabel(entities);
		
		// merge the buckets with the previous Buckets
		Map<String, FL_Cluster> modifiedClusters = mergeBuckets(buckets, prevBuckets);
				
		ClusterContext result = new ClusterContext();
		result.roots.putAll(modifiedClusters);
		result.clusters.putAll(modifiedClusters);
		return result;
	}

}
