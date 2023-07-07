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
import influent.server.utilities.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClusterCollapser {

	private static final Logger s_logger = LoggerFactory.getLogger(ClusterCollapser.class);

	private static final Comparator<FL_Cluster> COLLAPSE_COMPARATOR =  new Comparator<FL_Cluster>() {
		@Override
		public int compare(FL_Cluster o1, FL_Cluster o2) {
			return o2.getLevel() - o1.getLevel();
		}
	};
	
	//Returns a simplified set of clusters where all the chain-links have been removed (a chain-link being a
	//subcluster that contains only 1 child (1 subcluster or 1 member)
	//
	//Note that this method modifies the FL_Cluster objects directly, it does not make copies
	//
	//Note that the first element in the pair contains JUST the entity ids that have no significant clustering in the
	//context, not all the entities.  However, the second part of the pair DOES contain all the relevant cluster components
	//after simplifcation.
	public static Pair<Collection<String>, Collection<FL_Cluster>> collapse(List<FL_Cluster> clusterList, boolean leaveRoots, boolean pruneDeadBranches, Map<String, String> output_redirect) {
				
		Pair<Collection<String>, Collection<FL_Cluster>> output = new Pair<Collection<String>, Collection<FL_Cluster>>();
		
		Map<String, FL_Cluster> results = new LinkedHashMap<String, FL_Cluster>();

		Collection<String> entities = new ArrayList<String>();
		
		for (FL_Cluster cluster : clusterList) {
			results.put(cluster.getUid(),cluster);
		}
		
		// list MUST be sorted for dead branch removal
		if (pruneDeadBranches) {
			Collections.sort(clusterList, COLLAPSE_COMPARATOR);
		
			for(FL_Cluster cluster : clusterList) {			// first, prune dead branches from the cluster
				if(cluster.getMembers().size() == 0 && cluster.getSubclusters().size() == 0) {		
					results.remove(cluster.getUid());
					FL_Cluster parentCluster = results.get(cluster.getParent());
					if (parentCluster == null) {
						s_logger.error("Cannot locate parent " + cluster.getParent() + " for cluster " + cluster.getUid());
					}
					else {
						parentCluster.getSubclusters().remove(cluster.getUid());
						updateReferenceMap(output_redirect, cluster.getUid(), cluster.getParent());
					}
				}
			}
		}
		
		for (FL_Cluster cluster : clusterList) {		// next, prune any and all chain-links
			if (!results.containsKey(cluster.getUid())) {
				continue;
			}
			
			if (cluster.getMembers().size() == 0 && cluster.getSubclusters().size() == 1) {
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
					parentCluster.getSubclusters().remove(cluster.getUid());
					parentCluster.getSubclusters().add(subCluster.getUid());
					updateReferenceMap(output_redirect,cluster.getUid(), cluster.getSubclusters().get(0));
				}
				
			} else if (cluster.getMembers().size() == 1 && cluster.getSubclusters().size() == 0) {
				
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
					}
					// remove the cluster id from the parent subcluster list and add the entity id from above
					parentCluster.getSubclusters().remove(cluster.getUid());
					parentCluster.getMembers().add(cluster.getMembers().get(0));
					updateReferenceMap(output_redirect, cluster.getUid(), cluster.getMembers().get(0));
				}
				
			} else {
				// do nothing, cluster remains in set.
			}
		}
		
		output.first = entities;
		output.second = results.values();
		
		return output;
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
