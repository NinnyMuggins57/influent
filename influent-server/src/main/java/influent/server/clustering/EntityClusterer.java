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

import java.util.Collection;

public interface EntityClusterer {
	
	/***
	 * Method to pass arbitrary initialization parameters to entity clusterer
	 * 
	 * @param args
	 */
	public void init(Object[] args);
	
	/***
	 * Cluster the list of entities into Entity Cluster objects.
	 * 
	 * @param entities to cluster
	 * @return
	 */
	public ClusterContext clusterEntities(Collection<FL_Entity> entities);
	
	/***
	 * Cluster the list of entities into Entity Cluster objects. 
	 * 
	 * @param entities to cluster
	 * @param clusters is a list of existing cluster the entities should be merged with
	 * @param context is the current cluster hierarchy in the context
	 * @return
	 */
	public ClusterContext clusterEntities(Collection<FL_Entity> entities, Collection<FL_Cluster> clusters, ClusterContext context);
	
	/***
	 * Cluster the list of entities and immutable clusters into Entity Cluster objects. 
	 * 
	 * @param entities to cluster
	 * @param immutable clusters are clusters that are treated as atomic entities (their contents can't be altered)
	 * @param clusters is a list of existing clusters the entities and immutable clusters should be merged with
	 * @param context is the current cluster hierarchy in the context
	 * @return
	 */
	public ClusterContext clusterEntities(Collection<FL_Entity> entities, Collection<FL_Cluster> immutableClusters, Collection<FL_Cluster> clusters, ClusterContext context);
	
	/***
	 * Cluster the list of entities into Entity Cluster objects. 
	 * 
	 * @param entities to cluster
	 * @param context is the current cluster hierarchy in the context
	 * @return
	 */
	public ClusterContext clusterEntities(Collection<FL_Entity> entities, ClusterContext context);
}
