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
import java.sql.PreparedStatement;
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


public abstract class DataViewDataAccess implements FL_DataAccess {

	private static Logger s_logger = LoggerFactory.getLogger(DataViewDataAccess.class);
	
	protected final SQLConnectionPool _connectionPool;
	protected final FL_EntitySearch _search;
	private final DataNamespaceHandler _namespaceHandler;
	
	private static Pattern COLUMN_PATTERN = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*", 0);
	private final static int PARAMETER_BRACKET[] = {1000, 500, 200, 100, 50, 20};
	
	private final static boolean USE_PREPARED_STATEMENTS = false;
	
	public DataViewDataAccess(
		SQLConnectionPool connectionPool,
		FL_EntitySearch search,
		DataNamespaceHandler namespaceHandler
	) throws ClassNotFoundException, SQLException {
		
		// TODO: ehCacheConfig!!
		_connectionPool = connectionPool;
		_search = search;
		_namespaceHandler = namespaceHandler;
	}
	
	
	
	
	protected DataNamespaceHandler getNamespaceHandler() {
		return _namespaceHandler;
	}
	
	
	
	
	@Override
	public List<FL_Entity> getEntities(
		List<String> entities, 
		FL_LevelOfDetail levelOfDetail
	) throws AvroRemoteException {
		
		List<FL_Entity> results = new ArrayList<FL_Entity>();
		
		// Build a map of native Ids against searched-for entities, for comparison against results 
		HashMap<String, String> entityNativeIdMap = new HashMap<String, String>();
		for (String e : entities) {
			TypedId tId = TypedId.fromTypedId(e);
			entityNativeIdMap.put(tId.getNativeId(), e);
		}				
		
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
				
				s_logger.debug("Searched for " + qCount + " ids, found " + searchResult.getTotal());
				
				for (FL_SearchResult r : searchResult.getResults()) {
					FL_Entity fle = (FL_Entity)r.getResult();
					TypedId rTId = TypedId.fromTypedId(fle.getUid());
					String rNId = rTId.getNativeId();
					
					if (entityNativeIdMap.containsKey(rNId)) {
						String mEId = entityNativeIdMap.get(rNId);
						TypedId mTId = TypedId.fromTypedId(mEId);
						
						// Check if the namespaces and types returned are what we were searching for
						if (((rTId.getNamespace() == null && mTId.getNamespace() == null) ||
							  rTId.getNamespace().equals(mTId.getNamespace())) &&
							(rTId.getType() == mTId.getType() ||
							 (rTId.getType() == TypedId.ACCOUNT_OWNER && mTId.getType() == TypedId.CLUSTER_SUMMARY) ||
							 (rTId.getType() == TypedId.CLUSTER_SUMMARY && mTId.getType() == TypedId.ACCOUNT_OWNER)
							)
						) {
							 results.add(fle);
						} else {
							s_logger.error("Got entity "+fle.getUid()+" that doesn't match the type/namespace of the search");
						}
	
					} else {
						s_logger.error("Got entity "+fle.getUid()+" that wasn't in search");
					}
				}
				
				qCount=0;
				idList.clear();
			}
		}
		
		return results;
	}
	
	
	private Map<String, List<FL_Link>> getFlowAggregationNoPrepared(
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
				
				String focusIds = DataAccessHelper.createNodeIdListFromCollection(localFocusEntities, getNamespaceHandler(), namespace);
				
				String finFlowTable = getNamespaceHandler().tableName(namespace, DataAccessHelper.FLOW_TABLE);
				String finFlowIntervalTable = getNamespaceHandler().tableName(namespace, DataAccessHelper.standardTableName(DataAccessHelper.FLOW_TABLE, date.getDurationPerBin().getInterval()));
				String finFlowFromEntityIdColumn = getNamespaceHandler().columnName(DataAccessHelper.FLOW_COLUMN_FROM_ENTITY_ID);
				String finFlowFromEntityTypeColumn = getNamespaceHandler().columnName(DataAccessHelper.FLOW_COLUMN_FROM_ENTITY_TYPE);
				String finFlowToEntityIdColumn = getNamespaceHandler().columnName(DataAccessHelper.FLOW_COLUMN_TO_ENTITY_ID);
				String finFlowToEntityTypeColumn = getNamespaceHandler().columnName(DataAccessHelper.FLOW_COLUMN_TO_ENTITY_TYPE);
				String finFlowAmountColumn = getNamespaceHandler().columnName(DataAccessHelper.FLOW_COLUMN_AMOUNT);
				String finFlowDateColumn = getNamespaceHandler().columnName(DataAccessHelper.FLOW_COLUMN_PERIOD_DATE);
				
				List<String> idsCopy = new ArrayList<String>(entitiesByNamespace.getValue()); // copy the ids as we will take 100 at a time to process and the take method is destructive
				while (idsCopy.size() > 0) {
					List<String> tempSubList = (idsCopy.size() > 100) ? tempSubList = idsCopy.subList(0, 99) : idsCopy; // get the next 100
					List<String> subIds = new ArrayList<String>(tempSubList); // copy as the next step is destructive
					tempSubList.clear(); // this clears the IDs from idsCopy as tempSubList is backed by idsCopy 
		
					String ids = DataAccessHelper.createNodeIdListFromCollection(subIds, getNamespaceHandler(), namespace);
					String sourceDirectionClause = finFlowFromEntityIdColumn + " in ("+ids+")";
					String destDirectionClause = finFlowToEntityIdColumn + " in ("+ids+")";
					
					if (focusIds != null) {
						sourceDirectionClause += " and " + finFlowToEntityIdColumn + " in ("+focusIds+")";
						destDirectionClause += " and " + finFlowFromEntityIdColumn + " in ("+focusIds+")";				
					}
					String directionClause = (direction == FL_DirectionFilter.BOTH ) ? sourceDirectionClause+" and "+destDirectionClause : (direction == FL_DirectionFilter.DESTINATION ) ? destDirectionClause : (direction == FL_DirectionFilter.SOURCE ) ? sourceDirectionClause : "1=1";
					String entityTypeClause = linkEntityTypeClause(direction, entityType);
					
					String dateRespectingFlowSQL = "select " + finFlowFromEntityIdColumn + ", " + finFlowToEntityIdColumn + ", sum(" + finFlowAmountColumn + ") as " + finFlowAmountColumn + " from "+finFlowIntervalTable+" where " + finFlowDateColumn + " between '"+ getNamespaceHandler().formatDate(startDate)+"' and '"+getNamespaceHandler().formatDate(endDate)+"' and "+directionClause+" and "+entityTypeClause+" group by " + finFlowFromEntityIdColumn + ", " + finFlowToEntityIdColumn;
					String flowSQL = "select " + finFlowFromEntityIdColumn + ", " + finFlowFromEntityTypeColumn + ", " + finFlowToEntityIdColumn + ", " + finFlowToEntityTypeColumn + " from " + finFlowTable + " where "+directionClause+" and "+entityTypeClause;
						
					Map<String, Map<String, Double>> fromToAmountMap = new HashMap<String, Map<String, Double>>();
					
					s_logger.trace(dateRespectingFlowSQL);
					if (stmt.execute(dateRespectingFlowSQL)) {
						ResultSet rs = stmt.getResultSet();
						while (rs.next()) {
							String from = rs.getString(finFlowFromEntityIdColumn);
							String to = rs.getString(finFlowToEntityIdColumn);
							Double amount = rs.getDouble(finFlowAmountColumn);
							if (fromToAmountMap.containsKey(from)) {
								if (fromToAmountMap.get(from).containsKey(to)) {
									fromToAmountMap.get(from).put(to, fromToAmountMap.get(from).get(to) + amount);
									s_logger.warn("Duplicate entity to entity link discovered: " + from + " to " + to + ". The link has been aggregated.");
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
							String from = rs.getString(finFlowFromEntityIdColumn);
							String fromType = rs.getString(finFlowFromEntityTypeColumn).toLowerCase();
							String to = rs.getString(finFlowToEntityIdColumn);
							String toType = rs.getString(finFlowToEntityTypeColumn).toLowerCase();
	
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
	
	private Map<String, List<FL_Link>> getFlowAggregationPrepared(
			List<String> entities,
			List<String> focusEntities,
			FL_DirectionFilter direction,
			FL_LinkEntityTypeFilter entityType,
			FL_LinkTag tag,
			FL_DateRange date
		) throws AvroRemoteException {
			
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
				
					DateTime startDate = DataAccessHelper.getStartDate(date);
					DateTime endDate = DataAccessHelper.getEndDate(date);

					String finFlowTable = getNamespaceHandler().tableName(namespace, DataAccessHelper.FLOW_TABLE);
					String finFlowIntervalTable = getNamespaceHandler().tableName(namespace, DataAccessHelper.standardTableName(DataAccessHelper.FLOW_TABLE, date.getDurationPerBin().getInterval()));
					String finFlowFromEntityIdColumn = getNamespaceHandler().columnName(DataAccessHelper.FLOW_COLUMN_FROM_ENTITY_ID);
					String finFlowFromEntityTypeColumn = getNamespaceHandler().columnName(DataAccessHelper.FLOW_COLUMN_FROM_ENTITY_TYPE);
					String finFlowToEntityIdColumn = getNamespaceHandler().columnName(DataAccessHelper.FLOW_COLUMN_TO_ENTITY_ID);
					String finFlowToEntityTypeColumn = getNamespaceHandler().columnName(DataAccessHelper.FLOW_COLUMN_TO_ENTITY_TYPE);
					String finFlowAmountColumn = getNamespaceHandler().columnName(DataAccessHelper.FLOW_COLUMN_AMOUNT);
					String finFlowDateColumn = getNamespaceHandler().columnName(DataAccessHelper.FLOW_COLUMN_PERIOD_DATE);
					
					List<String> idsCopy = new ArrayList<String>(entitiesByNamespace.getValue()); // copy the ids as we will take 100 at a time to process and the take method is destructive
					List<String> focusIdsCopy = localFocusEntities == null ? null : new ArrayList<String>(localFocusEntities);
					
					int idPos = 0;
					int focusIdPos = 0;
					List<String> subIds = null;
					List<String> subFocusIds = null; 
					
					while (idPos < idsCopy.size()) {
						
						int idBracket = 0; 
						int focusIdBracket = 0;
						
						// Calculate the statement parameter bracket for the remaining amount of ids
						while (idBracket != PARAMETER_BRACKET.length && 
								idPos + PARAMETER_BRACKET[idBracket] > idsCopy.size()) {
							idBracket++;
						}
						if (localFocusEntities != null) {
							while (focusIdBracket != PARAMETER_BRACKET.length && 
									focusIdPos + PARAMETER_BRACKET[focusIdBracket] > focusIdsCopy.size()) {
								focusIdBracket++;
							}
						}
						
						// Create sublists using bracket offsets	
						int idOffset = (idBracket == PARAMETER_BRACKET.length) ? idsCopy.size() : idPos + PARAMETER_BRACKET[idBracket];
						subIds = idsCopy.subList(idPos, idOffset);
						if (localFocusEntities != null) {
							
							int focusIdOffset = (focusIdBracket == PARAMETER_BRACKET.length) ? focusIdsCopy.size() : focusIdPos + PARAMETER_BRACKET[focusIdBracket];
							subFocusIds = focusIdsCopy.subList(focusIdPos, focusIdOffset);
						}
						
						Map<String, Map<String, Double>> fromToAmountMap = new HashMap<String, Map<String, Double>>();
						
						// Create prepared statement
						String preparedStatementString = buildPreparedStatementForDateRespectingFlow(
							subIds.size(),
							(localFocusEntities == null) ? 0 : subFocusIds.size(),
							direction,
							entityType,
							finFlowIntervalTable,
							finFlowAmountColumn,
							finFlowFromEntityIdColumn,
							finFlowToEntityIdColumn,
							finFlowDateColumn,
							finFlowFromEntityTypeColumn,
							finFlowToEntityTypeColumn
						);
						PreparedStatement stmt = connection.prepareStatement(preparedStatementString);
						
						int index = 1;
						
						stmt.setString(index++, getNamespaceHandler().formatDate(startDate));
						stmt.setString(index++, getNamespaceHandler().formatDate(endDate));
						
						if (direction == FL_DirectionFilter.BOTH) {
							for (int i = 0; i < subIds.size(); i++) {
								stmt.setString(index++, getNamespaceHandler().toSQLId(subIds.get(i), namespace));
							}
							if (localFocusEntities != null) {
								for (int i = 0; i < subFocusIds.size(); i++) {
									stmt.setString(index++, getNamespaceHandler().toSQLId(subFocusIds.get(i), namespace));
								}
							}
							for (int i = 0; i < subIds.size(); i++) {
								stmt.setString(index++, getNamespaceHandler().toSQLId(subIds.get(i), namespace));
							}
							if (localFocusEntities != null) {
								for (int i = 0; i < subFocusIds.size(); i++) {
									stmt.setString(index++, getNamespaceHandler().toSQLId(subFocusIds.get(i), namespace));
								}
							}
						} else if (direction == FL_DirectionFilter.SOURCE) {
							for (int i = 0; i < subIds.size(); i++) {
								stmt.setString(index++, getNamespaceHandler().toSQLId(subIds.get(i), namespace));
							}
							if (localFocusEntities != null) {
								for (int i = 0; i < subFocusIds.size(); i++) {
									stmt.setString(index++, getNamespaceHandler().toSQLId(subFocusIds.get(i), namespace));
								}
							}
						} else if (direction == FL_DirectionFilter.DESTINATION) {
							for (int i = 0; i < subIds.size(); i++) {
								stmt.setString(index++, getNamespaceHandler().toSQLId(subIds.get(i), namespace));
							}
							if (localFocusEntities != null) {
								for (int i = 0; i < subFocusIds.size(); i++) {
									stmt.setString(index++, getNamespaceHandler().toSQLId(subFocusIds.get(i), namespace));
								}
							}
						}
						
						if (entityType != FL_LinkEntityTypeFilter.ANY) {
							String type = "A";
							if (entityType == FL_LinkEntityTypeFilter.ACCOUNT_OWNER) {
								type = "O";
							} else if (entityType == FL_LinkEntityTypeFilter.CLUSTER_SUMMARY) {
								type = "S";
							}
							
							stmt.setString(index++, type);
							if (direction == FL_DirectionFilter.BOTH) {
								stmt.setString(index++, type);
							}
						}
						
						// Execute prepared statement and evaluate results
						ResultSet rs = stmt.executeQuery();
						while (rs.next()) {
							String from = rs.getString(finFlowFromEntityIdColumn);
							String to = rs.getString(finFlowToEntityIdColumn);
							Double amount = rs.getDouble(finFlowAmountColumn);
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
						rs.close();
						
						// Close prepared statement
						stmt.close();

						// Create prepared statement
						preparedStatementString = buildPreparedStatementForFlow(
							subIds.size(),
							(localFocusEntities == null) ? 0 : subFocusIds.size(),
							direction,
							entityType,
							finFlowFromEntityIdColumn,
							finFlowFromEntityTypeColumn,
							finFlowToEntityIdColumn,
							finFlowToEntityTypeColumn,
							finFlowTable
						);
						stmt = connection.prepareStatement(preparedStatementString);
						
						index = 1;

						if (direction == FL_DirectionFilter.BOTH) {
							for (int i = 0; i < subIds.size(); i++) {
								stmt.setString(index++, getNamespaceHandler().toSQLId(subIds.get(i), namespace));
							}
							if (localFocusEntities != null) {
								for (int i = 0; i < subFocusIds.size(); i++) {
									stmt.setString(index++, getNamespaceHandler().toSQLId(subFocusIds.get(i), namespace));
								}
							}
							for (int i = 0; i < subIds.size(); i++) {
								stmt.setString(index++, getNamespaceHandler().toSQLId(subIds.get(i), namespace));
							}
							if (localFocusEntities != null) {
								for (int i = 0; i < subFocusIds.size(); i++) {
									stmt.setString(index++, getNamespaceHandler().toSQLId(subFocusIds.get(i), namespace));
								}
							}
						} else if (direction == FL_DirectionFilter.SOURCE) {
							for (int i = 0; i < subIds.size(); i++) {
								stmt.setString(index++, getNamespaceHandler().toSQLId(subIds.get(i), namespace));
							}
							if (localFocusEntities != null) {
								for (int i = 0; i < subFocusIds.size(); i++) {
									stmt.setString(index++, getNamespaceHandler().toSQLId(subFocusIds.get(i), namespace));
								}
							}
						} else if (direction == FL_DirectionFilter.DESTINATION) {
							for (int i = 0; i < subIds.size(); i++) {
								stmt.setString(index++, getNamespaceHandler().toSQLId(subIds.get(i), namespace));
							}
							if (localFocusEntities != null) {
								for (int i = 0; i < subFocusIds.size(); i++) {
									stmt.setString(index++, getNamespaceHandler().toSQLId(subFocusIds.get(i), namespace));
								}
							}
						}
						if (entityType != FL_LinkEntityTypeFilter.ANY) {
							String type = "A";
							if (entityType == FL_LinkEntityTypeFilter.ACCOUNT_OWNER) {
								type = "O";
							} else if (entityType == FL_LinkEntityTypeFilter.CLUSTER_SUMMARY) {
								type = "S";
							}
							
							stmt.setString(index++, type);
							if (direction == FL_DirectionFilter.BOTH) {
								stmt.setString(index++, type);
							}
						}
						
						// Execute prepared statement and evaluate results
						rs = stmt.executeQuery();
						while (rs.next()) {
							String from = rs.getString(finFlowFromEntityIdColumn);
							String fromType = rs.getString(finFlowFromEntityTypeColumn).toLowerCase();
							String to = rs.getString(finFlowToEntityIdColumn);
							String toType = rs.getString(finFlowToEntityTypeColumn).toLowerCase();

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
						
						// Close prepared statement
						stmt.close();
						
						// Move the sublist positions
						if (localFocusEntities != null) {
							
							if (focusIdBracket != PARAMETER_BRACKET.length) { 
								focusIdPos += PARAMETER_BRACKET[focusIdBracket];
							} else {
								
								focusIdPos = 0;
								if (idBracket != PARAMETER_BRACKET.length) { 
									idPos += PARAMETER_BRACKET[idBracket];
								} else {
									idPos = idsCopy.size();
								}
							}						
						} else {
							
							if (idBracket != PARAMETER_BRACKET.length) { 
								idPos += PARAMETER_BRACKET[idBracket];
							} else {
								idPos = idsCopy.size();
							}
						}
					}
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
	public Map<String, List<FL_Link>> getFlowAggregation(
		List<String> entities,
		List<String> focusEntities,
		FL_DirectionFilter direction,
		FL_LinkEntityTypeFilter entityType,
		FL_LinkTag tag,
		FL_DateRange date
	) throws AvroRemoteException {
		if (USE_PREPARED_STATEMENTS) {
			return getFlowAggregationPrepared(entities, focusEntities, direction, entityType, tag, date);
		} else {
			return getFlowAggregationNoPrepared(entities, focusEntities, direction, entityType, tag, date);
		}
	}
	
	@Override
	public Map<String, List<FL_Link>> getTimeSeriesAggregation(
			List<String> entities,
			List<String> focusEntities,
			FL_LinkTag tag,
			FL_DateRange date) throws AvroRemoteException {
		if (USE_PREPARED_STATEMENTS) {
			return getTimeSeriesAggregationPrepared(entities, focusEntities, tag, date);
		} else {
			return getTimeSeriesAggregationNoPrepared(entities, focusEntities, tag, date);
		}
	}
	
	private Map<String, List<FL_Link>> getTimeSeriesAggregationPrepared(
		List<String> entities,
		List<String> focusEntities,
		FL_LinkTag tag,
		FL_DateRange date
	) throws AvroRemoteException {
		
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
	
				String finFlowIntervalTable = getNamespaceHandler().tableName(namespace, DataAccessHelper.standardTableName(DataAccessHelper.FLOW_TABLE, date.getDurationPerBin().getInterval()));
				String finFlowFromEntityIdColumn = getNamespaceHandler().columnName(DataAccessHelper.FLOW_COLUMN_FROM_ENTITY_ID);
				String finFlowToEntityIdColumn = getNamespaceHandler().columnName(DataAccessHelper.FLOW_COLUMN_TO_ENTITY_ID);
				String finFlowAmountColumn = getNamespaceHandler().columnName(DataAccessHelper.FLOW_COLUMN_AMOUNT);
				String finFlowDateColumn = getNamespaceHandler().columnName(DataAccessHelper.FLOW_COLUMN_PERIOD_DATE);
				String finFlowDateColNoEscape = unescapeColumnName(finFlowDateColumn);

				String finEntityIntervalTable = getNamespaceHandler().tableName(namespace, DataAccessHelper.standardTableName(DataAccessHelper.ENTITY_TABLE, date.getDurationPerBin().getInterval()));
				String finEntityEntityIdColumn = getNamespaceHandler().columnName(DataAccessHelper.ENTITY_COLUMN_ENTITY_ID);
				String finEntityInboundAmountColumn = getNamespaceHandler().columnName(DataAccessHelper.ENTITY_COLUMN_INBOUND_AMOUNT);
				String finEntityOutboundAmountColumn = getNamespaceHandler().columnName(DataAccessHelper.ENTITY_COLUMN_OUTBOUND_AMOUNT);
				String finEntityDateColumn = getNamespaceHandler().columnName(DataAccessHelper.ENTITY_COLUMN_PERIOD_DATE);
				String finEntityDateColNoEscape = unescapeColumnName(finFlowDateColumn);
				
				List<String> idsCopy = new ArrayList<String>(entitiesByNamespace.getValue()); // copy the ids as we will take 100 at a time to process and the take method is destructive
				while (idsCopy.size() > 0) {
					List<String> tempSubList = (idsCopy.size() > 100) ? tempSubList = idsCopy.subList(0, 99) : idsCopy; // get the next 100
					List<String> subIds = new ArrayList<String>(tempSubList); // copy as the next step is destructive
					tempSubList.clear(); // this clears the IDs from idsCopy as tempSubList is backed by idsCopy 
		
					if (localFocusEntities != null && !localFocusEntities.isEmpty()) {
						
						// Create prepared statement
						String preparedStatementString = buildPreparedStatementForSkippingFoci(
							subIds.size(),
							(localFocusEntities == null) ? 0 : localFocusEntities.size(),
							finFlowFromEntityIdColumn, 
							finFlowToEntityIdColumn,
							finFlowDateColumn, 
							finFlowAmountColumn,
							finFlowIntervalTable
						);
						PreparedStatement stmt = connection.prepareStatement(preparedStatementString);

						int index = 1;
						
						stmt.setString(index++, getNamespaceHandler().formatDate(startDate));
						stmt.setString(index++, getNamespaceHandler().formatDate(endDate));
						for (int i = 0; i < subIds.size(); i++) {
							stmt.setString(index++, getNamespaceHandler().toSQLId(subIds.get(i), namespace));
						}
						if (localFocusEntities != null) {
							for (int i = 0; i < localFocusEntities.size(); i++) {
								stmt.setString(index++, getNamespaceHandler().toSQLId(localFocusEntities.get(i), namespace));
							}
						}
						stmt.setString(index++, getNamespaceHandler().formatDate(startDate));
						stmt.setString(index++, getNamespaceHandler().formatDate(endDate));
						for (int i = 0; i < subIds.size(); i++) {
							stmt.setString(index++, getNamespaceHandler().toSQLId(subIds.get(i), namespace));
						}
						if (localFocusEntities != null) {
							for (int i = 0; i < localFocusEntities.size(); i++) {
								stmt.setString(index++, getNamespaceHandler().toSQLId(localFocusEntities.get(i), namespace));
							}
						}
						// Log prepared statement
						s_logger.trace(stmt.toString());
						
						// Execute prepared statement and evaluate results
						ResultSet rs = stmt.executeQuery();
						while (rs.next()) {
							String from = rs.getString(finFlowFromEntityIdColumn);
							String to = rs.getString(finFlowToEntityIdColumn);
							Double amount = rs.getDouble(finFlowAmountColumn);
							Date rsDate = rs.getDate(finFlowDateColNoEscape);
	
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
						
						// Close prepared statement
						stmt.close();
					}

					// Create prepared statement
					String preparedStatementString = buildPreparedStatementForTimeSeriesAggregation(
						subIds.size(),
						finEntityEntityIdColumn, 
						finEntityDateColumn,
						finEntityInboundAmountColumn, 
						finEntityOutboundAmountColumn,
						finEntityIntervalTable
					);
					PreparedStatement stmt = connection.prepareStatement(preparedStatementString);
					
					int index = 1;
					
					stmt.setString(index++, getNamespaceHandler().formatDate(startDate));
					stmt.setString(index++, getNamespaceHandler().formatDate(endDate));
					for (int i = 0; i < subIds.size(); i++) {
						stmt.setString(index++, getNamespaceHandler().toSQLId(subIds.get(i), namespace));
					}
					
					// Log prepared statement
					s_logger.trace(stmt.toString());
					
					// Execute prepared statement and evaluate results
					ResultSet rs = stmt.executeQuery();
					while (rs.next()) {
						String entity = rs.getString(finEntityEntityIdColumn);
						Double inboundAmount = rs.getDouble(finEntityInboundAmountColumn);
						Double outboundAmount = rs.getDouble(finEntityOutboundAmountColumn);
						Date rsDate = rs.getDate(finEntityDateColNoEscape);

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

					// Close prepared statement
					stmt.close();
				}
			}
			connection.close();
		} catch (ClassNotFoundException e) {
			throw new AvroRemoteException(e);
		} catch (SQLException e) {
			throw new AvroRemoteException(e);
		}
		
		return results;				
	}
	
	
	
	private Map<String, List<FL_Link>> getTimeSeriesAggregationNoPrepared(
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
	
				
				// skip foci from different schemas.
				boolean skipFoci = false;
				if (!ns_focusEntities.isEmpty() && (localFocusEntities == null || localFocusEntities.isEmpty())) {
					skipFoci = true;
				}				
				
				Statement stmt = connection.createStatement();
	
				String focusIds = DataAccessHelper.createNodeIdListFromCollection(localFocusEntities, getNamespaceHandler(), namespace);
	
				String finFlowIntervalTable = getNamespaceHandler().tableName(namespace, DataAccessHelper.standardTableName(DataAccessHelper.FLOW_TABLE, date.getDurationPerBin().getInterval()));
				String finFlowFromEntityIdColumn = getNamespaceHandler().columnName(DataAccessHelper.FLOW_COLUMN_FROM_ENTITY_ID);
				String finFlowToEntityIdColumn = getNamespaceHandler().columnName(DataAccessHelper.FLOW_COLUMN_TO_ENTITY_ID);
				String finFlowAmountColumn = getNamespaceHandler().columnName(DataAccessHelper.FLOW_COLUMN_AMOUNT);
				String finFlowDateColumn = getNamespaceHandler().columnName(DataAccessHelper.FLOW_COLUMN_PERIOD_DATE);
				String finFlowDateColNoEscape = unescapeColumnName(finFlowDateColumn);

				String finEntityIntervalTable = getNamespaceHandler().tableName(namespace, DataAccessHelper.standardTableName(DataAccessHelper.ENTITY_TABLE, date.getDurationPerBin().getInterval()));
				String finEntityEntityIdColumn = getNamespaceHandler().columnName(DataAccessHelper.ENTITY_COLUMN_ENTITY_ID);
				String finEntityInboundAmountColumn = getNamespaceHandler().columnName(DataAccessHelper.ENTITY_COLUMN_INBOUND_AMOUNT);
				String finEntityOutboundAmountColumn = getNamespaceHandler().columnName(DataAccessHelper.ENTITY_COLUMN_OUTBOUND_AMOUNT);
				String finEntityDateColumn = getNamespaceHandler().columnName(DataAccessHelper.ENTITY_COLUMN_PERIOD_DATE);
				String finEntityDateColNoEscape = unescapeColumnName(finFlowDateColumn);
				
				List<String> idsCopy = new ArrayList<String>(entitiesByNamespace.getValue()); // copy the ids as we will take 100 at a time to process and the take method is destructive
				while (idsCopy.size() > 0) {
					List<String> tempSubList = (idsCopy.size() > 100) ? tempSubList = idsCopy.subList(0, 99) : idsCopy; // get the next 100
					List<String> subIds = new ArrayList<String>(tempSubList); // copy as the next step is destructive
					tempSubList.clear(); // this clears the IDs from idsCopy as tempSubList is backed by idsCopy 
		
					String ids = DataAccessHelper.createNodeIdListFromCollection(subIds, getNamespaceHandler(), namespace);
					String tsSQL = "select " + finEntityEntityIdColumn + ", " + finEntityDateColumn + ", " + finEntityInboundAmountColumn + ", " + finEntityOutboundAmountColumn + " from "+finEntityIntervalTable+" where " + finEntityDateColumn + " between '"+getNamespaceHandler().formatDate(startDate)+"' and '"+getNamespaceHandler().formatDate(endDate)+"' and " + finEntityEntityIdColumn + " in ("+ids+")" ;
	
					if (!skipFoci) {
						String sourceDirectionClause = finFlowFromEntityIdColumn + " in ("+ids+") and " + finFlowToEntityIdColumn + " in ("+focusIds+")";
						String destDirectionClause = finFlowToEntityIdColumn + " in ("+ids+") and " + finFlowFromEntityIdColumn + " in ("+focusIds+")";
						String focusedSQL = "select " + finFlowFromEntityIdColumn + ", " + finFlowToEntityIdColumn + ", " + finFlowDateColumn + ", " + finFlowAmountColumn + " from "+finFlowIntervalTable+" where " + finFlowDateColumn + " between '"+getNamespaceHandler().formatDate(startDate)+"' and '"+getNamespaceHandler().formatDate(endDate)+"' and "+sourceDirectionClause +
											" union " +
											"select " + finFlowFromEntityIdColumn + ", " + finFlowToEntityIdColumn + ", " + finFlowDateColumn + ", " + finFlowAmountColumn + " from "+finFlowIntervalTable+" where " + finFlowDateColumn + " between '"+getNamespaceHandler().formatDate(startDate)+"' and '"+getNamespaceHandler().formatDate(endDate)+"' and "+destDirectionClause;
					
						s_logger.trace(focusedSQL);
		
						if (stmt.execute(focusedSQL)) {
							ResultSet rs = stmt.getResultSet();
							while (rs.next()) {
								String from = rs.getString(finFlowFromEntityIdColumn);
								String to = rs.getString(finFlowToEntityIdColumn);
								Double amount = rs.getDouble(finFlowAmountColumn);
								Date rsDate = rs.getDate(finFlowDateColNoEscape);
		
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
					}
					
					s_logger.trace(tsSQL);
					
					if (stmt.execute(tsSQL)) {
						ResultSet rs = stmt.getResultSet();
						while (rs.next()) {
							String entity = rs.getString(finEntityEntityIdColumn);
							Double inboundAmount = rs.getDouble(finEntityInboundAmountColumn);
							Double outboundAmount = rs.getDouble(finEntityOutboundAmountColumn);
							Date rsDate = rs.getDate(finEntityDateColNoEscape);
	
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
		
			String clientStateTableName = getNamespaceHandler().tableName(null, "clientState");
				
			// Create prepared statement
			PreparedStatement stmt = connection.prepareStatement(
				"SELECT data " +
				"FROM " + clientStateTableName + " " +
				"WHERE sessionId = ?"
			);
			stmt.setString(1, sessionId);
				
			// Log prepared statement
			s_logger.trace(stmt.toString());
			
			// Execute prepared statement and evaluate results
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				data = rs.getString("data");
			}
			rs.close();
			
			// Close prepared statement
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
		
			String clientStateTableName = getNamespaceHandler().tableName(null, "clientState");
			boolean exists = false;
	
			// Create prepared statement
			PreparedStatement stmt = connection.prepareStatement(
				"SELECT sessionId " +
				"FROM " + clientStateTableName + " " +
				"WHERE sessionId = ?"
			);
			stmt.setString(1, sessionId);
			
			// Execute prepared statement and evaluate results
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				exists = true;
			}
			rs.close();
			
			// Close prepared statement
			stmt.close();

			String data = state.replaceAll("'", "''");
			DateTime now = new DateTime();
			
			if (exists) {
				// Create UPDATE prepared statement
				PreparedStatement pstmt = connection.prepareStatement(
					"UPDATE " + clientStateTableName + " " +
					"SET data = ?, modified = ? " +
					"WHERE sessionId = ?"
				);
				pstmt.setString(1, data);
				pstmt.setString(2, getNamespaceHandler().formatDate(now));
				pstmt.setString(3, sessionId);
				
				// Log prepared statement
				s_logger.trace(pstmt.toString());
				
				// Execute prepared statement and evaluate results
				pstmt.executeUpdate();
				
				// Close prepared statement
				pstmt.close();
			} else {
				// Create INSERT prepared statement
				PreparedStatement pstmt = connection.prepareStatement(
					"INSERT INTO " + clientStateTableName + " (sessionId, created, modified, data)" +
					"VALUES (?, ?, ?, ?) "
				);
				pstmt.setString(2, sessionId);
				pstmt.setString(3, getNamespaceHandler().formatDate(now));
				pstmt.setString(4, getNamespaceHandler().formatDate(now));
				pstmt.setString(5, data);
				
				// Log prepared statement
				s_logger.trace(pstmt.toString());
				
				// Execute prepared statement and evaluate results
				pstmt.executeUpdate();
				
				// Close prepared statement
				pstmt.close();
			}

			connection.close();
		} catch (ClassNotFoundException e) {
			throw new AvroRemoteException(e);
		} catch (SQLException e) {
			throw new AvroRemoteException(e);
		}
	}
	
	
	
	
	private String buildPreparedStatementForFlow(
		int numIds,
		int numFocusIds,
		FL_DirectionFilter direction, 
		FL_LinkEntityTypeFilter entityType,
		String finFlowFromEntityIdColumn,
		String finFlowFromEntityTypeColumn,
		String finFlowToEntityIdColumn,
		String finFlowToEntityTypeColumn,
		String finFlowTable
	) {
		if (direction == null ||
			entityType == null ||
			finFlowFromEntityIdColumn == null ||
			finFlowFromEntityTypeColumn == null ||
			finFlowToEntityIdColumn == null ||
			finFlowToEntityTypeColumn == null ||
			finFlowTable == null
		) {
			s_logger.error("buildPreparedStatementForDateRespectingFlow: Invalid parameter");
			return null;
		}
		
		boolean addedWhereClause = false;
		
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT " + finFlowFromEntityIdColumn + ", " + finFlowFromEntityTypeColumn + ", " + finFlowToEntityIdColumn + ", " + finFlowToEntityTypeColumn + " ");
		sb.append("FROM " + finFlowTable + " ");
		
		if (numIds > 0) {
			sb.append("WHERE ");
			addedWhereClause = true;
			
			if (direction == FL_DirectionFilter.DESTINATION) {
				sb.append(finFlowToEntityIdColumn + " ");
			} else {
				sb.append(finFlowFromEntityIdColumn + " ");
			}
			sb.append("IN (");
			for (int i = 1; i < numIds; i++) {
				sb.append("?, ");
			}
			sb.append("?) ");
		}
		if (numFocusIds > 0) {
			
			if (!addedWhereClause) {
				sb.append("WHERE ");
				addedWhereClause = true;
			} else {
				sb.append("AND ");
			}
			
			if (direction == FL_DirectionFilter.DESTINATION) {
				sb.append(finFlowFromEntityIdColumn + " ");
			} else {
				sb.append(finFlowToEntityIdColumn + " ");
			}
			sb.append("IN (");
			for (int i = 1; i < numFocusIds; i++) {
				sb.append("?, ");
			}
			sb.append("?) ");
		}
		if (direction == FL_DirectionFilter.BOTH) {
			
			if (numIds > 0) {
				sb.append("AND " + finFlowToEntityIdColumn + " IN (");
				for (int i = 1; i < numIds; i++) {
					sb.append("?, ");
				}
				sb.append("?) ");
			}
			if (numFocusIds > 0) {
				sb.append("AND " + finFlowFromEntityIdColumn + " IN (");
				for (int i = 1; i < numFocusIds; i++) {
					sb.append("?, ");
				}
				sb.append("?) ");
			}
		}
		if (entityType != FL_LinkEntityTypeFilter.ANY) {
			
			if (!addedWhereClause) {
				sb.append("WHERE ");
				addedWhereClause = true;
			} else {
				sb.append("AND ");
			}
			
			if (direction == FL_DirectionFilter.DESTINATION) {
				sb.append(finFlowFromEntityTypeColumn + " ");
			} else {
				sb.append(finFlowToEntityTypeColumn + " ");
			}
			sb.append("= ? ");
			if (direction == FL_DirectionFilter.BOTH) {
				sb.append("AND " + finFlowFromEntityTypeColumn + " = ?");
			}
		}
		
		return sb.toString();
	}
	
	
	
	
	private String buildPreparedStatementForDateRespectingFlow(
		int numIds,
		int numFocusIds,
		FL_DirectionFilter direction, 
		FL_LinkEntityTypeFilter entityType,
		String finFlowIntervalTable, 
		String finFlowAmountColumn,
		String finFlowFromEntityIdColumn,
		String finFlowToEntityIdColumn, 
		String finFlowDateColumn,
		String finFlowFromEntityTypeColumn,
		String finFlowToEntityTypeColumn
	) {
		if (direction == null ||
			entityType == null ||
			finFlowIntervalTable == null ||
			finFlowAmountColumn == null
		) {
			s_logger.error("buildPreparedStatementForDateRespectingFlow: Invalid parameter");
			return null;
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT " + finFlowFromEntityIdColumn + ", " + finFlowToEntityIdColumn + ", SUM(" + finFlowAmountColumn + ") AS " + finFlowAmountColumn + " ");
		sb.append("FROM " + finFlowIntervalTable + " ");
		sb.append("WHERE " + finFlowDateColumn + " BETWEEN ? AND ? ");
		if (numIds > 0) {
			sb.append("AND ");
			if (direction == FL_DirectionFilter.DESTINATION) {
				sb.append(finFlowToEntityIdColumn + " ");
			} else {
				sb.append(finFlowFromEntityIdColumn + " ");
			}
			sb.append("IN (");
			for (int i = 1; i < numIds; i++) {
				sb.append("?, ");
			}
			sb.append("?) ");
		}
		if (numFocusIds > 0) {
			sb.append("AND ");
			if (direction == FL_DirectionFilter.DESTINATION) {
				sb.append(finFlowFromEntityIdColumn + " ");
			} else {
				sb.append(finFlowToEntityIdColumn + " ");
			}
			sb.append("IN (");
			for (int i = 1; i < numFocusIds; i++) {
				sb.append("?, ");
			}
			sb.append("?) ");
		}
		if (direction == FL_DirectionFilter.BOTH) {
			if (numIds > 0) {
				sb.append("AND " + finFlowToEntityIdColumn + " IN (");
				for (int i = 1; i < numIds; i++) {
					sb.append("?, ");
				}
				sb.append("?) ");
			}
			if (numFocusIds > 0) {
				sb.append("AND " + finFlowFromEntityIdColumn + " IN (");
				for (int i = 1; i < numFocusIds; i++) {
					sb.append("?, ");
				}
				sb.append("?) ");
			}
		}
		if (entityType != FL_LinkEntityTypeFilter.ANY) {
			sb.append("AND ");
			if (direction == FL_DirectionFilter.DESTINATION) {
				sb.append(finFlowFromEntityTypeColumn + " ");
			} else {
				sb.append(finFlowToEntityTypeColumn + " ");
			}
			sb.append("= ? ");
			if (direction == FL_DirectionFilter.BOTH) {
				sb.append("AND " + finFlowFromEntityTypeColumn + " = ?");
			}
		}
		sb.append("GROUP BY " + finFlowFromEntityIdColumn + ", " + finFlowToEntityIdColumn);
		
		return sb.toString();
	}
	
	
	
	
	private String buildPreparedStatementForTimeSeriesAggregation(
		int numIds,
		String finEntityEntityIdColumn, 
		String finEntityDateColumn,
		String finEntityInboundAmountColumn,
		String finEntityOutboundAmountColumn, 
		String finEntityIntervalTable
	) {
		if (finEntityEntityIdColumn == null ||
			finEntityDateColumn == null ||
			finEntityInboundAmountColumn == null ||
			finEntityOutboundAmountColumn == null ||
			finEntityIntervalTable == null
		) {
			s_logger.error("buildPreparedStatementForTimeSeriesAggregation: Invalid parameter");
			return null;
		}

		StringBuilder sb = new StringBuilder();
		
		sb.append("SELECT " + finEntityEntityIdColumn + ", " + finEntityDateColumn + ", " + finEntityInboundAmountColumn + ", " + finEntityOutboundAmountColumn + " ");
		sb.append("FROM " + finEntityIntervalTable + " ");
		sb.append("WHERE " + finEntityDateColumn + " BETWEEN ? AND ? ");
		if (numIds > 0) {
			sb.append("AND " + finEntityEntityIdColumn + " IN (");
			for (int i = 1; i < numIds; i++) {
				sb.append("?, ");
			}
			sb.append("?) ");
		}
		
		return sb.toString();
	}




	private String buildPreparedStatementForSkippingFoci(
		int numIds, 
		int numFocusIds,
		String finFlowFromEntityIdColumn, 
		String finFlowToEntityIdColumn,
		String finFlowDateColumn, 
		String finFlowAmountColumn,
		String finFlowIntervalTable
	) {
		if (finFlowFromEntityIdColumn == null ||
			finFlowToEntityIdColumn == null ||
			finFlowDateColumn == null ||
			finFlowAmountColumn == null ||
			finFlowIntervalTable == null
		) {
			s_logger.error("buildPreparedStatementForSkippingFoci: Invalid parameter");
			return null;
		}
		
		StringBuilder sb = new StringBuilder();
		
		sb.append("SELECT " + finFlowFromEntityIdColumn + ", " + finFlowToEntityIdColumn + ", " + finFlowDateColumn + ", " + finFlowAmountColumn + " ");
		sb.append("FROM " + finFlowIntervalTable + " ");
		sb.append("WHERE " + finFlowDateColumn + " BETWEEN ? AND ? ");
		if (numIds > 0) {
			sb.append("AND " + finFlowFromEntityIdColumn + " IN (");
			for (int i = 1; i < numIds; i++) {
				sb.append("?, ");
			}
			sb.append("?) ");
		}
		if (numFocusIds > 0) {
			sb.append("AND " + finFlowToEntityIdColumn + " IN (");
			for (int i = 1; i < numFocusIds; i++) {
				sb.append("?, ");
			}
			sb.append("?) ");
		}
		sb.append("UNION ");
		sb.append("SELECT " + finFlowFromEntityIdColumn + ", " + finFlowToEntityIdColumn + ", " + finFlowDateColumn + ", " + finFlowAmountColumn + " ");
		sb.append("FROM " + finFlowIntervalTable + " ");
		sb.append("WHERE " + finFlowDateColumn + " BETWEEN ? AND ? ");
		if (numIds > 0) {
		sb.append("AND " + finFlowToEntityIdColumn + " IN (");
			for (int i = 1; i < numIds; i++) {
				sb.append("?, ");
			}
			sb.append("?) ");
		}
		if (numFocusIds > 0) {
			sb.append("AND " + finFlowFromEntityIdColumn + " IN (");
			for (int i = 1; i < numFocusIds; i++) {
				sb.append("?, ");
			}
			sb.append("?) ");
		}
		
		return sb.toString();
	}
	
	private static String unescapeColumnName(String columnName) {
		Matcher m = COLUMN_PATTERN.matcher(columnName);
		if (m.find()) {
			return m.group();
		}
		return columnName;
	}
	
	private String linkEntityTypeClause(FL_DirectionFilter direction, FL_LinkEntityTypeFilter entityType) {
		if (entityType == FL_LinkEntityTypeFilter.ANY) return " 1=1 ";
		
		StringBuilder clause = new StringBuilder();
		String type = "A";
		if (entityType == FL_LinkEntityTypeFilter.ACCOUNT_OWNER) {
			type = "O";
		} else if (entityType == FL_LinkEntityTypeFilter.CLUSTER_SUMMARY) {
			type = "S";
		}
		
		String finFlowFromEntityTypeColumn = getNamespaceHandler().columnName(DataAccessHelper.FLOW_COLUMN_FROM_ENTITY_TYPE);
		String finFlowToEntityTypeColumn = getNamespaceHandler().columnName(DataAccessHelper.FLOW_COLUMN_TO_ENTITY_TYPE);

		// reverse direction - we are filtering on other entity
		switch (direction) {
		case DESTINATION:
			clause.append(" " + finFlowFromEntityTypeColumn + " = '" + type + "' ");
			break;
		case SOURCE:
			clause.append(" " + finFlowToEntityTypeColumn + " = '" + type + "' ");
			break;
		case BOTH:
			clause.append(" " + finFlowFromEntityTypeColumn + " = '" + type + "' AND " + finFlowToEntityTypeColumn + " = '" + type + "' ");
			break;
		}
		
		return clause.toString();
	}	
}

