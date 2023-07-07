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
package influent.idlhelper;

import influent.idl.FL_Cluster;
import influent.idl.FL_Entity;
import influent.idl.FL_EntityTag;
import influent.idl.FL_Property;
import influent.idl.FL_PropertyTag;
import influent.idl.FL_Provenance;
import influent.idl.FL_Uncertainty;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ClusterHelper extends FL_Cluster {
	public ClusterHelper(String id, List<FL_EntityTag> tagList, FL_Provenance provenance, FL_Uncertainty uncertainty, List<FL_Property> properties, List<java.lang.String> members, List<java.lang.String> subclusters, String parent, String root, Integer level) {
		super(id, new ArrayList<FL_EntityTag>(tagList), provenance, uncertainty, new ArrayList<FL_Property>(properties), new ArrayList<String>(members), new ArrayList<String>(subclusters), parent, root, level, 1);
	}

	public ClusterHelper(String id, String label, List<FL_EntityTag> tagList, List<FL_Property> properties, List<java.lang.String> members, List<java.lang.String> subclusters, String parent, String root, Integer level) {
		this(id, tagList, null, null, merge(properties, Collections.singletonList((FL_Property)new PropertyHelper(FL_PropertyTag.LABEL, label))),
				members, subclusters, parent, root, level);
	}

	public ClusterHelper(String id, String label, FL_EntityTag tag, List<FL_Property> properties, List<java.lang.String> members, List<java.lang.String> subclusters, String parent, String root, Integer level) {
		this(id, label, Collections.singletonList(tag), properties, members, subclusters, parent, root, level);
	}

	public ClusterHelper(String id, String label, FL_EntityTag tag, List<FL_Property> properties, String parent, String root, Integer level) {
		this(id, label, Collections.singletonList(tag), properties, new ArrayList<String>(), new ArrayList<String>(), parent, root, level);
	}
	
	public PropertyHelper getFirstProperty(String key) {
		for (FL_Property property : getProperties()) {
			if (property.getKey().equals(key)) return PropertyHelper.from(property);
		}
		return null;
	}

	private static List<FL_Property> merge(List<FL_Property> list1, List<FL_Property> list2) {
		List<FL_Property> merged = new ArrayList<FL_Property>(list1);
		merged.addAll(list2);
		return merged;
	}
	
	public String getId() {
		return (String)getUid();
	}

	public String getLabel() {
		PropertyHelper label = getFirstProperty(FL_PropertyTag.LABEL.name());
		return (String) (label != null ? label.getValue() : null); 
	}
	
	public boolean isEmpty(FL_Cluster cluster) {
		return cluster.getMembers().isEmpty() && cluster.getSubclusters().isEmpty();
	}
	
	public String toJson() throws IOException {
		return SerializationHelper.toJson(this);
	}
	
	public static String toJson(FL_Cluster cluster) throws IOException {
		return SerializationHelper.toJson(cluster);
	}
	
	public static String toJson(List<FL_Cluster> clusters) throws IOException {
		return SerializationHelper.toJson(clusters, FL_Cluster.getClassSchema());
	}
	
	public static String toJson(Map<String, List<FL_Cluster>> clusters) throws IOException {
		return SerializationHelper.toJson(clusters, FL_Cluster.getClassSchema());
	}
	
	public static FL_Cluster fromJson(String json) throws IOException {
		return SerializationHelper.fromJson(json, FL_Cluster.getClassSchema());
	}
	
	public static List<FL_Cluster> listFromJson(String json) throws IOException {
		return SerializationHelper.listFromJson(json, FL_Cluster.getClassSchema());
	}
	
	public static Map<String,List<FL_Cluster>> mapFromJson(String json) throws IOException {
		return SerializationHelper.mapFromJson(json, FL_Cluster.getClassSchema());
	}
	
	public static PropertyHelper getFirstProperty(FL_Cluster cluster, String key) {
		for (FL_Property property : cluster.getProperties()) {
			if (property.getKey().equals(key)) return PropertyHelper.from(property);
		}
		return null;
	}
	
	public static PropertyHelper getFirstPropertyByTag(FL_Cluster cluster, FL_PropertyTag tag) {
		for (FL_Property property : cluster.getProperties()) {
			if (property.getTags().contains(tag)) return PropertyHelper.from(property);
		}
		return null;
	}
	
	public static void incrementVersion(FL_Cluster cluster) {
		Integer version = cluster.getVersion() + 1;
		cluster.setVersion(version);
	}
	
	public static void addMemberById(FL_Cluster cluster, String entityId) {
		cluster.getMembers().add(entityId);
		// increment the cluster version number
		incrementVersion(cluster);
	}
	
	public static void addMembersById(FL_Cluster cluster, List<String> entityIds) {
		cluster.getMembers().addAll(entityIds);
		// increment the cluster version number
		incrementVersion(cluster);
	}
	
	public static void addMember(FL_Cluster cluster, FL_Entity entity) {
		_addMember(cluster, entity);
		// increment the cluster version number
		incrementVersion(cluster);
	}
	
	private static void _addMember(FL_Cluster cluster, FL_Entity entity) {
		cluster.getMembers().add(entity.getUid());
	}
	
	public static void addMembers(FL_Cluster cluster, List<FL_Entity> entities) {
		for (FL_Entity entity : entities) {
			_addMember(cluster, entity);
		}
		// increment the cluster version number
		incrementVersion(cluster);
	}
	
	public static void addSubClusterById(FL_Cluster cluster, String subClusterId) {
		cluster.getSubclusters().add(subClusterId);
		// increment the cluster version number
		incrementVersion(cluster);
	}
	
	public static void addSubClustersById(FL_Cluster cluster, List<String> subClusterIds) {
		cluster.getSubclusters().addAll(subClusterIds);
		// increment the cluster version number
		incrementVersion(cluster);
	}
	
	public static void addSubCluster(FL_Cluster cluster, FL_Cluster subCluster) {
		_addSubCluster(cluster, subCluster);
		// increment the cluster version number
		incrementVersion(cluster);
	}
	
	private static void _addSubCluster(FL_Cluster cluster, FL_Cluster subCluster) {
		cluster.getSubclusters().add(subCluster.getUid());
	}
	
	public static void addSubClusters(FL_Cluster cluster, List<FL_Cluster> subClusters) {
		for (FL_Cluster subCluster : subClusters) {
			_addSubCluster(cluster, subCluster);
		}
		// increment the cluster version number
		incrementVersion(cluster);
	}
	
	public static void removeMemberById(FL_Cluster cluster, String entityId) {
		cluster.getMembers().remove(entityId);
		// increment the cluster version number
		incrementVersion(cluster);
	}
	
	public static void removeMembersById(FL_Cluster cluster, List<String> entityIds) {
		cluster.getMembers().removeAll(entityIds);
		// increment the cluster version number
		incrementVersion(cluster);
	}
	
	public static void removeMember(FL_Cluster cluster, FL_Entity entity) {
		_removeMember(cluster, entity);
		// increment the cluster version number
		incrementVersion(cluster);
	}
	
	private static void _removeMember(FL_Cluster cluster, FL_Entity entity) {
		cluster.getMembers().remove(entity.getUid());
	}
	
	public static void removeMembers(FL_Cluster cluster, List<FL_Entity> entities) {
		for (FL_Entity entity : entities) {
			_removeMember(cluster, entity);
		}
		// increment the cluster version number
		incrementVersion(cluster);
	}
	
	public static void removeSubClusterById(FL_Cluster cluster, String subClusterId) {
		cluster.getSubclusters().remove(subClusterId);
		// increment the cluster version number
		incrementVersion(cluster);
	}
	
	public static void removeSubClustersById(FL_Cluster cluster, List<String> subClusterIds) {
		cluster.getSubclusters().addAll(subClusterIds);
		// increment the cluster version number
		incrementVersion(cluster);
	}
	
	public static void removeSubCluster(FL_Cluster cluster, FL_Cluster subCluster) {
		_removeSubCluster(cluster, subCluster);
		// increment the cluster version number
		incrementVersion(cluster);
	}
	
	private static void _removeSubCluster(FL_Cluster cluster, FL_Cluster subCluster) {
		cluster.getSubclusters().remove(subCluster.getUid());
	}
	
	public static void removeSubClusters(FL_Cluster cluster, List<FL_Cluster> subClusters) {
		for (FL_Cluster subCluster : subClusters) {
			_removeSubCluster(cluster, subCluster);
		}
		// increment the cluster version number
		incrementVersion(cluster);
	}
	
}
