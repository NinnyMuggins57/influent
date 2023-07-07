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
        'jquery', 'lib/channels','lib/render/cardRenderer', 'lib/render/matchRenderer',
        'lib/render/clusterRenderer', 'lib/render/toolbarRenderer', 'lib/ui/xfModalDialog', 'lib/constants'
    ],
    function(
        $, chan, cardRenderer, matchRenderer,
        clusterRenderer, toolbarRenderer, xfModalDialog, constants
    ) {

        var _cardDefaults = cardRenderer.getRenderDefaults();
        var MARGIN_HOR = 10;
        var _fileRendererState = {
            maxClusterCount : 20 //TODO: Until this is loaded from a config, this value must be kept in sync with the value in toolbarRenderer.
        };
        var _renderDefaults = {
            MARGIN_LEFT : 5,
            MARGIN_TOP : 5,
            MARGIN_BOTTOM : 2,
            FILE_HEIGHT : 160,
            HEADER_HEIGHT : 38,
            FOOTER_HEIGHT : 14,
            FILE_WIDTH : _cardDefaults.CARD_WIDTH + 2*_cardDefaults.MARGIN + MARGIN_HOR,
            TITLE_HEIGHT : 18,
            TITLE_MAX_CHAR_LENGTH : 20
        };

        // Start (Private Functions) --------------------------------------------------------------------------

        var _insertIcons = function(iconList, canvas) {
            var defaults = cardRenderer.getRenderDefaults();

            var iconContainer = $('<div></div>');
            iconContainer.css('position','relative');
            iconContainer.width(defaults.CARD_WIDTH);
            iconContainer.height(defaults.ICON_HEIGHT + defaults.ELEMENT_PADDING);
            iconContainer.css('padding-top', _renderDefaults.MARGIN_TOP);

            canvas.append(iconContainer);

            if (iconList && iconList.length > 0 ) {
                var left = defaults.MARGIN;
                for (var i = 0; i < iconList.length; i++) {

                    var iconDiv = $('<div></div>');
                    iconDiv.css('position','absolute');
                    iconDiv.css('top',0);
                    iconDiv.css('left',left);
                    iconDiv.width(defaults.ICON_WIDTH).height(defaults.ICON_HEIGHT);
                    iconContainer.append(iconDiv);

                    // TODO: pull out the translation from here
                    var iconImg = $('<img/>');
                    iconImg.attr('src', aperture.palette.icon({type: iconList[i].imgUrl, width:defaults.ICON_WIDTH, height:defaults.ICON_HEIGHT}));
                    iconDiv.append(iconImg);

                    left += defaults.ICON_WIDTH + defaults.ELEMENT_PADDING;
                }
            }

            return defaults.ICON_HEIGHT + 2*defaults.ELEMENT_PADDING;
        };

        var _processChildren = function(visualInfo, parentCanvas){
            var childCluster = visualInfo.clusterUIObject;

            if (childCluster != null) {
                var visualInfo = childCluster.getVisualInfo();
                var objectType = childCluster.getUIType();

                var element = undefined;
                switch(objectType){
                    case constants.MODULE_NAMES.MUTABLE_CLUSTER : {
                        element = clusterRenderer.createElement(visualInfo);
                        break;
                    }
                    default : {
                        console.error('Attempted to add an unsupported UIObject type to file');
                    }
                }

                if (element){
                    element.css('left', _cardDefaults.CARD_LEFT);
                    parentCanvas.append(element);
                }
            }
        };

        var _constructFile = function(visualInfo, state) {
            var fileHeader = $('<div></div>');

            if(visualInfo.isHighlighted) {
                fileHeader.addClass('fileHeaderHighlighted');
            }
            else if(visualInfo.isSelected || visualInfo.isMatchHighlighted) {
                fileHeader.addClass('fileHeaderSelected');
            }
            else {
                fileHeader.addClass('fileHeader');
            }

            // Attach selection listener to the file header.
            fileHeader.click(state.onClick);

            var fileTitle = $('<div></div>');
            fileTitle.addClass('fileTitle');
            fileTitle.height(_renderDefaults.TITLE_HEIGHT);
            var fileTitleTextNode = $('<div></div>');
            fileTitleTextNode.addClass('fileTitleTextNode');
            fileTitle.append(fileTitleTextNode);
            if (visualInfo['title']) {
                fileTitleTextNode.text(visualInfo.title);
                // Attach selection listener to the file title text.
                fileTitleTextNode.click(state.onClick);
            }
            // Attach selection listener to the file title.
            fileTitle.click(state.onClick);


            var fileBody = $('<div></div>');

            if(visualInfo.isHighlighted) {
                fileBody.addClass('fileBodyHighlighted');
            }
            else if(visualInfo.isSelected || visualInfo.isMatchHighlighted) {
                fileBody.addClass('fileBodySelected');
            }
            else {
                fileBody.addClass('fileBody');
            }

            var showDetails = visualInfo.showDetails;
            fileBody.width(state.canvas.width());
            fileBody.height((visualInfo.clusterUIObject != null) ? state.canvas.height() - _renderDefaults.TITLE_HEIGHT : cardRenderer.getCardHeight(showDetails));
            fileBody.css('position','relative');


            var fileEmpty = $('<div></div>');
            fileEmpty.addClass('fileEmpty');
            fileEmpty.width(state.canvas.width()-_renderDefaults.MARGIN_LEFT*2-2);
            fileEmpty.height(cardRenderer.getCardHeight(showDetails));
            fileEmpty.css('display', '');
            fileEmpty.css('position', 'relative');
            fileEmpty.css('top', -_renderDefaults.MARGIN_TOP+'px');
            fileEmpty.css('left', _renderDefaults.MARGIN_LEFT+'px');
            fileBody.append(fileEmpty);
            // Attach selection listener to the empty file placeholder.
            fileEmpty.click(state.onClick);

            var fileFooter = $('<div></div>');

            if(visualInfo.isHighlighted) {
                fileFooter.addClass('fileFooterHighlighted');
            }
            else if(visualInfo.isSelected || visualInfo.isMatchHighlighted) {
                fileFooter.addClass('fileFooterSelected');
            }
            else {
                fileFooter.addClass('fileFooter');
            }

            // Attach selection listener to the file footer.
            fileFooter.click(state.onClick);



            var top = _cardDefaults.MARGIN;
            var iconList = state.spec['icons'];
            if ( iconList ) {
                top += _insertIcons(iconList, fileBody);
            }

            // create all the cards for memberIds
            if (visualInfo.clusterUIObject != null) {
                fileBody.css('height', 'auto');
                _processChildren(visualInfo, fileBody);
                fileEmpty.hide();
            }
            else {
                fileEmpty.show();
            }

            // if highlighted, match is already present and we need to prepend the file elements on top of it
            if(visualInfo.matchUIObject != null && visualInfo.matchUIObject.getVisualInfo().isSearchControlHighlighted) {
                state.canvas.prepend(fileFooter);
                state.canvas.prepend(fileBody);
                state.canvas.prepend(fileTitle);
                state.canvas.prepend(fileHeader);
            }
            else {
                state.canvas.append(fileHeader);
                state.canvas.append(fileTitle);
                state.canvas.append(fileBody);
                state.canvas.append(fileFooter);
            }

            // Create card controls.
            if (visualInfo.isHovered){
                toolbarRenderer.createControls(visualInfo, state.canvas, _renderDefaults.HEADER_HEIGHT + _renderDefaults.FOOTER_HEIGHT + fileBody.height());
            }

            // Determine if an xfMatch object needs to be renderered; if highlighted, the appropriate match will already exist
            if (visualInfo.matchUIObject != null && !visualInfo.isMatchHighlighted){
                var matchCanvas = matchRenderer.createElement(visualInfo.matchUIObject.getVisualInfo());
                state.canvas.append(matchCanvas);
            }

            var enterFunction = function() {
                if(!visualInfo.isHovered) {         // Notify the UIObject that it's being hovered over.
                    aperture.pubsub.publish(chan.HOVER_CHANGE_REQUEST, {
                        xfId : visualInfo.xfId
                    });
                }
                return false;
            };

            // add drop function to the core 'file' parts of the canvas.
            var dropFunction = function( event, ui ) {
                var dropId = visualInfo.xfId;
                var dragId = ui.draggable.data('xfId');

                // TODO should have card reordering (ie if we're dropping in the same file in a different position,
                // alter the order of the cards). Add animations to the reordering for bonus points.

                // first, check to see the draggable is already here; if so, we can essentially do nothing
                if (visualInfo.clusterUIObject != null) {
                    if(visualInfo.clusterUIObject.getUIObjectByXfId(dragId) != null) {
                        ui.draggable.draggable("disable");    // make the dropped card/cluster no longer draggable until the animation is done

                        ui.draggable.animate({	top: 0, left: ui.draggable.data("origPosition").left}, 	// animate the card/cluster back to it's original position
                                                "slow",
                                                "swing",
                                                function() { ui.draggable.draggable("enable"); }
                        );
                        return;
                    }
                }

                var dropSpec = {
                    containerId : dropId,
                    cardId: dragId
                };

                // Check if the drop target has children, and if
                // so, does it exceed our threshold value.
                var entityType = ui.draggable.data('entityType');
                var clusterCount = ui.draggable.data('clusterCount');
                if (entityType &&
                    (entityType == constants.MODULE_NAMES.IMMUTABLE_CLUSTER || entityType == constants.MODULE_NAMES.MUTABLE_CLUSTER) &&
                    clusterCount &&
                    clusterCount > _fileRendererState.maxClusterCount
                ){
                    var parentUIObject = ui.draggable.data('parent');
                    xfModalDialog.createInstance({
                        title : 'Add Cluster to File?',
                        contents : 'Adding large clusters to file may take longer than expected. Do you wish to continue?',
                        buttons : {
                            "Add File" : function() {
                                setTimeout((function() {
                                    ui.draggable.detach();    // make the dropped card/cluster no longer draggable, at least until the drop goes through
                                    aperture.pubsub.publish(chan.DROP_EVENT, dropSpec);
                                }).bind(ui.draggable),50);
                            },
                            "Cancel" : function() {
                                // Since the drop has been cancelled and we haven't updated
                                // any models, we only need to restore the visual state by
                                // triggering a render call on the drag item's parent.
                                aperture.pubsub.publish(chan.RENDER_UPDATE_REQUEST, {UIObject : parentUIObject});
                            }
                        }
                    });
                }
                else {
                    setTimeout((function() {
                        ui.draggable.detach();    // make the dropped card/cluster no longer draggable, at least until the drop goes through
                        aperture.pubsub.publish(chan.DROP_EVENT, dropSpec);
                    }).bind(ui.draggable),50);
                }
            };

            fileHeader.droppable( {drop: dropFunction} );
            fileBody.droppable( {drop: dropFunction} );
            fileFooter.droppable( {drop: dropFunction} );

            fileHeader.mouseenter(enterFunction);
            fileBody.mouseenter(enterFunction);
            fileFooter.mouseenter(enterFunction);

            // Title editing methods
            function startEditingTitle() {
                var currentTitle = $(fileTitle).contents();
                var oldTitleText = currentTitle.html();

                var editableTitle = $("<input />");
                editableTitle.css({ "font-family" : currentTitle.css("font-family"),
                                    "font-size" : currentTitle.css("font-size"),
                                    "width" : currentTitle.width(),
                                    "background-color" : "transparent",
                                    "margin-top" : "2px"});                 // can't be the same as fileTitleTextNode in .less due to differences between <input> and <div>
                editableTitle.attr("maxlength", _renderDefaults.TITLE_MAX_CHAR_LENGTH);
                editableTitle.val(oldTitleText);
                currentTitle.replaceWith(editableTitle);

                // clicking outside, double-clicking, or special keypresses end editing
                editableTitle.focus();
                editableTitle.blur(doneEditingTitle);
                editableTitle.keypress(function(e) {
                    switch (e.keyCode){
                        case 9:
                        case 13:
                        case 27:
                        case 33:
                        case 34:
                        case 35:
                        case 36:
                            doneEditingTitle();
                            break;
                    }
                });
            }

            function doneEditingTitle() {
                var currentTitle = $(fileTitle).contents();
                var newTitleText = currentTitle.val();

                if(newTitleText.length == 0) {
                    newTitleText = '(empty name)';
                }

                var newTitle = $('<div></div>');

                aperture.pubsub.publish(chan.CHANGE_FILE_TITLE, {
                    xfId: visualInfo.xfId,
                    newTitleText: newTitleText,
                    noRender: true
                });

                newTitle.addClass('fileTitleTextNode');
                newTitle.text(newTitleText);
                newTitle.click(state._onClick);

                currentTitle.replaceWith(newTitle);
                newTitle.mousedown(function(){ return false; });
                newTitle.dblclick(startEditingTitle);
            }

            fileTitleTextNode.mousedown(function(){ return false; });
            fileTitleTextNode.dblclick(startEditingTitle);
        };

        // End (Private Functions) --------------------------------------------------------------------------

        var fileRenderer = {};
        fileRenderer.createElement = function(visualInfo) {
            var spec = visualInfo.spec;
            var canvas = $('#' + visualInfo.xfId);
	        if (canvas.length > 0) {
    	    	// DO NOT remove the match card container, as it has it's own renderer and will be re-created if needed
        		canvas.children().not('.matchCardContainer').remove();
        	} else {
                canvas = $('<div class="file"></div>');
                canvas.attr('id', visualInfo.xfId);
            }

            canvas.width(_renderDefaults.FILE_WIDTH);

    // commented out until this is working. note that this is causing a drop pane to cover up scrollbars currently.
    //        canvas.influentDraggable({
    //            revert: 'invalid',
    //            opacity: 0.7,
    //            cursor: 'move',
    //            stack: '.fileContainer',
    //            helper: 'clone',
    //            start: function(event,ui) {
    //                aperture.pubsub.publish(chan.ADD_DROP_TARGETS);
    //            },
    //            stop: function(event,ui) {
    //                aperture.pubsub.publish(chan.REMOVE_DROP_TARGETS);
    //            },
    //            drag : function(event, ui) {
    //                aperture.pubsub.publish(chan.UPDATE_DROP_TARGETS, {mouseOffset: {left: event.clientX, top: event.clientY}});
    //            }
    //        });

            var _onClick = function() {
                var selectionState = !visualInfo.isSelected;
                return selectionState;
            };

            var _instanceState = {
                xfId : visualInfo.xfId,
                canvas : canvas,
                spec : _.clone(spec),
                onClick : _onClick
            };

            _constructFile(visualInfo, _instanceState);

            return canvas;
        };

        fileRenderer.getRenderDefaults = function(){
          return _renderDefaults;
        };
        return fileRenderer;
    }
);