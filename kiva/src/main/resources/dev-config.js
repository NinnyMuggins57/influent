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
{
	/*
	 * A default log configuration, which appends info to the console but pops up notifications.
	 */
	'aperture.log' : {
		level : 'log',
		logWindowErrors : {
			log : true,
			preventDefault : true
		},
		appenders : {
			consoleAppender : {
				level : 'info'
			},
			notifyAppender : {
				level : 'error'
			}//,
			//'draperAppender' : {'address' : 'http://xd-draper.xdata.data-tactics-corp.com:1337'}
		}
	},
	
	/*
	 * The endpoint locations for Aperture services accessed through the io interface
	 */
	'aperture.io' : {
		rpcEndpoint : '%host%/${project.artifactId}/rpc',
		restEndpoint : '%host%/${project.artifactId}/rest'
	},
	
	/*
	 * Influent specific configuration
	 */
	'influent.config' : {
		useAuth : true,
		banner : 'Kiva',
		title : 'Kiva',
		help : 'http://localhost:8080/help',
		workspaceWidth : 1100,
		dateRangeIntervals : {
			P14D : '2 weeks',
			P112D : '16 weeks',
			P1Y : '1 year',
			P16M : '16 months',
			P4Y : '4 years',
			P16Y : '16 years'
		},
		startingDateRange : 'P16M',
		defaultEndDate: new Date(2013, 4, 1),
		defaultShowDetails : true,
		doubleEncodeSourceUncertainty : true,
		maxSearchResults : 50,
		searchResultsPerPage : 12,
		searchGroupBy : 'GEO',
		sessionTimeoutInMinutes : 24*60,
		sessionRestorationEnabled : false,
		promptForDetailsInFooter : true,
		usePatternSearch : true,
		patternQueryDescriptionHTML : 'Behavioral query by example is provided by Graph QuBE, an MIT Lincoln Labs technology. '
			+ 'Graph QuBE uses one or more model accounts to find accounts with similar patterns of activity. Searching with one such '
			+ 'set of model accounts, specified here, will match accounts with similar activity. To match on a pattern of activity '
			+ '<i>between</i> accounts, open a match card for each of the roles in the pattern and provide one or more model accounts for each. '
			+ 'To supply more than one account as a model use commas. '
			+ '<br>HINT: Clicking the match button (<img src="img/search-small.png" style="bottom: -3px; position: relative;"/>) '
			+ 'on a populated role folder will populate its match card criteria with its accounts as models. ',
		enableAdvancedSearchMatchType : true,
		advancedSearchFuzzyLevels : {
			'is very like' : 0.9,
			'is like': 0.5,
			'is vaguely like': 0.1
		},
			
		objectDegreeWarningCount : 1000,
		objectDegreeLimitCount: 10000,
		promptForDetailsWithModalDialog: false,
		iconOrder : ['TYPE', 'GEO', 'STATUS', 'WARNING'],
		iconMap : {
			TYPE : {
				map : function(name, value) {
					switch (value) {
						case 'lender':
							return {
								title: 'lender',
								icon: {type: 'Person', attributes: {role: 'business'}}
							};
						case 'partner':
							return {
								title: 'partner',
								icon: {type: 'Organization', attributes: {role: 'business'}}
							};
						case 'loan':
							return {
								title: 'borrower / loan',
								icon: {type: 'Person'}
							};
					}
				},
				limit : 1
			},
			GEO : {
				map : function(name, value) {
					if (value && value.cc) {
						return {
							title: value.text || value.cc,
							icon: {type: 'Place', attributes: {code: value.cc, country: value.cc}}
						};
					}
				},
				limit: 3
			},
			STATUS : {
				map : function(name, value) {
					switch (value) {
						case 'defaulted':
							return {
								title: 'defaulted',
								icon: {type: 'stamp', attributes: {text: 'default'}}
							};
						case 'closed':
							return {
								title: 'closed',
								icon: {type: 'stamp', attributes: {text: 'closed'}}
							};
					}
				},
				limit: 1
			},
			WARNING : {
				map : function(name, value) {
					return {
						title: value,
						icon: {type: 'warning'}
					};
				}
			}
		}
	}
}
