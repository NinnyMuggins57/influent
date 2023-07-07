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
package influent.entity.clustering;

import java.util.Collection;

import scala.Tuple2;

import com.oculusinfo.ml.Instance;
import com.oculusinfo.ml.feature.BagOfWordsFeature;
import com.oculusinfo.ml.feature.GeoSpatialFeature;
import com.oculusinfo.ml.feature.NumericVectorFeature;
import com.oculusinfo.ml.spark.SparkInstanceParser;
import com.oculusinfo.ml.spark.SparkInstanceParserHelper;

public class EntityInstanceParser extends SparkInstanceParser {
	private static final long serialVersionUID = 7832910126397517914L;
	
	private Collection<SchemaField> schema;
	
	public EntityInstanceParser(Collection<SchemaField> schema) {
		this.schema = schema;
	}

	@Override
	public Tuple2<String, Instance> call(String line) throws Exception {
		String str = line;
		String key = null;
		if (line.startsWith("(")) {
			key = str.substring(1, str.indexOf(","));
			str = str.substring(str.indexOf(",")+1);
		}
		if (line.endsWith(")")) {
			str = str.substring(0, str.length()-1);
		}
	
		SparkInstanceParserHelper parser = new SparkInstanceParserHelper(str);
		
		// each entity MUST have an id
		String id = parser.fieldToString("id");
		if (key == null) {
			key = id;
		}
		Instance inst = new Instance(id);
		
		for (SchemaField field : schema) {
			switch (field.fieldType) {
			case CC:
			case CATEGORY:
				BagOfWordsFeature cat = parser.fieldToBagOfWordsFeature(field.fieldName);
				if (cat != null) {
					inst.addFeature(cat);
				}
				break;
			case GEO:
				GeoSpatialFeature location = parser.fieldToGeoSpatialFeature(field.fieldName);
				if (location != null) {
					inst.addFeature(location);
				}
				break;
			case LABEL:
				BagOfWordsFeature label = parser.fieldToBagOfWordsFeature(field.fieldName);
				if (label != null) {
					inst.addFeature(label);
				}
				break;
			case NUMBER:
				NumericVectorFeature num = parser.fieldToNumericVectorFeature(field.fieldName);
				if (num != null) {
					inst.addFeature(num);
				}
				break;
			default:
				/* unsupported - ignore */
				break;
			}
		}
		return new Tuple2<String, Instance>(key, inst);
	}
}
