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
	['modules/xfWorkspace', 'lib/module', 'lib/channels', 'lib/constants', 'lib/plugins'],
	function(xfWorkspace, modules, chan, constants, plugins) {

		var transactionsConstructor = function(sandbox) {

			var _transactionsState = {
				capture : sandbox.spec.capture,
				subscriberTokens : null,
				userSelectedTab: 'table',
				autoSelecting: false
			};

			//----------------------------------------------------------------------------------------------------------

			var _onUpdate = function(channel, data) {

				var footerContentDiv = $('#footer-content');
				var transDiv = $('#transactions');
				var tableIndex = $('#tableTab').index() - 1;
				var chartIndex = $('#chartTab').index() - 1;
				var tableDiv = $('#transactions-table');

				if (data) {

					if (aperture.config.get()['influent.config'].promptForDetailsInFooter) {

						// Check if overlay already exists
						footerContentDiv.find('.footer-overlay').remove();

						var visualInfo = xfWorkspace.getUIObjectByXfId(data.xfId).getVisualInfo();
						var shouldPrompt = visualInfo.spec.promptForDetails;
						if (shouldPrompt) {

							// Add overlay
							var overlay = $('<div></div>');
							overlay.addClass('footer-overlay');
							footerContentDiv.append(overlay);

							// Add prompt
							var prompt = $('<div></div>');
							prompt.addClass('footer-textbox');

							var html = 'Press OK to view details';

							// Override with plugin
							var extensions = plugins.get('details');
							var plugin = aperture.util.find(extensions, function(e) {
								return e.prompt !== undefined;
							});

							if (plugin) {
								html = plugin.prompt({
									dataId: data.dataId,
									type: data.accountType,
									label: data.label,
									numMembers: data.count
								});
							}

							prompt.html(html);

							prompt.append('<br><br>');
							overlay.append(prompt);

							// Add OK Button
							var button = $('<button></button>');
							button.text('OK');
							prompt.append(button);
							button.click({container: footerContentDiv},
								function (e) {

									// Remove overlay
									e.data.container
										.find('.footer-overlay')
										.fadeOut(150, function () {
											this.remove();
										});

									e.stopImmediatePropagation();

									visualInfo.spec.promptForDetails = false;
									aperture.pubsub.publish(chan.SELECTION_CHANGE_EVENT, data);
								}
							);
						}
					}

					tableDiv.show();
					if (data.uiType === constants.MODULE_NAMES.ENTITY) {

						if (_.size(transDiv.tabs('option', 'disabled')) > 0 &&
							_.contains(transDiv.tabs('option', 'disabled'), tableIndex)
						) {
							transDiv.tabs(
								'enable',
								tableIndex
							);
						}

						if (_transactionsState.userSelectedTab === 'table' &&
							transDiv.tabs('option', 'selected') === chartIndex
						) {
							_transactionsState.autoSelecting = true;
							transDiv.tabs(
								'select',
								tableIndex
							);
						}
					} else {
						if (_transactionsState.userSelectedTab === 'table' &&
							transDiv.tabs('option', 'selected') === tableIndex
						) {
							_transactionsState.autoSelecting = true;
							transDiv.tabs(
								'select',
								chartIndex
							);
						}
						transDiv.tabs(
							'disable',
							tableIndex
						);
						tableDiv.hide();
					}
				}
			};

			//----------------------------------------------------------------------------------------------------------
			var FOOTER_HEIGHT = 251;
			var HEADER_HEIGHT = 55;
			var ANIM_DURATION = 200;
			//----------------------------------------------------------------------------------------------------------
			var isExpanded = false;
			//----------------------------------------------------------------------------------------------------------

			function _onStateRequest(channel, data) {

				// EXPAND
				if (isExpanded !== data.expand) {
					isExpanded = data.expand;

					if (isExpanded && !_transactionsState.capture) {
						$('#footer-content').animate({
							height: FOOTER_HEIGHT

						}, ANIM_DURATION);

						// If there is a selected object and it is about to be hidden by the footer, then scroll
						// the workspace to accomodate
						var workspace = $('#workspace');
						var workspaceTop = workspace.scrollTop();
						var selectedObject = $('#' + data.selection.getXfId());
						if (selectedObject.length !== 0) {
							var objectPos = selectedObject.offset().top + selectedObject.height();
							var footerPos = (workspace.height() + HEADER_HEIGHT) - FOOTER_HEIGHT;

							if (objectPos > footerPos) {
								workspaceTop = workspace.scrollTop() + FOOTER_HEIGHT;
							}
						}

						workspace.animate({
							'bottom': FOOTER_HEIGHT,
							'scrollTop' : workspaceTop
						}, ANIM_DURATION);

					// COLLAPSE
					} else {
						$('#footer-content').css('height', 0);
						$('#workspace').css('bottom', 0);
					}
				}
			}

			//----------------------------------------------------------------------------------------------------------

			var initialize = function() {

				var footer = $('#footer');

				var footerContentDiv = $('<div></div>');
				footerContentDiv.attr('id', 'footer-content');
				footerContentDiv.css('height', 0);
				footerContentDiv.css('position', 'relative');
				footerContentDiv.css('overflow', 'hidden');
				footer.append(footerContentDiv);

				var detailsDiv = $('<div></div>');
				detailsDiv.attr('id', 'details');
				footerContentDiv.append(detailsDiv);

				var detailsContentDiv = $('<div></div>');
				detailsContentDiv.attr('id', 'details-content');
				detailsDiv.append(detailsContentDiv);

				var transactionsDiv = $('<div></div>');
				transactionsDiv.attr('id', 'transactions');
				footerContentDiv.append(transactionsDiv);

				var tabs = $('<ul></ul>');
				tabs.addClass('tabs');

				var tableTab = $('<li></li>');
				var tableHref = $('<a></a>');
				tableHref.attr('href', '#tableTab');
				tableHref.html('Transaction Table');
				tableTab.append(tableHref);
				tabs.append(tableTab);

				var chartTab = $('<li></li>');
				var chartHref = $('<a></a>');
				chartHref.attr('href', '#chartTab');
				chartHref.html('Transaction Chart');
				chartTab.append(chartHref);
				tabs.append(chartTab);

				transactionsDiv.append(tabs);

				var tableTabDiv = $('<div></div>');
				tableTabDiv.attr('id', 'tableTab');
				transactionsDiv.append(tableTabDiv);

				var filterHighlightedInput = $('<input>');
				filterHighlightedInput.attr('id', 'filterHighlighted');
				filterHighlightedInput.attr('type', 'checkbox');
				tableTabDiv.append(filterHighlightedInput);

				var filterHighlightedLabelDiv = $('<div></div>');
				filterHighlightedLabelDiv.attr('id', 'filterHighlightedLabel');
				filterHighlightedLabelDiv.html('<span id="filterHighlightedHighlight">Highlighted</span> Only');
				tableTabDiv.append(filterHighlightedLabelDiv);

				var exportTransactionsButton = $('<button></button>');
				exportTransactionsButton.attr('id', 'exportTransactions');
				exportTransactionsButton.text('Export');
				tableTabDiv.append(exportTransactionsButton);

				var transactionsTable = $('<table></table>');
				transactionsTable.attr('id', 'transactions-table');
				transactionsTable.addClass('display dataTable');
				transactionsTable.css('width', '100%');
				tableTabDiv.append(transactionsTable);

				var transactionsTableHead = $('<thead></thead>');
				transactionsTable.append(transactionsTableHead);

				var transactionsTableColumns = $('<tr></tr>');
				transactionsTableColumns.addClass('tHeader');
				transactionsTableColumns.html('<td>#</td><th>Date</th><th>Comment</th><th>Inflowing</th><th>Outflowing</th>');
				transactionsTableHead.append(transactionsTableColumns);

				var transactionsTableBody = $('<tbody></tbody>');
				transactionsTable.append(transactionsTableBody);

				var chartTabDiv = $('<div></div>');
				chartTabDiv.attr('id', 'chartTab');
				transactionsDiv.append(chartTabDiv);

				$(function() {
					$('#transactions').tabs(
						{
							select: function(event, ui) {

								if (ui.panel.id === 'chartTab') {
									aperture.pubsub.publish(chan.REQUEST_CURRENT_STATE);
								}

								if (_transactionsState.autoSelecting) {
									_transactionsState.autoSelecting = false;
								} else {
									_transactionsState.userSelectedTab = (ui.panel.id === 'chartTab') ?
										_transactionsState.userSelectedTab = 'chart' :
										_transactionsState.userSelectedTab = 'table';
								}
							}
						}
					);
				});
			};

			//----------------------------------------------------------------------------------------------------------

			var _initializeModule = function(eventChannel) {

				if (eventChannel !== chan.ALL_MODULES_STARTED) {
					return;
				}

				aperture.log.debug('_initializeModule xfFooter');
				initialize();
			};

			//----------------------------------------------------------------------------------------------------------

			var _start = function() {
				var subTokens = {};

				subTokens[chan.SELECTION_CHANGE_EVENT] = aperture.pubsub.subscribe(chan.SELECTION_CHANGE_EVENT, _onUpdate);
				subTokens[chan.ALL_MODULES_STARTED] = aperture.pubsub.subscribe(chan.ALL_MODULES_STARTED, _initializeModule);
				subTokens[chan.FOOTER_STATE_REQUEST] = aperture.pubsub.subscribe(chan.FOOTER_STATE_REQUEST, _onStateRequest);

				_transactionsState.subscriberTokens = subTokens;
			};

			//--------------------------------------------------------------------------------------------------------------

			var _end = function() {
				for (var token in _transactionsState.subscriberTokens) {
					if (_transactionsState.subscriberTokens.hasOwnProperty(token)) {
						aperture.pubsub.unsubscribe(_transactionsState.subscriberTokens[token]);
					}
				}
			};

			//--------------------------------------------------------------------------------------------------------------

			return {
				start : _start,
				end : _end
			};
		};

		modules.register('xfFooter', transactionsConstructor);
	}
);
