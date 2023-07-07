package influent.server.clustering.utils;

import influent.idl.FL_Cluster;
import influent.idl.FL_Entity;
import influent.server.clustering.ClusterContext;

import java.util.Collection;

public interface ContextReadWrite extends ContextRead {

	/**
	 * Raw unsimplified content
	 */
	public void setContext(ClusterContext context);
	
	/**
	 * Add child context
	 */
	public void addChildContext(String contextId);
	
	/**
	 * Remove child context
	 */
	public void removeChildContext(String contextId);

	/**
	 * Cache the simplified cluster context
	 */
	public void setSimplifiedContext(Collection<FL_Cluster> clusters);

	/**
	 * Cache the simplified cluster context
	 */
	public void setSimplifiedContext(Collection<FL_Cluster> clusters, Collection<FL_Entity> rootEntities);
}