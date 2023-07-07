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
		'lib/channels', 'lib/util/xfUtil', 'lib/render/cardRenderer', 'lib/render/fileRenderer',
		'lib/render/clusterRenderer', 'lib/ui/xfModalDialog', 'lib/constants'
	],
	function(
		chan, xfUtil, cardRenderer, fileRenderer,
		clusterRenderer, xfModalDialog, constants
	) {
		var columnRenderer = {};

		var _cardDefaults = cardRenderer.getRenderDefaults();
		var _renderDefaults = {
			COLUMN_DISTANCE : 300 // 300px between columns
		};
		var _processChildren = function(childObjects, parentCanvas){
			for (var i=0; i < childObjects.length; i++){
				var visualInfo = childObjects[i].getVisualInfo();
				var element = null;
				switch(childObjects[i].getUIType()){
					case constants.MODULE_NAMES.FILE : {
						element = fileRenderer.createElement(visualInfo);
						break;
					}
					case constants.MODULE_NAMES.IMMUTABLE_CLUSTER :
					case constants.MODULE_NAMES.MUTABLE_CLUSTER :
					case constants.MODULE_NAMES.SUMMARY_CLUSTER : {
						element = clusterRenderer.createElement(visualInfo);
						element.css('left', _cardDefaults.CARD_LEFT);
						break;
					}
					case constants.MODULE_NAMES.ENTITY : {
						element = cardRenderer.createElement(visualInfo);
						element.css('left', _cardDefaults.CARD_LEFT);
						break;
					}
					default : {
						aperture.log.error('Attempted to add an unsupported UIObject type to column: ' + childObjects[i].getUIType());
					}
				}
				if (element){
					parentCanvas.append(element);
				}
			}
		};

		columnRenderer.createElement = function(visualInfo){
			var canvas = $('#' + visualInfo.xfId),
				container;

			function showHeader() {
				if ($('.columnHeader', canvas).css('display') === 'none') {

					// hide all, just in case we didn't get a leave event
					$('.columnHeader').css('display', 'none');

					// show just me
					$('.columnHeader', canvas).css('display', '');
				}
			}

			// exists? empty it
			if (canvas.length > 0) {
				container = $('.columnContainer', canvas);
				container.empty();
			// else construct it
			} else {
				canvas = $('<div class="column"></div>');
				canvas.attr('id', visualInfo.xfId);

				// create the column header
				var header = $('<div class="columnHeader"></div>').appendTo(canvas).css('display', 'none');

				// create the content container
				container = $('<div class="columnContainer"></div>').appendTo(canvas);

				var fileBtn = xfUtil.makeButton('add new file to top of column', 'new-file', null, 'column-button', null).appendTo(header);
				fileBtn.click(function() {
					aperture.pubsub.publish(chan.CREATE_FILE_REQUEST, {xfId : visualInfo.xfId, isColumn: true});
					return false;
				});

				var cleanColumnBtn = xfUtil.makeButton('clear column of unfiled content', 'column-clear', null, 'column-button', null).appendTo(header);
				cleanColumnBtn.click(function() {
					xfModalDialog.createInstance({
						title: 'Clear Column?',
						contents: 'Clear all unfiled cards? Everything but file folders and match results will be removed.',
						buttons: {
							Clear : function() {
								aperture.pubsub.publish(
									chan.CLEAN_COLUMN_REQUEST,
									{
										xfId : visualInfo.xfId
									}
								);
							},
							Cancel : function() {}
						}
					});
					return false;
				});

				var sortColumnBtn = xfUtil.makeButton('sort column by visible flow', 'column-sort', null, 'column-button', null).appendTo(header);

				var sortOptions = $('<ul></ul>');
				sortOptions.attr('id', 'sort-options');
				sortOptions.addClass('nocapture');
				sortOptions.css('z-index', 1000);
				header.append(sortOptions);

				var inSortOption = $('<li></li>');
				var inSort = $('<a></a>');
				inSort.attr('id', 'incoming-descending');
				inSort.attr('href', '#');
				inSort.html('Incoming Flow');
				inSortOption.append(inSort);
				sortOptions.append(inSortOption);

				var outSortOption = $('<li></li>');
				var outSort = $('<a></a>');
				outSort.attr('id', 'outgoing-descending');
				outSort.attr('href', '#');
				outSort.html('Outgoing Flow');
				outSortOption.append(outSort);
				sortOptions.append(outSortOption);

				var bothSortOption = $('<li></li>');
				var bothSort = $('<a></a>');
				bothSort.attr('id', 'both-descending');
				bothSort.attr('href', '#');
				bothSort.html('Both');
				bothSortOption.append(bothSort);
				sortOptions.append(bothSortOption);

				sortColumnBtn.click(
					function() {

						sortOptions.hide();

						var menu = sortOptions.show().position(
							{
								my: 'right top',
								at: 'right bottom',
								of: this
							}
						);

						$(document).one(
							'click',
							function() {
								menu.hide();
							}
						);

						return false;
					}
				);

				sortOptions.menu();

				inSort.click(
					function (e) {
						e.preventDefault();

						aperture.pubsub.publish(
							chan.SORT_COLUMN_REQUEST,
							{
								xfId : visualInfo.xfId,
								sortDescription : constants.SORT_FUNCTION.INCOMING,
								sortFunction : xfUtil.incomingDescendingSort

							}
						);
					}
				);

				outSort.click(
					function (e) {
						e.preventDefault();

						aperture.pubsub.publish(
							chan.SORT_COLUMN_REQUEST,
							{
								xfId : visualInfo.xfId,
								sortDescription : constants.SORT_FUNCTION.OUTGOING,
								sortFunction : xfUtil.outgoingDescendingSort
							}
						);
					}
				);

				bothSort.click(
					function (e) {
						e.preventDefault();

						aperture.pubsub.publish(
							chan.SORT_COLUMN_REQUEST,
							{
								xfId : visualInfo.xfId,
								sortDescription : constants.SORT_FUNCTION.BOTH,
								sortFunction : xfUtil.bothDescendingSort
							}
						);
					}
				);

				// show when hovering or when clicked.
				canvas.click(showHeader);
				canvas.mousemove(showHeader);
				canvas.mouseleave(function() {
					$('.columnHeader', canvas).css('display', 'none');
				});
			}

			_processChildren(visualInfo.children, container);

			return canvas;
		};
		
		columnRenderer.getHeight = function(element) {
			var cc = element.children('.columnContainer');

			return cc.position().top + cc.height();
		};
		
		columnRenderer.getRenderDefaults = function(){
			return _.clone(_renderDefaults);
		};
		
		return columnRenderer;
	}
);
