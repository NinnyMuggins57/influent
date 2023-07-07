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
import influent.idl.FL_EntityTag;
import influent.idl.FL_GeoData;
import influent.idl.FL_Geocoding;
import influent.idl.FL_ListRange;
import influent.idl.FL_Property;
import influent.idl.FL_PropertyTag;
import influent.idl.FL_PropertyType;
import influent.idl.FL_SearchResult;
import influent.idl.FL_Uncertainty;
import influent.idlhelper.PropertyHelper;
import influent.idlhelper.SingletonRangeHelper;
import influent.kiva.server.dataaccess.KivaFLTagMaps;
import influent.server.utilities.TypedId;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.avro.AvroRemoteException;
import org.apache.avro.AvroRuntimeException;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrException;
import org.joda.time.DateTime;
import org.slf4j.Logger;


/**
 * Search result object for iterating through the results of a search.  Does paging, etc, behind the scenes.
 * @author msavigny
 *
 */

public class KivaEntitySearchIterator implements Iterator<FL_SearchResult> {

	private static Logger s_logger = org.slf4j.LoggerFactory.getLogger(KivaEntitySearchIterator.class);
	
	private int REFRESH_SIZE=200;
	
	
	private SolrServer _server;
	private SolrQuery _query;
	
	private final FL_Geocoding _geocoding;
	
	//Current query response
	private QueryResponse _qResp;
	private int _curIdx=0;
	private int _nextLocalIdx=0;

	private int _totalResults;
	private int _maxResults=-1;
	private int _startIdx = 0;
	
	private List<FL_SearchResult> _curResults;
	
	//Handle id prefixes for 'federated' management
	@SuppressWarnings("unused")
	private String _idPrefix="";
	
	public KivaEntitySearchIterator(SolrServer server, SolrQuery q, FL_Geocoding geocoding) {
		_server = server;
		_query = q;
		_geocoding = geocoding;
		_totalResults = -1;
		_curResults = new ArrayList<FL_SearchResult>(REFRESH_SIZE);
		for (int i=0;i<REFRESH_SIZE;i++) {
			_curResults.add(null);
		}
	}
	
	public void setIdPrefix(Character c) {
		_idPrefix = c.toString();
	}
	
	public void setStartIndex(int startIdx) {
		_curIdx = startIdx;
		_startIdx = startIdx;
	}
	
	public void setMaxResults(int maxResults) {
		_maxResults = maxResults;
		if (_maxResults < REFRESH_SIZE) {
			REFRESH_SIZE = _maxResults+1;
		}
	}
	
	@Override
	public boolean hasNext() {
		//Check for refresh
		
		if (needsRefresh()) doRefresh(_curIdx,REFRESH_SIZE);
		if (_maxResults != -1) {
			if (_curIdx < _startIdx+_maxResults && _curIdx < _totalResults) return true;
			return false;
		}
		if (_curIdx < _totalResults) return true;
		
		return false;
	}

	@Override
	public FL_SearchResult next() {
		
		if (needsRefresh()) doRefresh(_curIdx,REFRESH_SIZE);
		FL_SearchResult e = _curResults.get(_nextLocalIdx);
		_curIdx++;
		_nextLocalIdx++;
		return e;
		
	}

	@Override
	public void remove() {
		
	}

	public int getTotalResults() {
		if (needsRefresh()) doRefresh(_curIdx,REFRESH_SIZE);
		
		return _totalResults;
	}

	/**
	 * Returns true if there are more results to fetch but we've already
	 * iterated over the current set.
	 * @return
	 */
	private boolean needsRefresh() {
		if (_qResp == null) return true;
		if (_totalResults == -1) return true;
		
		if (_nextLocalIdx>=REFRESH_SIZE) return true;
		
		
		return false;
	}

	private void doRefresh(int startIdx, int pageSize)  {
		try {
			s_logger.warn("fetching solr page : (@"+startIdx+" of size "+pageSize+")");
			_query.setRows(pageSize).setStart(startIdx);
			_qResp = _server.query(_query);
			if (_totalResults == -1) {
				_totalResults = (int)_qResp.getResults().getNumFound();
			}
			_curResults.clear();
			
			//Go through the results, and keep a map of id->score, as well as ids to fetch
			for (int i=0;i<_qResp.getResults().size();i++) {
				SolrDocument sd = _qResp.getResults().get(i);
				FL_SearchResult es = new FL_SearchResult();
				FL_Entity entity = buildEntityFromDocument(sd);
				es.setResult(entity);
				double score = ((Float)sd.getFieldValue("score")).doubleValue();
				es.setScore(score);
				_curResults.add(es);
			}
			
			_nextLocalIdx = 0;
		} catch (SolrException e) {
			s_logger.error("Solr query error: " + e.getMessage());
		} catch (Exception e) {
			throw new AvroRuntimeException("Error paging search results "+e.getMessage(),e); 
		}
	}
	
	private FL_Entity buildEntityFromDocument(SolrDocument sd) {
		
		FL_Entity.Builder entityBuilder = FL_Entity.newBuilder();
		List<FL_Property> props = new ArrayList<FL_Property>();
		
		
		String uid = (String)sd.getFieldValue("id");

		entityBuilder.setProvenance(null);
		entityBuilder.setUncertainty(null);
		
		//Kiva specific type handling
		String type = "";
		
		if (uid.startsWith("l")) {
			type = "lender";
			
			final double notVeryConfidentDemonstration = 0.4*Math.random();
			
			//HACK : this is for demo purposes only, makes lenders parchmenty
			entityBuilder.setUncertainty(FL_Uncertainty.newBuilder().setConfidence(notVeryConfidentDemonstration).build());
		} else if (uid.startsWith("b")) {
			type = "loan";
		} else if (uid.startsWith("p")) {
			type = "partner";
		}
		
		props.add(FL_Property.newBuilder().setKey("type")
				.setFriendlyText("Kiva Account Type")
				.setProvenance(null)
				.setUncertainty(null)
				.setTags(Collections.singletonList(FL_PropertyTag.TYPE))
				.setRange(new SingletonRangeHelper(type, FL_PropertyType.STRING))
				.build());
		
		//TODO : get tags once added to solr.
		
		//Read and build properties.
		Map<String, Collection<Object>> docValues = sd.getFieldValuesMap();
		FL_GeoData.Builder geoBuilder = FL_GeoData.newBuilder().setCc("").setLat(0.0).setLon(0.0).setText("");
		FL_Property.Builder propBuilder;
		FL_PropertyTag[] tags;
		List<FL_PropertyTag> ltags;
		boolean geoDataFound = false;
		String label = null;
		for (String key : docValues.keySet()) {
			if ("score".equals(key)) continue;			//Skip the score, it does not belong in the entity object
			
			//create a FL_GeoData builder in which the known geo fields can be placed.
			//Set default values;
			
			
			for (Object val : docValues.get(key)) {
			
				//special case handling for geodata 
				if (key.equals("lender_whereabouts") || key.equals("loans_location_town") || key.equals("loans_location_country")) {
					String cleanVal = val.toString().trim();
					if (!cleanVal.isEmpty()) {
						geoBuilder.setText(geoBuilder.getText().isEmpty()? cleanVal : geoBuilder.getText()+", "+cleanVal);
						geoDataFound = true;
					}
					//continue;
				} else if (key.equals("lenders_countryCode") || key.equals("loans_location_countryCode") || key.equals("partners_cc")) {
					String cleanVal = val.toString().replaceAll(","," ").trim();
					// correct kiva's invented country code for south sudan with the now official iso standard
					cleanVal = cleanVal.replaceAll("QS", "SS");
					if (!cleanVal.isEmpty()) {
						geoBuilder.setCc(geoBuilder.getCc().isEmpty()? cleanVal : geoBuilder.getCc()+" "+cleanVal);
						geoDataFound = true;
					}
					//continue;
				} else if (key.equals("lat")) {
					geoBuilder.setLat((Double)val);
					geoDataFound = true;
					//continue;
				} else if (key.equals("lon")) {
					geoBuilder.setLon((Double)val);
					geoDataFound = true;
					//continue;
				}
				
				
				propBuilder = FL_Property.newBuilder();
				propBuilder.setKey(key);
				
				if (val instanceof Collection) {
					s_logger.warn("Prop "+key+" has a "+val.getClass()+" value, skipping for now");
					continue;
				} else {
					propBuilder.setFriendlyText(key);
				}
				
				propBuilder.setProvenance(null);
				propBuilder.setUncertainty(null);

				tags = KivaFLTagMaps.INSTANCE.getPropTagMap().get(key);
				if (tags == null || tags.length==0) {
					ltags = new ArrayList<FL_PropertyTag>();
					ltags.add(FL_PropertyTag.RAW);
					propBuilder.setTags(ltags);
				} else {
					ltags = Arrays.asList(tags);
					propBuilder.setTags(ltags);
				}
				
				/*if ((val instanceof Integer) || (val instanceof Long)) {
					val = val.toString()+"L";
				}*/
				
				//Special case value handling - jodatime
				if (val instanceof Date) {
					propBuilder.setRange(new SingletonRangeHelper(((Date)val).getTime(), FL_PropertyType.OTHER));
				} else if (val instanceof DateTime) {
					propBuilder.setRange(new SingletonRangeHelper(((DateTime)val).getMillis(), FL_PropertyType.OTHER));
				} else {
					propBuilder.setRange(new SingletonRangeHelper(val, FL_PropertyType.OTHER));
				}
				
				
				props.add(propBuilder.build());
				
			}
			
			// Added to resolve #6245; I am not terribly familiar with the inner workings of the back end, so feel 
			// free to refactor this if it isn't the best place to assign the value of 'label'
			if(label == null && docValues.get(key).size() > 0 &&
				(key.equals("lenders_name") || key.equals("partners_name") || key.equals("loans_name") || key.equals("teams_name"))) {

				Object value = docValues.get(key).iterator().next();
				
				if (value != null) {
					label = value.toString();
				}
			}
		}
		
		//Build the geo property if geo data was found
		if (geoDataFound) {
			String trimmed = geoBuilder.getText();
			
			if (trimmed != null) {
				trimmed = trimmed.trim();
				
				if (!trimmed.isEmpty()) {
					label += ". "+ trimmed;
				}
			}
			
			FL_GeoData flgd = geoBuilder.build();
			List<FL_GeoData> geos;
			
			// multiple values here. break them up.
			if (flgd.getCc() != null && flgd.getCc().indexOf(' ') != -1) {
				String ccs[] = flgd.getCc().split(" ");
				geos = new ArrayList<FL_GeoData>(ccs.length);
				
				for (int j=0; j<ccs.length; j++) {
					geos.add(new FL_GeoData(flgd.getText(), flgd.getLat(), flgd.getLon(), ccs[j]));
				}
				
			} else {
				geos = Collections.singletonList(flgd);
			}

			try {
				_geocoding.geocode(geos);
			} catch (AvroRemoteException e) {
				s_logger.info("Failed to geocode entity", e);
			}
			
			Object geoVal = (geos.size() > 1)? 
					FL_ListRange.newBuilder()
						.setType(FL_PropertyType.GEO)
						.setValues(Arrays.asList(geos.toArray()))
						.build() 
						: new SingletonRangeHelper(geos.get(0), FL_PropertyType.GEO);
			
			FL_Property geoProp = FL_Property.newBuilder()
					.setKey("geo")
					.setFriendlyText("Location")
					.setTags(Collections.singletonList(FL_PropertyTag.GEO))
					.setRange(geoVal)
					.setProvenance(null)
					.setUncertainty(null)
					.build();
			
			props.add(geoProp);
		}
		
		
		propBuilder = FL_Property.newBuilder();
		propBuilder.setKey("label");
		propBuilder.setFriendlyText("label");
		propBuilder.setProvenance(null);
		propBuilder.setUncertainty(null);
		ltags = new ArrayList<FL_PropertyTag>();
		ltags.add(FL_PropertyTag.LABEL);
		propBuilder.setTags(ltags);
		propBuilder.setRange(new SingletonRangeHelper(label, FL_PropertyType.OTHER));
		
		props.add(propBuilder.build());
		
		List<FL_EntityTag> etags = new ArrayList<FL_EntityTag>();
		
		if (type.equals("partner")) {
			 KivaFLTagMaps.INSTANCE.appendPartnerProperties(props);
			 etags.add(FL_EntityTag.ACCOUNT_OWNER);  // partners are account owners
			 entityBuilder.setUid(TypedId.fromNativeId(TypedId.ACCOUNT_OWNER, uid).getTypedId());
			 
			 // determine whether this a large account owner and if there is a cluster summary associated
			 final FL_Property numLoans = PropertyHelper.getPropertyByKey(props, "partners_loansPosted");
			
			 if (numLoans != null) {
				 final Number number = (Number) PropertyHelper.from(numLoans).getValue();
				
				 if (number != null && number.intValue() >= 1000) {
					 props.add(new PropertyHelper(FL_PropertyTag.CLUSTER_SUMMARY, TypedId.fromNativeId(TypedId.CLUSTER_SUMMARY, 's' + uid).getTypedId()));
					 entityBuilder.setUid(TypedId.fromNativeId(TypedId.CLUSTER_SUMMARY, uid).getTypedId());
				 }
			 }
		}
		else {
			etags.add(FL_EntityTag.ACCOUNT);  // all others are raw accounts
			entityBuilder.setUid(TypedId.fromNativeId(TypedId.ACCOUNT, uid).getTypedId());
		}
		
		entityBuilder.setTags(etags);
		
		entityBuilder.setProperties(props);
		
		return entityBuilder.build();
	}
}
