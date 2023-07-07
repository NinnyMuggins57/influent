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
package influent.server.dataaccess;

import influent.idl.FL_Cluster;
import influent.idl.FL_Clustering;
import influent.idl.FL_ClusteringDataAccess;
import influent.idl.FL_DateRange;
import influent.idl.FL_DirectionFilter;
import influent.idl.FL_DistributionRange;
import influent.idl.FL_EntityTag;
import influent.idl.FL_Frequency;
import influent.idl.FL_GeoData;
import influent.idl.FL_Geocoding;
import influent.idl.FL_Link;
import influent.idl.FL_LinkTag;
import influent.idl.FL_Property;
import influent.idl.FL_PropertyTag;
import influent.idl.FL_PropertyType;
import influent.idl.FL_RangeType;
import influent.idl.FL_SortBy;
import influent.idlhelper.ClusterHelper;
import influent.idlhelper.PropertyHelper;
import influent.server.utilities.SQLConnectionPool;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.avro.AvroRemoteException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */

public abstract class ClusteringDataAccess implements FL_ClusteringDataAccess {
	protected static Logger s_logger = LoggerFactory.getLogger(ClusteringDataAccess.class);
	
	protected final FL_Clustering _clustering;
	protected final SQLConnectionPool _connectionPool;
	protected final FL_Geocoding _geocoding;
	private final DataNamespaceHandler _namespaceHandler;
	
	public ClusteringDataAccess(SQLConnectionPool connectionPool, FL_Clustering clustering, FL_Geocoding geocoding, DataNamespaceHandler namespaceHandler) 
			throws ClassNotFoundException, SQLException {
		_connectionPool = connectionPool;
		_clustering = clustering;
		_geocoding = geocoding;
		_namespaceHandler = namespaceHandler;
	}
	
	protected DataNamespaceHandler getNamespaceHandler() {
		return _namespaceHandler;
	}
	
	@Override
	public List<FL_Cluster> getClusterSummary(List<String> entities)
			throws AvroRemoteException {
		// TODO Auto-generated method stub
		return null;
	}
	
	private Map<String, Map<String, List<String>>> getEntityMembers(Collection<String> clusterIds, String contextId, String sessionId, Statement stmt) throws SQLException {
		Map<String, Map<String, List<String>>> membership = new HashMap<String, Map<String, List<String>>>();
		
		long start = System.currentTimeMillis();
		
		String gClusterTable = getNamespaceHandler().tableName(null, DataAccessHelper.GLOBAL_CLUSTER_TABLE);
		// create the session cluster context data table
		String dClusterTable = getNamespaceHandler().tempTableName(
				getNamespaceHandler().tableName(null, DataAccessHelper.DYNAMIC_CLUSTER_TABLE) + "_" + sessionId + "_" + contextId);
		try {
			stmt.execute(getNamespaceHandler().createTempTable(dClusterTable, " ( rowid bigint ) "));
		} catch (Exception e) { /* ignore */ }
				
		// process src nodes in batches 
		List<String> idsCopy = new ArrayList<String>(clusterIds); // copy the ids as we will take 1000 at a time to process and the take method is destructive
		while (idsCopy.size() > 0) {
			List<String> tempSubList = (idsCopy.size() > 1000) ? tempSubList = idsCopy.subList(0, 999) : idsCopy; // get the next 1000
			List<String> subIds = new ArrayList<String>(tempSubList); // copy as the next step is destructive
			tempSubList.clear(); // this clears the IDs from idsCopy as tempSubList is backed by idsCopy 
								
			String inClause = DataAccessHelper.createInClause(subIds);
								
			StringBuilder sql = new StringBuilder();
			sql.append(" SELECT g.clusterid as parent, g.entityid as child ");
			sql.append("   FROM " + gClusterTable + " g ");
			sql.append("   JOIN " + dClusterTable + " c ");
			sql.append("     ON g.id = c.rowid ");
			sql.append("  WHERE g.clusterid IN " + inClause);
			sql.append("  GROUP BY g.clusterid, g.entityid ");
			
			if (stmt.execute(sql.toString())) {				
				ResultSet rs = stmt.getResultSet();
			
				while (rs.next()) {
					String cid = rs.getString("parent");
					String eid = rs.getString("child");
				
					if (membership.containsKey(cid)) {
						membership.get(cid).get("entity").add(eid);
					}
					else {
						Map<String, List<String>> members = new HashMap<String, List<String>>();
						members.put("cluster", new LinkedList<String>());
						members.put("entity", new LinkedList<String>());
						members.get("entity").add(eid); 
						membership.put(cid, members);
					}
				}
				rs.close();
			}
		}
		s_logger.info("Time to retrieve sub cluster entity members: " + ((System.currentTimeMillis() - start)/1000));
		
		return membership;
	}
	
	private Map<String, Map<String, List<String>>> getEntityMembers(String contextId, String sessionId, Statement stmt) throws SQLException {
		Map<String, Map<String, List<String>>> membership = new HashMap<String, Map<String, List<String>>>();
		
		String gClusterTable = getNamespaceHandler().tableName(null, DataAccessHelper.GLOBAL_CLUSTER_TABLE);
		// create the session cluster context data table
		String dClusterTable = getNamespaceHandler().tempTableName(
				getNamespaceHandler().tableName(null, DataAccessHelper.DYNAMIC_CLUSTER_TABLE) + "_" + sessionId + "_" + contextId);
		try {
			stmt.execute(getNamespaceHandler().createTempTable(dClusterTable, " ( rowid bigint ) "));
		} catch (Exception e) { /* ignore */ }
		
		StringBuilder sql = new StringBuilder();
		sql.append(" SELECT g2.clusterid as parent, g2.entityid as child ");
		sql.append("   FROM " + gClusterTable + " g2 ");
		sql.append("   JOIN " + dClusterTable + " c ");
		sql.append("     ON g2.id = c.rowid ");
		sql.append("  WHERE g2.isleaf = 'Y' ");
		sql.append(" GROUP BY g2.clusterid, g2.entityid ");
	
		long start = System.currentTimeMillis();
		
		if (stmt.execute(sql.toString())) {				
			ResultSet rs = stmt.getResultSet();
			
			while (rs.next()) {
				String cid = rs.getString("parent");
				String eid = rs.getString("child");
				
				if (membership.containsKey(cid)) {
					membership.get(cid).get("entity").add(eid);
				}
				else {
					Map<String, List<String>> members = new HashMap<String, List<String>>();
					members.put("cluster", new LinkedList<String>());
					members.put("entity", new LinkedList<String>());
					members.get("entity").add(eid); 
					membership.put(cid, members);
				}
			}
			rs.close();
		}
		s_logger.info("Time to retrieve sub cluster entity members: " + ((System.currentTimeMillis() - start)/1000));
		
		return membership;
	}
	
	protected Map<String, Map<String, List<String>>> selectClusterMembers(Collection<String> clusterIds, String contextId, String sessionId) throws SQLException, Exception {
		Map<String, Map<String, List<String>>> membership = new HashMap<String, Map<String, List<String>>>();
	
		if (clusterIds == null || clusterIds.isEmpty()) return membership;
	
		Connection connection = _connectionPool.getConnection();
		Statement stmt = connection.createStatement();
		
		String gClusterTable = getNamespaceHandler().tableName(null, DataAccessHelper.GLOBAL_CLUSTER_TABLE);
		
		// create the session cluster context data table
		String dClusterTable = getNamespaceHandler().tempTableName(
				getNamespaceHandler().tableName(null, DataAccessHelper.DYNAMIC_CLUSTER_TABLE) + "_" + sessionId + "_" + contextId);
		try {
			stmt.execute(getNamespaceHandler().createTempTable(dClusterTable, " ( rowid bigint ) "));
		} catch (Exception e) { /* ignore */ }
		
		long start = System.currentTimeMillis();
		
		// process src nodes in batches 
		List<String> idsCopy = new ArrayList<String>(clusterIds); // copy the ids as we will take 1000 at a time to process and the take method is destructive
		while (idsCopy.size() > 0) {
			List<String> tempSubList = (idsCopy.size() > 1000) ? tempSubList = idsCopy.subList(0, 999) : idsCopy; // get the next 1000
			List<String> subIds = new ArrayList<String>(tempSubList); // copy as the next step is destructive
			tempSubList.clear(); // this clears the IDs from idsCopy as tempSubList is backed by idsCopy 
								
			String inClause = DataAccessHelper.createInClause(subIds);
					
			StringBuilder sql = new StringBuilder();
			sql.append("SELECT g2.parentid as parent, g2.clusterid as child ");
			sql.append("  FROM " + gClusterTable + " g2 ");
			sql.append("  JOIN " + dClusterTable + " c2 ");
			sql.append("    ON g2.id = c2.rowid ");
			sql.append(" WHERE g2.parentid IN " + inClause);
			sql.append(" GROUP BY g2.parentid, g2.clusterid ");
			
			if (stmt.execute(sql.toString())) {				
				ResultSet rs = stmt.getResultSet();
				
				while (rs.next()) {
					String cid = rs.getString("parent");
					String eid = rs.getString("child");
					
					if (cid == null) continue;
					
					if (membership.containsKey(cid)) {
						membership.get(cid).get("cluster").add(eid);
					}
					else {
						Map<String, List<String>> members = new HashMap<String, List<String>>();
						members.put("cluster", new LinkedList<String>());
						members.put("entity", new LinkedList<String>());
						members.get("cluster").add(eid); 
						membership.put(cid, members);
					}
				}		
			}
		}

		Collection<String> leafClusterIds = new HashSet<String>();
		for (String id : clusterIds) {
			if (membership.containsKey(id) == false) {
				leafClusterIds.add(id);
			}
		}
		
		if (leafClusterIds.size() > 0) {
			membership.putAll( getEntityMembers(leafClusterIds, contextId, sessionId, stmt) );
		}
		
		s_logger.info("Time to fetch cluster members: " + ((System.currentTimeMillis() - start)/1000));
		
		stmt.close();
		connection.close();
		return membership;
	}
	
	protected Map<String, Map<String, List<String>>> selectClusterMembers(String contextId, String sessionId) throws SQLException, Exception {
		Map<String, Map<String, List<String>>> membership = new HashMap<String, Map<String, List<String>>>();
		
		Connection connection = _connectionPool.getConnection();
		Statement stmt = connection.createStatement();
		
		String gClusterTable = getNamespaceHandler().tableName(null, DataAccessHelper.GLOBAL_CLUSTER_TABLE);
		
		// create the session cluster context data table
		String dClusterTable = getNamespaceHandler().tempTableName(
				getNamespaceHandler().tableName(null, DataAccessHelper.DYNAMIC_CLUSTER_TABLE) + "_" + sessionId + "_" + contextId);
		try {
			stmt.execute(getNamespaceHandler().createTempTable(dClusterTable, " ( rowid bigint ) "));
		} catch (Exception e) { /* ignore */ }
		
		long start = System.currentTimeMillis();
		
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT g2.parentid as parent, g2.clusterid as child ");
		sql.append("  FROM " + gClusterTable + " g2 ");
		sql.append("  JOIN " + dClusterTable + " c2 ");
		sql.append("    ON g2.id = c2.rowid ");
		sql.append(" GROUP BY g2.parentid, g2.clusterid ");
		
		if (stmt.execute(sql.toString())) {				
			ResultSet rs = stmt.getResultSet();
			
			while (rs.next()) {
				String cid = rs.getString("parent");
				String eid = rs.getString("child");
				
				if (cid == null) continue;
				
				if (membership.containsKey(cid)) {
					membership.get(cid).get("cluster").add(eid);
				}
				else {
					Map<String, List<String>> members = new HashMap<String, List<String>>();
					members.put("cluster", new LinkedList<String>());
					members.put("entity", new LinkedList<String>());
					members.get("cluster").add(eid); 
					membership.put(cid, members);
				}
			}
				
		}
		
		membership.putAll( getEntityMembers(contextId, sessionId, stmt) );
		
		s_logger.info("Time to fetch cluster members: " + ((System.currentTimeMillis() - start)/1000));
		
		stmt.close();
		connection.close();
		return membership;
	}
	
	private List<String> getContextClusterIds(String contextId, String sessionId) throws ClassNotFoundException, SQLException {
		List<String> clusterIds = new LinkedList<String>();
		
		Connection connection = _connectionPool.getConnection();
		Statement stmt = connection.createStatement();
		
		String gClusterTable = getNamespaceHandler().tableName(null, DataAccessHelper.GLOBAL_CLUSTER_TABLE);
		
		// create the session cluster context data table
		String dClusterTable = getNamespaceHandler().tempTableName(
				getNamespaceHandler().tableName(null, DataAccessHelper.DYNAMIC_CLUSTER_TABLE) + "_" + sessionId + "_" + contextId);
		try {
			stmt.execute(getNamespaceHandler().createTempTable(dClusterTable, " ( rowid bigint ) "));
		} catch (Exception e) { /* ignore */ }
			
		StringBuilder sql = new StringBuilder();
		
		sql = new StringBuilder();
		sql.append(" SELECT DISTINCT g.clusterid ");
		sql.append("   FROM " + gClusterTable + " g ");
		sql.append("   JOIN " + dClusterTable + " c ");
		sql.append("     ON g.id = c.rowid ");
					
		s_logger.trace("execute: " + sql.toString());
		if (stmt.execute(sql.toString())) {
			ResultSet rs = stmt.getResultSet();
			while (rs.next()) {
				clusterIds.add(rs.getString("clusterid"));
			}
		}
		return clusterIds;
	}
	
	@SuppressWarnings("unchecked")
	private Map<String, ClusterHelper> buildClusterSummaries(Map<String, Map<String, Object>> clusterProps) {
		Map<String, ClusterHelper> clusters = new HashMap<String, ClusterHelper>();
		
		for (String id : clusterProps.keySet()) {
			Map<String, Object> props = clusterProps.get(id);
			
			String parent = "";
			String root = "";
			String label = "";
			int count = 0;
			int level = 0;
			
			List<FL_Property> entityProps = new LinkedList<FL_Property>();
		
			for (String prop : props.keySet()) {
				if (prop.equals("parent")) parent = (String)props.get(prop);
				else if (prop.equals("root")) root = (String)props.get(prop);
				else if (prop.equals("level")) level = (Integer)props.get(prop);
				else if (prop.equals("label")) {
					Map<String,Integer> dist = (Map<String,Integer>)props.get(prop);
					if (dist.size() > 0) label = dist.keySet().iterator().next();
				}
				else if (prop.equals("members")) {
					Map<String,Double> typeSummary = (Map<String,Double>)props.get(prop);
					for (String type : typeSummary.keySet()) {
						count += typeSummary.get(type);
					}
					
					// create the type distribution range property
					List<FL_Frequency> typeFreqs = new ArrayList<FL_Frequency>();
					for (String key : typeSummary.keySet()) {
						typeFreqs.add( new FL_Frequency(key, typeSummary.get(key)) );
					}
					FL_DistributionRange range = new FL_DistributionRange(typeFreqs, FL_RangeType.DISTRIBUTION, FL_PropertyType.STRING, false);
					FL_Property dist = new FL_Property("type-dist", "type-dist", range, null, null, Collections.singletonList(FL_PropertyTag.TYPE));
					entityProps.add(dist);
					
//					entityProps.add(new PropertyHelper("type-dist", "distribution of entity types", dist, FL_PropertyType.OTHER, Collections.singletonList(FL_PropertyTag.STAT)));
					entityProps.add(new PropertyHelper("count", "membership count", count, FL_PropertyType.LONG, Collections.singletonList(FL_PropertyTag.COUNT)));
				}
				else if (prop.equals("location-dist")) {
					Map<String,Double> locationSummary = (Map<String,Double>)props.get(prop);
					List<FL_Frequency> locFreqs = new ArrayList<FL_Frequency>();
					for (String key : locationSummary.keySet()) {
						FL_GeoData geo = new FL_GeoData(null, null, null, key);
						
						try {
							_geocoding.geocode(Collections.singletonList(geo));
						} catch (AvroRemoteException e) {
						}
						
						locFreqs.add( new FL_Frequency(geo, locationSummary.get(key)) );
					}
					FL_DistributionRange range = new FL_DistributionRange(locFreqs, FL_RangeType.DISTRIBUTION, FL_PropertyType.GEO, false);
					FL_Property dist = new FL_Property("location-dist", "location-dist", range, null, null, Collections.singletonList(FL_PropertyTag.GEO));
					entityProps.add(dist);
				}
				else {
					Map<String,Double> summary = (Map<String,Double>)props.get(prop);
					List<FL_Frequency> freqs = new ArrayList<FL_Frequency>();
					for (String key : summary.keySet()) {
						freqs.add( new FL_Frequency(key, summary.get(key)) );
					}
					FL_DistributionRange range = new FL_DistributionRange(freqs, FL_RangeType.DISTRIBUTION, FL_PropertyType.STRING, false);
					FL_Property dist = new FL_Property(prop, prop, range, null, null, Collections.singletonList(FL_PropertyTag.STAT));
					entityProps.add(dist);
//					entityProps.add(new PropertyHelper(prop, prop, (Map<String,Integer>)props.get(prop), FL_PropertyType.OTHER, Collections.singletonList(FL_PropertyTag.STAT)));
				}
			}
			
			if (props.containsKey("location-dist") == false) {
				FL_DistributionRange range = new FL_DistributionRange(null, FL_RangeType.DISTRIBUTION, FL_PropertyType.GEO, false);
				FL_Property dist = new FL_Property("location-dist", "location-dist", range, null, null, Collections.singletonList(FL_PropertyTag.GEO));
				entityProps.add(dist);
		
//				entityProps.add(new PropertyHelper("location-dist", "country code distribution", new HashMap<String, Integer>(), FL_PropertyType.OTHER, Collections.singletonList(FL_PropertyTag.STAT)));
			}
			
			clusters.put(id, new ClusterHelper(id, label, FL_EntityTag.CLUSTER, entityProps, parent, root, level));
		}
		
		return clusters;
	}
	
	protected Map<String, ClusterHelper> createClustersFromResultSet(ResultSet rs, String contextId, String sessionId) throws SQLException, Exception {
		Map<String, ClusterHelper> clusters = _createClustersFromResultSet(rs);
		
		_setClusterMembers(clusters, contextId, sessionId);
		
		return clusters;
	}
	
	protected void increment(String key, double increment, Map<String, Double> index) {
		double count = 0;
		
		if (index.containsKey(key)) {
			count = index.get(key);
		}
		count += increment;
		index.put(key, count);
	}
	
	@SuppressWarnings("unchecked")
	protected Map<String, ClusterHelper> _createClustersFromResultSet(ResultSet rs) throws SQLException, Exception {
		Map<String, Map<String, Object>> clusterProps = new HashMap<String, Map<String, Object>>();
		Map<String, ClusterHelper> clusters = new HashMap<String, ClusterHelper>();
		
		// construct the cluster summaries
		while (rs.next()) {			
			String id = rs.getString("clusterid");
			String parent = rs.getString("parentid");
			String root = rs.getString("rootid");
			int level = rs.getInt("hierarchylevel");
			String prop = rs.getString("property");
			String value = rs.getString("value");
			double stat = rs.getInt("stat"); 
			
			Map<String, Object> props = clusterProps.get(id);
			if (props == null) {
				props = new HashMap<String, Object>();
				props.put("parent", parent);
				props.put("root", root);
				props.put("level", level);
				clusterProps.put(id, props);
			}
			
			if (props.containsKey(prop) == false) {
				props.put(prop, new HashMap<String, Double>());
			}
			Map<String, Double> dist = (Map<String, Double>) props.get(prop);
			if (value.isEmpty()) value = "n/a";
			dist.put(value, stat);
		}
		
		clusters.putAll( buildClusterSummaries(clusterProps) );
		
		return clusters;
	}
	
	protected Map<String, ClusterHelper> _setClusterMembers(Map<String, ClusterHelper> clusters, String contextId, String sessionId) throws SQLException, Exception {
		// add the cluster member list
		Map<String, Map<String, List<String>>>  clusterMembers = selectClusterMembers(clusters.keySet(), contextId, sessionId);
		for (String id : clusterMembers.keySet()) {
			List<String> subclusters = clusterMembers.get(id).get("cluster");
			List<String> entities = clusterMembers.get(id).get("entity");
			clusters.get(id).setMembers(entities);
			clusters.get(id).setSubclusters(subclusters);
		}
		return clusters;
	}
	
	@Override
	public boolean clearContext(String contextId, String sessionId) throws AvroRemoteException {
		try {
			Connection connection = _connectionPool.getConnection();
			Statement stmt = connection.createStatement();
			
			// create the session cluster context data table
			String dClusterTable = getNamespaceHandler().tempTableName(
					getNamespaceHandler().tableName(null, DataAccessHelper.DYNAMIC_CLUSTER_TABLE) + "_" + sessionId + "_" + contextId);
			try {
				stmt.execute(getNamespaceHandler().createTempTable(dClusterTable, " ( rowid bigint ) "));
			} catch (Exception e) { /* ignore */ }
			
			StringBuilder sql = new StringBuilder();
			sql = new StringBuilder();
			sql.append("DELETE FROM " + dClusterTable);
			
			s_logger.trace("execute: " + sql.toString());
			int deleted = stmt.executeUpdate(sql.toString());
				
			connection.close();
			stmt.close();
			
			return (deleted > 0);
			
		} catch (Exception e) {
			throw new AvroRemoteException(e);
		}
	}
	
	@Override
	public long removeMembers(List<String> entities, String contextId, String sessionId) throws AvroRemoteException {
		long deleted = 0;
		
		try {
			Connection connection = _connectionPool.getConnection();
			Statement stmt = connection.createStatement();
			
			String gClusterTable = getNamespaceHandler().tableName(null, DataAccessHelper.GLOBAL_CLUSTER_TABLE);
			
			// create the session cluster context data table
			String dClusterTable = getNamespaceHandler().tempTableName(
					getNamespaceHandler().tableName(null, DataAccessHelper.DYNAMIC_CLUSTER_TABLE) + "_" + sessionId + "_" + contextId);
			try {
				stmt.execute(getNamespaceHandler().createTempTable(dClusterTable, " ( rowid bigint ) "));
			} catch (Exception e) { /* ignore */ }
			
			String inClause = DataAccessHelper.createInClause(entities);
			
			StringBuilder deleteSQL = new StringBuilder();
			deleteSQL.append(" DELETE c ");
			deleteSQL.append("   FROM " + dClusterTable + " c ");
			deleteSQL.append("   JOIN " + gClusterTable + " g ");
			deleteSQL.append("     ON c.rowid = g.id ");
			deleteSQL.append("  WHERE g.entityid IN " + inClause);
			
			s_logger.trace("update: " + deleteSQL.toString());
			deleted = stmt.executeUpdate(deleteSQL.toString());
			
			deleteSQL = new StringBuilder();
			deleteSQL.append(" DELETE c ");
			deleteSQL.append("   FROM " + dClusterTable + " as c ");
			deleteSQL.append("   JOIN " + gClusterTable + " g ");
			deleteSQL.append("     ON c.rowid = g.id ");
			deleteSQL.append("   JOIN " + gClusterTable + " g2 ");
			deleteSQL.append("     ON g.entityid = g2.entityid ");
			deleteSQL.append(" WHERE g2.clusterid IN " + inClause);
			
			s_logger.trace("update: " + deleteSQL.toString());
			deleted += stmt.executeUpdate(deleteSQL.toString());
			
			connection.close();
			stmt.close();
		} catch (ClassNotFoundException e) {
			throw new AvroRemoteException(e);
		} catch (SQLException e) {
			throw new AvroRemoteException(e);
		}
		
		return deleted;
	}
	
	/***
	 * Cluster the related nodes of srcNodes in the flow direction specified and store in dstContextId
	 * @param srcNodes
	 * @param dstContextId
	 * @param direction
	 * @return ids of the resulting clusters of related nodes
	 * @throws AvroRemoteException 
	 */
	protected List<String> clusterFlowNodes(List<String> srcNodes, String srcContextId, String dstContextId, String sessionId, FL_DirectionFilter direction) throws AvroRemoteException {
		List<String> clusterIds = new LinkedList<String>();
		
		try {
			Connection connection = _connectionPool.getConnection();
			Statement stmt = connection.createStatement();
			
			String gClusterTable = getNamespaceHandler().tableName(null, DataAccessHelper.GLOBAL_CLUSTER_TABLE);
			
			// create the session cluster context data table
			String dClusterTable = getNamespaceHandler().tempTableName(
					getNamespaceHandler().tableName(null, DataAccessHelper.DYNAMIC_CLUSTER_TABLE) + "_" + sessionId + "_" + srcContextId);
			try {
				stmt.execute(getNamespaceHandler().createTempTable(dClusterTable, " ( rowid bigint ) "));
			} catch (Exception e) { /* ignore */ }
						
			String finFlowTable = getNamespaceHandler().tableName(null, DataAccessHelper.FLOW_TABLE);
			
			// Find all the related entities to srcNodes in direction
			StringBuilder flowSQL = new StringBuilder();
			
			String inClause = DataAccessHelper.createInClause(srcNodes);
			
			String clusterField = "ToEntityId";
			String filterField = "FromEntityId";
		
			if (direction == FL_DirectionFilter.DESTINATION) {
				filterField = "ToEntityId";
				clusterField = "FromEntityId";
			}
			
			long start = System.currentTimeMillis();
			
			flowSQL = new StringBuilder();
			flowSQL.append("SELECT " + clusterField);
			flowSQL.append("  FROM " + finFlowTable); 
			flowSQL.append(" WHERE " + filterField + " IN " + inClause);
			flowSQL.append(" GROUP BY " + clusterField);	
			flowSQL.append(" UNION ALL ");
			flowSQL.append("SELECT " + clusterField);
			flowSQL.append("  FROM " + finFlowTable + " f"); 
			flowSQL.append("  JOIN " + gClusterTable + " g ");
			flowSQL.append("    ON f." + filterField + " = g.entityid ");
			flowSQL.append("  JOIN " + dClusterTable + " c ");
			flowSQL.append("    ON g.id = c.rowid ");
			flowSQL.append(" WHERE g.clusterid IN " + inClause);
			flowSQL.append(" GROUP BY " + clusterField);	
			
			s_logger.trace("execute: " + flowSQL.toString());
			if (stmt.execute(flowSQL.toString())) {
				Set<String> ids = new HashSet<String>();
				ResultSet rs = stmt.getResultSet();
				while (rs.next()) {
					ids.add(rs.getString(clusterField));
				}
				
				s_logger.info("Time to retrieve related entities: " + ((System.currentTimeMillis()-start)/1000));
				stmt.close();
				connection.close();
				
				return _clustering.clusterEntitiesById(new LinkedList<String>(ids), dstContextId, sessionId);
			}
		} catch (Exception e) {
			throw new AvroRemoteException(e);
		}
		
		return clusterIds;
	}

	@Override
	public List<FL_Cluster> getContext(String contextId, String sessionId, boolean computeSummaries) throws AvroRemoteException {
		List<String> clusterIds = new LinkedList<String>();
		try {
			// if cluster summaries were not requested then compute fast context
			if (computeSummaries == false) return getFastContext(contextId, sessionId);
			
			// otherwise we have a lot more work to do to compute application specific cluster summaries
			clusterIds = getContextClusterIds(contextId, sessionId);
			
		} catch (Exception e) {
			throw new AvroRemoteException(e);
		}
		return this.getClusters(clusterIds, contextId, sessionId);
	}
	
	@Override
	public Map<String, List<FL_Link>> getFlowAggregation(
			List<String> entities, List<String> focusEntities,
			FL_DirectionFilter direction, FL_LinkTag tag, FL_DateRange date, 
			String entitiesContextId, String focusContextId, String sessionId)
			throws AvroRemoteException {
		
		Map<String, List<FL_Link>> results = new HashMap<String, List<FL_Link>>();
		
		try {
			Connection connection = _connectionPool.getConnection();
			Statement stmt = connection.createStatement();
			
			if ( (entities == null || entities.isEmpty()) && (focusEntities == null || focusEntities.isEmpty()) ) 
				throw new AvroRemoteException("Exception getting flow aggregation: entities and focusEntities are both empty or null");
			
			List<String> srcNodes = new LinkedList<String>(entities);
			List<String> dstNodes = new LinkedList<String>();
			String srcContextId = entitiesContextId;
			String dstContextId = focusContextId;
			if (focusEntities != null) dstNodes.addAll(focusEntities);
			
			if (direction == FL_DirectionFilter.BOTH) {
				// merge src and dst - we want flow both ways
				srcNodes.addAll(dstNodes);     // TODO BUG! This is not how directions both ways should be handled  
				dstNodes = new LinkedList<String>(srcNodes);
			}
			else if (direction == FL_DirectionFilter.DESTINATION) {
				// swap src and dst - we want reverse flow into src
				List<String> tmpNodes = srcNodes;
				String tmpCtxId = srcContextId;
				srcNodes = dstNodes;
				srcContextId = dstContextId;
				dstNodes = tmpNodes;
				dstContextId = tmpCtxId;
			}
	
			// cluster the flow nodes if none were provided
			if (srcNodes.isEmpty()) {
				srcNodes = clusterFlowNodes(dstNodes, entitiesContextId, focusContextId, sessionId, direction);
			}
			else if (dstNodes.isEmpty()) {
				dstNodes = clusterFlowNodes(srcNodes, entitiesContextId, focusContextId, sessionId, direction);
			}
			
			// No flow can be computed return
			if (srcNodes.isEmpty() || dstNodes.isEmpty()) return results;
			
			DateTime startDate = DataAccessHelper.getStartDate(date);
			DateTime endDate = DataAccessHelper.getEndDate(date);
			
			String gClusterTable = getNamespaceHandler().tableName(null, DataAccessHelper.GLOBAL_CLUSTER_TABLE);
			
			// create the session cluster context data table
			String dSrcClusterTable = getNamespaceHandler().tempTableName(
					getNamespaceHandler().tableName(null, DataAccessHelper.DYNAMIC_CLUSTER_TABLE) + "_" + sessionId + "_" + srcContextId);
			try {
				stmt.execute(getNamespaceHandler().createTempTable(dSrcClusterTable, " ( rowid bigint ) "));
			} catch (Exception e) { /* ignore */ }
			
			// create the session cluster context data table
			String dDstClusterTable = getNamespaceHandler().tempTableName(
					getNamespaceHandler().tableName(null, DataAccessHelper.DYNAMIC_CLUSTER_TABLE) + "_" + sessionId + "_" + dstContextId);
			try {
				stmt.execute(getNamespaceHandler().createTempTable(dDstClusterTable, " ( rowid bigint ) "));
			} catch (Exception e) { /* ignore */ }
			
			String finFlowTable = getNamespaceHandler().tableName(null, DataAccessHelper.FLOW_TABLE);
			String finFlowIntervalTable = getNamespaceHandler().tableName(null, DataAccessHelper.standardTableName(DataAccessHelper.FLOW_TABLE, date.getDurationPerBin().getInterval()));
			String finFlowDateColumn = getNamespaceHandler().columnName("PeriodDate");
			
			long start = System.currentTimeMillis();
			
			// process src nodes in batches 
			List<String> idsCopy = new ArrayList<String>(srcNodes); // copy the ids as we will take 1000 at a time to process and the take method is destructive
			while (idsCopy.size() > 0) {
				List<String> tempSubList = (idsCopy.size() > 1000) ? tempSubList = idsCopy.subList(0, 999) : idsCopy; // get the next 1000
				List<String> subIds = new ArrayList<String>(tempSubList); // copy as the next step is destructive
				tempSubList.clear(); // this clears the IDs from idsCopy as tempSubList is backed by idsCopy 
				
				String fromClause = DataAccessHelper.createInClause(subIds);
				String toClause = DataAccessHelper.createInClause(dstNodes);
				
				//
				/// flow can be from: 1) cluster -> cluster; 2) cluster -> entity; 3) entity -> cluster; 3) entity -> entity  
				//
							
				StringBuilder dateRespectingFlowSQL = new StringBuilder();
				
				dateRespectingFlowSQL.append("SELECT g1.cid as FromEntityId, g2.cid as ToEntityId, SUM(Amount) as Amount ");
				dateRespectingFlowSQL.append("  FROM " + finFlowIntervalTable + " f ");
				dateRespectingFlowSQL.append("  JOIN (SELECT distinct g.entityid as eid, g.clusterid as cid ");
				dateRespectingFlowSQL.append("          FROM " + gClusterTable + " g ");
				dateRespectingFlowSQL.append("          JOIN " + dSrcClusterTable + " c ");
				dateRespectingFlowSQL.append("            ON g.id = c.rowid ");
				dateRespectingFlowSQL.append("         WHERE g.clusterid IN " + fromClause);
				dateRespectingFlowSQL.append("       ) g1 ON f.FromEntityId = g1.eid ");
				dateRespectingFlowSQL.append("  JOIN (SELECT distinct g.entityid as eid, g.clusterid as cid ");
				dateRespectingFlowSQL.append("          FROM " + gClusterTable + " g ");
				dateRespectingFlowSQL.append("          JOIN " + dDstClusterTable + " c ");
				dateRespectingFlowSQL.append("            ON g.id = c.rowid ");
				dateRespectingFlowSQL.append("         WHERE g.clusterid IN " + toClause);
				dateRespectingFlowSQL.append("       ) g2 ON f.ToEntityId = g2.eid ");
				dateRespectingFlowSQL.append(" WHERE " + finFlowDateColumn + " BETWEEN '" + DataAccessHelper.format(startDate) + "'");
				dateRespectingFlowSQL.append("   AND '" + DataAccessHelper.format(endDate) + "'");
				dateRespectingFlowSQL.append(" GROUP BY g1.cid, g2.cid ");
				
				dateRespectingFlowSQL.append(" UNION ALL ");
				
				dateRespectingFlowSQL.append("SELECT g1.cid as FromEntityId, f.ToEntityId, SUM(Amount) as Amount ");
				dateRespectingFlowSQL.append("  FROM " + finFlowIntervalTable + " f ");
				dateRespectingFlowSQL.append("  JOIN (SELECT distinct g.entityid as eid, g.clusterid as cid ");
				dateRespectingFlowSQL.append("          FROM " + gClusterTable + " g ");
				dateRespectingFlowSQL.append("          JOIN " + dSrcClusterTable + " c ");
				dateRespectingFlowSQL.append("            ON g.id = c.rowid ");
				dateRespectingFlowSQL.append("         WHERE g.clusterid IN " + fromClause);
				dateRespectingFlowSQL.append("       ) g1 ON f.FromEntityId = g1.eid ");
				dateRespectingFlowSQL.append(" WHERE " + finFlowDateColumn + " BETWEEN '" + DataAccessHelper.format(startDate) + "'");
				dateRespectingFlowSQL.append("   AND '" + DataAccessHelper.format(endDate) + "'");
				dateRespectingFlowSQL.append("   AND f.ToEntityId IN " + toClause);
				dateRespectingFlowSQL.append(" GROUP BY g1.cid, f.ToEntityId ");
				
				dateRespectingFlowSQL.append(" UNION ALL ");
				
				dateRespectingFlowSQL.append("SELECT f.FromEntityId, g2.cid as ToEntityId, SUM(Amount) as Amount ");
				dateRespectingFlowSQL.append("  FROM " + finFlowIntervalTable + " f ");
				dateRespectingFlowSQL.append("  JOIN (SELECT distinct g.entityid as eid, g.clusterid as cid ");
				dateRespectingFlowSQL.append("          FROM " + gClusterTable + " g ");
				dateRespectingFlowSQL.append("          JOIN " + dDstClusterTable + " c ");
				dateRespectingFlowSQL.append("            ON g.id = c.rowid ");
				dateRespectingFlowSQL.append("         WHERE g.clusterid IN " + toClause);
				dateRespectingFlowSQL.append("       ) g2 ON f.ToEntityId = g2.eid ");
				dateRespectingFlowSQL.append(" WHERE " + finFlowDateColumn + " BETWEEN '" + DataAccessHelper.format(startDate) + "'");
				dateRespectingFlowSQL.append("   AND '" + DataAccessHelper.format(endDate) + "'");
				dateRespectingFlowSQL.append("   AND f.FromEntityId IN " + fromClause);
				dateRespectingFlowSQL.append(" GROUP BY f.FromEntityId, g2.cid ");
				
				dateRespectingFlowSQL.append(" UNION ALL ");
				
				dateRespectingFlowSQL.append("SELECT FromEntityId, ToEntityId, SUM(Amount) as Amount ");
				dateRespectingFlowSQL.append("  FROM " + finFlowIntervalTable + " f ");
				dateRespectingFlowSQL.append(" WHERE FromEntityId IN " + fromClause);
				dateRespectingFlowSQL.append("   AND ToEntityId IN " + toClause);
				dateRespectingFlowSQL.append("   AND " + finFlowDateColumn + " BETWEEN '" + DataAccessHelper.format(startDate) + "'");
				dateRespectingFlowSQL.append("   AND '" + DataAccessHelper.format(endDate) + "'");
				dateRespectingFlowSQL.append(" GROUP BY FromEntityId, ToEntityId");
					
				// retrieve aggregate links irrespective of date range
				StringBuilder flowSQL = new StringBuilder();
				flowSQL.append("SELECT g1.cid as FromEntityId, g2.cid as ToEntityId, COUNT(*) as linkcount ");
				flowSQL.append("  FROM " + finFlowTable + " f ");
				flowSQL.append("  JOIN (SELECT distinct g.entityid as eid, g.clusterid as cid ");
				flowSQL.append("          FROM " + gClusterTable + " g ");
				flowSQL.append("          JOIN " + dSrcClusterTable + " c ");
				flowSQL.append("            ON g.id = c.rowid ");
				flowSQL.append("         WHERE g.clusterid IN " + fromClause);
				flowSQL.append("       ) g1 ON f.FromEntityId = g1.eid ");
				flowSQL.append("  JOIN (SELECT distinct g.entityid as eid, g.clusterid as cid ");
				flowSQL.append("          FROM " + gClusterTable + " g ");
				flowSQL.append("          JOIN " + dDstClusterTable + " c ");
				flowSQL.append("            ON g.id = c.rowid ");
				flowSQL.append("         WHERE g.clusterid IN " + toClause);
				flowSQL.append("       ) g2 ON f.ToEntityId = g2.eid ");
				flowSQL.append(" GROUP BY g1.cid, g2.cid ");
				
				flowSQL.append(" UNION ALL ");
				
				flowSQL.append("SELECT g1.cid as FromEntityId, f.ToEntityId, COUNT(*) as linkcount ");
				flowSQL.append("  FROM " + finFlowTable + " f ");
				flowSQL.append("  JOIN (SELECT distinct g.entityid as eid, g.clusterid as cid ");
				flowSQL.append("          FROM " + gClusterTable + " g ");
				flowSQL.append("          JOIN " + dSrcClusterTable + " c ");
				flowSQL.append("            ON g.id = c.rowid ");
				flowSQL.append("         WHERE g.clusterid IN " + fromClause);
				flowSQL.append("       ) g1 ON f.FromEntityId = g1.eid ");
				flowSQL.append(" WHERE f.ToEntityId IN " + toClause);
				flowSQL.append(" GROUP BY g1.cid, f.ToEntityId ");
				
				flowSQL.append(" UNION ALL ");
				
				flowSQL.append("SELECT f.FromEntityId, g2.cid as ToEntityId, COUNT(*) as linkcount ");
				flowSQL.append("  FROM " + finFlowTable + " f ");
				flowSQL.append("  JOIN (SELECT distinct g.entityid as eid, g.clusterid as cid ");
				flowSQL.append("          FROM " + gClusterTable + " g ");
				flowSQL.append("          JOIN " + dDstClusterTable + " c ");
				flowSQL.append("            ON g.id = c.rowid ");
				flowSQL.append("         WHERE g.clusterid IN " + toClause);
				flowSQL.append("       ) g2 ON f.ToEntityId = g2.eid ");
				flowSQL.append(" WHERE f.FromEntityId IN " + fromClause);
				flowSQL.append(" GROUP BY f.FromEntityId, g2.cid ");
				
				flowSQL.append(" UNION ALL ");
				
				flowSQL.append("SELECT FromEntityId, ToEntityId, COUNT(*) as linkcount ");
				flowSQL.append("  FROM " + finFlowTable + " f ");  
				flowSQL.append(" WHERE FromEntityId IN " + fromClause);
				flowSQL.append("   AND ToEntityId IN " + toClause);
				flowSQL.append(" GROUP BY FromEntityId, ToEntityId");
				
				// create (from, to) = Amount lookup table
				Map<String, Map<String, Double>> fromToAmountMap = new HashMap<String, Map<String, Double>>();
				
				s_logger.trace("execute: " + dateRespectingFlowSQL.toString());
				if (stmt.execute(dateRespectingFlowSQL.toString())) {
					ResultSet rs = stmt.getResultSet();
					while (rs.next()) {
						String from = rs.getString("FromEntityId");
						String to = rs.getString("ToEntityId");
						Double amount = rs.getDouble("Amount");
						if (fromToAmountMap.containsKey(from)) {
							if (fromToAmountMap.get(from).containsKey(to)) {
								throw new AssertionError("Group by clause in dateRespectingFlowSQL erroneously created duplicates"); 
							} else {
								fromToAmountMap.get(from).put(to, amount);
							}
						} else {
							Map<String, Double> toAmountMap = new HashMap<String, Double>();
							toAmountMap.put(to, amount);
							fromToAmountMap.put(from, toAmountMap);							
						}
					}
				}
		
				s_logger.trace("execute: " + flowSQL.toString());
				if (stmt.execute(flowSQL.toString())) {
					ResultSet rs = stmt.getResultSet();
					while (rs.next()) {
						String from = rs.getString("FromEntityId");
						String to = rs.getString("ToEntityId");
						Integer count = rs.getInt("linkcount");

						String keyId = from;
						if (entities.contains(to)) {
							keyId = to;
						}
						List<FL_Link> linkList = results.get(keyId);
						if (linkList == null) {
							linkList = new LinkedList<FL_Link>();
							results.put(keyId, linkList);
						}
	
						// only use the amount calculated above with the date respecting FLOW map
						Double amount = (fromToAmountMap.containsKey(from) && fromToAmountMap.get(from).containsKey(to)) ? amount = fromToAmountMap.get(from).get(to) : 0.0;
						List<FL_Property> properties = new ArrayList<FL_Property>();
						properties.add(new PropertyHelper(FL_PropertyTag.AMOUNT, amount));
						properties.add(new PropertyHelper(FL_PropertyTag.COUNT, count));
						
						//Finally, create the link between the two, and add it to the map.
						FL_Link link = new FL_Link(Collections.singletonList(FL_LinkTag.FINANCIAL), from, to, true, null, null, properties);
						linkList.add(link);
					}
				}
			}
			
			s_logger.info("Time to calculate cluster aggreagate flow: " + ((System.currentTimeMillis() - start)/1000));
			
			stmt.close();
			connection.close();
		} catch (ClassNotFoundException e) {
			throw new AvroRemoteException(e);
		} catch (SQLException e) {
			throw new AvroRemoteException(e);
		} finally { }
	
		return results;
	}

	@Override
	public Map<String, List<FL_Link>> getTimeSeriesAggregation(
			List<String> entities, List<String> focusEntities,
			FL_LinkTag tag, FL_DateRange date, String entitiesContextId, String focusContextId, String sessionId) throws AvroRemoteException {
		Map<String, List<FL_Link>> results = new HashMap<String, List<FL_Link>>();
		
		try {
			Connection connection = _connectionPool.getConnection();
			Statement stmt = connection.createStatement();
			
			if ( (entities == null || entities.isEmpty()) && (focusEntities == null || focusEntities.isEmpty()) ) 
				throw new AvroRemoteException("Exception getting flow aggregation: entities and focusEntities are both empty or null");
			
			List<String> srcNodes = new LinkedList<String>(entities);
			Set<String> focusNodes = new HashSet<String>();
			String srcContextId = entitiesContextId;
			String dstContextId = focusContextId;
			
			if (focusEntities != null) focusNodes.addAll(focusEntities);
			
			DateTime startDate = DataAccessHelper.getStartDate(date);
			DateTime endDate = DataAccessHelper.getEndDate(date);
			
			String gClusterTable = getNamespaceHandler().tableName(null, DataAccessHelper.GLOBAL_CLUSTER_TABLE);
			
			// create the session cluster context data table
			String dSrcClusterTable = getNamespaceHandler().tempTableName(
					getNamespaceHandler().tableName(null, DataAccessHelper.DYNAMIC_CLUSTER_TABLE) + "_" + sessionId + "_" + srcContextId);
			try {
				stmt.execute(getNamespaceHandler().createTempTable(dSrcClusterTable, " ( rowid bigint ) "));
			} catch (Exception e) { /* ignore */ }
						
			// create the session cluster context data table
			String dDstClusterTable = getNamespaceHandler().tempTableName(
					getNamespaceHandler().tableName(null, DataAccessHelper.DYNAMIC_CLUSTER_TABLE) + "_" + sessionId + "_" + dstContextId);
			try {
				stmt.execute(getNamespaceHandler().createTempTable(dDstClusterTable, " ( rowid bigint ) "));
			} catch (Exception e) { /* ignore */ }
			
			String finFlowIntervalTable = getNamespaceHandler().tableName(null, DataAccessHelper.standardTableName(
					DataAccessHelper.FLOW_TABLE, date.getDurationPerBin().getInterval()));
			String finEntityIntervalTable = getNamespaceHandler().tableName(null, DataAccessHelper.standardTableName(
					DataAccessHelper.ENTITY_TABLE, date.getDurationPerBin().getInterval()));
			String finFlowDateColumn = getNamespaceHandler().columnName("PeriodDate");
			String dateColNoEscape = unescapeColumnName(finFlowDateColumn);

			// process src nodes in batches 
			List<String> idsCopy = new ArrayList<String>(srcNodes); // copy the ids as we will take 1000 at a time to process and the take method is destructive
			while (idsCopy.size() > 0) {
				List<String> tempSubList = (idsCopy.size() > 1000) ? tempSubList = idsCopy.subList(0, 999) : idsCopy; // get the next 1000
				List<String> subIds = new ArrayList<String>(tempSubList); // copy as the next step is destructive
				tempSubList.clear(); // this clears the IDs from idsCopy as tempSubList is backed by idsCopy 
				
				String fromClause = DataAccessHelper.createInClause(subIds);
				String toClause = DataAccessHelper.createInClause(focusNodes);
				
				//
				/// flow can be from: 1) cluster -> cluster; 2) cluster -> entity; 3) entity -> cluster; 3) entity -> entity  
				//
				
				StringBuilder focusFlowSQL = new StringBuilder();
				focusFlowSQL.append("SELECT g1.clusterid as FromEntityId, g2.clusterid as ToEntityId, " + finFlowDateColumn + ", SUM(Amount) as Amount ");
				focusFlowSQL.append("  FROM " + finFlowIntervalTable + " f ");
				focusFlowSQL.append("  JOIN " + gClusterTable + " g1 ");
				focusFlowSQL.append("    ON f.FromEntityId = g1.entityid ");
				focusFlowSQL.append("  JOIN " + dSrcClusterTable + " c1 ");
				focusFlowSQL.append("    ON g1.id = c1.rowid ");
				focusFlowSQL.append("  JOIN " + gClusterTable + " g2 ");
				focusFlowSQL.append("    ON f.ToEntityId = g2.entityid"); 
				focusFlowSQL.append("  JOIN " + dDstClusterTable + " c2 ");
				focusFlowSQL.append("    ON g2.id = c2.rowid ");
				focusFlowSQL.append(" WHERE g1.clusterid IN " + fromClause);
				focusFlowSQL.append("   AND g2.clusterid IN " + toClause);
				focusFlowSQL.append("   AND " + finFlowDateColumn + " BETWEEN '" + DataAccessHelper.format(startDate) + "'");
				focusFlowSQL.append("   AND '" + DataAccessHelper.format(endDate) + "'");
				focusFlowSQL.append(" GROUP BY g1.clusterid, g2.clusterid, " + finFlowDateColumn + "");
				
				focusFlowSQL.append(" UNION ALL ");
				
				focusFlowSQL.append("SELECT g1.clusterid as FromEntityId, g2.clusterid as ToEntityId, " + finFlowDateColumn + ", SUM(Amount) as Amount ");
				focusFlowSQL.append("  FROM " + finFlowIntervalTable + " f ");
				focusFlowSQL.append("  JOIN " + gClusterTable + " g1 ");
				focusFlowSQL.append("    ON f.FromEntityId = g1.entityid "); 
				focusFlowSQL.append("  JOIN " + dDstClusterTable + " c1 ");
				focusFlowSQL.append("    ON g1.id = c1.rowid ");
				focusFlowSQL.append("  JOIN " + gClusterTable + " g2 ");
				focusFlowSQL.append("    ON f.ToEntityId = g2.entityid");
				focusFlowSQL.append("  JOIN " + dSrcClusterTable + " c2 ");
				focusFlowSQL.append("    ON g2.id = c2.rowid ");
				focusFlowSQL.append(" WHERE g1.clusterid IN " + toClause);
				focusFlowSQL.append("   AND g2.clusterid IN " + fromClause);
				focusFlowSQL.append("   AND " + finFlowDateColumn + " BETWEEN '" + DataAccessHelper.format(startDate) + "'");
				focusFlowSQL.append("   AND '" + DataAccessHelper.format(endDate) + "'");
				focusFlowSQL.append(" GROUP BY g1.clusterid, g2.clusterid, " + finFlowDateColumn + "");
				
				focusFlowSQL.append(" UNION ALL ");
				
				focusFlowSQL.append("SELECT g1.clusterid as FromEntityId, ToEntityId, " + finFlowDateColumn + ", SUM(Amount) as Amount ");
				focusFlowSQL.append("  FROM " + finFlowIntervalTable + " f ");
				focusFlowSQL.append("  JOIN " + gClusterTable + " g1 ");
				focusFlowSQL.append("    ON f.FromEntityId = g1.entityid "); 
				focusFlowSQL.append("  JOIN " + dSrcClusterTable + " c1 ");
				focusFlowSQL.append("    ON g1.id = c1.rowid ");
				focusFlowSQL.append(" WHERE g1.clusterid IN " + fromClause);
				focusFlowSQL.append("   AND ToEntityId IN " + toClause);
				focusFlowSQL.append("   AND " + finFlowDateColumn + " BETWEEN '" + DataAccessHelper.format(startDate) + "'");
				focusFlowSQL.append("   AND '" + DataAccessHelper.format(endDate) + "'");
				focusFlowSQL.append(" GROUP BY g1.clusterid, ToEntityId, " + finFlowDateColumn + "");
				
				focusFlowSQL.append(" UNION ALL ");
				
				focusFlowSQL.append("SELECT g1.clusterid as FromEntityId, ToEntityId, " + finFlowDateColumn + ", SUM(Amount) as Amount ");
				focusFlowSQL.append("  FROM " + finFlowIntervalTable + " f ");
				focusFlowSQL.append("  JOIN " + gClusterTable + " g1 ");
				focusFlowSQL.append("    ON f.FromEntityId = g1.entityid "); 
				focusFlowSQL.append("  JOIN " + dDstClusterTable + " c1 ");
				focusFlowSQL.append("    ON g1.id = c1.rowid ");
				focusFlowSQL.append(" WHERE g1.clusterid IN " + toClause);
				focusFlowSQL.append("   AND ToEntityId IN " + fromClause);
				focusFlowSQL.append("   AND " + finFlowDateColumn + " BETWEEN '" + DataAccessHelper.format(startDate) + "'");
				focusFlowSQL.append("   AND '" + DataAccessHelper.format(endDate) + "'");
				focusFlowSQL.append(" GROUP BY g1.clusterid, ToEntityId, " + finFlowDateColumn + "");
				
				focusFlowSQL.append(" UNION ALL ");
				
				focusFlowSQL.append("SELECT FromEntityId, g2.clusterid as ToEntityId, " + finFlowDateColumn + ", SUM(Amount) as Amount ");
				focusFlowSQL.append("  FROM " + finFlowIntervalTable + " f ");
				focusFlowSQL.append("  JOIN " + gClusterTable + " g2 ");
				focusFlowSQL.append("    ON f.ToEntityId = g2.entityid "); 
				focusFlowSQL.append("  JOIN " + dDstClusterTable + " c2 ");
				focusFlowSQL.append("    ON g2.id = c2.rowid ");
				focusFlowSQL.append(" WHERE FromEntityId IN " + fromClause);
				focusFlowSQL.append("   AND g2.clusterid IN " + toClause);
				focusFlowSQL.append("   AND " + finFlowDateColumn + " BETWEEN '" + DataAccessHelper.format(startDate) + "'");
				focusFlowSQL.append("   AND '" + DataAccessHelper.format(endDate) + "'");
				focusFlowSQL.append(" GROUP BY FromEntityId, g2.clusterid, " + finFlowDateColumn + "");
				
				focusFlowSQL.append(" UNION ALL ");
				
				focusFlowSQL.append("SELECT FromEntityId, g2.clusterid as ToEntityId, " + finFlowDateColumn + ", SUM(Amount) as Amount ");
				focusFlowSQL.append("  FROM " + finFlowIntervalTable + " f ");
				focusFlowSQL.append("  JOIN " + gClusterTable + " g2 ");
				focusFlowSQL.append("    ON f.ToEntityId = g2.entityid "); 
				focusFlowSQL.append("  JOIN " + dSrcClusterTable + " c2 ");
				focusFlowSQL.append("    ON g2.id = c2.rowid ");
				focusFlowSQL.append(" WHERE FromEntityId IN " + toClause);
				focusFlowSQL.append("   AND g2.clusterid IN " + fromClause);
				focusFlowSQL.append("   AND " + finFlowDateColumn + " BETWEEN '" + DataAccessHelper.format(startDate) + "'");
				focusFlowSQL.append("   AND '" + DataAccessHelper.format(endDate) + "'");
				focusFlowSQL.append(" GROUP BY FromEntityId, g2.clusterid, " + finFlowDateColumn + "");
				
				focusFlowSQL.append(" UNION ALL ");
				
				focusFlowSQL.append("SELECT FromEntityId, ToEntityId, " + finFlowDateColumn + ", SUM(Amount) as Amount ");
				focusFlowSQL.append("  FROM " + finFlowIntervalTable + " f ");
				focusFlowSQL.append(" WHERE FromEntityId IN " + fromClause);
				focusFlowSQL.append("   AND ToEntityId IN " + toClause);
				focusFlowSQL.append("   AND " + finFlowDateColumn + " BETWEEN '" + DataAccessHelper.format(startDate) + "'");
				focusFlowSQL.append("   AND '" + DataAccessHelper.format(endDate) + "'");
				focusFlowSQL.append(" GROUP BY FromEntityId, ToEntityId, " + finFlowDateColumn + "");
				
				focusFlowSQL.append(" UNION ALL ");
				
				focusFlowSQL.append("SELECT FromEntityId, ToEntityId, " + finFlowDateColumn + ", SUM(Amount) as Amount ");
				focusFlowSQL.append("  FROM " + finFlowIntervalTable + " f ");
				focusFlowSQL.append(" WHERE FromEntityId IN " + toClause);
				focusFlowSQL.append("   AND ToEntityId IN " + fromClause);
				focusFlowSQL.append("   AND " + finFlowDateColumn + " BETWEEN '" + DataAccessHelper.format(startDate) + "'");
				focusFlowSQL.append("   AND '" + DataAccessHelper.format(endDate) + "'");
				focusFlowSQL.append(" GROUP BY FromEntityId, ToEntityId, " + finFlowDateColumn + "");
					
				StringBuilder tsSQL = new StringBuilder();
				tsSQL.append("SELECT EntityId, " + finFlowDateColumn + ", InboundAmount, OutboundAmount ");
				tsSQL.append("  FROM " + finEntityIntervalTable);
				tsSQL.append(" WHERE " + finFlowDateColumn + " BETWEEN '"+DataAccessHelper.format(startDate)+"' AND '"+DataAccessHelper.format(endDate)+"'");
				tsSQL.append("   AND EntityId IN " + fromClause);
				
				tsSQL.append(" UNION ALL ");
				
				tsSQL.append("SELECT g.clusterid as EntityId, f." + finFlowDateColumn + ", SUM(f.InboundAmount) as InboundAmount, SUM(f.OutboundAmount) as OutboundAmount ");
				tsSQL.append("  FROM " + finEntityIntervalTable + " f ");
				tsSQL.append("  JOIN " + gClusterTable + " g ");
				tsSQL.append("    ON f.EntityId = g.entityid "); 
				tsSQL.append("  JOIN " + dSrcClusterTable + " c ");
				tsSQL.append("    ON g.id = c.rowid ");
				tsSQL.append(" WHERE " + finFlowDateColumn + " BETWEEN '"+DataAccessHelper.format(startDate)+"' AND '"+DataAccessHelper.format(endDate)+"'");
				tsSQL.append("   AND g.clusterid IN " + fromClause);
				tsSQL.append(" GROUP BY g.clusterid, f." + finFlowDateColumn + "");
					
				s_logger.trace("execute: " + focusFlowSQL.toString());
				if (stmt.execute(focusFlowSQL.toString())) {
					ResultSet rs = stmt.getResultSet();
					while (rs.next()) {
						String from = rs.getString("FromEntityId");
						String to = rs.getString("ToEntityId");
						Double amount = rs.getDouble("Amount");
						Date rsDate = rs.getDate(dateColNoEscape);
						
						String keyId = from;
						if (entities.contains(to)) {
							keyId = to;
						}
						List<FL_Link> linkList = results.get(keyId);
						if (linkList == null) {
							linkList = new LinkedList<FL_Link>();
							results.put(keyId, linkList);
						}
						
						// only use the amount calculated above with the date respecting FLOW map
						List<FL_Property> properties = new ArrayList<FL_Property>();
						properties.add(new PropertyHelper(FL_PropertyTag.AMOUNT, amount));
						properties.add(new PropertyHelper(FL_PropertyTag.DATE, rsDate));
	
						FL_Link link = new FL_Link(Collections.singletonList(FL_LinkTag.FINANCIAL), from, to, true, null, null, properties);
						linkList.add(link);
					}
				}
		
				s_logger.trace("execute: " + tsSQL.toString());
				if (stmt.execute(tsSQL.toString())) {
					ResultSet rs = stmt.getResultSet();
					while (rs.next()) {
						String entity = rs.getString("EntityId");
						Double inboundAmount = rs.getDouble("InboundAmount");
						Double outboundAmount = rs.getDouble("OutboundAmount");
						Date rsDate = rs.getDate(dateColNoEscape);

						List<FL_Link> linkList = results.get(entity);
						if (linkList == null) {
							linkList = new LinkedList<FL_Link>();
							results.put(entity, linkList);
						}
	
						List<FL_Property> inProperties = new ArrayList<FL_Property>();
						inProperties.add(new PropertyHelper(FL_PropertyTag.AMOUNT, inboundAmount));
						inProperties.add(new PropertyHelper(FL_PropertyTag.DATE, rsDate));

						List<FL_Property> outProperties = new ArrayList<FL_Property>();
						outProperties.add(new PropertyHelper(FL_PropertyTag.AMOUNT, outboundAmount));
						outProperties.add(new PropertyHelper(FL_PropertyTag.DATE, rsDate));

						FL_Link inLink = new FL_Link(Collections.singletonList(FL_LinkTag.FINANCIAL), null, entity, false, null, null, inProperties);
						FL_Link outLink = new FL_Link(Collections.singletonList(FL_LinkTag.FINANCIAL), entity, null, false, null, null, outProperties);
						linkList.add(inLink);
						linkList.add(outLink);
					}
					rs.close();
				}
			}
			
			stmt.close();
			connection.close();
		} catch (ClassNotFoundException e) {
			throw new AvroRemoteException(e);
		} catch (SQLException e) {
			throw new AvroRemoteException(e);
		} finally { }
	
		return results;
	}
	
	@Override
	public Map<String, List<FL_Link>> getAllTransactions(List<String> entities,
			FL_LinkTag tag, FL_DateRange date, FL_SortBy sort,
			List<String> linkFilter, long max, String contextId, String sessionId)
			throws AvroRemoteException {
		// TODO Auto-generated method stub
		return null;
	}
	
	
	
	private List<FL_Cluster> getFastContext(String contextId, String sessionId)
			throws AvroRemoteException {
		try {
			ArrayList<FL_Cluster> results = new ArrayList<FL_Cluster>();	
			
			Connection conn = _connectionPool.getConnection(); 
			Statement stmt = conn.createStatement();
		
			String gClusterTable = getNamespaceHandler().tableName(null, DataAccessHelper.GLOBAL_CLUSTER_TABLE);
			
			// create the session cluster context data table
			String dClusterTable = getNamespaceHandler().tempTableName(
					getNamespaceHandler().tableName(null, DataAccessHelper.DYNAMIC_CLUSTER_TABLE) + "_" + sessionId + "_" + contextId);
			try {
				stmt.execute(getNamespaceHandler().createTempTable(dClusterTable, " ( rowid bigint ) "));
			} catch (Exception e) { /* ignore */ }
			
			long start = System.currentTimeMillis();
			 
			StringBuilder sql = new StringBuilder();
			sql.append(" SELECT g.clusterid, g.parentid, g.rootid, g.hierarchylevel, '' as property, '' as value, 0 as stat ");
			sql.append("   FROM " + gClusterTable + " g ");
			sql.append("   JOIN " + dClusterTable + " c ");
			sql.append("     ON g.id = c.rowid "); 
			sql.append("  GROUP BY g.clusterid, g.parentid, g.rootid, g.hierarchylevel ");
	
			if (stmt.execute(sql.toString())) {				
				ResultSet rs = stmt.getResultSet();
				try {	
					Map<String, ClusterHelper> clusters = _createClustersFromResultSet(rs);
					// add the cluster member list
					Map<String, Map<String, List<String>>>  clusterMembers = selectClusterMembers(contextId, sessionId);
					for (String id : clusterMembers.keySet()) {
						Map<String, List<String>> clusterMember = clusterMembers.get(id);
						if (clusterMember != null) {
							List<String> subclusters = clusterMember.get("cluster");
							List<String> entities = clusterMember.get("entity");
							ClusterHelper clusterHelper = clusters.get(id);
							if (clusterHelper != null) {
								clusterHelper.setMembers(entities);
								clusterHelper.setSubclusters(subclusters);
							} else {
								System.out.println("Cluster " + id + " has no cluster helper");
							}
						}
					}
					results.addAll( clusters.values() );
				} finally {
					rs.close();	
				}
			}
			
			s_logger.info("Time to compute cluster summaries: " + ((System.currentTimeMillis() - start)/1000));
			
			stmt.close();
			conn.close();
			
			return results;
		
		} catch (Exception e) {
			throw new AvroRemoteException("Exception in getEntities",e);
		}
	}
	
	private static Pattern COLUMN_PATTERN = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*", 0);
	
	private static String unescapeColumnName(String columnName) {
		Matcher m = COLUMN_PATTERN.matcher(columnName);
		if (m.find()) {
			return m.group();
		}
		return columnName;

	}
	
}
