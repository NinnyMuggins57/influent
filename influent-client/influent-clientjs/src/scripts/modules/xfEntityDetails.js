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
define(['modules/xfWorkspace', 'modules/xfRest', 'lib/module', 'lib/channels',
		'lib/constants', 'lib/util/xfUtil', 'lib/plugins'],

	function(xfWorkspace, xfRest, modules, chan,
			 constants, xfUtil, plugins) {

		//--------------------------------------------------------------------------------------------------------------
		// Private Variables
		//--------------------------------------------------------------------------------------------------------------
		var MONTHS = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];
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

		var _stripHtml = function(textIn) {
			var tmp = document.createElement('div');
			tmp.innerHTML = textIn;
			return tmp.textContent || tmp.innerText || '';
		};

		//--------------------------------------------------------------------------------------------------------------

		var _removeSpecialCharacters = function(str) {
			return _stripHtml(str).replace(/(\\r\\n|\\n|\\r|\\t|\r\n|\r|\n|\t|\s+)/gm, ' ');
		};

		//--------------------------------------------------------------------------------------------------------------

		var _onSelect = function(channel, data) {

			if (channel !== chan.SELECTION_CHANGE_EVENT) {
				return;
			}

			// Only retrieve details for xfCluster and xfCard objects.
			var canvas = $('#details');
			canvas.empty();

			if (data != null) {

				if (data.xfId === _UIObjectState.lastRequestedObject) {
					return;
				}

				_UIObjectState.lastRequestedObject = data.xfId;

				if (data.uiType === constants.MODULE_NAMES.ENTITY)
				{
					// get details for entity here
					xfRest.request('/entitydetails')
						.withData({entityId : data.dataId})
						.inContext( data.xfId)
						.then( function (response) {

							_createEntityDetails(data.xfId, response);

						});

				} else if (data.hasOwnProperty('ownerId') && data.ownerId !== '') {

					// get details for entity here
					xfRest.request('/entitydetails')
						.withData({entityId : data.ownerId})
						.inContext( data.xfId)
						.then( function (response) {

							_createEntityDetails(data.xfId, response);
							_createClusterSummaryDetails(data.xfId);
						});


				} else {
					_createClusterDetails(data.xfId);

				}
			} else {
				_UIObjectState.lastRequestedObject = null;
			}
		};

		//--------------------------------------------------------------------------------------------------------------

		var _createEntityDetails = function(xfId, response) {
			// cache icon set
			var iconList = xfWorkspace.getUIObjectByXfId(xfId).getVisualInfo().spec['icons'];

			var canvas = $('#details');

			var header = $('<div class="detailsHeader"></div>').appendTo(canvas);
			var headerInfo = $('<div id="detailsHeaderInfo"><span class="detailsTitle">Account Details</span>').appendTo(header);
			var entityLabel = $('<div class="detailsEntityLabel"></div>').appendTo(headerInfo);

			// Label
			var label = xfWorkspace.getValueByTag(response, 'LABEL');
			entityLabel.append('<b>' + label + '</b>');

			// Icons
			var iconContainer = $('<div class="detailsIconContainer"></div>').appendTo(headerInfo);
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

					var image = $('<img class= "detailsIcon">').appendTo(iconContainer);
					image.attr('src', url);
					xfUtil.makeTooltip(image, icon.title);
				}
			}

			// Image
			_addImage(response);

			// Property table
			var insertPropertyTableRow = function(property) {

				var row = _buildPropertyTableRow(property);

				if (row) {
					row.appendTo(tbody);
				}
			};

			var body = $('<div id="detailsBody"></div>').appendTo(canvas);

			var table = $('<table class="propertyTable" style="bottom: 2px; left: 0px;"></table>').appendTo(body);
			var tbody = $('<tbody></table>').appendTo(table);

			insertPropertyTableRow({friendlyText: 'uid', value: response.uid});
			var propertyArray = xfUtil.propertyMapToDisplayOrderArray(response);
			for (i = 0; i < propertyArray.length; i++) {
				insertPropertyTableRow(propertyArray[i]);
			}
		};

		//--------------------------------------------------------------------------------------------------------------

		var _createClusterDetails = function(xfId) {

			var obj = xfWorkspace.getUIObjectByXfId(xfId);

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

			var detailsBody = $('<div id="detailsBody"></div>').appendTo(div);

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

			var spec = obj.getVisualInfo().spec;

			var detailsBody = $('#detailsBody');

			var detailsHeader = $('<div class="detailsHeader"></div>').appendTo(detailsBody);
			var label = spec.label;

			detailsHeader.append('<span class="detailsTitle">Owner Account Summary<span><br>');
			detailsHeader.append('<span class="detailsEntityLabel">'+ label+ '<span>');

			var iconClassOrder = aperture.config.get()['influent.config'].iconOrder;
			var iconClassMap = aperture.config.get()['influent.config'].iconMap;

			for(var i = 0; i < iconClassOrder.length; i++) {
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
						xfUtil.makeTooltip(image, icon.title);

						var imageDiv = $('<div class="detailsValueIcon"></div>').appendTo(valueBlock);
						imageDiv.append(image);

						// value
						valueBlock.append('<div class="detailsValueValue">' + icon.title + '</div>');

						// score bar
						var scoreBarContainer = $('<div class="detailsValueScoreBar"></div>');

						var score = ''+ Math.round(icon.score * 100) + '%';
						xfUtil.makeTooltip(scoreBarContainer, score, 'entity details score bar');

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

		//--------------------------------------------------------------------------------------------------------------

		var _buildPropertyTableRow = function(property) {

			if (aperture.util.hasAny(property.tags, ['IMAGE']) ||
				!property.friendlyText) {
				return;
			}

			var friendlyText = property.friendlyText;
			var value = property.value;

			if (aperture.util.has(property.tags, 'DATE')) {
				var date = new Date(parseInt(property.value, 10));
				value = MONTHS[date.getMonth()] + ' ' + date.getDay() + ', ' + date.getFullYear();
			}

			value = _removeSpecialCharacters(value);

			var row = $('<tr></tr>');
			var nameCell = $('<td></td>').appendTo(row);
			nameCell.addClass('propertyName');
			nameCell.html(friendlyText + ':');

			var valueCell = $('<td></td>').appendTo(row);
			valueCell.addClass('propertyValue');
			valueCell.html(value);

			var extensions = plugins.get('details');
			var clickPlugin = aperture.util.find(extensions, function(e) {
				return e.propertyClick !== undefined;
			});
			if (clickPlugin) {
				var onClick = clickPlugin.propertyClick(property);
				if (onClick) {
					valueCell.click(function(e) { onClick(e); });
					var image = $('<img>').appendTo(valueCell);
					image.attr('src', 'img/info_icon.png');
					image.css('margin-left', '5px');
				}
			}

			var hoverPlugin = aperture.util.find(extensions, function(e) {
				return e.propertyHover !== undefined;
			});
			var onHover = null;
			if (hoverPlugin) {
				onHover = hoverPlugin.propertyHover(property);
			}

			if (onHover) {
				if (onHover.fnIn)
					valueCell.mouseover(function(e) { onHover.fnIn(e); });

				if (onHover.fnOut)
					valueCell.mouseout(function(e) { onHover.fnOut(event); });
			} else {

				// Add tooltip to show uncertainty and provenance
				var uncertainty = property.uncertainty;
				var provenance = property.provenance;
				var tooltipString = '';

				if (uncertainty) {
					tooltipString += 'Uncertainty: ' + uncertainty + '\n';
				}
				if (provenance) {
					tooltipString += 'Provenance: ' + provenance + '\n';
				}
				$(this).css('title', tooltipString);
			}

			return row;
		};

//--------------------------------------------------------------------------------------------------------------

		var _addImage = function(response) {
			var header = $('.detailsHeader');
			var image = $('<div id="detailsHeaderImage"></div>').appendTo(header);

			var imageUrls = [];
			var imageProps = xfWorkspace.getPropertiesByTag(response, 'IMAGE');
			for (var j = 0; j < imageProps.length; j++) {
				if (imageProps[j].range && imageProps[j].range.values) {
					imageUrls = imageUrls.concat(imageProps[j].range.values);
				} else {
					imageUrls.push(imageProps[j].value);
				}
			}

			var imageContainer = $('<span class="imageContainer"></span>').appendTo(image);
			var imageLink = $('<a></a>').appendTo(imageContainer);
			imageLink.attr('target', '_blank');
			imageLink.click( function (e) {
				aperture.pubsub.publish(chan.OPEN_EXTERNAL_LINK_REQUEST, {
					link: $(this).attr('href')
				});
				e.preventDefault();
			});

			var img = $('<img>').appendTo(imageLink);
			img.attr('style', 'image-rendering:optimizeQuality;');
			img.load(function() {
				$(this).show();
			});
			var imageIdx = 0;



			// Image controls ------------------------------------------------------------------------------------------

			var imageControls = $('<div class="imageControls"></div>').appendTo(image);
			if (imageUrls.length <= 1) {
				imageControls.hide();
			}

			var setDisabled = function(state) {
				if (state) {
					this.attr('disabled', 'disabled');
					this.addClass('ui-state-disabled');
				} else {
					this.removeAttr('disabled');
					this.removeClass('ui-state-disabled');
				}
			};

			var updateImageIndex = function(idx) {

				if (idx >= 0 && idx < imageUrls.length) {
					prevButton.setDisabled(idx === 0);
					nextButton.setDisabled(idx === imageUrls.length - 1);

					imageLink.attr('href', imageUrls[idx]);
					img.attr('src', imageUrls[idx]);
					img.hide();
					photoControlLabel.text((imageIdx + 1) + ' of ' + imageUrls.length);
				}
			};


			// Previous button;
			var prevButton = $('<button></button>').appendTo(imageControls);
			prevButton.button({
				icons: {
					primary: 'ui-icon-carat-1-w'
				},
				text:false
			});
			prevButton.addClass('prevImageButton');
			prevButton.click(function() {

				updateImageIndex(--imageIdx);
			});
			prevButton.setDisabled = setDisabled;
			xfUtil.makeTooltip(prevButton, 'Previous image');


			// Label;
			var photoControlLabel = $('<div class="imageControlsLabel"></div>').appendTo(imageControls);

			// Next button;
			var nextButton = $('<button></button>').appendTo(imageControls);
			nextButton.button({
				icons: {
					primary: 'ui-icon-carat-1-e'
				},
				text:false
			});
			nextButton.addClass('nextImageButton');
			nextButton.click(function() {
				updateImageIndex(++imageIdx);
			});
			nextButton.setDisabled = setDisabled;
			xfUtil.makeTooltip(nextButton, 'Next image');

			updateImageIndex(imageIdx);
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
