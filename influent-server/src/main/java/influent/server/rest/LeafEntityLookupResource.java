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
import influent.idl.FL_EntityTag;
import influent.idl.FL_LevelOfDetail;
import influent.idl.FL_PropertyTag;
import influent.idlhelper.ClusterHelper;
import influent.idlhelper.PropertyHelper;
import influent.server.clustering.utils.ClusterContextCache;
import influent.server.clustering.utils.ClusterContextCache.PermitSet;
import influent.server.clustering.utils.ContextRead;
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



public class LeafEntityLookupResource extends ApertureServerResource{
	
	private final FL_DataAccess service;
	private final ClusterContextCache contextCache;
	
	
	
	@Inject
	public LeafEntityLookupResource(FL_DataAccess service, FL_ClusteringDataAccess clusterDataAccess, ClusterContextCache contextCache) {
		this.service = service;
		this.contextCache = contextCache;
	}
	
	
	
	
	private void getContainedEntitiesHelper(List<String> clusterIds, List<String> leafEntities, ContextRead context) {
		
		if (clusterIds == null || clusterIds.size() == 0) {
			return;
		}
		
		List<FL_Cluster> clusterResults = context.getClusters(clusterIds);
		for (FL_Cluster result : clusterResults) {
			PropertyHelper ownerIdProp = ClusterHelper.getFirstPropertyByTag(result, FL_PropertyTag.ACCOUNT_OWNER);
			
			if (result.getTags().contains(FL_EntityTag.ACCOUNT_OWNER)) {
				leafEntities.add((String)result.getUid());
			} else if (ownerIdProp != null) {
				leafEntities.add((String)ownerIdProp.getValue());
			} else {
				if (result.getMembers() != null && result.getMembers().size() > 0 ) {
					leafEntities.addAll(result.getMembers());
				}
				
				getContainedEntitiesHelper(result.getSubclusters(), leafEntities, context);
			}
		}
	}
	
	
	
	
	@Post("json")
	public StringRepresentation getContainedEntities(String jsonData) throws ResourceException {
		
		JSONObject jsonObj;
		JSONObject result = new JSONObject();
		
		try {
			jsonObj = new JSONObject(jsonData);
			
			String sessionId = jsonObj.getString("sessionId").trim();

			// Details or no?
			Boolean details = jsonObj.has("details")? jsonObj.getBoolean("details"):false;
			
			// get the root node ID from the form
			JSONArray sets = jsonObj.getJSONArray("entitySets");
			JSONArray resultSets = new JSONArray();
			
			
			for (int c=0; c < sets.length(); c++) {
				final JSONObject set = sets.getJSONObject(c);
				final String contextid = set.getString("contextId").trim();
				
				final JSONArray entityNodes = set.getJSONArray("entities");
				final List<String> entityIds = new ArrayList<String>(entityNodes.length());
			
				for (int i=0; i < entityNodes.length(); i++){
					String id = entityNodes.getString(i).trim();
					entityIds.add(id);
				}
				
				PermitSet permits = new PermitSet();
				
				final List<String> clusterIds = TypedId.filterTypedIds(entityIds, TypedId.CLUSTER);
				final List<String> leafIds = new ArrayList<String>();
				if (!clusterIds.isEmpty()) {
					try {
						final ContextRead contextRO = contextCache.getReadOnly(contextid, permits);
						if (contextRO != null) {
							getContainedEntitiesHelper(clusterIds, leafIds, contextRO);
						}
					} finally {
						permits.revoke();
					}
				}
				
				List<String> accountIds = TypedId.filterTypedIds(entityIds, TypedId.ACCOUNT);
				accountIds.addAll(TypedId.filterTypedIds(entityIds, TypedId.ACCOUNT_OWNER));
				accountIds.addAll(TypedId.filterTypedIds(entityIds, TypedId.CLUSTER_SUMMARY));
				accountIds.addAll(leafIds);
				
				JSONArray ja = new JSONArray();
				
				if (details) {
					List<FL_Entity> entityResults = service.getEntities(
						accountIds, FL_LevelOfDetail.FULL
					);
						
					for (FL_Entity e : entityResults) {
						ja.put(UISerializationHelper.toUIJson(e));	
					}
				} else {
					for (String id : accountIds) {
						ja.put(id);
					}
				}
				
				JSONObject resultSet = new JSONObject();
				resultSet.put("contextId", contextid);
				resultSet.put("entities", ja);
				
				resultSets.put(resultSet);
			}
			
			result.put("data", resultSets);
			result.put("sessionId", sessionId);
			
			return new StringRepresentation(result.toString(), MediaType.APPLICATION_JSON);
		
		} catch (JSONException e) {
			throw new ResourceException(
				Status.CLIENT_ERROR_BAD_REQUEST,
				"Unable to create JSON object from supplied options string",
				e
			);
		} catch (AvroRemoteException e) {
			throw new ResourceException(
				Status.SERVER_ERROR_INTERNAL,
				"Unable to retrieve entities",
				e
			);
		}
	}
}
