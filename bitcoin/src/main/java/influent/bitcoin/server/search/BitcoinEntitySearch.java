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
package influent.bitcoin.server.search;

import influent.idl.FL_Constraint;
import influent.idl.FL_Entity;
import influent.idl.FL_EntitySearch;
import influent.idl.FL_EntityTag;
import influent.idl.FL_Property;
import influent.idl.FL_PropertyDescriptor;
import influent.idl.FL_PropertyMatchDescriptor;
import influent.idl.FL_PropertyTag;
import influent.idl.FL_PropertyType;
import influent.idl.FL_RangeType;
import influent.idl.FL_SearchResult;
import influent.idl.FL_SearchResults;
import influent.idlhelper.EntityHelper;
import influent.idlhelper.PropertyHelper;
import influent.idlhelper.PropertyMatchDescriptorHelper;
import influent.server.dataaccess.DataAccessHelper;
import influent.server.dataaccess.DataNamespaceHandler;
import influent.server.utilities.SQLConnectionPool;
import influent.server.utilities.TypedId;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.avro.AvroRemoteException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

public class BitcoinEntitySearch implements FL_EntitySearch {

	private static final Logger s_logger = LoggerFactory.getLogger(BitcoinEntitySearch.class);
	
	private SQLConnectionPool _connectionPool;
	
	private final DataNamespaceHandler _namespaceHandler;


	@Inject
	public BitcoinEntitySearch(
		SQLConnectionPool connectionPool,
		DataNamespaceHandler namespaceHandler
	) {
		_connectionPool = connectionPool;
		_namespaceHandler = namespaceHandler;
	}
	
	
	
	
	@Override
	public FL_SearchResults search(
		String searchTerms, 
		List<FL_PropertyMatchDescriptor> terms, 
		long start, 
		long max, 
		String type
	) throws AvroRemoteException {

		String schema = type;
		
		FL_SearchResults results = FL_SearchResults.newBuilder()
				.setTotal(0)
				.setResults(new ArrayList<FL_SearchResult>())
				.build();
		
		ArrayList<FL_SearchResult> matches = new ArrayList<FL_SearchResult>();
		
		try {
			Connection connection = _connectionPool.getConnection();
		
			if (searchTerms != null && !searchTerms.trim().isEmpty()) {

				if (schema == null) {
					schema = "dbo";
				}
				
				PreparedStatement stmt = connection.prepareStatement(
					"SELECT d.id, CountOfTransactions, AvgTrasactionAmount, MaxTransactionAmount, SumTransactionAmount, MinTransDate, MaxTransDate, label, degree " + 
					"FROM Bitcoin." + schema + ".details d " +
					"WHERE [id] = ? " +
					"OR [label] like ?"
				);
				stmt.setString(1, searchTerms.trim());
				stmt.setString(2, "%" + searchTerms.trim() + "%");
				
				matches.addAll(searchEntities(connection, stmt, max, schema));
				
				stmt.close();
			}
			else {
				List<String> ids = new ArrayList<String>();
				
				// advanced search
				for (FL_PropertyMatchDescriptor pmd : terms) {
					PropertyMatchDescriptorHelper term = PropertyMatchDescriptorHelper.from(pmd); 
					String key = term.getKey();
					if (!key.equals("uid")) {
						// we only support "search" but id right now
						s_logger.error("Invalid Property term in search: " + key);
					} else {
						ids.add((String)term.getValue());
					}
				}
	
				if (!ids.isEmpty()) {
					Map<String, List<String>> bySchema = (schema == null) ? 
						_namespaceHandler.entitiesByNamespace(ids) : Collections.singletonMap(schema, ids);
					
					for (Entry<String, List<String>> entry : bySchema.entrySet()) {
						
						if (entry.getValue() == null || entry.getValue().isEmpty()) {
							continue;
						}
						
						StringBuilder sb = new StringBuilder();
						sb.append("SELECT d.id, CountOfTransactions, AvgTrasactionAmount, MaxTransactionAmount, SumTransactionAmount, MinTransDate, MaxTransDate, label, degree ");
						sb.append("FROM Bitcoin."+ entry.getKey() + ".details d ");
						sb.append("WHERE [id] IN (");
						for (int i = 1; i < entry.getValue().size(); i++) {
							sb.append("?, ");
						}
						sb.append("?) ");
						
						PreparedStatement stmt = connection.prepareStatement(sb.toString());
						
						int index = 1;
						for (String id : entry.getValue()) {
							stmt.setString(index++, _namespaceHandler.toSQLId(id, entry.getKey()));
						}

						matches.addAll(searchEntities(connection, stmt, max, entry.getKey()));
						
						stmt.close();
					}
				}
			}
			
			results.setResults(matches);
			results.setTotal((long) matches.size());
			
			connection.close();
			
		} catch (ClassNotFoundException e) {
			throw new AvroRemoteException(e);
		} catch (SQLException e) {
			throw new AvroRemoteException(e);
		}
		
		return results;
	}
	
	
	
	
	private List<FL_SearchResult> searchEntities(
		Connection connection,
		PreparedStatement statement, 
		long max,
		String schema
	) throws SQLException, ClassNotFoundException {
		
		if (max > 0) statement.setMaxRows((int) max);
		if (max > 0) statement.setFetchSize((int) max);
		
		List<FL_SearchResult> matches = new ArrayList<FL_SearchResult>();
		List<String> rawIds = new ArrayList<String>();

		ResultSet resultSet = statement.executeQuery();
		while (resultSet.next() && matches.size() < max) {
			String id = resultSet.getString(1);
			rawIds.add(id);
			long CountOfTransactions = resultSet.getLong(2);
			long AvgTrasactionAmount = resultSet.getLong(3);
			long MaxTransactionAmount = resultSet.getLong(4);
			long SumTransactionAmount = resultSet.getLong(5);
			Date MinTransDate = resultSet.getDate(6);
			Date MaxTransDate = resultSet.getDate(7);
			String label = resultSet.getString(8);
			long degree = resultSet.getLong(9);
			
			List<FL_Property> props = new ArrayList<FL_Property>();
			props.add(new PropertyHelper("CountOfTransactions", "transaction count", CountOfTransactions, Collections.singletonList(FL_PropertyTag.STAT)));
			props.add(new PropertyHelper("AvgTrasactionAmount", "avg transaction amount", AvgTrasactionAmount, Collections.singletonList(FL_PropertyTag.STAT)));
			props.add(new PropertyHelper("MaxTransactionAmount", "max transaction amount", MaxTransactionAmount, Collections.singletonList(FL_PropertyTag.STAT)));
			props.add(new PropertyHelper("SumTransactionAmount", "total transaction amount", SumTransactionAmount, Collections.singletonList(FL_PropertyTag.STAT)));
			props.add(new PropertyHelper("MinTransDate", "first transaction date", MinTransDate, Collections.singletonList(FL_PropertyTag.STAT)));
			props.add(new PropertyHelper("MaxTransDate", "last transaction date", MaxTransDate, Collections.singletonList(FL_PropertyTag.STAT)));
			props.add(new PropertyHelper("Degree", "degree", degree, Collections.singletonList(FL_PropertyTag.STAT)));
			
			if (label != null && !label.trim().isEmpty()) {
				props.add(new PropertyHelper("UserTag", "userTag", label, Collections.singletonList(FL_PropertyTag.TEXT)));
			} else {
				label = id;
			}

			id = _namespaceHandler.globalFromLocalEntityId(schema, id, TypedId.ACCOUNT);
			
			FL_Entity entity = new EntityHelper(id, label,
					FL_EntityTag.ACCOUNT.name(), FL_EntityTag.ACCOUNT, props);
			matches.add(new FL_SearchResult(1.0, entity));
		}
		resultSet.close();
		
		Map<String, int[]> entityStats = new HashMap<String, int[]>();
		
		if (!rawIds.isEmpty()) {
			
			// separately grab the FinEntity stats
			String finEntityTable = _namespaceHandler.tableName(null, DataAccessHelper.ENTITY_TABLE);
			String finEntityEntityIdColumn = _namespaceHandler.columnName(DataAccessHelper.ENTITY_COLUMN_ENTITY_ID);
			String finEntityUniqueInboundDegree = _namespaceHandler.columnName(DataAccessHelper.ENTITY_COLUMN_UNIQUE_INBOUND_DEGREE);
			String finEntityUniqueOutboundDegree = _namespaceHandler.columnName(DataAccessHelper.ENTITY_COLUMN_UNIQUE_OUTBOUND_DEGREE);
			
			StringBuilder sb = new StringBuilder();
			sb.append("SELECT " + finEntityEntityIdColumn + ", " + finEntityUniqueInboundDegree + ", " + finEntityUniqueOutboundDegree + " ");
			sb.append("FROM " + finEntityTable + " ");
			sb.append("WHERE " + finEntityEntityIdColumn + " IN (");
			for (int i = 1; i < rawIds.size(); i++) {
				sb.append("?, ");
			}
			sb.append("?) ");
			
			PreparedStatement stmt = connection.prepareStatement(sb.toString());
			
			int index = 1;
			for (String id : rawIds) {
				stmt.setString(index++, _namespaceHandler.toSQLId(id, schema));
			}
			
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				String entityId = rs.getString(finEntityEntityIdColumn);
				int inDegree = rs.getInt(finEntityUniqueInboundDegree);
				int outDegree = rs.getInt(finEntityUniqueOutboundDegree);
			
				entityStats.put(entityId, new int[]{inDegree, outDegree});
			}
			rs.close();
			
			stmt.close();
		}
		
		for (FL_SearchResult result : matches) {
			FL_Entity fle = (FL_Entity)result.getResult();
			int[] stats = entityStats.get( TypedId.fromTypedId( fle.getUid() ).getNativeId() );
		
			if (stats != null) {
				// add degree stats
				fle.getProperties().add( new PropertyHelper("inboundDegree", stats[0], FL_PropertyTag.INFLOWING) );
				fle.getProperties().add( new PropertyHelper("outboundDegree", stats[1], FL_PropertyTag.OUTFLOWING) );
			}
		}
		
		return matches;
	}
	
	
	
	
	/**
	 * Returns searchable properties for the advanced search.
	 */
	@Override
	public Map<String, List<FL_PropertyDescriptor>> getDescriptors() throws AvroRemoteException {
		LinkedHashMap<String, List<FL_PropertyDescriptor>> map = new LinkedHashMap<String, List<FL_PropertyDescriptor>>();
		
		// a list of searchable properties per namespace (db schema in this case).
		map.put("dbo", Arrays.asList(new FL_PropertyDescriptor[] {
				FL_PropertyDescriptor.newBuilder()
				.setKey("id")
				.setFriendlyText("id")
				.setType(FL_PropertyType.STRING)
				.setConstraint(FL_Constraint.REQUIRED_EQUALS)
				.setRange(FL_RangeType.SINGLETON)
				.build()
		}));
		
		return map;
	}
}
