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
package influent.kiva.server.dataaccess;

import influent.idl.FL_Constraint;
import influent.idl.FL_DataAccess;
import influent.idl.FL_DateRange;
import influent.idl.FL_Entity;
import influent.idl.FL_EntitySearch;
import influent.idl.FL_LevelOfDetail;
import influent.idl.FL_Link;
import influent.idl.FL_LinkTag;
import influent.idl.FL_ListRange;
import influent.idl.FL_Property;
import influent.idl.FL_PropertyMatchDescriptor;
import influent.idl.FL_PropertyTag;
import influent.idl.FL_PropertyType;
import influent.idl.FL_SearchResult;
import influent.idl.FL_SearchResults;
import influent.idl.FL_SortBy;
import influent.idl.FL_TransactionResults;
import influent.idlhelper.EntityHelper;
import influent.idlhelper.PropertyHelper;
import influent.server.dataaccess.DataAccessHelper;
import influent.server.dataaccess.DataNamespaceHandler;
import influent.server.dataaccess.DataViewDataAccess;
import influent.server.utilities.SQLConnectionPool;
import influent.server.utilities.TypedId;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.avro.AvroRemoteException;
import org.joda.time.DateTime;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

/**
 * Hopefully a better implementation of the Kiva Data Access
 * @author msavigny
 *
 */

public class KivaDataAccess extends DataViewDataAccess implements FL_DataAccess {

	public static final String s_kivaTypeKey = "kiva_type";
	private static Logger s_logger = LoggerFactory.getLogger(KivaDataAccess.class);

	// --- Financials ---
	public static final String FINANCIALS 			= "Financials";
	public static final String FINANCIALS_FROM 		= "Financials_FromEntityId";
	public static final String FINANCIALS_TO 		= "Financials_ToEntityId";
	public static final String FINANCIALS_DATE 		= "Financials_Date";
	public static final String FINANCIALS_AMOUNT 	= "Financials_Amount";
	public static final String FINANCIALS_COMMENT 	= "Financials_Comment";
	public static final String FINANCIALS_LOANID 	= "Financials_LoanId";
	
	// --- Brokers ---
	public static final String PARTNER_BROKERS 		= "PartnerBrokers";
	
	private static DecimalFormat world_df = new DecimalFormat("#,##0.00;-#,##0.00");
	
	private static String[] MONETARY_SUFFIX = new String[]{"","k", "m", "b", "t"};
	
	private final static boolean USE_PREPARED_STATEMENTS = false;
	
	@Inject	
	public KivaDataAccess(
		SQLConnectionPool connectionPool,
		FL_EntitySearch search,
		DataNamespaceHandler namespaceHandler
	) throws ClassNotFoundException, SQLException, JSONException {
		super(connectionPool, search, namespaceHandler);
	}
	
	
	
	
	/**
	 * TODO: push this change down from a list of singleton match descriptors to a single list match descriptor
	 */
	@Override
	public List<FL_Entity> getEntities(
		List<String> entities, 
		FL_LevelOfDetail levelOfDetail
	) throws AvroRemoteException {
	
		//Construct the id search query for the search
		
		List<FL_Entity> results = new ArrayList<FL_Entity>();
		
		// Build a map of native Ids against searched-for entities, for comparison against results 
		HashMap<String, String> entityNativeIdMap = new HashMap<String, String>();
		for (String e : entities) {
			TypedId tId = TypedId.fromTypedId(e);
			entityNativeIdMap.put(tId.getNativeId(), e);
		}		
		
		int qCount = 0;			//How many entities to query at once
		int maxFetch = 100;
		List<String> uidVals = new ArrayList<String>(maxFetch);
		Iterator<String> uidIter = entities.iterator();
		
		try {
		
			while (uidIter.hasNext()) {
				String uid = uidIter.next();
				uidVals.add(uid);
			
				if (qCount == (maxFetch-1) || !uidIter.hasNext()) {
					FL_PropertyMatchDescriptor uidMatch = FL_PropertyMatchDescriptor.newBuilder()
						.setKey("uid")
						.setRange(FL_ListRange.newBuilder().setType(FL_PropertyType.STRING).setValues(new ArrayList<Object>(uidVals)).build())
						.setConstraint(FL_Constraint.REQUIRED_EQUALS)
						.build();

					FL_SearchResults searchResult = _search.search(Collections.singletonList(uidMatch), 0, maxFetch);
				
					s_logger.info("Searched for "+qCount+" uids, found "+searchResult.getTotal());
				
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

						
						/*
						if (entities.contains(fle.getUid())) {
							results.add(fle);
						} else {
							s_logger.error("Got entitiy "+fle.getUid()+" that wasn't in search");
						}
						*/
					}
				
					qCount=0;
					uidVals.clear();
				} else {
					qCount++;
				}
			}
		}
		catch (Exception e) {
			throw new AvroRemoteException(e);
		}
		
		return results;
	}
	
	
	@Override
	public FL_TransactionResults getAllTransactions(
		List<String> entities,
		FL_LinkTag tag,
		FL_DateRange dateRange,
		FL_SortBy sort,
		List<String> linkFilter,
		long start,
		long max
	) throws AvroRemoteException {
		if (USE_PREPARED_STATEMENTS) {
			return getAllTransactionsPrepared(entities, tag, dateRange, sort, linkFilter, start, max);
		} else {
			return getAllTransactionsNoPrepared(entities, tag, dateRange, sort, linkFilter, start, max);
		}
	}
		
	public FL_TransactionResults getAllTransactionsNoPrepared(
			List<String> entities,
			FL_LinkTag tag,
			FL_DateRange dateRange,
			FL_SortBy sort,
			List<String> linkFilter,
			long start,
			long max) throws AvroRemoteException {
		
		final List<String> summaryIds = TypedId.filterTypedIds(entities, TypedId.CLUSTER_SUMMARY);
		final List<String> entityIds = TypedId.filterTypedIds(entities, TypedId.ACCOUNT);
		entityIds.addAll(TypedId.filterTypedIds(entities, TypedId.ACCOUNT_OWNER));
		
		final List<String> ns_summaries = TypedId.nativeFromTypedIds(summaryIds);
		final List<String> ns_entities = TypedId.nativeFromTypedIds(entityIds);
		final List<String> ns_filters = linkFilter != null? TypedId.nativeFromTypedIds(linkFilter): null;

		final List<FL_Link> links = new ArrayList<FL_Link>();
		final FL_TransactionResults.Builder results = FL_TransactionResults.newBuilder().setResults(links);
		
		try {
			Connection connection = _connectionPool.getConnection();
			Statement stmt = connection.createStatement();
			
			DateTime startDate = DataAccessHelper.getStartDate(dateRange);
			DateTime endDate = DataAccessHelper.getEndDate(dateRange);
			
			// TODO remove me when old dbs using 'Date' have been updated or rebuilt
//			String dateColumnName = getNamespaceHandler().tableName(null, "TransactionDate");
			
			// FIX: the splitting of entity ids will cause the top/limit function to be incorrect and require some sort of fix post collection.
			
			final String financials = getNamespaceHandler().tableName(null, FINANCIALS);
			
			// fetch all account transactions
			List<String> idsCopy = new ArrayList<String>(ns_entities); // copy the ids as we will take 100 at a time to process and the take method is destructive
			while (idsCopy.size() > 0) {
				List<String> tempSubList = (idsCopy.size() > 100) ? tempSubList = idsCopy.subList(0, 99) : idsCopy; // get the next 100
				List<String> subIds = new ArrayList<String>(tempSubList); // copy as the next step is destructive
				tempSubList.clear(); // this clears the IDs from idsCopy as tempSubList is backed by idsCopy 
								
				String sql = buildTransactionSQL(subIds, false, financials, startDate, endDate, ns_filters, sort, max);
				
				s_logger.trace("execute: " + sql);

				links.addAll( fetchTransactionsNoPrepared(stmt, sql, ns_entities, sort, results) );
			}
			
			final List<String> ns_summaryEntities = getSummaryEntitiesNoPrepared(stmt, ns_summaries);
			
			// fetch all summary transactions
			idsCopy = new ArrayList<String>(ns_summaries); // copy the ids as we will take 100 at a time to process and the take method is destructive
			while (idsCopy.size() > 0) {
				List<String> tempSubList = (idsCopy.size() > 100) ? tempSubList = idsCopy.subList(0, 99) : idsCopy; // get the next 100
				List<String> subIds = new ArrayList<String>(tempSubList); // copy as the next step is destructive
				tempSubList.clear(); // this clears the IDs from idsCopy as tempSubList is backed by idsCopy 
								
				String sql = buildTransactionSQL(subIds, true, financials, startDate, endDate, ns_filters, sort, max);
				
				s_logger.trace("execute: " + sql);

				links.addAll( fetchTransactionsNoPrepared(stmt, sql, ns_summaryEntities, sort, results) );
			}
			
			stmt.close();
			connection.close();
		} catch (SQLException e) {
			throw new AvroRemoteException(e);
		} catch (ClassNotFoundException e) {
			throw new AvroRemoteException(e);
		}

		return results.build();
	}	
	
	
	public FL_TransactionResults getAllTransactionsPrepared(
		List<String> entities,
		FL_LinkTag tag,
		FL_DateRange dateRange,
		FL_SortBy sort,
		List<String> linkFilter,
		long start,
		long max
	) throws AvroRemoteException {
		
		final List<String> summaryIds = TypedId.filterTypedIds(entities, TypedId.CLUSTER_SUMMARY);
		final List<String> entityIds = TypedId.filterTypedIds(entities, TypedId.ACCOUNT);
		entityIds.addAll(TypedId.filterTypedIds(entities, TypedId.ACCOUNT_OWNER));
		
		final List<String> ns_summaries = TypedId.nativeFromTypedIds(summaryIds);
		final List<String> ns_entities = TypedId.nativeFromTypedIds(entityIds);
		final List<String> ns_filters = linkFilter != null? TypedId.nativeFromTypedIds(linkFilter): null;

		final List<FL_Link> links = new ArrayList<FL_Link>();
		final FL_TransactionResults.Builder results = FL_TransactionResults.newBuilder().setResults(links);
		
		try {
			Connection connection = _connectionPool.getConnection();
			
			DateTime startDate = DataAccessHelper.getStartDate(dateRange);
			DateTime endDate = DataAccessHelper.getEndDate(dateRange);
			
			final String financials = getNamespaceHandler().tableName(null, FINANCIALS);
			
			// fetch all account transactions
			List<String> idsCopy = new ArrayList<String>(ns_entities); // copy the ids as we will take 100 at a time to process and the take method is destructive
			while (idsCopy.size() > 0) {
				List<String> tempSubList = (idsCopy.size() > 100) ? tempSubList = idsCopy.subList(0, 99) : idsCopy; // get the next 100
				List<String> subIds = new ArrayList<String>(tempSubList); // copy as the next step is destructive
				tempSubList.clear(); // this clears the IDs from idsCopy as tempSubList is backed by idsCopy 
								
				String preparedStatementString = buildTransactionPreparedStatementString(subIds, false, financials, ns_filters, sort, max);
				PreparedStatement stmt = connection.prepareStatement(preparedStatementString);
				
				setTransactionPreparedStatement(stmt, false, subIds, ns_filters, startDate, endDate);
				
				links.addAll(
					fetchTransactionsPrepared(
						stmt, 
						ns_entities, 
						sort, 
						results
					)
				);
				
				stmt.close();
			}
			
			final List<String> ns_summaryEntities = getSummaryEntitiesPrepared(connection, ns_summaries);
			
			// fetch all summary transactions
			idsCopy = new ArrayList<String>(ns_summaries); // copy the ids as we will take 100 at a time to process and the take method is destructive
			while (idsCopy.size() > 0) {
				List<String> tempSubList = (idsCopy.size() > 100) ? tempSubList = idsCopy.subList(0, 99) : idsCopy; // get the next 100
				List<String> subIds = new ArrayList<String>(tempSubList); // copy as the next step is destructive
				tempSubList.clear(); // this clears the IDs from idsCopy as tempSubList is backed by idsCopy 
								
				String preparedStatementString = buildTransactionPreparedStatementString(subIds, true, financials, ns_filters, sort, max);
				PreparedStatement stmt = connection.prepareStatement(preparedStatementString);

				setTransactionPreparedStatement(stmt, true, subIds, ns_filters, startDate, endDate);
				
				links.addAll(
					fetchTransactionsPrepared(
						stmt, 
						ns_summaryEntities, 
						sort, 
						results
					) 
				);
				
				stmt.close();
			}
			
			connection.close();
		} catch (SQLException e) {
			throw new AvroRemoteException(e);
		} catch (ClassNotFoundException e) {
			throw new AvroRemoteException(e);
		}

		return results.build();
	}

	@Override
	public Map<String, List<FL_Entity>> getAccounts(
			List<String> entities
		) throws AvroRemoteException {
		
		if (USE_PREPARED_STATEMENTS) {
			return getAccountsPrepared(entities);
		} else {
			return getAccountsNoPrepared(entities);
		}
	}
	
	
	public Map<String, List<FL_Entity>> getAccountsNoPrepared(
			List<String> entities
	) throws AvroRemoteException {
		final List<String> ns_entities = TypedId.nativeFromTypedIds(entities);
		Map<String, List<FL_Entity>> map = new HashMap<String, List<FL_Entity>>();
		
		if (ns_entities.isEmpty()) return map;
		
		try {
			Connection connection = _connectionPool.getConnection();
			Statement stmt = connection.createStatement();
			
			String partnerBrokersTable = getNamespaceHandler().tableName(null, PARTNER_BROKERS);
			
			// lookup partner accounts for partner entities
			String sql = "select entityId, rawEntityId " +
						 "   from " + partnerBrokersTable + " " +
						 "  where rawEntityId IN " + DataAccessHelper.createInClause(ns_entities);
			
			List<String> accounts = new LinkedList<String>();
			
			if (stmt.execute(sql)) {
				ResultSet rs = stmt.getResultSet();
				while (rs.next()) {
                    TypedId tId = TypedId.fromNativeId(TypedId.ACCOUNT, rs.getString("entityId"));
                    String accountId = getNamespaceHandler().globalFromLocalEntityId(tId.getNamespace(), tId.getNativeId(), tId.getType());
					accounts.add(accountId);
				}
			}
			
			for (FL_Entity entity : getEntities(accounts, FL_LevelOfDetail.SUMMARY) ) {
				String ownerId = (String)EntityHelper.getFirstPropertyByTag(entity, FL_PropertyTag.ACCOUNT_OWNER).getValue();
				
				if (!map.containsKey(ownerId)) {
					map.put(ownerId, new LinkedList<FL_Entity>());
				}
				map.get(ownerId).add(entity);
			}
			
			stmt.close();
			connection.close();
		}
		catch (Exception e) {
			throw new AvroRemoteException(e);
		}
		return map;
	}	
	
	public Map<String, List<FL_Entity>> getAccountsPrepared(
		List<String> entities
	) throws AvroRemoteException {

		final List<String> ns_entities = TypedId.nativeFromTypedIds(entities);
		Map<String, List<FL_Entity>> map = new HashMap<String, List<FL_Entity>>();
		
		if (ns_entities.isEmpty()) return map;
		
		try {
			List<String> accounts = new LinkedList<String>();
			
			Connection connection = _connectionPool.getConnection();
			
			String partnerBrokersTable = getNamespaceHandler().tableName(null, PARTNER_BROKERS);
			
			String preparedStatementString = buildPreparedStatementForGetAccounts(
				ns_entities.size(),
				partnerBrokersTable
			);
			PreparedStatement stmt = connection.prepareStatement(preparedStatementString);
			
			int index = 1;
			
			for (int i = 0; i < ns_entities.size(); i++) {
				stmt.setString(index++, getNamespaceHandler().toSQLId(ns_entities.get(i), null));
			}

			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
                TypedId tId = TypedId.fromNativeId(TypedId.ACCOUNT, rs.getString("entityId"));
                String accountId = getNamespaceHandler().globalFromLocalEntityId(tId.getNamespace(), tId.getNativeId(), tId.getType());
				accounts.add(accountId);
			}

			
			for (FL_Entity entity : getEntities(accounts, FL_LevelOfDetail.SUMMARY) ) {
				String ownerId = (String)EntityHelper.getFirstPropertyByTag(entity, FL_PropertyTag.ACCOUNT_OWNER).getValue();
				
				if (!map.containsKey(ownerId)) {
					map.put(ownerId, new LinkedList<FL_Entity>());
				}
				map.get(ownerId).add(entity);
			}
			
			stmt.close();
			connection.close();
		}
		catch (Exception e) {
			throw new AvroRemoteException(e);
		}
		return map;
	}
	
	
	
	
	private String buildPreparedStatementForGetAccounts(
		int numIds,
		String partnerBrokersTable
	) {
		if (numIds < 1 || 
			partnerBrokersTable == null
		) {
			s_logger.error("buildPreparedStatementForGetAccounts: Invalid parameter");
			return null;
		}

		StringBuilder sb = new StringBuilder();
		
		sb.append("SELECT entityId, rawEntityId ");
		sb.append("FROM " + partnerBrokersTable + " ");
		sb.append("WHERE rawEntityId IN (");
		for (int i = 1; i < numIds; i++) {
			sb.append("?, ");
		}
		sb.append("?) ");
		
		return sb.toString();
	}
	
	private List<FL_Link> fetchTransactionsNoPrepared(
			Statement stmt, 
			String sql, 
			List<String> entities, 
			FL_SortBy sort, 
			FL_TransactionResults.
			Builder results
	) throws SQLException {
		final List<FL_Link> links = new ArrayList<FL_Link>();
		
		String fromColumn = unescapeName(getNamespaceHandler().columnName(FINANCIALS_FROM));
		String toColumn = unescapeName(getNamespaceHandler().columnName(FINANCIALS_TO));
		String dateColumn = unescapeName(getNamespaceHandler().columnName(FINANCIALS_DATE));
		String loanIdColumn = unescapeName(getNamespaceHandler().columnName(FINANCIALS_LOANID));
		String amountColumn = unescapeName(getNamespaceHandler().columnName(FINANCIALS_AMOUNT));
		String commentColumn = unescapeName(getNamespaceHandler().columnName(FINANCIALS_COMMENT));
		
		if (stmt.execute(sql)) {
			ResultSet rs = stmt.getResultSet();
			while (rs.next()) {
				String from = rs.getString(fromColumn);
				
				if (from == null) {
					results.setTotal(rs.getLong(loanIdColumn));
					
					continue;
				}
				
				String to = rs.getString(toColumn);
				Date date = new Date(rs.getTimestamp(dateColumn).getTime());
				Double amount = rs.getDouble(amountColumn);
				String comment = rs.getString(commentColumn);
				
				Double credit = checkEntitiesContainsEntity(entities, to) ? amount : 0.0;
				Double debit = checkEntitiesContainsEntity(entities, from) ? amount : 0.0;

				// drop the postfix if we're sorting by amount so that linklist only has 1 value
				// could probably generalize this but let's be conservative for safety's sake
				if(FL_SortBy.AMOUNT == sort) {		
					if(from.indexOf('-') != -1) {
						from = from.substring(0, from.indexOf('-'));
					}
					if(to.indexOf('-') != -1) {
						to = to.substring(0, to.indexOf('-'));
					}
				}
				
				// do some aesthetic enhancements on the comments field; this can be removed if the source tables 
				// are cleaned or properly regenerated in the future
				comment = formatCommentString(comment);
				
				List<FL_Property> properties = new ArrayList<FL_Property>();
				properties.add(
					new PropertyHelper(
						"inflowing", 
						"inflowing", 
						credit, 
						Arrays.asList(
							FL_PropertyTag.INFLOWING, 
							FL_PropertyTag.AMOUNT, 
							FL_PropertyTag.USD
						)
					)
				);
				properties.add(
					new PropertyHelper(
						"outflowing", 
						"outflowing", 
						debit, 
						Arrays.asList(
							FL_PropertyTag.OUTFLOWING, 
							FL_PropertyTag.AMOUNT, 
							FL_PropertyTag.USD
						)
					)
				);
				properties.add(
					new PropertyHelper(
						"comment", 
						"comment", 
						comment, 
						Collections.singletonList(
							FL_PropertyTag.ANNOTATION
						)
					)
				);
				properties.add(
					new PropertyHelper(
						FL_PropertyTag.DATE, 
						date
					)
				);
				properties.add(
					new PropertyHelper(
						FL_PropertyTag.ID, 
						rs.getString(loanIdColumn)
					)
				);

                TypedId fromId = TypedId.fromNativeId(TypedId.ACCOUNT, from);
                TypedId toId = TypedId.fromNativeId(TypedId.ACCOUNT, to);
                FL_Link link = new FL_Link(Collections.singletonList(FL_LinkTag.FINANCIAL),
                        getNamespaceHandler().globalFromLocalEntityId(fromId.getNamespace(), fromId.getNativeId(), fromId.getType()),
                        getNamespaceHandler().globalFromLocalEntityId(toId.getNamespace(), toId.getNativeId(), toId.getType()),
						true, null, null, properties);
				links.add(link);
			}
			rs.close();
		}
		
		return links;
	}

	private List<FL_Link> fetchTransactionsPrepared(
		PreparedStatement stmt, 
		List<String> entities, 
		FL_SortBy sort, 
		FL_TransactionResults.Builder results
	) throws SQLException {
		
		final List<FL_Link> links = new ArrayList<FL_Link>();
		
		String fromColumn = unescapeName(getNamespaceHandler().columnName(FINANCIALS_FROM));
		String toColumn = unescapeName(getNamespaceHandler().columnName(FINANCIALS_TO));
		String dateColumn = unescapeName(getNamespaceHandler().columnName(FINANCIALS_DATE));
		String loanIdColumn = unescapeName(getNamespaceHandler().columnName(FINANCIALS_LOANID));
		String amountColumn = unescapeName(getNamespaceHandler().columnName(FINANCIALS_AMOUNT));
		String commentColumn = unescapeName(getNamespaceHandler().columnName(FINANCIALS_COMMENT));
		
		ResultSet rs = stmt.executeQuery();
		while (rs.next()) {
			String from = rs.getString(fromColumn);
			
			if (from == null) {
				results.setTotal(rs.getLong(loanIdColumn));
				
				continue;
			}
			
			String to = rs.getString(toColumn);
			Date date = new Date(rs.getTimestamp(dateColumn).getTime());
			Double amount = rs.getDouble(amountColumn);
			String comment = rs.getString(commentColumn);
			
			Double credit = checkEntitiesContainsEntity(entities, to) ? amount : 0.0;
			Double debit = checkEntitiesContainsEntity(entities, from) ? amount : 0.0;

			// drop the postfix if we're sorting by amount so that linklist only has 1 value
			// could probably generalize this but let's be conservative for safety's sake
			if(FL_SortBy.AMOUNT == sort) {		
				if(from.indexOf('-') != -1) {
					from = from.substring(0, from.indexOf('-'));
				}
				if(to.indexOf('-') != -1) {
					to = to.substring(0, to.indexOf('-'));
				}
			}
			
			// do some aesthetic enhancements on the comments field; this can be removed if the source tables 
			// are cleaned or properly regenerated in the future
			comment = formatCommentString(comment);
			
			List<FL_Property> properties = new ArrayList<FL_Property>();
			properties.add(
				new PropertyHelper(
					"inflowing", 
					"inflowing", 
					credit, 
					Arrays.asList(
						FL_PropertyTag.INFLOWING, 
						FL_PropertyTag.AMOUNT, 
						FL_PropertyTag.USD
					)
				)
			);
			properties.add(
				new PropertyHelper(
					"outflowing", 
					"outflowing", 
					debit, 
					Arrays.asList(
						FL_PropertyTag.OUTFLOWING, 
						FL_PropertyTag.AMOUNT, 
						FL_PropertyTag.USD
					)
				)
			);
			properties.add(
				new PropertyHelper(
					"comment", 
					"comment", 
					comment, 
					Collections.singletonList(
						FL_PropertyTag.ANNOTATION
					)
				)
			);
			properties.add(
				new PropertyHelper(
					FL_PropertyTag.DATE, 
					date
				)
			);
			properties.add(
				new PropertyHelper(
					FL_PropertyTag.ID, 
					rs.getString(loanIdColumn)
				)
			);

            TypedId fromId = TypedId.fromNativeId(TypedId.ACCOUNT, from);
            TypedId toId = TypedId.fromNativeId(TypedId.ACCOUNT, to);
            FL_Link link = new FL_Link(Collections.singletonList(FL_LinkTag.FINANCIAL),
                    getNamespaceHandler().globalFromLocalEntityId(fromId.getNamespace(), fromId.getNativeId(), fromId.getType()),
                    getNamespaceHandler().globalFromLocalEntityId(toId.getNamespace(), toId.getNativeId(), toId.getType()),
                    true, null, null, properties);
			links.add(link);
		}
		rs.close();
		
		return links;
	}
	
	
	
	
	private String unescapeName(String name) {

		String unescapedName = name.replace("[", "");
		unescapedName = unescapedName.replace("]", "");
		
		return unescapedName;
	}
	
	private String buildTransactionSQL(
			List<String> entities, 
			boolean summaries, 
			String tableName, 
			DateTime startDate, 
			DateTime endDate, 
			List<String> linkFilter, 
			FL_SortBy sort, 
			long max
	) {
		
		String fromColumn = getNamespaceHandler().columnName(FINANCIALS_FROM);
		String toColumn = getNamespaceHandler().columnName(FINANCIALS_TO);
		String dateColumn = getNamespaceHandler().columnName(FINANCIALS_DATE);
		String loanIdColumn = getNamespaceHandler().columnName(FINANCIALS_LOANID);
		String amountColumn = getNamespaceHandler().columnName(FINANCIALS_AMOUNT);
		String commentColumn = getNamespaceHandler().columnName(FINANCIALS_COMMENT);
		
		String orderBy = "";
		if (sort != null) {
			switch (sort) {
			case AMOUNT:
				orderBy = " ORDER BY Amount DESC";
				break;
			case DATE:
				orderBy = " ORDER BY " + dateColumn + " ASC";
				break;
			}
		}
		
		String focusIds = "";
		if (linkFilter != null) {
			focusIds = DataAccessHelper.createNodeIdListFromCollection(linkFilter);
		}
		
		String selector = "";
		
		if (summaries) {
			String membersTable = getNamespaceHandler().tableName(null, DataAccessHelper.CLUSTER_SUMMARY_MEMBERS_TABLE);
			String membersEntityIdColumn = getNamespaceHandler().columnName(DataAccessHelper.CLUSTER_SUMMARY_MEMBERS_COLUMN_ENTITY_ID);

			String fromIds = buildSearchIdsStringNoPrepared("SummaryId", entities);
			String toIds = buildSearchIdsStringNoPrepared("SummaryId", entities);
			String fromFocus = buildSearchIdsStringNoPrepared(fromColumn, linkFilter);
			String toFocus = buildSearchIdsStringNoPrepared(toColumn, linkFilter);
			
			selector = " from " +tableName+ " f join " +membersTable+ " m on f."+fromColumn + " = m." + membersEntityIdColumn + " or f."+toColumn + " = m." + membersEntityIdColumn + " " +
					" where f." + dateColumn + " between '"+DataAccessHelper.format(startDate)+"' and '"+DataAccessHelper.format(endDate)+
					"' and ((" + fromIds + (focusIds.isEmpty() ? "" : " and " + toFocus) +
					") or (" + toIds + (focusIds.isEmpty() ? "" : " and " + fromFocus) +
					")) ";
			
			return "select " + fromColumn + "," + toColumn + "," + dateColumn + ", " + amountColumn + ", " + commentColumn + ", " + loanIdColumn + " from ("+
					getNamespaceHandler().rowLimit("*"+ selector + orderBy, max) + ") a";
		} else {
			String fromIds = buildSearchIdsStringNoPrepared(fromColumn, entities);
			String toIds = buildSearchIdsStringNoPrepared(toColumn, entities);
			String fromFocus = buildSearchIdsStringNoPrepared(fromColumn, linkFilter);
			String toFocus = buildSearchIdsStringNoPrepared(toColumn, linkFilter);
		
			selector = " from " +tableName+ 
				" where " + dateColumn + " between '"+DataAccessHelper.format(startDate)+"' and '"+DataAccessHelper.format(endDate)+
				"' and ((" + fromIds + (focusIds.isEmpty() ? "" : " and " + toFocus) +
				") or (" + toIds + (focusIds.isEmpty() ? "" : " and " + fromFocus) +
				")) ";
			
			return "select " + fromColumn + "," + toColumn + "," + dateColumn + ", " + amountColumn + ", " + commentColumn + ", " + loanIdColumn + " from ("+
						getNamespaceHandler().rowLimit("*"+ selector + orderBy, max)
						+ ") a union all select NULL,NULL,NULL,NULL,NULL,COUNT(*)" + selector;
		}
	}	
		
	private String buildTransactionPreparedStatementString(
		List<String> entities, 
		boolean summaries, 
		String tableName, 
		List<String> linkFilter, 
		FL_SortBy sort, 
		long max
	) {
		
		String fromColumn = getNamespaceHandler().columnName(FINANCIALS_FROM);
		String toColumn = getNamespaceHandler().columnName(FINANCIALS_TO);
		String dateColumn = getNamespaceHandler().columnName(FINANCIALS_DATE);
		String loanIdColumn = getNamespaceHandler().columnName(FINANCIALS_LOANID);
		String amountColumn = getNamespaceHandler().columnName(FINANCIALS_AMOUNT);
		String commentColumn = getNamespaceHandler().columnName(FINANCIALS_COMMENT);
		
		String orderBy = "";
		if (sort != null) {
			switch (sort) {
			case AMOUNT:
				orderBy = "ORDER BY Amount DESC ";
				break;
			case DATE:
				orderBy = "ORDER BY " + dateColumn + " ASC ";
				break;
			}
		}
		
		if (summaries) {
			String membersTable = getNamespaceHandler().tableName(null, DataAccessHelper.CLUSTER_SUMMARY_MEMBERS_TABLE);
			String membersEntityIdColumn = getNamespaceHandler().columnName(DataAccessHelper.CLUSTER_SUMMARY_MEMBERS_COLUMN_ENTITY_ID);

			String fromIds = buildSearchIdsStringPrepared("SummaryId", entities);
			String toIds = buildSearchIdsStringPrepared("SummaryId", entities);
			String fromFocus = buildSearchIdsStringPrepared(fromColumn, linkFilter);
			String toFocus = buildSearchIdsStringPrepared(toColumn, linkFilter);
			
			StringBuilder selectorBuilder = new StringBuilder();
			
			selectorBuilder.append("FROM " + tableName + " f ");
			selectorBuilder.append("JOIN " + membersTable + " m ");
			selectorBuilder.append("ON f." + fromColumn + " = m." + membersEntityIdColumn + " ");
			selectorBuilder.append("OR f." + toColumn + " = m." + membersEntityIdColumn + " ");
			selectorBuilder.append("WHERE f." + dateColumn + " BETWEEN ? AND ? ");
			selectorBuilder.append("AND (( ");
			selectorBuilder.append(fromIds + " ");
			if (linkFilter != null && !linkFilter.isEmpty()) {
				selectorBuilder.append("AND ");
				selectorBuilder.append(toFocus + " ");
			}
			selectorBuilder.append(") OR ( ");
			selectorBuilder.append(toIds + " ");
			if (linkFilter != null && !linkFilter.isEmpty()) {
				selectorBuilder.append("AND ");
				selectorBuilder.append(fromFocus + " ");
			}		
			selectorBuilder.append(")) ");	
			
			StringBuilder sb = new StringBuilder();
			
			sb.append("SELECT ");
			sb.append(fromColumn + ", ");
			sb.append(toColumn + ", ");
			sb.append(dateColumn + ", ");
			sb.append(amountColumn + ", ");
			sb.append(commentColumn + ", ");
			sb.append(loanIdColumn + " ");
			sb.append("FROM (");
			sb.append(
				getNamespaceHandler().rowLimit(
					"* " + selectorBuilder.toString() + orderBy, 
					max
				)
			);
			sb.append(") a ");
			
			return sb.toString();
			
		} else {
			
			String fromIds = buildSearchIdsStringPrepared(fromColumn, entities);
			String toIds = buildSearchIdsStringPrepared(toColumn, entities);
			String fromFocus = buildSearchIdsStringPrepared(fromColumn, linkFilter);
			String toFocus = buildSearchIdsStringPrepared(toColumn, linkFilter);

			StringBuilder selectorBuilder = new StringBuilder();
			
			selectorBuilder.append("FROM " + tableName + " ");
			selectorBuilder.append("WHERE " + dateColumn + " BETWEEN ? AND ? ");
			selectorBuilder.append("AND (( ");
			selectorBuilder.append(fromIds + " ");
			if (linkFilter != null && !linkFilter.isEmpty()) {
				selectorBuilder.append("AND ");
				selectorBuilder.append(toFocus + " ");
			}
			selectorBuilder.append(") OR ( ");
			selectorBuilder.append(toIds + " ");
			if (linkFilter != null && !linkFilter.isEmpty()) {
				selectorBuilder.append("AND ");
				selectorBuilder.append(fromFocus + " ");
			}		
			selectorBuilder.append(")) ");	
			
			StringBuilder sb = new StringBuilder();
			
			sb.append("SELECT ");
			sb.append(fromColumn + ", ");
			sb.append(toColumn + ", ");
			sb.append(dateColumn + ", ");
			sb.append(amountColumn + ", ");
			sb.append(commentColumn + ", ");
			sb.append(loanIdColumn + " ");
			sb.append("FROM (");
			sb.append(
				getNamespaceHandler().rowLimit(
					"* " + selectorBuilder.toString() + orderBy, 
					max
				)
			);
			sb.append(") a ");
			sb.append("UNION ALL SELECT NULL, NULL, NULL, NULL, NULL, COUNT(*) ");
			sb.append(selectorBuilder.toString());
			
			return sb.toString();
		}
	}
	
	
	
	
	private void setTransactionPreparedStatement(
		PreparedStatement stmt,
		boolean summaries, 
		List<String> ids, 
		List<String> focusIds, 
		DateTime startDate,
		DateTime endDate
	) throws SQLException {
		int index = 1;
		
		stmt.setString(index++, DataAccessHelper.format(startDate));
		stmt.setString(index++, DataAccessHelper.format(endDate));
		
		index = setSearchIdsString(stmt, ids, index);
		if (focusIds != null && !focusIds.isEmpty()) {
			index = setSearchIdsString(stmt, focusIds, index);
		}
		index = setSearchIdsString(stmt, ids, index);
		if (focusIds != null && !focusIds.isEmpty()) {
			index = setSearchIdsString(stmt, focusIds, index);
		}
		
		if (!summaries) {
			stmt.setString(index++, DataAccessHelper.format(startDate));
			stmt.setString(index++, DataAccessHelper.format(endDate));
			
			index = setSearchIdsString(stmt, ids, index);
			if (focusIds != null && !focusIds.isEmpty()) {
				index = setSearchIdsString(stmt, focusIds, index);
			}
			index = setSearchIdsString(stmt, ids, index);
			if (focusIds != null && !focusIds.isEmpty()) {
				index = setSearchIdsString(stmt, focusIds, index);
			}
		}
	}
	
	
	private List<String> getSummaryEntitiesNoPrepared(
			Statement stmt, 
			List<String> entities
	) throws SQLException {
		
		final List<String> summaryEntityIds = new ArrayList<String>();
		
		if (entities.isEmpty()) return summaryEntityIds;
		
		String membersTable = getNamespaceHandler().tableName(null, DataAccessHelper.CLUSTER_SUMMARY_MEMBERS_TABLE);
		String membersEntityIdColumn = getNamespaceHandler().columnName(DataAccessHelper.CLUSTER_SUMMARY_MEMBERS_COLUMN_ENTITY_ID);

		String fromIds = buildSearchIdsStringNoPrepared("SummaryId", entities);
		
		String sql = "select " + membersEntityIdColumn + " from " + membersTable + " where " + fromIds;
		
		if (stmt.execute(sql)) {
			ResultSet rs = stmt.getResultSet();
			while (rs.next()) {
				String id = rs.getString(membersEntityIdColumn);
				summaryEntityIds.add(id);
			}
			rs.close();
		}
		
		return summaryEntityIds;
	}	
	
	private List<String> getSummaryEntitiesPrepared(
		Connection connection, 
		List<String> entities
	) throws SQLException {
		
		final List<String> summaryEntityIds = new ArrayList<String>();
		
		if (entities.isEmpty()) return summaryEntityIds;
		
		String membersTable = getNamespaceHandler().tableName(null, DataAccessHelper.CLUSTER_SUMMARY_MEMBERS_TABLE);
		String membersEntityIdColumn = getNamespaceHandler().columnName(DataAccessHelper.CLUSTER_SUMMARY_MEMBERS_COLUMN_ENTITY_ID);

		String fromIds = buildSearchIdsStringPrepared("SummaryId", entities);
		
		PreparedStatement stmt = connection.prepareStatement(
			"SELECT " + membersEntityIdColumn + " FROM " + 
			membersTable + " " +
			"WHERE " +
			fromIds
		);
		
		setSearchIdsString(stmt, entities, 1);
		
		ResultSet rs = stmt.executeQuery();
		while (rs.next()) {
			String id = rs.getString(membersEntityIdColumn);
			summaryEntityIds.add(id);
		}
		rs.close();
		stmt.close();
		
		return summaryEntityIds;
	}




	private boolean checkEntitiesContainsEntity(
		List<String> entities, 
		String entity
	) {
		
		if (entities.contains(entity)) {
			return true;
		}
		
		// check for full partners against brokers
		if (entity.startsWith("p") && entity.contains("-")) {
			String fullPartner = entity.split("-")[0];
			if (entities.contains(fullPartner)) {
				return true;
			}
		}
		
		return false;
	}
	
	
	private String buildSearchIdsStringNoPrepared(
			String column, 
			List<String> ids
	) {
		
		if (ids == null) {
			return "1=1";
		}
		
		List<String> ins = new ArrayList<String>();
		List<String> likes = new ArrayList<String>();
		for (String id : ids) {
			if (id.startsWith("p")) {
				if (id.contains("-")) {
					ins.add(id);
				} else {
					likes.add(id + "-%");
				}
			} else {
				ins.add(id);
			}
		}
		
		Boolean needsOr = false;
		
		StringBuilder str = new StringBuilder();
		str.append("(");
		if (ins.size() > 0) {
			for (int i = 0; i < ins.size() - 1; i++) {
				str.append(column + " = '" + ins.get(i) + "'");
				str.append(" or ");
			}
			str.append(column + " = '" + ins.get(ins.size() - 1) + "'");
			needsOr = true;
		}

		if (likes.size() > 0) {
			
			if (needsOr) {
				str.append(" or ");
			}
			
			for (int i = 0; i < likes.size() - 1; i++) {
				str.append(column + " like '" + likes.get(i) + "'");
				str.append(" or ");
			}
			str.append(column + " like '" + likes.get(likes.size() - 1) + "'");
		}
		
		str.append(")");
		
		return str.toString();
	}
		
	
	private String buildSearchIdsStringPrepared(
			String column, 
			List<String> ids
	) {
		
		if (ids == null) {
			return "1=1";
		}
		
		int inEntities = 0;
		int likeEntities = 0;
		for (String id : ids) {
			if (id.startsWith("p")) {
				if (id.contains("-")) {
					inEntities++;
				} else {
					likeEntities++;
				}
			} else {
				inEntities++;
			}
		}
		
		Boolean needsOr = false;
		
		StringBuilder str = new StringBuilder();
		str.append("( ");
		if (inEntities > 0) {
			for (int i = 0; i < inEntities - 1; i++) {
				str.append(column + " = ? ");
				str.append("OR ");
			}
			str.append(column + " = ? ");
			needsOr = true;
		}

		if (likeEntities > 0) {
			
			if (needsOr) {
				str.append("OR ");
			}
			
			for (int i = 0; i < likeEntities - 1; i++) {
				str.append(column + " LIKE ? ");
				str.append("OR ");
			}
			str.append(column + " LIKE ? ");
		}
		
		str.append(")");
		
		return str.toString();
	}
	
	
	
	
	private int setSearchIdsString(
		PreparedStatement stmt,
		List<String> ids, 
		int startIndex
	) throws SQLException {
		
		int index = startIndex;
		
		
		List<String> ins = new ArrayList<String>();
		List<String> likes = new ArrayList<String>();
		for (String id : ids) {
			if (id.startsWith("p")) {
				if (id.contains("-")) {
					ins.add(id);
				} else {
					likes.add(id + "-%");
				}
			} else {
				ins.add(id);
			}
		}
		
		if (!ins.isEmpty()) {
			for (int i = 0; i < ins.size(); i++) {
				stmt.setString(index++, ins.get(i));
			}
		}
		
		if (!likes.isEmpty()) {
			for (int i = 0; i < likes.size(); i++) {
				stmt.setString(index++, likes.get(i));
			}
		}
		
		return index;
	}
	
	
	
	
	// clean up bad comments - should have been done properly in the first place
	private static String formatCommentString(String comment) {
		
		if (comment == null) return "";
		
		if(comment.contains("e+") || comment.contains("000")) {
			int plusIdx = comment.indexOf('+');			// need to remove + character string so that stringRep won't implode
			if(plusIdx != -1) {
				comment = comment.substring(0, plusIdx) + comment.substring(plusIdx+1);
			}

			String[] fields = comment.split("\\s+");
			if(fields.length > 3 && fields[3] != null &&
					(fields[3].contains("e") || fields[3].contains("000"))) {								
				comment = comment.replaceFirst(fields[3],  monetaryFormat(fields[3]));
			}
		}
		
		
		int startIndex = comment.indexOf("(");
		int endIndex = comment.indexOf(")");
		
		if (startIndex>-1 && endIndex>-1) {
			String currencyComment = comment.substring(startIndex + 1, endIndex);
			
			String[] cur = currencyComment.split(" in ");
			
			if (cur.length > 1) {
			
				Double val = Double.parseDouble(cur[0]);		
				return comment.substring(0, startIndex) + "(" + world_df.format(val) + " in " + cur[1] + ")";
			}
			
			try {
				Double val = Double.parseDouble(cur[0]);
				return comment.substring(0, startIndex) + "(" + world_df.format(val) + ")";
			} catch (NumberFormatException e) {
				// do nothing
			}
		}
		
		return comment;
	}
	
	
	
	
	private static final String monetaryFormat(final String toFormat) {
		String toReturn = toFormat;
		
		try {
			double dblValue = new BigDecimal(toFormat).doubleValue();
			toReturn = new DecimalFormat("##0E0").format(dblValue);
			toReturn = toReturn.replaceAll("E[0-9]*", MONETARY_SUFFIX[((int) Math.log10(dblValue))/3]);
		}
		catch(NumberFormatException nfe) { toReturn = toFormat; }
		catch(IllegalArgumentException iae) { toReturn = toFormat; }
		catch(ArithmeticException ae) { toReturn = toFormat; }
		
		return toReturn;
	}
}
