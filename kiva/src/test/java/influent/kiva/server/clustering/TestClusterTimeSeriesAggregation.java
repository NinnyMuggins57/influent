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

import influent.idl.FL_DateInterval;
import influent.idl.FL_DateRange;
import influent.idl.FL_Duration;
import influent.idl.FL_Entity;
import influent.idl.FL_Geocoding;
import influent.idl.FL_Link;
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
import java.util.Map;

import org.apache.avro.AvroRemoteException;
import org.json.JSONException;

public class TestClusterTimeSeriesAggregation {

	
	public static void testClusterTimeSeriesAggregation(KivaClusteringDataAccess clusteringda, EntityClustering clusterer) {
		try {
			Map<String, List<FL_Link>> timeseries = clusteringda.getTimeSeriesAggregation(
					Collections.singletonList("65EC3F27-60CD-4960-84AB-7D18B9D31E83"), 
					null, 
					null, 
					new FL_DateRange(0L, 100L, new FL_Duration(FL_DateInterval.YEARS, 10L)), 
					"497c3218-fd9a-4807-b928-b6b9a0f7000", 
					clusterer.createContext(),
					"test");
			System.out.println(timeseries);
		} catch (AvroRemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void testEntityTimeSeriesAggregation(KivaClusteringDataAccess clusteringda, EntityClustering clusterer) {
		try {
			Map<String, List<FL_Link>> timeseries1 = clusteringda.getTimeSeriesAggregation(
					Collections.singletonList("b426598"), 
					null, 
					null, 
					new FL_DateRange(0L, 100L, new FL_Duration(FL_DateInterval.YEARS, 10L)), 
					"497c3218-fd9a-4807-b928-b6b9a0f7000", 
					clusterer.createContext(),
					"test");
			System.out.println(timeseries1);
			Map<String, List<FL_Link>> timeseries2 = clusteringda.getTimeSeriesAggregation(
					Collections.singletonList("lanonnl"), 
					null, 
					null, 
					new FL_DateRange(0L, 100L, new FL_Duration(FL_DateInterval.YEARS, 10L)), 
					"497c3218-fd9a-4807-b928-b6b9a0f7000", 
					clusterer.createContext(),
					"test");
			System.out.println(timeseries2);
			Map<String, List<FL_Link>> timeseries3 = clusteringda.getTimeSeriesAggregation(
					Collections.singletonList("p239-426598"), 
					null, 
					null, 
					new FL_DateRange(0L, 100L, new FL_Duration(FL_DateInterval.YEARS, 10L)), 
					"497c3218-fd9a-4807-b928-b6b9a0f7000", 
					clusterer.createContext(),
					"test");
			System.out.println(timeseries3);
		} catch (AvroRemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
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
			
//			String contextId = "497c3218-fd9a-4807-b928-b6b9a0f7000";
			
			entities.clear();
			entities.add("b426598");  
			entities.add("b427120");

//			List<String> clusterIds = clusterer.clusterEntitiesById(entities, contextId);
			
			testEntityTimeSeriesAggregation(clusteringda, clusterer);
			
//			testClusterFlow(clusteringda, clusterer);
			
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
