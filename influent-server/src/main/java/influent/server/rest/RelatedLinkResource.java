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
import influent.idl.FL_DateRange;
import influent.idl.FL_DirectionFilter;
import influent.idl.FL_Entity;
import influent.idl.FL_Link;
import influent.idl.FL_LinkTag;
import influent.server.clustering.utils.ContextCollapser;
import influent.server.clustering.utils.ClusterContextCache;
import influent.server.clustering.utils.ContextReadWrite;
import influent.server.clustering.utils.ClusterContextCache.PermitSet;
import influent.server.utilities.DateRangeBuilder;
import influent.server.utilities.DateTimeParser;
import influent.server.utilities.GuidValidator;
import influent.server.utilities.Pair;
import influent.server.utilities.UISerializationHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

public class RelatedLinkResource extends ApertureServerResource{

	private final FL_ClusteringDataAccess clusterAccess;
	@SuppressWarnings("unused")
	private final FL_DataAccess entityAccess;
	private final ClusterContextCache contextCache;

	private static final Logger s_logger = LoggerFactory.getLogger(RelatedLinkResource.class);
	
	
	
	@Inject
	public RelatedLinkResource(FL_ClusteringDataAccess clusterAccess, FL_DataAccess entityAccess, ClusterContextCache contextCache) {
		this.clusterAccess = clusterAccess;
		this.entityAccess = entityAccess;
		this.contextCache = contextCache;
	}
	
	
	
	
	@Post
	public StringRepresentation getLinks(String jsonData) throws ResourceException {
		JSONObject result = new JSONObject();

		// This list is populated with the entities at the other end of the discovered links, and pass to the UI.
		// This is so the UI doesn't need to make a second call to get the entities at the end of the found links.
		List<Object> targets = new ArrayList<Object>();
		boolean fetchTargetEntities = true;
		
		try {
			JSONObject jsonObj = new JSONObject(jsonData);
			
			String sessionId = jsonObj.getString("sessionId").trim();
			if (!GuidValidator.validateGuidString(sessionId)) {
				throw new ResourceException(Status.CLIENT_ERROR_EXPECTATION_FAILED, "sessionId is not a valid UUID");
			}

			// determine the related link direction requested
			FL_DirectionFilter direction = FL_DirectionFilter.BOTH;
			if (jsonObj.has("linktype")) {
				String linktype = jsonObj.getString("linktype");
				
				if (linktype.equalsIgnoreCase ("source"))
					direction = FL_DirectionFilter.SOURCE;
				else if (linktype.equalsIgnoreCase ("destination"))
					direction = FL_DirectionFilter.DESTINATION;
			}
			
			String contextId = jsonObj.getString("srcContextId").trim();
			String targetContextId = jsonObj.getString("tarContextId").trim();
			
			if (jsonObj.has("fetchTargets")) {
				fetchTargetEntities = Boolean.parseBoolean(jsonObj.getString("fetchTargets"));
			}
			
			// the "entity" to fetch related links for - entity could be one of: file cluster, entity cluster or raw entity
			String entityId = jsonObj.getString("entity");
			List<String> flowEntityIds = Collections.singletonList(entityId);
						
			// determine the link filter to use
			FL_LinkTag tag = FL_LinkTag.OTHER;
			if (jsonObj.has("type")){
				String type = jsonObj.getString("type");
				if (type.equalsIgnoreCase ("communication")) {
					tag = FL_LinkTag.COMMUNICATION;
				} else if (type.equalsIgnoreCase ("financial")) {
					tag = FL_LinkTag.FINANCIAL;
				} else if (type.equalsIgnoreCase ("social")) {
					tag = FL_LinkTag.SOCIAL;
				}
			}

			// determine the date filter to use
			DateTime startDate = (jsonObj.has("startdate")) ? DateTimeParser.parse(jsonObj.getString("startdate")) : null;
			DateTime endDate = (jsonObj.has("enddate")) ? DateTimeParser.parse(jsonObj.getString("enddate")) : null;
			FL_DateRange dateRange = DateRangeBuilder.getDateRange(startDate, endDate);
			
			long ams = System.currentTimeMillis();
			
			// determine the focus filter to use - only fetch links that point to these focus ids
			List<String> focusIds;
			if (jsonObj.has("targets")) {
				focusIds = UISerializationHelper.buildListFromJson(jsonObj, "targets");
			} else {
				focusIds = new ArrayList<String>(0);  // no focus ids
			}

			// retrieve and aggregate the related links for the flow entities using the appropriate filters
			// If no focus filter is specified this will cluster the related entities and aggregate the related links accordingly 
			Map<String, List<FL_Link>> relatedLinks = clusterAccess.getFlowAggregation(flowEntityIds, focusIds, direction, tag, dateRange, contextId, targetContextId);	
			
			long bms = System.currentTimeMillis();
			
			// get the target ids aka "related entities" associated with the aggregated related links
			Set<String> targetIds = new HashSet<String>();

			for (String key : relatedLinks.keySet()) {
				for (FL_Link link : relatedLinks.get(key)) {
					if (flowEntityIds.contains(link.getSource())) {
						targetIds.add(link.getTarget());
					} else {
						targetIds.add(link.getSource());
					}
				}
			}
			
			// Id->Id redirect map this is necessary to re-point links if the context is simplified
			Map<String, String> redirect = new HashMap<String, String>();
			
			long cms = System.currentTimeMillis();
			long dms = cms;
			
			Pair<Collection<FL_Entity>, Collection<FL_Cluster>> simpleContext;
			
			final PermitSet permits = new PermitSet();
			
			Collection<FL_Cluster> clusters;
			Collection<FL_Entity> entities;
			
			try {
				final ContextReadWrite targetContextRW = contextCache.getReadWrite(targetContextId, permits);
											
				dms = System.currentTimeMillis();
			
				// collapse context entity cluster hierarchy by simplifying the tree for usability
				// returns root level entities that have no containing cluster and all the simplified clusters in the context
				simpleContext = ContextCollapser.collapse(targetContextRW, false, redirect);
				
				clusters = simpleContext.second;
				entities = simpleContext.first;
				
				// revise the context stored in the cache to use the new simplified context
				targetContextRW.setSimplifiedContext(clusters, entities);
				
			} finally {
				permits.revoke();
			}
			
			long ems = System.currentTimeMillis(); 
			
			// add all the root level entities to the targets - aggregate related links point to them
			targets.addAll(entities);
			
			// find the root clusters in the simplified context - aggregate related links point to them
			for (FL_Cluster cluster : clusters) {
				if (cluster.getParent() == null) {
					targets.add(cluster);
				}
			}
						
			// log timing of this branch was expensive
			long fms = System.currentTimeMillis();
			if ((fms-ams)>5000) {
				s_logger.warn("Slow Branch : took "+(fms-ams)+" ms");
				s_logger.warn("Get Flow : "+(bms-ams)+" ms");
				s_logger.warn("Get Target Entities : "+(cms-bms)+" ms");
				s_logger.warn("Get Context : "+(dms-cms)+" ms");
				s_logger.warn("Collapse Context : "+(ems-dms)+" ms");
			}
			
			// if we have targets and targets were requested by the client then serialize them into the response
			if (!targets.isEmpty() && fetchTargetEntities) {
				//This will be a mix of EntityClusters and FL_Entities
				JSONArray ja = new JSONArray();
				for (Object o : targets) {
					if (o instanceof FL_Entity) {
						ja.put(UISerializationHelper.toUIJson((FL_Entity)o));
					} else if (o instanceof FL_Cluster) {
						ja.put(UISerializationHelper.toUIJson((FL_Cluster)o));
					}
				}
				
				result.put("targets", ja);
			}
			
			// serialize the aggregated related links 
			// and update the source / target's if the context was simplified - they may now point to a simplified object 
			if (!relatedLinks.isEmpty()) {
				JSONObject dmap = new JSONObject();
				for (String key : relatedLinks.keySet()) {
					JSONArray larr = new JSONArray();
					for (FL_Link link : relatedLinks.get(key)) {
						
						// revise the source / target if simplification changed what they should point to
						if (redirect.containsKey(link.getSource())) {
							link.setSource(redirect.get(link.getSource()));
						}
						if (redirect.containsKey(link.getTarget())) {
							link.setTarget(redirect.get(link.getTarget()));
						}
						
						larr.put(UISerializationHelper.toUIJson(link));
					}
					dmap.put(key, larr);
				}
				result.put("data",dmap);
			}
			
			result.put("sessionId", sessionId);

			return new StringRepresentation(result.toString(), MediaType.APPLICATION_JSON);

		} catch (AvroRemoteException e) {
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e.getMessage());
		} catch (JSONException e) {
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e.getMessage());
		}	
	}
}
