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
package influent.kiva.server.clustering;

import influent.idl.FL_Cluster;
import influent.idl.FL_Clustering;
import influent.idl.FL_Geocoding;
import influent.idl.FL_Uncertainty;
import influent.idlhelper.ClusterHelper;
import influent.midtier.kiva.data.KivaID.EntityType;
import influent.server.dataaccess.ClusteringDataAccess;
import influent.server.dataaccess.DataAccessHelper;
import influent.server.dataaccess.DataNamespaceHandler;
import influent.server.utilities.SQLConnectionPool;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.avro.AvroRemoteException;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 */

public class KivaClusteringDataAccess extends ClusteringDataAccess {
	
	private static Logger s_logger = LoggerFactory.getLogger(KivaClusteringDataAccess.class);
	
	public KivaClusteringDataAccess(SQLConnectionPool connectionPool, FL_Clustering clustering, FL_Geocoding geocoding, DataNamespaceHandler namespaceHandler) throws ClassNotFoundException, SQLException, JSONException {
		super(connectionPool, clustering, geocoding, namespaceHandler);
	}
	
	/*package*/ Map<String, ClusterHelper> selectPartners(Collection<String> partnerClusterIds, String contextId, String sessionId) throws SQLException, Exception {
		Map<String, ClusterHelper> results = new HashMap<String,ClusterHelper>();
		
		// retrieve partner cluster summaries
		if (partnerClusterIds == null || partnerClusterIds.isEmpty()) return results;
		
		Connection conn = _connectionPool.getConnection(); 
		Statement stmt = conn.createStatement();
		
		String gClusterTable = getNamespaceHandler().tableName(null, DataAccessHelper.GLOBAL_CLUSTER_TABLE);
		String dClusterTable = getNamespaceHandler().tempTableName(
				getNamespaceHandler().tableName(null, DataAccessHelper.DYNAMIC_CLUSTER_TABLE) + "_" + sessionId + "_" + contextId);
		
		List<String> idsCopy = new ArrayList<String>(partnerClusterIds); // copy the ids as we will take 1000 at a time to process and the take method is destructive
		
		long start = System.currentTimeMillis();
		
		while (idsCopy.size() > 0) {
			List<String> tempSubList = (idsCopy.size() > 1000) ? tempSubList = idsCopy.subList(0, 999) : idsCopy; // get the next 1000
			List<String> subIds = new ArrayList<String>(tempSubList); // copy as the next step is destructive
			tempSubList.clear(); // this clears the IDs from idsCopy as tempSubList is backed by idsCopy 
			
			String inClause = DataAccessHelper.createInClause(subIds);
			
			// cluster summary is # members, most frequent label, freq count of top ten country codes
			StringBuilder sql = new StringBuilder();
			sql.append("SELECT clusterid, parentid, rootid, hierarchylevel, 'members' as property, 'partner' as value, stat ");
			sql.append("  FROM ");
			sql.append("    (SELECT g.clusterid, g.parentid, g.rootid, g.hierarchylevel, count(g.clusterid) as stat ");
			sql.append("       FROM " + gClusterTable + " g ");
			sql.append("       JOIN " + dClusterTable + " c ");
			sql.append("         ON g.id = c.rowid ");
			sql.append("      WHERE g.clusterid IN " + inClause);
//			sql.append("        AND c.contextid = '" + contextId + "'"); 
			sql.append("      GROUP BY g.clusterid, g.parentid, g.rootid, g.hierarchylevel) q ");
			sql.append(" UNION ALL ");
			sql.append("SELECT clusterid, parentid, rootid, hierarchylevel, 'label' as property, value, stat ");
			sql.append("  FROM ");
			sql.append("    (SELECT clusterid, parentid, rootid, hierarchylevel, value, ROW_NUMBER() OVER (PARTITION BY clusterid Order by stat desc ) as seq, stat ");
			sql.append("       FROM "); 
			sql.append("         (SELECT g.clusterid, g.parentid, g.rootid, g.hierarchylevel, l.partners_name as value, count(l.partners_name) as stat ");
			sql.append("            FROM " + gClusterTable + " g ");
			sql.append("            JOIN " + dClusterTable + " c ");
			sql.append("              ON g.id = c.rowid ");
			sql.append("            JOIN entityId_to_partnerId e ");
			sql.append("              ON g.entityid = e.entityid ");
			sql.append("            JOIN partnersMetaData l ");
			sql.append("              ON l.partners_id = e.partnerid ");
			sql.append("           WHERE g.clusterid IN " + inClause);
//			sql.append("             AND c.contextid = '" + contextId + "'");
			sql.append("           GROUP BY g.clusterid, g.parentid, g.rootid, g.hierarchylevel, l.partners_name) q1 ) q2 ");
			sql.append("      WHERE seq = 1 ");
			sql.append(" UNION ALL ");
			sql.append("SELECT clusterid, parentid, rootid, hierarchylevel, 'location-dist' as property, value, stat ");
			sql.append("  FROM ");
			sql.append("    (SELECT clusterid, parentid, rootid, hierarchylevel, value, ROW_NUMBER() OVER (PARTITION BY clusterid Order by stat desc ) as seq, stat ");
			sql.append("       FROM "); 
			sql.append("         (SELECT g.clusterid, g.parentid, g.rootid, g.hierarchylevel, l.partners_countries_isoCode as value, count(l.partners_countries_isoCode) as stat ");
			sql.append("            FROM " + gClusterTable + " g ");
			sql.append("            JOIN " + dClusterTable + " c ");
			sql.append("              ON g.id = c.rowid ");
			sql.append("            JOIN entityId_to_partnerId e ");
			sql.append("              ON g.entityid = e.entityid ");
			sql.append("            JOIN partnerCountries l ");
			sql.append("              ON l.partners_id = e.partnerid ");
			sql.append("           WHERE g.clusterid IN " + inClause);
//			sql.append("             AND c.contextid = '" + contextId + "'");
			sql.append("           GROUP BY g.clusterid, g.parentid, g.rootid, g.hierarchylevel, l.partners_countries_isoCode) q1 ) q2 ");
			sql.append("       WHERE seq < 10 ");
			
			if (stmt.execute(sql.toString())) {				
				ResultSet rs = stmt.getResultSet();
				try {	
					results.putAll( createClustersFromResultSet(rs, contextId, sessionId) );
				} finally {				
					rs.close();
				}
			}
		}
		s_logger.info("Total Time to compute partner cluster summaries: " + ((System.currentTimeMillis()-start)/1000));
		stmt.close();
		conn.close();

		return results;
	}
	
	/*package*/ Map<String, ClusterHelper> selectLenders(Collection<String> lenderClusterIds, String contextId, String sessionId) throws SQLException, Exception {
		Map<String, ClusterHelper> results = new HashMap<String,ClusterHelper>();
		
		// retrieve partner cluster summaries
		if (lenderClusterIds == null || lenderClusterIds.isEmpty()) return results;
		
		Connection conn = _connectionPool.getConnection(); 
		Statement stmt = conn.createStatement();
		
		String gClusterTable = getNamespaceHandler().tableName(null, DataAccessHelper.GLOBAL_CLUSTER_TABLE);
		String dClusterTable = getNamespaceHandler().tempTableName(
				getNamespaceHandler().tableName(null, DataAccessHelper.DYNAMIC_CLUSTER_TABLE) + "_" + sessionId + "_" + contextId);
		
		List<String> idsCopy = new ArrayList<String>(lenderClusterIds); // copy the ids as we will take 1000 at a time to process and the take method is destructive
		
		long start = System.currentTimeMillis();
		
		while (idsCopy.size() > 0) {
			List<String> tempSubList = (idsCopy.size() > 1000) ? tempSubList = idsCopy.subList(0, 999) : idsCopy; // get the next 1000
			List<String> subIds = new ArrayList<String>(tempSubList); // copy as the next step is destructive
			tempSubList.clear(); // this clears the IDs from idsCopy as tempSubList is backed by idsCopy 
		
			String inClause = DataAccessHelper.createInClause(subIds);
			
			// cluster summary is # members, most frequent label, freq count of top ten country codes	
			StringBuilder sql = new StringBuilder();
			sql.append("SELECT clusterid, parentid, rootid, hierarchylevel, 'members' as property, 'lender' as value, stat ");
			sql.append("  FROM ");
			sql.append("    (SELECT g.clusterid, g.parentid, g.rootid, g.hierarchylevel, count(g.clusterid) as stat ");
			sql.append("       FROM " + gClusterTable + " g ");
			sql.append("       JOIN " + dClusterTable + " c ");
			sql.append("         ON g.id = c.rowid ");
			sql.append("      WHERE g.clusterid IN " + inClause);
//			sql.append("        AND c.contextid = '" + contextId + "'"); 
			sql.append("      GROUP BY g.clusterid, g.parentid, g.rootid, g.hierarchylevel) q ");
			sql.append(" UNION ALL ");
			sql.append("SELECT clusterid, parentid, rootid, hierarchylevel, 'label' as property, value, stat ");
			sql.append("  FROM ");
			sql.append("    (SELECT clusterid, parentid, rootid, hierarchylevel, value, ROW_NUMBER() OVER (PARTITION BY clusterid Order by stat desc ) as seq, stat ");
			sql.append("       FROM "); 
			sql.append("         (SELECT g.clusterid, g.parentid, g.rootid, g.hierarchylevel, l.lenders_name as value, count(l.lenders_name) as stat ");
			sql.append("            FROM " + gClusterTable + " g ");
			sql.append("       		JOIN " + dClusterTable + " c ");
			sql.append("              ON g.id = c.rowid ");
			sql.append("            JOIN entityId_to_lenderId e ");
			sql.append("              ON g.entityid = e.entityid ");
			sql.append("            JOIN lenders l ");
			sql.append("              ON l.lenders_lenderid = e.lenderid ");  
			sql.append("           WHERE g.clusterid IN " + inClause);
//			sql.append("             AND c.contextid = '" + contextId + "'");
			sql.append("           GROUP BY g.clusterid, g.parentid, g.rootid, g.hierarchylevel, l.lenders_name) q1 ) q2 ");
			sql.append("      WHERE seq = 1 ");
			sql.append(" UNION ALL ");
			sql.append("SELECT clusterid, parentid, rootid, hierarchylevel, 'location-dist' as property, value, stat ");
			sql.append("  FROM ");
			sql.append("    (SELECT clusterid, parentid, rootid, hierarchylevel, value, ROW_NUMBER() OVER (PARTITION BY clusterid Order by stat desc ) as seq, stat ");
			sql.append("       FROM "); 
			sql.append("         (SELECT g.clusterid, g.parentid, g.rootid, g.hierarchylevel, l.lenders_countryCode as value, count(l.lenders_countryCode) as stat ");
			sql.append("            FROM " + gClusterTable + " g ");
			sql.append("       		JOIN " + dClusterTable + " c ");
			sql.append("              ON g.id = c.rowid ");
			sql.append("            JOIN entityId_to_lenderId e ");
			sql.append("              ON g.entityid = e.entityid ");
			sql.append("            JOIN lenders l ");
			sql.append("              ON l.lenders_lenderid = e.lenderid ");  
			sql.append("           WHERE g.clusterid IN " + inClause);
//			sql.append("             AND c.contextid = '" + contextId + "'");
			sql.append("           GROUP BY g.clusterid, g.parentid, g.rootid, g.hierarchylevel, l.lenders_countryCode) q1 ) q2 ");
			sql.append("       WHERE seq < 10 ");
		
			if (stmt.execute(sql.toString())) {				
				ResultSet rs = stmt.getResultSet();
				try {	
					results.putAll( createClustersFromResultSet(rs, contextId, sessionId) );
				} finally {
					rs.close();	
				}
				
				//HACK : this is for demo purposes only, makes lender clusters parchmenty - idealy this should be read from the db
				for (String key : results.keySet()) {
					ClusterHelper cluster = results.get(key);
					final double notVeryConfidentDemonstration = 0.4*Math.random();
					
					cluster.setUncertainty(FL_Uncertainty.newBuilder().setConfidence(notVeryConfidentDemonstration).build());
				}
			}
		}
		s_logger.info("Total Time to compute lender cluster summaries: " + ((System.currentTimeMillis()-start)/1000));
		stmt.close();
		conn.close();

		return results;
	}
	
	/*package*/ Map<String, ClusterHelper> selectLoans(Collection<String> loanClusterIds, String contextId, String sessionId) throws SQLException, Exception {
		Map<String, ClusterHelper> results = new HashMap<String,ClusterHelper>();
		
		// retrieve loan cluster summaries
		if (loanClusterIds == null || loanClusterIds.isEmpty()) return results;
		
		Connection conn = _connectionPool.getConnection(); 
		Statement stmt = conn.createStatement();
		
		String gClusterTable = getNamespaceHandler().tableName(null, DataAccessHelper.GLOBAL_CLUSTER_TABLE);
		String dClusterTable = getNamespaceHandler().tempTableName(
				getNamespaceHandler().tableName(null, DataAccessHelper.DYNAMIC_CLUSTER_TABLE) + "_" + sessionId + "_" + contextId);
		
		List<String> idsCopy = new ArrayList<String>(loanClusterIds); // copy the ids as we will take 1000 at a time to process and the take method is destructive
		
		long start = System.currentTimeMillis();
		
		while (idsCopy.size() > 0) {
			List<String> tempSubList = (idsCopy.size() > 1000) ? tempSubList = idsCopy.subList(0, 999) : idsCopy; // get the next 1000
			List<String> subIds = new ArrayList<String>(tempSubList); // copy as the next step is destructive
			tempSubList.clear(); // this clears the IDs from idsCopy as tempSubList is backed by idsCopy 
		
			String inClause = DataAccessHelper.createInClause(subIds);
			
			// cluster summary is # members, most frequent label, freq count of top ten country codes			
			StringBuilder sql = new StringBuilder();
			sql.append("SELECT clusterid, parentid, rootid, hierarchylevel, 'members' as property, 'loan' as value, stat ");
			sql.append("  FROM ");
			sql.append("    (SELECT g.clusterid, ");
			sql.append("            g.parentid, ");
			sql.append("            g.rootid, ");
			sql.append("            g.hierarchylevel, ");
			sql.append("            count(g.clusterid) as stat ");
			sql.append("       FROM " + gClusterTable + " g ");
			sql.append("       JOIN " + dClusterTable + " c ");
			sql.append("         ON g.id = c.rowid ");
			sql.append("      WHERE g.clusterid IN " + inClause);
//			sql.append("        AND c.contextid = '" + contextId + "'"); 
			sql.append("      GROUP BY g.clusterid, g.parentid, g.rootid, g.hierarchylevel) q ");
			sql.append(" UNION ALL ");
			sql.append("SELECT clusterid, parentid, rootid, hierarchylevel, 'label' as property, value, stat ");
			sql.append("  FROM ");
			sql.append("    (SELECT clusterid, parentid, rootid, hierarchylevel, value, ROW_NUMBER() OVER (PARTITION BY clusterid Order by stat desc ) as seq, stat ");
			sql.append("       FROM "); 
			sql.append("         (SELECT g.clusterid, ");
			sql.append("                 g.parentid, ");
			sql.append("                 g.rootid, ");
			sql.append("                 g.hierarchylevel, ");
			sql.append("                 l.loans_name as value, ");
			sql.append("                 count(l.loans_name) as stat ");
			sql.append("            FROM " + gClusterTable + " g ");
			sql.append("            JOIN " + dClusterTable + " c ");
			sql.append("              ON g.id = c.rowid ");
			sql.append("            JOIN entityId_to_loanId e ");
			sql.append("              ON g.entityid = e.entityid ");
			sql.append("            JOIN loanMetaData l ");
			sql.append("              ON l.loans_id = e.loanid ");
			sql.append("           WHERE g.clusterid IN " + inClause);
//			sql.append("             AND c.contextid = '" + contextId + "'");
			sql.append("             AND l.loans_name <> 'Anonymous' ");
			sql.append("             AND l.loans_name <> 'Anonymous Group' ");
			sql.append("           GROUP BY g.clusterid, g.parentid, g.rootid, g.hierarchylevel, l.loans_name) q1 ) q2 ");
			sql.append("      WHERE seq = 1 ");
			sql.append(" UNION ALL ");
			sql.append("SELECT clusterid, parentid, rootid, hierarchylevel, 'location-dist' as property, value, stat ");
			sql.append("  FROM ");
			sql.append("    (SELECT clusterid, parentid, rootid, hierarchylevel, value, ROW_NUMBER() OVER (PARTITION BY clusterid Order by stat desc ) as seq, stat ");
			sql.append("       FROM "); 
			sql.append("         (SELECT g.clusterid, g.parentid, g.rootid, g.hierarchylevel, l.loans_location_countryCode as value, count(l.loans_location_countryCode) as stat ");
			sql.append("            FROM " + gClusterTable + " g ");
			sql.append("            JOIN " + dClusterTable + " c ");
			sql.append("              ON g.id = c.rowid ");
			sql.append("            JOIN entityId_to_loanId e ");
			sql.append("              ON g.entityid = e.entityid ");
			sql.append("            JOIN loanMetaData l ");
			sql.append("              ON l.loans_id = e.loanid ");
			sql.append("           WHERE g.clusterid IN " + inClause);
//			sql.append("             AND c.contextid = '" + contextId + "'");
			sql.append("           GROUP BY g.clusterid, g.parentid, g.rootid, g.hierarchylevel, l.loans_location_countryCode) q1 ) q2 ");
			sql.append("       WHERE seq < 10 ");
			
			// END NEW WAY
		
			if (stmt.execute(sql.toString())) {				
				ResultSet rs = stmt.getResultSet();
				try {
					results.putAll( createClustersFromResultSet(rs, contextId, sessionId) );
				} finally {				
					rs.close();
				}
			}
		}		
		s_logger.info("Total Time to compute loan cluster summaries: " + ((System.currentTimeMillis()-start)/1000));
		stmt.close();
		conn.close();
			
		return results;
	}
	
	/*package*/ Map<String, EntityType> getClusterTypes(List<String> clusterIds, String contextId, String sessionId) throws SQLException, ClassNotFoundException {
		Map<String, EntityType> types = new HashMap<String, EntityType>();
		
		if (clusterIds == null || clusterIds.isEmpty()) return types;
		
		Connection conn = _connectionPool.getConnection(); 
		Statement stmt = conn.createStatement();
		
		try {			
			List<String> idsCopy = new ArrayList<String>(clusterIds); // copy the ids as we will take 1000 at a time to process and the take method is destructive
			while (idsCopy.size() > 0) {
				List<String> tempSubList = (idsCopy.size() > 1000) ? tempSubList = idsCopy.subList(0, 999) : idsCopy; // get the next 1000
				List<String> subIds = new ArrayList<String>(tempSubList); // copy as the next step is destructive
				tempSubList.clear(); // this clears the IDs from idsCopy as tempSubList is backed by idsCopy 
				
				String inClause = DataAccessHelper.createInClause(subIds);
						
				StringBuilder sql = new StringBuilder();
				sql.append(" SELECT clusterid, entitytype ");
				sql.append("   FROM cluster_type ");
				sql.append("  WHERE clusterid IN " + inClause);
		
				s_logger.trace("execute: " + sql);
				if (stmt.execute(sql.toString())) {				
					ResultSet rs = stmt.getResultSet();
				
					while (rs.next()) {
						String clusterid = rs.getString("clusterId");
						String typeStr = rs.getString("entitytype");
					
						if (typeStr.equalsIgnoreCase("b")) {
							types.put(clusterid, EntityType.Borrowers);
						}
						else if (typeStr.equalsIgnoreCase("l")) {
							types.put(clusterid, EntityType.Lender);
						}
						else if (typeStr.equalsIgnoreCase("p")) {
							types.put(clusterid, EntityType.Partner);
						}
					}
				}
			}
		} finally {
			stmt.close();
			conn.close();
		}
		
		return types;
	}
	
	@Override
	public List<FL_Cluster> getClusters(List<String> clusterIds, String contextId, String sessionId)
			throws AvroRemoteException {
		
		try {
			ArrayList<FL_Cluster> results = new ArrayList<FL_Cluster>();		
		
			//Use sets to handle any duplicates that come in - use tree sets to maintain ordering
			Set<String> lenderClusterIds = new TreeSet<String>();
			Set<String> loanClusterIds = new TreeSet<String>();
			Set<String> partnerClusterIds = new TreeSet<String>();
						
			long start = System.currentTimeMillis();
			Map<String, EntityType> types = getClusterTypes(clusterIds, contextId, sessionId);
			s_logger.info("Time to determine cluster types: " + ((System.currentTimeMillis() - start)/1000));
			
			for (String id : types.keySet()) {
				switch (types.get(id)) {
				case Borrowers:
					loanClusterIds.add(id);
					break;
				case Partner:
					partnerClusterIds.add(id);
					break;
				case Lender:
					lenderClusterIds.add(id);
					break;
				default:
					break;
				}
			}	

			results.addAll(selectLenders(lenderClusterIds, contextId, sessionId).values());
			results.addAll(selectPartners(partnerClusterIds, contextId, sessionId).values());
			results.addAll(selectLoans(loanClusterIds, contextId, sessionId).values());
			
			return results;
		
		} catch (Exception e) {
			throw new AvroRemoteException("Exception in getEntities",e);
		}
	}
}
