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

import influent.idl.*;
import influent.server.dataaccess.DataAccessException;
import influent.server.spi.EntityPropertiesViewService;

import influent.server.utilities.UISerializationHelper;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;


public class GenericEntityPropertiesView implements EntityPropertiesViewService {
	
	//private final DateTimeFormatter date_formatter = DateTimeFormat.forPattern("dd MMM yyyy");

	@Override
	public JSONObject getContent(FL_Entity entity) throws DataAccessException {
        // TODO: Make a copy of the entity and override it with friendly values?
        try {

            return UISerializationHelper.toUIJson(entity);

        } catch (JSONException e) {
            throw new ResourceException(
                    Status.CLIENT_ERROR_BAD_REQUEST,
                    "Unable to create JSON object from supplied options string",
                    e
            );
        }
	}

	/*
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
	*/
}
