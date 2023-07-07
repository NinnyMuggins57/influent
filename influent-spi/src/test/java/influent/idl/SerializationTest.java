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
package influent.idl;

import influent.idl.FL_Entity;
import influent.idl.FL_EntityTag;
import influent.idl.FL_Property;
import influent.idlhelper.EntityHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.junit.Test;


public class SerializationTest {
	
	@Test
	public void serializeEntity() throws IOException {
		FL_Entity entity = FL_Entity.newBuilder()
			.setUid("ABCid")
			.setTags(Collections.singletonList(FL_EntityTag.ACCOUNT))
			.setProperties(new ArrayList<FL_Property>())
			.setProvenance(null)
			.setUncertainty(null)
			.build();

		// serialization: entity
		String json = EntityHelper.toJson(entity);
		
		EntityHelper.fromJson(json);
	}
	
	@Test
	public void serializeEntityList() throws IOException {
		FL_Entity entity = FL_Entity.newBuilder()
			.setUid("ABCid")
			.setTags(Collections.singletonList(FL_EntityTag.ACCOUNT))
			.setProperties(new ArrayList<FL_Property>())
			.setProvenance(null)
			.setUncertainty(null)
			.build();

		// serialization: entity list
		String json = EntityHelper.toJson(Arrays.asList(new FL_Entity[] {entity,entity}));
		
		EntityHelper.listFromJson(json);
	}
	
	@Test
	public void serializeEntityMap() throws IOException {
		FL_Entity entity = FL_Entity.newBuilder()
			.setUid("ABCid")
			.setTags(Collections.singletonList(FL_EntityTag.ACCOUNT))
			.setProperties(new ArrayList<FL_Property>())
			.setProvenance(null)
			.setUncertainty(null)
			.build();

		// serialization: entity map
		HashMap<String, List<FL_Entity>> map = new HashMap<String,List<FL_Entity>>();
		map.put("test", Arrays.asList(new FL_Entity[] {entity,entity}));
		String json = EntityHelper.toJson(map);
		
		EntityHelper.mapFromJson(json);
	}
}
