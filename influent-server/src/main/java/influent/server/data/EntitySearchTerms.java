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
package influent.server.data;

import influent.idl.FL_Constraint;
import influent.idl.FL_PropertyMatchDescriptor;
import influent.idl.FL_PropertyType;
import influent.idlhelper.SingletonRangeHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EntitySearchTerms {

	private String extraTerms = "";
	private List<FL_PropertyMatchDescriptor> terms = new ArrayList<FL_PropertyMatchDescriptor>();
	private Boolean doCluster = null;
	private String matchType = null;
	
	private String dataType = null;
	
	private Set<String> parseSpecialTags(Pattern regex, String term) {
		Matcher specialTagsMatcher = regex.matcher(term.trim());
		Set<String> specialTags = new HashSet<String>();
		while (specialTagsMatcher.find()) {
			
			String tagName = specialTagsMatcher.group(1).toString().trim();
			
			if (tagName.equalsIgnoreCase("cluster")) {
				String clusterBoolString = specialTagsMatcher.group(2).toString().trim();
				if (clusterBoolString != null) {
					doCluster = (clusterBoolString.equalsIgnoreCase("true") || clusterBoolString.equalsIgnoreCase("yes"));
				}
				specialTags.add("cluster");
				continue;
			}
			
			if (tagName.equalsIgnoreCase("datatype")) {
				dataType = specialTagsMatcher.group(2).toString().trim();
				if (dataType.indexOf('"') == 0) {
					dataType = dataType.substring(1);
				}
				if (dataType.lastIndexOf('"') == dataType.length()-1) {
					dataType = dataType.substring(0, dataType.length()-1);
				}
				specialTags.add("datatype");
				continue;
			}
			
			if (tagName.equalsIgnoreCase("matchtype")) {
				String matchTypeString = specialTagsMatcher.group(2).toString().trim();
				if (matchTypeString != null) {
					matchType = matchTypeString;
				}
				if (matchType.indexOf('"') == 0) {
					matchType = matchType.substring(1);
				}
				if (matchType.lastIndexOf('"') == matchType.length()-1) {
					matchType = matchType.substring(0, matchType.length()-1);
				}
				specialTags.add("matchtype");
				continue;
			}
		}
		return specialTags;
	}
	
	public EntitySearchTerms(String term) {
		Pattern extraTermRegEx = Pattern.compile("\\A([^:]*)(\\s*$| [^:\\s]+:.*)");
		Pattern tagsRegEx = Pattern.compile("([^:\\s]+)(?<!\\\\):(.*?) (?=[^\\s\\\\]*?:)");
		Pattern boostPattern = Pattern.compile("\\^([\\.0-9]+)$");
		Pattern similarityPattern = Pattern.compile("\\~([\\.0-9]+)$");		
		Matcher extraTermMatcher = extraTermRegEx.matcher(term);
		StringBuilder extraTermsBuilder = new StringBuilder();
		while (extraTermMatcher.find()) {
			extraTermsBuilder.append(extraTermMatcher.group(1).toString().trim());
		}
		
		Set<String> specialTagSet = parseSpecialTags(tagsRegEx, term.trim());
		
		extraTerms = extraTermsBuilder.toString().trim();
		
		Matcher tagsMatcher = tagsRegEx.matcher(term.trim());
		while (tagsMatcher.find()) {
			
			String tagName = tagsMatcher.group(1).toString().trim();
			if (specialTagSet.contains(tagName)) {
				continue;
			}
			
			String[] values = tagsMatcher.group(2).toString().trim().split("(?<!\\\\),(?=(?:[^\"]*\"[^\"]*\")*(?![^\"]*\"))");
			for (String val : values) {
				val = val.trim();
				
				FL_PropertyMatchDescriptor.Builder termBuilder = FL_PropertyMatchDescriptor.newBuilder();

				boolean quoted = false;
				
				Matcher boostMatch = boostPattern.matcher(val);
				if (boostMatch.find()) {
					String weightStr = boostMatch.group(1);

					try {
						Float weight= Float.valueOf(weightStr);
						val = val.substring(0, val.length()-weightStr.length()-1);
						
						termBuilder.setWeight(weight);
						
					} catch (Exception e) {
					}
					
				}
				
				Matcher similarityMatch = similarityPattern.matcher(val);
				if (similarityMatch.find()) {
					String similarityStr = similarityMatch.group(1);

					try {
						Float similarity = Float.valueOf(similarityStr);
						val = val.substring(0, val.length()-similarityStr.length()-1);
						
						termBuilder.setSimilarity(similarity);
						
					} catch (Exception e) {
					}
					
				}				
				
				if (val.startsWith("\"") && val.endsWith("\"")) {
					val = val.substring(1, val.length() - 1);
					quoted = true;
				}
				
				if (tagName.startsWith("-")) {
					tagName = tagName.substring(1);
					termBuilder.setInclude(false);
				} 
				if (quoted) {
					if (matchType == null || matchType.equalsIgnoreCase("all")) {
						termBuilder.setConstraint(FL_Constraint.REQUIRED_EQUALS);
					} else {
						termBuilder.setConstraint(FL_Constraint.OPTIONAL_EQUALS);
					}
				} else {
					if (matchType == null || matchType.equalsIgnoreCase("any")) {
						termBuilder.setConstraint(FL_Constraint.FUZZY_PARTIAL_OPTIONAL);
					} else {
						termBuilder.setConstraint(FL_Constraint.FUZZY_REQUIRED);
					}
				}
				
				termBuilder.setKey(tagName);
				termBuilder.setRange(new SingletonRangeHelper(val, FL_PropertyType.STRING));
				
				terms.add(termBuilder.build());	
			}
		}

	}	
	
	public String getExtraTerms() {
		return extraTerms;
	}
	
	public List<FL_PropertyMatchDescriptor> getTerms() {
		return terms;
	}
	
	
	public String getType() {
		return dataType;
	}
	
	public Boolean doCluster() {
		return doCluster;
	}
	
	public String getBooleanOperator() { 
		return matchType;
	}
}
