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

import oculus.aperture.spi.common.Properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author djonker
 *
 */
public abstract class AbstractDataNamespaceHandler implements DataNamespaceHandler {
	private static Logger s_logger = LoggerFactory.getLogger(AbstractDataNamespaceHandler.class);
	private static final String TABLE_NAME_CONFIG = "influent.data.view.tables";
	private static final String ID_TYPE_CONFIG = "influent.data.view.idType";
	
	public enum ID_TYPE {
		NUMERIC, STRING, HEX // binary/raw referred to locally in hex without the 0x prefix, like '7D'
	}

	private final Map<String, String> _tableNames;
	protected final ID_TYPE _idType;
	private final Properties _config;
	
	
	/**
	 * Constructs a handler which uses standard table names.
	 */
	public AbstractDataNamespaceHandler() {
		_tableNames = Collections.emptyMap();
		_idType = ID_TYPE.STRING;
		_config = null;
	}
	
	
	
	
	/**
	 * Creates a new handler based on a configuration defined as a JSON object string
	 * mapping standard names to localized names.
	 * 
	 * @throws JSONException 
	 */
	@SuppressWarnings("unchecked")
	public AbstractDataNamespaceHandler(Properties config) throws JSONException {

		String tableNamesJson = config.getString(TABLE_NAME_CONFIG, "");
		String idType = config.getString(ID_TYPE_CONFIG, "STRING");

		_tableNames = new HashMap<String, String>();
		_config = config;

		// entity id type (default)
		ID_TYPE eIdType = ID_TYPE.STRING;
		try {
			eIdType = Enum.valueOf(ID_TYPE.class, idType.toUpperCase());
		} catch (Exception e) {
			s_logger.warn("Unrecognized " +ID_TYPE_CONFIG+ ": " +idType);
		}
		_idType = eIdType;
		
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
		_idType = ID_TYPE.STRING;
		_config = null;
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
	
	
	/* (non-Javadoc)
	 * @see influent.server.dataaccess.DataNamespaceHandler#getSQLIdType(java.lang.String, java.lang.String)
	 */

	public ID_TYPE getIdType(String namespace) {
		ID_TYPE type = _idType;
		if(namespace != null) {
			String idConfig = "influent.data.view." + namespace + ".idType";
			String idStr = _config.getString(idConfig, null);
			if(idStr != null) {
				try {
					type = Enum.valueOf(ID_TYPE.class, idStr.toUpperCase());;
				} catch (Exception e) {
					s_logger.debug("Unrecognized id type for " + idConfig + ": " + idStr);
				}
			}
		}
		
		return type;
	}
	
	
	/* (non-Javadoc)
	 * @see influent.server.dataaccess.DataNamespaceHandler#toSQLId(java.lang.String, java.lang.String)
	 */
	@Override
	public String toSQLId(String id, String namespace) {
		ID_TYPE type = getIdType(namespace);

		if (type == ID_TYPE.HEX) {
			return idToBinaryFromHex(id);
		} else {
			return id;
		}
	}
	

	
	/**
	 * Returns binary id sql from a hexadecimal string id. Called by toSQLId.
	 * 
	 * @param id
	 * @return
	 */
	protected abstract String idToBinaryFromHex(String id);

	
	
	/* (non-Javadoc)
	 * @see influent.server.dataaccess.DataNamespaceHandler#fromSQLId(java.lang.String)
	 */
	@Override
	public String fromSQLId(String id) {
		return id;
	}



    /* (non-Javadoc)
     * @see influent.server.dataaccess.DataNamespaceHandler#toSQLIdColumn(java.lang.String, java.lang.String)
     */
    @Override
    public String toSQLIdColumn(String columnName, String namespace) {
        ID_TYPE type = getIdType(namespace);

        if (type == ID_TYPE.HEX) {
            return columnToHex(columnName);
        } else {
            return columnName;
        }
    }

    /**
     * Returns binary-converted column sql. Called by toSQLIdColumn.
     *
     * @param columnName
     * @return
     */
    protected abstract String columnToHex(String columnName);


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
