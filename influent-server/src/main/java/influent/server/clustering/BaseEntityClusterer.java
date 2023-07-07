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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import influent.idl.FL_Cluster;
import influent.idl.FL_Entity;
import influent.idl.FL_PropertyTag;
import influent.idlhelper.ClusterHelper;
import influent.idlhelper.EntityHelper;
import influent.idlhelper.PropertyHelper;
import influent.server.clustering.utils.EntityClusterFactory;

public abstract class BaseEntityClusterer implements EntityClusterer {
	protected static final Logger log = LoggerFactory.getLogger("influent");
	
	protected String clusterField;
	protected EntityClusterFactory clusterFactory;
	
	protected Map<String, FL_Entity> createEntityIndex(Collection<FL_Entity> entities) {
		Map<String, FL_Entity> index = new HashMap<String, FL_Entity>();
		
		for (FL_Entity entity : entities) {
			index.put(entity.getUid(), entity);
		}
		return index;
	}
	
	protected Map<String, FL_Cluster> createClusterIndex(Collection<FL_Cluster> clusters) {
		Map<String, FL_Cluster> index = new HashMap<String, FL_Cluster>();
		
		for (FL_Cluster cluster : clusters) {
			index.put(cluster.getUid(), cluster);
		}
		return index;
	}
	
	protected PropertyHelper getFirstProperty(FL_Entity entity, String tagOrName) {
		PropertyHelper prop = null;
		FL_PropertyTag tag = null;
		
		try {
			tag = FL_PropertyTag.valueOf(tagOrName);
		}
		catch (Exception e) { }
		
		if (tag != null) {
			prop = EntityHelper.getFirstPropertyByTag(entity, tag);
		}
		if (prop == null) {
			prop = EntityHelper.getFirstProperty(entity, tagOrName);
		}
		return prop;
	}
	
	protected PropertyHelper getFirstProperty(FL_Cluster cluster, String tagOrName) {
		PropertyHelper prop = null;
		FL_PropertyTag tag = FL_PropertyTag.valueOf(tagOrName);
		if (tag != null) {
			prop = ClusterHelper.getFirstPropertyByTag(cluster, tag);
		}
		if (prop == null) {
			prop = ClusterHelper.getFirstProperty(cluster, tagOrName);
		}
		return prop;
	}
	
	protected List<FL_Entity> getChildEntities(FL_Cluster cluster, ClusterContext context) {
		List<FL_Entity> entities = new LinkedList<FL_Entity>();
		
		for (String entityId : cluster.getMembers()) {
			FL_Entity entity = context.entities.get(entityId);
			entities.add(entity);
		}
		
		for (String subClusterId : cluster.getSubclusters()) {
			FL_Cluster subCluster = context.clusters.get(subClusterId);
			entities.addAll( getChildEntities(subCluster, context) );
		}
		return entities;
	}
	
	protected FL_Entity getFirstChildEntity(FL_Cluster cluster, ClusterContext context) {
		if (!cluster.getMembers().isEmpty()) {
			FL_Entity child = context.entities.get(cluster.getMembers().get(0));
			return child;
		}
		else if (!cluster.getSubclusters().isEmpty()) {
			FL_Cluster child = context.clusters.get(cluster.getSubclusters().get(0));
			return getFirstChildEntity(child, context);
		}
		return null;
	}
	
	protected Map<String, FL_Cluster> mergeBuckets(Map<String, List<FL_Entity>> buckets, Map<String, FL_Cluster> prevBuckets) {
		Map<String, FL_Cluster> modifiedClusters = new HashMap<String, FL_Cluster>();
		
		// merge buckets that have the same bucket id or create a new cluster if no previous bucket exists
		for (String bucketId : buckets.keySet()) {
			if (prevBuckets.containsKey(bucketId)) {
				// merge with the prev bucket
				FL_Cluster cluster = prevBuckets.get(bucketId);
				
				for (FL_Entity entity : buckets.get(bucketId)) {
					cluster.getMembers().add(entity.getUid());
				}
				modifiedClusters.put(cluster.getUid(), cluster);
			}
			else {
				FL_Cluster cluster = clusterFactory.toCluster(buckets.get(bucketId), null);
				modifiedClusters.put(cluster.getUid(), cluster);
			}
		}		
		return modifiedClusters;
	}
}
