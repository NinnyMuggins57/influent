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
	 * A default map configuration for the examples
	 */
	'aperture.map' : {
		'defaultMapConfig' : {

			/*
			 * Map wide options which are required for proper use of
			 * the tile set below.
			 */
			'options' : {
				'projection': 'EPSG:900913',
				'displayProjection': 'EPSG:900913',
				'units': 'm',
				'numZoomLevels': 12,
				'maxExtent': [
					-20037500,
					-20037500,
					20037500,
					20037500
				]
			},

			/* The example maps use a Tile Map Service (TMS), which when
			 * registered with the correct settings here in the client
			 * requires no server side code. Tile images are simply requested
			 * by predictable url paths which resolve to files on the server.
			 */
			'baseLayer' : {

				/*
				 * The example map tile set was produced with TileMill,
				 * a free and highly recommended open source tool for
				 * producing map tiles, provided by MapBox.
				 * http://www.mapbox.com.
				 *
				 * Requires specific map-wide options, see above.
				 */
				'tms' : {
					'name' : 'Base Map',
					'url' : 'http://aperture.oculusinfo.com/map-world-graphite/',
					'options' : {
						'layername': 'world-graphite',
						'osm': 0,
						'type': 'png',
						serverResolutions: [156543.0339,78271.51695,39135.758475,19567.8792375,9783.93961875,4891.96980938,2445.98490469,1222.99245234], // ,611.496226172,305.748113086,152.874056543,76.4370282715,38.2185141357,19.1092570679,9.55462853394,4.77731426697,2.38865713348,1.19432856674,0.597164283371
						resolutions: [156543.0339,78271.51695,39135.758475,19567.8792375,9783.93961875,4891.96980938,2445.98490469,1222.99245234] //,611.496226172,305.748113086,152.874056543,76.4370282715
					}
				}
			}
		}
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
        'useAuth' : true,
		'banner' : 'Kiva',
		'title' : 'Kiva',
		'workspaceWidth' : 1100,
		'dateRangeIntervals' : {
		    P14D : '2 weeks',
		    P112D : '16 weeks',
		    P1Y : '1 year',
		    P16M : '16 months',
		    P4Y : '4 years',
		    P16Y : '16 years'
		},
        'startingDateRange' : 'P16M',
        'defaultShowDetails' : true,
        'doubleEncodeSourceUncertainty' : true,
        'maxSearchResults' : 50,
        'searchResultsPerPage' : 12,
        'sessionTimeoutInMinutes' : 24*60,
        'usePatternSearch' : true,
        'patternQueryDescriptionHTML' : 'Behavioral query by example is provided by Graph QuBE, an MIT Lincoln Labs technology. '
        	+ 'Graph QuBE uses one or more model accounts to find accounts with similar patterns of activity. Searching with one such '
        	+ 'set of model accounts, specified here, will match accounts with similar activity. To match on a pattern of activity '
        	+ '<i>between</i> accounts, open a match card for each of the roles in the pattern and provide one or more model accounts for each. '
        	+ 'To supply more than one account as a model use commas. '
        	+ '<br>HINT: Clicking the match button (<img src="img/search-small.png" style="bottom: -3px; position: relative;"/>) '
        	+ 'on a populated role folder will populate its match card criteria with its accounts as models. ',
        
        activityLogging : {
        	enabled: false,
        	address: 'rest/activity'
        	//address: 'http://172.16.98.9:1337'
        	//provider: 'draper',
        },
        
        iconOrder : ['TYPE', 'GEO', 'STATUS', 'WARNING'],
        
        // maps property tags to icons.
		// for each, can supply an icon spec or a url, and optionally a title.
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
