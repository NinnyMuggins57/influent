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
import influent.idl.FL_Entity;
import influent.idl.FL_LevelOfDetail;
import influent.server.dataaccess.DataAccessException;
import influent.server.dataaccess.DataAccessHelper;
import influent.server.spi.EntityPropertiesViewService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import oculus.aperture.common.rest.ApertureServerResource;

import org.apache.avro.AvroRemoteException;
import org.restlet.data.Form;
import org.restlet.resource.Get;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

public class EntityDetailsResource extends ApertureServerResource {

	private static final Logger s_logger = LoggerFactory.getLogger(EntityDetailsResource.class);

	private FL_DataAccess service;
	private EntityPropertiesViewService propView;

	
	@Inject 
	public EntityDetailsResource (FL_DataAccess service, EntityPropertiesViewService propView) {
		this.service = service;
		this.propView = propView;
	}
	
	
	
	
	@Get
	public Map<String, Object> getContent()  {
		Map<String, Object> props = new HashMap<String, Object> ();
		
		Form form = getRequest().getResourceRef().getQueryAsForm();
		String queryId = form.getFirstValue("queryId").trim();
		props.put("queryId", queryId);
		String entityId = form.getFirstValue("entityId").trim();

		try {
			String html = "";
			try {
				List<FL_Entity> entities = service.getEntities(DataAccessHelper.detailsSubject(entityId), FL_LevelOfDetail.FULL);
				if (entities != null && !entities.isEmpty()) {
					//html="<html><head>Found</head><body>Found entity for "+entityId+"</body></html>";
					html = propView.getContent(entities.get(0));
				}
				else{
					s_logger.error("Unable to look up entity (for details pane) " + entityId);
				}
			} catch (AvroRemoteException e) {
				e.printStackTrace();
			}
			
			props.put("content", html);
	
			//return new StringRepresentation(html, MediaType.TEXT_PLAIN);
			return props;
		} catch (DataAccessException e) {
			String html="<html><body>Couldn't find entity for "+entityId+"</body></html>";
			props.put("content", html);
			return props;
		}
	}
}
