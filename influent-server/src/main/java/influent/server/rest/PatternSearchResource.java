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
import influent.idl.FL_Clustering;
import influent.idl.FL_ClusteringDataAccess;
import influent.idl.FL_DataAccess;
import influent.idl.FL_Entity;
import influent.idl.FL_EntityMatchDescriptor;
import influent.idl.FL_EntityMatchResult;
import influent.idl.FL_Future;
import influent.idl.FL_LevelOfDetail;
import influent.idl.FL_PatternDescriptor;
import influent.idl.FL_PatternSearch;
import influent.idl.FL_PatternSearchResult;
import influent.idl.FL_PatternSearchResults;
import influent.idl.FL_PropertyType;
import influent.idlhelper.SerializationHelper;
import influent.server.utilities.AvroUtils;
import influent.server.utilities.DateTimeParser;
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

import com.google.inject.Inject;



public class PatternSearchResource extends ApertureServerResource{

//	private static final Logger s_logger = LoggerFactory.getLogger(PatternSearchResource.class);
	
	private final FL_PatternSearch patternSearcher;
	private final FL_DataAccess dataAccess;
//	private final FL_Clustering clusterer;
//	private final FL_ClusteringDataAccess clusterAccess;
	
	protected final static int DEFAULT_MAX_LIMIT = 50;
	
	@Inject
	public PatternSearchResource(
		FL_PatternSearch patternSearcher, 
		FL_Clustering cluster, 
		FL_ClusteringDataAccess clusterAccess,
		FL_DataAccess dataAccess
	) {
		this.patternSearcher = patternSearcher;
//		this.clusterer = cluster;
//		this.clusterAccess = clusterAccess;
		this.dataAccess = dataAccess;
	}
	
	@Post("json")
	public StringRepresentation search(String jsonData) throws ResourceException {
		JSONObject jsonObj;

		try {
			jsonObj = new JSONObject(jsonData);
			
			String sessionId = jsonObj.getString("sessionId").trim();

			// Get the query id. This is used by the client to ensure
			// it only processes the latest response.
			String queryId = jsonObj.getString("queryId").trim();

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
			
			List<FL_EntityMatchDescriptor> exampleEntities = example.getEntities();
			Map<String, String> hackMap = new HashMap<String, String>(exampleEntities.size());
			for (int i = 0; i < exampleEntities.size(); i++) {
				final FL_EntityMatchDescriptor emd = exampleEntities.get(i);
				hackMap.put("E" + i, emd.getUid());

				List<String> ids = new ArrayList<String>(emd.getExamplars().size());

				// get native entity id refs from globals and substitute
				for (String uid : emd.getExamplars()) {
					String nid = TypedId.fromTypedId(uid).getNativeId();
					if (nid != null) {
						ids.add(nid);
					}
				}
				
				emd.setExamplars(ids);
			}

			getLogger().info("Executing pattern search: "+ SerializationHelper.toJson(example));
			
			Object response = null;
			
			try {
				response = patternSearcher.searchByExample(example, "QuBE", (long)startIndex, (long)resultLimit, dateRange, useAptima);
			} catch (AvroRemoteException are) {
				getLogger().severe(are.getMessage());
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
					String entityId = entityResult.getEntity().getUid().toString();

					trace.append(" ");
					trace.append(entityId);
					
					entityId = TypedId.fromNativeId(TypedId.ACCOUNT, entityId).getTypedId();
					
					String resultUid = entityResult.getUid();
					if (hackMap.containsKey(resultUid)) {
						resultUid = hackMap.get(resultUid);
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
								entityInstances.put(entityId, eis = new ArrayList<FL_EntityMatchResult>());
							}
							
							eis.add(entityResult);
						}
					}
				}
				
				trace.append("\n      ----\n      ");
			}
			
			getLogger().info(trace.toString());
			

			// look up entity details
			final List<FL_Entity> entities = dataAccess.getEntities(new ArrayList<String>(entityInstances.keySet()), FL_LevelOfDetail.SUMMARY);
			
			// and slot them into our enrichment set
			for (FL_Entity entity : entities) {
				List<FL_EntityMatchResult> instances= entityInstances.get(entity.getUid());
				for (FL_EntityMatchResult emr : instances) {
					emr.setEntity(entity);
				}
			}
			
			JSONObject result = new JSONObject();
			JSONArray jsonRoleResults = new JSONArray();
			
			for (Map.Entry<String,List<FL_EntityMatchResult>> entry : roleResults.entrySet()) {
				JSONArray jsonRoleResultSet = new JSONArray();
				
				for (FL_EntityMatchResult emr : entry.getValue()) {
					JSONObject jo = UISerializationHelper.toUIJson(emr.getEntity()); 
					jsonRoleResultSet.put(jo);
				}
				
				JSONObject jsonRoleResult = new JSONObject();
				jsonRoleResult.put("uid", entry.getKey());
				jsonRoleResult.put("results", jsonRoleResultSet);
				jsonRoleResults.put(jsonRoleResult);
			}
			
			result.put("roleResults", jsonRoleResults);
			result.put("graphResults", AvroUtils.encodeJSON(searchResults));
			result.put("totalResults", searchResults.getTotal());
			result.put("queryId", queryId);
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
