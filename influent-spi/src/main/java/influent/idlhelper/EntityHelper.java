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
import influent.idl.FL_EntityTag;
import influent.idl.FL_Property;
import influent.idl.FL_PropertyTag;
import influent.idl.FL_Provenance;
import influent.idl.FL_Uncertainty;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class EntityHelper extends FL_Entity {

	public EntityHelper(String id, List<FL_EntityTag> tagList, FL_Provenance provenance, FL_Uncertainty uncertainty, List<FL_Property> properties) {
		super(id, new ArrayList<FL_EntityTag>(tagList), provenance, uncertainty, new ArrayList<FL_Property>(properties));
	}

	public EntityHelper(String id, String label, String type, List<FL_EntityTag> tagList, List<FL_Property> properties) {
		this(id, tagList, null, null, merge(properties, Arrays.asList(new FL_Property[] {
				new PropertyHelper(FL_PropertyTag.LABEL, label),
				new PropertyHelper(FL_PropertyTag.TYPE, type)
		})));
	}

	private static List<FL_Property> merge(List<FL_Property> list1, List<FL_Property> list2) {
		List<FL_Property> merged = new ArrayList<FL_Property>(list1);
		merged.addAll(list2);
		return merged;
	}

	public EntityHelper(String id, String label, String type, FL_EntityTag tag, List<FL_Property> properties) {
		this(id, label, type, Collections.singletonList(tag), new ArrayList<FL_Property>(properties));
	}

	public PropertyHelper getFirstProperty(String key) {
		for (FL_Property property : getProperties()) {
			if (property.getKey().equals(key)) return PropertyHelper.from(property);
		}
		return null;
	}

	public String getId() {
		return (String)getUid();
	}

	public String getLabel() {
		PropertyHelper label = getFirstProperty(FL_PropertyTag.LABEL.name());
		return (String) (label != null ? label.getValue() : null); 
	}
	
	public String toJson() throws IOException {
		return SerializationHelper.toJson(this);
	}
	
	public static String toJson(FL_Entity entity) throws IOException {
		return SerializationHelper.toJson(entity);
	}
	
	public static String toJson(List<FL_Entity> entities) throws IOException {
		return SerializationHelper.toJson(entities, FL_Entity.getClassSchema());
	}
	
	public static String toJson(Map<String, List<FL_Entity>> entities) throws IOException {
		return SerializationHelper.toJson(entities, FL_Entity.getClassSchema());
	}
	
	public static FL_Entity fromJson(String json) throws IOException {
		return SerializationHelper.fromJson(json, FL_Entity.getClassSchema());
	}
	
	public static List<FL_Entity> listFromJson(String json) throws IOException {
		return SerializationHelper.listFromJson(json, FL_Entity.getClassSchema());
	}
	
	public static Map<String,List<FL_Entity>> mapFromJson(String json) throws IOException {
		return SerializationHelper.mapFromJson(json, FL_Entity.getClassSchema());
	}
	
	public static PropertyHelper getFirstProperty(FL_Entity entity, String key) {
		for (FL_Property property : entity.getProperties()) {
			if (property.getKey().equals(key)) return PropertyHelper.from(property);
		}
		return null;
	}
	
	public static PropertyHelper getFirstPropertyByTag(FL_Entity entity, FL_PropertyTag tag) {
		for (FL_Property property : entity.getProperties()) {
			if (property.getTags().contains(tag)) return PropertyHelper.from(property);
		}
		return null;
	}
	
}
