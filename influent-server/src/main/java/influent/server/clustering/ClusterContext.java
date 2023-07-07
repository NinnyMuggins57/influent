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
package influent.server.clustering;

import influent.idl.FL_Cluster;
import influent.idl.FL_Entity;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.avro.Schema;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;

public class ClusterContext implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	public static final Schema CLUSTER_MAP_SCHEMA = Schema.createMap(FL_Cluster.getClassSchema());
	public static final Schema ENTITY_MAP_SCHEMA = Schema.createMap(FL_Entity.getClassSchema());
	
	public Map<String, FL_Cluster> roots = new HashMap<String, FL_Cluster>();
	public Map<String, FL_Cluster> clusters = new HashMap<String, FL_Cluster>();
	public Map<String, FL_Entity> entities = new HashMap<String, FL_Entity>();
	
	
	
	public void addEntities(Collection<FL_Entity> entities) {
		for (FL_Entity entity : entities) {
			this.entities.put(entity.getUid(), entity);
		}
	}
	
	
	/**
	 * Override Java serialization methods
	 */
	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
		final Encoder encoder = EncoderFactory.get().binaryEncoder(out, null);

		final DatumWriter<Map<String, FL_Cluster>> cwriter = new SpecificDatumWriter<Map<String, FL_Cluster>>(CLUSTER_MAP_SCHEMA);
		cwriter.write(roots, encoder);
		cwriter.write(clusters, encoder);

		final DatumWriter<Map<String, FL_Entity>> ewriter = new SpecificDatumWriter<Map<String, FL_Entity>>(ENTITY_MAP_SCHEMA);
		ewriter.write(entities, encoder);
		encoder.flush();
	}
	
	
	
	
	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		final Decoder decoder = DecoderFactory.get().binaryDecoder(in, null);


		final DatumReader<Map<String, FL_Cluster>> creader = new SpecificDatumReader<Map<String, FL_Cluster>>(CLUSTER_MAP_SCHEMA);
		roots = creader.read(roots, decoder);
		clusters = creader.read(clusters, decoder);
		
		final DatumReader<Map<String, FL_Entity>> ereader = new SpecificDatumReader<Map<String, FL_Entity>>(ENTITY_MAP_SCHEMA);
		entities = ereader.read(entities, decoder);
	}
} 
