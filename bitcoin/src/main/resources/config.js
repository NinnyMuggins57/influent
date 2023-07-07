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
	 * A default log configuration, which simply appends to the console.
	 */
	'aperture.log' : {
		'level' : 'info',
		'appenders' : {
			// Log to the console (if exists)
			'consoleAppender' : {'level': 'info'}
		}
	},

	/*
	 * The endpoint locations for Aperture services accessed through the io interface
	 */
	'aperture.io' : {
		'rpcEndpoint' : '%host%/${project.artifactId}/rpc',
		'restEndpoint' : '%host%/${project.artifactId}/rest'
	},

	/*
	 * An example palette definition.
	 */
	'aperture.palette' : {
		'color' : {
			'bad'  : '#FF3333',
			'good' : '#66CCC9',
			'selected' : '#7777DD'
		},

		'colors' : {
			'series.10' : ['#1f77b4', '#ff7f0e', '#2ca02c', '#d62728', '#9467bd', '#8c564b', '#e377c2', '#7f7f7f', '#bcbd22', '#17becf']
		}

	},

	// EXAMPLES.
	'influent.config' : {
        'useAuth' : false,
		'banner' : 'Bitcoin',
		'title' : 'Bitcoin',
		'workspaceWidth' : 1100,
		'dateRangeIntervals' : {
		    P14D : '2 weeks',
		    P112D : '16 weeks',
		    P1Y : '1 year',
		    P16M : '16 months',
		    P4Y : '4 years',
		    P16Y : '16 years'
		},
        'startingDateRange' : 'P4Y',
        'defaultEndDate': new Date(2013, 6, 1),
        'defaultShowDetails' : true,
        'maxSearchResults' : 50,
        'searchResultsPerPage' : 12,
        'searchGroupBy' : 'GEO',
        'usePatternSearch' : false,
        'patternQueryDescriptionHTML' : 'Behavioral query by example is provided by Graph QuBE, an MIT Lincoln Labs technology. '
        	+ 'Graph QuBE uses one or more model accounts to find accounts with similar patterns of activity. Searching with one such '
        	+ 'set of model accounts, specified here, will match accounts with similar activity. To match on a pattern of activity '
        	+ '<i>between</i> accounts, open a match card for each of the roles in the pattern and provide one or more model accounts for each. '
        	+ 'To supply more than one account as a model use commas. '
        	+ '<br>HINT: Clicking the match button (<img src="img/search-small.png" style="bottom: -3px; position: relative;"/>) '
        	+ 'on a populated role folder will populate its match card criteria with its accounts as models. ',
        
        iconOrder : ['TYPE'],
        
        // maps property tags to icons.
		// for each, can supply an icon spec or a url, and optionally a title.
        iconMap : {
        	TYPE : {
        		map : function(name, value) {
        			return {
        				title: 'bitcoin account',
        				url: 'img/bitcoin.png'
        			};
        		},
        		distName : 'type-dist',
        		limit : 1
        	}
		}
	}
}
