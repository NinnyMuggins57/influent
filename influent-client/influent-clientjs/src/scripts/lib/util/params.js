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

/**
 * Defines the base application state object and functions on that object.
 * State is for instance the current time window.
 */
define([], function() {
	return {
		
		/**
		 * Parses a string hash value and returns a set of parameters.
		 * 
		 * @params {String} hash
		 * 	The hash value to parse.
		 * 
		 * @returns {Object}
		 * 	A set of parameters.
		 */
		parse : function (hash) {
			if (hash == null) {
				return null;
			}
			
			var params = hash.split(")!");
			var p={}, param, bs, be, name, value;

			for (var i= 0; i< params.length; i++) {
				param = params[i];

				bs = param.indexOf('(');
				be = param.length;

				if (bs > 0 && be > bs+1) {
					name = param.substring(0, bs);
					value = param.substring(bs+1, be);
					p[name] = value;
				}
			}
			return p;
			
		},
		
		
		/**
		 * Updates the specified set of parameters with any number of potential changes,
		 * returning only those parameters which have changed.
		 * 
		 * @param {Object} params
		 * 	the set of parameters to be updated
		 * 
		 * @param {Object} updates
		 * 	the parameter updates to apply
		 * 
		 * @returns {Object}
		 * 	any changed parameters, or null if unchanged
		 */
		update : function (params, updates) {

			var changes = {};
			var changed = false;
	
			// 
			if (aperture.util.isString(updates)) {
				updates = parse(updates);
			}
			
			// Check each of the newly set properties, have any changed?
			// NOTE THAT this should deep copy any object values, which
			// we currently take care of after this loop.
			for (var prop in updates) {
				// Copy own properties, data only, no functions
				// Note: if copy functions we really screw up the use of "that" in the closure
				if (updates.hasOwnProperty(prop) && _.isFunction(updates[prop]) === false ) {
					var myprop = params[prop];
					var nwprop = updates[prop];
	
					// cannot compare dates using equals operator
					if (myprop && nwprop && myprop.getTime && nwprop.getTime) {
						if (myprop.getTime() === nwprop.getTime())
							continue;
					}
					else if (myprop === nwprop) {
						continue;
					}
	
					// A new value for this property
					params[prop] = nwprop;
	
					// Store it in the set of changes.
					changes[prop] = nwprop;
					changed = true;
				}
			}
	
			return changed? changes : null;
		},
		
		
		/**
		 * Constructs and returns a hash value from the specified parameters
		 * 
		 * @param {Object} params
		 * 	The parameters to hash
		 * 
		 * @returns {String}
		 * 	A hash value.
		 */
		hash: function (params) {
			var hash = '';
	
			for (var prop in params) {
				if (params.hasOwnProperty(prop) && _.isFunction(params[prop]) === false ) {
					var value = params[prop];
	
					if (value) {
						hash+= prop + '('+ value + ')!';
					}
				}
			}
	
			return hash;
		}
	};
});


