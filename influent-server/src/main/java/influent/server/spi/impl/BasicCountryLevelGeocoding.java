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
package influent.server.spi.impl;

import influent.idl.FL_ContinentCode;
import influent.idl.FL_Country;
import influent.idl.FL_GeoData;
import influent.idl.FL_Geocoding;
import influent.idlconst.ImmutableCountry;
import influent.idlconst.ImmutableGeoData;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.avro.AvroRemoteException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.CharStreams;
import com.google.common.io.Closeables;
import com.google.inject.Singleton;

/**
 * Default provider of simple country level geocoding.
 */
@Singleton
public class BasicCountryLevelGeocoding implements FL_Geocoding {

	private static final Logger s_logger = LoggerFactory.getLogger(BasicCountryLevelGeocoding.class);

	// code to country lookup
	private final Map<String, FL_Country> countryMap;

	// countries ordered by length of name.
	private final List<FL_Country> nameList;
	
	
	/**
	 * Constructs a new geocoder and initialises it by loading local country data.
	 */
	public BasicCountryLevelGeocoding() {
		nameList = new ArrayList<FL_Country>();
		countryMap = new HashMap<String, FL_Country>();
		
		final InputStream inp = BasicCountryLevelGeocoding.class.getResourceAsStream("countries.json");
		
		if (inp != null) {
			try {
				final String json = CharStreams.toString(new BufferedReader(
						new InputStreamReader(inp, Charset.forName("UTF-8"))));
				
				final JSONArray array = new JSONArray(json);
				
				for (int i=0; i< array.length(); i++) {
					final JSONObject record = array.getJSONObject(i);
					
					final FL_Country country = new ImmutableCountry(
						new ImmutableGeoData(
							getString(record, "CountryName"),
							getDouble(record, "Latitude"),
							getDouble(record, "Longitude"),
							getString(record, "ISOCC3")),
							getString(record, "GlobalRegion"),
							FL_ContinentCode.valueOf(getString(record, "ContinentCode"))
						);
					

					final String isoCC2 = getString(record, "ISOCC2");
					final String fips = getString(record, "FIPS");
					final String ccTLD = getString(record, "InternetCCTLD");
					final Long isoNo = getLong(record, "ISONo");
					
					countryMap.put(isoCC2, country);
					countryMap.put(country.getCountry().getCc(), country);
					
					// add non-conflicting fips.
					if (fips != null && !countryMap.containsKey(fips)) {
						countryMap.put(fips, country);
					}
					if (isoNo != null) {
						countryMap.put(String.valueOf(isoNo), country);
					}
					
					// not necessary (same as iso 2), but...
					if (ccTLD != null && !countryMap.containsKey(ccTLD)) {
						countryMap.put(ccTLD, country);
					}
					
					nameList.add(country);
				}
				
				// sort countries
				Collections.sort(nameList, new Comparator<FL_Country>() {
					public int compare(FL_Country o1, FL_Country o2) {
						return o2.getCountry().getText().length() - o1.getCountry().getText().length();
					}
				});
				
			} catch(IOException e) {
				s_logger.error("Failed to loan countries.json", e);
			} catch (JSONException e) {
				s_logger.error("Failed to parse countries.json", e);
			}finally {
				Closeables.closeQuietly(inp);
			}
			
		}
	}
	
	private static String getString(JSONObject record, String fieldName) {
		try {
			return record.getString(fieldName);
		} catch (Exception e) {
			return null;
		}
	}
	
	private static Double getDouble(JSONObject record, String fieldName) {
		try {
			return record.getDouble(fieldName);
		} catch (Exception e) {
			return null;
		}
	}
	
	private static Long getLong(JSONObject record, String fieldName) {
		try {
			return record.getLong(fieldName);
		} catch (Exception e) {
			return null;
		}
	}
	
	/**
	 * This is a simple placeholder implementation that searches each address for a country name.
	 */
	@Override
	public boolean geocode(List<FL_GeoData> locations) throws AvroRemoteException {
		boolean changed = false;
		
		for (FL_GeoData location : locations) {
			
			// if has a country code...
			if (location.getCc() != null && !location.getCc().isEmpty()) {
				
				// if nothing to do, continue.
				if (location.getLat() != null && location.getLon() != null && 
						location.getText() != null && !location.getText().isEmpty() &&
						location.getCc().length() == 3) {
					continue;
				}

				final FL_Country country = countryMap.get(location.getCc());
				
				if (country != null) {
					final FL_GeoData countryGeo = country.getCountry();
					if (location.getLat() == null) {
						location.setLat(countryGeo.getLat());
						location.setLon(countryGeo.getLon());
					}
					if (location.getText() == null || location.getText().isEmpty()) {
						location.setText(countryGeo.getText());
					}
					
					location.setCc(countryGeo.getCc());
					
					// found, continue.
					changed = true;
					continue;
				}
			}
			
			final String address = location.getText();
			
			// if has text, simply look for country name
			if (address != null && !address.isEmpty()) {
				for (FL_Country country : nameList) {
					if (address.indexOf(country.getCountry().getText()) != -1) {
						final FL_GeoData countryGeo = country.getCountry();
						
						location.setCc(countryGeo.getCc());
						
						if (location.getLat() == null) {
							location.setLat(countryGeo.getLat());
							location.setLon(countryGeo.getLon());
						}
						
						changed = true;
						break;
					}
				}
			}
			
			// we do not have the ability to reverse geocode from lat / lon.
		}
		
		return changed;
	}

	/**
	 * Does a lookup for country data based on country code. Locations must be geocoded with
	 * country codes if a country result is not to be null.
	 */
	@Override
	public List<FL_Country> getCountries(List<FL_GeoData> locations) throws AvroRemoteException {
		final List<FL_Country> countries = new ArrayList<FL_Country>(locations.size());
		
		for (FL_GeoData location : locations) {
			countries.add(location.getCc() == null? null : countryMap.get(location.getCc()));
		}

		return countries;
	}
	
}
