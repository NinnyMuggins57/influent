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
		'lib/channels','lib/render/cardRenderer', 'lib/render/matchRenderer',
		'lib/render/clusterRenderer', 'lib/render/toolbarRenderer', 'lib/ui/xfModalDialog', 'lib/constants'
	],
	function(
		chan, cardRenderer, matchRenderer,
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
				visualInfo = childCluster.getVisualInfo();

				var element = clusterRenderer.createElement(visualInfo);
				if (element){
					element.css('left', _cardDefaults.CARD_LEFT);
					parentCanvas.append(element);
				}
			}
		};

		var _getFileHeight = function(showDetails){
			return cardRenderer.getCardHeight(showDetails) +
					_renderDefaults.MARGIN_TOP +
					_renderDefaults.MARGIN_BOTTOM +
					_renderDefaults.HEADER_HEIGHT +
					_renderDefaults.FOOTER_HEIGHT +
					_renderDefaults.TITLE_HEIGHT;
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
			fileBody.css('position','relative');
			fileBody.css('height', 'auto');

			var fileEmpty = $('<div></div>');
			fileEmpty.addClass('fileEmpty');
			fileEmpty.width(state.canvas.width()-_renderDefaults.MARGIN_LEFT*2-2);
			fileEmpty.height(cardRenderer.getCardHeight(showDetails) + _renderDefaults.MARGIN_TOP*2-3);
			fileEmpty.css('display', '');
			fileEmpty.css('position', 'relative');
			fileEmpty.css('top', -_renderDefaults.MARGIN_TOP+'px');
			fileEmpty.css('left', _renderDefaults.MARGIN_LEFT+'px');

			// Determine if the top-level wait spinner is visible.
			if (visualInfo.showSpinner){
				var spinnerContainer = $('<div></div>');
				spinnerContainer.attr('id', 'spinnerContainer_'+visualInfo.xfId);
				fileEmpty.append(spinnerContainer);
				spinnerContainer.css('background', constants.AJAX_SPINNER_BG);
				spinnerContainer.width(_renderDefaults.FILE_WIDTH);
				spinnerContainer.height(cardRenderer.getCardHeight(showDetails));
				spinnerContainer.css('top', -_renderDefaults.MARGIN_TOP+'px');
				spinnerContainer.css('left', _renderDefaults.MARGIN_LEFT+'px');
			}

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
			if ( visualInfo.clusterUIObject != null &&
				!visualInfo.clusterUIObject.getVisualInfo().isHidden) {
				_processChildren(visualInfo, fileBody);
				fileEmpty.hide();
			}
			else {
				fileEmpty.show();
			}

			// prepend in reverse order to account for the fact that the match card is a child
			// but it is not managed by this file renderer. match cards always sit below.
			state.canvas.prepend(fileFooter);
			state.canvas.prepend(fileBody);
			state.canvas.prepend(fileTitle);
			state.canvas.prepend(fileHeader);

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

				if (visualInfo.clusterUIObject != null) {

					// first, check to see the draggable is already here; if so, we can essentially do nothing
					if (visualInfo.clusterUIObject.getVisualInfo() == null || visualInfo.clusterUIObject.getUIObjectByXfId(dragId) != null) {
						ui.draggable.draggable('disable');    // make the dropped card/cluster no longer draggable until the animation is done

						ui.draggable.animate({	top: 0, left: ui.draggable.data('origPosition').left}, // animate the card/cluster back to it's original position
												'slow',
												'swing',
												function() { ui.draggable.draggable('enable'); }
						);
						return;
					}
				}

				var clusterCount = ui.draggable.data('clusterCount');
				var isLargeCluster = clusterCount && clusterCount > _fileRendererState.maxClusterCount;

				var dropSpec = {
						containerId : dropId,
						cardId: dragId,
						showSpinner: isLargeCluster
					};

				setTimeout(function() {
					ui.draggable.detach();    // make the dropped card/cluster no longer draggable, at least until the drop goes through
					aperture.pubsub.publish(chan.DROP_EVENT, dropSpec);
				}.bind(ui.draggable),50);
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

				var editableTitle = $('<input />');
				editableTitle.css({ 'font-family' : currentTitle.css('font-family'),
									'font-size' : currentTitle.css('font-size'),
									'width' : currentTitle.width(),
									'background-color' : 'transparent',
									'margin-top' : '2px'});                 // can't be the same as fileTitleTextNode in .less due to differences between <input> and <div>
				editableTitle.attr('maxlength', _renderDefaults.TITLE_MAX_CHAR_LENGTH);
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
							doneEditingTitle();
							break;
					}
				});
			}

			function doneEditingTitle() {
				var currentTitle = $(fileTitle).contents();
				var newTitleText = currentTitle.val();

				if(newTitleText.length === 0) {
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

			var _onClick = function() {
				return !visualInfo.isSelected;
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

		fileRenderer.getFileHeight = _getFileHeight;

		return fileRenderer;
	}
);
