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
        'jquery', 'lib/module', 'lib/channels', 'lib/util/xfUtil', 'lib/render/cardRenderer',
        'lib/render/clusterRenderer', 'lib/render/fileRenderer', 'lib/render/matchRenderer','lib/render/columnRenderer',
        'lib/render/workspaceRenderer', 'lib/render/toolbarRenderer', 'lib/constants'
    ],
    function(
        $, modules, chan, xfUtil, cardRenderer,
        clusterRenderer, fileRenderer, matchRenderer, columnRenderer,
        workspaceRenderer, toolbarRenderer, constants
    ) {


        var _renderState = {
            canvas : undefined,
            capture : false,
            offsetX : 40, // Extra padding when scrolling left to ensure our buttons don't get clipped.
            subscriberTokens : null,
            isRenderingSuspended : false,
            deferredRenderRequestData : []
        };

        var _toolbarDefaults = toolbarRenderer.getRenderDefaults();
        var _fileDefaults = fileRenderer.getRenderDefaults();

        //--------------------------------------------------------------------------------------------------------------

        var performLayout = function(layoutRequest){
            if (layoutRequest != null){
                var layoutProvider = layoutRequest.layoutProvider;
                var layoutInfo = layoutRequest.layoutInfo;

                var positionMap = layoutProvider.layoutUIObjects(layoutInfo);

                // Set the positions of all the other columns.
                var columns = layoutInfo.workspace.getVisualInfo().children;
                for (var i=0; i < columns.length; i++){
                    var columnObj = columns[i];
                    var columnId = columnObj.getXfId();
                    if (columnId != targetColumnId){
                        $('#' + columnId).css('top', positionMap[columnId].top);
                    }
                }

                var workspace = $('#workspace');
                var sankey = $('#sankey');

                if (layoutInfo.type == 'insert'){
                    // Now update the position of the corresponding layout object;
                    var refPosition = positionMap[layoutInfo.refInfo.uiObject.getXfId()];
                    var refColumnId = layoutInfo.refInfo.columnObject.getXfId();

                    var targetPosition = positionMap[layoutInfo.targetInfo.uiObject.getXfId()];
                    var targetColumnId = layoutInfo.targetInfo.columnObject.getXfId();

                    var newTop = positionMap[targetColumnId].top;

                    var isInsertRight = refPosition.left < targetPosition.left;
                    var scrollX = Math.max(targetPosition.left + (isInsertRight?1:-1)*_renderState.offsetX,0);

                    workspace.scrollLeft(scrollX);

                    var postAnimate = function(){
                        // Assume that xfFiles are always added to the top.
                        var offsetY =  (layoutInfo.targetInfo.uiObject.getUIType() == constants.MODULE_NAMES.FILE ?_fileDefaults.HEADER_HEIGHT : 0) + _toolbarDefaults.TOOLBAR_BTN_HEIGHT + 5;

                        var scrollY = Math.max((isInsertRight?targetPosition.top : refPosition.top) - offsetY, _toolbarDefaults.TOOLBAR_BTN_HEIGHT);
                        // Check if we need to scroll the viewport.
                        if ($('#cards').height() + workspace.scrollTop() < scrollY){

                            var postWorkspaceAnimate = function() {
                                // Wait until the animation has completed so we know
                                // the full coverage of the visuals. Then update the
                                // dimensions of the Sankey canvas if necessary.
                                sankey.height(workspace[0].scrollHeight);
                                sankey.width(workspace[0].scrollWidth);
                                aperture.pubsub.publish(chan.UPDATE_SANKEY_EVENT, {workspace : layoutInfo.workspace, layoutProvider : layoutProvider});
                            };

                            if (_renderState.capture) {
                                workspace.css('scrollTop', scrollY);
                                postWorkspaceAnimate();
                            } else {
                                workspace.animate({scrollTop : scrollY}, 750, postWorkspaceAnimate);
                            }

                        }
                        else {
                            aperture.pubsub.publish(chan.UPDATE_SANKEY_EVENT, {workspace : layoutInfo.workspace, layoutProvider : layoutProvider});
                        }
                    };
                    if (isInsertRight){
                        if (_renderState.capture) {
                            $('#' + targetColumnId).css('top', newTop) ;
                            postAnimate();
                        } else {
                            $('#' + targetColumnId).animate({top: newTop}, 750, postAnimate);
                        }
                    }
                    else {
                        if (_renderState.capture) {
                            $('#' + refColumnId).css('top', positionMap[refColumnId].top);
                            postAnimate();
                        } else {
                            $('#' + refColumnId).animate({top: positionMap[refColumnId].top}, 750, postAnimate);
                        }
                    }
                }
                else if (layoutInfo.type == 'zoomTo'){
                    var refPosition = positionMap[layoutInfo.refInfo.uiObject.getXfId()];
                    // Assume that xfFiles are always added to the top.
                    var offsetY =  (layoutInfo.refInfo.uiObject.getUIType() == constants.MODULE_NAMES.FILE ? _fileDefaults.HEADER_HEIGHT : 0) + _toolbarDefaults.TOOLBAR_BTN_HEIGHT + 5;

                    var scrollY = Math.max(refPosition.top - offsetY, _toolbarDefaults.TOOLBAR_BTN_HEIGHT);

                    var postWorkspaceAnimate = function() {
                        aperture.pubsub.publish(chan.UPDATE_SANKEY_EVENT, {workspace : layoutInfo.workspace, layoutProvider : layoutProvider});
                    };

                    if (_renderState.capture) {
                        workspace.css('scrollTop', scrollY);
                        postWorkspaceAnimate();
                    } else {
                        workspace.animate({scrollTop : scrollY}, 750, function(){
                            aperture.pubsub.publish(chan.UPDATE_SANKEY_EVENT, {workspace : layoutInfo.workspace, layoutProvider : layoutProvider});
                        });
                    }

                    workspace.animate({scrollTop : scrollY}, 750, postWorkspaceAnimate);
                }
                else {
                    // Update the Sankeys.
                    aperture.pubsub.publish(chan.UPDATE_SANKEY_EVENT, {workspace : layoutInfo.workspace, layoutProvider : layoutProvider});
                }
            }
        };

        //--------------------------------------------------------------------------------------------------------------

        var _onSuspendRendering = function(eventChannel, data) {
            _renderState.deferredRenderRequestData = [];
            _renderState.isRenderingSuspended = true;
        };

        //--------------------------------------------------------------------------------------------------------------

        var _onResumeRendering = function(eventChannel, data) {
            setTimeout(function() {
                _renderState.isRenderingSuspended = false;
                for (var i = 0; i < _renderState.deferredRenderRequestData.length; i++) {
                    aperture.pubsub.publish(chan.RENDER_UPDATE_REQUEST, _renderState.deferredRenderRequestData[i]);
                }
            }, 100);
        };

        //--------------------------------------------------------------------------------------------------------------

        var render = function(eventChannel, data){

            // Defer this request if rendering is temporarily suspended
            if (_renderState.isRenderingSuspended) {
                _renderState.deferredRenderRequestData.push(data);
                return;
            }

            var uiObject = data.UIObject;
            var objectType = uiObject.getUIType();
            var visualInfo = uiObject.getVisualInfo();
            var element = undefined;

            if (visualInfo == null || visualInfo.xfId == null) {
                return;
            }

            var isFirst = $(
                '#' +
                visualInfo.xfId +
                ((objectType == constants.MODULE_NAMES.IMMUTABLE_CLUSTER ||
                  objectType == constants.MODULE_NAMES.MUTABLE_CLUSTER ||
                  objectType == constants.MODULE_NAMES.SUMMARY_CLUSTER) ? '_cluster' : '')
            ).length == 0;

            // Determine what we're rendering.
            switch(objectType){
                case constants.MODULE_NAMES.WORKSPACE : {
                    element = workspaceRenderer.createElement(visualInfo);
                    break;
                }
                case constants.MODULE_NAMES.COLUMN : {
                    element = columnRenderer.createElement(visualInfo);
                    break;
                }
                case constants.MODULE_NAMES.FILE : {
                    element = fileRenderer.createElement(visualInfo);
                    break;
                }
                case constants.MODULE_NAMES.MATCH : {
                    element = matchRenderer.createElement(visualInfo);
                    break;
                }
                case constants.MODULE_NAMES.IMMUTABLE_CLUSTER :
                case constants.MODULE_NAMES.MUTABLE_CLUSTER :
                case constants.MODULE_NAMES.SUMMARY_CLUSTER : {
                    element = clusterRenderer.createElement(visualInfo);
                    break;
                }
                case constants.MODULE_NAMES.ENTITY : {
                    element = cardRenderer.createElement(visualInfo);
                    break;
                }
                default :{
                    aperture.log.error('Attempted to render an unsupported UIObject type: ' + objectType);
                }
            }
            if (isFirst){
                if (objectType == 'xfWorkspace') {
                    var cardsDiv = $('#cards');
                    cardsDiv.empty();
                    cardsDiv.append(element);
                    
                    if(visualInfo.footerDisplay == 'none') {
                        var footerDiv = $('#footer');
                        footerDiv.accordion('activate', false );
                    }
                }
                else {
                    var parentCanvas = $('#' + uiObject.getParent().getXfId());
                    parentCanvas.append(element);
                }
            }

            // Call the layout provider if a layout request is present.
            performLayout(data.layoutRequest);
        };

        //--------------------------------------------------------------------------------------------------------------

        var _initializeModule = function() {
            // Clear out any existing visuals.
            _renderState.canvas = $('#cards');
            _renderState.canvas.empty();
            $('#sankey').height(0);

            // If we are rendering a capture then we hide unnecessary div elements
            if (_renderState.capture) {
                $('.nocapture').hide();

                var workspaceDiv = $('#workspace');
                workspaceDiv.css('height', '100%');
                workspaceDiv.css('overflow', 'none');
                workspaceDiv.css('background-color', 'white');

                var headerDiv = $('#header');
                headerDiv.css('background-color', 'white');
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
                    subTokens[chan.SUSPEND_RENDERING] = aperture.pubsub.subscribe(chan.SUSPEND_RENDERING, _onSuspendRendering);
                    subTokens[chan.RESUME_RENDERING] = aperture.pubsub.subscribe(chan.RESUME_RENDERING, _onResumeRendering);

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
            var workspaceDiv = $('#workspace');
            var headerDiv = $('#header');
            var width = workspaceDiv[0].scrollWidth;
            var height = workspaceDiv[0].scrollHeight;
            height += headerDiv[0].scrollHeight;
            return {
                width : width,
                height : height
            };
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