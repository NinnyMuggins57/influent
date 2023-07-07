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
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package influent.server.clustering.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.slf4j.Logger;

import org.slf4j.LoggerFactory;

public class PropertyManager {
	private Map<String, String> pMap;

	public static final String ENABLE_STOPWORDS = "entity.clusterer.enablestopwords";
	public static final String STOP_WORDS = "entity.clusterer.stopwords";
	public static final String CLUSTER_FIELDS = "entity.clusterer.clusterfields";
	public static final String MIN_CLUSTER_SIZE = "entity.clusterer.minclustersize";
	public static final String CLUSTER_PROPERTIES = "entity.clusterer.clusterproperties";
	
	private static Logger log = LoggerFactory.getLogger("influent");
	
	@SuppressWarnings("unchecked")
	public PropertyManager(InputStream configResource) {
		pMap = new HashMap<String, String>();
		Properties props = readProperties(configResource);
		for (Enumeration<String> e = (Enumeration<String>) props.propertyNames(); e.hasMoreElements(); ) {
			String key = e.nextElement();
			pMap.put(key, props.getProperty(key));
		}
	}
	
	public String getProperty(String key) {
		return pMap.get(key);
	}
	
	public String getProperty(String key, String defaultVal) {
		String val = pMap.get(key);
		return val == null ? defaultVal : val;
	}
	
	public List<String> getPropertyArray(String key) {
		String joined = getProperty(key);
		String[] split = joined.split(",");
		List<String> list = new ArrayList<String>();
		for (String s : split) {
			list.add(s);
		}
		return list;
	}
	
	public Set<String> getPropertySet(String key) {
		Set<String> set = new HashSet<String>();
		String joined = getProperty(key);
		if (joined!=null) {
			String[] split = joined.split(",");
			for (String s : split) {
				set.add(s);
			}
		}
		return set;		
	}
	
	private Properties readProperties(InputStream configResource) {			
		Properties props = new Properties();
		
		try {
			props.load(configResource);
		} catch (IOException e) { 
			log.error("Unable to read clusterer.properties file from resources.  Using defaults."); 
		}
	
		return props;
	}
}

