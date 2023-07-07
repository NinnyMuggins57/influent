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
import influent.idl.FL_DataAccess;
import influent.idl.FL_DateRange;
import influent.server.clustering.utils.ClusterContextCache;
import influent.server.data.ChartData;
import influent.server.utilities.ChartBuilder;
import influent.server.utilities.DateRangeBuilder;
import influent.server.utilities.DateTimeParser;
import influent.server.utilities.GuidValidator;
import influent.server.utilities.TypedId;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
		chartBuilder = new ChartBuilder(clusterAccess, ehCacheConfig);
	}
	
	
	
	
	@Post("json")
	public Map<String, ChartData> getBigChartData(String jsonData) {

		try {
			JSONObject jsonObj = new JSONObject(jsonData);
			
			String focusContextId = (jsonObj.isNull("focuscontextid")) ? null : jsonObj.getString("focuscontextid");
			
			String sessionId = jsonObj.getString("sessionId").trim();
			if (!GuidValidator.validateGuidString(sessionId)) {
				throw new ResourceException(Status.CLIENT_ERROR_EXPECTATION_FAILED, "sessionId is not a valid UUID");
			}
			
			DateTime startDate = null;
			try {
				startDate = DateTimeParser.parse(jsonObj.getString("startDate"));
			} catch (IllegalArgumentException iae) {
				throw new ResourceException(
					Status.CLIENT_ERROR_BAD_REQUEST,
					"BigChartResource: An illegal argument was passed into the 'startDate' parameter."
				);
			}
			
			DateTime endDate = null;
			try {
				endDate = DateTimeParser.parse(jsonObj.getString("endDate"));
			} catch (IllegalArgumentException iae) {
				throw new ResourceException(
					Status.CLIENT_ERROR_BAD_REQUEST,
					"BigChartResource: An illegal argument was passed into the 'endDate' parameter."
				);
			}
			
			List<String> focusIds = null;
			if (!jsonObj.isNull("focusId")) {
				focusIds = new LinkedList<String>();
				JSONArray focusObj = jsonObj.getJSONArray("focusId");
				
				for (int i=0; i < focusObj.length(); i++) {
					String entityId = focusObj.getString(i);
					List<String> entities = new ArrayList<String>();
	
					TypedId id = TypedId.fromTypedId(entityId);
					
					// Point account owners and summaries to their owner account
					if (id.getType() == TypedId.ACCOUNT_OWNER || 
						id.getType() == TypedId.CLUSTER_SUMMARY) {
							
						entities.add(TypedId.fromNativeId(TypedId.ACCOUNT, id.getNamespace(), id.getNativeId()).toString());
							
					} else if (id.getType() == TypedId.CLUSTER) {
						
						String nId = id.getNativeId();  
						if (nId.startsWith("|")) { // group cluster
							for (String sId : nId.split("\\|")) {
								if (!sId.isEmpty()) {
									entities.add(sId);
								}
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
				
				List<String> entityIds = new ArrayList<String>();

				// Check to see if this entityId belongs to a group cluster.
				TypedId id = TypedId.fromTypedId(entityId);
					
				if (id.getType() == TypedId.CLUSTER) {
					String nId = id.getNativeId();  
					if (nId.startsWith("|")) {  // group cluster
						for (String sId : nId.split("\\|")) {
							if (!sId.isEmpty()) {
								entityIds.add(sId);
							}
						}
					} else {
						entityIds.add(entityId);
					}
				} else {
					entityIds.add(entityId);
				}
					
				ChartHash hash = new ChartHash(entityIds, 
											   startDate, 
											   endDate, 
											   focusIds, 
											   focusMaxDebitCredit, 
											   dateRange.getNumBins().intValue(), 
											   width, 
											   height, 
											   entityContextId, 
											   focusContextId, 
											   sessionId,
											   contextCache);
				
				ChartData chartData = chartBuilder.computeChart(dateRange, 
																entityIds, 
																focusIds, 
																entityContextId, 
																focusContextId, 
																sessionId, 
																dateRange.getNumBins().intValue(), 
																hash);

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
}
