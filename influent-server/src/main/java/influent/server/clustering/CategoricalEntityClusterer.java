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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import influent.idl.FL_Cluster;
import influent.idl.FL_Entity;
import influent.idlhelper.PropertyHelper;
import influent.server.clustering.utils.EntityClusterFactory;

public class CategoricalEntityClusterer extends BaseEntityClusterer {
	private static Logger log = LoggerFactory.getLogger("influent");
	
	@Override
	public void init(Object[] args) {
		try {
			clusterFactory = (EntityClusterFactory)args[0];
			clusterField = (String)args[1];
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
					value = (String)prop.getValue();
				}
				catch (Exception e) {
					log.error("Categorical field is not a string! Ignoring.");
				}
			}
			buckets.put(value, cluster);
		}
		return buckets;
	}
	
	private Map<String, List<FL_Entity>> bucketByCategory(Collection<FL_Entity> entities) {
		Map<String, List<FL_Entity>> buckets = new HashMap<String, List<FL_Entity>>();
		
		for (FL_Entity entity : entities) {
			String value = "UNKNOWN";
			PropertyHelper prop = getFirstProperty(entity, clusterField);
			if (prop != null) {
				try {
					value = (String)prop.getValue();
				}
				catch (Exception e) {
					log.error("Categorical field is not a string! Ignoring.");
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
		
		Map<String, List<FL_Entity>> buckets = bucketByCategory(entities);
		
		// merge the buckets with the previous Buckets
		Map<String, FL_Cluster> modifiedClusters = mergeBuckets(buckets, prevBuckets);
		
		ClusterContext result = new ClusterContext();
		result.roots.putAll(modifiedClusters);
		result.clusters.putAll(modifiedClusters);
		return result;
	}

}
