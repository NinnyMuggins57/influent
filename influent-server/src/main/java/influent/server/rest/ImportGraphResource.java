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

import influent.server.spi.ImportDataService;

import java.util.List;

import javax.xml.bind.JAXBException;

import oculus.aperture.common.rest.ApertureServerResource;
import oculus.aperture.spi.store.ConflictException;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.ext.fileupload.RestletFileUpload;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class ImportGraphResource extends ApertureServerResource {

	private final ImportDataService service;
	
	
	
	@Inject
	public ImportGraphResource(
		ImportDataService service,
		@Named("influent.charts.maxage") Integer maxCacheAge
	) {
		this.service = service;
	}
	
	
	
	
	@Post
	public StringRepresentation getCapturedImage(Representation entity) throws ResourceException {
	
		if(entity == null || !MediaType.MULTIPART_FORM_DATA.equals(entity.getMediaType(), true)) return null;

		DiskFileItemFactory factory = new DiskFileItemFactory();
        factory.setSizeThreshold(1000240);
        RestletFileUpload upload = new RestletFileUpload(factory);

		try {
            List<FileItem> items = upload.parseRequest(getRequest());
            String xmlData = new String(items.get(0).get());
			
			JSONObject influentState = executeTask(xmlData); // hit the service to get the JSONObject
			
			if (influentState == null) {
				throw new ResourceException(
					Status.SERVER_ERROR_INTERNAL,
					"XML processing failed to complete for an unknown reason."
				);
			}
			
			return new StringRepresentation(influentState.toString(), MediaType.TEXT_PLAIN);
		
		} catch (Exception e) {
			throw new ResourceException(
				Status.CLIENT_ERROR_BAD_REQUEST,
				"Unable to create JSON object from supplied options string",
				e
			);
		}
	}
	
	
	

	private JSONObject executeTask(String xmlData) throws ConflictException, JSONException, JAXBException {
		return service.importFromXML(xmlData);
	}
}
