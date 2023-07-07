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
import influent.idl.FL_Geocoding;
import influent.idl.FL_ListRange;
import influent.idl.FL_Property;
import influent.idl.FL_PropertyDescriptor;
import influent.idl.FL_PropertyMatchDescriptor;
import influent.idl.FL_PropertyTag;
import influent.idl.FL_PropertyType;
import influent.idl.FL_SearchResult;
import influent.idl.FL_SearchResults;
import influent.idl.FL_SingletonRange;
import influent.idlhelper.PropertyHelper;
import influent.idlhelper.SolrUtils;
import influent.midtier.solr.search.ConfigFileDescriptors;
import influent.server.dataaccess.DataAccessHelper;
import influent.server.dataaccess.DataNamespaceHandler;
import influent.server.utilities.SQLConnectionPool;
import influent.server.utilities.TypedId;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	private final SQLConnectionPool _connectionPool;
	private final DataNamespaceHandler _namespaceHandler;
	
	private final ConfigFileDescriptors _cfd = new ConfigFileDescriptors();
	
	private static final Map<String, Double> s_basicSearchKeys;
	static {
		s_basicSearchKeys = new HashMap<String, Double>();
		s_basicSearchKeys.put("lenders_name", 1.0);
		s_basicSearchKeys.put("loans_name", 1.0);
		s_basicSearchKeys.put("partners_name", 1.0);
	}
	
	
	public KivaEntitySearch(String solrURL, String solrDescriptor, FL_Geocoding geocoding, SQLConnectionPool connectionPool, DataNamespaceHandler namespaceHandler) {
		_solrURL = solrURL;
		_solr = new HttpSolrServer(_solrURL);
		_geocoding = geocoding;
		_connectionPool = connectionPool;
		_namespaceHandler = namespaceHandler;
		
		try {
			_cfd.readDescriptorsFromFile(solrDescriptor);
		} catch (IOException e) {
			s_logger.warn("Exception reading entity descriptors "+e.getMessage(),e);
		}
		
	}
	
	private FL_PropertyMatchDescriptor filterIds(FL_PropertyMatchDescriptor pst, Map<String, List<String>> brokerIds) {
		final String k = pst.getKey();
		
		if (k.equals("uid")) {
			Collection<Object> values;

			final Object r = pst.getRange();
			
			if (r instanceof FL_SingletonRange) {
				values = Collections.singleton(((FL_SingletonRange)r).getValue());
			} else if (r instanceof FL_ListRange) {
				values = ((FL_ListRange)r).getValues();
			} else {
				return pst;
			}

			// filtered list
			List<Object> filtered = new ArrayList<Object>(values.size());
			
			for (Object obj : values) {
				final String id = TypedId.fromTypedId(String.valueOf(obj)).getNativeId();
				
				if (id.charAt(0) == 'p') {
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
			
			return FL_PropertyMatchDescriptor.newBuilder()
					.setKey("id")
					.setConstraint(pst.getConstraint())
					.setInclude(pst.getInclude())
					.setVariable(pst.getVariable())
					.setRange(FL_ListRange.newBuilder().setType(FL_PropertyType.STRING).setValues(filtered).build())
					.build();
		}
		
		return pst;
	}
	
	@Override
	public FL_SearchResults search(String searchTerms, List<FL_PropertyMatchDescriptor> terms, long start, long max, String type) throws AvroRemoteException {

		final List<FL_PropertyMatchDescriptor> filteredTerms = new ArrayList<FL_PropertyMatchDescriptor>(terms.size());
		
		// special partner handling. lookup broker partners by partner id, then map them back afterwards
		Map<String, List<String>> brokerIds = new HashMap<String, List<String>>();
		
		for (FL_PropertyMatchDescriptor term : terms) {
			final FL_PropertyMatchDescriptor flpmd = filterIds(term, brokerIds);
			
			if (flpmd != null) {
				filteredTerms.add(flpmd);
			}
		}
		
		// form the query
		final String searchStr = SolrUtils.toSolrQuery(searchTerms, s_basicSearchKeys, filteredTerms);
	
		// issue the query.
		s_logger.info("Issuing Solr Query "+ searchStr);
		
		SolrQuery query = new SolrQuery();
		query.setQuery(searchStr);
		query.setFields("*", "score");
		
		KivaEntitySearchIterator ssr = new KivaEntitySearchIterator(_solr, query, _geocoding);
		
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
					FL_Entity copy = FL_Entity.newBuilder().setUid(TypedId.fromNativeId(TypedId.ACCOUNT, rid).getTypedId()).setProperties(copyProps).setTags(Arrays.asList(FL_EntityTag.ACCOUNT)).setProvenance(null).setUncertainty(null).build();
					FL_SearchResult srcopy = FL_SearchResult.newBuilder().setResult(copy).setScore(fsr.getScore()).build();
					
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
			Statement stmt = connection.createStatement();
			
			// separately grab the FinEntity stats
			String finEntityTable = _namespaceHandler.tableName(null, DataAccessHelper.ENTITY_TABLE);

			String sql = "select EntityId, UniqueInboundDegree, UniqueOutboundDegree " +
							" from " + finEntityTable +
							" where EntityId in " +  DataAccessHelper.createInClause(rawIds);
			
			s_logger.info(sql);
			
			if (!rawIds.isEmpty() && stmt.execute(sql)) {
				ResultSet rs = stmt.getResultSet();
				while (rs.next()) {
					String entityId = rs.getString("EntityId");
					int inDegree = rs.getInt("UniqueInboundDegree");
					int outDegree = rs.getInt("UniqueOutboundDegree");
				
					entityStats.put(entityId, new int[]{inDegree, outDegree});
				}
				rs.close();
			}
			
			for (FL_SearchResult result : results) {
				FL_Entity fle = (FL_Entity)result.getResult();
				int[] stats = entityStats.get( TypedId.fromTypedId( fle.getUid() ).getNativeId() );
			
				if (stats != null) {
					// add degree stats
					fle.getProperties().add( new PropertyHelper("inboundDegree", stats[0], FL_PropertyTag.INFLOWING) );
					fle.getProperties().add( new PropertyHelper("outboundDegree", stats[1], FL_PropertyTag.OUTFLOWING) );
				}
			}
			stmt.close();
			connection.close();
		} catch (Exception e) {
			throw new AvroRemoteException(e);
		}
		return new FL_SearchResults((long)ssr.getTotalResults(), results);
	}

	@Override
	public Map<String, List<FL_PropertyDescriptor>> getDescriptors() throws AvroRemoteException {
		return _cfd.getEntityDescriptors();
	}

}
