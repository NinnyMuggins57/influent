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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

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
	        || c == '*' || c == '?' || c == '|' || c == '&'  || c == ';' || c == '/'
	        || Character.isWhitespace(c)) {
	        sb.append('\\');
	      }
	      sb.append(c);
	    }
	    return sb.toString();
	  }

	  
	private static Pattern NUMBER_PATTERN = Pattern.compile("[0-9]");
	private static Pattern TERM_SEPARATOR = Pattern.compile("\\.\\s+|[,;:\\?\\s]+");

	/**
	 * Returns a Solr query clause to represent the descriptor supply.
	 * 
	 * @param descriptor
	 * 		The match specification
	 * 
	 * @return
	 */
	public static String toSolrClause(FL_PropertyMatchDescriptor descriptor) {
		return toSolrClause(descriptor, null);
	}

	/**
	 * Returns a Solr query clause to represent the descriptor supply. Since descriptors do not
	 * yet include weights the weight is supplied here as a separate parameter.
	 * 
	 * @param descriptor
	 * 		The match specification
	 * 
	 * @param weight
	 * 		The weight to give the parameter, where 1.0 is the default if unspecified
	 * 
	 * @return
	 */
	public static String toSolrClause(FL_PropertyMatchDescriptor descriptor, Double weight) {
		
		String k = (String) descriptor.getKey();
		
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
		
		if (FL_Constraint.FUZZY_PARTIAL_OPTIONAL.equals(descriptor.getConstraint())) {
			s.append(":(");
			
			for (Object v : values) {
				
				// add each term separately
				String wsTokens[] = TERM_SEPARATOR.split(v.toString());
				
				for (String token : wsTokens) {
					if (token.indexOf('-') == -1 || NUMBER_PATTERN.matcher(token).find()) {
						s.append(escapeQueryChars(token));
						s.append("~ ");
					} else {
						String hyphenTokens[] = token.split("-");
						for (String seg : hyphenTokens) {
							s.append(escapeQueryChars(seg));
							s.append("~ ");
						}
					}
				}
			}
			
			s.setLength(s.length()-1);
			s.append(")");
			
		} else { // not | required / equals
			s.append(":(");
			
			for (Object v : values) {
				s.append("\"");
				s.append(v);
				s.append("\" ");
			}
			
			s.setLength(s.length()-1);
			s.append(")");
		}
			
	
		if (weight != null && weight != 1.0) {
			s.append("^");
			s.append(weight);
		}
		
		return s.toString();
	}

	/**
	 * Returns an OR'd series of Solr clauses to represent the list of terms specified.
	 * 
	 * @param basicQuery
	 * 		A search over default fields supplied by basicQueryFieldWeights
	 * 
	 * @param basicQueryFieldWeights
	 * 		A map of default field names to weightings, where 1.0 is the default weight.
	 * 
	 * @param advancedTerms
	 * 		A list of explicitly defined terms
	 * 
	 * @return
	 * 		The solr query string or null if not a valid query.
	 */
	public static String toSolrQuery(String basicQuery, Map<String, Double> basicQueryFieldWeights, List<FL_PropertyMatchDescriptor> advancedTerms) {
		
		// copy terms to merge basic with advanced.
		final List<FL_PropertyMatchDescriptor> ors = 
				new ArrayList<FL_PropertyMatchDescriptor>(advancedTerms.size());
			final List<FL_PropertyMatchDescriptor> nots = 
				new ArrayList<FL_PropertyMatchDescriptor>(advancedTerms.size());
				
		// first add basic query terms.
		if (basicQuery != null && !basicQuery.isEmpty()) {
			final Object values = PropertyMatchDescriptorHelper.rangeFromBasicTerms(basicQuery);
			
			if (values != null) {
				for (String key : basicQueryFieldWeights.keySet()) {
					ors.add(
						FL_PropertyMatchDescriptor.newBuilder()
							.setConstraint(FL_Constraint.REQUIRED_EQUALS)
							.setKey(key)
							.setRange(values)
							.build()
					);
				}
			}
		}
		
		
		// now form it into a query
		StringBuilder query = new StringBuilder();
		
		// separate ors from nots
		for (FL_PropertyMatchDescriptor term : advancedTerms) {
			(PropertyMatchDescriptorHelper.isExclusion(term)? nots: ors).add(term);
		}
	
		// not valid
		if (ors.isEmpty()) {
			return null;
		}
	
		// ors
		for (FL_PropertyMatchDescriptor term : ors) {
			query.append(toSolrClause(term));
			query.append(" OR ");
		}
		
		// trim last OR
		query.setLength(query.length() - 4);
	
		// nots
		if (!nots.isEmpty()) {
			query.insert(0, '(');
			query.append(')');
			
			for (FL_PropertyMatchDescriptor term : nots) {
				query.append(" AND ");
				query.append(toSolrClause(term));
			}
		}
		
		return query.toString();
	}
}
