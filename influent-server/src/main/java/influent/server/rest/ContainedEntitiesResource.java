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
import influent.idl.FL_DataAccess;
import influent.server.clustering.utils.ClusterContextCache;
import influent.server.clustering.utils.ContextRead;
import influent.server.clustering.utils.ClusterContextCache.PermitSet;
import influent.server.utilities.UISerializationHelper;

import java.util.ArrayList;
import java.util.List;

import oculus.aperture.common.rest.ApertureServerResource;

import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;

import com.google.inject.Inject;



public class ContainedEntitiesResource extends ApertureServerResource{
	
	final FL_DataAccess dataAccess;
	private final ClusterContextCache contextCache;

	@Inject
	public ContainedEntitiesResource(FL_DataAccess dataAccess, ClusterContextCache contextCache) {
		this.dataAccess = dataAccess;
		this.contextCache = contextCache;
	}
	
	private void getContainedEntitiesHelper(List<String> clusterIds, List<String> leafEntities, ContextRead context) {
		
		if (clusterIds == null || clusterIds.size() == 0) {
			return;
		}
		
		List<FL_Cluster> clusterResults = context.getClusters(clusterIds);
		for (FL_Cluster result : clusterResults) {
			if (result.getMembers() != null && result.getMembers().size() > 0 ) {
				leafEntities.addAll(result.getMembers());
			}
			getContainedEntitiesHelper(result.getSubclusters(), leafEntities, context);
		}
	}
	
	@Post("json")
	public StringRepresentation getContainedEntities(String jsonData) throws ResourceException {
		try {
			JSONObject jsonObj = new JSONObject(jsonData);
			
			String sessionId = jsonObj.getString("sessionId").trim();
			
			// Get the query id. This is used by the client to ensure
			// it only processes the latest response.
			String queryId = jsonObj.getString("queryId").trim();
			String contextid = jsonObj.getString("contextid").trim();
			
						
			// Get the query id. This is used by the client to ensure
			// it only processes the latest response.
			List<String> rawIds = UISerializationHelper.buildListFromJson(jsonObj, "clusterIds");
			List<String> leafIds = new ArrayList<String>();

			PermitSet permits = new PermitSet();
			
			try {
				ContextRead contextRO = contextCache.getReadOnly(contextid, permits);
				
				if (contextRO != null) {
					getContainedEntitiesHelper(rawIds,leafIds,contextRO);
				}
				
			} finally {
				permits.revoke();
			}

			JSONObject result = new JSONObject();
			result.put("queryId", queryId);
			result.put("sessionId", sessionId);
			result.put("data", leafIds);

			return new StringRepresentation(result.toString(), MediaType.APPLICATION_JSON);		
		
		} catch (JSONException e) {
			throw new ResourceException(
				Status.CLIENT_ERROR_BAD_REQUEST,
				"Unable to create JSON object from supplied options string",
				e
			);
		}
	}
}
