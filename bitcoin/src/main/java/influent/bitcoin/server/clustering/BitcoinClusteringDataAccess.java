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
package influent.bitcoin.server.clustering;

import influent.idl.FL_Cluster;
import influent.idl.FL_Clustering;
import influent.idl.FL_Geocoding;
import influent.server.dataaccess.ClusteringDataAccess;
import influent.server.dataaccess.DataAccessHelper;
import influent.server.dataaccess.DataNamespaceHandler;
import influent.server.utilities.SQLConnectionPool;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.avro.AvroRemoteException;


/**
 */

public class BitcoinClusteringDataAccess extends ClusteringDataAccess {

	public BitcoinClusteringDataAccess(SQLConnectionPool connectionPool,
			FL_Clustering clustering, FL_Geocoding geocoding, DataNamespaceHandler namespaceHandler)
			throws ClassNotFoundException, SQLException {
		super(connectionPool, clustering, geocoding, namespaceHandler);
	}

	@Override
	public List<FL_Cluster> getClusters(List<String> entities, String contextId, String sessionId)
			throws AvroRemoteException {
		try {
			ArrayList<FL_Cluster> results = new ArrayList<FL_Cluster>();	
			
			// retrieve partner cluster summaries
			if (entities == null || entities.isEmpty()) return results;
			
			Connection conn = _connectionPool.getConnection(); 
			Statement stmt = conn.createStatement();
		
			String gClusterTable = getNamespaceHandler().tableName(null, DataAccessHelper.GLOBAL_CLUSTER_TABLE);
			String dClusterTable = getNamespaceHandler().tempTableName(
					getNamespaceHandler().tableName(null, DataAccessHelper.DYNAMIC_CLUSTER_TABLE) + "_" + sessionId + "_" + contextId);
		
			List<String> idsCopy = new ArrayList<String>(entities); // copy the ids as we will take 1000 at a time to process and the take method is destructive
			
			long start = System.currentTimeMillis();
			
			while (idsCopy.size() > 0) {
				List<String> tempSubList = (idsCopy.size() > 1000) ? tempSubList = idsCopy.subList(0, 999) : idsCopy; // get the next 1000
				List<String> subIds = new ArrayList<String>(tempSubList); // copy as the next step is destructive
				tempSubList.clear(); // this clears the IDs from idsCopy as tempSubList is backed by idsCopy 
				
				String inClause = DataAccessHelper.createInClause(subIds);
		
				// NOTE this joins on one entity details table only -if we were using multiple schemas or tables this gets more complicated
				StringBuilder sql = new StringBuilder();
				sql.append(" select clusterid, parentid, rootid, hierarchylevel, 'members' as property, 'account' as value, stat ");
				sql.append("  from ");
				sql.append("    (select g.clusterid, g.parentid, g.rootid, g.hierarchylevel, count(g.clusterid) as stat ");
				sql.append("       from " + gClusterTable + " g ");
				sql.append("       join " + dClusterTable + " c ");
				sql.append("         on g.id = c.rowid ");
				sql.append("      where g.clusterid IN " + inClause);
//				sql.append("        and c.contextid = '" + contextId + "'"); 
				sql.append("      group by g.clusterid, g.parentid, g.rootid, g.hierarchylevel) q ");
				sql.append(" union all ");
				sql.append(" select clusterid, parentid, rootid, hierarchylevel, 'label' as property, value, stat ");
				sql.append("  from ");
				sql.append("    (select clusterid, parentid, rootid, hierarchylevel, value, ROW_NUMBER() OVER (PARTITION BY clusterid Order by stat desc ) as seq, stat ");
				sql.append("       from "); 
				sql.append("         (select g.clusterid, g.parentid, g.rootid, g.hierarchylevel, l.label as value, count(l.label) as stat ");
				sql.append("       		from " + gClusterTable + " g ");
				sql.append("       		join " + dClusterTable + " c ");
				sql.append("         	  on g.id = c.rowid ");
				sql.append("            join details l ");
				sql.append("              on l.id = g.entityid ");  
				sql.append("           where g.clusterid IN " + inClause);
//				sql.append("             and c.contextid = '" + contextId + "'");
				sql.append("             and l.label <> '' ");
				sql.append("           group by g.clusterid, g.parentid, g.rootid, g.hierarchylevel, l.label");
				sql.append("          union all ");
				sql.append("          select g.clusterid, g.parentid, g.rootid, g.hierarchylevel, l.id as value, 0 as stat ");
				sql.append("       		from " + gClusterTable + " g ");
				sql.append("       		join " + dClusterTable + " c ");
				sql.append("         	  on g.id = c.rowid ");
				sql.append("            join details l ");
				sql.append("              on l.id = g.entityid ");  
				sql.append("           where g.clusterid IN " + inClause + " ) q1 ) q2 ");
				sql.append("      where seq = 1 ");
	
				if (stmt.execute(sql.toString())) {				
					ResultSet rs = stmt.getResultSet();
					try {	
						results.addAll( createClustersFromResultSet(rs, contextId, sessionId).values() );
					} finally {
						rs.close();	
					}
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
}
