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
package influent.idlhelper;

import influent.idl.FL_GeoData;
import influent.idl.FL_PropertyType;
import influent.idl.FL_SingletonRange;

import java.util.Date;

public class SingletonRangeHelper extends FL_SingletonRange {

	public SingletonRangeHelper(Object value) {
		FL_PropertyType type = FL_PropertyType.STRING;
		
		if (value != null && !(value instanceof String)) {
			type = FL_PropertyType.STRING;
			
			if (value instanceof Number) {
				Number number = (Number)value;
				
				if (number instanceof Integer) {
					type = FL_PropertyType.LONG;
					value = Long.valueOf(number.longValue());
					
				} else if (number instanceof Long) {
					type = FL_PropertyType.LONG;
					
				} else {
					type = FL_PropertyType.DOUBLE;
					value = Double.valueOf(number.doubleValue());
				}
				
			} else if (value instanceof Boolean) {
				type = FL_PropertyType.BOOLEAN;
				
			} else if (value instanceof Date) {
				type = FL_PropertyType.DATE;
				value = Long.valueOf(((Date)value).getTime());
				
			} else if (value instanceof FL_GeoData) {
				type = FL_PropertyType.GEO;
				
			} else {
				value = value.toString();
			}
		}
		
		setValue(value);
		setType(type);
	}
	public SingletonRangeHelper(Object value, FL_PropertyType type) {
		setValue(value);
		setType(type);
	}

	public static Object rangeValue(Object range) {
		return range instanceof FL_SingletonRange? ((FL_SingletonRange)range).getValue() : null;
	}
	
	public static String toString(Object range) {
		return String.valueOf(rangeValue(range));
	}
}
