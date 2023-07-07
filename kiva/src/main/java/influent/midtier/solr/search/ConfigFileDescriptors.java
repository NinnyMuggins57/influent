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
package influent.midtier.solr.search;

import influent.idl.FL_Constraint;
import influent.idl.FL_PropertyDescriptor;
import influent.idl.FL_PropertyDescriptor.Builder;
import influent.idl.FL_PropertyType;
import influent.idl.FL_RangeType;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Currently the only implementation for producing EntityDescriptors.  If other methods are
 * implemented, then an interface will need to be derived from this class.
 * @author msavigny	
 *
 */


public class ConfigFileDescriptors {

	private static final Logger s_logger = LoggerFactory.getLogger(ConfigFileDescriptors.class);

	private Map<String, List<FL_PropertyDescriptor>> _descriptors;

	
	
	public ConfigFileDescriptors() {
		_descriptors = new LinkedHashMap<String, List<FL_PropertyDescriptor>>();
	}
	
	
	
	
	public Map<String, List<FL_PropertyDescriptor>> getEntityDescriptors() {
		return _descriptors;
	}
	
	
	
	
	public void readDescriptorsFromFile(String fileName) throws IOException {		
		File file = new File("./conf/" + fileName);
		InputStream is = file.exists()? new FileInputStream(file) : 
			getClass().getResourceAsStream("/"+fileName);

		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader br = new BufferedReader(isr);

		int lx = 0;

		List<FL_PropertyDescriptor> curDesc = null;

		while (true) {
			lx++;
			String line = br.readLine();
			if (line == null) break;
			line=line.trim();
			if (line.startsWith("#")) continue;
			if (line.isEmpty()) continue;

			//Ok, first split on ' '
			String split1[] = line.split(" ");

			//Then get the type and the name
			String typeAndName[] = split1[0].split(":");

			if (typeAndName.length!=2) {
				s_logger.warn("Error in "+file.getAbsolutePath()+" on line "+lx);
				continue;
			}

			//Check and see if this is a new entity descriptor or not.
			if (typeAndName[0].equalsIgnoreCase("type")) {
				curDesc = new ArrayList<FL_PropertyDescriptor>();
						
				// new entity type.
				_descriptors.put(typeAndName[1], curDesc);

				s_logger.info("New descriptors for type "+ typeAndName[1]);

				continue;
			} else {
				//New property descriptor
				FL_PropertyType ptype = null;
				
				if (typeAndName[0].equalsIgnoreCase("string")) {
					ptype = FL_PropertyType.STRING;
				} else if (typeAndName[0].equalsIgnoreCase("integer")) {
					ptype = FL_PropertyType.LONG;
				} else if (typeAndName[0].equalsIgnoreCase("real")) {
					ptype = FL_PropertyType.DOUBLE;
				} else if (typeAndName[0].equalsIgnoreCase("boolean")) {
					ptype = FL_PropertyType.BOOLEAN;
				} else if (typeAndName[0].equalsIgnoreCase("date")) {
					ptype = FL_PropertyType.DATE;
				}

				if (ptype == null) {
					s_logger.warn("Error in "+file.getAbsolutePath()+" on line "+lx);
					continue;
				}

				Builder pd = FL_PropertyDescriptor.newBuilder()
						.setType(ptype)
						.setRange(FL_RangeType.SINGLETON)
						.setConstraint(FL_Constraint.FUZZY_PARTIAL_OPTIONAL)
						.setKey(typeAndName[1]);
				
				s_logger.info("Property Descriptor "+pd.getKey()+":"+pd.getType()+" added to type");

				// there is currently no provision for returning a restricted option type of search,
				// or a list of suggestions. commenting out - DJ
/*				
				if (line.split("\\[").length > 1) {
					final List<Object> values = new ArrayList<Object>();
					
					s_logger.info("Adding suggested terms for " + pd.getKey());
					String suggestions = split1[1].trim();
					suggestions = suggestions.replace("[", "");
					suggestions = suggestions.replace("]", "");
					String[] splitSuggestions = suggestions.split(",");
					for (String suggest : splitSuggestions) {
						s_logger.info("\"" + suggest + "\"");
						values.add(suggest);
					}
					
					pd.setConstraint(constraint);
				}
*/
				//Now, check for friendly text
				if (line.split("\\{").length > 1) {
					Pattern p = Pattern.compile("\\{(.*?)\\}");
					Matcher m = p.matcher(line);

					if (m.find()) {
						pd.setFriendlyText(m.group(1));
						s_logger.info("Added " + m.group(1) + ": friendly text for " + pd.getKey());
					}
				}
				
				if (line.trim().endsWith("<<")) {
					pd.setDefaultTerm(true);
				}
				
				if (curDesc == null) {
					curDesc = new ArrayList<FL_PropertyDescriptor>();
					_descriptors.put("default", curDesc);
				}
				
				curDesc.add(pd.build());
			}

		}

		is.close();
		isr.close();
		br.close();
	}
}
