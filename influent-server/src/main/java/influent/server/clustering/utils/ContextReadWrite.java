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
	 * Merges changes into this context.
	 */
	public void merge(Collection<FL_Cluster> clusters,
			boolean summariesComputed, boolean updateMembers);

	/**
	 * Merges changes into this context.
	 */
	public void merge(Collection<FL_Cluster> files,
			Collection<FL_Cluster> clusters, Collection<FL_Entity> entities,
			boolean summariesComputed, boolean updateMembers);

	/**
	 * Remove members
	 */
	public void remove(Collection<String> ids);

}