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

import influent.idl.FL_BoundedRange;
import influent.idl.FL_Constraint;
import influent.idl.FL_ListRange;
import influent.idl.FL_PropertyMatchDescriptor;
import influent.idl.FL_PropertyType;
import influent.idl.FL_SingletonRange;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class PropertyMatchDescriptorHelper extends FL_PropertyMatchDescriptor {

	public static PropertyMatchDescriptorHelper from(FL_PropertyMatchDescriptor descriptor) {
		if (descriptor instanceof PropertyMatchDescriptorHelper) return (PropertyMatchDescriptorHelper)descriptor;
		
		PropertyMatchDescriptorHelper helper = new PropertyMatchDescriptorHelper();
		helper.setKey(descriptor.getKey());
		helper.setConstraint(descriptor.getConstraint());
		helper.setVariable(descriptor.getVariable());
		helper.setInclude(descriptor.getInclude());
		helper.setRange(descriptor.getRange());
		return helper;
	}
	
	public String toJson() throws IOException {
		return SerializationHelper.toJson(this);
	}
	
	public static String toJson(FL_PropertyMatchDescriptor descriptor) throws IOException {
		return SerializationHelper.toJson(descriptor);
	}
	
	public static String toJson(List<FL_PropertyMatchDescriptor> descriptors) throws IOException {
		return SerializationHelper.toJson(descriptors, FL_PropertyMatchDescriptor.getClassSchema());
	}

	public static String toJson(Map<String, List<FL_PropertyMatchDescriptor>> map) throws IOException {
		return SerializationHelper.toJson(map, FL_PropertyMatchDescriptor.getClassSchema());
	}
	
	public static FL_PropertyMatchDescriptor fromJson(String json) throws IOException {
		return SerializationHelper.fromJson(json, FL_PropertyMatchDescriptor.getClassSchema());
	}
	
	public static List<FL_PropertyMatchDescriptor> listFromJson(String json) throws IOException {
		return SerializationHelper.listFromJson(json, FL_PropertyMatchDescriptor.getClassSchema());
	}
	
	public static Map<String, List<FL_PropertyMatchDescriptor>> mapFromJson(String json) throws IOException {
		return SerializationHelper.mapFromJson(json, FL_PropertyMatchDescriptor.getClassSchema());
	}

	/**
	 * Returns an FL_*Range Object representing a set of basic string terms
	 * 
	 * @param terms
	 * 		the terms to represent
	 * @return
	 * 		an FL_*Range Object - either a singleton or list
	 */
	public static Object rangeFromBasicTerms(String terms) {
		
		// if quoted, return as one term
		if (terms.charAt(0) == '"') {
			int end = terms.length() 
					- (terms.charAt(terms.length()-1) == '"'? 1: 0);
			
			return FL_SingletonRange.newBuilder()
				.setType(FL_PropertyType.STRING)
				.setValue(terms.substring(1, end))
				.build();
		}
		
		final String tokens[] = terms.split("\\s+");

		// else break by whitespace
		switch (tokens.length) {
		case 0:
			return null;
			
		case 1:
			return FL_SingletonRange.newBuilder()
				.setType(FL_PropertyType.STRING)
				.setValue(tokens[0])
				.build();
			
		default:
			return FL_ListRange.newBuilder()
				.setType(FL_PropertyType.STRING)
				.setValues(Arrays.asList(Arrays.copyOf(tokens, tokens.length, Object[].class)))
				.build();
		}
	}
	
	/**
	 * Returns true if the match descriptor is exclusive, accounting for both the include
	 * property and the NOT constraint. 
	 * 
	 * Match descriptors have a NOT constraint which is redundant with the include boolean
	 * for searches other than pattern searches. Here we interpret "NOT exclude" as "NOT/exclude", since
	 * otherwise the criteria would have no effect at all.
	 * 
	 * @param descriptor
	 * 		The match specification
	 * 
	 * @return
	 * 		true if an exclusion
	 */
	public static boolean isExclusion(FL_PropertyMatchDescriptor descriptor) {
		return FL_Constraint.NOT.equals(descriptor) || !descriptor.getInclude();
	}
	
	public FL_PropertyType getType() {
		Object range = getRange();
		if (range instanceof FL_SingletonRange)
			return ((FL_SingletonRange)range).getType();
		else if (range instanceof FL_ListRange)
			return ((FL_ListRange)range).getType();
		else if (range instanceof FL_BoundedRange)
			return ((FL_BoundedRange)range).getType();
		return null;
	}

	public Object getValue() {
		Object range = getRange();
		if (range instanceof FL_SingletonRange) {
			return ((FL_SingletonRange)range).getValue();
		}
		else if (range instanceof FL_ListRange) {
			return ((FL_ListRange)range).getValues().iterator().next();
		}
		else if (range instanceof FL_BoundedRange) {
			FL_BoundedRange bounded = (FL_BoundedRange)range;
			return bounded.getStart() != null ? bounded.getStart() : bounded.getEnd();
		}
		return null;
	}
}
