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
package influent.kiva.server.search;

import influent.idl.FL_Entity;
import influent.idl.FL_EntitySearch;
import influent.idl.FL_EntityTag;
import influent.idl.FL_Property;
import influent.idl.FL_TypeMapping;
import influent.idl.FL_PropertyMatchDescriptor;
import influent.idl.FL_PropertyTag;
import influent.idl.FL_PropertyType;
import influent.idl.FL_SingletonRange;
import influent.idl.FL_ListRange;
import influent.idl.FL_SearchResult;
import influent.idl.FL_SearchResults;
import influent.idl.FL_TypeDescriptor;
import influent.idl.FL_Geocoding;
import influent.idl.FL_PropertyDescriptors;

import influent.idlhelper.PropertyHelper;
import influent.idlhelper.SolrUtils;
import influent.midtier.solr.search.ConfigFileDescriptors;
import influent.server.dataaccess.DataAccessHelper;
import influent.server.dataaccess.DataNamespaceHandler;
import influent.server.utilities.SQLConnectionPool;
import influent.server.utilities.TypedId;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import oculus.aperture.spi.common.Properties;

import org.apache.avro.AvroRemoteException;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KivaEntitySearch implements FL_EntitySearch {

	private static final Logger s_logger = LoggerFactory.getLogger(KivaEntitySearch.class);

	private final FL_Geocoding _geocoding;
	private String _solrURL;
	private SolrServer _solr;
	private Properties _config;
	private final SQLConnectionPool _connectionPool;
	private final DataNamespaceHandler _namespaceHandler;

	private final ConfigFileDescriptors _cfd = new ConfigFileDescriptors();

	public KivaEntitySearch(
			String solrURL,
			String solrDescriptor,
			Properties config,
			FL_Geocoding geocoding,
			SQLConnectionPool connectionPool,
			DataNamespaceHandler namespaceHandler
	) {
		_solrURL = solrURL;
		_solr = new HttpSolrServer(_solrURL);
		_config = config;
		_geocoding = geocoding;
		_connectionPool = connectionPool;
		_namespaceHandler = namespaceHandler;

		try {
			_cfd.readDescriptorsFromFile(solrDescriptor);
		} catch (IOException e) {
			getLogger().warn("Exception reading entity descriptors " + e.getMessage(), e);
		}

	}


	protected Logger getLogger() {
		return s_logger;
	}


	protected DataNamespaceHandler getNamespaceHandler() {
		return _namespaceHandler;
	}


	private FL_PropertyMatchDescriptor filterIds(
			FL_PropertyMatchDescriptor pst,
			Map<String, List<String>> brokerIds
	) throws AvroRemoteException {

		final String k = pst.getKey();

		// we use 'uid' for influent unique ids and the data native 'id' on the client. Accept both for broker filtering.
		if (k.equals("uid") || k.equals("id")) {

			Collection<Object> values;

			final Object r = pst.getRange();

			if (r instanceof FL_SingletonRange) {
				values = Collections.singleton(((FL_SingletonRange) r).getValue());
			} else if (r instanceof FL_ListRange) {
				values = ((FL_ListRange) r).getValues();
			} else {
				return pst;
			}

			// filtered list
			List<Object> filtered = new ArrayList<Object>(values.size());

			for (Object obj : values) {
				final String id = k.equals("id") ? String.valueOf(obj) :
						TypedId.fromTypedId(String.valueOf(obj)).getNativeId();

				if (id != null && id.charAt(0) == 'p') {
					final int idash = id.indexOf('-');

					if (idash != -1) {
						final String pid = id.substring(0, idash);

						List<String> realIds = brokerIds.get(pid);

						// don't add the partner as a criteria more than once
						if (realIds != null) {
							realIds.add(id);

							continue;
						}

						filtered.add(pid);

						realIds = new ArrayList<String>();
						realIds.add(id);
						brokerIds.put(pid, realIds);

						continue;
					}
				}

				filtered.add(id);
			}

			if (pst.getTypeMappings().isEmpty()) {

				// TODO: Currently uids are matched as ids against all types
				List<FL_TypeMapping> typeMappings = new ArrayList<FL_TypeMapping>();
				for (FL_TypeDescriptor td : getDescriptors().getTypes()) {
					typeMappings.add(FL_TypeMapping.newBuilder()
						.setType(td.getKey())
						.setMemberKey("id")
						.build());
				}
				pst.setTypeMappings(typeMappings);
			}

			return FL_PropertyMatchDescriptor.newBuilder(pst)
					.setKey("id")
					.setRange(FL_ListRange.newBuilder()
							.setType(FL_PropertyType.STRING)
							.setValues(filtered)
							.build())
					.build();
		}

		return pst;
	}



	@Override
	public FL_SearchResults search(
		List<FL_PropertyMatchDescriptor> termList,
		long start, 
		long max
	) throws AvroRemoteException {

		// special partner handling. lookup broker partners by partner id, then map them back afterwards
		Map<String, List<String>> brokerIds = new HashMap<String, List<String>>();
		final List<FL_PropertyMatchDescriptor> filteredTerms = new ArrayList<FL_PropertyMatchDescriptor>();

		if (termList != null) {

			for (FL_PropertyMatchDescriptor term : termList) {

				final FL_PropertyMatchDescriptor flpmd = filterIds(term, brokerIds);

				// Only include successfully filtered ids
				if (flpmd != null) {
					filteredTerms.add(flpmd);
				}
			}
		}

		// form the query
		final String searchStr = SolrUtils.toSolrQuery( filteredTerms );
	
		// issue the query.
		getLogger().info("Issuing Solr Query " + searchStr);
		
		SolrQuery solrQuery = new SolrQuery();
		solrQuery.setQuery(searchStr);
		solrQuery.setFields("*", "score");
		
		KivaEntitySearchIterator ssr = buildKivaEntitySearchIterator(_solr, solrQuery, _config, _geocoding);
		
		if (start >= 0) {
			ssr.setStartIndex((int) start);
		}
		if (max > 0) {
			ssr.setMaxResults((int)max);
		}
		
		List<String> rawIds = new ArrayList<String>();
		List<FL_SearchResult> results = new ArrayList<FL_SearchResult>();
		while (ssr.hasNext()) {
			FL_SearchResult fsr = ssr.next();

			FL_Entity fle = (FL_Entity)fsr.getResult();
			TypedId tId = TypedId.fromTypedId(fle.getUid());

			String namespacedUId = getNamespaceHandler().globalFromLocalEntityId(tId.getNamespace(), tId.getNativeId(), tId.getType());
			fle.setUid(namespacedUId);

			String rawId = TypedId.fromTypedId(fle.getUid()).getNativeId();
			
			if (brokerIds.containsKey(rawId)) {
				List<String> realIds = brokerIds.get(rawId);
				for (String rid : realIds) {
					List<FL_Property> copyProps = new ArrayList<FL_Property>();
					for (FL_Property oldProp : fle.getProperties()) {
						copyProps.add(FL_Property.newBuilder(oldProp).build());
					}
					// add owner property
					copyProps.add( new PropertyHelper(
							"owner", 
							"Account Owner", 
							fle.getUid(), 
							Arrays.asList(
								FL_PropertyTag.ACCOUNT_OWNER
							)
						));

					TypedId tRId = TypedId.fromNativeId(TypedId.ACCOUNT, rid);
					String namespacedRId = getNamespaceHandler().globalFromLocalEntityId(tRId.getNamespace(), tRId.getNativeId(), tRId.getType());

					FL_Entity copy = FL_Entity.newBuilder()
							.setUid(namespacedRId)
							.setProperties(copyProps)
							.setTags(Arrays.asList(FL_EntityTag.ACCOUNT))
							.setProvenance(null)
							.setUncertainty(null)
							.build();

					FL_SearchResult srcopy = FL_SearchResult.newBuilder()
							.setResult(copy)
							.setScore(fsr.getScore())
							.build();
					
					results.add(srcopy);
					rawIds.add(rid);
				}
			} else {
				results.add(fsr);
				rawIds.add(rawId);
			}
		}
		
		try {
			Map<String, int[]> entityStats = new HashMap<String, int[]>();
			
			Connection connection = _connectionPool.getConnection();
			
			// separately grab the FinEntity stats
			String finEntityTable = _namespaceHandler.tableName(null, DataAccessHelper.ENTITY_TABLE);
			String finEntityEntityId = _namespaceHandler.columnName(DataAccessHelper.ENTITY_COLUMN_ENTITY_ID);
			String finEntityUniqueOutboundDegree = _namespaceHandler.columnName(DataAccessHelper.ENTITY_COLUMN_UNIQUE_OUTBOUND_DEGREE);
			String finEntityUniqueInboundDegree = _namespaceHandler.columnName(DataAccessHelper.ENTITY_COLUMN_UNIQUE_INBOUND_DEGREE);

			// Create prepared statement
			String preparedStatementString = buildPreparedStatementForSearch(
				rawIds.size(),
				finEntityEntityId,
				finEntityUniqueInboundDegree,
				finEntityUniqueOutboundDegree,
				finEntityTable
			);
			PreparedStatement stmt = connection.prepareStatement(preparedStatementString);
			
			int index = 1;
			for (int i = 0; i < rawIds.size(); i++) {
				stmt.setString(index++, getNamespaceHandler().toSQLId(rawIds.get(i), null));
			}
			
			if (!rawIds.isEmpty()) {
				ResultSet rs = stmt.executeQuery();
				while (rs.next()) {
					String entityId = rs.getString(finEntityEntityId);
					int inDegree = rs.getInt(finEntityUniqueInboundDegree);
					int outDegree = rs.getInt(finEntityUniqueOutboundDegree);
				
					entityStats.put(entityId, new int[]{inDegree, outDegree});
				}
				rs.close();
			}
			
			stmt.close();
			
			for (FL_SearchResult result : results) {
				FL_Entity fle = (FL_Entity)result.getResult();
				int[] stats = entityStats.get( TypedId.fromTypedId( fle.getUid() ).getNativeId() );
			
				if (stats != null) {
					// add degree stats
					fle.getProperties().add( new PropertyHelper("inboundDegree", stats[0], FL_PropertyTag.INFLOWING) );
					fle.getProperties().add( new PropertyHelper("outboundDegree", stats[1], FL_PropertyTag.OUTFLOWING) );
				}
			}

			connection.close();
		} catch (Exception e) {
			throw new AvroRemoteException(e);
		}
		return new FL_SearchResults((long)ssr.getTotalResults(), results);
	}




	@Override
	public FL_PropertyDescriptors getDescriptors() throws AvroRemoteException {
		return  _cfd.getEntityDescriptors();
	}


	
	
	public KivaEntitySearchIterator buildKivaEntitySearchIterator(SolrServer solr, SolrQuery query, Properties config, FL_Geocoding geocoding) {
		return new KivaEntitySearchIterator(solr, query, config, geocoding);
	}
	
	
	
	
	private String buildPreparedStatementForSearch(
		int numIds,
		String finEntityEntityId, 
		String finEntityUniqueInboundDegree,
		String finEntityUniqueOutboundDegree, 
		String finEntityTable
	) {
		if (finEntityEntityId == null ||
			finEntityUniqueInboundDegree == null ||
			finEntityUniqueOutboundDegree == null ||
			finEntityTable == null
		) {
			s_logger.error("buildPreparedStatementFoSearch: Invalid parameter");
			return null;
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT " + finEntityEntityId + ", " + finEntityUniqueInboundDegree + ", " + finEntityUniqueOutboundDegree + " ");
		sb.append("FROM " + finEntityTable + " ");
		if (numIds > 0) {
			sb.append("WHERE " + finEntityEntityId + " IN (");
			for (int i = 1; i < numIds; i++) {
				sb.append("?, ");
			}
			sb.append("?) ");
		}
		
		return sb.toString();
	}
}
