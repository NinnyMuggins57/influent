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
package influent.server.rest;

import influent.idl.FL_Cluster;
import influent.idl.FL_Clustering;
import influent.idl.FL_ClusteringDataAccess;
import influent.idl.FL_DataAccess;
import influent.idl.FL_Entity;
import influent.idl.FL_EntitySearch;
import influent.idl.FL_EntityTag;
import influent.idl.FL_GeoData;
import influent.idl.FL_Property;
import influent.idl.FL_PropertyTag;
import influent.idl.FL_SearchResult;
import influent.idl.FL_SearchResults;
import influent.idlhelper.EntityHelper;
import influent.idlhelper.PropertyHelper;
import influent.server.clustering.utils.ContextCollapser;
import influent.server.clustering.utils.ClusterContextCache;
import influent.server.clustering.utils.ClusterContextCache.PermitSet;
import influent.server.clustering.utils.ContextReadWrite;
import influent.server.clustering.utils.EntityClusterFactory;
import influent.server.data.EntitySearchTerms;
import influent.server.utilities.GuidValidator;
import influent.server.utilities.Pair;
import influent.server.utilities.TypedId;
import influent.server.utilities.UISerializationHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import oculus.aperture.common.rest.ApertureServerResource;

import org.apache.avro.AvroRemoteException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;



public class EntitySearchResource extends ApertureServerResource{

	private final FL_DataAccess entityAccess;
	private final FL_EntitySearch entitySearcher;
	private final FL_Clustering clusterer;
	private final FL_ClusteringDataAccess clusterAccess;
	private final EntityClusterFactory clusterFactory;
	private final ClusterContextCache contextCache;

	protected final static int DEFAULT_MAX_LIMIT = 50;
	
	private static final Logger s_logger = LoggerFactory.getLogger(EntitySearchResource.class);
	
	
	
	@Inject
	public EntitySearchResource(FL_DataAccess entityAccess, 
								FL_EntitySearch entitySearcher, 
								FL_Clustering cluster, 
								FL_ClusteringDataAccess clusterAccess, 
								EntityClusterFactory clusterFactory,
								ClusterContextCache contextCache) {
		this.entityAccess = entityAccess;
		this.entitySearcher = entitySearcher;
		this.clusterer = cluster;
		this.clusterAccess = clusterAccess;	
		this.clusterFactory = clusterFactory;
		this.contextCache = contextCache;
	}
	
	
	
	
	@Post("json")
	public StringRepresentation search(String jsonData) throws ResourceException {
		JSONObject jsonObj;
		Map<String, Double> scores = new HashMap<String, Double>();
		List<FL_Entity> entities = new ArrayList<FL_Entity>();
		List<FL_Cluster> summaries = new ArrayList<FL_Cluster>();
		List<FL_Cluster> owners = new ArrayList<FL_Cluster>();
		
		try {
			jsonObj = new JSONObject(jsonData);
			
			String sessionId = jsonObj.getString("sessionId").trim();
			if (!GuidValidator.validateGuidString(sessionId)) {
				throw new ResourceException(Status.CLIENT_ERROR_EXPECTATION_FAILED, "sessionId is not a valid UUID");
			}
			
			// Flag to indicate whether or not to cluster the results.
			boolean doCluster = true;
			
			if (jsonObj.has("cluster")) {
				doCluster = jsonObj.getBoolean("cluster");
			}
						
			String groupByTagOrFieldName = null;
			
			if (jsonObj.has("groupby")) {
				groupByTagOrFieldName = jsonObj.getString("groupby");
			}
			
			String contextId = "UNKNOWN-CONTEXT";
			if (jsonObj.has("contextId")) {
				contextId = jsonObj.getString("contextId").trim();
			}

			// Determine the number of results to return.
			int resultLimit = DEFAULT_MAX_LIMIT;
			if (jsonObj.has("limit")){
				resultLimit = jsonObj.getInt("limit") > 0?jsonObj.getInt("limit"):resultLimit;
			}

			// Determine the start index.
			int startIndex = 0;
			if (jsonObj.has("start")){
				startIndex = jsonObj.getInt("start");
			}
			
			// get the search term
			final String term = jsonObj.getString("term").trim();
			
			final EntitySearchTerms terms = new EntitySearchTerms(term);
			
			// get the data type
			final String type = terms.getType();
			
			if (terms.doCluster() != null) {
				doCluster = terms.doCluster();
			}
			
			FL_SearchResults sResponse = entitySearcher.search(terms.getExtraTerms(), terms.getTerms(), (long)startIndex, (long)resultLimit, type);
			
			// TODO modify below code to use Cluster data access getAccountOwners() API method 
			
			Map<String, String> clusterSummaries = new HashMap<String, String>();
			List<String> accountOwners = new ArrayList<String>();
			
			for (FL_SearchResult sResult : sResponse.getResults()){
				FL_Entity entity = (FL_Entity)sResult.getResult();

				// If this is an account owner either we retrieve a cluster summary or all accounts
				if (entity.getTags().contains(FL_EntityTag.ACCOUNT_OWNER)) {
					FL_Property summary = EntityHelper.getFirstPropertyByTag(entity, FL_PropertyTag.CLUSTER_SUMMARY);
					
					if (summary != null) {
						// retrieve the cluster summary for this account owner
						clusterSummaries.put(entity.getUid(), (String)PropertyHelper.from(summary).getValue());
					} else {
						// retrieve the accounts for account owner
						accountOwners.add(entity.getUid());
					}
				}
			}

			// fetch all accounts for account owners
			Map<String, List<FL_Entity>> accountsForAccountOwners = entityAccess.getAccounts(accountOwners);
			
			Map<String, List<FL_Entity>> groupedEntities = new HashMap<String, List<FL_Entity>>();
			
			// fetch all cluster summaries
			summaries.addAll( clusterAccess.getClusterSummary(new ArrayList<String>(clusterSummaries.values())) );
			
			PermitSet permits = new PermitSet();
			
			try {
				// Ok, now process the entities.  If the entity key is in the accounts map, construct account owner, otherwise use the entity
				for (FL_SearchResult sResult : sResponse.getResults()) {
					FL_Entity entity = (FL_Entity)sResult.getResult();
	
					// If this is an account owner then construct an account owner cluster
					if (accountsForAccountOwners.containsKey(entity.getUid())) {
						List<FL_Entity> accounts = accountsForAccountOwners.get(entity.getUid());
						if (accounts != null) {
							FL_Cluster owner = clusterFactory.toAccountOwnerSummary(entity, accounts, new ArrayList<FL_Cluster>(0));
							owners.add(owner);
						}
					} else if (!clusterSummaries.containsKey(entity.getUid())) {  // if not a cluster summary then add entity
						entities.add(entity);
					}
				}
				
				// group entities if a grouping field was provided
				if (groupByTagOrFieldName != null) {
					for (FL_Entity entity : entities) {
						PropertyHelper prop = getFirstProperty(entity, groupByTagOrFieldName);
						if (prop != null) {
							Object val = prop.getValue();
							String key = null;
							
							if (val instanceof FL_GeoData) {
								key = ((FL_GeoData)val).getCc();
							} else if (val instanceof String) {
								key = (String)val;
							}
							
							List<FL_Entity> group = groupedEntities.get(key);
							if (group == null) {
								group = new ArrayList<FL_Entity>();
								groupedEntities.put(key, group);
							}
							
							group.add(entity);
						}
					}
				}
	
				normalizeScores(scores);
				
				// Perform clustering is required - only support grouping XOR clustering
				if (groupByTagOrFieldName == null && doCluster && entities.size()>4){
				
					final ContextReadWrite contextRW = contextCache.getReadWrite(contextId, permits);
					
					clusterAccess.clearContext(contextId);
					
					/*List<String> clusterIds =*/ clusterer.clusterEntities(entities, null, null, contextId);
					
					Pair<Collection<FL_Entity>,Collection<FL_Cluster>> simpleContext = ContextCollapser.collapse(contextRW,false,null);
	
					JSONObject result = new JSONObject();
					JSONArray rArr = new JSONArray();
					List<String> fetchFullClusters = new ArrayList<String>();
	
					contextRW.setSimplifiedContext(simpleContext.second);
					
					for (FL_Cluster cluster : simpleContext.second) {
						if (cluster.getParent() == null) {
							fetchFullClusters.add(cluster.getUid());
						}
					}
					if (!fetchFullClusters.isEmpty()) {
						List<FL_Cluster> topClusters = clusterAccess.getClusters(fetchFullClusters, contextId);
						contextRW.setSimplifiedContext(topClusters);
	
						//Re-insert the clusters with the computed summaries
						for (String cid : fetchFullClusters) {
							rArr.put(UISerializationHelper.toUIJson(contextRW.getCluster(cid)));
						}
					}
					
					for (FL_Entity entity : entities) {
						if (simpleContext.first.contains(entity.getUid())) {
							rArr.put(UISerializationHelper.toUIJson(entity));
						}
					}
					
					result.put("data",rArr);
					result.put("totalResults",sResponse.getTotal());
					result.put("sessionId", sessionId);
					
					StringRepresentation responseSR = new StringRepresentation(result.toString(),MediaType.APPLICATION_JSON);
					
					return responseSR;
				}
				else {
					JSONObject result = new JSONObject();
					
					JSONArray ja = new JSONArray();
				
					List<Object> entityGroups = new LinkedList<Object>();
					
					// initially add all the entities to the entityGroups
					entityGroups.addAll(entities);
					
					// replace entities with group clusters as necessary
					for (String key : groupedEntities.keySet()) {
						final List<FL_Entity> group = groupedEntities.get(key);
					
						if (group.size() < 2) continue;
						
						// create cluster for the group
						FL_Cluster cluster = clusterFactory.toCluster(group, new ArrayList<FL_Cluster>(0));
						
						// HACK for now the cluster id is a concatenation of it's child ids
						StringBuilder str = new StringBuilder();
						
						for (FL_Entity entity : group) {
							str.append("|" + entity.getUid());
						}
						cluster.setUid( TypedId.fromNativeId(TypedId.CLUSTER, str.toString()).getTypedId() );
	
						// find the index of the first entity in the group - the cluster will be placed at this location in the results
						int idx = entityGroups.indexOf(group.get(0));
						
						// remove the group from the entities result set - replace with a cluster
						entityGroups.removeAll(group);
						entityGroups.add(idx, cluster);
					}
					
					// add the found entities and group clusters to result set
					for (Object obj : entityGroups) {
						JSONObject jo;
						String id = null;
						try {
							if (obj instanceof FL_Cluster) {
								FL_Cluster cluster = (FL_Cluster)obj;
								id = cluster.getUid();
								jo = UISerializationHelper.toUIJson(cluster);
							} else {
								FL_Entity entity = (FL_Entity)obj;
								id = entity.getUid();
								jo = UISerializationHelper.toUIJson(entity);
							}
						} catch (NullPointerException npe) {
							s_logger.error("Error serializing object with uid: " + id + ".   Omitting from results.");
							continue;
						}
						ja.put(jo);
					}
					
					// add the found owners to result set
					for (FL_Cluster owner : owners) {
						JSONObject jo;
						try {
							jo = UISerializationHelper.toUIJson(owner);
						} catch (NullPointerException npe) {
							s_logger.error("Error serializing entity with uid: " + owner.getUid() + ".   Omitting from results.");
							continue;
						}
						ja.put(jo);
					}
					// add the found cluster summaries to result set
					for (FL_Cluster summary : summaries) {
						JSONObject jo;
						try {
							jo = UISerializationHelper.toUIJson(summary);
						} catch (NullPointerException npe) {
							s_logger.error("Error serializing entity with uid: " + summary.getUid() + ".   Omitting from results.");
							continue;
						}
						ja.put(jo);
					}
					
					result.put("data", ja);
					result.put("scores", scores);
					result.put("totalResults", sResponse.getTotal());
					result.put("sessionId", sessionId);
	
					return new StringRepresentation(result.toString(),MediaType.APPLICATION_JSON);
	
				}
				
			} finally {
				permits.revoke();
			}
		}
		catch (JSONException e) {
			throw new ResourceException(
				Status.CLIENT_ERROR_BAD_REQUEST,
				"Unable to create JSON object from supplied options string",
				e
			);
		} catch (AvroRemoteException e) {
			throw new ResourceException(
					Status.CLIENT_ERROR_BAD_REQUEST,
					"Exception during AVRO processing",
					e
				);
		}
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
	
	
	
	
	public static void normalizeScores (Map<String, Double> scores) {
		double maxscore = 0;
		
		for (String entity : scores.keySet()) 
			if (scores.get(entity) > maxscore)
				maxscore = scores.get(entity);
		
		if (maxscore != 0)
			for (String entity : scores.keySet()) 
				scores.put(entity, scores.get(entity)/maxscore);
	}
}
