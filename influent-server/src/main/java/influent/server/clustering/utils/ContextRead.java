package influent.server.clustering.utils;

import influent.idl.FL_Cluster;
import influent.idl.FL_Entity;
import influent.server.clustering.ClusterContext;

import java.util.Collection;
import java.util.List;

public interface ContextRead {
	
	/**
	 * Returns the context id
	 */
	public String getUid();

	/**
	 * Returns the cluster context associated with this context
	 */
	public ClusterContext getContext();

	/**
	 * Given an id, return the associated cluster.
	 */
	public FL_Cluster getCluster(String id);

	/**
	 * Return all clusters.
	 */
	public List<FL_Cluster> getClusters();
	
	/**
	 * Return root level objects in context - either FL_Entity or FL_Cluster objects
	 */
	public List<Object> getRootObjects();
	
	/**
	 * Return all child context ids
	 */
	public List<String> getChildContexts();

	/**
	 * Return all clusters matching the specified ids.
	 */
	public List<FL_Cluster> getClusters(Collection<String> ids);

	/**
	 * Given an id, return the associated entity.
	 */
	public FL_Entity getEntity(String id);

	/**
	 * Return all entities matching the specified ids.
	 */
	public List<FL_Entity> getEntities(Collection<String> ids);
	
	/**
	 * Returns whether any entities or clusters exist in context
	 */
	public boolean isEmpty();
	
	/**
	 * Returns the version of the context; which is updated whenever it is modified
	 */	
	public int getVersion();
}