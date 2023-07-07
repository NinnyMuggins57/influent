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
import influent.idl.FL_PropertyDescriptor;
import influent.idl.FL_TypeDescriptor;
import influent.idl.FL_TypeMapping;
import influent.idl.FL_PropertyDescriptors;
import influent.idl.FL_ListRange;
import influent.idlhelper.PropertyMatchDescriptorHelper;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EntitySearchTerms {

	private List<FL_PropertyMatchDescriptor> _termsList = null;
	private String _matchType = null;
	
	private List<String> _dataTypes = null;
	
	
	
	
	private Set<String> parseSpecialTags(Pattern regex, String term) {
		Matcher specialTagsMatcher = regex.matcher(term.trim());
		Set<String> specialTags = new HashSet<String>();
		while (specialTagsMatcher.find()) {
			
			String tagName = specialTagsMatcher.group(1).toString().trim();
			
			if (tagName.equalsIgnoreCase("datatype")) {
				String dataType = specialTagsMatcher.group(2).toString().trim();
				if (dataType.indexOf('"') == 0) {
					dataType = dataType.substring(1);
				}
				if (dataType.lastIndexOf('"') == dataType.length()-1) {
					dataType = dataType.substring(0, dataType.length()-1);
				}
				if (this._dataTypes == null) {
					this._dataTypes = new ArrayList<String>();
				}
				this._dataTypes.add(dataType);
				specialTags.add("datatype");
				continue;
			}
			
			if (tagName.equalsIgnoreCase("matchtype")) {
				String matchTypeString = specialTagsMatcher.group(2).toString().trim();
				if (matchTypeString != null) {
					_matchType = matchTypeString;
				}
				if (_matchType.indexOf('"') == 0) {
					_matchType = _matchType.substring(1);
				}
				if (_matchType.lastIndexOf('"') == _matchType.length()-1) {
					_matchType = _matchType.substring(0, _matchType.length()-1);
				}
				specialTags.add("matchtype");
				continue;
			}
		}
		return specialTags;
	}

	public EntitySearchTerms(String term, FL_PropertyDescriptors descriptors) {
		Pattern extraTermRegEx = Pattern.compile("\\A([^:]*)(\\s*$| [^:\\s]+:.*)");
		Pattern tagsRegEx = Pattern.compile("([^:\\s]+):(\"([^\"]*)\"|[^:]*)( |$)");
		Pattern boostPattern = Pattern.compile("\\^([\\.0-9]+)$");
        Pattern quotePattern = Pattern.compile("((?<![\\\\])\")((?:.(?!(?<![\\\\])\\1))*.?)\\1");
		Pattern similarityPattern = Pattern.compile("\\~([\\.0-9]+)$");		
		
		Matcher extraTermMatcher = extraTermRegEx.matcher(term);
		StringBuilder extraTermsBuilder = new StringBuilder();
		while (extraTermMatcher.find()) {
			extraTermsBuilder.append(extraTermMatcher.group(1).toString().trim());
		}
		String extraTerms = extraTermsBuilder.toString().trim();

		Set<String> specialTagSet = parseSpecialTags(tagsRegEx, term.trim());

		_termsList = new ArrayList<FL_PropertyMatchDescriptor>();

		Matcher tagsMatcher = tagsRegEx.matcher(term.trim());
		while (tagsMatcher.find()) {

			String tagName = tagsMatcher.group(1).toString().trim();

			if (specialTagSet.contains(tagName)) {
				continue;
			}


            FL_PropertyMatchDescriptor.Builder termBuilder = FL_PropertyMatchDescriptor.newBuilder();

            termBuilder.setKey(tagName);
            if (tagName.startsWith("-")) {
                tagName = tagName.substring(1);
                termBuilder.setInclude(false);
            }

            String tagValue = tagsMatcher.group(2).toString().trim();

            Matcher boostMatch = boostPattern.matcher(tagValue);
            if (boostMatch.find()) {
                String weightStr = boostMatch.group(1);

                try {
                    Float weight= Float.valueOf(weightStr);
                    tagValue = tagValue.substring(0, tagValue.length()-weightStr.length()-1);

                    termBuilder.setWeight(weight);

                } catch (Exception e) {
                }

            }

            Matcher similarityMatch = similarityPattern.matcher(tagValue);
            if (similarityMatch.find()) {
                String similarityStr = similarityMatch.group(1);

                try {
                    Float similarity = Float.valueOf(similarityStr);
                    tagValue = tagValue.substring(0, tagValue.length()-similarityStr.length()-1);

                    termBuilder.setSimilarity(similarity);

                } catch (Exception e) {
                }

            }


            boolean quoted = false;
            Matcher quoteMatch = quotePattern.matcher(tagValue);
            if (quoteMatch.find()) {
                tagValue = quoteMatch.group(2);
				quoted = true;
			}

			if (quoted) {
				if (_matchType == null || _matchType.equalsIgnoreCase("all")) {
					termBuilder.setConstraint(FL_Constraint.REQUIRED_EQUALS);
				} else {
					termBuilder.setConstraint(FL_Constraint.OPTIONAL_EQUALS);
				}
			} else {
				if (_matchType == null || _matchType.equalsIgnoreCase("any")) {
					termBuilder.setConstraint(FL_Constraint.FUZZY_PARTIAL_OPTIONAL);
				} else {
					termBuilder.setConstraint(FL_Constraint.FUZZY_REQUIRED);
				}
			}

			// Match properties to Search Descriptors
			List<FL_TypeMapping> typeMappings = new ArrayList<FL_TypeMapping>();
			for (FL_PropertyDescriptor pd : descriptors.getProperties()) {
				if (pd.getKey().equals(tagName)) {
					if (_dataTypes != null) {
						for (FL_TypeMapping td : pd.getMemberOf()) {
							if (_dataTypes.contains(td.getType())) {
								typeMappings.add(td);
							}
						}
					}
				}
			}

			termBuilder.setTypeMappings(typeMappings);

			List<Object> values = new ArrayList<Object>(Arrays.asList(tagValue.split(",")));
			termBuilder.setRange(FL_ListRange.newBuilder().setType(FL_PropertyType.STRING).setValues(new ArrayList<Object>(values)).build());

			_termsList.add(termBuilder.build());
		}


		// Add extra terms against freeTextIndexed properties
		if (extraTerms != null && !extraTerms.isEmpty()) {
			final Object values = PropertyMatchDescriptorHelper.rangeFromBasicTerms(extraTerms);

			if (values != null) {
				for (FL_PropertyDescriptor pd : descriptors.getProperties()) {

					if (pd.getFreeTextIndexed()) {
						_termsList.add(FL_PropertyMatchDescriptor.newBuilder()
							.setConstraint(FL_Constraint.OPTIONAL_EQUALS)
							.setKey(pd.getKey())
							.setRange(values)
							.setTypeMappings(pd.getMemberOf())
							.build()
						);
					}
				}
			}
		}
	}
	
	

	
	public List<FL_PropertyMatchDescriptor> getTerms() {
		return _termsList;
	}
	
	
	
	
	public List<String> getTypes() {
		if (_dataTypes == null) {
			return Collections.singletonList(null);
		} else {
			return _dataTypes;
		}
	}
}
