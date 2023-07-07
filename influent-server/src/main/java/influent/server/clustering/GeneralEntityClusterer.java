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

import influent.idl.FL_Cluster;
import influent.idl.FL_Entity;
import influent.idl.FL_Geocoding;
import influent.server.clustering.utils.EntityClusterFactory;
import influent.server.clustering.utils.PropertyManager;

public class GeneralEntityClusterer extends BaseEntityClusterer {
	private FL_Geocoding geoCoder;
	private EntityClusterFactory clusterFactory;
	private PropertyManager pMgr;
	private List<EntityClusterer> clusterStages;
	private int MIN_CLUSTER_SIZE = 10;

	@Override
	public void init(Object[] args) {
		try {
			this.clusterFactory = (EntityClusterFactory)args[0];
			this.geoCoder = (FL_Geocoding)args[1];
			this.pMgr = (PropertyManager)args[2];
			this.MIN_CLUSTER_SIZE = Integer.parseInt(pMgr.getProperty(PropertyManager.MIN_CLUSTER_SIZE, "10"));
			this.clusterStages = createClusterStages(pMgr);
		}
		catch (Exception e) {
			throw new IllegalArgumentException("Invalid initialization parameters.", e);
		}
	}
	
	private List<EntityClusterer> createClusterStages(PropertyManager pMgr){
		List<EntityClusterer> clusterStages = new ArrayList<EntityClusterer>();
		
		String[] clusterFields = pMgr.getProperty(PropertyManager.CLUSTER_FIELDS, "GEO:geo,TYPE:categorical,LABEL:label").split(",");
		
		for (String field : clusterFields) {
			String[] tokens = field.split(":");  // TagOrFieldName, Type, Fuzzy (if label feature)
			if (tokens.length > 1) {
				String tagOrFieldName = tokens[0];
				String type = tokens[1];
				
				if (type.equalsIgnoreCase("categorical")) {
					CategoricalEntityClusterer clusterer = new CategoricalEntityClusterer();
					Object[] params = {clusterFactory, tagOrFieldName}; 
					clusterer.init(params);
					clusterStages.add(clusterer);
				}
				else if (type.equalsIgnoreCase("label")) {
					String clusterType = tokens.length == 3 ? tokens[2] : "fingerprint";  // default to fingerprint if none
					
					// first group alphabetically
					LabelEntityClusterer clusterer = new LabelEntityClusterer();
					Object[] params = {clusterFactory, tagOrFieldName, "alpha", pMgr}; 
					clusterer.init(params);
					clusterStages.add(clusterer);
					
					// next fuzzy or finger print cluster
					clusterer = new LabelEntityClusterer();
					Object[] params2 = {clusterFactory, tagOrFieldName, clusterType, pMgr}; 
					clusterer.init(params2);
					clusterStages.add(clusterer);
				}
				else if (type.equalsIgnoreCase("geo")) {
					GeoEntityClusterer clusterer = new GeoEntityClusterer();
					Object[] params = {clusterFactory, tagOrFieldName, geoCoder, GeoEntityClusterer.GEO_LEVEL.Continent}; 
					clusterer.init(params);
					clusterStages.add(clusterer);
					
					clusterer = new GeoEntityClusterer();
					Object[] params2 = {clusterFactory, tagOrFieldName, geoCoder, GeoEntityClusterer.GEO_LEVEL.Region}; 
					clusterer.init(params2);
					clusterStages.add(clusterer);
					
					clusterer = new GeoEntityClusterer();
					Object[] params3 = {clusterFactory, tagOrFieldName, geoCoder, GeoEntityClusterer.GEO_LEVEL.Country}; 
					clusterer.init(params3);
					clusterStages.add(clusterer);
					
					clusterer = new GeoEntityClusterer();
					Object[] params4 = {clusterFactory, tagOrFieldName, geoCoder, GeoEntityClusterer.GEO_LEVEL.LatLon}; 
					clusterer.init(params4);
					clusterStages.add(clusterer);
				}
				else if (type.equalsIgnoreCase("numeric")) {
					Integer numBins = tokens.length == 3 ? Integer.parseInt(tokens[2]) : 5;  // default to 5 bins
					NumericEntityClusterer clusterer = new NumericEntityClusterer();
					Object[] params = {clusterFactory, tagOrFieldName, numBins}; 
					clusterer.init(params);
					clusterStages.add(clusterer);
				}
				else {
					log.warn("Invalid cluster field type specified in clusterer.properties - verify the value of entity.clusterer.clusterfields");
				}
			}
			else {
				log.warn("Invalid cluster field specified in clusterer.properties - verify the value of entity.clusterer.clusterfields");
			}
		}
		
		return clusterStages;
	}
	
	private List<FL_Entity> getClusterMembers(FL_Cluster cluster, ClusterContext context) {
		List<FL_Entity> members = new LinkedList<FL_Entity>();
		
		for (String id : cluster.getMembers()) {
			members.add(context.entities.get(id));
		}
		return members;
	}
	
	private List<FL_Cluster> getSubClusters(FL_Cluster cluster, ClusterContext context) {
		List<FL_Cluster> subClusters = new LinkedList<FL_Cluster>();
		
		for (String id : cluster.getSubclusters()) {
			subClusters.add( context.clusters.get(id) );
		}
		return subClusters;
	}

	@Override
	public ClusterContext clusterEntities(Collection<FL_Entity> entities) {
		return this.clusterEntities(entities, new ClusterContext());
	}
	
	@Override
	public ClusterContext clusterEntities(Collection<FL_Entity> entities, Collection<FL_Cluster> clusters, ClusterContext context) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public ClusterContext clusterEntities(Collection<FL_Entity> entities, ClusterContext context) {
		if (clusterStages == null || clusterStages.isEmpty()) return null;
	
		// create a default context if the passed in one is empty
		if (context == null) {
			context = new ClusterContext();
		}
		
		// add the entities to the context
		context.addEntities(entities);
		
		// first stage generates root objects in entity cluster hierarchy
		ClusterContext results = clusterStages.get(0).clusterEntities(entities, context.roots.values(), context);
		
		// add the new/modified roots to the context roots
		context.roots.putAll(results.roots);
		
		// keep track of the modified roots - only they need to have their summaries recomputed
		Map<String, FL_Cluster> modifiedRoots = new HashMap<String, FL_Cluster>(results.roots);
		
		// find the candidate clusters to split
		Collection<FL_Cluster> clustersToSplit = new LinkedList<FL_Cluster>();
		findClustersToSplit(results.roots, clustersToSplit);
		
		// add the roots to the allClusters map in context
		context.clusters.putAll(results.roots);
		
		//
		// Top down Divisive hierarchical clustering using binning and k-means clustering - each stage clusters by a distinct feature
		//
		for (int i=1; i < clusterStages.size(); i++) {
			EntityClusterer clusterer = clusterStages.get(i); 
					
			Map<String, FL_Cluster> stageResults = new HashMap<String, FL_Cluster>();
			
			for (FL_Cluster cluster : clustersToSplit) {
				// sub-cluster the entity cluster
				results = clusterer.clusterEntities( getClusterMembers(cluster, context), getSubClusters(cluster, context), context );
				
				// update the cluster children to be the sub-clustering results
				// and update the root and parent of the children
				updateClusterReferences(cluster, results.roots);	

				// store the new/modified clusters in the context
				context.clusters.putAll(results.roots);
					
				// schedule the cluster for further splitting on another stage
				stageResults.putAll(results.roots);
			}
			// update the clusters to split for next stage
			clustersToSplit.clear();
			findClustersToSplit(stageResults, clustersToSplit);
		}
		
		// lastly update the modified clusters summaries
		clusterFactory.updateClusterProperties(modifiedRoots, context);
		
		// return back the modified context
		return context;
	}
	
	private void findClustersToSplit(Map<String, FL_Cluster> candidates, Collection<FL_Cluster> splitQueue) {
		for (String id : candidates.keySet()) {
			FL_Cluster c = candidates.get(id);
			if (c.getMembers().size() > MIN_CLUSTER_SIZE || (c.getMembers().size() > 0 && c.getSubclusters().size() > 0))  {
				splitQueue.add(c);
			}
		}
	}

	private void updateClusterReferences(FL_Cluster cluster, Map<String, FL_Cluster> children) {
		List<String> childClusterIds = new LinkedList<String>();
		
		String parentId = cluster.getUid();
		String rootId = cluster.getRoot() == null ? parentId : cluster.getRoot();
		
		for (String id : children.keySet()) {
			FL_Cluster c = children.get(id);
			c.setParent(parentId);
			c.setRoot(rootId);
			childClusterIds.add(c.getUid());
		}
		cluster.getMembers().clear();
		Set<String> uniqueIds = new HashSet<String>(cluster.getSubclusters());
		uniqueIds.addAll(childClusterIds);
		cluster.setSubclusters(new LinkedList<String>(uniqueIds));
	}
}
