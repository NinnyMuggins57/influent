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

import influent.idl.FL_Clustering;
import influent.idl.FL_Entity;
import influent.server.utilities.SQLConnectionPool;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.avro.AvroRemoteException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EntityClustering implements FL_Clustering {
	private static Logger s_logger = LoggerFactory.getLogger(EntityClustering.class);
	
	protected final SQLConnectionPool _connectionPool;
	private final DataNamespaceHandler _namespaceHandler;
	
	public EntityClustering(SQLConnectionPool connectionPool, DataNamespaceHandler namespaceHandler) 
			throws ClassNotFoundException, SQLException {
		_connectionPool = connectionPool;
		_namespaceHandler = namespaceHandler;
	}
	
	@Override
	public List<String> clusterEntities(List<FL_Entity> entities, String contextId, String sessionId)
			throws AvroRemoteException {
		List<String> ids = new ArrayList<String>(entities.size());
		for (FL_Entity entity : entities) {
			ids.add(entity.getUid());
		}
		return clusterEntitiesById(ids, contextId, sessionId);
	}

	protected DataNamespaceHandler getNamespaceHandler() {
		return _namespaceHandler;
	}
	
	@Override
	public List<String> clusterEntitiesById(List<String> entityIds, String contextId, String sessionId)
			throws AvroRemoteException {
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
			
			Set<String> clusterIds = new HashSet<String>();
			List<String> idsCopy = new ArrayList<String>(entityIds); // copy the ids as we will take 1000 at a time to process and the take method is destructive
			
			long start = System.currentTimeMillis();
						
			connection.setAutoCommit(false);
			
			StringBuilder sql = new StringBuilder();
			sql = new StringBuilder();
			
			while (idsCopy.size() > 0) {
				List<String> tempSubList = (idsCopy.size() > 1000) ? tempSubList = idsCopy.subList(0, 999) : idsCopy; // get the next 1000
				List<String> subIds = new ArrayList<String>(tempSubList); // copy as the next step is destructive
				tempSubList.clear(); // this clears the IDs from idsCopy as tempSubList is backed by idsCopy 
				
				String ids = DataAccessHelper.createInClause(subIds);
				
				sql = new StringBuilder();
				sql.append("INSERT INTO " + dClusterTable + " ( rowid ) ");
				sql.append("SELECT g.id ");
				sql.append("  FROM " + gClusterTable + " g ");			
				sql.append("  LEFT JOIN " + dClusterTable + " c ");
				sql.append("    ON g.id = c.rowid ");
				sql.append(" WHERE c.rowid IS NULL ");
				sql.append("   AND g.entityid IN " + ids);
				
				stmt.addBatch(sql.toString());
			}
			stmt.executeBatch();
		    connection.commit();
		    connection.setAutoCommit(true);
		    stmt.clearBatch();
		    
		    s_logger.info("Cluster time: " + ((System.currentTimeMillis() - start)/1000.0) );
		    
		    start = System.currentTimeMillis();

		    idsCopy = new ArrayList<String>(entityIds); // copy the ids as we will take 1000 at a time to process and the take method is destructive

		    // keep track of which entities are anonymous - initially assume they all are
		    Set<String> anonymousEntities = new HashSet<String>(entityIds);
		    
		    while (idsCopy.size() > 0) {
				List<String> tempSubList = (idsCopy.size() > 1000) ? tempSubList = idsCopy.subList(0, 999) : idsCopy; // get the next 1000
				List<String> subIds = new ArrayList<String>(tempSubList); // copy as the next step is destructive
				tempSubList.clear(); // this clears the IDs from idsCopy as tempSubList is backed by idsCopy 
				
				String ids = DataAccessHelper.createInClause(subIds);

				// Return back the resulting clusters ids
				sql = new StringBuilder();
				sql.append("SELECT g.clusterid, g.entityid ");
				sql.append("  FROM " + gClusterTable + " g ");
				sql.append(" WHERE g.entityid IN " + ids);
				sql.append("   AND g.hierarchylevel = 0"); 
				ResultSet rs = stmt.executeQuery(sql.toString());
				
				while (rs.next()) {
					clusterIds.add(rs.getString("clusterid"));
					anonymousEntities.remove(rs.getString("entityid"));  // member of cluster so not anonymous
				}
		    }
		    
		    // add the anonymous entites to the result
		    clusterIds.addAll(anonymousEntities);
						
			s_logger.info("Fetch root clusters time: " + ((System.currentTimeMillis() - start)/1000.0) );
		
			connection.close();
			stmt.close();
			
			return new LinkedList<String>(clusterIds);
			
		} catch (Exception e) {
			throw new AvroRemoteException("Exception in clusterEntitiesById",e);
		}
	}

	@Override
	public String createContext() throws AvroRemoteException {
		// A context represents a distinct group of dynamic clusters
		// For the moment a context is only a unique identifier associated with dynamic clusters
		return UUID.randomUUID().toString();
	}
}
