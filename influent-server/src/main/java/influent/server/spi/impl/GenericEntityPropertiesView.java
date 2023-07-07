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
package influent.server.spi.impl;

import influent.idl.FL_Entity;
import influent.idl.FL_GeoData;
import influent.idl.FL_Property;
import influent.idl.FL_PropertyTag;
import influent.idlhelper.EntityHelper;
import influent.idlhelper.PropertyHelper;
import influent.server.dataaccess.DataAccessException;
import influent.server.spi.EntityPropertiesViewService;

import java.awt.Dimension;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;



public class GenericEntityPropertiesView implements EntityPropertiesViewService {
	
	private final String defaultIconUrl = "rest/icon/aperture-hscb/Person?iconWidth=32&amp;iconHeight=32";
	private final DateTimeFormatter date_formatter = DateTimeFormat.forPattern("dd MMM yyyy");

	protected String getImageUrl(FL_Entity entity, int index){
		// Get the image id.
		
		int count = 0;
		PropertyHelper imagep = null;
		
		for (FL_Property property : entity.getProperties()) {
			if (property.getTags().contains(FL_PropertyTag.IMAGE)) {
				if (count == index) {
					imagep = PropertyHelper.from(property);
					break;
				}
				count++;
			}
		}		
		
		// If a non-valid, zero id is returned, use the default image.
		if (imagep == null || imagep.getValue() == null || imagep.getValue().toString().trim().isEmpty()) {
			return null;
		}
		return imagep.getValue().toString();
	}
	
	protected int getImagesCount(FL_Entity entity){
		// Get the image id.
		
		int count = 0;
		for (FL_Property property : entity.getProperties()) {
			if (property.getTags().contains(FL_PropertyTag.IMAGE)) {
				count++;
			}
		}		
		return count;
	}	
	
	protected Dimension getImageBoxSize() {
		return new Dimension(125, 50);
	}

	protected String getIconUrl(FL_Entity entity, int width, int height){
		return defaultIconUrl;
	}

	protected void insertRow(String friendlyText, String value, StringBuilder builder){
		builder.append("		<tr>"); 
		builder.append("			<td class='propertyName'>" + friendlyText + ":</td>");
		builder.append("			<td class='propertyValue'>" + value + "</td>");
		builder.append("		</tr>");
	}
	
	protected void processProperties(List<FL_Property> properties) { }
	
	@Override
	public String getContent(FL_Entity entity, int imageIdx) throws DataAccessException {
		PropertyHelper labelp = EntityHelper.getFirstPropertyByTag(entity, FL_PropertyTag.NAME);
		if (labelp == null) {
			labelp = EntityHelper.getFirstPropertyByTag(entity, FL_PropertyTag.LABEL);
		}
		
		
		String label = labelp.getValue().toString();

		// If we have more than one image, wrap imageCount around in both directions
		int imageCount = getImagesCount(entity);
		imageIdx = imageCount == 0 ? 0 : (imageIdx % imageCount + imageCount) % imageCount;
			
		//Construct the HTML.
		StringBuilder html = new StringBuilder();		
		String imageUrl = getImageUrl(entity, imageIdx);
		Dimension imageDim = getImageBoxSize();
		
		List<FL_Property> properties = entity.getProperties();
		processProperties(properties);


		html.append("<div class='detailsHeader'>");
		html.append("	<div id='detailsHeaderInfo'><span class='detailsTitle'>Account Details</span> ");
		html.append("		<div class='detailsEntityLabel'>"); 
		html.append("			<b>"+label+"</b>");

		html.append("		</div>");
		html.append("	</div>");
		html.append("	<div id='detailsHeaderPhoto' style='width:"+imageDim.width+"px; height:"+imageDim.height+"px;'>");

		if (imageUrl != null) {
			html.append(" 		<div style='float: right; clear: left;'>");
			html.append("			<a href='" + imageUrl + "' target='_blank'><img src='" + imageUrl + "' style='image-rendering:optimizeQuality;' border='0'></a>");
			html.append("		</div>");
			
			if (imageCount > 1) {
					
				html.append("		<div class='photoCarouselControls'>");
				html.append("			<div class='photoCarouselLabel'>" + (imageIdx + 1) + " of " + imageCount + "</div>");
				html.append("		</div>");
			}
		}
		
		html.append("	</div>");
		html.append("</div>");
		html.append("<div id='detailsBody'>");
		html.append("	<table class='propertyTable' style='bottom: 2px; left: 0px;'><tbody>");

		insertRow("uid", entity.getUid(), html);
		
		for (FL_Property prop : properties){
			if(prop.getTags().contains(FL_PropertyTag.IMAGE)) {
				continue;
			}
			
			PropertyHelper property = PropertyHelper.from(prop);
			
			String friendlyText = property.getFriendlyText();
			String friendlyValue = getFriendlyValue(property);
			
			insertRow(friendlyText, friendlyValue, html);
		}			
		html.append("	</tbody></table>");
		html.append("</div>");
		
		String str = html.toString();
		str = str.replace("\\r", "");
		str = str.replace("\\n", "<br>");
		
		return str;
	}
	
	protected String getFriendlyValue(FL_Property property) {	// can be overridden if a particular implementation desires a specific format for certain prop types
		String toReturn = new String();
		PropertyHelper propertyHelper = PropertyHelper.from(property);
		Object propObj = propertyHelper.getValue();
		
		switch(propertyHelper.getType()) {
			case DATE:
				toReturn = propObj.toString();
				if (toReturn != null && !toReturn.isEmpty()) {
					DateTime dateTime = new DateTime(Long.parseLong(toReturn));
					toReturn = dateTime.toString(date_formatter);
				}
				else {
					toReturn = "Date Format Error";
				}
			break;
			case GEO:
				StringBuilder geoBuilder = new StringBuilder();
				FL_GeoData geoData = (FL_GeoData) propObj;
				
				if (geoData.getLat() != null) {
					geoBuilder.append(geoData.getLat());
					geoBuilder.append(", ");
					geoBuilder.append(geoData.getLon());
				}
				
				if (geoData.getText() != null && !geoData.getText().equals(geoData.getCc())) {
					geoBuilder.append(" ");
					geoBuilder.append(geoData.getText());
				}
				
				if (geoData.getCc() != null) {
					if (geoBuilder.length() > 0) {
						geoBuilder.append(" (");
						geoBuilder.append(geoData.getCc());
						geoBuilder.append(')');
					} else {
						geoBuilder.append(geoData.getCc());
					}
				}
				
				toReturn = geoBuilder.toString();
			break;
			default:
				toReturn = propObj.toString();
			break;
		}
		
		return toReturn;
	}
}
