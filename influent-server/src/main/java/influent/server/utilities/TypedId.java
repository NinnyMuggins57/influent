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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Native ids are typed for internal reference to make it easy to distinguish
 * between entity types, like accounts or clusters.
 * 
 * @author djonker
 * 
 */
public final class TypedId {

	/**
	 * The account id type.
	 */
	public final static char ACCOUNT = 'a';
	
	/**
	 * The account owner id type.
	 */
	public final static char ACCOUNT_OWNER = 'o';
	
	/**
	 * The cluster id type.
	 */
	public final static char CLUSTER = 'c';
	
	/**
	 * The cluster summary id type.
	 */
	public final static char CLUSTER_SUMMARY = 's';

	/**
	 * The file id type.
	 */
	public final static char FILE = 'f';
	
	
	private final char idType;
	private final String namespace;
	private final String nativeId;
	private final String typedId;
	
	
	
	/**
	 * Constructs from a native id, type, and namespace.
	 */
	public static TypedId fromNativeId(final char type, final String namespace, final String id) {
		return new TypedId(type, namespace, id);
	}
	
	
	
	
	/**
	 * Constructs from a native id and type.
	 */
	public static TypedId fromNativeId(final char type, final String id) {
		return new TypedId(type, null, id);
	}
	
	
	
	
	/**
	 * Constructs from a typed id.
	 */
	public static TypedId fromTypedId(final String typedId) {
		return new TypedId(typedId);
	}
	
	
	
	
	/**
	 * Converts a list of typed ids to native ids, returning a new list.
	 */
	public static List<String> nativeFromTypedIds(final Collection<String> typedIds) {
		final List<String> list = new ArrayList<String>(typedIds.size());
		
		for (String s : typedIds) {
			list.add(fromTypedId(s).nativeId);
		}
		
		return list;
	}
	
	
	
	
	/**
	 * Converts a list of typed ids to native ids, filtering for the specified type and returning a new list.
	 */
	public static List<String> nativeFromTypedIds(final Collection<String> typedIds, final char filterType) {
		final List<String> list = new ArrayList<String>(typedIds.size());
		
		for (String s : typedIds) {
			final TypedId typedId = fromTypedId(s);
			
			if (typedId.idType == filterType) {
				list.add(typedId.nativeId);
			}
		}
		
		return list;
	}
	
	
	
	
	/**
	 * Converts a list of typed ids to native ids, filtering for the specified type and returning a new list.
	 */
	public static List<String> filterTypedIds(final Collection<String> typedIds, final char filterType) {
		final List<String> list = new ArrayList<String>(typedIds.size());
		
		for (String s : typedIds) {
			final TypedId typedId = fromTypedId(s);
			
			if (typedId.idType == filterType) {
				list.add(typedId.typedId);
			}
		}
		
		return list;
	}
	
	
	
	
	/**
	 * Converts a list of native ids to typed ids, returning a new list.
	 */
	public static List<String> typedFromNativeIds(char type, String namespace, final Collection<String> nativeIds) {
		final List<String> list = new ArrayList<String>(nativeIds.size());
		
		for (String s : nativeIds) {
			list.add(fromNativeId(type, namespace, s).typedId);
		}
		
		return list;
	}
	
	
	
	/**
	 * Return whether the typed id has the specified type
	 */
	public static boolean hasType(String typedId, char type) {
		return (fromTypedId(typedId).getType() == type);
	}
	
	
	
	/**
	 * @return the type of id
	 */
	public char getType() {
		return idType;
	}
	
	
	
	
	/**
	 * @return the native id
	 */
	public String getNativeId() {
		return nativeId;
	}
	
	
	
	
	/**
	 * @return the typed id
	 */
	public String getTypedId() {
		return typedId;
	}
	
	
	
	
	/**
	 * @return the typed id
	 */
	public String getNamespace() {
		return namespace;
	}
	
	
	
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return typedId;
	}
	
	
	
	
	/**
	 * For clarity we wrap this constructor for access by a static method above.
	 */
	private TypedId(final String id) {
		this.typedId  = id;
		
		if (id != null && id.length() > 2) {
			String[] splits = id.split("\\.", 3);
			
			if (splits.length > 0) {
				this.idType = splits[0].charAt(0);
			} else {
				this.idType = ACCOUNT;
			}
			
			if (splits.length > 1 && !splits[1].equalsIgnoreCase("null")) {
				this.namespace = splits[1];
			} else {
				this.namespace = null;
			}
			
			if (splits.length > 2 && !splits[2].equalsIgnoreCase("null")) {
				this.nativeId = splits[2];
			} else {
				this.nativeId = null;
			}
		} else {
			this.nativeId = null;
			this.namespace = null;
			this.idType = ACCOUNT;
		}
	}
	
	
	
	
	/**
	 * For clarity we wrap this constructor for access by a static method above.
	 */
	private TypedId(final char type, final String namespace, final String id) {
		this.idType    = type;
		this.namespace = namespace;
		this.nativeId  = id;
		this.typedId   = "" + type + "." + namespace + "." + id;
	}
}
