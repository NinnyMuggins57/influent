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
define(['modules/xfWorkspace', 'lib/module', 'lib/channels', 'lib/constants'],
	function(xfWorkspace, modules, chan, constants) {

		//--------------------------------------------------------------------------------------------------------------
		// Private Variables
		//--------------------------------------------------------------------------------------------------------------

		var MODULE_NAME = 'xfEntityDetails';

		var _UIObjectState = {
			UIType : MODULE_NAME,
			properties : {},
			parentId : '',
			subscriberTokens : null,
			lastRequestedObject : null
		};

		var _renderDefaults = {
			SCORE_BAR_WIDTH     : 28,
			SCORE_BAR_HEIGHT    : 4
		};

		//--------------------------------------------------------------------------------------------------------------
		// Private Methods
		//--------------------------------------------------------------------------------------------------------------

		var _onSearch = function(channel, data) {
			$('#details').empty();
		};

		//--------------------------------------------------------------------------------------------------------------

		var _onSelect = function(channel, data) {

			// Only retrieve details for xfCluster and xfCard objects.
			if (data != null) {

				_UIObjectState.lastRequestedObject = data.xfId;

				if (!data.hasOwnProperty('imageIdx')) {
					data.imageIdx = 0;
				}

				if (data.uiType === constants.MODULE_NAMES.ENTITY)
				{
					// cache icon set
					var iconList = xfWorkspace.getUIObjectByXfId(data.xfId).getVisualInfo().spec['icons'];

					// get details for entity here
					aperture.io.rest(
						'/entitydetails',
						'POST',
						function(response){
							$('#details').html(response.content);

							// FIXME: Copy the label from the card's visualInfo to ensure any count is correct
							var obj = xfWorkspace.getUIObjectByXfId(data.xfId);
							if (obj !== null) {
								$('#detailsHeaderInfo').find('.detailsEntityLabel').html('<b>' + obj.getVisualInfo().spec.label + '</b>');
							}

							// Add image carousel buttons if necessary
							_addImageCarouselButtons(data);

							var parent = $('<div class="detailsIconContainer"></div>').appendTo('#detailsHeaderInfo');
							var url;
							var icon;
							var i;

							if (iconList) {
								for (i = 0; i < iconList.length; i++) {
									icon = iconList[i];

									// supersize it
									url = icon.imgUrl
										.replace(/iconWidth=[0-9]+/, 'iconWidth=32')
										.replace(/iconHeight=[0-9]+/, 'iconHeight=32');

									parent.append(
										'<img class= "detailsIcon" src="' +
										url +
										'" title="' +
										icon.title +
										'"/>'
									);
								}
							}
						},
						{
							postData : {
								entityId : data.dataId,
								imageIdx : data.imageIdx
							},
							contentType: 'application/json'
						}
					);
				} else if (data.hasOwnProperty('ownerId') && data.ownerId !== '') {

					// get details for entity here
					aperture.io.rest(
						'/entitydetails',
						'POST',
						function(response){
							$('#details').html(response.content);

							// FIXME: Copy the label from the card's visualInfo to ensure any count is correct
							var obj = xfWorkspace.getUIObjectByXfId(data.xfId);
							if (obj !== null) {
								$('#detailsHeaderInfo').find('.detailsEntityLabel').html('<b>' + obj.getVisualInfo().spec.label + '</b>');
							}

							// Add image carousel buttons if necessary
							_addImageCarouselButtons(data);

							_createClusterSummaryDetails(data.xfId);
						},
						{
							postData : {
								entityId : data.ownerId,
								imageIdx : data.imageIdx
							},
							contentType: 'application/json'
						}
					);

				} else {
					$('#details').empty();
					aperture.pubsub.publish(chan.REQUEST_OBJECT_BY_XFID, {xfId:data.xfId});
				}
			} else {
				_UIObjectState.lastRequestedObject = null;
				$('#details').html('');
			}
		};

		//--------------------------------------------------------------------------------------------------------------

		var _createClusterDetails = function(eventChannel, data) {

			var obj = data.clonedObject;

			if (eventChannel !== chan.OBJECT_FROM_XFID ||
				_UIObjectState.lastRequestedObject == null ||
				obj.getXfId() !== _UIObjectState.lastRequestedObject
			) {
				return;
			}

			var spec = obj.getVisualInfo().spec;

			var div = $('#details');

			var detailsHeader = $('<div class="detailsHeader"></div>').appendTo(div);
			detailsHeader.append('<span class="detailsTitle">Cluster Member Summary<span><br>');

			var label = spec.label;

			// HACK: append cluster count for mutable clusters
			if (spec.count && spec.count > 1 &&
				obj.getVisualInfo().UIType === constants.MODULE_NAMES.MUTABLE_CLUSTER) {

				// Some clusters may already have a count appended
				if (!(label.indexOf('(+') !== -1 && label.charAt(label.length - 1) === ')')) {
					label += ' (+' + (spec.count - 1) + ')';
				}
			}

			detailsHeader.append('<span class="detailsEntityLabel">'+ label+ '<span>');

			var detailsBody = $('<div></div>');
			detailsBody.attr('id', 'detailsBody');
			div.append(detailsBody);

			var iconClassOrder = aperture.config.get()['influent.config'].iconOrder;
			var iconClassMap = aperture.config.get()['influent.config'].iconMap;

			for(var i = 0; i < iconClassOrder.length; i++) {
				_addPropertyBlock(detailsBody, iconClassOrder[i], iconClassMap, spec);
			}
		};

		//--------------------------------------------------------------------------------------------------------------

		var _createClusterSummaryDetails = function(xfId) {

			var obj = xfWorkspace.getUIObjectByXfId(xfId);

			// no longer accessible, disable selection
			if(obj === null) {
				aperture.pubsub.publish(
					chan.SELECTION_CHANGE_REQUEST,
					{
						xfId: null,
						selected : true,
						noRender: true
					}
				);
				return;
			}

			var iconList = xfWorkspace.getUIObjectByXfId(xfId).getVisualInfo().spec['icons'];

			var parent = $('<div class="detailsIconContainer"></div>').appendTo('#detailsHeaderInfo');
			var url;
			var icon;
			var i;

			if (iconList) {
				for (i = 0; i < iconList.length; i++) {
					icon = iconList[i];

					// supersize it
					url = icon.imgUrl
						.replace(/iconWidth=[0-9]+/, 'iconWidth=32')
						.replace(/iconHeight=[0-9]+/, 'iconHeight=32');

					parent.append(
						'<img class= "detailsIcon" src="' +
							url +
							'" title="' +
							icon.title +
							'"/>'
					);
				}
			}

			var spec = obj.getVisualInfo().spec;

			var div = $('#details');

			var detailsHeader = $('<div class="detailsHeader"></div>').appendTo(div);
			detailsHeader.append('<span class="detailsTitle">Owner Account Summary<span><br>');

			var label = spec.label;

			detailsHeader.append('<span class="detailsEntityLabel">'+ label+ '<span>');

			var detailsBody = $('<div></div>');
			detailsBody.attr('id', 'detailsBody');
			div.append(detailsBody);

			var iconClassOrder = aperture.config.get()['influent.config'].iconOrder;
			var iconClassMap = aperture.config.get()['influent.config'].iconMap;

			for(i = 0; i < iconClassOrder.length; i++) {
				_addPropertyBlock(detailsBody, iconClassOrder[i], iconClassMap, spec);
			}
		};

		//--------------------------------------------------------------------------------------------------------------

		var _addPropertyBlock = function(parent, iconClass, iconClassMap, spec) {

			var row = null;
			var detailsBlockBody = null;

			if (iconClassMap[iconClass]) {
				for (var j = 0; j < spec.icons.length; j++) {

					var icon = spec.icons[j];
					if (icon.type === iconClass) {

						if (row == null) {
							detailsBlockBody = $('<div class="detailsBlockBody"></div>');

							row = $('<div class="detailsBlock"></div>');
							row.append('<div class="detailsBlockHeader">'+ icon.friendlyName +'</div>');
							row.append(detailsBlockBody);
						}

						var valueBlock = $('<div class="detailsValueBlock"></div>').appendTo(detailsBlockBody);

						var imgUrl = icon.imgUrl;

						// ick.
						imgUrl = imgUrl.replace(/iconWidth=[0-9]+/, 'iconWidth=28')
								.replace(/iconHeight=[0-9]+/, 'iconHeight=28');

						// icon
						var image = $('<img>');
						image.attr('src', imgUrl);
						image.attr('title', icon.title);

						var imageDiv = $('<div class="detailsValueIcon"></div>').appendTo(valueBlock);
						imageDiv.append(image);

						// value
						valueBlock.append('<div class="detailsValueValue">' + icon.title + '</div>');

						// score bar
						var scoreBarContainer = $('<div class="detailsValueScoreBar"></div>');

						var score = ''+ Math.round(icon.score * 100) + '%';
						scoreBarContainer.attr('title', score);

						var scoreBarBackground = $('<div></div>');
						scoreBarContainer.append(scoreBarBackground);
						scoreBarBackground.addClass('scoreBarBackground');
						scoreBarBackground.width(_renderDefaults.SCORE_BAR_WIDTH);
						scoreBarBackground.height(_renderDefaults.SCORE_BAR_HEIGHT);

						if (icon.score > 0) {
							var scoreBarForeground = $('<div></div>');
							scoreBarContainer.append(scoreBarForeground);
							scoreBarForeground.addClass('scoreBarForeground');
							scoreBarForeground.width((_renderDefaults.SCORE_BAR_WIDTH -1) * icon.score);
							scoreBarForeground.height(_renderDefaults.SCORE_BAR_HEIGHT);
						}

						valueBlock.append(scoreBarContainer);

						// score value
						valueBlock.append('<div class="detailsValueScore">' + score + '</div>');
					}
				}
			}

			if (row != null) {
				parent.append(row);
			}
		};

		var _addImageCarouselButtons = function(data) {

			var controls = $('#detailsHeaderPhoto').find('.photoCarouselControls');
			if (controls) {

				var prevButton = $('<button></button>').button({
					icons: {
						primary: 'ui-icon-carat-1-w'
					},
					text:false
				}).addClass('prevPhotoButton');
				prevButton.click(function() {
					data.imageIdx -= 1;
					aperture.pubsub.publish(chan.SELECTION_CHANGE_EVENT, data);
				});

				var nextButton = $('<button></button>').button({
					icons: {
						primary: 'ui-icon-carat-1-e'
					},
					text:false
				}).addClass('nextPhotoButton');
				nextButton.click(function() {
					data.imageIdx += 1;
					aperture.pubsub.publish(chan.SELECTION_CHANGE_EVENT, data);
				});

				controls.prepend(prevButton);
				controls.append(nextButton);
			}
		};

		//--------------------------------------------------------------------------------------------------------------
		// Public
		//--------------------------------------------------------------------------------------------------------------

		var xfEntityDetailsModule = {};

		//--------------------------------------------------------------------------------------------------------------

		// Register the module with the system
		modules.register('xfEntityDetails', function() {
			return {
				start : function() {
					var subTokens = {};
					subTokens[chan.SELECTION_CHANGE_EVENT] = aperture.pubsub.subscribe(chan.SELECTION_CHANGE_EVENT, _onSelect);
					subTokens[chan.SEARCH_REQUEST] = aperture.pubsub.subscribe(chan.SEARCH_REQUEST, _onSearch);
					subTokens[chan.OBJECT_FROM_XFID] = aperture.pubsub.subscribe(chan.OBJECT_FROM_XFID, _createClusterDetails);
					_UIObjectState.subscriberTokens = subTokens;
				},
				end : function(){
					for (var token in _UIObjectState.subscriberTokens) {
						if (_UIObjectState.subscriberTokens.hasOwnProperty(token)) {
							aperture.pubsub.unsubscribe(_UIObjectState.subscriberTokens[token]);
						}
					}
				}
			};
		});

		return xfEntityDetailsModule;
	}
);
