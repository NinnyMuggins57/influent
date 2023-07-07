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

import influent.idl.FL_Constraint;
import influent.idl.FL_DataAccess;
import influent.idl.FL_DateRange;
import influent.idl.FL_DirectionFilter;
import influent.idl.FL_Entity;
import influent.idl.FL_EntitySearch;
import influent.idl.FL_LevelOfDetail;
import influent.idl.FL_Link;
import influent.idl.FL_LinkEntityTypeFilter;
import influent.idl.FL_LinkTag;
import influent.idl.FL_Property;
import influent.idl.FL_PropertyMatchDescriptor;
import influent.idl.FL_PropertyTag;
import influent.idl.FL_PropertyType;
import influent.idl.FL_SearchResult;
import influent.idl.FL_SearchResults;
import influent.idlhelper.PropertyHelper;
import influent.idlhelper.SingletonRangeHelper;
import influent.server.utilities.SQLConnectionPool;
import influent.server.utilities.TypedId;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.avro.AvroRemoteException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public abstract class DataViewDataAccess implements FL_DataAccess {

	private static Logger s_logger = LoggerFactory.getLogger(DataViewDataAccess.class);
	
	protected final SQLConnectionPool _connectionPool;
	protected final FL_EntitySearch _search;
	private final DataNamespaceHandler _namespaceHandler;
	
	public DataViewDataAccess(SQLConnectionPool connectionPool,
			FL_EntitySearch search,
			DataNamespaceHandler namespaceHandler)
			throws ClassNotFoundException, SQLException {
		
		// TODO: ehCacheConfig!!
		_connectionPool = connectionPool;
		_search = search;
		_namespaceHandler = namespaceHandler;
	}
	
	protected DataNamespaceHandler getNamespaceHandler() {
		return _namespaceHandler;
	}
	
	@Override
	public List<FL_Entity> getEntities(List<String> entities, FL_LevelOfDetail levelOfDetail) throws AvroRemoteException {
		//Construct the id search query for the search
		
		List<FL_Entity> results = new ArrayList<FL_Entity>();
		
		List<FL_PropertyMatchDescriptor> idList = new ArrayList<FL_PropertyMatchDescriptor>();
		
		int maxFetch = 100;
		int qCount = 0;			//How many entities to query at once
		Iterator<String> idIter = entities.iterator();
		while (idIter.hasNext()) {
			String entity = idIter.next();
			
			FL_PropertyMatchDescriptor idMatch = FL_PropertyMatchDescriptor.newBuilder()
				.setKey("uid")
				.setRange(new SingletonRangeHelper(entity, FL_PropertyType.STRING))
				.setConstraint(FL_Constraint.REQUIRED_EQUALS)
				.build();
			idList.add(idMatch);
			qCount++;
			
			if (qCount == (maxFetch-1) || !idIter.hasNext()) {
				FL_SearchResults searchResult = _search.search(null, idList, 0, 100, null);
				
				s_logger.info("Searched for "+qCount+" ids, found "+searchResult.getTotal());
				
				for (FL_SearchResult r : searchResult.getResults()) {
					FL_Entity fle = (FL_Entity)r.getResult();
					results.add(fle);
				}
				
				qCount=0;
				idList.clear();
			}
		}
		
		return results;
	}

	@Override
	public Map<String, List<FL_Link>> getFlowAggregation(
			List<String> entities,
			List<String> focusEntities,
			FL_DirectionFilter direction,
			FL_LinkEntityTypeFilter entityType,
			FL_LinkTag tag,
			FL_DateRange date) throws AvroRemoteException {
		
		Map<String, List<FL_Link>> results = new HashMap<String, List<FL_Link>>();
		
		final Map<String, List<String>> ns_entities = getNamespaceHandler().entitiesByNamespace(entities);
		final Map<String, List<String>> ns_focusEntities = getNamespaceHandler().entitiesByNamespace(focusEntities);

		try {
			Connection connection = _connectionPool.getConnection();
			
			for (Entry<String, List<String>>  entitiesByNamespace : ns_entities.entrySet()) {
				final String namespace = entitiesByNamespace.getKey();
				final List<String> localFocusEntities = ns_focusEntities.get(namespace);
				
				// will be no matches - these are from different schemas.
				if (!ns_focusEntities.isEmpty() && localFocusEntities == null) {
					continue;
				}
				
				Statement stmt = connection.createStatement();
			
				DateTime startDate = DataAccessHelper.getStartDate(date);
				DateTime endDate = DataAccessHelper.getEndDate(date);
				
				String focusIds = DataAccessHelper.createNodeIdListFromCollection(localFocusEntities, true, false);
				
				String finFlowTable = getNamespaceHandler().tableName(namespace, DataAccessHelper.FLOW_TABLE);
				String finFlowIntervalTable = getNamespaceHandler().tableName(namespace, DataAccessHelper.standardTableName(DataAccessHelper.FLOW_TABLE, date.getDurationPerBin().getInterval()));
				String finFlowDateColumn = getNamespaceHandler().columnName("PeriodDate");		// TODO replace me when all the old db tables have gone away
				
				List<String> idsCopy = new ArrayList<String>(entitiesByNamespace.getValue()); // copy the ids as we will take 100 at a time to process and the take method is destructive
				while (idsCopy.size() > 0) {
					List<String> tempSubList = (idsCopy.size() > 100) ? tempSubList = idsCopy.subList(0, 99) : idsCopy; // get the next 100
					List<String> subIds = new ArrayList<String>(tempSubList); // copy as the next step is destructive
					tempSubList.clear(); // this clears the IDs from idsCopy as tempSubList is backed by idsCopy 
		
					String ids = DataAccessHelper.createNodeIdListFromCollection(subIds, true, false);
					String sourceDirectionClause = "FromEntityId in ("+ids+")";
					String destDirectionClause = "ToEntityId in ("+ids+")";
					
					if (focusIds != null) {
						sourceDirectionClause += " and ToEntityId in ("+focusIds+")";
						destDirectionClause += " and FromEntityId in ("+focusIds+")";				
					}
					String directionClause = (direction == FL_DirectionFilter.BOTH ) ? sourceDirectionClause+" and "+destDirectionClause : (direction == FL_DirectionFilter.DESTINATION ) ? destDirectionClause : (direction == FL_DirectionFilter.SOURCE ) ? sourceDirectionClause : "1=1";
					String entityTypeClause = DataAccessHelper.linkEntityTypeClause(direction, entityType);
					
					String dateRespectingFlowSQL = "select FromEntityId, ToEntityId, sum(Amount) as Amount from "+finFlowIntervalTable+" where " + finFlowDateColumn + " between '"+DataAccessHelper.format(startDate)+"' and '"+DataAccessHelper.format(endDate)+"' and "+directionClause+" and "+entityTypeClause+" group by FromEntityId, ToEntityId";
					String flowSQL = "select FromEntityId, FromEntityType, ToEntityId, ToEntityType from " + finFlowTable + " where "+directionClause+" and "+entityTypeClause;
						
					Map<String, Map<String, Double>> fromToAmountMap = new HashMap<String, Map<String, Double>>();
					
					s_logger.trace(dateRespectingFlowSQL);
					if (stmt.execute(dateRespectingFlowSQL)) {
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
					s_logger.trace(flowSQL);
					if (stmt.execute(flowSQL)) {
						ResultSet rs = stmt.getResultSet();
						while (rs.next()) {
							String from = rs.getString("FromEntityId");
							String fromType = rs.getString("FromEntityType").toLowerCase();
							String to = rs.getString("ToEntityId");
							String toType = rs.getString("ToEntityType").toLowerCase();
	
							String keyId = from;
							char type = fromType.charAt(0); // from type must be a single char
							if (subIds.contains(to)) {
								keyId = to;
								type = toType.charAt(0); // to type must be a single char
							}
							
							// globalize this for return
							keyId = getNamespaceHandler().globalFromLocalEntityId(namespace, keyId, type);
							
							List<FL_Link> linkList = results.get(keyId);
							if (linkList == null) {
								linkList = new LinkedList<FL_Link>();
								results.put(keyId, linkList);
							}
		
							// only use the amount calculated above with the date respecting FLOW map
							Double amount = (fromToAmountMap.containsKey(from) && fromToAmountMap.get(from).containsKey(to)) ? amount = fromToAmountMap.get(from).get(to) : 0.0;
							List<FL_Property> properties = new ArrayList<FL_Property>();
							properties.add(new PropertyHelper(FL_PropertyTag.AMOUNT, amount));
		
							// globalize these for return
							from = getNamespaceHandler().globalFromLocalEntityId(namespace, from, fromType.charAt(0));
							to = getNamespaceHandler().globalFromLocalEntityId(namespace, to, toType.charAt(0));
							
							//Finally, create the link between the two, and add it to the map.
							FL_Link link = new FL_Link(Collections.singletonList(FL_LinkTag.FINANCIAL), from, to, true, null, null, properties);
							linkList.add(link);
						}
						rs.close();
					}
				}
				
				stmt.close();
			}
			
			connection.close();
		} catch (ClassNotFoundException e) {
			throw new AvroRemoteException(e);
		} catch (SQLException e) {
			throw new AvroRemoteException(e);
		}
		
		return results;
	}

	@Override
	public Map<String, List<FL_Link>> getTimeSeriesAggregation(
			List<String> entities,
			List<String> focusEntities,
			FL_LinkTag tag,
			FL_DateRange date) throws AvroRemoteException {
		Map<String, List<FL_Link>> results = new HashMap<String, List<FL_Link>>();
		
		DateTime startDate = DataAccessHelper.getStartDate(date);
		DateTime endDate = DataAccessHelper.getEndDate(date);
			
		
		final Map<String, List<String>> ns_entities = getNamespaceHandler().entitiesByNamespace(entities);
		final Map<String, List<String>> ns_focusEntities = getNamespaceHandler().entitiesByNamespace(focusEntities);


		
		try {
			Connection connection = _connectionPool.getConnection();
			
			
			for (Entry<String, List<String>>  entitiesByNamespace : ns_entities.entrySet()) {
				final String namespace = entitiesByNamespace.getKey();
				final List<String> localFocusEntities = ns_focusEntities.get(namespace);
				
				// will be no matches - these are from different schemas.
				if (!ns_focusEntities.isEmpty() && (localFocusEntities == null || localFocusEntities.isEmpty())) {
					continue;
				}
	
				Statement stmt = connection.createStatement();
	
				String focusIds = DataAccessHelper.createNodeIdListFromCollection(localFocusEntities, true, false);
	
				String finFlowIntervalTable = getNamespaceHandler().tableName(namespace, DataAccessHelper.standardTableName(DataAccessHelper.FLOW_TABLE, date.getDurationPerBin().getInterval()));
				String finEntityIntervalTable = getNamespaceHandler().tableName(namespace, DataAccessHelper.standardTableName(DataAccessHelper.ENTITY_TABLE, date.getDurationPerBin().getInterval()));
				String finFlowDateColumn = getNamespaceHandler().columnName("PeriodDate");
				String dateColNoEscape = unescapeColumnName(finFlowDateColumn);
				
				List<String> idsCopy = new ArrayList<String>(entitiesByNamespace.getValue()); // copy the ids as we will take 100 at a time to process and the take method is destructive
				while (idsCopy.size() > 0) {
					List<String> tempSubList = (idsCopy.size() > 100) ? tempSubList = idsCopy.subList(0, 99) : idsCopy; // get the next 100
					List<String> subIds = new ArrayList<String>(tempSubList); // copy as the next step is destructive
					tempSubList.clear(); // this clears the IDs from idsCopy as tempSubList is backed by idsCopy 
		
					String ids = DataAccessHelper.createNodeIdListFromCollection(subIds, true, false);
					String sourceDirectionClause = "FromEntityId in ("+ids+") and ToEntityId in ("+focusIds+")";
					String destDirectionClause = "ToEntityId in ("+ids+") and FromEntityId in ("+focusIds+")";
					String focusedSQL = "select FromEntityId, ToEntityId, " + finFlowDateColumn + ", Amount from "+finFlowIntervalTable+" where " + finFlowDateColumn + " between '"+DataAccessHelper.format(startDate)+"' and '"+DataAccessHelper.format(endDate)+"' and "+sourceDirectionClause +
										" union " +
										"select FromEntityId, ToEntityId, " + finFlowDateColumn + ", Amount from "+finFlowIntervalTable+" where " + finFlowDateColumn + " between '"+DataAccessHelper.format(startDate)+"' and '"+DataAccessHelper.format(endDate)+"' and "+destDirectionClause;
					String tsSQL = "select EntityId, " + finFlowDateColumn + ", InboundAmount, OutboundAmount from "+finEntityIntervalTable+" where " + finFlowDateColumn + " between '"+DataAccessHelper.format(startDate)+"' and '"+DataAccessHelper.format(endDate)+"' and EntityId in ("+ids+")" ;
	
					s_logger.trace(focusedSQL);
	
					if (stmt.execute(focusedSQL)) {
						ResultSet rs = stmt.getResultSet();
						while (rs.next()) {
							String from = rs.getString("FromEntityId");
							String to = rs.getString("ToEntityId");
							Double amount = rs.getDouble("Amount");
							Date rsDate = rs.getDate(dateColNoEscape);
	
							String keyId = from;
							if (subIds.contains(to)) {
								keyId = to;
							}

							// globalize this for return
							keyId = getNamespaceHandler().globalFromLocalEntityId(namespace, keyId, TypedId.ACCOUNT);
							
							List<FL_Link> linkList = results.get(keyId);
							if (linkList == null) {
								linkList = new LinkedList<FL_Link>();
								results.put(keyId, linkList);
							}
							
							// only use the amount calculated above with the date respecting FLOW map
							List<FL_Property> properties = new ArrayList<FL_Property>();
							properties.add(new PropertyHelper(FL_PropertyTag.AMOUNT, amount));
							properties.add(new PropertyHelper(FL_PropertyTag.DATE, rsDate));
		
							// globalize these for return
							from = getNamespaceHandler().globalFromLocalEntityId(namespace, from, TypedId.ACCOUNT);
							to = getNamespaceHandler().globalFromLocalEntityId(namespace, to, TypedId.ACCOUNT);
							
							FL_Link link = new FL_Link(Collections.singletonList(FL_LinkTag.FINANCIAL), from, to, true, null, null, properties);
							linkList.add(link);
						}
						rs.close();
					}
					
					s_logger.trace(tsSQL);
					
					if (stmt.execute(tsSQL)) {
						ResultSet rs = stmt.getResultSet();
						while (rs.next()) {
							String entity = rs.getString("EntityId");
							Double inboundAmount = rs.getDouble("InboundAmount");
							Double outboundAmount = rs.getDouble("OutboundAmount");
							Date rsDate = rs.getDate(dateColNoEscape);
	
							// globalize this for return
							entity = getNamespaceHandler().globalFromLocalEntityId(namespace, entity, TypedId.ACCOUNT);
							
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
			}
			connection.close();
		} catch (ClassNotFoundException e) {
			throw new AvroRemoteException(e);
		} catch (SQLException e) {
			throw new AvroRemoteException(e);
		}
		
		return results;				
	}

	
	@Override
	public Map<String, List<FL_Entity>> getAccounts(List<String> entities) throws AvroRemoteException {
		Map<String, List<FL_Entity>> map = new HashMap<String, List<FL_Entity>>();
		for (FL_Entity entity : getEntities(entities, FL_LevelOfDetail.SUMMARY)) {
			map.put(entity.getUid(), Collections.singletonList(entity));
		}
		return map;
	}
	
	public String getClientState(String sessionId) throws AvroRemoteException {
		String data = null;
		try {
			Connection connection = _connectionPool.getConnection();
			Statement stmt = connection.createStatement();
		
			String clientStateTableName = getNamespaceHandler().tableName(null, "clientState");
				
			String sql = "select data from " + clientStateTableName + " where sessionId='"+sessionId+"'";
				
			s_logger.trace(sql);
			if (stmt.execute(sql)) {
				ResultSet rs = stmt.getResultSet();
				while (rs.next()) {
					data = rs.getString("data");
				}
				
				rs.close();
			}
			
			stmt.close();
			connection.close();
		} catch (ClassNotFoundException e) {
			throw new AvroRemoteException(e);
		} catch (SQLException e) {
			throw new AvroRemoteException(e);
		}
		
		return data;
	}
	
	public void setClientState(String sessionId, String state) throws AvroRemoteException {
		try {
			Connection connection = _connectionPool.getConnection();
			Statement stmt = connection.createStatement();
		
			String clientStateTableName = getNamespaceHandler().tableName(null, "clientState");
			boolean exists = false;
			String sql = "select sessionId from " + clientStateTableName + " where sessionId='"+sessionId+"'";
				
			s_logger.trace(sql);
			if (stmt.execute(sql)) {
				ResultSet rs = stmt.getResultSet();
				while (rs.next()) {
					exists = true;
				}
				rs.close();
			}

			String data = state.replaceAll("'", "''");
			DateTime now = new DateTime();
			if (exists) {
				sql = "update " + clientStateTableName + " set data='"+data+"', modified='"+DataAccessHelper.format(now)+"' where sessionId='"+sessionId+"'";
			} else {
				sql = "insert into " + clientStateTableName + " (sessionId, created, modified, data) values(sessionId='"+sessionId+"', '"+DataAccessHelper.format(now)+"', '"+DataAccessHelper.format(now)+"', '"+data+"')";				
			}
			
			stmt.close();
			connection.close();
		} catch (ClassNotFoundException e) {
			throw new AvroRemoteException(e);
		} catch (SQLException e) {
			throw new AvroRemoteException(e);
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

