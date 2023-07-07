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
package influent.server.dataaccess;

import influent.idl.FL_Cluster;
import influent.idl.FL_Clustering;
import influent.idl.FL_DataAccess;
import influent.idl.FL_DateRange;
import influent.idl.FL_DirectionFilter;
import influent.idl.FL_Entity;
import influent.idl.FL_Geocoding;
import influent.idl.FL_LevelOfDetail;
import influent.idl.FL_Link;
import influent.idl.FL_LinkTag;
import influent.idl.FL_PropertyTag;
import influent.idl.FL_SortBy;
import influent.idlhelper.ClusterHelper;
import influent.idlhelper.EntityHelper;
import influent.server.clustering.BaseEntityClusterer;
import influent.server.clustering.ClusterContext;
import influent.server.clustering.EntityClusterer;
import influent.server.clustering.utils.ClusterContextCache;
import influent.server.clustering.utils.ClusterContextCache.PermitSet;
import influent.server.clustering.utils.ContextRead;
import influent.server.clustering.utils.ContextReadWrite;
import influent.server.clustering.utils.EntityAggregatedLinks;
import influent.server.clustering.utils.EntityClusterFactory;
import influent.server.utilities.Pair;
import influent.server.utilities.SQLConnectionPool;
import influent.server.utilities.TypedId;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import oculus.aperture.spi.common.Properties;

import org.apache.avro.AvroRemoteException;

import com.google.inject.Singleton;

/**
 * Dynamic clustering impl.
 * @author slangevi
 *
 */
@Singleton
public class DynamicClustering extends AbstractClusteringDataAccess implements FL_Clustering {
	
	private final ClusterContextCache _cache;
	
	public DynamicClustering(
		SQLConnectionPool connectionPool,
		DataNamespaceHandler namespaceHandler,
		FL_DataAccess entityAccess, 
		FL_Geocoding geocoding, 
		EntityClusterer clusterer, 
		EntityClusterFactory clusterFactory,
		Properties config,
		ClusterContextCache cache
	) throws ClassNotFoundException, SQLException {
	
		super(connectionPool, namespaceHandler, entityAccess, geocoding, clusterer, clusterFactory, config);
		
		_clusterer.init( new Object[] {_clusterFactory, _geoCoder, config} );
		
		_cache = cache;
	}
	
	@Override
	public List<String> clusterEntitiesById(
		List<String> entityIds,
		String sourceContextId,
		String targetContextId
	) throws AvroRemoteException {
				
		List<String> accountIds = TypedId.filterTypedIds(entityIds, TypedId.ACCOUNT);
		List<String> clusterIds = TypedId.filterTypedIds(entityIds, TypedId.CLUSTER);
		clusterIds.addAll( TypedId.filterTypedIds(entityIds, TypedId.ACCOUNT_OWNER) );
		clusterIds.addAll( TypedId.filterTypedIds(entityIds, TypedId.CLUSTER_SUMMARY) );
		
		List<FL_Entity> entities = _entityAccess.getEntities(accountIds, FL_LevelOfDetail.SUMMARY);
		List<FL_Cluster> clusters = getClusters(clusterIds, sourceContextId);
		
		return clusterEntities( entities, clusters, sourceContextId, targetContextId );
	}
	
	@Override
	public List<String> clusterEntities(
		List<FL_Entity> entities,
		List<FL_Cluster> clusters,
		String sourceContextId,
		String targetContextId
	) throws AvroRemoteException {

		List<String> rootIds = new ArrayList<String>();
		
		PermitSet permits = new PermitSet();
		
		// fetch the current context
		try {			
			// can be relatively long time to hold a lock on the context but not much to be done about it.
			ContextReadWrite contextRW = _cache.getReadWrite(targetContextId, permits);
			ClusterContext targetContext = contextRW.getContext();
			
			// create a default context if the passed in one is empty
			if (targetContext == null) {
				targetContext = new ClusterContext();
			}
					
			// list of entities to cluster
			List<FL_Entity> newEntities = new ArrayList<FL_Entity>(entities.size());
			
			// split clusters into mutable and immutable clusters
			List<FL_Cluster> mutableClusters = BaseEntityClusterer.filterMutableClusters(clusters);
			List<FL_Cluster> immutableClusters = BaseEntityClusterer.filterImmutableClusters(clusters);
			
			// list of immutable clusters to cluster
			List<FL_Cluster> newClusters = new ArrayList<FL_Cluster>();
		
			// mutable clusters we fetch their child entities and immutable clusters and cluster them
			Pair<List<FL_Entity>, List<FL_Cluster>> immutableChildren = getImmutableChildren(mutableClusters, sourceContextId);
			
			// add the discovered entities to the entities list
			entities.addAll(immutableChildren.first);
			
			// add the discovered immutable cluster to the immutableClusters list
			immutableClusters.addAll(immutableChildren.second);
			
			// filter out all entities that already belong to this context
			newEntities.addAll( filterExistingEntities(entities, targetContext) );
			
			// filter out all immutable clusters that already belong to this context
			newClusters.addAll( filterExistingClusters(immutableClusters, targetContext) );		
			
			// add new entities to context
			targetContext.addEntities(newEntities);
			
			// add immutable clusters to context
			targetContext.addClusters(newClusters);
			
			// add immutable cluster members to context
			targetContext.addEntities( getImmutableChildEntities(newClusters) );
			
			// nothing to do - return
			if (newEntities.isEmpty() && newClusters.isEmpty()) return rootIds;
			
			ClusterContext updatedContext = _clusterer.clusterEntities(newEntities, newClusters, targetContext);
			
			rootIds.addAll(updatedContext.roots.keySet());
			
			contextRW.setContext(updatedContext);
			
		} finally {
			permits.revoke();
		}
		return rootIds;
	}
	
	private List<FL_Entity> getImmutableChildEntities(List<FL_Cluster> immutableClusters) throws AvroRemoteException {
		List<FL_Entity> entities = new ArrayList<FL_Entity>();
		
		for (FL_Cluster immutableCluster : immutableClusters) {
			entities.addAll( _entityAccess.getEntities(immutableCluster.getMembers(), FL_LevelOfDetail.SUMMARY) );
		}
		return entities;
	}
	
	@Override
	public List<FL_Cluster> getClusters(
		List<String> clusterIds, 
		String contextId
	) throws AvroRemoteException {
		
		List<FL_Cluster> results = new ArrayList<FL_Cluster>();
		
		PermitSet permits = new PermitSet();
		
		// fetch the current context
		try {			
			final ContextRead contextRO = _cache.getReadOnly(contextId, permits);
			
			if (contextRO != null) {
				results.addAll( contextRO.getClusters(clusterIds) );
			}
		} finally {
			permits.revoke();
		}
		return results;
	}
	

	private void removeFromAncestor(FL_Entity entity, ClusterContext context) {
		if (entity != null) {
			String parentId = (String)EntityHelper.getFirstPropertyByTag(entity, FL_PropertyTag.CLUSTER).getValue();
			FL_Cluster parent = context.clusters.get(parentId);
			if (parent != null) {
				ClusterHelper.removeMember(parent, entity);
				if (parent.getMembers().isEmpty() && parent.getSubclusters().isEmpty()) {
					context.clusters.remove(parentId);
					if (parent.getParent() != null) {
						removeFromAncestor(parent, context);
					}
					else {
						context.roots.remove(parentId);
					}
				}
			}
		}
	}
		
	private void removeFromAncestor(FL_Cluster cluster, ClusterContext context) {
		if (cluster != null) {
			String parentId = cluster.getParent();
			if (parentId != null) {
				FL_Cluster parent = context.clusters.get(parentId);
				if (parent != null) {
					ClusterHelper.removeSubCluster(parent, cluster);
					if (parent.getMembers().isEmpty() && parent.getSubclusters().isEmpty()) {
						context.clusters.remove(parentId);
						if (parent.getParent() != null) {
							removeFromAncestor(parent, context);
						}
						else {
							context.roots.remove(parentId);
						}
					}
				}
			}
		}
	}
	
	@Override
	public long removeMembers(List<String> entityIds, String contextId)
			throws AvroRemoteException {

		int count = 0;
		
		PermitSet permits = new PermitSet();
		
		// make local copy of list to avoid concurrent modification exceptions
		List<String> removeMembers = new ArrayList<String>(entityIds);
		
		try {
			ContextReadWrite contextRW = _cache.getReadWrite(contextId, permits);
			ClusterContext context = contextRW.getContext();
			
			if (context != null) {			
				for (String id : removeMembers) {
					if (TypedId.hasType(id, TypedId.ACCOUNT)) { 
						// remove the entity from the context
						FL_Entity entity = context.entities.remove(id);
						if (entity != null) {
							removeFromAncestor(entity, context);
							count++;
						}
					}
					else {
						// remove the cluster and all it's members and sub-clusters from the context
						FL_Cluster cluster = context.clusters.remove(id);
						if (cluster != null) {
							count++;
						
							// remove all entities from the context that were members of this cluster
							for (String eId : cluster.getMembers()) {
								context.entities.remove(eId);
							}
						
							// remove all sub-clusters from the context too!
							count += removeMembers(cluster.getSubclusters(), contextId);
							
							if (cluster.getParent() != null) {
								removeFromAncestor(cluster, context);
							}
							else {
								context.roots.remove(id);
							}
						}
					} 
				}
				// lastly update the clusters starting from the roots
				_clusterFactory.updateClusterProperties(context.roots, context, true);
				
				// update the raw cluster context stored in the context
				contextRW.setContext(context);
			}
			
		} finally {
			permits.revoke();
		}
		return count;
	}
	
	@Override
	public List<FL_Cluster> getContext(
		String contextId
	) throws AvroRemoteException {
		
		List<FL_Cluster> results = new ArrayList<FL_Cluster>();

		PermitSet permits = new PermitSet();
		
		try {
			final ContextRead contextRO = _cache.getReadOnly(contextId, permits);
			
			if (contextRO != null) {
				final ClusterContext context = contextRO.getContext();
				
				if (context != null) {
					for (FL_Cluster c : context.clusters.values()) {
						if (c!= null) {
							// return copies of the cluster to avoid tampering with the internal context!
							FL_Cluster copy = FL_Cluster.newBuilder(c).build();
							// the default avro builders use a immutable list for members and subclusters
							// set mutable version explicitly to ease cluster simplification
							copy.setMembers( new ArrayList<String>(c.getMembers()) );
							copy.setSubclusters( new ArrayList<String>(c.getSubclusters()) );
							results.add( copy );
						}
					}
				}
			}
		} finally {
			permits.revoke();
		}
		return results;
	}
	
	@Override
	public boolean clearContext(
		String contextId
	) throws AvroRemoteException {
		PermitSet permits = new PermitSet();
		
		try {
			ContextReadWrite contextRW = _cache.getReadWrite(contextId, permits);
			if (contextRW != null) {
				contextRW.setContext(null);
				return true;
			}
		} finally {
			permits.revoke();
		}
		return false;
	}
	
	@Override
	public Map<String, List<FL_Link>> getFlowAggregation(
		List<String> entityIds,
		List<String> focusEntityIds, 
		FL_DirectionFilter direction,
		FL_LinkTag tag, 
		FL_DateRange date, 
		String entitiesContextId,
		String focusContextId
	) throws AvroRemoteException {
		
		PermitSet permits = new PermitSet();
		
		try {
			final ContextRead contextRO = _cache.getReadOnly(entitiesContextId, permits);
			
			if (focusEntityIds == null || focusEntityIds.isEmpty()) {
				final ContextReadWrite focusContextRW = _cache.getReadWrite(focusContextId, permits);
				Map<String, List<FL_Link>> relatedLinks = EntityAggregatedLinks.getRelatedAggregatedLinks(entityIds, direction, tag, date, _entityAccess, true, this, this, contextRO, focusContextRW, _cache);
				return relatedLinks;
			} else {
				final ContextRead focusContextRO = _cache.getReadOnly(focusContextId, permits);
				Map<String, List<FL_Link>> relatedLinks = EntityAggregatedLinks.getAggregatedLinks(entityIds, focusEntityIds, direction, tag, date, _entityAccess, this, contextRO, focusContextRO);
				return relatedLinks;
			}
		} catch (DataAccessException e) {
			throw new AvroRemoteException(e);
		} finally {
			permits.revoke();
		}
	}
	
	@Override
	public Map<String, List<FL_Link>> getTimeSeriesAggregation(
		List<String> entityIds, 
		List<String> focusEntityIds, 
		FL_LinkTag tag,
		FL_DateRange date, 
		String entitiesContextId, 
		String focusContextId
	) throws AvroRemoteException {

		//Find leaf nodes for focii, and keep track of leaf id ->original cluster id
		Map<String,String> fociiIdMap = new HashMap<String, String>();
		List<String> fociiList = new ArrayList<String>();
		for (String focusid : focusEntityIds) {
			Set<String> flids = getLeafIds(Collections.singletonList(focusid), focusContextId);
			fociiList.addAll(flids);
			for (String fli : flids) {
				
				// Point account owners and summaries to their owner account
				TypedId id = TypedId.fromTypedId(fli);
				if (id.getType() == TypedId.ACCOUNT_OWNER || 
					id.getType() == TypedId.CLUSTER_SUMMARY) {
						fli = TypedId.fromNativeId(TypedId.ACCOUNT, id.getNamespace(), id.getNativeId()).toString();
				} 				
				fociiIdMap.put(fli, focusid);
			}
		}
		
		//Find leaf nodes for entities, and keep track of leaf id ->original cluster id
		Map<String,String> entIdMap = new HashMap<String, String>();
		List<String> leafList = new ArrayList<String>();
		for (String entid : entityIds) {
			Set<String> elids = getLeafIds(Collections.singletonList(entid), entitiesContextId);
			leafList.addAll(elids);
			for (String eli : elids) {
				entIdMap.put(eli, entid);
			}
		}
		
		Map<String, List<FL_Link>> links = _entityAccess.getTimeSeriesAggregation(leafList, fociiList, tag, date);	
		
		//Change the ids on the links, so the map to the original clusters passed in, not the underlying leaf nodes.
		for (List<FL_Link> linklist : links.values()) {
			for (FL_Link link : linklist) {
				if (link.getSource()!=null && fociiIdMap.containsKey(link.getSource())) {
					link.setSource(fociiIdMap.get(link.getSource()));
				}
				if (link.getTarget()!=null && fociiIdMap.containsKey(link.getTarget())) {
					link.setTarget(fociiIdMap.get(link.getTarget()));
				}
			}
		}
		return links;
	}
	
	@Override
	public Map<String, List<FL_Link>> getAllTransactions(
		List<String> entityIds,
		FL_LinkTag tag, 
		FL_DateRange date, 
		FL_SortBy sort,
		List<String> linkFilter, 
		long max, 
		String contextId, 
		String sessionId
	) throws AvroRemoteException {
		
		// Currently not supported
		return null;
	}
	
	private List<FL_Entity> filterExistingEntities(List<FL_Entity> entities, ClusterContext context) {
		// if context doesn't exist then all entities are new
		if (context == null) return entities;
		
		List<FL_Entity> newEntities = new ArrayList<FL_Entity>(entities.size());
		
		for (FL_Entity entity : entities) {
			String id = entity.getUid();
			if (!context.entities.containsKey(id)) {
				newEntities.add(entity);
			}
		}
		return newEntities;
	}
	
	private List<FL_Cluster> filterExistingClusters(List<FL_Cluster> clusters, ClusterContext context) {
		// if context doesn't exist then all entities are new
		if (context == null) return clusters;
				
		List<FL_Cluster> newClusters = new ArrayList<FL_Cluster>(clusters.size());
		
		for (FL_Cluster cluster : clusters) {
			String id = cluster.getUid();
			if (!context.clusters.containsKey(id)) {
				newClusters.add(cluster);
			}
		}
		return newClusters;
	}
	
	private Pair<List<FL_Entity>, List<FL_Cluster>> getImmutableChildren(List<FL_Cluster> clusters, String contextId) {		
		Pair<List<FL_Entity>, List<FL_Cluster>> children = new Pair<List<FL_Entity>, List<FL_Cluster>>();
		children.first = new ArrayList<FL_Entity>();
		children.second = new ArrayList<FL_Cluster>();
		
		if (clusters == null || clusters.isEmpty()) return children;
		
		PermitSet permits = new PermitSet();
		
		try {
			final ContextRead contextRO = _cache.getReadOnly(contextId, permits);
			
			if (contextRO != null) {
				for (FL_Cluster cluster : clusters) {
					children.first.addAll( contextRO.getEntities(cluster.getMembers()) );
					
					List<String> subClusterIds = TypedId.filterTypedIds(cluster.getSubclusters(), TypedId.CLUSTER);
					List<String> immutableClusterIds = TypedId.filterTypedIds(cluster.getSubclusters(), TypedId.ACCOUNT_OWNER);
					immutableClusterIds.addAll( TypedId.filterTypedIds(cluster.getSubclusters(), TypedId.CLUSTER_SUMMARY) );
					
					children.second.addAll( contextRO.getClusters(immutableClusterIds) );
					
					Pair<List<FL_Entity>, List<FL_Cluster>> subChildren = getImmutableChildren( 
																				contextRO.getClusters(subClusterIds), contextId);
					children.first.addAll(subChildren.first);
					children.second.addAll(subChildren.second);
				}
			}
			
		} finally {
			permits.revoke();
		}
		return children;
	}
	
	private Set<String> getLeafIds(
		List<String> ids, 
		String context
	) throws AvroRemoteException {
		
		if (ids == null || ids.isEmpty()) return Collections.emptySet();
		
		Set<String> lids = new HashSet<String>();

		lids.addAll(TypedId.filterTypedIds(ids, TypedId.ACCOUNT));
		lids.addAll(TypedId.filterTypedIds(ids, TypedId.ACCOUNT_OWNER));
		lids.addAll(TypedId.filterTypedIds(ids, TypedId.CLUSTER_SUMMARY));
		lids.addAll(getClusterLeafIds(TypedId.filterTypedIds(ids, TypedId.CLUSTER), context));
		
		return lids;
	}
	
	private Set<String> getClusterLeafIds(
		List<String> clusterIds, 
		String context
	) throws AvroRemoteException {
		
		if (clusterIds == null || clusterIds.isEmpty()) return Collections.emptySet();
		
		Set<String> lids = new HashSet<String>();

		List<FL_Cluster> toSearch = getClusters(clusterIds, context);
		
		for (FL_Cluster flc : toSearch) {
			lids.addAll(flc.getMembers());
			if (flc.getSubclusters()!= null && !flc.getSubclusters().isEmpty()) {
				lids.addAll(getLeafIds(flc.getSubclusters(), context));
			}
		}
		return lids;
	}
}
