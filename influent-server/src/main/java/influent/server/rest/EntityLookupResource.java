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
import influent.server.clustering.utils.ContextRead;
import influent.server.clustering.utils.ClusterContextCache.PermitSet;
import influent.server.utilities.GuidValidator;
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
	private final ClusterContextCache contextCache;
	
	
	
	@Inject
	public EntityLookupResource(FL_DataAccess service, FL_ClusteringDataAccess clusterDataAccess, ClusterContextCache contextCache) {
		this.service = service;
		this.contextCache = contextCache;
	}
	
	
	
	
	@Post("json")
	public StringRepresentation lookup(String jsonData) throws ResourceException {
		JSONObject jsonObj;
		JSONObject result = new JSONObject();
		try {
			jsonObj = new JSONObject(jsonData);
			
			String sessionId = jsonObj.getString("sessionId").trim();
			if (!GuidValidator.validateGuidString(sessionId)) {
				throw new ResourceException(Status.CLIENT_ERROR_EXPECTATION_FAILED, "sessionId is not a valid UUID");
			}
			
			String contextid = jsonObj.getString("contextid").trim();
			
			Boolean details = jsonObj.has("details")? jsonObj.getBoolean("details"):false;
			
			// get the root node ID from the form
			JSONArray entityNodes = jsonObj.getJSONArray("entities");
			List<String> entityIds = new ArrayList<String>(entityNodes.length());
		
			for (int i=0; i < entityNodes.length(); i++){
				String id = entityNodes.getString(i).trim();
				entityIds.add(id);
			}

			List<String> clusterIds = TypedId.filterTypedIds(entityIds, TypedId.CLUSTER);
			
			List<String> clusterSummaryIds = TypedId.filterTypedIds(entityIds, TypedId.CLUSTER_SUMMARY);
			clusterIds.addAll(clusterSummaryIds);
			
			List<String> accountIds = TypedId.filterTypedIds(entityIds, TypedId.ACCOUNT);
			
			// If there are any owners, look those up as accounts:
			List<String> ownerIds = TypedId.filterTypedIds(entityIds, TypedId.ACCOUNT_OWNER);
			clusterIds.addAll(ownerIds);	
			
			PermitSet permits = new PermitSet();
			List<FL_Cluster> clusterResults = new ArrayList<FL_Cluster>();
			
			if (!clusterIds.isEmpty()) {
				try {
					ContextRead contextRO = contextCache.getReadOnly(contextid, permits);
					clusterResults.addAll(contextRO.getClusters(clusterIds));									
				} finally {
					permits.revoke();
				}
			}
		
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
