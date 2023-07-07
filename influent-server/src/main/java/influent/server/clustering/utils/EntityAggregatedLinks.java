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


import influent.idl.FL_BoundedRange;
import influent.idl.FL_Cluster;
import influent.idl.FL_Clustering;
import influent.idl.FL_ClusteringDataAccess;
import influent.idl.FL_DataAccess;
import influent.idl.FL_DateRange;
import influent.idl.FL_DirectionFilter;
import influent.idl.FL_Entity;
import influent.idl.FL_LevelOfDetail;
import influent.idl.FL_Link;
import influent.idl.FL_LinkEntityTypeFilter;
import influent.idl.FL_LinkTag;
import influent.idl.FL_Property;
import influent.idl.FL_PropertyTag;
import influent.idl.FL_PropertyType;
import influent.idlhelper.EntityHelper;
import influent.idlhelper.LinkHelper;
import influent.idlhelper.PropertyHelper;
import influent.idlhelper.SingletonRangeHelper;
import influent.server.clustering.BaseEntityClusterer;
import influent.server.clustering.utils.ClusterContextCache.PermitSet;
import influent.server.dataaccess.DataAccessException;
import influent.server.utilities.TypedId;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.avro.AvroRemoteException;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EntityAggregatedLinks {
	private static final String PROP_LINK_COUNT = "link-count";
	private static final String PROP_DATE = "cluster-link-date-range";
	
	private static final Logger s_logger = LoggerFactory.getLogger(EntityAggregatedLinks.class);
	
	/***
	 * Retrieve related links for src entityId and aggregate them by clustering the destination entities. Entities can be FL_Entities or FL_Clusters
	 * 
	 * @param entityId - src entity to fetch related links for
	 * @param direction - the direction of the link from the entities
	 * @param tag - a tag filter for returned related links
	 * @param dateRange - a date filter for returned related links
	 * @param da - the data access module
	 * @param clusterAggregation - whether to cluster the destination entities
	 * @param clusterer - the entity clustering module
	 * @param clusterAccess - the entity clustering data access module
	 * @param srcContext - the source entities context
	 * @param dstContextId - the dest entities context id
	 * @param cache - the cluster context cache
	 * @return a map of src entities and their aggregated related links
	 * @throws DataAccessException
	 * @throws AvroRemoteException
	 */
	public static Map<String, List<FL_Link>> getRelatedAggregatedLinks(String entityId,
																	FL_DirectionFilter direction,
																	FL_LinkTag tag,
																	FL_DateRange dateRange,
																	FL_DataAccess da,
																	boolean clusterAggregation,
																	FL_Clustering clusterer,
																	FL_ClusteringDataAccess clusterAccess,
																	ContextRead srcContext,
																	ContextReadWrite dstContext,
																	ClusterContextCache cache
																	) throws DataAccessException, AvroRemoteException {
		return getRelatedAggregatedLinks(Collections.singletonList(entityId), 
										 direction, 
										 tag, 
										 dateRange, 
										 da, 
										 clusterAggregation, 
										 clusterer, 
										 clusterAccess, 
										 srcContext, 
										 dstContext, 
										 cache);
	}
	
	/***
	 * Retrieve related links for src entityIds and aggregate them by clustering the destination entities. Entities can be FL_Entities or FL_Clusters
	 * 
	 * @param entityIds - src entities to fetch related links for
	 * @param direction - the direction of the link from the entities
	 * @param tag - a tag filter for returned related links
	 * @param dateRange - a date filter for returned related links
	 * @param da - the data access module
	 * @param clusterAggregation - whether to cluster the destination entities
	 * @param clusterer - the entity clustering module
	 * @param clusterAccess - the entity clustering data access module
	 * @param srcContext - the source entities context
	 * @param dstContextId - the dest entities context id
	 * @param cache - the cluster context cache
	 * @return a map of src entities and their aggregated related links
	 * @throws DataAccessException
	 * @throws AvroRemoteException
	 */
	public static Map<String, List<FL_Link>> getRelatedAggregatedLinks(List<String> entityIds, 
																	FL_DirectionFilter direction, 
																	FL_LinkTag tag,
																	FL_DateRange dateRange,
																	FL_DataAccess da,
																	boolean clusterAggregation,
																	FL_Clustering clusterer,
																	FL_ClusteringDataAccess clusterAccess,
																	ContextRead srcContext,
																	ContextReadWrite dstContext,
																	ClusterContextCache cache
																	) throws DataAccessException, AvroRemoteException {
		
		try {
			s_logger.info("Started getRelatedAggregateLinks for "+entityIds.size()+" entities");
			
			if (entityIds == null || entityIds.isEmpty()) {
				s_logger.warn("Empty entity ID list passed to getRelatedAggregatedLinks");
				return null;
			}
			 
			long startms = System.currentTimeMillis();
			
			// filter source entities by type
			List<String> srcClusterIds = TypedId.filterTypedIds(entityIds, TypedId.CLUSTER);
			List<String> srcOwnerIds = TypedId.filterTypedIds(entityIds, TypedId.ACCOUNT_OWNER);
			List<String> srcSummaryIds = TypedId.filterTypedIds(entityIds, TypedId.CLUSTER_SUMMARY);
			List<String> srcEntityIds = TypedId.filterTypedIds(entityIds, TypedId.ACCOUNT);
			
			// retrieve the src entities
			List<Object> srcThings = new ArrayList<Object>();
			srcThings.addAll( srcContext.getClusters(srcClusterIds) );
			srcThings.addAll( srcOwnerIds );
			srcThings.addAll( srcSummaryIds );
			srcThings.addAll( srcEntityIds );
						
			s_logger.info("Found "+srcThings.size()+" src entities  (total time : "+(System.currentTimeMillis()-startms)/1000+"s)");
			
			startms = System.currentTimeMillis();
			
			// keep track of the parent of each entity
			Map<String, String> entityAncestorIndex = new HashMap<String, String>();  // entity id -> ancestor id
			
			// Get all the atomic children ids associated with src things and create ancestor map
			List<String> srcChildIds = buildEntityAncestorMap(srcThings, entityAncestorIndex, srcContext);
						
			// filter the source children by type
			List<String> srcChildEntityIds = TypedId.filterTypedIds(srcChildIds, TypedId.ACCOUNT);
			List<String> srcChildOwnerIds = TypedId.filterTypedIds(srcChildIds, TypedId.ACCOUNT_OWNER);
			List<String> srcChildSummaryIds = TypedId.filterTypedIds(srcChildIds, TypedId.CLUSTER_SUMMARY);
			
			s_logger.info("Created ancestor map (total time : "+(System.currentTimeMillis()-startms)/1000+"s)");
			
			startms = System.currentTimeMillis();
			
			Map<String, List<FL_Link>> rawLinks = new HashMap<String, List<FL_Link>>();
			Map<String, List<FL_Link>> relatedLinks =  new HashMap<String, List<FL_Link>>();
			
			// Get the related links for all the child srcEntities and add to the raw related links for further processing
			rawLinks.putAll( da.getFlowAggregation(srcChildEntityIds, new ArrayList<String>(0), direction, FL_LinkEntityTypeFilter.ACCOUNT, tag ,dateRange) );
			
			// add account owner links to the raw links
			rawLinks.putAll( da.getFlowAggregation(srcChildOwnerIds, new ArrayList<String>(0), direction, FL_LinkEntityTypeFilter.ACCOUNT, tag ,dateRange) );
						
			// add cluster summary links to the raw links
			rawLinks.putAll( da.getFlowAggregation(srcChildSummaryIds, new ArrayList<String>(0), direction, FL_LinkEntityTypeFilter.CLUSTER_SUMMARY, tag ,dateRange) );
						
			// filter out duplicate links already contained in child contexts (files)
			filterDuplicateLinks(dstContext, rawLinks, cache);
			
			s_logger.info("Found related raw links (total time : "+(System.currentTimeMillis()-startms)/1000+"s)");
					
			startms = System.currentTimeMillis();
			
			// If no aggregation was request just pass back the raw links 
			if (!clusterAggregation) return rawLinks;
			
			// retrieve the dstEntities associated with rawLinks
			Set<String> dstIds = getDestLinkEntity(rawLinks); 
			
			// filter the accounts from the destinations
			List<String> dstEntityIds = TypedId.filterTypedIds(dstIds, TypedId.ACCOUNT);
		
			// filter the account owners from the destinations
			List<String> dstOwnerIds = TypedId.filterTypedIds(dstIds, TypedId.ACCOUNT_OWNER);
			
			// filter the cluster summaries from the destinations
			List<String> dstClusterSummaryIds = TypedId.filterTypedIds(dstIds, TypedId.CLUSTER_SUMMARY);
			
			// fetch the dst entities for clustering
			List<FL_Entity> dstEntities = da.getEntities(dstEntityIds, FL_LevelOfDetail.SUMMARY);
			
			// fetch the dst account owners for clustering
			List<FL_Cluster> dstImmutableClusters = clusterAccess.getAccountOwners(dstOwnerIds);
			
			// fetch the dst account summaries for clustering
			dstImmutableClusters.addAll( clusterAccess.getClusterSummary(dstClusterSummaryIds) );
			
			// handle loop links - they are exempt from clustering and returned directly
			filterLoopEntitiesAndLinks(rawLinks, relatedLinks, dstEntities);
			
			// We now have loop free raw links from all src and dst immutable objects (entities, immutable clusters)
			// Now we can cluster dst entities and aggregate links on src and dst by cluster membership
			
			s_logger.info("Found "+dstEntities.size()+" destination entities (total time : "+(System.currentTimeMillis()-startms)/1000+"s)");
			
			startms = System.currentTimeMillis();
				
			// cluster the dst entities and immutable clusters
			clusterer.clusterEntities(dstEntities, dstImmutableClusters, srcContext.getUid(), dstContext.getUid());
			
			s_logger.info("Done clustering link entities (total time : "+(System.currentTimeMillis()-startms)/1000+"s)");

			startms = System.currentTimeMillis();
			
			// retrieve the dst cluster context so we can aggregate raw links by cluster membership
			List<FL_Cluster> ctxEnts = clusterAccess.getContext(dstContext.getUid());
				
			// create an index for clusters in dst context
			Map<String, FL_Cluster> resultClusterMap = new HashMap<String, FL_Cluster>();
			for (FL_Cluster c : ctxEnts) {
				resultClusterMap.put(c.getUid(), c);
			}	
			
			List<Object> dstThings = new LinkedList<Object>(dstEntities);
			dstThings.addAll(dstImmutableClusters);
			
			// update the ancestor index for the dst things
			updateAncestorIndex(dstThings, resultClusterMap, entityAncestorIndex);
			
			s_logger.info("Created link entity ancestor map (total time : "+(System.currentTimeMillis()-startms)/1000+"s)");
							
			startms = System.currentTimeMillis();
			
			// now we can aggregate on the linkEntities and raw links - add the to the relatedLinks
			Map<String, List<FL_Link>> agLinks = aggregateLinks(rawLinks, direction, entityAncestorIndex);
			
			// Need to merge results, not put, or loop links might get lost.
			for (String agKey : agLinks.keySet()) {
				if (relatedLinks.containsKey(agKey)) {
					relatedLinks.get(agKey).addAll(agLinks.get(agKey));
				} else {
					relatedLinks.put(agKey, agLinks.get(agKey));
				}
			}
			
			s_logger.info("Aggregation complete (aggregation took "+(System.currentTimeMillis()-startms)/1000+"s)");
		
			return relatedLinks;
		
		} catch (Exception e) {
			s_logger.error("Exception caught during getRelatedAggregateLinks : "+e.getMessage(),e);
			throw new DataAccessException("Exception while aggregating links "+e.getMessage(),e);
		}															
	}
	
	private static void filterLoopEntitiesAndLinks(Map<String, List<FL_Link>> rawLinks, 
												   Map<String, List<FL_Link>> relatedLinks, 
												   List<FL_Entity> dstEntities) {
		for (String key : new HashSet<String>(rawLinks.keySet())) { // make copy of keyset to avoid concurrent modification exception
			List<FL_Link> loops = new ArrayList<FL_Link>();
			for (FL_Link link : rawLinks.get(key)) {
				if (link.getSource().equals(link.getTarget())) {
					loops.add(link);
					
					//Find the entity in dstEntities and remove it
					for (FL_Entity fle : dstEntities) {
						if (fle.getUid().equals(key)) {
							dstEntities.remove(fle);
							break;
						}
					}
				}
			}
			
			//Remove from the rawlinks (don't process in clustering)
			rawLinks.get(key).removeAll(loops);
			if (rawLinks.get(key).isEmpty()) {
				rawLinks.remove(key);
			}
			
			//Add to related links
			if (!loops.isEmpty()) relatedLinks.put(key, loops);
		}
	}
	
	private static void filterDuplicateLinks(ContextRead targetContext, Map<String, List<FL_Link>> rawLinks, ClusterContextCache cache) {
		PermitSet permits = new PermitSet();
		
		try {	
			Set<String> duplicateEntities = new HashSet<String>();
			if (targetContext != null) {
				// find all entities belonging to child contexts
				for (String childContextId : targetContext.getChildContexts()) {
					ContextRead childContext = cache.getReadOnly(childContextId, permits);
					
					if (childContext != null) {
						if (childContext.getContext() != null) {
							duplicateEntities.addAll( childContext.getContext().entities.keySet() );
						}
						List<String> clusterSummaryIds = new LinkedList<String>();
						for (FL_Cluster cluster : childContext.getClusters()) {
							if (TypedId.hasType(cluster.getUid(), TypedId.CLUSTER_SUMMARY)) {
								clusterSummaryIds.add(cluster.getUid());
							}
						}
						duplicateEntities.addAll( clusterSummaryIds );
					}
				}
				
				// filter links to duplicate entities
				for (String key : new HashSet<String>(rawLinks.keySet())) { // make copy of keyset to avoid concurrent modification exception
					List<FL_Link> links = rawLinks.get(key);
					List<FL_Link> filteredLinks = new LinkedList<FL_Link>();
					for (FL_Link link : links) {
						String destId = link.getTarget().equalsIgnoreCase(key) ? link.getSource(): link.getTarget();
						
						if (!duplicateEntities.contains(destId)) {
							filteredLinks.add(link);
						}
					}
					
					if (filteredLinks.isEmpty()) {
						rawLinks.remove(key);
					}
					else {
						rawLinks.put(key, filteredLinks);
					}
				}
			}
			
		} finally {
			permits.revoke();
		}
	}

	
	/***
	 * Retrieve aggregated related links between src entityIds and dst entityIds. Entities can be FL_Entities or FL_Clusters
	 * 
	 * @param srcEntityIds - src entities to fetch related links for
	 * @param dstEntityIds - dst entities to fetch related links for
	 * @param direction - the direction of the link from the entities
	 * @param tag - a tag filter for returned related links
	 * @param dateRange - a date filter for returned related links
	 * @param da - the data access module
	 * @param clusterAccess - the entity clustering data access module
	 * @param srcContext - the source entities context
	 * @param dstContext - the dest entities context
	 * @param sessionId - the current session id
	 * @return a map of src entities and their aggregated related links to dst entities
	 * @throws DataAccessException
	 * @throws AvroRemoteException
	 */
	public static Map<String, List<FL_Link>> getAggregatedLinks(List<String> srcEntityIds, 
															 List<String> dstEntityIds,
															 FL_DirectionFilter direction,
															 FL_LinkTag tag,
															 FL_DateRange dateRange,  
															 FL_DataAccess da,
															 FL_ClusteringDataAccess clusterAccess,
															 ContextRead srcContext,
															 ContextRead dstContext
															 ) throws DataAccessException {
		
		try {
			s_logger.info("Started getAggregatedLinks for "+srcEntityIds.size()+" src entities and "+dstEntityIds.size()+ " dst entities");
			
			if (srcEntityIds == null || srcEntityIds.isEmpty()) {
				s_logger.warn("Empty src entity ID list passed to getAggregatedLinks");
				return null;
			}
			
			if (dstEntityIds == null || dstEntityIds.isEmpty()) {
				s_logger.warn("Empty dst entity ID list passed to getAggregatedLinks");
				return null;
			}
		
			long startms = System.currentTimeMillis();
			
			// retrieve the src entities
			List<Object> srcThings = new ArrayList<Object>();
			if (srcContext != null) {
				srcThings.addAll( srcContext.getClusters(TypedId.filterTypedIds(srcEntityIds, TypedId.CLUSTER)) );
			}
			srcThings.addAll( TypedId.filterTypedIds(srcEntityIds, TypedId.ACCOUNT_OWNER) );
			srcThings.addAll( TypedId.filterTypedIds(srcEntityIds, TypedId.CLUSTER_SUMMARY) );
			srcThings.addAll( TypedId.filterTypedIds(srcEntityIds, TypedId.ACCOUNT) );
			
			s_logger.info("Found "+srcThings.size()+" src entities  (total time : "+(System.currentTimeMillis()-startms)/1000+"s)");
		
			startms = System.currentTimeMillis();
			
			// retrieve the dst entities
			List<Object> dstThings = new ArrayList<Object>();
			if (dstContext != null) {
				dstThings.addAll( dstContext.getClusters(TypedId.filterTypedIds(dstEntityIds, TypedId.CLUSTER)) );
			}
			dstThings.addAll( TypedId.filterTypedIds(dstEntityIds, TypedId.ACCOUNT_OWNER) );
			dstThings.addAll( TypedId.filterTypedIds(dstEntityIds, TypedId.CLUSTER_SUMMARY) );
			dstThings.addAll( TypedId.filterTypedIds(dstEntityIds, TypedId.ACCOUNT) );
			
			s_logger.info("Found "+dstThings.size()+" dst entities  (total time : "+(System.currentTimeMillis()-startms)/1000+"s)");
		
			startms = System.currentTimeMillis();
			
			// keep track of the parent of each entity
			Map<String, String> entityAncestorIndex = new HashMap<String, String>();  // entity id -> ancestor id
			
			// Get all the entity Ids associated with srcEntities and create ancestor map
			List<String> srcChildEntityIds = buildEntityAncestorMap(srcThings, entityAncestorIndex, srcContext);
		
			// create ancestor map for dstEntities
			List<String> dstChildEntityIds = buildEntityAncestorMap(dstThings, entityAncestorIndex, dstContext);
			
			s_logger.info("Created ancestor map (total time : "+(System.currentTimeMillis()-startms)/1000+"s)");
		
			startms = System.currentTimeMillis();
			
			// Get the related links for the srcEntities to dstEntities
			Map<String, List<FL_Link>> rawLinks = da.getFlowAggregation(srcChildEntityIds, dstChildEntityIds, direction, FL_LinkEntityTypeFilter.ANY, tag, dateRange);
			
			s_logger.info("Found related raw links (total time : "+(System.currentTimeMillis()-startms)/1000+"s)");
			
			startms = System.currentTimeMillis();
			
			// now we can aggregate on the linkEntities and raw links	
			Map<String, List<FL_Link>> aggregatedLinks = aggregateLinks(rawLinks, direction, entityAncestorIndex);
			
			s_logger.info("Aggregation complete (aggregation took "+(System.currentTimeMillis()-startms)/1000+"s)");
			
			return aggregatedLinks;
		} catch (Exception e) {
			s_logger.error("Exception caught during getAggregatedLinks : "+e.getMessage(),e);
			throw new DataAccessException("Exception while aggregating links "+e.getMessage(),e);
		}	
	}	
	
	private static String findRootClusterId(FL_Cluster cluster, Map<String, FL_Cluster> searchIndex) {
		FL_Cluster ancestor = cluster;
		while (ancestor.getParent() != null && !ancestor.getParent().isEmpty()) {
			ancestor = searchIndex.get(ancestor.getParent());
		}
		return ancestor.getUid();
	}
	
	private static void updateAncestorIndex(List<Object> immutables, Map<String, FL_Cluster> searchIndex, Map<String, String> ancestorMap) {
		for (Object immutable : immutables) {
	
			if (immutable instanceof FL_Entity) {
				FL_Entity entity = (FL_Entity)immutable;
				PropertyHelper clusterProp = EntityHelper.getFirstPropertyByTag(entity, FL_PropertyTag.CLUSTER);
				
				if (clusterProp != null) {
					// find the root cluster for this entity
					String clusterId = (String)clusterProp.getValue();
					FL_Cluster ancestor = searchIndex.get(clusterId);
					String rootId = findRootClusterId(ancestor, searchIndex);
					ancestorMap.put(entity.getUid(), rootId);
				}
				else { // we could not find an ancestor then add the entity to the ancestor map as an ancestor to itself
					ancestorMap.put(entity.getUid(), entity.getUid());
				}
			}
			else { // must be a cluster
				FL_Cluster cluster = (FL_Cluster)immutable;
				ancestorMap.put( cluster.getUid(), findRootClusterId(cluster, searchIndex) );
			}
		}
	}
	
	private static List<String> buildEntityAncestorMap(Collection<? extends Object> things, 
			 										   Map<String, String> entityAncestorIndex,
			 										   ContextRead context) throws AvroRemoteException {
		List<String> entityIds = new LinkedList<String>();

		for (Object entity : things) {
			if (entity instanceof FL_Cluster) {
				FL_Cluster cluster = (FL_Cluster)entity;
				
				// find all the entities that are decendents of this cluster
				List<String> childIds = fetchLeaves(cluster, context);

				// keep track of the ancestor for each childId
				for (String childId : childIds) {
					entityIds.add(childId);
					entityAncestorIndex.put(childId, cluster.getUid());
				}
			} else {
				String id = (String)entity;
				entityIds.add(id);
				entityAncestorIndex.put(id, id);
			}
		}
		// return back all child entity ids (or cluster summaries) found
		return entityIds;
	}
	
	public static List<String> fetchLeaves(FL_Cluster cluster, ContextRead context) throws AvroRemoteException {
		List<String> eids = new ArrayList<String>();
		Deque<FL_Cluster> toProcess = new LinkedList<FL_Cluster>();
		toProcess.addLast(cluster);
		
		while(!toProcess.isEmpty()) {
			FL_Cluster procCluster = toProcess.removeFirst();
			
			List<String> subClusterIds = new LinkedList<String>();
			for (String subclusterId : procCluster.getSubclusters()) {
				if (BaseEntityClusterer.isImmutableCluster(subclusterId)) {  // immutable clusters are treated like leaves
					eids.add(subclusterId);
				}
				else {
					subClusterIds.add(subclusterId);
				}
			}
			if (!subClusterIds.isEmpty()) {
				toProcess.addAll( context.getClusters(subClusterIds) );
			}
			if (!procCluster.getMembers().isEmpty()) {
				eids.addAll(procCluster.getMembers());
			}
		}
		return eids;
	}
	
	public static List<String> fetchLeaves(String clusterId, ContextRead context) throws AvroRemoteException {
		return fetchLeaves( context.getCluster(clusterId), context );
	}
	
	public static List<String> fetchLeaves(List<String> clusterIds, ContextRead context) throws AvroRemoteException {
		List<String> eids = new ArrayList<String>();
		
		for (String clusterId : clusterIds) {
			eids.addAll( fetchLeaves(clusterId, context) );
		}
		return eids;
	}
	
	public static Map<String, List<FL_Link>> aggregateLinks(Map<String, 
														  List<FL_Link>> links, 
														  FL_DirectionFilter direction, 
														  Map<String, String> ancestorIndex) {
		Map<String, FL_Link> aggregateLinks = new HashMap<String, FL_Link>();
		
		for (String id : links.keySet()) {
			for (FL_Link link : links.get(id)) {
				String srcId = link.getSource();
				String src = ancestorIndex.get(srcId);
				String destId = link.getTarget();
				String dst = ancestorIndex.get(destId);
				
				// only process links that are in the ancestorIndex
				if (src == null || dst == null) continue;
			
				String key = src + "_:_" + dst;  // generate unique key for this cluster link

				FL_Link aggregateLink = aggregateLinks.get(key);
			
				if (aggregateLink == null) {
					List<FL_Property> props = new ArrayList<FL_Property>();
				
					DateTime minDate = null;
					DateTime maxDate = null;
					
					for (FL_Property prop : link.getProperties()) {
						PropertyHelper p = PropertyHelper.from(prop);
						props.add(p);
						if (p.getType() == FL_PropertyType.DATE) {
							DateTime pdate = new DateTime((long)(Long)p.getValue(), DateTimeZone.UTC);
							if (minDate== null || minDate.isAfter(pdate)) {
								minDate = pdate;
							}
							if (maxDate == null || maxDate.isBefore(pdate)) {
								maxDate = pdate;
							}
						}
					}
					FL_Property linkCount = new PropertyHelper(PROP_LINK_COUNT, PROP_LINK_COUNT, 1, FL_PropertyType.LONG, Collections.singletonList(FL_PropertyTag.CONSTRUCTED));
					props.add(linkCount);
					
					if (minDate != null && maxDate != null) {
						List<FL_PropertyTag> datePropTags = new ArrayList<FL_PropertyTag>();
						datePropTags.add(FL_PropertyTag.CONSTRUCTED);
						datePropTags.add(FL_PropertyTag.STAT);
						datePropTags.add(FL_PropertyTag.DATE);
						FL_Property dateProp = new PropertyHelper(PROP_DATE,PROP_DATE, minDate, maxDate, FL_PropertyType.DATE, datePropTags);
						props.add(dateProp);
					}
					
					aggregateLink = FL_Link.newBuilder().setDirected(link.getDirected()).setProperties(props).setProvenance(null)
							.setSource(src).setTags(Collections.singletonList(FL_LinkTag.FINANCIAL)).setTarget(dst).setUncertainty(null).build();
					
					aggregateLinks.put(key, aggregateLink);
				}
				else {
					// otherwise increment the link count for the cluster link
					PropertyHelper linkCount = LinkHelper.getFirstProperty(aggregateLink, PROP_LINK_COUNT);
					PropertyHelper dateProp = LinkHelper.getFirstProperty(aggregateLink, PROP_DATE);

					int count = ((Integer)linkCount.getValue())+1;
					linkCount.setRange(new SingletonRangeHelper(count, FL_PropertyType.LONG));
				
					DateTime minDate = null;
					DateTime maxDate = null;
					if (dateProp != null) {
						FL_BoundedRange range = (FL_BoundedRange) dateProp.getRange();
						minDate = new DateTime((long)(Long)range.getStart(), DateTimeZone.UTC);
						maxDate = new DateTime((long)(Long)range.getEnd(), DateTimeZone.UTC);
					}
					
					// aggregate properties
					for (FL_Property prop : link.getProperties()) {
						PropertyHelper p = PropertyHelper.from(prop);
						//First, handle dates and update min-max date
						if (p.getType() == FL_PropertyType.DATE && minDate != null && maxDate!=null) {
							DateTime pdate = new DateTime((long)(Long)p.getValue(), DateTimeZone.UTC);
							if (minDate== null || minDate.isAfter(pdate)) {
								minDate = pdate;
								((FL_BoundedRange)dateProp.getRange()).setStart(minDate);
							}
							if (maxDate == null || maxDate.isBefore(pdate)) {
								maxDate = pdate;
								((FL_BoundedRange)dateProp.getRange()).setEnd(maxDate);
							}
							
						}
						
						List<PropertyHelper> a = LinkHelper.getProperties(aggregateLink, p.getKey());
						if (a != null && a.size() > 0) {
							switch (a.get(0).getType()) {
							case LONG:
								try {
									int oldival = (Integer)a.get(0).getValue();
									int ival = (Integer)p.getValue();
									a.get(0).setRange(new SingletonRangeHelper(oldival + ival, FL_PropertyType.LONG));
								} catch (Exception e) { /* ignore */ }
								break;
							case DOUBLE:
								try {
									double olddval = (Double)a.get(0).getValue();
									double dval = (Double)p.getValue();
									a.get(0).setRange(new SingletonRangeHelper(olddval + dval, FL_PropertyType.DOUBLE));
								} catch (Exception e) { /* ignore */ }
								break;
							default:
								continue;
							}
						}
						else {
							link.getProperties().add(p);
						}
					}
				}
			}
		}
		
		Map<String, List<FL_Link>> results = new HashMap<String, List<FL_Link>>();
		
		// create the resulting links for each src entity
		for (String key : aggregateLinks.keySet()) {
			FL_Link link = aggregateLinks.get(key);
		
			String[] tokens = key.split("_:_");
				
			String src = (direction == FL_DirectionFilter.SOURCE) ? tokens[0] : tokens[1];
			
			List<FL_Link> linkset = results.get(src);
			
			if (linkset == null) {
				linkset = new LinkedList<FL_Link>();
				results.put(src, linkset);
			}
			linkset.add(link);
		}
		
		return results;
	}
	
	private static Set<String> getDestLinkEntity(Map<String, List<FL_Link>> links) {
		// Get all the dst entity Ids associated with links
		Set<String> dstEntityIds = new HashSet<String>();
		for (String src : links.keySet()) {
			for (FL_Link link : links.get(src)) {
				if (link.getTarget().equalsIgnoreCase(src)) {
					dstEntityIds.add(link.getSource());
				}
				else {
					dstEntityIds.add(link.getTarget());
				}
			}
		}
		return dstEntityIds;
	}
}
