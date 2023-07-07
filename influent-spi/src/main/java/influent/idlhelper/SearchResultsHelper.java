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

import influent.idl.FL_SearchResults;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class SearchResultsHelper extends FL_SearchResults {
	
	public String toJson() throws IOException {
		return SerializationHelper.toJson(this);
	}
	
	public static String toJson(FL_SearchResults descriptor) throws IOException {
		return SerializationHelper.toJson(descriptor);
	}

	public static String toJson(List<FL_SearchResults> descriptors) throws IOException {
		return SerializationHelper.toJson(descriptors, FL_SearchResults.getClassSchema());
	}

	public static String toJson(Map<String, List<FL_SearchResults>> map) throws IOException {
		return SerializationHelper.toJson(map, FL_SearchResults.getClassSchema());
	}
	
	public static FL_SearchResults fromJson(String json) throws IOException {
		return SerializationHelper.fromJson(json, FL_SearchResults.getClassSchema());
	}
	
	public static List<FL_SearchResults> listFromJson(String json) throws IOException {
		return SerializationHelper.listFromJson(json, FL_SearchResults.getClassSchema());
	}
	
	public static Map<String, List<FL_SearchResults>> mapFromJson(String json) throws IOException {
		return SerializationHelper.mapFromJson(json, FL_SearchResults.getClassSchema());
	}
}
