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
package influent.server.clustering.utils;

import influent.idl.FL_Cluster;
import influent.idl.FL_DistributionRange;
import influent.idl.FL_Entity;
import influent.idl.FL_EntityTag;
import influent.idl.FL_Frequency;
import influent.idl.FL_GeoData;
import influent.idl.FL_Geocoding;
import influent.idl.FL_Property;
import influent.idl.FL_PropertyTag;
import influent.idl.FL_PropertyType;
import influent.idl.FL_RangeType;
import influent.idl.FL_Uncertainty;
import influent.idlhelper.ClusterHelper;
import influent.idlhelper.EntityHelper;
import influent.idlhelper.PropertyHelper;
import influent.idlhelper.SingletonRangeHelper;
import influent.server.clustering.ClusterContext;
import influent.server.clustering.ClusterDistributionProperty;
import influent.server.utilities.IdGenerator;
import influent.server.utilities.TypedId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.avro.AvroRemoteException;

public class EntityClusterFactory {
	
	protected final FL_Geocoding geocoder;
	protected final IdGenerator idGenerator;
	protected final List<ClusterDistributionProperty> distProperties = new ArrayList<ClusterDistributionProperty>();
	
	private class Distribution {
		public final Map<String, Double> frequencies = new HashMap<String, Double>();
		public final Set<FL_PropertyTag> tags = new HashSet<FL_PropertyTag>();
	}
	
	public EntityClusterFactory(IdGenerator idGen, FL_Geocoding geocoding, PropertyManager pMgr) {
		this.idGenerator = idGen;
		this.geocoder = geocoding;
		initDistProperties(pMgr);
	}
	
	private void initDistProperties(PropertyManager pMgr) {
		String[] properties = pMgr.getProperty(PropertyManager.CLUSTER_PROPERTIES, "TYPE:type-dist,GEO:location-dist").split(",");
		
		for (String property : properties) {
			String[] tokens = property.split(":");
			distProperties.add( new ClusterDistributionProperty(tokens[0], tokens[1]) );
		}
	}
	
	protected static void increment(String key, double increment, Map<String, Double> index) {
		double count = 0;
		
		if (index.containsKey(key)) {
			count = index.get(key);
		}
		count += increment;
		index.put(key, count);
	}
	
	protected static void increment(List<FL_Frequency> stats, Map<String, Double> index) {
		for (FL_Frequency freq : stats) {
			String key = "";
			if (freq.getRange() instanceof FL_GeoData) {
				key = ((FL_GeoData)freq.getRange()).getCc();
			}
			else {
				key = (String)freq.getRange();
			}
			double count = freq.getFrequency();
			increment(key, count, index);
		}
	}
	
	protected String getRepresentativeLabel(List<FL_Entity> entities, List<FL_Cluster> clusters) {
		// if we have entities pick the first label we find
		if (entities != null) {
			for (FL_Entity entity : entities) {
				PropertyHelper labelProp = EntityHelper.getFirstPropertyByTag(entity, FL_PropertyTag.LABEL);
				if (labelProp != null) {
					return (String)labelProp.getValue();
				}
			}
		}
		else if (clusters != null) { // otherwise, if we have clusters then pick the first label we find
			for (FL_Cluster cluster : clusters) {
				PropertyHelper labelProp = ClusterHelper.getFirstPropertyByTag(cluster, FL_PropertyTag.LABEL);
				if (labelProp != null) {
					return (String)labelProp.getValue();
				}
			}
		}
		return "";
	}
	
	protected PropertyHelper getFirstProperty(FL_Entity entity, String tagOrName) {
		PropertyHelper prop = null;
		FL_PropertyTag tag = null;
		
		try {
			tag = FL_PropertyTag.valueOf(tagOrName);
		}
		catch (Exception e) { }
		
		if (tag != null) {
			prop = EntityHelper.getFirstPropertyByTag(entity, tag);
		}
		if (prop == null) {
			prop = EntityHelper.getFirstProperty(entity, tagOrName);
		}
		return prop;
	}
	
	private Map<String, Distribution> initDistSummaries() {
		Map<String, Distribution> distSummaries = new HashMap<String, Distribution>();
		
		for (ClusterDistributionProperty distProp : this.distProperties) {
			distSummaries.put(distProp.clusterFieldName, new Distribution());
		}
		
		return distSummaries;
	}
	
	private void updateDistSummaries(FL_Entity entity, Map<String, Distribution> distSummaries) {
		for (ClusterDistributionProperty distProp : this.distProperties) {
			PropertyHelper prop = getFirstProperty(entity, distProp.entityTagOrFieldName);
			if (prop != null) {
				Object value = prop.getValue();
				if (value != null) {
					Distribution distribution = distSummaries.get(distProp.clusterFieldName);
					distribution.tags.addAll( prop.getTags() );
					
					if (value instanceof FL_GeoData) {
						String cc = ((FL_GeoData)value).getCc();
						if (cc != null && !cc.isEmpty()) {
							increment(cc, 1, distribution.frequencies);
						}
					}
					else if (value instanceof String) {
						increment( (String)value, 1, distribution.frequencies );
					}
					// all other value types are ignored
				}
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	private void updateDistSummaries(FL_Cluster cluster, Map<String, Distribution> distSummaries) {
		for (ClusterDistributionProperty distProp : this.distProperties) {
			PropertyHelper prop = ClusterHelper.getFirstProperty(cluster, distProp.clusterFieldName);
			if (prop != null) {
				Distribution distribution = distSummaries.get(distProp.clusterFieldName);
				
				distribution.tags.addAll( prop.getTags() );
				increment( (List<FL_Frequency>)prop.getValue(), distribution.frequencies );
			}
		}
	}
	
	protected List<FL_Property> createDistProperties(Map<String, Distribution> distSummaries) {
		List<FL_Property> properties = new ArrayList<FL_Property>(distSummaries.size());
		
		for (String fieldName : distSummaries.keySet()) {
			Distribution distribution = distSummaries.get(fieldName);
			
			List<FL_Frequency> freqs = new ArrayList<FL_Frequency>();
			boolean isGeo = distribution.tags.contains(FL_PropertyTag.GEO);
			FL_PropertyType type = isGeo ? FL_PropertyType.GEO : FL_PropertyType.STRING;
			
			for (String key : distribution.frequencies.keySet()) {
				Object range = key;
				if (isGeo) {
					FL_GeoData geo = new FL_GeoData(null, null, null, key);
					try {
						geocoder.geocode(Collections.singletonList(geo));
					} catch (AvroRemoteException e) { /* ignore */ }
					range = geo;
				}
				freqs.add( new FL_Frequency(range, distribution.frequencies.get(key)) );
			}
			
			FL_DistributionRange range = new FL_DistributionRange(freqs, FL_RangeType.DISTRIBUTION, type, false);
			FL_Property dist = new FL_Property(fieldName, fieldName, range, null, null, new ArrayList<FL_PropertyTag>(distribution.tags));
			properties.add(dist);
		}
			
		return properties;
	}
	
	public void updateClusterProperties(Map<String, FL_Cluster> clusters, ClusterContext context) {
		updateClusterProperties(clusters, context.entities, context.clusters);
	}
	
	public void updateClusterProperties(Map<String, FL_Cluster> clusters, Map<String, FL_Entity> relatedEntities, Map<String, FL_Cluster> relatedClusters) {
		for (String id : clusters.keySet()) {
			FL_Cluster cluster = clusters.get(id);
			updateClusterProperties(cluster, relatedEntities, relatedClusters);
		}
	}
	
	public void updateClusterProperties(FL_Cluster cluster, Map<String, FL_Entity> relatedEntities, Map<String, FL_Cluster> relatedClusters) {
		List<FL_Entity> members = new ArrayList<FL_Entity>(cluster.getMembers().size());
		List<FL_Cluster> subClusters = new ArrayList<FL_Cluster>(cluster.getSubclusters().size());
		
		// Find all the member entities for this cluster
		for (String memberId : cluster.getMembers()) {
			FL_Entity member = relatedEntities.get(memberId);

			if(member == null) continue;
			members.add(member);
		}
		
		// Find all the sub clusters for this cluster
		// depth first search update sub clusters properties
		for (String subClusterId : cluster.getSubclusters()) {
			FL_Cluster subCluster = relatedClusters.get(subClusterId);
			subClusters.add(subCluster);
			updateClusterProperties(subCluster, relatedEntities, relatedClusters);
		}
		
		// compute the new cluster properties
		List<FL_Property> updatedProps = computeClusterProperties(members, subClusters);
		
		// update the cluster properties - only replace the updated properties
		for (FL_Property updatedProp : updatedProps) {
			FL_Property oldProp = ClusterHelper.getFirstProperty(cluster, updatedProp.getKey());
			
			if (oldProp == null) {
				// property doesn't exist yet - add it to cluster
				cluster.getProperties().add(updatedProp);
			} else {
				// otherwise update the range
				oldProp.setRange(updatedProp.getRange());
			}
		}
	}
	
	protected List<FL_Property> computeClusterProperties(List<FL_Entity> members, List<FL_Cluster> subClusters) {
		List<FL_Property> properties = new LinkedList<FL_Property>();
		
		// initialize decendentCount to number of entities - if there are any
		long decendentCount = members == null ? 0 : members.size();
		long outDegree = -1;
		long inDegree = -1;
		double avgConfidence = 0;
				
		Map<String, Distribution> distSummaries = initDistSummaries();
		
		// first process the entities
		if (members != null) {
			for (FL_Entity entity : members) {
				updateDistSummaries(entity, distSummaries);
				
				PropertyHelper inFlow = EntityHelper.getFirstPropertyByTag(entity, FL_PropertyTag.INFLOWING);
				if (inFlow != null) {
					inDegree += (Long)inFlow.getValue();
				}
				PropertyHelper outFlow = EntityHelper.getFirstPropertyByTag(entity, FL_PropertyTag.OUTFLOWING);
				if (outFlow != null) {
					outDegree += (Long)outFlow.getValue();
				}
				
				FL_Uncertainty pConfidence = entity.getUncertainty();
				double confidence = pConfidence != null? pConfidence.getConfidence() : 1.0;
				
				avgConfidence += confidence;
			}
		}
		
		// next process the sub clusters
		if (subClusters != null) {
			for (FL_Cluster cluster : subClusters) {
				long count = ((Number)ClusterHelper.getFirstProperty(cluster, "count").getValue()).longValue(); 
				decendentCount += count;
				
				PropertyHelper inFlow = ClusterHelper.getFirstPropertyByTag(cluster, FL_PropertyTag.INFLOWING);
				if (inFlow != null) {
					inDegree += (Long)inFlow.getValue();
				}
				
				PropertyHelper outFlow = ClusterHelper.getFirstPropertyByTag(cluster, FL_PropertyTag.OUTFLOWING);
				if (outFlow != null) {
					outDegree += (Long)outFlow.getValue();
				}
			
				updateDistSummaries(cluster, distSummaries);
			
				FL_Uncertainty pConfidence = cluster.getUncertainty();
				double confidence = pConfidence != null? pConfidence.getConfidence() : 1.0;
			
				avgConfidence += count * confidence;
			}
		}
		
		if (decendentCount < 1) decendentCount = 1;
		
		avgConfidence /= decendentCount;
		
		// Create mandatory cluster properites
		FL_Property p = new PropertyHelper(
				"count",
				"count",
				decendentCount,
				FL_PropertyType.LONG,
				FL_PropertyTag.STAT);
		properties.add(p);
		
		if (inDegree > -1) {
			p = new PropertyHelper(
					"inDegree",
					"inDegree",
					(inDegree + 1),
					FL_PropertyType.LONG,
					FL_PropertyTag.INFLOWING);
			properties.add(p);
		}
		
		if (outDegree > -1) {
			p = new PropertyHelper(
					"outDegree",
					"outDegree",
					(outDegree + 1),
					FL_PropertyType.LONG,
					FL_PropertyTag.OUTFLOWING);
			properties.add(p);
		}
		
		// compute the representative label for the cluster and set as name
		String label = getRepresentativeLabel(members, subClusters);
		p = new PropertyHelper(
				"name",
				"name",
				label,
				FL_PropertyType.STRING,
				FL_PropertyTag.TEXT);
		properties.add(p);
		
		// Add the cluster distribution properties
		properties.addAll( createDistProperties(distSummaries) );

		// add a confidence property
		p = new PropertyHelper(
				"confidence",
				"confidence",
				avgConfidence,
				FL_PropertyType.DOUBLE,
				FL_PropertyTag.STAT);
		properties.add(p);
		
		return properties;
	}

	public FL_Cluster toCluster(List<FL_Entity> entities, List<FL_Cluster> subClusters) {
		String clusterId = idGenerator.nextId();
		
		List<String> childEntityIds = new LinkedList<String>();
		List<String> childClusterIds = new LinkedList<String>();
		
		// create the cluster entity properties
		List<FL_Property> properties = computeClusterProperties(entities, subClusters); 
		
		// add entity members if there are any
		if (entities != null) {
			for (FL_Entity entity : entities) {
				childEntityIds.add(entity.getUid());
			}
		}
		
		// add sub clusters if there are any
		if (subClusters != null) {
			for (FL_Cluster cluster : subClusters) {
//				cluster.setParent(clusterId);  // sub cluster is now a child of new cluster
				childClusterIds.add(cluster.getUid());
			}
		}
		
		// retrieve the cluster label and average confidence from the properties
		String label = "";
		double avgConfidence = 0;
		for (FL_Property prop : properties) {
			if (prop.getKey().equals("confidence")) {
				avgConfidence = (Double)PropertyHelper.from(prop).getValue();
			} else if (prop.getKey().equals("name")) {
				label = (String)PropertyHelper.from(prop).getValue();
			}
		}

		// create the entity cluster and return
		ClusterHelper ch = new ClusterHelper(clusterId, 
											 label,
											 FL_EntityTag.CLUSTER,
											 properties,
											 new LinkedList<String>(childEntityIds),
											 new LinkedList<String>(childClusterIds),
											 null,
											 null,
											 -1);
		ch.setUncertainty(FL_Uncertainty.newBuilder().setConfidence(avgConfidence).build());
		
		return ch;
	}
	
	public FL_Cluster toAccountOwnerSummary(FL_Entity owner, List<FL_Entity> accounts, List<FL_Cluster> accountClusters) {
		FL_Cluster ownerCluster = this.toCluster(accounts, accountClusters);
		
		String rawId = TypedId.fromTypedId(owner.getUid()).getNativeId();
		String namespace = TypedId.fromTypedId(owner.getUid()).getNamespace();
		
		ownerCluster.setUid( TypedId.fromNativeId(TypedId.ACCOUNT_OWNER, namespace, rawId).getTypedId() );
		ownerCluster.getTags().add(FL_EntityTag.ACCOUNT_OWNER);
		
		FL_Property label = ClusterHelper.getFirstPropertyByTag(ownerCluster, FL_PropertyTag.LABEL);
		label.setRange( new SingletonRangeHelper( EntityHelper.getFirstPropertyByTag(owner, FL_PropertyTag.LABEL).getValue(), FL_PropertyType.STRING) );
		
		for (FL_Cluster cluster : accountClusters) {
			cluster.setParent(ownerCluster.getUid());
			cluster.setRoot(ownerCluster.getUid());
		}
		
		return ownerCluster;
	}
}
