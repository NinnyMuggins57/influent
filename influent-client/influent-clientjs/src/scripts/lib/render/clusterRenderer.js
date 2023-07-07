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
        'jquery', 'lib/channels','lib/render/cardRenderer', 'lib/constants'
    ],
    function(
        $, chan, cardRenderer, constants
    ) {

        var _renderDefaults = {
            STACK_WIDTH : 3,
            STACK_COUNT : 3,
            CLIP_LEADER_OFFSET : 8
        };
        _renderDefaults.EXPANDED_PADDING_BOTTOM = (_renderDefaults.STACK_COUNT-1)+_renderDefaults.STACK_COUNT;
        var _cardDefaults = cardRenderer.getRenderDefaults();
        var clusterRenderer = {};

        // Private Functions ------------------------------------------------------------------------------

        // we don't indent expanded clusters for viz reasons, but that means then that
        // their leaders need to stack up off to the side. a top level expanded
        // cluster will have a bigger gap between its leader and itself.
        var _updateLeaders = function(state) {
            var root = state.canvas.parents('.clusterExpanded').last();
            var strata;
            var left = -_renderDefaults.CLIP_LEADER_OFFSET;

            // find starting left point based on depth of tree
            for (strata = root; strata.length; strata = strata.children('.clusterExpanded')) {
                left -= _renderDefaults.CLIP_LEADER_OFFSET;
            }
            // then actually apply the positions
            for (strata = root; strata.length; strata = strata.children('.clusterExpanded')) {
                strata.children('.clusterBracket').css('left',  left+ 'px');
                left += _renderDefaults.CLIP_LEADER_OFFSET;
            }
        };

        //--------------------------------------------------------------------------------------------------------------

        var _createFaceElement = function(visualInfo, state){
            state.canvas.addClass('clusterContainer');
            state.canvas.width(_cardDefaults.CARD_WIDTH);

            // create the stack
            var stackCards = [];
            var left = (_renderDefaults.STACK_COUNT-1) * _renderDefaults.STACK_WIDTH;
            var top = 0;
            for (var i = 0; i < _renderDefaults.STACK_COUNT-1; i++) {
                var stackElement = $('<div></div>');
                stackElement.width(_cardDefaults.CARD_WIDTH);
                stackElement.height(_cardDefaults.CARD_HEIGHT);
                stackElement.addClass('stackElement');
                stackElement.css('position','absolute');
                stackElement.css('top',top);
                stackElement.css('left',left);

                left -= _renderDefaults.STACK_WIDTH;
                top += _renderDefaults.STACK_WIDTH;
                state.canvas.append(stackElement);
                stackCards.push(stackElement);
            }

            // Create the face card of the cluster.
            var faceElement = cardRenderer.createElement(visualInfo);
            faceElement.css('top', top);
            state.canvas.append(faceElement);
            // Resize the stacked cards to be the same as the face card.
            for (var i = 0; i < stackCards.length; i++) {
                stackCards[i].height( faceElement.height() );
            }

            // insert clip decorator
            if (visualInfo.UIType == constants.MODULE_NAMES.IMMUTABLE_CLUSTER ||
                visualInfo.UIType == constants.MODULE_NAMES.MUTABLE_CLUSTER
            ) {
                var clipContainer = $('<div class="clipContainer" title="Unstack"></div>');
                clipContainer.click(function() {
                    aperture.pubsub.publish(
                        chan.EXPAND_EVENT,
                        {
                            xfId : visualInfo.xfId
                        }
                    );
                    return true;
                });
                state.canvas.append(clipContainer);
            }
        };

        //--------------------------------------------------------------------------------------------------------------

        var _createExpansionElements = function(visualInfo, state) {
            state.canvas.addClass('clusterExpanded');

            // Add a margin div that will contract on click
            var marginDiv = $(document.createElement('div'));
            marginDiv.attr('title', 'Restack '+ state.spec.label);
            marginDiv.addClass('clusterBracket');
            marginDiv.append($('<div class="clusterBracketClip"></div>'));
            state.canvas.append(marginDiv);
            // Add the listener for collapsing the cluster.
            marginDiv.click(function() {
                aperture.pubsub.publish(chan.COLLAPSE_EVENT, {xfId : visualInfo.xfId});
                return false;   // prevents click handler on card from firing and re-expanding
            });

            // Adjust parent leader positions as needed.
            _updateLeaders(state);

            state.canvas.css('height','auto');

            return false;   // prevents click handler on card from firing and re-expanding
        };

        //--------------------------------------------------------------------------------------------------------------

        var _processChildren = function(childObjects, parentCanvas){
            var childCount = childObjects.length;
            for (var i=0; i < childCount; i++){
                var visualInfo = childObjects[i].getVisualInfo();
                var element = undefined;
                var objectType = childObjects[i].getUIType();
                switch(objectType){
                    case constants.MODULE_NAMES.IMMUTABLE_CLUSTER :
                    case constants.MODULE_NAMES.MUTABLE_CLUSTER :
                    case constants.MODULE_NAMES.SUMMARY_CLUSTER : {
                        element = _createElement(visualInfo, parentCanvas);
                        break;
                    }
                    case constants.MODULE_NAMES.ENTITY : {
                        element = cardRenderer.createElement(visualInfo);
                        parentCanvas.append(element);
                        break;
                    }
                    default : {
                        console.error('Attempted to add an unsupported UIObject type to column');
                    }
                }
                if (element){
                    element.addClass('clusterSubElement');
                }
            }
        };

        //--------------------------------------------------------------------------------------------------------------

        var _initElement = function(visualInfo, canvas){
            // Initialize the drag/drop logic.
            canvas.draggable('destroy');
            canvas.influentDraggable({
                revert: 'invalid',
                opacity: 0.7,
                cursor: 'move',
                stack: '.fileContainer'
            });

            canvas.data('xfId', visualInfo.xfId);
            // We need to pass the count value in so that on
            // drop, if the 'count' value is greater than the
            // threshold value, we can notify the user that
            // flattening this cluster may take a while.
            canvas.data('clusterCount', visualInfo.spec.count);
            canvas.data('entityType', visualInfo.spec.type);
            canvas.data('parent', visualInfo.spec.parent);

            // If this card is an immediate child of a column, we need
            // to add a left offset to ensure all the cards in a
            // column align.
            if (visualInfo.spec.parent.getUIType() == constants.MODULE_NAMES.COLUMN){
                canvas.css('left', _renderDefaults.CARD_LEFT);
            }

            canvas.css('padding-bottom', _renderDefaults.EXPANDED_PADDING_BOTTOM);
        };

        //--------------------------------------------------------------------------------------------------------------

        var _createElement = function(visualInfo, parentCanvas){
            var spec = visualInfo.spec;
            // Add a suffix of "_cluster" so that we can differentiate
            // between the cluster's container, and the face card
            // the represents a collapsed cluster.
            var canvas = $('#' + visualInfo.xfId + '_cluster');
            if (canvas.length > 0){
                canvas.empty();
            }
            else {
                canvas = $('<div></div>');
                canvas.attr('id', visualInfo.xfId + '_cluster');
            }

            _initElement(visualInfo, canvas);

            var _instanceState = {
                xfId : visualInfo.xfId,
                spec : _.clone(spec),
                canvas : canvas
            };

            // parent before processing children so that children can climb ancestry for leaders.
            if (parentCanvas) {
                parentCanvas.append(canvas);
            }

            // Check if the card is expanded, if it is we
            // need to process it's children and change it's visuals.
            // create all the cards for memberIds
            if (visualInfo.isExpanded){
                _createExpansionElements(visualInfo, _instanceState);
                // create all the cards for memberIds
                var childUIObjects = visualInfo.children;
                if ( childUIObjects.length > 0 ) {
                    _processChildren(childUIObjects, canvas);
                }
            }
            // The cluster is not expanded so we only need to draw
            // the visuals for its face card and the stacked card
            // elements.
            else {
                _createFaceElement(visualInfo, _instanceState);
            }

            canvas.draggable('destroy');
            canvas.influentDraggable({
                revert: 'invalid',
                opacity: 0.7,
                cursor: 'move',
                stack: '.fileContainer',
                start: function(){
                    canvas.data("origPosition", canvas.position());
                }
            });

            return canvas;
        };

        // Public Functions ------------------------------------------------------------------------------

        clusterRenderer.createElement = _createElement;
        clusterRenderer.getRenderDefaults = function(){
          return _renderDefaults;
        };
        return clusterRenderer;
    }
);