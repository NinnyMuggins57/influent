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
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package influent.server.clustering.utils;

import influent.idl.FL_Cluster;
import influent.idl.FL_Entity;
import influent.idlhelper.ClusterHelper;
import influent.idlhelper.EntityHelper;
import influent.server.utilities.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContextCollapser {
	private static final Logger s_logger = LoggerFactory.getLogger(ContextCollapser.class);
	
	// Returns a simplified set of clusters where all the chain-links have been removed (a chain-link being a
	// subcluster that contains only 1 child (1 subcluster or 1 member)
	public static Pair<Collection<FL_Entity>, Collection<FL_Cluster>> collapse(ContextRead context, boolean leaveRoots, Map<String, String> output_redirect) {
				
		Pair<Collection<FL_Entity>, Collection<FL_Cluster>> output = new Pair<Collection<FL_Entity>, Collection<FL_Cluster>>();
		
		Map<String, FL_Cluster> results = copyContext(context);

		Collection<String> entities = new ArrayList<String>();
		
		List<FL_Cluster> clusterList = new ArrayList<FL_Cluster>(results.values());
		
		for (FL_Cluster cluster : clusterList) {		// prune any and all chain-links
			if ( isChain(cluster) ) {
				// singleton cluster with one subcluster
				
				// check to see if this cluster is the root (BUG : roots currently null, use parent instead)
				if (cluster.getParent() == null) { 
					if (!leaveRoots) {
						results.remove(cluster.getUid());
						
						String newRoot = cluster.getSubclusters().get(0);
						setNewRoot(cluster.getUid(),newRoot, results.values());
						updateReferenceMap(output_redirect,cluster.getUid(), cluster.getSubclusters().get(0));
					}			
				} else {
					// remove interior singleton cluster
					results.remove(cluster.getUid());
					
					// point the subcluster to the parent of this trimmed cluster
					FL_Cluster subCluster = results.get(cluster.getSubclusters().get(0));
					subCluster.setParent(cluster.getParent());
					subCluster.setRoot(cluster.getRoot());
					
					// remove the cluster id from the parent subcluster list and add the subcluster id from above
					FL_Cluster parentCluster = results.get(cluster.getParent());
					ClusterHelper.removeSubCluster(parentCluster, cluster);
					ClusterHelper.addSubCluster(parentCluster, subCluster);
				
					updateReferenceMap(output_redirect,cluster.getUid(), cluster.getSubclusters().get(0));
				}
				
			} else if ( isSingleton(cluster) ) {
				// singleton cluster with one entity member
				
				// first case, the cluster is a root cluster, meaning the entity is a 'result' - entity with no containing cluster.
				if (cluster.getParent() == null) {
					if (!leaveRoots) {
						results.remove(cluster.getUid());
						entities.add(cluster.getMembers().get(0));
						updateReferenceMap(output_redirect, cluster.getUid(), cluster.getMembers().get(0));
					}
				} else {
					// otherwise, this is a leaf singleton cluster
					results.remove(cluster.getUid());
					FL_Cluster parentCluster = results.get(cluster.getParent());
					if (parentCluster == null) {
						s_logger.error("Cannot locate parent " + cluster.getParent() + " for cluster " + cluster.getUid());
					} else {
						// remove the cluster id from the parent subcluster list and add the entity id from above
						ClusterHelper.removeSubCluster(parentCluster, cluster);
						ClusterHelper.addMemberById(parentCluster, cluster.getMembers().get(0));
						updateReferenceMap(output_redirect, cluster.getUid(), cluster.getMembers().get(0));
					}
				}
			} else {
				// do nothing, cluster remains in set.
			}
		}
		
		output.first = copyEntities(context.getEntities(entities));
		output.second = results.values();
		
		return output;
	}
	
	private static boolean isChain(FL_Cluster cluster) {
		// singleton cluster with one subcluster
		return cluster.getMembers().size() == 0 && cluster.getSubclusters().size() == 1;
	}
	
	private static boolean isSingleton(FL_Cluster cluster) {
		// singleton cluster with one entity member
		return cluster.getMembers().size() == 1 && cluster.getSubclusters().isEmpty();
	}
	
	private static List<FL_Entity> copyEntities(List<FL_Entity> entities) {
		List<FL_Entity> copies = new ArrayList<FL_Entity>(entities.size());
		
		for (FL_Entity entity: entities) {
			copies.add( EntityHelper.newBuilder(entity).build() ); 
		}
		return copies;
	}
	
	private static Map<String, FL_Cluster> copyContext(ContextRead context) {
		Map<String, FL_Cluster> results = new HashMap<String, FL_Cluster>();
		for (FL_Cluster c : context.getContext().clusters.values()) {
			if (c!= null) {
				// return copies of the cluster to avoid tampering with the internal context!
				FL_Cluster copy = FL_Cluster.newBuilder(c).build();
				// the default avro builders use a immutable list for members and subclusters
				// set mutable version explicitly to ease cluster simplification
				copy.setMembers( new ArrayList<String>(c.getMembers()) );
				copy.setSubclusters( new ArrayList<String>(c.getSubclusters()) );
				results.put(copy.getUid(), copy);
			}
		}
		return results;
	}

	private static void setNewRoot(String oldRoot, String newRoot, Collection<FL_Cluster> clusters) {
		for (FL_Cluster cluster : clusters) {
			// If this cluster is the new root, set root and parent to null
			if (cluster.getUid().equals(newRoot)) {
				cluster.setRoot(null);
				cluster.setParent(null);
			} else {
				// Otherwise repoint things to the new root.
				if (cluster.getRoot()!=null && cluster.getRoot().equals(oldRoot)) {
					cluster.setRoot(newRoot);
				}
				// Adjust the parent if necessary as well.
				if (cluster.getParent()!=null && cluster.getParent().equals(oldRoot)) {
					cluster.setParent(newRoot);
				}
			}
		}
	}
	
	private static void updateReferenceMap(Map<String, String> refMap, String oldId, String refId) {
		
		if (refMap == null) return;
		
		// First, add the oldId->refId to the map
		refMap.put(oldId, refId);
		
		// Then, find any existing references in the map that point to oldId.
		// Change them to point to refId
		List<String> changeList = new ArrayList<String>();
		for (String key : refMap.keySet()) {
			if (refMap.get(key).equals(oldId)) {
				changeList.add(key);
			}
		}
		
		for (String changeKey : changeList) {
			refMap.put(changeKey, refId);
		}
	}
}
