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
import influent.idl.FL_Geocoding;
import influent.server.dataaccess.DataNamespaceHandler;
import influent.server.dataaccess.EntityClustering;
import influent.server.dataaccess.MSSQLDataNamespaceHandler;
import influent.server.spi.impl.BasicCountryLevelGeocoding;
import influent.server.utilities.BoneCPConnectionPool;
import influent.server.utilities.SQLConnectionPool;

import java.sql.SQLException;
import java.util.List;

import org.apache.avro.AvroRemoteException;
import org.json.JSONException;

public class TestGetContext {

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
			
			SQLConnectionPool connection = new BoneCPConnectionPool(serverName, portNumber, databaseName, username, password, null);
			
			DataNamespaceHandler namespaceHandler = new MSSQLDataNamespaceHandler();
			EntityClustering clusterer = new EntityClustering(connection, namespaceHandler);
			
			FL_Geocoding geocoding = new BasicCountryLevelGeocoding();
			KivaClusteringDataAccess clusteringda = new KivaClusteringDataAccess(connection, clusterer, geocoding, namespaceHandler);

			
//			String contextId = "497c3218-fd9a-4807-b928-b6b9a0f7000";
			
			String sessionId = "test";
			
			String contextId = "5BB58995-B650-97E7-AAA8-DC21E416CA7F";  // 200 clusters
		
//			String contextId = "a16977c1-c004-4ffa-b61a-8e9d8b9fcdce"; // 462 clusters
			
//			String contextId = "B5720CD6-CE99-8A4F-630C-BFC770E2CAD2"; // 5912 clusters
			
//			String contextId = "B7F44A22-E457-BFA2-74E2-BC838A228810"; // 11877 clusters
			
//			String contextId = "ADE559CE-3480-A885-CF38-1BCE2AC7F82E"; // 47580 clusters
			
			long start = System.currentTimeMillis();
			
			List<FL_Cluster> clusters = clusteringda.getContext(contextId, sessionId, true);
			
			System.out.println("Time: " + ((System.currentTimeMillis() - start)/1000));
			
			start = System.currentTimeMillis();
			
			clusters = clusteringda.getContext(contextId, sessionId, false);
			
			System.out.println("Time: " + ((System.currentTimeMillis() - start)/1000));
			
			
			System.out.println("Number of clusters fetched: " + clusters.size());
//			
//			for (FL_Cluster cluster : clusters) {
//				System.out.println(cluster);
//			}
			
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
