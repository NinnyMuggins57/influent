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
package influent.idlhelper;

import influent.idl.FL_Entity;
import influent.idl.FL_Link;
import influent.idl.FL_LinkTag;
import influent.idl.FL_Property;
import influent.idl.FL_PropertyTag;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class LinkHelper extends FL_Link {
	
	public LinkHelper(FL_LinkTag tag, String source, String target, List<FL_Property> props) {
		setTags(Collections.singletonList(tag));
		setDirected(true);
		setProvenance(null);
		setUncertainty(null);
		setSource(source);
		setTarget(target);
		setProperties(new ArrayList<FL_Property>(props));
	}

	public String toJson() throws IOException {
		return SerializationHelper.toJson(this);
	}
	
	public static String toJson(FL_Link link) throws IOException {
		return SerializationHelper.toJson(link);
	}
	
	public static String toJson(List<FL_Link> links) throws IOException {
		return SerializationHelper.toJson(links, FL_Link.getClassSchema());
	}
	
	public static String toJson(Map<String, List<FL_Link>> map) throws IOException {
		return SerializationHelper.toJson(map, FL_Link.getClassSchema());
	}

	public static FL_Entity fromJson(String json) throws IOException {
		return SerializationHelper.fromJson(json, FL_Entity.getClassSchema());
	}
	
	public static List<FL_Link> listFromJson(String json) throws IOException {
		return SerializationHelper.listFromJson(json, FL_Link.getClassSchema());
	}
	
	public static Map<String,List<FL_Link>> mapFromJson(String json) throws IOException {
		return SerializationHelper.mapFromJson(json, FL_Link.getClassSchema());
	}
	
	public static PropertyHelper getFirstProperty(FL_Link link, String key) {
		for (FL_Property property : link.getProperties()) {
			if (property.getKey() == key) return PropertyHelper.from(property);
		}
		return null;
	}
	
	public static PropertyHelper getFirstPropertyByTag(FL_Link link, FL_PropertyTag tag) {
		for (FL_Property property : link.getProperties()) {
			if (property.getTags().contains(tag)) return PropertyHelper.from(property);
		}
		return null;
	}
	
	public static List<PropertyHelper> getProperties(FL_Link link, String key) {
		List<PropertyHelper> matches = new ArrayList<PropertyHelper>();
		for (FL_Property property : link.getProperties()) {
			if (property.getKey()==key) matches.add(PropertyHelper.from(property));
		}
		return matches;
		
	}
}
