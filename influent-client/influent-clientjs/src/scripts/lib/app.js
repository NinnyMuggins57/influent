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
 * Main module that defines the application
 *
 */
define(['jquery', 'view', 'lib/util/params', 'lib/channels', 'lib/ui/status'],

function($, View, params, chan, statusDialog) {

	/**
	 * Main application controller object that manages app state, history, etc.  It
	 * defers control to the mode controllers to handle transitions, DIV arrangement,
	 * etc.
	 */
	var App = function() {
		var currentHash = undefined,
			useRootHash = false,

			/**
			 * View manager
			 */
			view = new View(),
			
			// Set up validation callback.
			// (don't do anything until all requested have been validated).
			// Note that validation can be asynchronous if we need to hit the
			// server to fill stuff in, thus the callback.
			onStateChange = function( state, changes ) {
				
				// Cancel obsolete status dialog if necessary.
				statusDialog.close(true);

				// Update the hash
				var newHash = useRootHash? '' : params.hash(state);
				if( newHash !== currentHash ) {
					// Hash is different, push into browser's location (adds a back button history state)
					// This will cause a notification of hashchange which will make its way back
					// to this function.  However the changed hash will equal the current state so no
					// change should be detected
					$.address.value(encodeURIComponent(newHash));
				}

				// retitle
				document.title = view.title();

				// Notify everyone of a real, valid state change
				aperture.pubsub.publish( chan.STATE, {
					'state': state,
					'changes': changes
				});
			},

			/**
			 * Notification from the wire that the state was requested to be changed by someone
			 * (possibly even ourselves).  Update the state, if changed update the hash
			 */
			onStateChangeRequest = function( channel, stateChangeData ) {
	
				// sets the state
				view.update( stateChangeData, onStateChange );
			},

			/**
			 * This function is called when the location hash changes
			 */
			onHashChange = function (hashChange) {
				hashChange = hashChange.value;
				
				// Retrieve the current hash, decode it, and remove any leading /s.
				currentHash = hashChange? decodeURIComponent(hashChange) : undefined;
				
				// treat the root url as a special - means use defaults, even if changed.
				useRootHash = !currentHash;
					
				// picked up by subscriber below, which chains eventually to onStateChange if there are changes
				aperture.pubsub.publish( chan.STATE_REQUEST, params.parse(currentHash) );
				
				// reset
				useRootHash = false;
			};

		// Disable the auto-formating the plugin
		// is set to do by default.
		$.address.strict(false);

		// Listen for changes
		aperture.pubsub.subscribe( chan.STATE_REQUEST, onStateChangeRequest );


		/**
		 * DOM is available, start the app
		 */
		this.start = function() {

			// now that we are initialized, set hash.
			var hash = document.URL;

			if (hash) {
				var hashi = hash.indexOf('#');

				hash = (hashi >= 0)? hash.substr(hashi+1) : '';

			}

			onHashChange( {value: hash} );

			// Set the hash change listener.
			$.address.change(onHashChange);
			
			// initialize title
			document.title = view.title();
		};
	};



	/*
	 * Public iface of the app
	 */
	return {
		app : null,

		setup : function() {
			this.app = new App();
		},

		start : function() {
			this.app.start();
		}
	};
});
