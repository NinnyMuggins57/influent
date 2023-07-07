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
import influent.idl.FL_Entity;
import influent.server.clustering.utils.ClusterContextCache;
import influent.server.clustering.utils.ContextRead;
import influent.server.clustering.utils.ClusterContextCache.PermitSet;
import influent.server.data.ChartData;
import influent.server.utilities.ChartBuilder;
import influent.server.utilities.DateRangeBuilder;
import influent.server.utilities.DateTimeParser;
import influent.server.utilities.TypedId;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import oculus.aperture.common.rest.ApertureServerResource;

import org.apache.avro.AvroRemoteException;
import org.joda.time.DateTime;
import org.joda.time.MutableDateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.data.Status;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class BigChartResource extends ApertureServerResource {
	
	private static final String SEPARATOR 		= "|";
	private static final String ID_SECTION 		= SEPARATOR + ":I:";
	private static final String FOCUS_ID 		= ":F:";
	private static final String UUID_SEPARATOR	= ":U:";
	
	final Logger logger = LoggerFactory.getLogger(getClass());

	private final ChartBuilder chartBuilder;
	private final ClusterContextCache contextCache;

	@Inject
	public BigChartResource(
		FL_DataAccess entityAccess,
		FL_ClusteringDataAccess clusterAccess,
		@Named("influent.midtier.ehcache.config") String ehCacheConfig, 
		ClusterContextCache contextCache
	) {
		this.contextCache = contextCache;
		chartBuilder = new ChartBuilder(clusterAccess, ehCacheConfig, contextCache);
	}
	
	
	
	
	public static String hashString(String str) {
		return Integer.toString(str.hashCode());
	}
	
	
	
	
	@Post("json")
	public Map<String, ChartData> getBigChartData(String jsonData) {

		try {
			JSONObject jsonObj = new JSONObject(jsonData);
			
			String focusContextId = jsonObj.getString("focuscontextid");
			
			String sessionId = jsonObj.getString("sessionId").trim();
			
			DateTime startDate = DateTimeParser.parse(jsonObj.getString("startDate"));
			DateTime endDate = DateTimeParser.parse(jsonObj.getString("endDate"));
			
			List<String> focusIds = new LinkedList<String>();
			JSONArray focusObj = jsonObj.getJSONArray("focusId");


			for (int i=0; i < focusObj.length(); i++) {
				String entityId = focusObj.getString(i);
				List<String> entities = new ArrayList<String>();

				TypedId id = TypedId.fromTypedId(entityId);
				
				if (id.getType() == TypedId.CLUSTER) {
					String nId = id.getNativeId();  
					if (nId.startsWith("|")) { // group cluster
						for (String sId : nId.split("\\|")) {
							entities.add(sId);
						}
					} else {
						entities.add(entityId);
					}
				} else {
					entities.add(entityId);
				}
				
				for (String fid : entities){
					if (!focusIds.contains(fid)){
						focusIds.add(fid);
					}
				}
			}
				
			
			String tempFocusMaxDebitCredit = jsonObj.getString("focusMaxDebitCredit");
			Double focusMaxDebitCredit = null;
			try {
				focusMaxDebitCredit = tempFocusMaxDebitCredit.trim().length()==0 ? null : Double.parseDouble(tempFocusMaxDebitCredit);
			} catch(Exception ignore) {}
			
			Integer width = jsonObj.has("width")?Integer.parseInt(jsonObj.getString("width")):145;
			Integer height = jsonObj.has("height")?Integer.parseInt(jsonObj.getString("height")):60;

			JSONArray entityArray = jsonObj.getJSONArray("entities");

			Map<String, ChartData> infoList = new HashMap<String, ChartData>(entityArray.length());
			
			/// TODO : make this date range sanity check better
			if (startDate.getYear()<1900 || startDate.getYear()>9999) {
				MutableDateTime msdt = new MutableDateTime(startDate);
				msdt.setYear(2007);
				startDate = msdt.toDateTime();
				logger.warn("Invalid start date passed from UI, setting to default");
			}
			if (endDate.getYear()<1900 || endDate.getYear()>9999) {
				MutableDateTime medt = new MutableDateTime(endDate);
				medt.setYear(2013);
				endDate = medt.toDateTime();
				logger.warn("Invalid end date passed from UI, setting to default");
			}
			FL_DateRange dateRange = DateRangeBuilder.getBigChartDateRange(startDate, endDate);
			
			// compute an individual chart for each entity received
			for (int i = 0; i < entityArray.length(); i++) {
				JSONObject entityRequest = entityArray.getJSONObject(i);
				final String entityId = entityRequest.getString("dataId");
				final String entityContextId = entityRequest.getString("contextId");
				
				List<String> entities = new ArrayList<String>();

				// Check to see if this entityId belongs to a group cluster.
				TypedId id = TypedId.fromTypedId(entityId);
				
				if (id.getType() == TypedId.CLUSTER) {
					String nId = id.getNativeId();  
					if (nId.startsWith("|")) {  // group cluster
						for (String sId : nId.split("\\|")) {
							entities.add(sId);
						}
					} else {
						entities.add(entityId);
					}
				} else {
					entities.add(entityId);
				}
					
				Hash hashed = new Hash(entityId, entities, startDate, endDate, focusIds, focusMaxDebitCredit, dateRange.getNumBins().intValue(), width, height, entityContextId, focusContextId, sessionId);
				
				ChartData chartData = chartBuilder.computeChart(dateRange, entities, focusIds, entityContextId, focusContextId, sessionId, dateRange.getNumBins().intValue(), hashed.hash);

				infoList.put(
						entityId, //memberIds.get(0), 
						chartData
				);
			}			
			
			return infoList;

		} catch (AvroRemoteException e) {
			throw new ResourceException(
				Status.CLIENT_ERROR_BAD_REQUEST,
				"Data access error.",
				e
			);
		} catch (JSONException je) {
			throw new ResourceException(
				Status.CLIENT_ERROR_BAD_REQUEST,
				"JSON parse error.",
				je
			);
		}
	}
	
	//TODO : de-hack this once the new clustering is in place.
	public static List<String> getLeafNodes (List<? extends Object> entities) {
		List<String> IDs = new ArrayList<String> ();
		for (Object i : entities) {
			if (i instanceof FL_Entity) {
				IDs.add(((FL_Entity)i).getUid());
			} else if (i instanceof FL_Cluster) {
				FL_Cluster ec = (FL_Cluster)i;
				
				//Get the stored clusters.
				List<Object> recurseList = new ArrayList<Object>();
				for (String id : ec.getMembers()) {
					/*EntityCluster subcluster = MemoryTransientClusterStore.getInstance().getMap().get(id);
					if (subcluster != null) {
						recurseList.add(subcluster);
					} else*/ {
						IDs.add(id);		//Not a cluster, assume a raw entity
					}
				}
				IDs.addAll(getLeafNodes(recurseList));
			}				
		}
		return IDs;
	}
	
	private class Hash {
		List<String> ids;
		DateTime startDate; 
		DateTime endDate; 
		List<String> focusIds;
		Double focusMaxDebitCredit;
		Integer numBuckets;
		Integer width;
		Integer height;
		String hash;
		String id;
		String contextId;
		String focusContextId;
		String sessionId;
		
		Hash(String id, List<String> ids, DateTime startDate, DateTime endDate, List<String> focusIds, Double focusMaxDebitCredit, Integer numBuckets, Integer width, Integer height, String contextId, String focusContextId, String sessionId) {
			this.id = id;
			this.ids = ids;
			this.startDate = startDate;
			this.endDate = endDate;
			this.focusIds = focusIds;
			this.focusMaxDebitCredit = focusMaxDebitCredit;
			this.numBuckets = numBuckets;
			this.width = width;
			this.height = height;
			this.contextId = contextId;
			this.focusContextId = focusContextId;
			this.sessionId = sessionId;
			this.hash = makeHash();
		}

		private String makeHash() {
			StringBuilder buffer = new StringBuilder();
			buffer.append(sessionId+SEPARATOR);
			buffer.append(contextId+SEPARATOR);
			buffer.append(focusContextId+SEPARATOR);
			buffer.append(startDate.toString() + SEPARATOR);
			buffer.append(endDate.toString() + SEPARATOR);
//			buffer.append(focusId + SEPARATOR);
			buffer.append(focusMaxDebitCredit + SEPARATOR);
			buffer.append(numBuckets.toString() + SEPARATOR);
			buffer.append(width.toString() + SEPARATOR);
			buffer.append(height.toString() + SEPARATOR);
			buffer.append(id.replaceAll(" ", "%20"));					// URL hash CANNOT have spaces!!
			buffer.append(ID_SECTION); 									// Define where ids start 
			
			final PermitSet permits = new PermitSet();
			
			for (String id : ids) {
				buffer.append(SEPARATOR + id.replaceAll(" ", "%20"));
				
				TypedId tId = TypedId.fromTypedId(id);
				
				// Extract the membership of files to form UUIDs from them
				String membershipUUID = "";
				if (tId.getType() == TypedId.FILE) {
					try {
						final ContextRead entityContext = contextCache.getReadOnly(contextId, permits);
	
						if (entityContext != null) {
							FL_Cluster flcluster = entityContext.getFile(id);
							
							if (flcluster != null) {
								
								for (String subcluster : flcluster.getSubclusters()) {
									membershipUUID += subcluster;
								}
								for (String member : flcluster.getMembers()) {
									membershipUUID += member;
								}
								
							}
						}
					} finally {
						permits.revoke();
					}
					
					buffer.append(UUID_SEPARATOR + UUID.nameUUIDFromBytes(membershipUUID.getBytes()));
				}	
			}
			
			for (String id : focusIds) {
				buffer.append(SEPARATOR + FOCUS_ID + id.replaceAll(" ", "%20"));
				
				TypedId tId = TypedId.fromTypedId(id);
				
				// Extract the membership of files to form UUIDs from them
				String membershipUUID = "";
				if (tId.getType() == TypedId.FILE) {
					try {
						final ContextRead focusContext = contextCache.getReadOnly(focusContextId, permits);
	
						if (focusContext != null) {
							FL_Cluster flcluster = focusContext.getFile(id);
							
							if (flcluster != null) {
								
								for (String subcluster : flcluster.getSubclusters()) {
									membershipUUID += subcluster;
								}
								for (String member : flcluster.getMembers()) {
									membershipUUID += member;
								}
								
							}
						}
					} finally {
						permits.revoke();
					}
					
					buffer.append(UUID_SEPARATOR + UUID.nameUUIDFromBytes(membershipUUID.getBytes()));
				}	
				
			}
			
			return buffer.toString();
		}

	}
}
