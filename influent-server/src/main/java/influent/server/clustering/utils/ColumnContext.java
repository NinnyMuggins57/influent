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
import influent.idl.FL_Entity;
import influent.server.clustering.ClusterContext;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;

/**
 * @author djonker
 */
public class ColumnContext implements Serializable, ContextReadWrite {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	// raw cluster hierarchy
	private ClusterContext _rawContext;
	
	// regular collections
	private Set<String> _memberIds;
	private Map<String, Boolean> _summarized;

	// avro maps
	private Map<String,FL_Cluster> _files;
	private Map<String,FL_Cluster> _clusters;
	private Map<String,FL_Entity> _entities;
	

	/**
	 * Override Java serialization methods.
	 * MAKE SURE THESE STAY IN SYNC WITH FIELDS ABOVE.
	 */
	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
		
		// write out raw cluster hierarchy and pojos
		out.writeObject(_rawContext);
		out.writeObject(_memberIds);
		out.writeObject(_summarized);
		
		// write out avro collections, which do not support Java serialization
		final Encoder encoder = EncoderFactory.get().binaryEncoder(out, null);

		final DatumWriter<Map<String, FL_Cluster>> cwriter = new SpecificDatumWriter<Map<String, FL_Cluster>>(ClusterContext.CLUSTER_MAP_SCHEMA);
		cwriter.write(_files, encoder);
		cwriter.write(_clusters, encoder);

		final DatumWriter<Map<String, FL_Entity>> ewriter = new SpecificDatumWriter<Map<String, FL_Entity>>(ClusterContext.ENTITY_MAP_SCHEMA);
		ewriter.write(_entities, encoder);

		
		// flush all writes
		encoder.flush();
	}
	
	@SuppressWarnings("unchecked")
	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		
		// read raw cluster hierarchy and pojos
		_rawContext = (ClusterContext) in.readObject();
		_memberIds = (Set<String>) in.readObject();
		_summarized = (Map<String, Boolean>) in.readObject();
		
		// avro
		final Decoder decoder = DecoderFactory.get().binaryDecoder(in, null);
		
		final DatumReader<Map<String, FL_Cluster>> creader = new SpecificDatumReader<Map<String, FL_Cluster>>(ClusterContext.CLUSTER_MAP_SCHEMA);
		_files = creader.read(_files, decoder);
		_clusters = creader.read(_clusters, decoder);
		
		final DatumReader<Map<String, FL_Entity>> ereader = new SpecificDatumReader<Map<String, FL_Entity>>(ClusterContext.ENTITY_MAP_SCHEMA);
		_entities = ereader.read(_entities, decoder);
	}
	
	/**
	 * 
	 */
	public ColumnContext() {
		_files = new HashMap<String, FL_Cluster>();
		_clusters = new HashMap<String, FL_Cluster>();
		_entities = new HashMap<String, FL_Entity>();
		_summarized = new HashMap<String, Boolean>();
		_memberIds = new HashSet<String>();
	}

	/* (non-Javadoc)
	 * @see influent.entity.clustering.utils.ReadWriteContext#setRawContext(influent.midtier.api.ClusterContext)
	 */
	@Override
	public void setContext(ClusterContext context) {
		_rawContext = context;
	}
	
	/* (non-Javadoc)
	 * @see influent.entity.clustering.utils.ReadContext#getRawContext()
	 */
	@Override
	public ClusterContext getContext() {
		return _rawContext;
	}
	

	/* (non-Javadoc)
	 * @see influent.entity.clustering.utils.ReadContext#getFile(java.lang.String)
	 */
	@Override
	public FL_Cluster getFile(String id) {
		return _files.get(id);
	}
	
	/* (non-Javadoc)
	 * @see influent.entity.clustering.utils.ReadContext#getFiles()
	 */
	@Override
	public List<FL_Cluster> getFiles() {
		return new ArrayList<FL_Cluster>(_files.values());
	}
	
	/* (non-Javadoc)
	 * @see influent.entity.clustering.utils.ReadContext#getFiles(java.util.Collection)
	 */
	@Override
	public List<FL_Cluster> getFiles(Collection<String> ids) {
		List<FL_Cluster> results = new ArrayList<FL_Cluster>();
		for (String id : ids) {
			FL_Cluster c = _files.get(id);
			
			if (c != null) {
				results.add(c);
			}
		}
		return results;
	}

	
	/* (non-Javadoc)
	 * @see influent.entity.clustering.utils.ReadContext#getCluster(java.lang.String)
	 */
	@Override
	public FL_Cluster getCluster(String id) {
		return _clusters.get(id);
	}

	/* (non-Javadoc)
	 * @see influent.entity.clustering.utils.ReadContext#getClusters()
	 */
	@Override
	public List<FL_Cluster> getClusters() {
		return new ArrayList<FL_Cluster>(_clusters.values());
	}
	
	/* (non-Javadoc)
	 * @see influent.entity.clustering.utils.ReadContext#getClusters(java.util.Collection)
	 */
	@Override
	public List<FL_Cluster> getClusters(Collection<String> ids) {
		List<FL_Cluster> results = new ArrayList<FL_Cluster>();
		for (String id : ids) {
			FL_Cluster c = _clusters.get(id);

			if (c != null) {
				results.add(c);
			}
		}
		return results;
	}
	
	/* (non-Javadoc)
	 * @see influent.entity.clustering.utils.ReadContext#getClusterIdsWithoutSummaries(java.util.List)
	 */
	@Override
	public List<String> getClusterIdsWithoutSummaries(List<String> ids) {
		List<String> nosumids = new ArrayList<String>();
		for (String id : ids) {
			Boolean summary = _summarized.get(id);
			if (summary != null && !summary) {
				nosumids.add(id);
			}
		}
		return nosumids;
	}

	
	/* (non-Javadoc)
	 * @see influent.entity.clustering.utils.ReadContext#getEntity(java.lang.String)
	 */
	@Override
	public FL_Entity getEntity(String id) {
		return _entities.get(id);
	}

		
	/* (non-Javadoc)
	 * @see influent.entity.clustering.utils.ReadContext#getEntities(java.util.Collection)
	 */
	@Override
	public List<FL_Entity> getEntities(Collection<String> ids) {
		List<FL_Entity> results = new ArrayList<FL_Entity>();
		for (String id : ids) {
			FL_Entity c = _entities.get(id);
			
			if (c != null) {
				results.add(c);
			}
		}
		return results;
	}

	
	/* (non-Javadoc)
	 * @see influent.entity.clustering.utils.ReadWriteContext#merge(java.util.Collection, boolean, boolean)
	 */
	@Override
	public void merge(Collection<FL_Cluster> clusters, boolean summariesComputed, boolean updateMembers) {
		merge(Collections.<FL_Cluster>emptyList(), clusters, Collections.<FL_Entity>emptyList(), summariesComputed, updateMembers);
	}
	
	/* (non-Javadoc)
	 * @see influent.entity.clustering.utils.ReadWriteContext#merge(java.util.Collection, java.util.Collection, java.util.Collection, boolean, boolean)
	 */
	@Override
	public void merge(Collection<FL_Cluster> files, Collection<FL_Cluster> clusters, Collection<FL_Entity> entities, boolean summariesComputed, boolean updateMembers) {
		for (FL_Cluster file : files) {
			if (_files.containsKey(file.getUid())) {
				FL_Cluster oldFile = _files.get(file.getUid());		
				oldFile.setProperties(file.getProperties());
				oldFile.setMembers(file.getMembers());
			} else {
				_files.put(file.getUid(), file);
				_memberIds.add(file.getUid());
				_memberIds.addAll(file.getMembers());
			}
		}
		
		for (FL_Cluster cluster : clusters) {
			
			// if the cluster already exists, just replace the properties
			// DON'T replace the whole thing, or the simplification pointers will be overwritten
			if (_clusters.containsKey(cluster.getUid())) {
				FL_Cluster oldCluster = _clusters.get(cluster.getUid());		
				oldCluster.setProperties(cluster.getProperties());
				oldCluster.setUncertainty(cluster.getUncertainty());
				oldCluster.setProvenance(cluster.getProvenance());
				
				if (updateMembers) {
					oldCluster.setMembers(cluster.getMembers());
					oldCluster.setSubclusters(cluster.getSubclusters());
					_memberIds.addAll(cluster.getMembers());
				}
			} else {
				_clusters.put(cluster.getUid(), cluster);
				_memberIds.add(cluster.getUid());
				// Not putting in subclusters, they will be handled on their own interation through the clusters list
				_memberIds.addAll(cluster.getMembers());
			}
			
			
			_summarized.put(cluster.getUid(), summariesComputed);
			

		}
		
		for (FL_Entity entity : entities) {
			if (_entities.containsKey(entity.getUid())) {
				FL_Entity oldEntity = _entities.get(entity.getUid());		
				oldEntity.setProperties(entity.getProperties());
			} else {
				_entities.put(entity.getUid(), entity);
				_memberIds.add(entity.getUid());
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see influent.entity.clustering.utils.ReadWriteContext#remove(java.util.Collection)
	 */
	@Override
	public void remove(Collection<String> ids) {
		for(String id : ids) {
			_clusters.remove(id);
			_memberIds.remove(id);
		}
	}
	
}
