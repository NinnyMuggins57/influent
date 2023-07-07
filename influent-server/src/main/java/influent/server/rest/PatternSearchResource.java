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

import influent.idl.FL_BoundedRange;
import influent.idl.FL_Cluster;
import influent.idl.FL_ClusteringDataAccess;
import influent.idl.FL_DataAccess;
import influent.idl.FL_Entity;
import influent.idl.FL_EntityMatchDescriptor;
import influent.idl.FL_EntityMatchResult;
import influent.idl.FL_EntityTag;
import influent.idl.FL_Future;
import influent.idl.FL_LevelOfDetail;
import influent.idl.FL_LinkMatchResult;
import influent.idl.FL_PatternDescriptor;
import influent.idl.FL_PatternSearch;
import influent.idl.FL_PatternSearchResult;
import influent.idl.FL_PatternSearchResults;
import influent.idl.FL_Property;
import influent.idl.FL_PropertyTag;
import influent.idl.FL_PropertyType;
import influent.idlhelper.ClusterHelper;
import influent.idlhelper.EntityHelper;
import influent.idlhelper.PropertyHelper;
import influent.idlhelper.SerializationHelper;
import influent.server.clustering.utils.EntityClusterFactory;
import influent.server.utilities.AvroUtils;
import influent.server.utilities.DateTimeParser;
import influent.server.utilities.GuidValidator;
import influent.server.utilities.TypedId;
import influent.server.utilities.UISerializationHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import oculus.aperture.common.rest.ApertureServerResource;

import org.apache.avro.AvroRemoteException;
import org.joda.time.DateTime;
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


public class PatternSearchResource extends ApertureServerResource{

	private final FL_PatternSearch patternSearcher;
	private final FL_DataAccess dataAccess;
	private final FL_ClusteringDataAccess clusterAccess;
	private final EntityClusterFactory clusterFactory;
	
	protected final static int DEFAULT_MAX_LIMIT = 50;
	
	private static final Logger s_logger = LoggerFactory.getLogger(EntitySearchResource.class);
	
	
	
	@Inject
	public PatternSearchResource(
		FL_PatternSearch patternSearcher, 
		FL_ClusteringDataAccess clusterAccess,
		EntityClusterFactory clusterFactory,
		FL_DataAccess dataAccess
	) {
		this.patternSearcher = patternSearcher;
		this.clusterAccess = clusterAccess;
		this.clusterFactory = clusterFactory;
		this.dataAccess = dataAccess;
	}
	
	
	
	
	@Post("json")
	public StringRepresentation search(String jsonData) throws ResourceException {
		JSONObject jsonObj;

		try {
			jsonObj = new JSONObject(jsonData);
			
			String sessionId = jsonObj.getString("sessionId").trim();
			if (!GuidValidator.validateGuidString(sessionId)) {
				throw new ResourceException(Status.CLIENT_ERROR_EXPECTATION_FAILED, "sessionId is not a valid UUID");
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
			
			boolean useAptima = false;
			if (jsonObj.has("useAptima")){
				useAptima = jsonObj.getBoolean("useAptima");
			}

			// if date range is supplied, use it.
			FL_BoundedRange dateRange = null;
			DateTime startDate = null;
			DateTime endDate = null;
			
			try {
				startDate = DateTimeParser.parse(jsonObj.getString("startDate"));
			} catch (JSONException e) {
			}
			try {
				endDate = DateTimeParser.parse(jsonObj.getString("endDate"));
			} catch (JSONException e) {
			}
			
			if (startDate != null || endDate != null) {
				FL_BoundedRange.Builder builder= FL_BoundedRange.newBuilder();
				builder.setStart(startDate.getMillis());
				builder.setEnd(endDate.getMillis());
				builder.setInclusive(true);
				builder.setType(FL_PropertyType.DATE);
				dateRange = builder.build();
			}

			
			// get the search term
			String term = jsonObj.getString("term").trim();
			FL_PatternDescriptor example = (FL_PatternDescriptor)AvroUtils.decodeJSON(FL_PatternDescriptor.getClassSchema(), term);
			
			Map<String, Character> searchEntityTypeMap = new HashMap<String, Character>();
			Map<String, String> searchEntityNamespaceMap = new HashMap<String, String>();
			boolean hasExamplars = true;
			
			List<FL_EntityMatchDescriptor> exampleEntities = example.getEntities();
			Map<String, String> hackMap = new HashMap<String, String>(exampleEntities.size());
			for (int i = 0; i < exampleEntities.size(); i++) {
				final FL_EntityMatchDescriptor emd = exampleEntities.get(i);
				hackMap.put("E" + i, emd.getUid());

				List<String> ids = new ArrayList<String>(emd.getExamplars().size());

				// get native entity id refs from globals and substitute
				for (String uid : emd.getExamplars()) {
					String nid = TypedId.fromTypedId(uid).getNativeId();
					
					if (nid == null) {
						continue;
					}
					
					ids.add(nid);

					// Assume that we're looking for entities of a similar type 
					// in same namespace as the first entity in the list
					if (searchEntityTypeMap.get(emd.getUid()) == null) {
						searchEntityTypeMap.put(emd.getUid(), TypedId.fromTypedId(uid).getType());
						searchEntityNamespaceMap.put(emd.getUid(), TypedId.fromTypedId(uid).getNamespace());
					}
				}
				
				if (ids.size() == 0)
					hasExamplars = false;
				
				emd.setExamplars(ids);
			}

			Object response = null;
			
			if (!hasExamplars) {
				// If we failed to pick up any exemplars, it probably means that the search 
				// terms are malformed. don't do the query, just return empty results
				s_logger.warn("Invalid search terms. Not executing pattern search.");
			} else {
				s_logger.info("Executing pattern search: "+ SerializationHelper.toJson(example));
				
				try {
					response = patternSearcher.searchByExample(example, "QuBE", (long)startIndex, (long)resultLimit, dateRange, useAptima);
				} catch (AvroRemoteException are) {
					// FIXME: Temporarily suppressing QuBE errors until version is updated (#7758)
					//throw new RuntimeException("Error reported by Query by Example. ", are);
				}
			}
			
			
			final FL_PatternSearchResults searchResults;
			
			if (response instanceof FL_Future) {
				// this will block, even though is a potentially long-running analytic
				// TODO: async API
				searchResults = patternSearcher.getResults((FL_Future) response);
				
			} else if (response instanceof FL_PatternSearchResults) {
				searchResults = (FL_PatternSearchResults)response;
				
			} else {
				searchResults = FL_PatternSearchResults.newBuilder()
					.setTotal(0).setResults(Collections.<FL_PatternSearchResult> emptyList())
					.build();
			}
			
			
			final Map<String, List<FL_EntityMatchResult>> roleResults = new LinkedHashMap<String, List<FL_EntityMatchResult>>();

			// compile lists by role
			for(FL_EntityMatchDescriptor exampleEntity : example.getEntities()) {
				roleResults.put(exampleEntity.getUid(), new ArrayList<FL_EntityMatchResult>());
			}
			
			
			final Map<String, List<FL_EntityMatchResult>> entityInstances= new HashMap<String, List<FL_EntityMatchResult>>();
			
			final StringBuilder trace = new StringBuilder("----Results----\n      ");
			
			// Create the tailored client results, which is a merge of all graph results
			for (FL_PatternSearchResult searchResult : searchResults.getResults()) {

				// gather a set of all entity ids to lookup
				for (FL_EntityMatchResult entityResult : searchResult.getEntities()) {
					String nId = entityResult.getEntity().getUid().toString();
					trace.append(" ");
					trace.append(nId);

					String resultUid = entityResult.getUid();
					
					// The returned UID may be an alias or a fully qualified id. 
					// If it's an alias then map it back to an id. 
					if (hackMap.containsKey(resultUid)) {
						resultUid = hackMap.get(resultUid);
					} else if (!hackMap.containsValue(resultUid)) {
						s_logger.warn("Result UID of: " + resultUid + " does not have a matching file ID");
						continue;
					}
					
					// Calculate the actual entity id by assuming it matches the searched-for type/namespace
					char entityType = searchEntityTypeMap.get(resultUid);
					String entityNamespace = searchEntityNamespaceMap.get(resultUid);
					String entityId = TypedId.fromNativeId(entityType, entityNamespace, nId).getTypedId();
					
					// Put our new entityId back into the results set
					entityResult.getEntity().setUid(entityId);
	
					// Fix any links to this entity
					for (FL_LinkMatchResult link : searchResult.getLinks()) {
						if (link.getLink().getSource().equals(nId)) {
							link.getLink().setSource(entityId);
						}
							
						if (link.getLink().getTarget().equals(nId)) {
							link.getLink().setTarget(entityId);
						}
					}
				
					
					// find the role back
					List<FL_EntityMatchResult> roleMatches = roleResults.get(resultUid);
					
					if (roleMatches != null) {
						int i;
						
						// look for duplicate, and if found add to cumulative score
						for (i=0; i < roleMatches.size(); i++) {
							if (roleMatches.get(i).getEntity().getUid().toString().equals(entityId)) {
								roleMatches.get(i).setScore(roleMatches.get(i).getScore() + entityResult.getScore());
								break;
							}
						}
						
						// if didn't find it, create a new clone for enrichment
						if (i == roleMatches.size()) {
							roleMatches.add(entityResult = FL_EntityMatchResult.newBuilder(entityResult).build());
							
							// keep index of all enriched entity instances by id so we can slot in full entity details later.
							List<FL_EntityMatchResult> eis = entityInstances.get(entityId);
							
							if (eis == null) {
								eis = new ArrayList<FL_EntityMatchResult>();
								entityInstances.put(entityId, eis);
							}
							
							eis.add(entityResult);
						}
					}
				}
				
				trace.append("\n      ----\n      ");
			}
			
			s_logger.info(trace.toString());

			
			// look up entity details
			final List<FL_Entity> searchedEntities = dataAccess.getEntities(new ArrayList<String>(entityInstances.keySet()), FL_LevelOfDetail.SUMMARY);
			
			Map<String, String> clusterSummaries = new HashMap<String, String>();
			List<String> accountOwners = new ArrayList<String>();
			
			for (FL_Entity entity : searchedEntities) {
				
				// Plug the searched entities back into the enrichment set
				for (Map.Entry<String,List<FL_EntityMatchResult>> entry : roleResults.entrySet()) {
					
					for (FL_EntityMatchResult emr : entry.getValue()) {
						String tempNId = TypedId.fromTypedId(emr.getEntity().getUid()).getNativeId();
						String searchedNId = TypedId.fromTypedId(entity.getUid()).getNativeId();
						
						if (tempNId.equals(searchedNId)) {
							emr.setEntity(entity);
						}
					}
				}
				
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
			
			Map<String, FL_Cluster> summaries = new HashMap<String, FL_Cluster>();
			Map<String, FL_Cluster> owners = new HashMap<String, FL_Cluster>();
			Map<String, FL_Entity> entities = new HashMap<String, FL_Entity>();

			// fetch all accounts for account owners
			Map<String, List<FL_Entity>> accountsForAccountOwners = dataAccess.getAccounts(accountOwners);
			
			// fetch all cluster summaries, indexed by owner
			for (FL_Cluster clusterSummary : clusterAccess.getClusterSummary(new ArrayList<String>(clusterSummaries.values()))) {
				String ownerId = (String)ClusterHelper.getFirstPropertyByTag(clusterSummary, FL_PropertyTag.ACCOUNT_OWNER).getValue();

				summaries.put(ownerId, clusterSummary);
			}
			
			// Ok, now process the entities.  If the entity key is in the accounts map, construct account owner, otherwise use the entity
			for (FL_Entity entity : searchedEntities) {

				// If this is an account owner then construct an account owner cluster
				if (accountsForAccountOwners.containsKey(entity.getUid())) {
					List<FL_Entity> accounts = accountsForAccountOwners.get(entity.getUid());
					if (accounts != null) {
						FL_Cluster owner = clusterFactory.toAccountOwnerSummary(entity, accounts, new ArrayList<FL_Cluster>(0));
						owners.put(owner.getUid(), owner);
					}
				} else if (!clusterSummaries.containsKey(entity.getUid())) {  // if not a cluster summary then add entity
					entities.put(entity.getUid(), entity);
				}
					
			}
			
			JSONArray jsonRoleResults = new JSONArray();
			JSONObject jo;

			// Build the JSON results by Role and entity
			for (Map.Entry<String,List<FL_EntityMatchResult>> entry : roleResults.entrySet()) {
				JSONArray jsonRoleResultSet = new JSONArray();
				
				for (FL_EntityMatchResult emr : entry.getValue()) {
					String Uid = emr.getEntity().getUid();

					try {
						if (entities.containsKey(Uid)) {
							Object obj = entities.get(Uid); 
							
							if (obj instanceof FL_Cluster) {
								FL_Cluster cluster = (FL_Cluster)obj;
								jo = UISerializationHelper.toUIJson(cluster);
								jsonRoleResultSet.put(jo);
							} else {
								FL_Entity entity = (FL_Entity)obj;
								jo = UISerializationHelper.toUIJson(entity);
							}
							
							jsonRoleResultSet.put(jo);
						}
	
						if (owners.containsKey(Uid)) {
							FL_Cluster owner = owners.get(Uid); 
							jo = UISerializationHelper.toUIJson(owner);
							jsonRoleResultSet.put(jo);
						}
						
						if (summaries.containsKey(Uid)) {
							FL_Cluster owner = summaries.get(Uid); 
							jo = UISerializationHelper.toUIJson(owner);
							jsonRoleResultSet.put(jo);
						}	
						
					} catch (NullPointerException npe) {
						s_logger.error("Error serializing entity with uid: " + Uid + ".   Omitting from results.");
						continue;
					} 
				}
				
				JSONObject jsonRoleResult = new JSONObject();
				jsonRoleResult.put("uid", entry.getKey());
				jsonRoleResult.put("results", jsonRoleResultSet);
				jsonRoleResults.put(jsonRoleResult);
			}
				
			JSONObject result = new JSONObject();
			result.put("roleResults", jsonRoleResults);
			result.put("graphResults", AvroUtils.encodeJSON(searchResults));
			result.put("totalResults", searchResults.getTotal());
			result.put("sessionId", sessionId);

			return new StringRepresentation(result.toString(),MediaType.APPLICATION_JSON);
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
		} catch (Exception e) {
			throw new ResourceException(
				Status.CLIENT_ERROR_BAD_REQUEST,
				"Exception during pattern descriptor processing",
				e
			);
		}
		
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
