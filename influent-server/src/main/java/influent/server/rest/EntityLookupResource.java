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
import influent.idl.FL_ClusteringDataAccess;
import influent.idl.FL_DataAccess;
import influent.idl.FL_Entity;
import influent.idl.FL_LevelOfDetail;
import influent.server.clustering.utils.ClusterContextCache;
import influent.server.clustering.utils.ContextReadWrite;
import influent.server.clustering.utils.ClusterContextCache.PermitSet;
import influent.server.utilities.TypedId;
import influent.server.utilities.UISerializationHelper;

import java.util.ArrayList;
import java.util.List;

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

import com.google.inject.Inject;


public class EntityLookupResource extends ApertureServerResource{
	
	private final FL_DataAccess service;
	private final FL_ClusteringDataAccess clusterAccess;
	private final ClusterContextCache contextCache;

	private List<String> flattenEntityCluster(List<String> entityIds, String contextId, ContextReadWrite context, String sessionId, boolean isFlattened) throws AvroRemoteException{
		List<String> clusterIds = TypedId.filterTypedIds(entityIds, TypedId.CLUSTER);
		List<String> accountIds = TypedId.filterTypedIds(entityIds, TypedId.ACCOUNT);
		
		List<String> flatIdList = new ArrayList<String>();
		//First, check to see how many of the ids are clusters without summaries
		List<String> nosumClusters = context.getClusterIdsWithoutSummaries(clusterIds);
		
		if (!nosumClusters.isEmpty()) {
			List<FL_Cluster> computeClusterSums = clusterAccess.getClusters(clusterIds, contextId, sessionId);
			
			context.merge(computeClusterSums, true, false);
		}
		
		List<FL_Cluster> clusterResults = context.getClusters(clusterIds);
		// DJ: why is this here?
		List<FL_Entity> entityResults = service.getEntities(accountIds, FL_LevelOfDetail.SUMMARY);
		if (isFlattened){
			for (FL_Cluster cluster : clusterResults){
				List<String> childIds = cluster.getMembers();
				childIds.addAll(cluster.getSubclusters());
				flatIdList.addAll(flattenEntityCluster(childIds, contextId, context, sessionId, isFlattened));
			}
			for (FL_Entity entity : entityResults){
				flatIdList.add(entity.getUid());
			}			
		}

		return flatIdList;
	}
	
	@Inject
	public EntityLookupResource(FL_DataAccess service, FL_ClusteringDataAccess clusterDataAccess, ClusterContextCache contextCache) {
		this.service = service;
		clusterAccess = clusterDataAccess;
		this.contextCache = contextCache;
	}
	
	@Post("json")
	public StringRepresentation lookup(String jsonData) throws ResourceException {
		JSONObject jsonObj;
		JSONObject result = new JSONObject();
		try {
			jsonObj = new JSONObject(jsonData);
			
			String sessionId = jsonObj.getString("sessionId").trim();
			
			// Get the query id. This is used by the client to ensure
			// it only processes the latest response.
			String queryId = jsonObj.getString("queryId").trim();

			String contextid = jsonObj.getString("contextid").trim();
			
			Boolean isFlattened = jsonObj.has("isFlattened")?jsonObj.getBoolean("isFlattened"):false;
			Boolean details = jsonObj.has("details")? jsonObj.getBoolean("details"):false;
			
			// get the root node ID from the form
			JSONArray entityNodes = jsonObj.getJSONArray("entities");
			List<String> entityIds = new ArrayList<String>(entityNodes.length());
			
			//Tmp : old-esque clustering lookup
			
			for (int i=0; i < entityNodes.length(); i++){
				String id = entityNodes.getString(i).trim();
				entityIds.add(id);
			}

			List<String> clusterIds = TypedId.filterTypedIds(entityIds, TypedId.CLUSTER);
			List<String> accountIds = TypedId.filterTypedIds(entityIds, TypedId.ACCOUNT);
			
			PermitSet permits = new PermitSet();
			List<FL_Cluster> clusterResults = new ArrayList<FL_Cluster>();
			
			try {
				// technically there is only one write op, which is the merge calls, and don't like holding
				// this lock while calling getClusters but there are recursive merge calls in flatten cluster
				ContextReadWrite contextRW = contextCache.getReadWrite(contextid, permits);
				
				//First, check to see how many of the ids are clusters without summaries
				List<String> nosumClusters = contextRW.getClusterIdsWithoutSummaries(clusterIds);
				
				if (!nosumClusters.isEmpty()) {
					List<FL_Cluster> computeClusterSums = clusterAccess.getClusters(clusterIds, contextid, sessionId);

					contextRW.merge(computeClusterSums, true, false);
				}
			
				if (isFlattened){
					accountIds.addAll(flattenEntityCluster(clusterIds, contextid, contextRW, sessionId, isFlattened));
				}
				else {
					clusterResults.addAll(contextRW.getClusters(clusterIds));
				}
									
			} finally {
				permits.revoke();
			}
			
			//List<FL_Cluster> clusterResults = clusterAccess.getEntities(entityIds, contextid);
		
			List<FL_Entity> entityResults = service.getEntities(accountIds, 
					details? FL_LevelOfDetail.FULL: FL_LevelOfDetail.SUMMARY);

			JSONArray ja = new JSONArray();
			
			for (FL_Entity e : entityResults) {
				ja.put(UISerializationHelper.toUIJson(e));	
			}
			for (FL_Cluster ec : clusterResults) {
				ja.put(UISerializationHelper.toUIJson(ec));
			}
			
			
			result.put("data", ja);
			result.put("queryId", queryId);
			result.put("sessionId", sessionId);
			
			return new StringRepresentation(result.toString(), MediaType.APPLICATION_JSON);
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
					"Unable to create JSON object from supplied options string",
					e
				);
		}
		
		
	}
	
}
