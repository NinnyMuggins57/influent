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

import influent.idl.FL_ClusteringDataAccess;
import influent.idl.FL_DateRange;
import influent.idl.FL_DirectionFilter;
import influent.idl.FL_Link;
import influent.idl.FL_LinkTag;
import influent.server.clustering.utils.ClusterContextCache;
import influent.server.utilities.DateRangeBuilder;
import influent.server.utilities.DateTimeParser;
import influent.server.utilities.GuidValidator;
import influent.server.utilities.UISerializationHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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

public class AggregatedLinkResource extends ApertureServerResource{

	private final FL_ClusteringDataAccess clusterAccess;
	
	
	
	@Inject
	public AggregatedLinkResource(FL_ClusteringDataAccess clusterAccess, ClusterContextCache contextCache) {
		this.clusterAccess = clusterAccess;
	}
	
	
	
	
	@Post("json")
	public StringRepresentation getLinks(String jsonData) throws ResourceException {
		JSONObject jsonObj;
		JSONObject result = new JSONObject();
		Map<String, List<FL_Link>> links = new HashMap<String, List<FL_Link>>();
		
		String type = null;
		FL_DirectionFilter direction = FL_DirectionFilter.DESTINATION;
		FL_DateRange dateRange = null;
		
		try {
			jsonObj = new JSONObject(jsonData);

			String sessionId = jsonObj.getString("sessionId").trim();
			if (!GuidValidator.validateGuidString(sessionId)) {
				throw new ResourceException(Status.CLIENT_ERROR_EXPECTATION_FAILED, "sessionId is not a valid UUID");
			}
			
			/*
			 * Valid arguments are:
			 *   - source
			 *   - destination
			 *   - both
			 */
			if (jsonObj.has("linktype")) {
				type = jsonObj.getString("linktype");
				
				if (type.equalsIgnoreCase ("source"))
					direction = FL_DirectionFilter.SOURCE;
				else if (type.equalsIgnoreCase ("destination"))
					direction = FL_DirectionFilter.DESTINATION;
				else 
					direction = FL_DirectionFilter.BOTH;
			}

			List<String> srcEntities = UISerializationHelper.buildListFromJson(jsonObj, "sourceIds");
			List<String> dstEntities = UISerializationHelper.buildListFromJson(jsonObj, "targetIds");

			DateTime startDate = null;
			try {
				startDate = (jsonObj.has("startdate")) ? DateTimeParser.parse(jsonObj.getString("startdate")) : null;
			} catch (IllegalArgumentException iae) {
				throw new ResourceException(
					Status.CLIENT_ERROR_BAD_REQUEST,
					"AggregatedLinkResource: An illegal argument was passed into the 'startdate' parameter."
				);
			}
			
			DateTime endDate = null;
			try {
				endDate = (jsonObj.has("enddate")) ? DateTimeParser.parse(jsonObj.getString("enddate")) : null;
			} catch (IllegalArgumentException iae) {
				throw new ResourceException(
					Status.CLIENT_ERROR_BAD_REQUEST,
					"AggregatedLinkResource: An illegal argument was passed into the 'enddate' parameter."
				);
			}

			if (startDate != null && endDate != null) {
				dateRange = DateRangeBuilder.getDateRange(startDate, endDate);
			}
			
			String srcContextId = jsonObj.getString("contextId").trim();
			String dstContextId = jsonObj.getString("targetContextId").trim();
						
			links = clusterAccess.getFlowAggregation(srcEntities, dstEntities, direction, FL_LinkTag.FINANCIAL, dateRange, srcContextId, dstContextId);			

			if (links != null && !links.isEmpty()) {
				JSONObject dmap = new JSONObject();
				for (Entry<String, List<FL_Link>> entry : links.entrySet()) {
					JSONArray larr = new JSONArray();
					for (FL_Link link : entry.getValue()) {
						larr.put(UISerializationHelper.toUIJson(link));
					}
					dmap.put(entry.getKey(), larr);
				}
				
				result.put("data",dmap);
			}

			result.put("sessionId", sessionId);

			return new StringRepresentation(result.toString(),MediaType.APPLICATION_JSON);
			
		} catch (JSONException e) {
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e.getMessage());
		} catch (AvroRemoteException e) {
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e.getMessage());
		}
	}
}
