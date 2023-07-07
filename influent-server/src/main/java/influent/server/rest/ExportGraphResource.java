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

import influent.server.spi.ExportDataService;

import java.util.Collections;
import java.util.Map;

import javax.xml.bind.JAXBException;

import oculus.aperture.capture.phantom.data.ProcessedTaskInfo;
import oculus.aperture.common.rest.ApertureServerResource;
import oculus.aperture.spi.store.ConflictException;
import oculus.aperture.spi.store.ContentService.DocumentDescriptor;

import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.data.CacheDirective;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.name.Named;

public class ExportGraphResource extends ApertureServerResource {

	private final ExportDataService service;
	private final int maxCacheAge;

	
	
	@Inject
	public ExportGraphResource(
		ExportDataService service,
		@Named("influent.charts.maxage") Integer maxCacheAge
	) {
		this.service = service;
		this.maxCacheAge = maxCacheAge;
	}
	
	
	
	
	@Post
	public Representation getCapturedImage(String jsonData) throws ResourceException {
	
		try {
			JSONObject jsonObj = new JSONObject(jsonData);

			// Get the persistence data
			JSONObject data = new JSONObject(jsonObj.getString("data").trim());
			
			Representation rep = executeTask(data);
			
			getResponse().setCacheDirectives(
				Collections.singletonList(
					CacheDirective.maxAge(maxCacheAge)
				)
			);

			if (rep == null) {
				throw new ResourceException(
					Status.SERVER_ERROR_INTERNAL,
					"XML processing failed to complete for an unknown reason."
				);
			}
			
			return rep;
		
		} catch (Exception e) {
			throw new ResourceException(
				Status.CLIENT_ERROR_BAD_REQUEST,
				"Unable to create JSON object from supplied options string",
				e
			);
		}
	}
	
	
	

	private Representation executeTask(JSONObject data) throws ConflictException, JSONException, JAXBException {

		// execute
		DocumentDescriptor taskInfo = service.exportToAnb(data);

		Map<String,Object> response = Maps.newHashMap();
		
		// process result.
		if (taskInfo != ProcessedTaskInfo.NONE) {
			
			// Return a response containing a JSON block with the id/rev
			response.put("ok", true); 
			response.put("id", taskInfo.getId());
			response.put("store", taskInfo.getStore());
			
	
			// get the resource URI from this post
			String resourceUri = getRootRef().toString() 
				+ "/cms/" + Reference.encode(taskInfo.getStore()) 
				+ "/" + Reference.encode(taskInfo.getId());
			
			// if have a revision append it.
			if (taskInfo.getRevision() != null) {
				response.put("rev", taskInfo.getRevision());
				resourceUri += "?rev=" + Reference.encode(taskInfo.getRevision());
			}
			
			// Return the resulting image in the location header
			getResponse().setLocationRef(new Reference(resourceUri));
			
		} else {
			response.put("ok", false); 
		}

		// Return a JSON response
		return new JsonRepresentation(response);
	}
}
