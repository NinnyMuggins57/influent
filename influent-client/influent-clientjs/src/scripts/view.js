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
define(
	[
		'lib/viewproto', 'lib/channels', 'lib/extern/cookieUtil', 'lib/util/GUID', 'lib/ui/xfModalDialog',
		'modules/xfCards', 'modules/xfRenderer', 'modules/xfHeader', 'modules/xfEntityDetails',
		'modules/xfAdvancedSearch', 'modules/xfSankey', 'modules/xfTransactionTable', 'modules/xfTransactionGraph',
		'modules/xfDropTargetRenderer', 'modules/xfFooter', 'modules/xfFileUpload', 'modules/xfRest'
	],
	function(View, chan, cookieUtil, GUID, dialog) {


		// get key settings
		var urlFlags = (function() {
			function getQueryParam(name) {
				var regex = new RegExp('[\\?&]' + name + '=([^&#]*)'),
					values = regex.exec(location.search);

				return values == null ? null : decodeURIComponent(values[1].replace(/\+/g, ' '));
			}

			return {
				sessionId : getQueryParam('sessionId') || '',
				capture : getQueryParam('capture') || false,
				entityId : getQueryParam('entityId') || null
			};

		}());

		return View.extend( 'Influent', {

			/**
			 * Constructor function.
			 */
			init : function() {

				// call base class constructor.
				View.prototype.init.call(this);

				this.inited = false;
			},

			/**
			 * Returns a title that summarises the state
			 *
			 * @returns {String}
			 *	the title
			 */
			title : function() {

				// this.state things should be added behind the name here.
				return aperture.config.get()['influent.config'].title || 'Name Me';
			},

			/**
			 * Default state
			 */
			defaults : {
				// state variables here
			},

			/**
			 * Set view state
			 */
			view : function(trialState, callback) {

				// VALIDATE HERE. can use this.ajax to check server things.
				var validState = trialState;


				// apply when done.
				this.doView(validState, callback);
			},

			/**
			 * Set view state
			 */
			doView : function (validState, callback) {

				var that = this;

				function startView(settings) {
					// trace it out for now
					aperture.log.info('session: ' + settings.sessionId + ', capture: ' + settings.capture + ', entityId: ' + settings.entityId);

					// make sure these modules are started. could do module switches here
					that.modules.start('xfHeader', {div:'header'});
					that.modules.start('xfCards', {div:'cards', sessionId: settings.sessionId, entityId: settings.entityId});
					that.modules.start('xfFooter', {div:'footer', capture: settings.capture});
					that.modules.start('xfAdvancedSearch', {div:'search'});
					that.modules.start('xfEntityDetails', {div:'popup'});
					that.modules.start('xfTransactionTable', {div:'tableTab'});
					that.modules.start('xfTransactionGraph', {div:'chartTab'});
					that.modules.start('xfDropTargetRenderer', {div:'drop-target-canvas'});
					that.modules.start('xfSankey', {div:'sankey', capture: settings.capture});
					that.modules.start('xfRenderer', {div:'cards', capture: settings.capture});
					that.modules.start('xfFileUpload', {div:'fileUpload'});
					that.modules.start('xfRest', {capture: settings.capture});

					// then call base implementation too
					View.prototype.doView.call(that, validState, callback);

					// Send a notification that all modules have been started.   This will kickoff initialization
					if (!that.inited) {
						that.inited = true;
						aperture.pubsub.publish(chan.ALL_MODULES_STARTED, {noRender : true});
					}
				}

				var banner = aperture.config.get()['influent.config']['banner'];
				if (banner) {
					$('.banner-text').html(banner);
				}

				if (urlFlags.sessionId) {
					startView(urlFlags);

				} else {

					var sessionRestorationEnabled = aperture.config.get()['influent.config']['sessionRestorationEnabled'];

					// use the application specific part of the host url to label the cookie distinctly
					var cookieId = aperture.config.get()['aperture.io'].restEndpoint.replace('%host%', 'sessionId');
					var cookie = cookieUtil.readCookie(cookieId);

					// handle loads and new sessions
					if (!cookie || !sessionRestorationEnabled) {
						startView({sessionId: GUID.generateGuid(), capture: urlFlags.capture, entityId: urlFlags.entityId});
					} else {
						dialog.createInstance({
							title : 'Session Found',
							contents : 'A previous session of Influent was found. Attempt to reload it? Or start a new session?',
							buttons : {
								'Reload' : function() {
									startView({sessionId: cookie, capture: urlFlags.capture, entityId: urlFlags.entityId});
								},
								'New Session' : function() {
									startView({sessionId: GUID.generateGuid(), capture: urlFlags.capture, entityId: urlFlags.entityId});
								}
							}
						});
						cookieUtil.eraseCookie(cookieId);
					}
				}
			}
		});
	}
);


