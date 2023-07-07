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

import influent.idl.FL_Cluster;
import influent.idl.FL_ClusteringDataAccess;
import influent.idl.FL_DataAccess;
import influent.idl.FL_Entity;
import influent.idl.FL_LevelOfDetail;
import influent.idlhelper.FileHelper;
import influent.server.clustering.utils.ClusterContextCache;
import influent.server.clustering.utils.ContextReadWrite;
import influent.server.clustering.utils.EntityClusterFactory;
import influent.server.clustering.utils.ClusterContextCache.PermitSet;
import influent.server.utilities.TypedId;
import influent.server.utilities.UISerializationHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import oculus.aperture.common.rest.ApertureServerResource;

import org.apache.avro.AvroRemoteException;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.data.MediaType;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

public class ModifyContextResource extends ApertureServerResource{
	
	private final EntityClusterFactory clusterFactory;
	private final FL_ClusteringDataAccess clusterAccess;
	private final FL_DataAccess entityAccess;
	private final ClusterContextCache contextCache;

	private static final Logger s_logger = LoggerFactory.getLogger(ModifyContextResource.class);

	@Inject
	public ModifyContextResource(
		FL_ClusteringDataAccess clusterAccess, 
		FL_DataAccess entityAccess,
		EntityClusterFactory clusterFactory,
		ClusterContextCache contextCache
	) {
		this.clusterAccess = clusterAccess;
		this.entityAccess = entityAccess;
		this.clusterFactory = clusterFactory;
		this.contextCache = contextCache;
	}
	
	@Post 
	public StringRepresentation modifyContext (String jsonData) throws ResourceException {
		
		try {
			JSONObject jsonObj = new JSONObject(jsonData);

			String erroneousArgument = null;
			String sessionId = jsonObj.getString("sessionId");
			String contextId = jsonObj.getString("contextId");
			String edit = jsonObj.getString("edit"); //Allowed values, 'insert', 'remove', 'create'
			String fileId = jsonObj.getString("fileId"); //Must be non-null
			List<String> childIds = UISerializationHelper.buildListFromJson(jsonObj, "childIds"); //Must be non-null for insert and remove
			
			if (sessionId == null) {
				erroneousArgument = "'sessionId'";
			}
			else if (contextId == null) {
				erroneousArgument = "'contextId'";
			}
			else if (fileId == null) {
				erroneousArgument = "'mainParentId'";
			}
			else {
				PermitSet permits = new PermitSet();
				
				try {
					// there are non-trivial ops like calls out to the db here while we have a lock on the context,
					// but the changes can be extensive. Safer for now to lock it for the complete op.
					final ContextReadWrite contextRW = contextCache.getReadWrite(contextId, permits);
					
					if ("insert".equals(edit)) {
						if (childIds == null || childIds.size() == 0) {
							erroneousArgument = "'childIds'";
						} else {
							insert(fileId, childIds, contextRW, sessionId);
							return new StringRepresentation("insert complete", MediaType.APPLICATION_JSON);
						}
					} else if ("remove".equals(edit)) {
						if (childIds == null || childIds.size() == 0) {
							erroneousArgument = "'childIds'";
						} else {
							remove(fileId, childIds, contextId, contextRW, sessionId);
							return new StringRepresentation("remove complete", MediaType.APPLICATION_JSON);
						}
					} else if ("create".equals(edit)) {
						create(fileId, contextRW, sessionId);
						return new StringRepresentation("create complete", MediaType.APPLICATION_JSON);
					}
					else {
						erroneousArgument = "edit";
					}
					
				} finally {
					permits.revoke();
				}
			}
			
			s_logger.error("Argument" + erroneousArgument + " is invalid; unable to modify context");
			return new StringRepresentation("", MediaType.APPLICATION_JSON);
		} catch (JSONException e) {
			throw new ResourceException(e);
		} catch (AvroRemoteException e) {
			throw new ResourceException(e);
		}
	}
	
	private FL_Cluster updateFileCluster(FL_Cluster file, List<FL_Entity> entities, List<FL_Cluster> clusters) throws AvroRemoteException {			
		Map<String, FL_Entity> relatedEntities = new HashMap<String, FL_Entity>();
		for (FL_Entity entity : entities) {
			relatedEntities.put(entity.getUid(), entity);
		}
		
		Map<String, FL_Cluster> relatedClusters = new HashMap<String, FL_Cluster>();
		for (FL_Cluster cluster : clusters) {
			relatedClusters.put(cluster.getUid(), cluster);
		}

		clusterFactory.updateClusterProperties(file, relatedEntities, relatedClusters);
		
		return file;
	}
	
	private void create(
		String fileId, 
		ContextReadWrite context,
		String sessionId
	) {
		FL_Cluster newFile = new FileHelper(fileId);
		
		context.merge(
			Collections.<FL_Cluster>singletonList(newFile), 
			Collections.<FL_Cluster>emptyList(), 
			Collections.<FL_Entity>emptyList(), 
			false, 
			true
		);
	}
	
	private void insert(
		String fileId, 
		List<String> childIds, 
		ContextReadWrite context,
		String sessionId
	) throws JSONException, AvroRemoteException {
		
		FL_Cluster file = context.getFile(fileId);
		
		if (file == null) {
			file = new FileHelper(fileId);
		}
		
		List<String> eIds = TypedId.filterTypedIds(childIds, TypedId.ACCOUNT);
		List<String> sIds = TypedId.filterTypedIds(childIds, TypedId.CLUSTER_SUMMARY);
		
		// Currently we don't support clusters but in the future we might
		// List<String> cIds = TypedId.filterTypedIds(childIds, TypedId.CLUSTER);
		
		// Only add entity ids that haven't already been added.
		for (String id : eIds){
			if (!file.getMembers().contains(id)){
				file.getMembers().add(id);
			}
		}
		
		// Only add cluster ids that haven't already been added.
		for (String id : sIds){
			if (!file.getSubclusters().contains(id)){
				file.getSubclusters().add(id);
			}
		}
		
		List<FL_Entity> entities = new ArrayList<FL_Entity>(context.getEntities(eIds));
		List<String> newEntities = new ArrayList<String>(eIds);

		for (FL_Entity entity : entities) {
			newEntities.remove(entity.getUid());
		}
		if (newEntities.size() > 0) {
			entities.addAll(entityAccess.getEntities(newEntities, FL_LevelOfDetail.SUMMARY));
		}
		
		List<FL_Cluster> clusters = new ArrayList<FL_Cluster>(context.getClusters(eIds));
		List<String> newClusters = new ArrayList<String>(sIds);
		for (FL_Cluster cluster : clusters) {
			newClusters.remove(cluster.getUid());
		}
		if (newClusters.size() > 0) {
			clusters.addAll(clusterAccess.getClusterSummary(newClusters));
		}
		
		file = updateFileCluster(file, entities, clusters);
		
		context.merge(Collections.<FL_Cluster>singletonList(file), clusters, entities, false, true);
	}
	
	private void remove(
		String fileId, 
		List<String> childIds, 
		String contextId,
		ContextReadWrite context,
		String sessionId
	) throws JSONException, AvroRemoteException {

		if (fileId.equals(contextId)) {
			clusterAccess.removeMembers(childIds, contextId, sessionId);
			context.remove(childIds);
		} else {
		
			FL_Cluster file;
			
			List<FL_Cluster> files = context.getFiles(Collections.<String>singletonList(fileId));
			
			if (files != null) {
				if (files.size() > 1) {
					throw new AvroRemoteException("cluster context cache contained more than one file with the same id");
				}
				
				if (files.size() > 0) {
					
					file = files.get(0);
					file.getMembers().removeAll(childIds);
					file.getSubclusters().removeAll(childIds);
					file = updateFileCluster(file, context.getEntities(file.getMembers()), context.getClusters(file.getSubclusters()));
				
					context.merge(Collections.<FL_Cluster>singletonList(file), Collections.<FL_Cluster>emptyList(), Collections.<FL_Entity>emptyList(), false, true);
				}
			}
		}
	}
}
