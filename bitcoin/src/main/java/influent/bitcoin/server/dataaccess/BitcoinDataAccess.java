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
package influent.bitcoin.server.dataaccess;

import influent.idl.FL_DataAccess;
import influent.idl.FL_DateRange;
import influent.idl.FL_EntitySearch;
import influent.idl.FL_Link;
import influent.idl.FL_LinkTag;
import influent.idl.FL_Property;
import influent.idl.FL_PropertyTag;
import influent.idl.FL_SortBy;
import influent.idl.FL_Entity;
import influent.idl.FL_TransactionResults;
import influent.idl.FL_EntityTag;
import influent.idl.FL_LevelOfDetail;
import influent.idlhelper.EntityHelper;
import influent.idlhelper.LinkHelper;
import influent.idlhelper.PropertyHelper;
import influent.server.dataaccess.DataAccessHelper;
import influent.server.dataaccess.DataNamespaceHandler;
import influent.server.dataaccess.DataViewDataAccess;
import influent.server.utilities.SQLConnectionPool;
import influent.server.utilities.TypedId;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.List;
import java.util.LinkedList;

import org.apache.avro.AvroRemoteException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hopefully a better implementation of the Data Access
 * @author msavigny
 *
 */

public class BitcoinDataAccess extends DataViewDataAccess implements FL_DataAccess {
	
	
	private static final Logger s_logger = LoggerFactory.getLogger(BitcoinDataAccess.class);

	private final DataNamespaceHandler _namespaceHandler;

	public BitcoinDataAccess(
			SQLConnectionPool connectionPool,
			FL_EntitySearch search,
			DataNamespaceHandler namespaceHandler
	) throws ClassNotFoundException, SQLException {

		super(connectionPool, search, namespaceHandler);
		_namespaceHandler = namespaceHandler;
	}
	
	
	
	
	public FL_TransactionResults getAllTransactions(
			List<String> entities,
			FL_LinkTag tag,
			FL_DateRange date,
			FL_SortBy sort,
			List<String> linkFilter,
			long start,
			long max
	) throws AvroRemoteException {

		if (entities.size() == 1) {
			return getAllTransactionsForEntity(entities.get(0), tag, date, sort, max, linkFilter);
		}

		long count = 0;
		final List<FL_Link> links = new ArrayList<FL_Link>();

		for (String id : entities) {
			FL_TransactionResults results = getAllTransactionsForEntity(id, tag, date, sort, max, linkFilter);

			count += results.getTotal();
			links.addAll(results.getResults());
		}

		return FL_TransactionResults.newBuilder().setTotal(count).setResults(links).build();
	}


	private FL_TransactionResults getAllTransactionsForEntity(
			String id,
			FL_LinkTag tag,
			FL_DateRange date,
			FL_SortBy sort,
			long max,
			List<String> linkFilter
	) throws AvroRemoteException {
		Integer iid = null;
		String schema = null;

		try {
			// translate global to local ids, with namespace
			schema = getNamespaceHandler().namespaceFromGlobalEntityId(id);
			id = getNamespaceHandler().localFromGlobalEntityId(id);
			iid = Integer.valueOf(id);

		} catch (Exception e) {
			s_logger.warn("Failed to convert id to number: " + id);
		}

		if (iid == null) {
			return new FL_TransactionResults(0L, new ArrayList<FL_Link>(0));
		}

		try {
			Connection connection = _connectionPool.getConnection();

			String top = max > 0 ? ("top " + max) : "";

			List<Integer> linkFilterIds = null;
			if (linkFilter != null) {
				// we will never find links across schemas, so filter it down again.
				linkFilter = getNamespaceHandler().entitiesByNamespace(linkFilter).get(schema);

				if (linkFilter == null) {
					return new FL_TransactionResults(0L, new ArrayList<FL_Link>(0));
				}

				linkFilterIds = new ArrayList<Integer>(linkFilter.size());
				for (String lid : linkFilter) {
					try {
						linkFilterIds.add(Integer.valueOf(lid));
					} catch (Exception e) {
						s_logger.warn("Failed to convert linked id to number: " + lid);
					}
				}
			}

			String preparedStatementString = buildPreparedStatement(
					(linkFilter == null) ? 0 : linkFilterIds.size(),
					top,
					(date != null && date.getStartDate() != null),
					sort
			);
			PreparedStatement stmt = connection.prepareStatement(preparedStatementString);

			int index = 1;

			stmt.setInt(index++, iid);
			if (linkFilterIds != null) {
				for (Integer lid : linkFilterIds) {
					stmt.setInt(index++, lid);
				}
			}
			stmt.setInt(index++, iid);
			if (linkFilterIds != null) {
				for (Integer lid : linkFilterIds) {
					stmt.setInt(index++, lid);
				}
			}
			stmt.setString(index++, DataAccessHelper.format(DataAccessHelper.getStartDate(date)));
			stmt.setString(index++, DataAccessHelper.format(DataAccessHelper.getEndDate(date)));

			List<FL_Link> records = new ArrayList<FL_Link>();
			ResultSet resultSet = stmt.executeQuery();

			while (resultSet.next()) {
				String trans_id = resultSet.getString(1);
				String source_id = resultSet.getString(2);
				String dest_id = resultSet.getString(3);
				Date dt = new java.util.Date(resultSet.getTimestamp(4).getTime());
				Double btc = resultSet.getDouble(5);
				Double usd = resultSet.getDouble(6);

				List<FL_Property> props = new ArrayList<FL_Property>();
				props.add(new PropertyHelper(FL_PropertyTag.ID, trans_id));
				props.add(new PropertyHelper("dt", "dt", dt, Collections.singletonList(FL_PropertyTag.DATE)));
				props.add(new PropertyHelper("amount", "amount", usd, Collections.singletonList(id.equals(source_id) ? FL_PropertyTag.OUTFLOWING : FL_PropertyTag.INFLOWING)));

				String comment = (btc + " BTC ") + ((id.equals(source_id)) ? ("to " + dest_id) : ("from " + source_id));
				props.add(new PropertyHelper("comment", "comment", comment, Collections.singletonList(FL_PropertyTag.ANNOTATION)));

				// globalize these for return
				source_id = getNamespaceHandler().globalFromLocalEntityId(schema, source_id, TypedId.ACCOUNT);
				dest_id = getNamespaceHandler().globalFromLocalEntityId(schema, dest_id, TypedId.ACCOUNT);

				records.add(new LinkHelper(FL_LinkTag.FINANCIAL, source_id, dest_id, props));
			}

			resultSet.close();
			stmt.close();
			connection.close();

			return new FL_TransactionResults((long) records.size(), records);

		} catch (SQLException e) {
			throw new AvroRemoteException(e);
		} catch (ClassNotFoundException e) {
			throw new AvroRemoteException(e);
		}
	}


	private String buildPreparedStatement(
			int numFocusIds,
			String top,
			boolean hasDate,
			FL_SortBy sort
	) {

		StringBuilder sb = new StringBuilder();

		sb.append("SELECT " + top + " TxId, SenderId, ReceiverId, TxTime, BTC, USD ");
		sb.append("FROM Bitcoin.dbo.UserTransactions ");
		sb.append("WHERE ((SenderId = ?");
		if (numFocusIds > 0) {
			sb.append(" AND ReceiverId IN (");
			for (int i = 1; i < numFocusIds; i++) {
				sb.append("?, ");
			}
			sb.append("?)");
		}
		sb.append(") ");
		sb.append("OR (ReceiverId = ?");
		if (numFocusIds > 0) {
			sb.append(" AND SenderId IN (");
			for (int i = 1; i < numFocusIds; i++) {
				sb.append("?, ");
			}
			sb.append("?)");
		}
		sb.append(")) ");

		if (hasDate) {
			sb.append("AND TxTime BETWEEN ? AND ? ");
		}

		if (sort != null) {
			sb.append("ORDER BY ");
			sb.append((sort == FL_SortBy.DATE ? "TxTime ASC" : "USD DESC"));
		}

		return sb.toString();
	}


	@Override
	public List<FL_Entity> getEntities(
			List<String> entities,
			FL_LevelOfDetail levelOfDetail
	) throws AvroRemoteException {
		List<FL_Entity> results = new LinkedList<FL_Entity>();


		if (entities == null || entities.isEmpty()) return results;

		Map<String, List<String>> bySchema = _namespaceHandler.entitiesByNamespace(entities);

		try {

			Connection connection = _connectionPool.getConnection();

			for (Map.Entry<String, List<String>> entry : bySchema.entrySet()) {
				if (entry.getValue() == null || entry.getValue().isEmpty()) {
					continue;
				}

				// process entities in batches
				List<String> idsCopy = new ArrayList<String>(entry.getValue()); // copy the ids as we will take 1000 at a time to process and the take method is destructive
				while (idsCopy.size() > 0) {
					List<String> tempSubList = (idsCopy.size() > 1000) ? idsCopy.subList(0, 999) : idsCopy; // get the next 1000
					List<String> subIds = new ArrayList<String>(tempSubList); // copy as the next step is destructive
					tempSubList.clear(); // this clears the IDs from idsCopy as tempSubList is backed by idsCopy

					StringBuilder sb = new StringBuilder();
					sb.append("SELECT EntityId, Label, InboundDegree, UniqueInboundDegree, OutboundDegree, UniqueOutboundDegree ");
					sb.append("FROM Bitcoin." + entry.getKey() + ".FinEntity ");
					sb.append("WHERE EntityId IN (");
					for (int i = 1; i < subIds.size(); i++) {
						sb.append("?, ");
					}
					sb.append("?) ");
					PreparedStatement stmt = connection.prepareStatement(sb.toString());
					int index = 1;

					for (int i = 0; i < subIds.size(); i++) {
						stmt.setString(index++, getNamespaceHandler().toSQLId(subIds.get(i), entry.getKey()));
					}

					// Execute prepared statement and evaluate results
					ResultSet rs = stmt.executeQuery();
					while (rs.next()) {
						Integer entityId = rs.getInt(1);
						String label = rs.getString(2);
						int inboundDegree = rs.getInt(3);
						int uniqueInboundDegree = rs.getInt(4);
						int outboundDegree = rs.getInt(5);
						int uniqueOutboundDegree = rs.getInt(6);

						String uid = _namespaceHandler.globalFromLocalEntityId(entry.getKey(), entityId.toString(), TypedId.ACCOUNT);
						boolean labeled = label != null && !label.trim().isEmpty();

						List<FL_Property> props = new ArrayList<FL_Property>();
						props.add(new PropertyHelper("InboundDegree", "Inbound Transfers", inboundDegree, Collections.singletonList(FL_PropertyTag.STAT)));
						props.add(new PropertyHelper("UniqueInboundDegree", "Unique Inbound Links", uniqueInboundDegree, Collections.singletonList(FL_PropertyTag.INFLOWING)));
						props.add(new PropertyHelper("OutboundDegree", "Outbound Transfers", outboundDegree, Collections.singletonList(FL_PropertyTag.STAT)));
						props.add(new PropertyHelper("UniqueOutboundDegree", "Unique Outbound Links", uniqueOutboundDegree, Collections.singletonList(FL_PropertyTag.OUTFLOWING)));
						props.add(new PropertyHelper("Identification", "Identification", labeled ? "Tagged" : "Anonymous", Collections.singletonList(FL_PropertyTag.TYPE)));
						props.add(new PropertyHelper("image", "Image", "img/bitcoin_default.png", Collections.singletonList(FL_PropertyTag.IMAGE)));

						if (labeled) {
							props.add(new PropertyHelper("UserTag", "User Tag", label, Collections.singletonList(FL_PropertyTag.TEXT)));
						} else {
							label = entityId.toString();
						}


						FL_Entity entity = new EntityHelper(uid, label, FL_EntityTag.ACCOUNT.name(), FL_EntityTag.ACCOUNT, props);

						results.add(entity);
					}
					rs.close();

					// Close prepared statement
					stmt.close();

				}
			}

			connection.close();
			return results;

		} catch (Exception e) {
			throw new AvroRemoteException(e);
		}

	}
}
