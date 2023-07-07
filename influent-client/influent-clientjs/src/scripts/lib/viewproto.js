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
 * Manages the view for the app.
 */
define(['lib/module', 'lib/sandbox', 'lib/ajax', 'lib/util/params'], function(moduleFeature, Sandbox, createBroker, params) {

	return aperture.Class.extend( 'BasicView', {
		init : function() {

			/**
			 * Request broker, handles efficient requests and caching
			 */
			this.ajax = createBroker(20);

			/**
			 * View state
			 */
			this.state = _.clone(this.defaults);


			var that = this;

			/**
			 * Module manager
			 */
			this.modules = moduleFeature.createManager(function( spec ) {
				spec.state = that.state;
				spec.broker = that.ajax;
				return new Sandbox( spec );
			});
		},

		/**
		 * Updates the view state, validating it in the process.
		 *
		 * @params {Object} changes
		 *	The changes to apply.
		 *
		 * @param {Function} callback
		 *	The callback to invoke if any changes occur
		 */
		update : function (changes, callback) {

			var trialState;

			if (changes) {
				// don't touch real state until post validation.
				trialState = _.clone(this.state);

				// and first apply the changes to that, in prep for validation.
				params.update(trialState, changes);

			} else {

				// don't touch real state until post validation.
				trialState = _.clone(this.defaults);
			}

			// validate
			this.view(trialState, callback);
		},

		/**
		 * Sets the view state
		 *
		 * @param {Object} validState
		 *	The validated new state
		 *
		 * @param {Function} callback
		 *	The callback to invoke if any changes occur
		 */
		doView : function (validState, callback) {

			// validated. now apply.
			var changes = params.update(this.state, validState);

			// anything changed?
			if (changes) {
				callback( this.state, changes );
			}
		}

	});
});


