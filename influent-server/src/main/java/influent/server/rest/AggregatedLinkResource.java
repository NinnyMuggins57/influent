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
import influent.idl.FL_DateRange;
import influent.idl.FL_DirectionFilter;
import influent.idl.FL_Link;
import influent.idl.FL_LinkTag;
import influent.idl.FL_Property;
import influent.idl.FL_PropertyType;
import influent.idlhelper.LinkHelper;
import influent.idlhelper.PropertyHelper;
import influent.idlhelper.SingletonRangeHelper;
import influent.server.clustering.utils.ClusterContextCache;
import influent.server.clustering.utils.ContextRead;
import influent.server.clustering.utils.ClusterContextCache.PermitSet;
import influent.server.utilities.DateRangeBuilder;
import influent.server.utilities.DateTimeParser;
import influent.server.utilities.TypedId;
import influent.server.utilities.UISerializationHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
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
	private final ClusterContextCache contextCache;
	
//	private static final String PROP_LINK_COUNT = "link-count";
	private static final String PROP_DATE = "cluster-link-date-range";
	
	@Inject
	public AggregatedLinkResource(FL_ClusteringDataAccess clusterAccess, ClusterContextCache contextCache) {
		this.clusterAccess = clusterAccess;
		this.contextCache = contextCache;
	}

	@Post
	public StringRepresentation getLinks(String jsonData) throws ResourceException {
		JSONObject jsonObj;
		JSONObject result = new JSONObject();
		Map<String, List<FL_Link>> links = new HashMap<String, List<FL_Link>>();
		
		List<String> sEntities;
		List<String> tEntities;
		String type = null, queryId = null/*, filterType = null*/;
		FL_DirectionFilter direction = FL_DirectionFilter.DESTINATION;
		FL_DateRange dateRange = null;
		
		try {
			jsonObj = new JSONObject(jsonData);

			String sessionId = jsonObj.getString("sessionId").trim();
			
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

			sEntities = UISerializationHelper.buildListFromJson(jsonObj, "sourceIds");
			tEntities = UISerializationHelper.buildListFromJson(jsonObj, "targetIds");

			
			DateTime startDate = (jsonObj.has("startdate")) ? DateTimeParser.parse(jsonObj.getString("startdate")) : null;
			DateTime endDate = (jsonObj.has("enddate")) ? DateTimeParser.parse(jsonObj.getString("enddate")) : null;
			if (startDate != null && endDate != null) {
				dateRange = DateRangeBuilder.getDateRange(startDate, endDate);
			}
			
			String srcContextId = jsonObj.getString("contextid").trim();
			String dstContextId = jsonObj.getString("targetcontextid").trim();
			
			List<String> srcEntities = new ArrayList<String>();
			List<String> dstEntities = new ArrayList<String>();
			Map<String, String> memberToClusterMap = new HashMap<String, String>();

			PermitSet permits = new PermitSet();
			
			try {
				ContextRead srcContext = null;
				ContextRead dstContext = null;
				
				// Check if any of the entities are actually mutable clusters (i.e. internal clusters of files).
				// If they are, unroll them and create a map of parent-cluster-to-child-card ids.
				for (String entityId : sEntities){
					final char etype = TypedId.fromTypedId(entityId).getType();
					
					if (etype == TypedId.FILE) {
						if (srcContext == null) {
							srcContext = contextCache.getReadOnly(srcContextId, permits);
						}
						if (srcContext != null) {
							FL_Cluster flcluster = srcContext.getFile(entityId);
							
							if(flcluster != null) {
								List<String>clusterMembers = new ArrayList<String>();
								clusterMembers.addAll(flcluster.getSubclusters());
								clusterMembers.addAll(flcluster.getMembers());
								for (String memberId : clusterMembers){
									memberToClusterMap.put(memberId, entityId);
								}
								srcEntities.addAll(clusterMembers);
							}
						}
						
					} else {
						srcEntities.add(entityId);
					}
				}
	
				// Check if any of the entities are actually mutable clusters (i.e. internal clusters of files).
				// If they are, unroll them and create a map of parent-cluster-to-child-card ids.
				for (String entityId : tEntities){
					final char etype = TypedId.fromTypedId(entityId).getType();
					
					if (etype == TypedId.FILE) {
						if (dstContext == null) {
							dstContext = contextCache.getReadOnly(dstContextId, permits);
						}
						if (dstContext != null) {
							FL_Cluster flcluster = dstContext.getFile(entityId);
							if(flcluster != null) {
								List<String>clusterMembers = new ArrayList<String>();
								clusterMembers.addAll(flcluster.getSubclusters());
								clusterMembers.addAll(flcluster.getMembers());
								for (String memberId : clusterMembers){
									memberToClusterMap.put(memberId, entityId);
								}
								dstEntities.addAll(clusterMembers);
							}
						}
					} else {
						dstEntities.add(entityId);
					}
				}			
			} finally {
				permits.revoke();
			}
			
			links = clusterAccess.getFlowAggregation(srcEntities, dstEntities, direction, FL_LinkTag.FINANCIAL, dateRange, srcContextId, dstContextId, sessionId);			
			
			// Get the query id. This is used by the client to ensure
			// it only processes the latest response.
			queryId = jsonObj.getString("queryId").trim();

			if (links != null && !links.isEmpty()) {
				
				Map<String, FL_Link> linkMap = new HashMap<String, FL_Link>();
				Map<String, List<FL_Link>> dataMap = new HashMap<String, List<FL_Link>>();
				for (String key : links.keySet()) {
					
					for (FL_Link link : links.get(key)) {
						// Iterate through the links and re-map the children to
						// the parent cluster where appropriate.
						String sId = link.getSource();
						String tId = link.getTarget();
						if (memberToClusterMap.containsKey(sId)){
							link.setSource(memberToClusterMap.get(sId));
						}
						if (memberToClusterMap.containsKey(tId)){
							link.setTarget(memberToClusterMap.get(tId));
						}
						
						String linkName = link.getSource() + "_:_" + link.getTarget();
						if (linkMap.containsKey(linkName)) {
							updateLinkWithNewProperties(linkMap.get(linkName), link);
						} else {
							linkMap.put(linkName, link);
						}
					}
				}
				
				// create the resulting links for each src entity
				for (String key : linkMap.keySet()) {
					FL_Link link = linkMap.get(key);
				
					String[] tokens = key.split("_:_");
						
					String src = (direction == FL_DirectionFilter.SOURCE) ? tokens[0] : tokens[1];
					
					List<FL_Link> linkset = dataMap.get(src);
					
					if (linkset == null) {
						linkset = new LinkedList<FL_Link>();
						dataMap.put(src, linkset);
					}
					linkset.add(link);
				}
				
				JSONObject dmap = new JSONObject();
				for (Entry<String, List<FL_Link>> entry : dataMap.entrySet()) {
					JSONArray larr = new JSONArray();
					for (FL_Link link : entry.getValue()) {
						larr.put(UISerializationHelper.toUIJson(link));
					}
					dmap.put(entry.getKey(), larr);
				}
				
				result.put("data",dmap);
			}

			result.put("queryId", queryId);
			result.put("sessionId", sessionId);

			return new StringRepresentation(result.toString(),MediaType.APPLICATION_JSON);
			
		} catch (JSONException e) {
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e.getMessage());
		} catch (AvroRemoteException e) {
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e.getMessage());
		}
	}
	
	
	
	
	private void updateLinkWithNewProperties(FL_Link oldLink, FL_Link newLink) {

//		PropertyHelper oldLinkCount = LinkHelper.getFirstProperty(oldLink, PROP_LINK_COUNT);
		PropertyHelper oldDateProp = LinkHelper.getFirstProperty(oldLink, PROP_DATE);
		
//		PropertyHelper newLinkCount = LinkHelper.getFirstProperty(newLink, PROP_LINK_COUNT);
		PropertyHelper newDateProp = LinkHelper.getFirstProperty(newLink, PROP_DATE);

//		int count = ((Integer)oldLinkCount.getValue()) + ((Integer)newLinkCount.getValue());
//		oldLinkCount.setRange(new SingletonRangeHelper(count, FL_PropertyType.LONG));
	
		DateTime minDate = null;
		DateTime maxDate = null;
		if (oldDateProp != null || newDateProp != null) {
			if (oldDateProp != null) {
				FL_BoundedRange oldRange = (FL_BoundedRange) oldDateProp.getRange();
				minDate = DateTimeParser.fromFL(oldRange.getStart());
				maxDate = DateTimeParser.fromFL(oldRange.getEnd());
			}
			
			if (newDateProp != null) {
				FL_BoundedRange newRange = (FL_BoundedRange) newDateProp.getRange();
				DateTime newMinDate = DateTimeParser.fromFL(newRange.getStart());
				DateTime newMaxDate = DateTimeParser.fromFL(newRange.getEnd());
				if (minDate == null || minDate.isAfter(newMinDate)) {
					((FL_BoundedRange)oldDateProp.getRange()).setStart(newMinDate);
				}
				if (maxDate == null || maxDate.isBefore(newMaxDate)) {
					((FL_BoundedRange)oldDateProp.getRange()).setEnd(newMaxDate);
				}
			}
		}
		
		for (FL_Property prop : newLink.getProperties()) {
			
			PropertyHelper propHelp = PropertyHelper.from(prop);
			List<PropertyHelper> oldPropList = LinkHelper.getProperties(oldLink, propHelp.getKey());
			
			if (oldPropList != null && oldPropList.size() > 0) {
				switch (oldPropList.get(0).getType()) {
				case LONG:
					try {
						int oldival = (Integer)oldPropList.get(0).getValue();
						int ival = (Integer)propHelp.getValue();
						oldPropList.get(0).setRange(new SingletonRangeHelper(oldival + ival, FL_PropertyType.LONG));
					} catch (Exception e) { /* ignore */ }
					break;
				case DOUBLE:
					try {
						double olddval = (Double)oldPropList.get(0).getValue();
						double dval = (Double)propHelp.getValue();
						oldPropList.get(0).setRange(new SingletonRangeHelper(olddval + dval, FL_PropertyType.DOUBLE));
					} catch (Exception e) { /* ignore */ }
					break;
				default:
					continue;
				}
			}
			else {
				oldLink.getProperties().add(propHelp);
			}
		}
	}
}
