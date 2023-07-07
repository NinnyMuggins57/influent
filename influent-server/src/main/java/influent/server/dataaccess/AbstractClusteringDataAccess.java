package influent.server.dataaccess;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.avro.AvroRemoteException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import influent.idl.FL_Cluster;
import influent.idl.FL_ClusteringDataAccess;
import influent.idl.FL_DataAccess;
import influent.idl.FL_DistributionRange;
import influent.idl.FL_EntityTag;
import influent.idl.FL_Frequency;
import influent.idl.FL_GeoData;
import influent.idl.FL_Geocoding;
import influent.idl.FL_Property;
import influent.idl.FL_PropertyTag;
import influent.idl.FL_PropertyType;
import influent.idl.FL_RangeType;
import influent.idlhelper.ClusterHelper;
import influent.idlhelper.PropertyHelper;
import influent.server.clustering.EntityClusterer;
import influent.server.clustering.utils.EntityClusterFactory;
import influent.server.clustering.utils.PropertyManager;
import influent.server.utilities.SQLConnectionPool;
import influent.server.utilities.TypedId;

public abstract class AbstractClusteringDataAccess implements FL_ClusteringDataAccess {
	protected static Logger s_logger = LoggerFactory.getLogger(ClusteringDataAccess.class);
	
	protected final FL_DataAccess _entityAccess;
	protected final FL_Geocoding _geoCoder;
	protected final EntityClusterer _clusterer;
	protected final EntityClusterFactory _clusterFactory;
	protected final SQLConnectionPool _connectionPool;
	protected final DataNamespaceHandler _namespaceHandler;

	public AbstractClusteringDataAccess(SQLConnectionPool connectionPool,
										DataNamespaceHandler namespaceHandler,
										FL_DataAccess entityAccess, 
										FL_Geocoding geocoding, 
										EntityClusterer clusterer, 
										EntityClusterFactory clusterFactory,
										PropertyManager config) throws ClassNotFoundException, SQLException {
		_connectionPool = connectionPool;
		_namespaceHandler = namespaceHandler;
		_entityAccess = entityAccess;
		_clusterer = clusterer;
		_clusterFactory = clusterFactory;
		_geoCoder = geocoding;
	}
	
	protected DataNamespaceHandler getNamespaceHandler() {
		return _namespaceHandler;
	}

	@Override
	public List<FL_Cluster> getClusterSummary(List<String> clusterIds)
			throws AvroRemoteException {
		List<FL_Cluster> summaryClusters = new LinkedList<FL_Cluster>();
		
		if (clusterIds == null || clusterIds.isEmpty()) return summaryClusters;
		
		final List<String> ns_entities = TypedId.nativeFromTypedIds(clusterIds);
		
		try {
			Map<String, Map<String, PropertyHelper>> entityPropMap = new HashMap<String, Map<String, PropertyHelper>>();
			
			Connection connection = _connectionPool.getConnection();
			Statement stmt = connection.createStatement();
		
			String summaryTable = getNamespaceHandler().tableName(null, DataAccessHelper.CLUSTER_SUMMARY_TABLE);
		
			// process src nodes in batches 
			List<String> idsCopy = new ArrayList<String>(ns_entities); // copy the ids as we will take 1000 at a time to process and the take method is destructive
			while (idsCopy.size() > 0) {
				List<String> tempSubList = (idsCopy.size() > 1000) ? tempSubList = idsCopy.subList(0, 999) : idsCopy; // get the next 1000
				List<String> subIds = new ArrayList<String>(tempSubList); // copy as the next step is destructive
				tempSubList.clear(); // this clears the IDs from idsCopy as tempSubList is backed by idsCopy 
			
				String finEntityTable = _namespaceHandler.tableName(null, DataAccessHelper.ENTITY_TABLE);
				String inClause = DataAccessHelper.createInClause(subIds);
				
				Map<String, int[]> entityStats = new HashMap<String, int[]>();
			
				StringBuilder sql = new StringBuilder();
				
				sql.append(" select EntityId, UniqueInboundDegree, UniqueOutboundDegree ");
				sql.append("   from " + finEntityTable);
				sql.append("  where EntityId in " +  inClause);
				
				if (stmt.execute(sql.toString())) {
					ResultSet rs = stmt.getResultSet();
					while (rs.next()) {
						String entityId = rs.getString("EntityId");
						int inDegree = rs.getInt("UniqueInboundDegree");
						int outDegree = rs.getInt("UniqueOutboundDegree");
					
						entityStats.put(entityId, new int[]{inDegree, outDegree});
					}
					rs.close();
				}
				
				sql = new StringBuilder();
				sql.append(" SELECT EntityId, Property, Tag, Type, Value, Stat ");
				sql.append("   FROM " + summaryTable);
				sql.append("  WHERE EntityId IN " + inClause);
				sql.append("  ORDER BY EntityId");
			
				if (stmt.execute(sql.toString())) {				
					ResultSet rs = stmt.getResultSet();
			
					while (rs.next()) {
						String id = rs.getString("EntityId");
						String property = rs.getString("Property");
						String tag = rs.getString("Tag");
						String type = rs.getString("Type");
						String value = rs.getString("Value");
						float stat = rs.getFloat("Stat");
						
						Map<String, PropertyHelper> propMap = entityPropMap.get(id);
						if (propMap == null) {
							propMap = new HashMap<String, PropertyHelper>();
							entityPropMap.put(id, propMap);
						}
						
						PropertyHelper prop = propMap.get(property);
						if (prop == null) {
							prop = createProperty(property, tag, type, value, stat);
							propMap.put(property, prop);
						}
						else {
							updateProperty(prop, type, value, stat);
						}
					}
					
					for (String id : entityPropMap.keySet()) {
						int[] stats = entityStats.get( id );
						
						if (stats != null) {
							// add degree stats
							Map<String, PropertyHelper> propMap = entityPropMap.get(id);
							propMap.put("inboundDegree", new PropertyHelper("inboundDegree", stats[0], FL_PropertyTag.INFLOWING) );
							propMap.put("outboundDegree", new PropertyHelper("outboundDegree", stats[1], FL_PropertyTag.OUTFLOWING) );
						}
					}
					
					summaryClusters.addAll( createSummaryClusters(entityPropMap) );
				}
			}
			
			return summaryClusters;
		} catch (Exception e) {
			throw new AvroRemoteException(e);
		}
	}
	
	private Object getPropertyValue(FL_PropertyType type, String value) {
		Object propValue = null;
		
		switch (type) {
		case BOOLEAN:
			propValue = Boolean.parseBoolean(value);
			break;
		case DOUBLE:
			propValue = Double.parseDouble(value);
			break;
		case LONG:
			propValue = Long.parseLong(value);
			break;
		case DATE:
			propValue = DateTime.parse(value);
			break;
		case STRING:
		default:
			propValue = value;
			break;
		}
		
		return propValue;
	}
	
	private PropertyHelper createProperty(String property, String tag, String type, String value, float stat) {
		FL_PropertyTag propTag = FL_PropertyTag.valueOf(tag);
		FL_PropertyType propType = FL_PropertyType.valueOf(type);
		
		boolean isDist = (stat > 0);
		
		PropertyHelper prop = null;
		Object propValue = null;
		
		// handle special properties
		if (propTag == FL_PropertyTag.COUNTRY_CODE) {
			property = "location-dist";  // UI expects the property to be named location-dist so rename
			propTag = FL_PropertyTag.GEO;
			FL_GeoData geo = new FL_GeoData(null, null, null, value);
			try {
				_geoCoder.geocode(Collections.singletonList(geo));
			} catch (AvroRemoteException e) { /* ignore - we do our best to geo code */ }
			
			propValue = geo;
		} else if (propTag == FL_PropertyTag.TYPE) {
			property = "type-dist";   // UI expects the property to be named type-dist so rename
			propValue = value;
		} else {  // otherwise we build non-special properties
			propValue = getPropertyValue(propType, value);
		}
		
		if (isDist) {
			List<FL_Frequency> freqs = new ArrayList<FL_Frequency>();
			freqs.add( new FL_Frequency(propValue, new Double(stat)) );
			FL_DistributionRange range = new FL_DistributionRange(freqs, FL_RangeType.DISTRIBUTION, propType, false);
			prop = new PropertyHelper(property, property, null, null, Collections.singletonList(propTag), range);
		} else {
			prop = new PropertyHelper(property, property, propValue, propType, propTag);
		}
			
		return prop;
	}
	
	@SuppressWarnings("unchecked")
	private void updateProperty(PropertyHelper property, String type, String value, float stat) {
		FL_PropertyType propType = FL_PropertyType.valueOf(type);
		
		Object propValue = getPropertyValue(propType, value);
				
		// update distribution range - assumes updateProperty is only applied to distribution properties - which should be the case
		List<FL_Frequency> freqs = (List<FL_Frequency>)property.getValue();
		
		freqs.add( new FL_Frequency(propValue, new Double(stat)) );
	}
	
	@SuppressWarnings("unchecked")
	private List<FL_Cluster> createSummaryClusters(Map<String, Map<String, PropertyHelper>> entityPropMap) {
		List<FL_Cluster> summaries = new ArrayList<FL_Cluster>(entityPropMap.size());
		
		for (String id : entityPropMap.keySet()) {
			String label = "";
			
			Map<String, PropertyHelper> propMap = entityPropMap.get(id);
			
			List<FL_Property> props = new ArrayList<FL_Property>(propMap.size());
			
			for (String prop : propMap.keySet()) {
				PropertyHelper p = propMap.get(prop);
				if (p.hasTag(FL_PropertyTag.LABEL)) {
					Object val = p.getValue();
					if (val instanceof String) {
						label = (String)p.getValue();
					} else {
						List<FL_Frequency> freqs = (List<FL_Frequency>)val;
						label = (freqs.isEmpty()) ? "Unknown" : (String)freqs.get(0).getRange();
					}
				} else {
					props.add(p);
				}
			}
			
			summaries.add( new ClusterHelper( TypedId.fromNativeId(TypedId.CLUSTER_SUMMARY, id).getTypedId(), 
					 						  label,
					 						  FL_EntityTag.CLUSTER_SUMMARY,
					 						  props,
					 						  new ArrayList<String>(0),
					 						  new ArrayList<String>(0),
					 						  null,
					 						  null,
					 						  -1) );
		}
		return summaries;
	}
}
