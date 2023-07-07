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
import influent.idl.FL_Entity;
import influent.idl.FL_LevelOfDetail;
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

import oculus.aperture.common.rest.ApertureServerResource;

import org.joda.time.DateTime;
import org.json.JSONObject;
import org.restlet.data.Disposition;
import org.restlet.data.MediaType;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;

import com.google.inject.Inject;


public class ExportTransactionTableResource extends ApertureServerResource {
	
	private static final SimpleDateFormat csvDateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
	
	private final FL_DataAccess dataAccess;
	
	@Inject
	public ExportTransactionTableResource(FL_DataAccess dataAccess) {
		this.dataAccess = dataAccess;
	}
	
	
	
	
	@Post("json")
	public StringRepresentation getLedger(String jsonData) throws ResourceException {
		
		try {
			JSONObject jsonObj = new JSONObject(jsonData);
		
			String entityId = jsonObj.getString("entityId").trim();
			
			List<String> entityIds = Collections.singletonList(entityId);
			
			String startDateStr = jsonObj.getString("startDate").trim();
			String endDateStr = jsonObj.getString("endDate").trim();
			DateTime startDate = DateTimeParser.parse(startDateStr);
			DateTime endDate = DateTimeParser.parse(endDateStr);
			String fileName = entityId;
			List<FL_Entity> entityList = dataAccess.getEntities(entityIds, FL_LevelOfDetail.SUMMARY);
			if(entityList.size() > 0) {
				for(FL_Property p : entityList.get(0).getProperties()) {
					PropertyHelper prop = PropertyHelper.from(p);
					if(prop.hasValue() && prop.hasTag(FL_PropertyTag.LABEL)) {
						fileName = prop.getValue().toString();
						break;
					}
				}
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
						
			Disposition attatchment = new Disposition(Disposition.TYPE_ATTACHMENT);
			attatchment.setFilename("transactions_"+fileName+".csv");
			
			StringRepresentation result = new StringRepresentation(csvBuilder, MediaType.APPLICATION_OCTET_STREAM);
			result.setDisposition(attatchment);
			
			return result;
			
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	
	
	
	private static String formatProperty(FL_Property prop) {
		
		PropertyHelper property = PropertyHelper.from(prop);
		
		if (property.hasTag(FL_PropertyTag.DATE)) {
			return csvDateFormat.format(DateTimeParser.fromFL(property.getValue()).toDate());
		}

		return String.valueOf(property.getValue());
	}
}
