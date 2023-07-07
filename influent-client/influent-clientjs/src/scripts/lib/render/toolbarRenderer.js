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
		'lib/channels', 'lib/util/xfUtil', 'lib/ui/toolbarOperations', 'lib/ui/xfModalDialog', 'lib/constants', 'lib/util/currency', 'lib/plugins'
	],
	function(
		chan, xfUtil, toolbarOp, xfModalDialog, constants, currency, plugins
	) {
		var _toolbarState = {
			canvas : null,
			toolbarDiv : null,
			searchDiv : null,
			leftOp : null,
			rightOp : null,
			leftOpState : null,
			rightOpState : null,
			maxClusterCount : 20, //TODO: Until this is loaded from a config, this value must be kept in sync with the value in fileRenderer.
			matchDiv : null
		};

		var _renderDefaults = {
			TOOLBAR_BTN_HEIGHT : 20,
			CARD_BUTTON_HEIGHT : 24
		};

		//--------------------------------------------------------------------------------------------------------------

		var _isVisualDescendant = function(visualInfo, uiType){
			var parentObj = visualInfo.spec.parent;
			if (_.isEmpty(parentObj) // Parent has not been set - newly created item.
				|| parentObj.getUIType() === constants.MODULE_NAMES.COLUMN){
				return false;
			}
			if (parentObj.getUIType() === uiType){
				return true;
			}
			return _isVisualDescendant(parentObj.getVisualInfo(), uiType);
		};

		//--------------------------------------------------------------------------------------------------------------

		var _getVisualAncestor = function(visualInfo, uiType){
			var parentObj = visualInfo.spec.parent;
			if (_.isEmpty(parentObj) // Parent has not been set - newly created item.
				|| visualInfo.UIType === constants.MODULE_NAMES.COLUMN){
				return null;
			}
			if (parentObj.getUIType() === uiType){
				return parentObj;
			}
			return _getVisualAncestor(parentObj.getVisualInfo(), uiType);
		};

		//--------------------------------------------------------------------------------------------------------------

		var _resetControls = function(){
			if (_toolbarState.toolbarDiv){
				_toolbarState.toolbarDiv.remove();
			}
			if (_toolbarState.matchDiv){
				_toolbarState.matchDiv.remove();
			}
			if (_toolbarState.searchDiv){
				_toolbarState.searchDiv.remove();
			}
			if (_toolbarState.leftOp && _toolbarState.leftOpState !== toolbarOp.WORKING){
				_toolbarState.leftOp.remove();
			}
			if (_toolbarState.rightOp && _toolbarState.rightOpState !== toolbarOp.WORKING){
				_toolbarState.rightOp.remove();
			}
		};

		//--------------------------------------------------------------------------------------------------------------

		var _getStateElement = function(selector){
			var element = _toolbarState.canvas.children(selector).first();
			return (element.length === 0)?null:element;
		};

		//--------------------------------------------------------------------------------------------------------------

		var _getDegreeLabel = function(degree, shownDegree, inOrOut) {

			// Test for equality here to handle numbers and strings
			if (degree == '0') {
				return 'no links';
			}
			if (degree == 'null') {
				return '';
			}
			var d = parseInt(degree);
			if (isNaN(d)) {
				return '';
			}
			return 'showing ' + currency.formatNumber(shownDegree) + '/' + currency.formatNumber(d) + '<br> '+ inOrOut;
		};

		//--------------------------------------------------------------------------------------------------------------

		var _createBranchControls = function(visualInfo, cardHeight){
			_toolbarState.leftOp = _getStateElement('.leftOp');
			var leftImg = null;
			var leftOp = null;
			var inDegree = (visualInfo.spec.inDegree != null ? visualInfo.spec.inDegree : 0);
			var outDegree = (visualInfo.spec.outDegree != null ? visualInfo.spec.outDegree : 0);
			var shownInDegree = 0;
			var shownOutDegree = 0;

			// compute the shown in/out degree
			for (var xfId in visualInfo.links) {
				if (visualInfo.links.hasOwnProperty(xfId)) {
					var link = visualInfo.links[xfId];
					if (link.getDestination().getXfId() === visualInfo.xfId) {
						shownInDegree+= link.getLinkCount();
					} else {
						shownOutDegree+= link.getLinkCount();
					}
				}
			}

			if (_toolbarState.leftOp == null) {
				_toolbarState.leftOp = leftOp = $('<div class="leftOp"></div>')
					.appendTo(_toolbarState.canvas);

				if (inDegree !== 0) {
					leftImg = xfUtil.makeButton('branch left to inflowing sources', 'expand', null, 'card-button', null)
						.addClass('branch-button')
						.appendTo(leftOp);
				}

				$('<div class="opDegree leftOpDegree"></div>')
					.html(_getDegreeLabel(inDegree, shownInDegree, '<span class="opArrow">&#x2039;</span> sources'))
					.appendTo(leftOp);

				_toolbarState.leftOpState = toolbarOp.BRANCH;
			} else {
				leftOp = _toolbarState.leftOp;
				leftImg = $(_toolbarState.leftOp.children()[0]);
			}

			var requestedLeftOp = visualInfo.spec.leftOperation;

			if (leftImg != null) {
				leftImg.unbind('click');
				leftImg.removeClass('button-branching');
				if (requestedLeftOp === toolbarOp.BRANCH) {
					leftImg.click(
							function(event) {
								publishBranchRequest('left');
								event.stopImmediatePropagation();
							}
					);
				} else if (requestedLeftOp === toolbarOp.WORKING) {
					leftImg.addClass('button-branching');
				}
				_toolbarState.leftOpState = requestedLeftOp;
			}
			leftOp.css('top', (cardHeight - _renderDefaults.CARD_BUTTON_HEIGHT)/2);

			_toolbarState.rightOp = _getStateElement('.rightOp');
			var rightImg = null;
			var rightOp = null;
			if (_toolbarState.rightOp == null) {
				_toolbarState.rightOp = rightOp = $('<div class="rightOp"></div>')
					.appendTo(_toolbarState.canvas);

				if (outDegree !== 0) {
					rightImg = xfUtil.makeButton('branch right to outflowing destinations', 'expand', null, 'card-button', null)
						.addClass('branch-button')
						.appendTo(rightOp);
				}

				$('<div class="opDegree rightOpDegree"></div>')
					.appendTo(rightOp)
					.html(_getDegreeLabel(outDegree, shownOutDegree, 'destinations <span class="opArrow">&#x203A;</span>'));

			} else {
				rightOp = _toolbarState.rightOp;
				rightImg = $(_toolbarState.rightOp.children()[0]);
			}

			var requestedRightOp = visualInfo.spec.rightOperation;

			if (rightImg != null) {
				rightImg.unbind('click');
				rightImg.removeClass('button-branching');
				if (requestedRightOp === toolbarOp.BRANCH) {
					rightImg.click(
							function(event) {
								publishBranchRequest('right');
								event.stopImmediatePropagation();
							}
					);
				} else if (requestedRightOp === toolbarOp.WORKING ) {
					rightImg.addClass('button-branching');
				}
				_toolbarState.rightOpState = requestedRightOp;
			}
			rightOp.css('top', (cardHeight - _renderDefaults.CARD_BUTTON_HEIGHT)/2);

			var publishBranchRequest = function(direction) {
				aperture.pubsub.publish(
					chan.BRANCH_REQUEST,
					{
						xfId : visualInfo.xfId,
						direction : direction
					}
				);
			};
		};

		//--------------------------------------------------------------------------------------------------------------

		var _createToolbar = function(visualInfo){
			// Create toolbar buttons.
			_toolbarState.toolbarDiv = _getStateElement('.cardToolbar');
			var buttonItemList = [];

			if (_toolbarState.toolbarDiv == null){
				_toolbarState.toolbarDiv = $('<div class="cardToolbar"></div>');

				_toolbarState.canvas.append(_toolbarState.toolbarDiv);

				// If this card is a branch result, show
				// the additional controls like the
				// new file folder button, etc.
				if (visualInfo.showToolbar){
					// File button
					var bFileable = visualInfo.toolbarSpec['allowFile'];
					if (bFileable === true) {
						var isCopy =
							visualInfo.spec.parent.getUIType() === constants.MODULE_NAMES.IMMUTABLE_CLUSTER;

						var fileButton = xfUtil.makeButton(isCopy? 'copy to new file':'move to new file', 'new-file', null, 'card-button', null).click(
							function() {
								// Check if this cluster is within the max. cluster count threshold.
								// If it isn't, alert the user that this action may take a while.
								var clusterCount = visualInfo.spec.count;
								var isLargeCluster = clusterCount && clusterCount > _toolbarState.maxClusterCount;

								aperture.pubsub.publish(chan.CREATE_FILE_REQUEST, {xfId : visualInfo.xfId, showSpinner : isLargeCluster});
								return false;
							});

						buttonItemList.push(fileButton);
					}

					// Search button
					var bSearchable = visualInfo.toolbarSpec['allowSearch'];
					if (bSearchable === true) {
						var searchTip = visualInfo.UIType === constants.MODULE_NAMES.FILE?
								'search for accounts to add' : 'search for similar accounts';

						var searchButton = xfUtil.makeButton(searchTip, 'search-small', null, 'card-button', null).click(
							function() {
								var rootFile = _getVisualAncestor(visualInfo, constants.MODULE_NAMES.FILE);
								if (visualInfo.UIType === constants.MODULE_NAMES.FILE || rootFile != null) {
									var fileVisualInfo = (rootFile == null) ? visualInfo : rootFile.getVisualInfo();
									var matchUIObj = fileVisualInfo.matchUIObject;
									if(matchUIObj == null) {
										aperture.pubsub.publish(
											chan.SHOW_MATCH_REQUEST,
											{
												xfId : fileVisualInfo.xfId
											}
										);
									}

									// if did not click a file...
									if (fileVisualInfo.xfId !== visualInfo.xfId) {

										aperture.pubsub.publish(
											chan.ADVANCE_SEARCH_DIALOG_REQUEST,
											{
												fileId : fileVisualInfo.xfId,
												terms : null,
												dataIds : xfUtil.getContainedCardDataIds(visualInfo),
												contextId : fileVisualInfo.spec.dataId
											}
										);
									}
								} else {
									aperture.pubsub.publish(chan.CREATE_FILE_REQUEST, {xfId : visualInfo.xfId, showMatchCard : true});
								}
								return false;
							});

						buttonItemList.push(searchButton);
					}

					// Focus button
					var bFocusable = visualInfo.toolbarSpec['allowFocus'];
					if ( bFocusable === true) {
						var highlightButton = xfUtil.makeButton('highlight flow', 'highlight-flow', null, 'card-button', null).click(
							function() {
								aperture.pubsub.publish(chan.FOCUS_CHANGE_REQUEST, {xfId : visualInfo.xfId});
								return false;
							});

						buttonItemList.push(highlightButton);
					}

					// xfFile objects require different positioning due to the asymmetrical shape of the file tabs.
					if (visualInfo.UIType === constants.MODULE_NAMES.FILE){
						_toolbarState.toolbarDiv.css('top', -0.5*_renderDefaults.CARD_BUTTON_HEIGHT - 6);
						_toolbarState.toolbarDiv.css('right', 10);
					}
					else {
						_toolbarState.toolbarDiv.css('top', -_renderDefaults.CARD_BUTTON_HEIGHT - 5);
						_toolbarState.toolbarDiv.css('right', 0);
					}
				}
				else {
					_toolbarState.toolbarDiv.css('right', 5);
					_toolbarState.toolbarDiv.css('top', 5);
				}

				// Add default Close button
				var bCloseable = visualInfo.toolbarSpec.allowClose;
				if ( bCloseable === true || bCloseable === undefined ) {
					var closeButton = xfUtil.makeButton('remove', 'remove', null, 'card-button', null).click(
						function() {
							if (visualInfo.UIType === constants.MODULE_NAMES.FILE){
								xfModalDialog.createInstance({
									title : 'Remove File?',
									contents : 'Are you sure you want to remove "<b>' + visualInfo.title + '"</b>?',
									buttons : {
										'Remove' : function() {
											aperture.pubsub.publish(
												chan.REMOVE_REQUEST,
												{
													xfIds : [visualInfo.xfId],
													dispose : true,
                                                    userRequested : true
												}
											);
										},
										'Cancel' : function() {}
									}
								});
							}
							else {
								aperture.pubsub.publish(
									chan.REMOVE_REQUEST,
									{
										xfIds : [visualInfo.xfId],
										dispose : true,
                                        userRequested : true
									}
								);
							}
							return false;
						}
					);

					buttonItemList.push(closeButton);

				}

				_createPluginButtons(visualInfo, buttonItemList);
			}

			for (var i=0; i < buttonItemList.length; i++){
				_toolbarState.toolbarDiv.append(buttonItemList[i]);
			}

		};

		//--------------------------------------------------------------------------------------------------------------

		/**
		 * Creates custom controls for the search results
		 * contained within a xfMatch uiObject.
		 * @param visualInfo
		 * @param cardHeight
		 * @private
		 */
		var _createMatchControls = function(visualInfo){
			_toolbarState.matchDiv = _getStateElement('.matchToolbar');
			var buttonItemList = [];

			if (_toolbarState.matchDiv == null){
				_toolbarState.matchDiv = $('<div class="matchToolbar"></div>');
				_toolbarState.canvas.append(_toolbarState.matchDiv);

				// Focus button
				var bFocusable = visualInfo.toolbarSpec['allowFocus'];
				if ( bFocusable === true) {
					var focusBtn = xfUtil.makeButton('highlight flow', 'highlight-flow', null, 'card-button', null)
						.click(function() {
							aperture.pubsub.publish(chan.FOCUS_CHANGE_REQUEST, {xfId : visualInfo.xfId});
							return false;
						});

					buttonItemList.push(focusBtn);

				}

				// Add move-to-file button.
				// Find the parent xfFile for this xfMatch.
				var fileObj = _getVisualAncestor(visualInfo, constants.MODULE_NAMES.FILE);
				if (fileObj != null){
					var fileBtn = xfUtil.makeButton('add to file', 'add-to-file', null, 'card-button', null)
						.click(function(){
							aperture.pubsub.publish(chan.ADD_TO_FILE_REQUEST, {
								containerId : fileObj.getXfId(),
								cardId: visualInfo.xfId
							});
							return false;
						});
					buttonItemList.push(fileBtn);
				}
				else {
					aperture.log.error('There is no parent xfFile associated with the xfMatch: ' + visualInfo.xfId);
				}

				// Add close button.
				var closeBtn = xfUtil.makeButton('remove', 'remove', null, 'card-button', null)
					.click(
						function() {
							aperture.pubsub.publish(
								chan.REMOVE_REQUEST,
								{
									xfIds : [visualInfo.xfId],
									dispose : true
								}
							);
							return false;
						}
					);

				buttonItemList.push(closeBtn);

				_createPluginButtons(visualInfo, buttonItemList);
			}

			for (var i=0; i < buttonItemList.length; i++){
				_toolbarState.matchDiv.append(buttonItemList[i]);
			}
		};

		//--------------------------------------------------------------------------------------------------------------

		var _createControls = function(visualInfo, parentCanvas, cardHeight) {
			if (_toolbarState.canvas != null){
				_resetControls();
			}

			// Create the toolbar buttons.
			_toolbarState.canvas = parentCanvas;

			// If this is an xfMatch object, we want to use a
			// custom button layout, otherwise use the default
			// toolbar.
			if (_isVisualDescendant(visualInfo, constants.MODULE_NAMES.MATCH)){
				_createMatchControls(visualInfo);
			}
			else {
				_createToolbar(visualInfo);
			}

			// Add expand right/left buttons.
			if (!(_isVisualDescendant(visualInfo, constants.MODULE_NAMES.MATCH) ||
				visualInfo.UIType === constants.MODULE_NAMES.FILE)){
				_createBranchControls(visualInfo, cardHeight);
			}
		};

		//--------------------------------------------------------------------------------------------------------------

		var _createPluginButtons = function(visualInfo, buttonItemList) {
			var extensions = plugins.get('cards');
			aperture.util.forEach(extensions, function(e) {
				if (e.toolbar) {
					var cardspec = {
						dataId : visualInfo.spec.dataId,
						type : visualInfo.spec.accounttype
					};

					var ebuttons = e.toolbar(cardspec);
					if (ebuttons) {
						if (!aperture.util.isArray(ebuttons)) {
							ebuttons = [ebuttons];
						}

						aperture.util.forEach(ebuttons, function(ebutton) {
							buttonItemList.splice(0,0,
								xfUtil.makeButton(ebutton.title || '', ebutton.icon, null, 'card-button', null).click(
									function() {
										if (ebutton.click) {
											return ebutton.click(arguments);
										}
									})
							);
						});
					}
				}
			});
		};

		//--------------------------------------------------------------------------------------------------------------

		return {
			createControls : _createControls,
			getRenderDefaults : function(){
				return _.clone(_renderDefaults);
			}
		};
	}
);
