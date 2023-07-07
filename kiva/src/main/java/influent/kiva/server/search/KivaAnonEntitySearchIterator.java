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
import influent.idl.FL_Uncertainty;
import influent.idlhelper.PropertyHelper;
import influent.idlhelper.SingletonRangeHelper;
import influent.kiva.server.dataaccess.KivaPropertyMapping;
import influent.kiva.server.dataaccess.KivaPropertyMaps;
import influent.server.utilities.TypedId;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import oculus.aperture.spi.common.Properties;

import org.apache.avro.AvroRemoteException;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.common.SolrDocument;
import org.joda.time.DateTime;
import org.slf4j.Logger;


/**
 * Search result object for iterating through the results of a search.  Does paging, etc, behind the scenes.
 * @author msavigny
 *
 */

public class KivaAnonEntitySearchIterator extends KivaEntitySearchIterator {

	private final Properties _config;
	private String _imageURLPrefix = "";

	private static Logger s_logger = org.slf4j.LoggerFactory.getLogger(KivaAnonEntitySearchIterator.class);
	
	private static HashSet<String> LENDER_IGNORE_FIELDS = new HashSet<String>(Arrays.asList("id", "lender_personalURL", "text", "image_id"));
	
	public KivaAnonEntitySearchIterator(SolrServer server, SolrQuery q, Properties config, FL_Geocoding geocoding) {
		super(server, q, config, geocoding);
		_config = config;
		_imageURLPrefix = _config.getString("influent.kiva.imageURL", 
											"http://www.kiva.org/img/w400/");
	}
	
	@Override
	protected Logger getLogger() {
		return s_logger;
	}

	@Override
	protected FL_Entity buildEntityFromDocument(SolrDocument sd) {
		
		FL_Entity.Builder entityBuilder = FL_Entity.newBuilder();
		List<FL_Property> props = new ArrayList<FL_Property>();
		List<FL_EntityTag> etags = new ArrayList<FL_EntityTag>();

		String uid = (String)sd.getFieldValue("id");

		entityBuilder.setProvenance(null);
		entityBuilder.setUncertainty(null);
		
		//Kiva specific type handling
		String type = "";
		
		if (uid.startsWith("l")) {
			type = "lender";
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
		
		Collection<Object> imageURLs = null;
		
		if (type.equals("lender")) {
			imageURLs = new ArrayList<Object>();
			imageURLs.add("726677");
			
		} else {
			imageURLs = sd.getFieldValues("image_id");
		}
		
		// --- FEATURE DEMOS ------------------------------------------
		
		// Prompt for details
		if (uid.equals("leivind")) {
			etags.add(FL_EntityTag.PROMPT_FOR_DETAILS);
		}

		// Multiple image carousel
		if (uid.equals("b150236")) {
			imageURLs.add("146773");
			imageURLs.add("148448");
		}
		
		// Make lenders parchmenty
		if (type.equals("lender")) {
			final double notVeryConfidentDemonstration = 0.4*Math.random();
			
			entityBuilder.setUncertainty(FL_Uncertainty.newBuilder().setConfidence(notVeryConfidentDemonstration).build());
		}
		
		// ------------------------------------------------------------		
		
		for (Object url :  imageURLs) {
		
			String imageURL = _imageURLPrefix + url.toString() + ".jpg";
			
			// Add a Kiva image
			props.add(FL_Property.newBuilder().setKey("image")
					.setFriendlyText("Image")
					.setProvenance(null)
					.setTags(Collections.singletonList(FL_PropertyTag.IMAGE))
					.setRange(new SingletonRangeHelper(imageURL, FL_PropertyType.OTHER))
					.build());
		}
				
		
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
			if(type.equals("lender") && LENDER_IGNORE_FIELDS.contains(key)) {
				continue;
			}
			
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

				KivaPropertyMapping keyMapping = KivaPropertyMaps.INSTANCE.getPropMap().get(key);
				
				if (val instanceof Collection) {
					getLogger().warn("Prop "+key+" has a "+val.getClass()+" value, skipping for now");
					continue;
				} else {
					if (keyMapping != null) {
						String friendlyName = KivaPropertyMaps.INSTANCE.getPropMap().get(key).getFriendlyName();
						propBuilder.setFriendlyText(friendlyName);
					} else {
						propBuilder.setFriendlyText(key);
					}
				}
				
				propBuilder.setProvenance(null);
				propBuilder.setUncertainty(null);

				tags = null;
				if (keyMapping != null) {
					tags = keyMapping.getPropertyTags();
				}
				
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
					propBuilder.setRange(new SingletonRangeHelper(((Date)val).getTime(), FL_PropertyType.DATE));
				} else if (val instanceof DateTime) {
					propBuilder.setRange(new SingletonRangeHelper(((DateTime)val).getMillis(), FL_PropertyType.DATE));
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
				getLogger().info("Failed to geocode entity", e);
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
		
		if (type.equals("partner")) {
			 KivaPropertyMaps.INSTANCE.appendPartnerProperties(props);
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
