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
package influent.server.utilities;

import influent.idl.*;
import influent.idlhelper.PropertyHelper;
import influent.idlhelper.SingletonRangeHelper;

import java.util.ArrayList;
import java.util.List;

import org.apache.avro.specific.SpecificRecord;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


/**
 * Takes FL_* objects and converts them into JSON expected by the front-end 
 * 
 *
 */

public class UISerializationHelper {

	public static JSONObject toUIJson(FL_Cluster cluster) throws JSONException {
		JSONObject fle = new JSONObject();
		
		fle.put("uid", cluster.getUid());
		
		String entityType = "entity_cluster";
		if (cluster.getTags().contains(FL_EntityTag.CLUSTER_SUMMARY)) {
			entityType = "cluster_summary";
		} else if (cluster.getTags().contains(FL_EntityTag.ACCOUNT_OWNER)) {
			entityType = "account_owner";
		}
		
		fle.put("isRoot", cluster.getParent() == null);
		fle.put("entityType", entityType);
		fle.put("members", cluster.getMembers());
		fle.put("subclusters", cluster.getSubclusters());
		
		if (cluster.getUncertainty() != null) {
			fle.put("uncertainty", new JSONObject(cluster.getUncertainty().toString()));
		}
		
		if (cluster.getTags().contains(FL_EntityTag.PROMPT_FOR_DETAILS)) {
			fle.put("promptForDetails", true);
		}
		
		JSONObject props = new JSONObject();
		fle.put("properties", props);
		
		// Append number of cluster count to label (temporary fix for #6942)
		final FL_Property countProp = PropertyHelper.getPropertyByKey(cluster.getProperties(), "count");
		FL_Property labelProp = PropertyHelper.getPropertyByKey(cluster.getProperties(), "LABEL");
		if (labelProp != null && countProp != null)	{
			
			long count = (long)(Long)PropertyHelper.from(countProp).getValue();
			
			if (count > 1) {
				String labelStr = PropertyHelper.from(labelProp).getValue().toString();

				if (!(labelStr.contains("(+") && labelStr.charAt(labelStr.length() - 1) == ')')) {
					// For summary clusters, show number of accounts, otherwise show cluster count
					if (cluster.getTags().contains(FL_EntityTag.CLUSTER_SUMMARY)) {
						labelStr += " (" + (count - 1) + " accounts)";
						labelProp.setRange( new SingletonRangeHelper( labelStr, FL_PropertyType.STRING ) );
					} else {
						labelStr += " (+" + (count - 1) + ")";
						labelProp.setRange( new SingletonRangeHelper( labelStr, FL_PropertyType.STRING ) );
					}
				}
			}
		}
		
		for (FL_Property prop : cluster.getProperties()) {
			try {
				props.put(prop.getKey(), toUIJson(prop));
			} catch (JSONException e) {		
			}
		}

		return fle;
	}
	
	public static JSONObject toUIJson(FL_Entity entity) throws JSONException {
		JSONObject fle = new JSONObject();

		fle.put("uid", entity.getUid());

		fle.put("entityType", "entity");
		
		if (entity.getUncertainty() != null) {
			fle.put("uncertainty", new JSONObject(entity.getUncertainty().toString()));
		}
		
		if (entity.getTags().contains(FL_EntityTag.PROMPT_FOR_DETAILS)) {
			fle.put("promptForDetails", true);
		}
		
		JSONObject props = new JSONObject();
		fle.put("properties", props);
		
		for (int i = 0; i < entity.getProperties().size(); i++) {
			FL_Property prop = entity.getProperties().get(i);
			try {
				props.put(prop.getKey(), toUIJson(prop, i));
			} catch (JSONException e) {				
			}
		}
		
		return fle;
	}
	
	public static JSONObject toUIJson(FL_Property prop, int displayIndex) throws JSONException {
		JSONObject obj = toUIJson(prop);
		obj.put("displayOrder", displayIndex);
		return obj;
	}
	
	public static JSONObject toUIJson(FL_Property prop) throws JSONException {
		JSONObject json = new JSONObject();
		json.put("friendlyText", prop.getFriendlyText());
		
		if (prop instanceof SpecificRecord) {
			if (prop.getRange() instanceof FL_SingletonRange) {
				Object value =((FL_SingletonRange)prop.getRange()).getValue();
				
				if (value instanceof SpecificRecord) {
					value = new JSONObject(value.toString());
				}
				
				json.put("value", value);//getFriendlyValue(prop));
				
			} else {
				json.put("range", new JSONObject(prop.getRange().toString()));
			}
		}

		json.put("displayOrder", 0);
		json.put("uncertainty", prop.getUncertainty());
		json.put("tags", prop.getTags());
		
		return json;
	}
	
	public static JSONObject toUIJson(FL_PropertyDescriptor flpd) throws JSONException {
		JSONObject jo = new JSONObject();
		jo.put("key", flpd.getKey());
		jo.put("friendlyText", flpd.getFriendlyText());
		jo.put("constraint",  flpd.getConstraint());
		jo.put("range", flpd.getRange());
		jo.put("defaultTerm", flpd.getDefaultTerm());
		jo.put("freeTextIndexed", flpd.getFreeTextIndexed());

		JSONObject typeMappings = new JSONObject();
		jo.put("typeMappings", typeMappings);

		for (FL_TypeMapping desc : flpd.getMemberOf()) {
			try {
				typeMappings.put(desc.getType(), desc.getMemberKey());
			} catch (JSONException e) {
			}
		}

		return jo;
	}

	public static JSONObject toUIJson(FL_TypeDescriptor td) throws JSONException {
		JSONObject jo = new JSONObject();
		jo.put("key", td.getKey());
		jo.put("friendlyText", td.getFriendlyText());
		jo.put("group", td.getGroup());
		jo.put("exclusive", td.getExclusive());
		return jo;
	}

	public static JSONObject toUIJson(FL_Link link) throws JSONException {
		JSONObject fll = new JSONObject();
		
		fll.put("source", link.getSource());
		fll.put("target", link.getTarget());
		JSONObject props = new JSONObject();
		for (FL_Property prop : link.getProperties()) {
			try {
				props.put(prop.getKey(), toUIJson(prop));
			} catch (JSONException e) {				
			}
		}
		fll.put("properties", props);
		return fll;
	}
	
	public static List<String> buildListFromJson(JSONObject jsonObject, String listName) throws JSONException {
		JSONArray jsonArray = jsonObject.getJSONArray(listName);
		List<String> toReturn = new ArrayList<String>();
		for (int i = 0; i < jsonArray.length(); i++) {
			toReturn.add(jsonArray.getString(i));
		}
		return toReturn;
	}
}
