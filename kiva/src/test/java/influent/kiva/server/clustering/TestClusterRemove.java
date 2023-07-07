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
import influent.idl.FL_Entity;
import influent.idl.FL_Geocoding;
import influent.idl.FL_SearchResult;
import influent.idl.FL_SearchResults;
import influent.kiva.server.search.KivaEntitySearch;
import influent.server.dataaccess.DataNamespaceHandler;
import influent.server.dataaccess.EntityClustering;
import influent.server.dataaccess.MSSQLDataNamespaceHandler;
import influent.server.spi.impl.BasicCountryLevelGeocoding;
import influent.server.utilities.BoneCPConnectionPool;
import influent.server.utilities.SQLConnectionPool;

import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.avro.AvroRemoteException;
import org.json.JSONException;

public class TestClusterRemove {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			String serverName = "azure";
			int portNumber = 1433;
			String databaseName = "Kiva";
			String username = "kivaadmin";
			String password = "kiva";
			String solrUrl = "http://192.168.0.55:8983/solr/solr";
			String solrDescriptor = "kiva-fields.txt";
			
			SQLConnectionPool connection = new BoneCPConnectionPool(serverName, portNumber, databaseName, username, password, null);
			
			DataNamespaceHandler namespaceHandler = new MSSQLDataNamespaceHandler();
			EntityClustering clusterer = new EntityClustering(connection, namespaceHandler);
			
			FL_Geocoding geocoding = new BasicCountryLevelGeocoding();
			KivaClusteringDataAccess clusteringda = new KivaClusteringDataAccess(connection, clusterer, geocoding, namespaceHandler);
			
			KivaEntitySearch search = new KivaEntitySearch(solrUrl, solrDescriptor, geocoding, connection, namespaceHandler);
			FL_SearchResults results = search.search("daniel", null, 0, 100, null);
			
			List<String> entities = new LinkedList<String>();
			
			for (FL_SearchResult result : results.getResults()) {
				FL_Entity entity = (FL_Entity)result.getResult();
				entities.add(entity.getUid().substring(1));
			}
			
			String contextId = "f14fb928-ab9a-472c-8e80-42997bb86e9d"; //clusterer.createContext();
			String sessionId = "test";
			
			entities.clear();
			entities.add("b426598");  //b426598   b427120
			entities.add("b427120");

			List<String> clusterIds = clusterer.clusterEntitiesById(entities, contextId, sessionId);
			
			long num = clusteringda.removeMembers(Collections.singletonList("b427120"), contextId, sessionId);
			
			System.out.println("Removed " + num);
			
			List<FL_Cluster> clusters = clusteringda.getClusters(clusterIds, contextId, sessionId);
			
			for (FL_Cluster cluster : clusters) {
				System.out.println(cluster);
			}
			
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (AvroRemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
