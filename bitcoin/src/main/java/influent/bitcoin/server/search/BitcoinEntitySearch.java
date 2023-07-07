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
import influent.idl.FL_SingletonRange;
import influent.idl.FL_ListRange;
import influent.idl.FL_SearchResult;
import influent.idl.FL_SearchResults;
import influent.idl.FL_TypeDescriptor;
import influent.idl.FL_PropertyDescriptors;
import influent.idl.FL_TypeMapping;

import influent.idlhelper.EntityHelper;
import influent.idlhelper.PropertyHelper;
import influent.server.dataaccess.DataNamespaceHandler;
import influent.server.utilities.SQLConnectionPool;
import influent.server.utilities.TypedId;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Collections;
import java.util.Collection;
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
		List<FL_PropertyMatchDescriptor> termList,
		long start, 
		long max
	) throws AvroRemoteException {

		FL_SearchResults results = FL_SearchResults.newBuilder()
				.setTotal(0)
				.setResults(new ArrayList<FL_SearchResult>())
				.build();
		
		ArrayList<FL_SearchResult> matches = new ArrayList<FL_SearchResult>();

		try {
			Connection connection = _connectionPool.getConnection();

			// Process the term map by schema
			for (Entry<String, List<FL_PropertyMatchDescriptor>> entry : _getTermsByType(termList).entrySet()) {

				String type = entry.getKey();
				for (FL_PropertyMatchDescriptor pmd : entry.getValue()) {

					Collection<Object> values = _getMatchValues(pmd);

					if (values != null) {

						FL_PropertyDescriptor property = _getPropertyByKey(pmd.getKey());
						String typedKey = _getTypedKey(pmd.getKey(), type);

						// Filter match values for correct types
						List<Object> filtered = new ArrayList<Object>();
						for (Object obj : values) {

							if (property.getPropertyType() == FL_PropertyType.LONG) {
								Integer intgr = null;
								try {
									intgr = Integer.valueOf((String)obj);
								} catch (Exception e) {
								}

								if (intgr != null) {
									filtered.add(intgr);
								}

							} else {
								filtered.add(obj);
							}
						}


						if (filtered.size() > 0) {

							// Build prepared statement
							//FIXME: Currently builds a statement for each match descriptor and doesn't handle matchtype (ANY/ALL)
							StringBuilder sb = new StringBuilder();
							sb.append("SELECT EntityId, Label, InboundDegree, UniqueInboundDegree, OutboundDegree, UniqueOutboundDegree ");
							sb.append("FROM Bitcoin." + entry.getKey() + ".FinEntity ");

							// Handle property constraints
							if (property.getConstraint() == FL_Constraint.FUZZY_REQUIRED) {
								sb.append("WHERE " + typedKey + " LIKE ? " );
								for (int i = 1; i < filtered.size(); i++) {
									sb.append("OR " + typedKey + " LIKE ? ");
								}
							} else {
								sb.append("WHERE " + typedKey + " IN (");
								for (int i = 1; i < filtered.size(); i++) {
									sb.append("?, ");
								}
								sb.append("?) ");
							}

							PreparedStatement stmt = connection.prepareStatement(sb.toString());

							// set statement arguments
							int index = 1;
							for (Object obj : filtered) {
								if (property.getPropertyType() == FL_PropertyType.LONG) {
									stmt.setInt(index++, (Integer) obj);
								} else {
									if (property.getConstraint() == FL_Constraint.FUZZY_REQUIRED)
										stmt.setString(index++, "%" + ((String)obj).trim() + "%");
									else
										stmt.setString(index++, ((String)obj).trim());
								}
							}

							matches.addAll(_searchEntities(stmt, max, entry.getKey()));

							stmt.close();
						}
					}
				}
			}

			results.setResults(matches);
			results.setTotal((long) matches.size());
			
			connection.close();
			
		} catch (Exception e) {
			throw new AvroRemoteException(e);
		}

		return results;
	}


	private List<FL_SearchResult> _searchEntities(
		PreparedStatement statement,
		long max,
		String schema
	) throws SQLException, ClassNotFoundException {
		
		if (max > 0) statement.setMaxRows((int) max);
		if (max > 0) statement.setFetchSize((int) max);
		
		List<FL_SearchResult> matches = new ArrayList<FL_SearchResult>();

		ResultSet resultSet = statement.executeQuery();
		while (resultSet.next() && matches.size() < max) {
			Integer entityId = resultSet.getInt(1);
			String label = resultSet.getString(2);
			
			long inboundDegree = resultSet.getLong(3);
			long uniqueInboundDegree = resultSet.getLong(4);
			long outboundDegree = resultSet.getLong(5);
			long uniqueOutboundDegree = resultSet.getLong(6);
			
			boolean labeled = label != null && !label.trim().isEmpty();
			
			List<FL_Property> props = new ArrayList<FL_Property>();
			props.add(new PropertyHelper("Identification", "Identification", labeled? "Tagged":"Anonymous", Collections.singletonList(FL_PropertyTag.TYPE)));
			props.add(new PropertyHelper("InboundDegree", "Inbound Transfers", inboundDegree, Collections.singletonList(FL_PropertyTag.STAT)));
			props.add(new PropertyHelper("UniqueInboundDegree", "Unique Inbound Links", uniqueInboundDegree, Collections.singletonList(FL_PropertyTag.INFLOWING)));
			props.add(new PropertyHelper("OutboundDegree", "Outbound Transfers", outboundDegree, Collections.singletonList(FL_PropertyTag.STAT)));
			props.add(new PropertyHelper("UniqueOutboundDegree", "Unique Outbound Links", uniqueOutboundDegree, Collections.singletonList(FL_PropertyTag.OUTFLOWING)));
			props.add(new PropertyHelper("image", "Image", "img/bitcoin_default.png", Collections.singletonList(FL_PropertyTag.IMAGE)));

			if (labeled) {
				props.add(new PropertyHelper("UserTag", "User Tag", label, Collections.singletonList(FL_PropertyTag.TEXT)));
			} else {
				label = entityId.toString();
			}

			String uid = _namespaceHandler.globalFromLocalEntityId(schema, entityId.toString(), TypedId.ACCOUNT);
			
			FL_Entity entity = new EntityHelper(uid, label,
					FL_EntityTag.ACCOUNT.name(), FL_EntityTag.ACCOUNT, props);
			matches.add(new FL_SearchResult(1.0, entity));
		}
		
		resultSet.close();
		
		return matches;
	}


	private FL_PropertyDescriptor _getPropertyByKey(String key) throws Exception{
		for (FL_PropertyDescriptor pd : getDescriptors().getProperties()) {
			if (pd.getKey().equals(key)) {
				return pd;
			}
		}

		throw new Exception("Cannot find property with type " + key);
	}

	private String _getTypedKey(String key, String type) throws Exception{

		// Return a property's key in a type
		FL_PropertyDescriptor pd = _getPropertyByKey(key);

		for (FL_TypeMapping td : pd.getMemberOf()) {
			if (td.getType().equals(type)) {
				return td.getMemberKey();
			}
		}

		throw new Exception("Cannot find field name " + key + " in " + type);
	}

	private Collection<Object> _getMatchValues(FL_PropertyMatchDescriptor pmd) {

		if (pmd != null) {
			final Object r = pmd.getRange();

			if (r instanceof FL_SingletonRange) {
				return Collections.singleton(((FL_SingletonRange) r).getValue());
			} else if (r instanceof FL_ListRange) {
				return ((FL_ListRange) r).getValues();
			}
		}

		return null;
	}

	private Map<String, List<FL_PropertyMatchDescriptor>> _getTermsByType(
			List<FL_PropertyMatchDescriptor> terms
	) throws AvroRemoteException {

		Map<String, List<FL_PropertyMatchDescriptor>> typePropMap = new HashMap<String, List<FL_PropertyMatchDescriptor>>();

		// Create map of properties by type
		for (FL_PropertyMatchDescriptor term : terms) {
			for (FL_TypeMapping td : term.getTypeMappings()) {
				List<FL_PropertyMatchDescriptor> typedTerms = typePropMap.get(td.getType());

				if (typedTerms == null) {
					typedTerms = new ArrayList<FL_PropertyMatchDescriptor>();
				}

				typedTerms.add(term);

				typePropMap.put(td.getType(), typedTerms);
			}
		}

		return typePropMap;
	}




	/**
	 * Returns searchable properties for the advanced search.
	 */
	@Override
	public FL_PropertyDescriptors getDescriptors() throws AvroRemoteException {


		List<FL_TypeDescriptor> typeList = new ArrayList<FL_TypeDescriptor>();
		typeList.add(FL_TypeDescriptor.newBuilder()
			.setKey("dbo")
			.setFriendlyText("Account")
			.build());



		// Form the property list
		List<FL_PropertyDescriptor> propList = new ArrayList<FL_PropertyDescriptor>();

		// ID
		propList.add(FL_PropertyDescriptor.newBuilder()
			.setKey("id")
			.setFriendlyText("ID")
			.setPropertyType(FL_PropertyType.LONG)
			.setConstraint(FL_Constraint.REQUIRED_EQUALS)
			.setRange(FL_RangeType.SINGLETON)
			.setFreeTextIndexed(true)
			.setMemberOf(Collections.singletonList(
				FL_TypeMapping.newBuilder()
					.setType("dbo")
					.setMemberKey("entityId")
					.build()
				)
			)
			.build()
		);

		// Name
		propList.add(FL_PropertyDescriptor.newBuilder()
			.setKey("name")
			.setFriendlyText("Name")
			.setPropertyType(FL_PropertyType.STRING)
			.setConstraint(FL_Constraint.FUZZY_REQUIRED)
			.setRange(FL_RangeType.SINGLETON)
			.setFreeTextIndexed(true)
			.setDefaultTerm(true)
			.setMemberOf(Collections.singletonList(
				FL_TypeMapping.newBuilder()
					.setType("dbo")
					.setMemberKey("LABEL")
					.build()
				)
			)
			.build()
		);

		return new FL_PropertyDescriptors(propList, typeList);
	}
}
