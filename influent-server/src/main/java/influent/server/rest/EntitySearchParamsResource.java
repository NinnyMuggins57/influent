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

import influent.idl.FL_EntitySearch;
import influent.idl.FL_PropertyDescriptor;
import influent.idl.FL_TypeDescriptor;
import influent.server.utilities.UISerializationHelper;

import java.util.ArrayList;
import java.util.List;

import oculus.aperture.common.rest.ApertureServerResource;

import org.apache.avro.AvroRemoteException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.data.MediaType;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;

import com.google.inject.Inject;

public class EntitySearchParamsResource extends ApertureServerResource {

	private final FL_EntitySearch searcher;
	
	
	
	@Inject 
	public EntitySearchParamsResource (FL_EntitySearch searcher) {
		this.searcher = searcher;	
	}
	
	
	
	
	@Post("json")
	public StringRepresentation propertyList (String jsonData) throws ResourceException{
		
		try {
			JSONObject jo = new JSONObject();

			List<FL_PropertyDescriptor> properties = searcher.getDescriptors().getProperties();
			List<FL_TypeDescriptor> types = searcher.getDescriptors().getTypes();

			if (properties == null)
				properties = new ArrayList<FL_PropertyDescriptor>();
			if (types == null)
				types = new ArrayList<FL_TypeDescriptor>();

            JSONArray propertyJSON = new JSONArray();
            for (FL_PropertyDescriptor pd : properties) {
	            propertyJSON.put(UISerializationHelper.toUIJson(pd));
            }

			JSONArray typeJSON = new JSONArray();
			for (FL_TypeDescriptor td : types) {
				typeJSON.put(UISerializationHelper.toUIJson(td));
			}

			jo.put("properties", propertyJSON);
			jo.put("types", typeJSON);

			return new StringRepresentation(jo.toString(), MediaType.APPLICATION_JSON);
		} catch (AvroRemoteException ae) {
			throw new ResourceException(ae);
		} catch (JSONException e) {
			throw new ResourceException(e);
		}
	}
}