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

import influent.idl.FL_DataAccess;
import influent.idl.FL_DateRange;
import influent.idl.FL_Link;
import influent.idl.FL_LinkTag;
import influent.idl.FL_Property;
import influent.idl.FL_PropertyTag;
import influent.idl.FL_SortBy;
import influent.idl.FL_TransactionResults;
import influent.idlhelper.PropertyHelper;
import influent.server.utilities.DateRangeBuilder;
import influent.server.utilities.DateTimeParser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;

import oculus.aperture.capture.phantom.data.ProcessedTaskInfo;
import oculus.aperture.common.rest.ApertureServerResource;
import oculus.aperture.spi.store.ConflictException;
import oculus.aperture.spi.store.ContentService;
import oculus.aperture.spi.store.ContentService.Document;
import oculus.aperture.spi.store.ContentService.DocumentDescriptor;

import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.data.CacheDirective;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.name.Named;


public class ExportTransactionTableResource extends ApertureServerResource {
	
	private static final SimpleDateFormat csvDateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
	
	private final FL_DataAccess dataAccess;
	
	private final ContentService service;
	
	private final int maxCacheAge;
	
	// The name of the CMS store we'll use for data captures
	private final static String DEFAULT_STORE = "aperture.data";
	
	@Inject
	public ExportTransactionTableResource(
			ContentService service,
			FL_DataAccess dataAccess,
			@Named("influent.charts.maxage") Integer maxCacheAge) {
		this.dataAccess = dataAccess;
		this.service = service;
		this.maxCacheAge = maxCacheAge;
	}
	
	@Post("json")
	public Representation getLedger(String jsonData) throws ResourceException {
		
		try {
			JSONObject jsonObj = new JSONObject(jsonData);
		
			String entityId = jsonObj.getString("entityId").trim();
			
			List<String> entityIds = Collections.singletonList(entityId);
			
			DateTime startDate = null;
			try {
				startDate = DateTimeParser.parse(jsonObj.getString("startDate"));
			} catch (IllegalArgumentException iae) {
				throw new ResourceException(
					Status.CLIENT_ERROR_BAD_REQUEST,
					"ExportTransactionTableResource: An illegal argument was passed into the 'startDate' parameter."
				);
			}
			
			DateTime endDate = null;
			try {
				endDate = DateTimeParser.parse(jsonObj.getString("endDate"));
			} catch (IllegalArgumentException iae) {
				throw new ResourceException(
					Status.CLIENT_ERROR_BAD_REQUEST,
					"ExportTransactionTableResource: An illegal argument was passed into the 'endDate' parameter."
				);
			}

			FL_DateRange dateRange = DateRangeBuilder.getDateRange(startDate, endDate);
			FL_TransactionResults results = dataAccess.getAllTransactions(
					entityIds, FL_LinkTag.FINANCIAL, dateRange, FL_SortBy.DATE, null, 0, 1000000);
			ArrayList<String> colHeader = null;
			if(results.getResults().size() > 0) {
				colHeader = new ArrayList<String>();
				colHeader.add("Source");
				colHeader.add("Target");
				for(FL_Property prop : results.getResults().get(0).getProperties()) {
					colHeader.add(prop.getFriendlyText());
				}
			}

			StringBuilder csvBuilder = new StringBuilder();
			if(colHeader != null) {
				for (int i = 0; i < colHeader.size(); i++) {
					csvBuilder.append(colHeader.get(i));
					if ( i < colHeader.size() - 1) {
						csvBuilder.append(",");
					}
				}
				csvBuilder.append("\n");
				
				for(FL_Link link : results.getResults()) {
					csvBuilder.append(link.getSource());
					csvBuilder.append(",");
					csvBuilder.append(link.getTarget());
					csvBuilder.append(",");
					for(int col = 0; col < link.getProperties().size(); col++) {
						csvBuilder.append(formatProperty(link.getProperties().get(col)));
						if ( col < link.getProperties().size() - 1) {
							csvBuilder.append(",");
						}
					}
					csvBuilder.append("\n");
				}
			}
			
			Representation rep = getRepresentaion(csvBuilder.toString());
			
			getResponse().setCacheDirectives(
				Collections.singletonList(
					CacheDirective.maxAge(maxCacheAge)
				)
			);

			if (rep == null) {
				throw new ResourceException(
					Status.SERVER_ERROR_INTERNAL,
					"Data table processing failed to complete for an unknown reason."
				);
			}
			
			return rep;
			
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	private Representation getRepresentaion(String data) throws ConflictException, JSONException, JAXBException {
		
		byte[] csvData = data.getBytes();		
		final String csvType = "text/csv";
		
		// Store to the content service, return a URL to the image
		Document doc = service.createDocument();
		doc.setContentType(csvType);
		doc.setDocument(csvData);
		
		// Store and let the content service pick the id
		DocumentDescriptor descriptor = service.storeDocument(
			doc, 
			DEFAULT_STORE, 
			null, 
			null
		);

		Map<String,Object> response = Maps.newHashMap();
		
		// process result.
		if (descriptor != ProcessedTaskInfo.NONE) {
			
			// Return a response containing a JSON block with the id/rev
			response.put("id", descriptor.getId());
			response.put("store", descriptor.getStore());
			
			// if have a revision append it.
			if (descriptor.getRevision() != null) {
				response.put("rev", descriptor.getRevision());
			}
			
		} else {
			return null;
		}

		// Return a JSON response
		return new JsonRepresentation(response);
	}
	
	
	private static String formatProperty(FL_Property prop) {
		
		PropertyHelper property = PropertyHelper.from(prop);
		
		if (property.hasTag(FL_PropertyTag.DATE)) {
			return csvDateFormat.format(DateTimeParser.fromFL(property.getValue()).toDate());
		}

		return String.valueOf(property.getValue());
	}
}
