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

import influent.idl.FL_Constraint;
import influent.idl.FL_ListRange;
import influent.idl.FL_PropertyMatchDescriptor;
import influent.idl.FL_SingletonRange;
import influent.idl.FL_TypeMapping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 */
public class SolrUtils {

	  /**
	   * See org.apache.solr.client.solrj.util.ClientUtils
	   * See: {@link org.apache.lucene.queryparser.classic queryparser syntax} 
	   * for more information on Escaping Special Characters
	   */
	  public static String escapeQueryChars(String s) {
	    StringBuilder sb = new StringBuilder();
	    for (int i = 0; i < s.length(); i++) {
	      char c = s.charAt(i);
	      // These characters are part of the query syntax and must be escaped
	      if (c == '\\' || c == '+' || c == '-' || c == '!'  || c == '(' || c == ')' || c == ':'
	        || c == '^' || c == '[' || c == ']' || c == '\"' || c == '{' || c == '}' || c == '~'
	        || c == '*' || c == '?' || c == '|' || c == '&'  || c == ';' || c == '/') {
	        sb.append('\\');
	      }
	      sb.append(c);
	    }
	    return sb.toString();
	  }

	  
	/**
	 * Returns a Solr query clause to represent the descriptor supply.
	 * 
	 * @param descriptor
	 * 		The match specification
	 * 
	 * @return
	 */
	public static String toSolrClause(FL_PropertyMatchDescriptor descriptor) {
		
		String k = descriptor.getKey();
		
		Collection<Object> values;
	
		final Object r = descriptor.getRange();
		
		if (r instanceof FL_SingletonRange) {
			values = Collections.singleton(((FL_SingletonRange)r).getValue());
		} else if (r instanceof FL_ListRange) {
			values = ((FL_ListRange)r).getValues();
			
		// TODO: account for bounded ranges
		} else {
			values = null; 
		}
		
	
		final StringBuilder s = new StringBuilder();
		
		// fuzzy?
		if (PropertyMatchDescriptorHelper.isExclusion(descriptor)) {
			s.append("-");
		}
		
		s.append(k);
		
		if (FL_Constraint.FUZZY_PARTIAL_OPTIONAL.equals(descriptor.getConstraint()) || FL_Constraint.FUZZY_REQUIRED.equals(descriptor.getConstraint())) {
			s.append(":(");

			for (Object value : values) {
				s.append("(");
				String valueStr = (String)value;
				String[] tokens = escapeQueryChars(valueStr).split("\\s");
				for (int i = 0; i < tokens.length; i++) {
					if (tokens[i].length() == 0)
						continue;

					s.append(tokens[i]);
					s.append("~");
					if (descriptor.getSimilarity() != null)
						s.append(descriptor.getSimilarity());

					if (i < tokens.length - 1) {
						s.append(" AND ");
					}
				}
				s.append(") ");
			}
			
			s.append(")");
			
		} else { // not | required / equals
			s.append(":(");
			
			for (Object value : values) {
				s.append("\"");
				s.append(value);
				s.append("\" ");
			}
			
			s.setLength(s.length()-1);
			s.append(")");
		}
			
	
		if (descriptor.getWeight() != null && descriptor.getWeight() != 1.0) {
			s.append("^");
			s.append(descriptor.getWeight());
		}
		
		return s.toString();
	}

	/**
	 * Returns an OR'd series of Solr clauses to represent the map of terms specified.

	 * @param terms
	 * 		A list of explicitly defined terms
	 *
	 * @return
	 * 		The solr query string or null if not a valid query.
	 */
	public static String toSolrQuery(List<FL_PropertyMatchDescriptor> terms) {

		Map<String, List<FL_PropertyMatchDescriptor>> typePropMap = new HashMap<String, List<FL_PropertyMatchDescriptor>>();

		// Create map of properties by type
		for (FL_PropertyMatchDescriptor term : terms) {
			for (FL_TypeMapping td : term.getTypeMappings()) {
				List<FL_PropertyMatchDescriptor> typedTerms = typePropMap.get(td.getType());

				if (typedTerms == null) {
					typedTerms = new ArrayList<FL_PropertyMatchDescriptor>();
				}

				FL_PropertyMatchDescriptor.Builder termBuilder = FL_PropertyMatchDescriptor.newBuilder(term);

				termBuilder.setKey(td.getMemberKey());

				typedTerms.add(termBuilder.build());

				typePropMap.put(td.getType(), typedTerms);
			}
		}

		StringBuilder query = new StringBuilder();
		boolean subsequentType = false;

		// Build the query by type
        for (Map.Entry<String, List<FL_PropertyMatchDescriptor>> entry : typePropMap.entrySet()) {

	        // Create a query fragment for this type
	        StringBuilder queryFragment = new StringBuilder();

	        List<FL_PropertyMatchDescriptor> termsByType = entry.getValue();

			final List<FL_PropertyMatchDescriptor> orsAnds = new ArrayList<FL_PropertyMatchDescriptor>(termsByType.size());
			final List<FL_PropertyMatchDescriptor> nots = new ArrayList<FL_PropertyMatchDescriptor>(termsByType.size());
			
			// separate ors from nots
			for (FL_PropertyMatchDescriptor term : termsByType) {
				(PropertyMatchDescriptorHelper.isExclusion(term)? nots: orsAnds).add(term);
			}
		
			// not valid
			if (orsAnds.isEmpty()) {
				return null;
			}
		
			// orsAnds
			for (FL_PropertyMatchDescriptor term : orsAnds) {
				queryFragment.append(toSolrClause(term));
				if (term.getConstraint().equals(FL_Constraint.OPTIONAL_EQUALS) || term.getConstraint().equals(FL_Constraint.FUZZY_PARTIAL_OPTIONAL)) {
					queryFragment.append(" OR ");
				} else {
					queryFragment.append(" AND ");
				}
				
			}
			
			// trim last OR/AND
	        queryFragment.setLength(queryFragment.length() - 4);
		
			// nots
			if (!nots.isEmpty()) {
				queryFragment.insert(0, '(');
				queryFragment.append(')');
				
				for (FL_PropertyMatchDescriptor term : nots) {
					queryFragment.append(" AND ");
					queryFragment.append(toSolrClause(term));
				}
			}


	        // Check fragment is not already in the query
	        if (query.indexOf(queryFragment.toString()) == -1) {

		        // bracket the fragment
		        if (subsequentType) {
			        query.append(" OR (");
		        } else {
			        query.append(" (");
			        subsequentType = true;
		        }

		        // Add fragment to query
		        query.append(queryFragment);

		        query.append(") ");

	        }
		}
		
		return query.toString();
	}
}
