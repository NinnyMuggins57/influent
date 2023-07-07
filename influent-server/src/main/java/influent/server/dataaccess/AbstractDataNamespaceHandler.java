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
package influent.server.dataaccess;

import influent.server.utilities.TypedId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author djonker
 *
 */
public abstract class AbstractDataNamespaceHandler implements DataNamespaceHandler {

	private final Map<String, String> _tableNames;
	
	
	
	/**
	 * Constructs a handler which uses standard table names.
	 */
	public AbstractDataNamespaceHandler() {
		_tableNames = Collections.emptyMap();
	}
	
	
	
	
	/**
	 * Creates a new handler based on a configuration defined as a JSON object string
	 * mapping standard names to localized names.
	 * 
	 * @throws JSONException 
	 */
	@SuppressWarnings("unchecked")
	public AbstractDataNamespaceHandler(String tableNamesJson) throws JSONException {
		_tableNames = new HashMap<String, String>();
		
		// parse table names
		JSONObject map = new JSONObject(tableNamesJson);
		
		for (Iterator<String> i = map.sortedKeys(); i.hasNext(); ) {
			String key = i.next();
			_tableNames.put(key, map.getString(key));
		}
	}
	
	
	
	
	/**
	 * Constructs a namespace handler from a
	 * map of standard names to localized names.
	 */
	public AbstractDataNamespaceHandler(Map<String, String> tableNames) {
		_tableNames = tableNames;
	}
	
	
	
	
	/* (non-Javadoc)
	 * @see influent.server.dataaccess.DataNamespaceHandler#globalFromLocalEntityId(java.lang.String, java.lang.String)
	 */
	@Override
	public String globalFromLocalEntityId(String namespace, String localEntityId, char entityType) {		
		return TypedId.fromNativeId(entityType, namespace, localEntityId).getTypedId();
	}
	
	
	
	
	/* (non-Javadoc)
	 * @see influent.server.dataaccess.DataNamespaceHandler#localFromGlobalEntityId(java.lang.String)
	 */
	@Override
	public String localFromGlobalEntityId(String globalEntityId) {
		return TypedId.fromTypedId(globalEntityId).getNativeId();
	}
	
	
	
	
	/* (non-Javadoc)
	 * @see influent.server.dataaccess.DataNamespaceHandler#namespaceFromGlobalEntityId(java.lang.String)
	 */
	@Override
	public String namespaceFromGlobalEntityId(String globalEntityId) {
		return TypedId.fromTypedId(globalEntityId).getNamespace();
	}
	
	
	
	
	/* (non-Javadoc)
	 * @see influent.server.dataaccess.DataNamespaceHandler#tableName(java.lang.String)
	 */
	@Override
	public String tableName(String namespace, String standardTableName) {
		String mappedName = _tableNames.get(standardTableName);
		
		if (mappedName == null) {
			mappedName = standardTableName;
		}

		if (namespace != null && !namespace.isEmpty()) {
			return namespace + "." + mappedName;
		}
			
		return mappedName;
	}
	
	
	
	
	/* (non-Javadoc)
	 * @see influent.server.dataaccess.DataNamespaceHandler#columnName(java.lang.String)
	 */
	@Override
	public String columnName(String standardColumnName) {
		String mappedName = _tableNames.get(standardColumnName);
		
		if (mappedName == null) {
			mappedName = standardColumnName;
		}
			
		return mappedName;
	}
	
	
	
	
	/* (non-Javadoc)
	 * @see influent.server.dataaccess.DataNamespaceHandler#entitiesByNamespace(java.util.List)
	 */
	@Override
	public Map<String, List<String>> entitiesByNamespace(List<String> entities) {
		Map<String, List<String>> namespaces = new LinkedHashMap<String, List<String>>();
		
		// global to local conversion, organized by namespace.
		if (entities != null) {
			for (String id : entities) {
				final String namespace = namespaceFromGlobalEntityId(id);
	
				// get the list of entities for this namespace.
				findOrCreateNamespace(namespace, namespaces).add(
						localFromGlobalEntityId(id));
			}
		}

		return namespaces;
	}
	
	
	
	
	/**
	 * Finds or creates a namespace in the existing map of namespaces.
	 */
	private List<String> findOrCreateNamespace(String namespace, Map<String, List<String>> namespaces) {
		List<String> ns = namespaces.get(namespace);

		if (ns == null) {
			ns = new ArrayList<String>();
			namespaces.put(namespace, ns);
		}
		
		return ns;
	}
}
