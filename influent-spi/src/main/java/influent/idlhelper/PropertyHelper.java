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
import influent.idl.FL_DistributionRange;
import influent.idl.FL_GeoData;
import influent.idl.FL_ListRange;
import influent.idl.FL_Property;
import influent.idl.FL_PropertyTag;
import influent.idl.FL_PropertyType;
import influent.idl.FL_Provenance;
import influent.idl.FL_SingletonRange;
import influent.idl.FL_Uncertainty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class PropertyHelper extends FL_Property {

	public PropertyHelper(String key, String friendlyText, Object value, FL_PropertyType type, FL_Provenance provenance, FL_Uncertainty uncertainty, List<FL_PropertyTag> tags) {
		setKey(key);
		setFriendlyText(friendlyText);
		setProvenance(provenance);
		setUncertainty(uncertainty);
		setTags(tags);
		setRange(new SingletonRangeHelper(value, type));
	}

	public PropertyHelper(String key, Object value, FL_PropertyTag tag) {
		setKey(key);
		setFriendlyText(key.replaceAll("([a-z])([A-Z0-9])","$1 $2").replace('_',' '));
		setProvenance(null);
		setUncertainty(null);
		setTags(new ArrayList<FL_PropertyTag>(2));
		setRange(new SingletonRangeHelper(value));
		
		getTags().add(tag);
	}

	public PropertyHelper(String key, String friendlyText, Object value, FL_PropertyType type, FL_PropertyTag tag) {
		setKey(key);
		setFriendlyText(friendlyText);
		setProvenance(null);
		setUncertainty(null);
		setTags(new ArrayList<FL_PropertyTag>(2));
		setRange(new SingletonRangeHelper(value, type));
		
		getTags().add(tag);
	}

	public PropertyHelper(String key, String friendlyText, Object value, FL_PropertyType type, List<FL_PropertyTag> tags) {
		setKey(key);
		setFriendlyText(friendlyText);
		setProvenance(null);
		setUncertainty(null);
		setTags(tags);
		setRange(new SingletonRangeHelper(value, type));
	}

	public PropertyHelper(String key, String friendlyText, Object startValue, Object endValue, FL_PropertyType type, List<FL_PropertyTag> tags) {
		setKey(key);
		setFriendlyText(friendlyText);
		setProvenance(null);
		setUncertainty(null);
		setTags(tags);
		setRange(FL_BoundedRange.newBuilder().setStart(startValue).setEnd(endValue).setType(type));
	}

	public PropertyHelper(String key, String friendlyText, FL_Provenance provenance, FL_Uncertainty uncertainty, List<FL_PropertyTag> tags, Object range) {
		setKey(key);
		setFriendlyText(friendlyText);
		setProvenance(provenance);
		setUncertainty(uncertainty);
		setTags(tags);
		setRange(range);
	}
	
	public static PropertyHelper from(FL_Property property) {
		if (property == null) return null;
		if (property instanceof PropertyHelper) return (PropertyHelper) property;
		
		return new PropertyHelper(
				property.getKey(),
				property.getFriendlyText(),
				property.getProvenance(),
				property.getUncertainty(),
				property.getTags(),
				property.getRange());
	}

	public PropertyHelper(FL_PropertyTag tag, String value) {
		this(tag.name(), tag.name(), value, Collections.singletonList(tag));
	}

	public PropertyHelper(FL_PropertyTag tag, double value) {
		this(tag.name(), tag.name(), value, Collections.singletonList(tag));
	}

	public PropertyHelper(FL_PropertyTag tag, Date value) {
		this(tag.name(), tag.name(), value, Collections.singletonList(tag));
	}

	
	public PropertyHelper(String key, String friendlyText, String value, List<FL_PropertyTag> tags) {
		this(key, friendlyText, value, FL_PropertyType.STRING, tags);
	}
	
	public PropertyHelper(String key, String friendlyText, double value, List<FL_PropertyTag> tags) {
		this(key, friendlyText, value, FL_PropertyType.DOUBLE, tags);
	}
	
	public PropertyHelper(String key, String friendlyText, Date date, List<FL_PropertyTag> tags) {
		this(key, friendlyText, date.getTime(), FL_PropertyType.DATE, tags);
	}
	
	public PropertyHelper(String key, String friendlyText, long value, List<FL_PropertyTag> tags) {
		this(key, friendlyText, value, FL_PropertyType.LONG, tags);
	}
	
	public PropertyHelper(String key, String friendlyText, FL_GeoData value, List<FL_PropertyTag> tags) {
		this(key, friendlyText, value, FL_PropertyType.GEO, tags);
	}

	public static FL_Property getPropertyByKey(List<FL_Property> props, String key) {
		if (props == null) {
			return null;
		}
		for (FL_Property prop : props) {
			if (prop.getKey().equalsIgnoreCase(key)) {
				return prop;
			}
		}
		return null;
	}

	public FL_PropertyType getType() {
		Object range = getRange();
		if (range instanceof FL_SingletonRange)
			return ((FL_SingletonRange)range).getType();
		else if (range instanceof FL_ListRange)
			return ((FL_ListRange)range).getType();
		else if (range instanceof FL_BoundedRange)
			return ((FL_BoundedRange)range).getType();
		else if (range instanceof FL_DistributionRange) 
			return ((FL_DistributionRange)range).getType();
		return null;
	}

	public Object getValue() {
		Object range = getRange();
		if (range == null) return null;
		
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
		else if (range instanceof FL_DistributionRange) {
			FL_DistributionRange dist = (FL_DistributionRange)range;
			return dist.getDistribution();
		}
		
		return null;
	}
	public List<Object> getValues() {
		Object range = getRange();
		
		if (range != null) {
			if (range instanceof FL_SingletonRange) {
				return Collections.singletonList(((FL_SingletonRange)range).getValue());
			}
			else if (range instanceof FL_ListRange) {
				return ((FL_ListRange)range).getValues();
			}
			else if (range instanceof FL_BoundedRange) {
				FL_BoundedRange bounded = (FL_BoundedRange)range;
				return Arrays.asList(bounded.getStart(), bounded.getEnd());
			}
			else if (range instanceof FL_DistributionRange) {
				FL_DistributionRange dist = (FL_DistributionRange)range;
				List<Object> values = new ArrayList<Object>(dist.getDistribution().size());
				values.addAll(dist.getDistribution());
				return values;
			}
		}
		
		return Collections.emptyList();
	}

	public boolean hasTag(FL_PropertyTag tag) {
		return getTags().contains(tag);
	}

	public boolean hasValue() {
		return getValue() != null;
	}
}
