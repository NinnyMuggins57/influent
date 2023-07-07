package influent.server.clustering.utils;

import influent.idl.FL_Cluster;
import influent.idl.FL_Entity;
import influent.server.clustering.ClusterContext;

import java.util.Collection;
import java.util.List;

public interface ContextRead {

	public ClusterContext getContext();

	/**
	 * Given an id, return the associated file.
	 */
	public FL_Cluster getFile(String id);

	/**
	 * Return all files.
	 */
	public List<FL_Cluster> getFiles();

	/**
	 * Return all files matching the specified ids.
	 */
	public List<FL_Cluster> getFiles(Collection<String> ids);

	/**
	 * Given an id, return the associated cluster.
	 */
	public FL_Cluster getCluster(String id);

	/**
	 * Return all clusters.
	 */
	public List<FL_Cluster> getClusters();

	/**
	 * Return all clusters matching the specified ids.
	 */
	public List<FL_Cluster> getClusters(Collection<String> ids);

	/**
	 * Given a set of ids and a context, return the list of clusters that exist in the cache,
	 * but do not have their summaries computed.  If an id does not exist in the context, then it will not
	 * be returned.  This method is intended to inform the calling class which clusters need to be fetched
	 * to have their summaries computed.
	 */
	public List<String> getClusterIdsWithoutSummaries(List<String> ids);

	/**
	 * Given an id, return the associated entity.
	 */
	public FL_Entity getEntity(String id);

	/**
	 * Return all entities matching the specified ids.
	 */
	public List<FL_Entity> getEntities(Collection<String> ids);

}