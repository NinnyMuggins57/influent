package influent.server.clustering;

public class ClusterDistributionProperty {
	public final String entityTagOrFieldName;
	public final String clusterFieldName;

	public ClusterDistributionProperty(String entityTagOrFieldName, String clusterFieldName) {
		this.entityTagOrFieldName = entityTagOrFieldName;
		this.clusterFieldName = clusterFieldName;
	}
}
