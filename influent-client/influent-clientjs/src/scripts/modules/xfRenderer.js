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
define(
	[
		'lib/module', 'lib/channels', 'lib/util/xfUtil', 'lib/render/cardRenderer',
		'lib/render/clusterRenderer', 'lib/render/fileRenderer', 'lib/render/matchRenderer','lib/render/columnRenderer',
		'lib/render/workspaceRenderer', 'lib/render/toolbarRenderer', 'lib/constants'
	],
	function(
		modules, chan, xfUtil, cardRenderer,
		clusterRenderer, fileRenderer, matchRenderer, columnRenderer,
		workspaceRenderer, toolbarRenderer, constants
	) {

		/**
		 * the xfRenderer is responsible for render request processing
		 */
		var _renderState = {
			capture : false,
			offsetX : 40, // Extra padding when scrolling left to ensure our buttons don't get clipped.
			subscriberTokens : null,
			isRenderingSuspended : false,
			renderSuspendedSemaphore : 0,
			deferredRenderRequestData : []
		};

		//--------------------------------------------------------------------------------------------------------------

		var performLayout = function(layoutRequest){
			if (layoutRequest != null){
				var layoutProvider = layoutRequest.layoutProvider;
				var layoutInfo = layoutRequest.layoutInfo;

				layoutProvider.layoutUIObjects(layoutInfo);

				aperture.pubsub.publish(chan.UPDATE_SANKEY_EVENT, {workspace : layoutInfo.workspace, layoutProvider : layoutProvider});
			}
		};

		//--------------------------------------------------------------------------------------------------------------

		var _onSuspendRendering = function(eventChannel, data) {
			_renderState.isRenderingSuspended = true;
			_renderState.renderSuspendedSemaphore++;
		};

		//--------------------------------------------------------------------------------------------------------------

		var _onResumeRendering = function(eventChannel, data) {
			_renderState.renderSuspendedSemaphore--;

			if (_renderState.renderSuspendedSemaphore <= 0) {
				_renderState.renderSuspendedSemaphore = 0;
				_renderState.isRenderingSuspended = false;

				for (var i = 0; i < _renderState.deferredRenderRequestData.length; i++) {
					aperture.pubsub.publish(chan.RENDER_UPDATE_REQUEST, _renderState.deferredRenderRequestData[i]);
				}
				_renderState.deferredRenderRequestData = [];
			}
		};

		//--------------------------------------------------------------------------------------------------------------

		var render = function(eventChannel, data){

			// Defer this request if rendering is temporarily suspended
			if (_renderState.isRenderingSuspended) {
				_renderState.deferredRenderRequestData.push(data);
				return;
			}

			var uiObject = data.UIObject;
			var visualInfo = uiObject.getVisualInfo();

			if (visualInfo == null || visualInfo.xfId == null) {
				return;
			}

			var objectType = uiObject.getUIType();

			// Determine what we're rendering.
			switch(objectType){
				case constants.MODULE_NAMES.WORKSPACE : {
					workspaceRenderer.createElement(visualInfo, _renderState.capture);
					break;
				}
				case constants.MODULE_NAMES.COLUMN : {
					columnRenderer.createElement(visualInfo);
					break;
				}
				case constants.MODULE_NAMES.FILE : {
					fileRenderer.createElement(visualInfo);
					break;
				}
				case constants.MODULE_NAMES.MATCH : {
					matchRenderer.createElement(visualInfo);
					break;
				}
				case constants.MODULE_NAMES.IMMUTABLE_CLUSTER :
				case constants.MODULE_NAMES.MUTABLE_CLUSTER :
				case constants.MODULE_NAMES.SUMMARY_CLUSTER : {
					clusterRenderer.createElement(visualInfo);
					break;
				}
				case constants.MODULE_NAMES.ENTITY : {
					cardRenderer.createElement(visualInfo);
					break;
				}
				default : {
					aperture.log.error('Attempted to render an unsupported UIObject type: ' + objectType);
					return;
				}
			}
			
			// Call the layout provider if a layout request is present.
			performLayout(data.layoutRequest);
		};

		//--------------------------------------------------------------------------------------------------------------

		var _initializeModule = function() {
			
			// If we are rendering a capture then we hide unnecessary div elements
			if (_renderState.capture) {
				$('.nocapture').hide();
			}
		};

		//--------------------------------------------------------------------------------------------------------------

		var rendererConstructor = function(sandbox){

			_renderState.capture =  xfUtil.stringToBoolean(sandbox.spec.capture);

			return {
				render : render,
				start : function(){

					var subTokens = {};
					// Subscribe to the appropriate calls.
					subTokens[chan.RENDER_UPDATE_REQUEST] = aperture.pubsub.subscribe(chan.RENDER_UPDATE_REQUEST, render);
					subTokens[chan.SUSPEND_RENDERING_REQUEST] = aperture.pubsub.subscribe(chan.SUSPEND_RENDERING_REQUEST, _onSuspendRendering);
					subTokens[chan.RESUME_RENDERING_REQUEST] = aperture.pubsub.subscribe(chan.RESUME_RENDERING_REQUEST, _onResumeRendering);

					subTokens[chan.ALL_MODULES_STARTED] = aperture.pubsub.subscribe(chan.ALL_MODULES_STARTED, _initializeModule);

					_renderState.subscriberTokens = subTokens;
				},
				end : function(){
					for (var token in _renderState.subscriberTokens) {
						if (_renderState.subscriberTokens.hasOwnProperty(token)) {
							aperture.pubsub.unsubscribe(_renderState.subscriberTokens[token]);
						}
					}
				}
			};
		};

		//--------------------------------------------------------------------------------------------------------------

		var _getCaptureDimensions = function() {
			var size = workspaceRenderer.getSize();
			
			var headerDiv = $('#header');
			size.height += headerDiv.height();
			
			return size;
		};

		//--------------------------------------------------------------------------------------------------------------

		// Register the module with the system
		modules.register('xfRenderer', rendererConstructor);
		return {
			render : render,
			getCaptureDimensions : _getCaptureDimensions
		};
	}
);
