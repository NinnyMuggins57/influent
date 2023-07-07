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
import influent.idl.FL_PropertyDescriptors;
import influent.idl.FL_PropertyType;
import influent.idl.FL_RangeType;
import influent.idl.FL_TypeDescriptor;
import influent.idl.FL_TypeMapping;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.RandomAccess;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


/**
 * Currently the only implementation for producing EntityDescriptors.  If other methods are
 * implemented, then an interface will need to be derived from this class.
 * @author msavigny	
 *
 */

public class ConfigFileDescriptors {

	private static final Logger s_logger = LoggerFactory.getLogger(ConfigFileDescriptors.class);

	private FL_PropertyDescriptors _descriptors;

	public ConfigFileDescriptors() {
		_descriptors = new FL_PropertyDescriptors();
	}
	
	public FL_PropertyDescriptors getEntityDescriptors() {
		return _descriptors;
	}

	public void readDescriptorsFromFile(String filename) throws IOException {

		try {

			// Open xml file for parsing
			File fXmlFile;
			fXmlFile = new File("./conf/" + filename);
			if (!fXmlFile.exists()) {
				URL fileURL = getClass().getResource("/" + filename);
				if (fileURL == null) {
					throw new IOException("could not find file " + "./conf/" + filename + " or " + "/" + filename);
				}
				fXmlFile = new File(fileURL.getFile());
			}

			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(fXmlFile);

			doc.getDocumentElement().normalize();

			NodeList typeNodes = doc.getElementsByTagName("type");
			NodeList groupNodes = doc.getElementsByTagName("group");
			NodeList propertyNodes = doc.getElementsByTagName("property");

			// -- Types ------------------------------------------------------------------------------------------------
			List<FL_TypeDescriptor> typeList = new ArrayList<FL_TypeDescriptor>();
			for (Element t : asList(typeNodes)) {

				String groupId = t.getAttribute("group");
				String groupName = "";
				boolean isExclusive = false;

				// Look up the group attribute
				for (Element group : asList(groupNodes)) {

					if (group.getAttribute("key").equals(groupId)) {
						groupName = group.getAttribute("friendlyText");
						if (groupName == null || groupName.isEmpty())
							groupName = groupId;
						isExclusive = group.getAttribute("exclusive").equals("true");
						break;
					}
				}

				FL_TypeDescriptor typeDesc = FL_TypeDescriptor.newBuilder()
								.setKey(t.getAttribute("key"))
								.setFriendlyText(t.getAttribute("friendlyText"))
								.setGroup(groupName)
								.setExclusive(isExclusive)
								.build();

				typeList.add(typeDesc);
			}

			// -- Properties -------------------------------------------------------------------------------------------
			List<FL_PropertyDescriptor> propertyList = new ArrayList<FL_PropertyDescriptor>();
			for (Element p : asList(propertyNodes)) {

				String dataType = p.getAttribute("dataType");

				FL_PropertyType ptype = FL_PropertyType.STRING;
				if (dataType.equalsIgnoreCase("string")) {
					ptype = FL_PropertyType.STRING;
				} else if (dataType.equalsIgnoreCase("long")) {
					ptype = FL_PropertyType.LONG;
				} else if (dataType.equalsIgnoreCase("double")) {
					ptype = FL_PropertyType.DOUBLE;
				} else if (dataType.equalsIgnoreCase("boolean")) {
					ptype = FL_PropertyType.BOOLEAN;
				} else if (dataType.equalsIgnoreCase("date")) {
					ptype = FL_PropertyType.DATE;
				} else if (dataType.equalsIgnoreCase("geo")) {
					ptype = FL_PropertyType.GEO;
				}
				//else if (dataType.equalsIgnoreCase("other")) {
				//    ptype = FL_PropertyType.OTHER;
				//}

				List<FL_TypeMapping> typeMappings = new ArrayList<FL_TypeMapping>();
				FL_PropertyDescriptor propDesc = FL_PropertyDescriptor.newBuilder()
						.setKey(p.getAttribute("key"))
						.setFriendlyText(p.getAttribute("friendlyText"))
						.setDefaultTerm(p.getAttribute("defaultTerm").equals("true"))
						.setFreeTextIndexed(p.getAttribute("freeTextIndexed").equals("true"))
						.setPropertyType(ptype)
						.setRange(FL_RangeType.SINGLETON)
						.setConstraint(FL_Constraint.FUZZY_PARTIAL_OPTIONAL)
						.setMemberOf(typeMappings)
						.build();

				// Parse the applicable types
				NodeList appliesToNodes = p.getElementsByTagName("appliesTo");
				for (Element a : asList(appliesToNodes)) {

					FL_TypeMapping typeMapping = FL_TypeMapping.newBuilder()
						.setType(a.getAttribute("typeKey"))
						.setMemberKey(a.getAttribute("memberKey"))
						.build();

					typeMappings.add(typeMapping);
				}

				propertyList.add(propDesc);
			}

			_descriptors.setTypes(typeList);
			_descriptors.setProperties(propertyList);

		} catch (Exception e) {
			s_logger.error("Error parsing definition file", e);
		}
	}

	public static List<Element> asList(NodeList n) {
		return n.getLength()==0?
				Collections.<Element>emptyList(): new NodeListWrapper(n);
	}
	static final class NodeListWrapper extends AbstractList<Element>
			implements RandomAccess {
		private final NodeList list;
		NodeListWrapper(NodeList l) {
			list=l;
		}
		public Element get(int index) {
			return (Element)list.item(index);
		}
		public int size() {
			return list.getLength();
		}
	}
}
