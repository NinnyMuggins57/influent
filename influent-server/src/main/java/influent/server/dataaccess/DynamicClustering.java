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
import influent.idl.FL_SortBy;
import influent.server.clustering.ClusterContext;
import influent.server.clustering.EntityClusterer;
import influent.server.clustering.utils.ClusterContextCache;
import influent.server.clustering.utils.ContextRead;
import influent.server.clustering.utils.ContextReadWrite;
import influent.server.clustering.utils.EntityAggregatedLinks;
import influent.server.clustering.utils.EntityClusterFactory;
import influent.server.clustering.utils.PropertyManager;
import influent.server.clustering.utils.ClusterContextCache.PermitSet;
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
import java.util.UUID;

import org.apache.avro.AvroRemoteException;

import com.google.inject.Singleton;

/**
 * Dynamic clustering impl.
 * @author msavigny
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
		PropertyManager config,
		ClusterContextCache cache
	) throws ClassNotFoundException, SQLException {
	
		super(connectionPool, namespaceHandler, entityAccess, geocoding, clusterer, clusterFactory, config);
		
		_clusterer.init( new Object[] {_clusterFactory, _geoCoder, config} );
		
		_cache = cache;
	}
	
	@Override
	public String createContext() throws AvroRemoteException {
		String randContext = UUID.randomUUID().toString();
		
		PermitSet permits = new PermitSet();
		try {
			_cache.getReadWrite(randContext, permits).setContext(new ClusterContext());
		} finally {
			permits.revoke();
		}

		return randContext;
	}
	
	@Override
	public List<String> clusterEntitiesById(
		List<String> entityIds,
		String contextId, 
		String sessionId
	) throws AvroRemoteException {
		
		return clusterEntities(_entityAccess.getEntities(entityIds, FL_LevelOfDetail.SUMMARY),contextId,sessionId);
	}
	
	@Override
	public List<String> clusterEntities(
		List<FL_Entity> entities,
		String contextId, 
		String sessionId
	) throws AvroRemoteException {

		List<String> rootIds = new ArrayList<String>();
		
		PermitSet permits = new PermitSet();
		
		// fetch the current context
		try {			
			// can be relatively long time to hold a lock on the context but not much to be done about it.
			ContextReadWrite contextRW = _cache.getReadWrite(contextId, permits);
			ClusterContext context = contextRW.getContext();
					
			List<FL_Entity> clusterEntities = entities;
			
			// if there is an existing context then filter out all entities that already belong to this context
			if (context != null) {
				clusterEntities = new ArrayList<FL_Entity>(entities.size());
				
				for (FL_Entity entity : entities) {
					if (!context.entities.containsKey(entity.getUid())) {
						clusterEntities.add(entity);
					}
				}
			}
			
			// nothing to do - return
			if (clusterEntities.isEmpty()) return rootIds;
			
			ClusterContext updatedContext = _clusterer.clusterEntities(clusterEntities, context);
					
			rootIds.addAll(updatedContext.roots.keySet());
			
			contextRW.setContext(updatedContext);
			
		} finally {
			permits.revoke();
		}
		
		return rootIds;
	}
	
	@Override
	public List<FL_Cluster> getClusters(
		List<String> entities, 
		String contextId, 
		String sessionId
	) throws AvroRemoteException {
		
		List<FL_Cluster> results = new ArrayList<FL_Cluster>();
		
		PermitSet permits = new PermitSet();
		
		// fetch the current context
		try {			
			final ContextRead contextRO = _cache.getReadOnly(contextId, permits);
			
			if (contextRO != null) {
				final ClusterContext context = contextRO.getContext();
				
				if (context != null) {
					for (String id : entities) {
						FL_Cluster c = context.clusters.get(id);
						if (c!= null) {
							results.add(c);
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
	public long removeMembers(List<String> entities, String contextId, String sessionId)
			throws AvroRemoteException {

		int count = 0;
		
		PermitSet permits = new PermitSet();
		
		try {
			ContextReadWrite contextRW = _cache.getReadWrite(contextId, permits);
			ClusterContext context = contextRW.getContext();
			if (context != null) {			
				for (String id : entities) {
					FL_Cluster c = context.clusters.get(id);
					if (c!= null) {
						context.clusters.remove(id);
						count++;
						
						// remove all entities from the context that were members of this cluster
						for (String eId : c.getMembers()) {
							context.entities.remove(eId);
						}
						
						// remove all sub-clusters from the context too!
						count += removeMembers(c.getSubclusters(), contextId, sessionId);
					}
				}
			}
			
		} finally {
			permits.revoke();
		}
		
		return count;
	}
	
	@Override
	public List<FL_Cluster> getContext(
		String contextId, 
		String sessionId,
		boolean computeSummaries
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
		String contextId, 
		String sessionId
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
		List<String> entities,
		List<String> focusEntities, 
		FL_DirectionFilter direction,
		FL_LinkTag tag, 
		FL_DateRange date, 
		String entitiesContextId,
		String focusContextId, 
		String sessionId
	) throws AvroRemoteException {
		
		try {
			if (focusEntities == null || focusEntities.isEmpty()) {
				Map<String, List<FL_Link>> relatedLinks = EntityAggregatedLinks.getRelatedAggregatedLinks(entities, direction, tag, date, _entityAccess, true, null,this,this,entitiesContextId, focusContextId, sessionId);
				return relatedLinks;
			} else {
				Map<String, List<FL_Link>> relatedLinks = EntityAggregatedLinks.getAggregatedLinks(entities, focusEntities, direction, tag, date, _entityAccess, this, entitiesContextId, focusContextId, sessionId);
				return relatedLinks;
			}
		} catch (DataAccessException e) {
			throw new AvroRemoteException(e);
		}
	}
	
	@Override
	public Map<String, List<FL_Link>> getTimeSeriesAggregation(
		List<String> entities, 
		List<String> focusEntities, 
		FL_LinkTag tag,
		FL_DateRange date, 
		String entitiesContextId, 
		String focusContextId, 
		String sessionId
	) throws AvroRemoteException {

		//Find leaf nodes for focii, and keep track of leaf id ->original cluster id
		
		// TODO this should handle Cluster Summaries and Account Owners?
		
		Map<String,String> fociiIdMap = new HashMap<String, String>();
		List<String> fociiList = new ArrayList<String>();
		for (String focusid : focusEntities) {
			Set<String> flids = getLeafIds(Collections.singletonList(focusid), focusContextId, sessionId);
			fociiList.addAll(flids);
			for (String fli : flids) {
				fociiIdMap.put(fli, focusid);
			}
		}
		
		//Find leaf nodes for entities, and keep track of leaf id ->original cluster id
		Map<String,String> entIdMap = new HashMap<String, String>();
		List<String> leafList = new ArrayList<String>();
		for (String entid : entities) {
			Set<String> elids = getLeafIds(Collections.singletonList(entid), entitiesContextId, sessionId);
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
		List<String> entities,
		FL_LinkTag tag, 
		FL_DateRange date, 
		FL_SortBy sort,
		List<String> linkFilter, 
		long max, 
		String contextId, 
		String sessionId
	) throws AvroRemoteException {
		
		return null;
	}
	
	private Set<String> getLeafIds(
		List<String> ids, 
		String context, 
		String sessionId
	) throws AvroRemoteException {
		
		if (ids == null || ids.isEmpty()) return Collections.emptySet();
		
		Set<String> lids = new HashSet<String>();

		lids.addAll(TypedId.filterTypedIds(ids, TypedId.ACCOUNT));
		lids.addAll(TypedId.filterTypedIds(ids, TypedId.ACCOUNT_OWNER));
		lids.addAll(TypedId.filterTypedIds(ids, TypedId.CLUSTER_SUMMARY));
		lids.addAll(getClusterLeafIds(TypedId.filterTypedIds(ids, TypedId.CLUSTER), context, sessionId));
		
		return lids;
	}
	
	private Set<String> getClusterLeafIds(
		List<String> clusterIds, 
		String context, 
		String sessionId
	) throws AvroRemoteException {
		
		if (clusterIds == null || clusterIds.isEmpty()) return Collections.emptySet();
		
		Set<String> lids = new HashSet<String>();

		List<FL_Cluster> toSearch = getClusters(clusterIds, context, sessionId);
		
		for (FL_Cluster flc : toSearch) {
			lids.addAll(flc.getMembers());
			if (flc.getSubclusters()!= null && !flc.getSubclusters().isEmpty()) {
				lids.addAll(getClusterLeafIds(flc.getSubclusters(), context, sessionId));
			}
		}
		
		return lids;
	}
}
