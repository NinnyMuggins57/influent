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
import influent.server.utilities.UISerializationHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import oculus.aperture.common.rest.ApertureServerResource;

import org.apache.avro.AvroRemoteException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;

import com.google.inject.Inject;

public class EntitySearchParamsResource extends ApertureServerResource {

	private final FL_EntitySearch searcher;
	
	@Inject 
	public EntitySearchParamsResource (FL_EntitySearch searcher) {
		this.searcher = searcher;	
	}

	@Get
	public StringRepresentation propertyList () throws ResourceException{
		
		try {
			JSONObject props = new JSONObject();
			Map<String, List<FL_PropertyDescriptor>> descriptions = searcher.getDescriptors();
			
			if (descriptions == null)
				descriptions = new HashMap<String,List<FL_PropertyDescriptor>>();
	
			Form form = getRequest().getResourceRef().getQueryAsForm();
			String queryId = form.getFirstValue("queryId").trim();
			
			JSONArray typearr = new JSONArray();
			for (String key : descriptions.keySet()) {
				JSONArray ja = new JSONArray();
				for (FL_PropertyDescriptor flpd : descriptions.get(key)) {
					ja.put(UISerializationHelper.toUIJson(flpd));
				}
				JSONObject jo = new JSONObject();
				jo.put("type",key);
				jo.put("propertyDescriptors",ja);
				typearr.put(jo);
			}
			
			props.put("data", typearr);
			props.put("queryId", queryId);
			
			return new StringRepresentation(props.toString(), MediaType.APPLICATION_JSON);
		} catch (AvroRemoteException ae) {
			throw new ResourceException(ae);
		} catch (JSONException e) {
			throw new ResourceException(e);
		}
	}
	
}