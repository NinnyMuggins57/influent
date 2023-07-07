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

import java.util.List;
import java.util.Map;

public interface DataNamespaceHandler {

	/**
	 * Creates a globally unique entity id given a local entity id and a namespace.
	 * The default implementation here simply returns the typed local entity id.
	 * An example override might look like this:
	 * <code>
	 * 		return namespace + "." + entityType + "." + localEntityId;
	 * </code>
	 * 
	 * @param namespace
	 * 		the namespace the entity belongs to
	 * 
	 * @param localEntityId
	 * 		the entity id as it exists in the data store
	 * 
	 * @param entityType
	 * 		the type of entity : see influent.server.utilities.TypedId types
	 * 
	 * @return
	 * 		a globally unique id used for reference in the system
	 */
	public String globalFromLocalEntityId(String namespace, String localEntityId, char entityType);

	/**
	 * Translates an entity id from global to local namespace for querying in
	 * the Data View. The default implementation returns the local id.
	 * 
	 * An example override might look like this:
	 * <code>
	 *     final int i = globalEntityId.indexOf('.');
	 *
	 *     if (i >= 0) {
	 *         return TypedId.fromTypedId(globalEntityId.substring(i+1)).getNativeId();
	 *     }
	 *     return super.localFromGlobalEntityId(globalEntityId);
	 * </code>
	 * 
	 * @param globalEntityId
	 * 		the globally unique entity id
	 * 
	 * @return
	 * 		the entity id as it exists in the data store
	 */
	public String localFromGlobalEntityId(String globalEntityId);

	/**
	 * Extracts and returns a namespace from a global entity id, typically formed
	 * by a call to createGlobalEntityId. The default implementation here returns
	 * null.
	 * 
	 * An example override might look like this:
	 * <code>
	 *     final int i = globalEntityId.indexOf('.');
	 *
	 *     if (i >= 0) {
	 *         return globalEntityId.substring(0, i);
	 *     }
	 *     return super.namespaceFromGlobalEntityId(globalEntityId);
	 * </code>
	 * 
	 * @param globalEntityId
	 * 		the globally unique entity id
	 * 
	 * @return
	 */
	public String namespaceFromGlobalEntityId(String globalEntityId);

	/**
	 * Returns a data view table name, given a namespace to consider. The
	 * default implementation here maps the name using any mappings defined
	 * in construction, then prepends the namespace as a db schema in the form
	 * namespace.tableName if a namespace is provided. 
	 * @param namespace TODO
	 * @param standardTableName
	 * 		The standard data view table name.
	 * 
	 * @return
	 * 		The localized data view table name.
	 */
	public String tableName(String namespace, /*String namespace,*/String standardTableName);
	
	/**
	 * Returns a data view column name. The default implementation here maps the name using 
	 * any mappings defined in construction.
	 *
	 * @param standardColumnName
	 * 		The standard data view column name.
	 * 
	 * @return
	 * 		The localized data view column name.
	 */
	public String columnName(String standardColumnName);

	/**
	 * Localizes and organizes entities by namespace
	 */
	public Map<String, List<String>> entitiesByNamespace(List<String> entities);

	/**
	 * Escapes the column name in a db specific manner. This method is 
	 * provided for convenience.
	 */
	public String escapeColumnName(String columnName);

	/**
	 * Creates a temp table name in a db specific manner. This method is 
	 * provided for convenience.
	 */
	public String createTempTable(String tableName, String spec);
	
	/**
	 * Returns a temp table name in a db specific manner. This method is 
	 * provided for convenience.
	 */
	public String tempTableName(String tableName);

	/**
	 * Sets a row limit in a db specific manner. This method is 
	 * provided for convenience.
	 * 
	 * @param selectBody
	 * 		the body of the select statement, minus the leading select keyword itself
	 * @returns
	 * 		a full row limited statement, including the select keyword.
	 */
	public String rowLimit(String selectBody, long limit);
}