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
    ['jquery', 'lib/module', 'lib/channels', 'lib/constants'],
    function($, modules, chan, constants) {
	
        var transactionsConstructor = function(sandbox) {

            var _transactionsState = {
                subscriberTokens : null,
                userSelectedTab: 'table',
                autoSelecting: false
            };

            //----------------------------------------------------------------------------------------------------------

            var _onUpdate = function(channel, data) {

                var transDiv = $('#transactions');
                var tableIndex = $("#tableTab").index() - 1;
                var chartIndex = $("#chartTab").index() - 1;

                if (data) {
                    if (data.uiType == constants.MODULE_NAMES.ENTITY) {

                        if (_.size(transDiv.tabs('option', 'disabled')) > 0 &&
                            _.contains(transDiv.tabs('option', 'disabled'), tableIndex)
                        ) {
                            transDiv.tabs(
                                'enable',
                                tableIndex
                            );
                        }

                        if (_transactionsState.userSelectedTab == 'table' &&
                            transDiv.tabs('option', 'selected') == chartIndex
                        ) {
                            _transactionsState.autoSelecting = true;
                            transDiv.tabs(
                                'select',
                                tableIndex
                            );
                        }
                    } else {
                        if (_transactionsState.userSelectedTab == 'table' &&
                            transDiv.tabs('option', 'selected') == tableIndex
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
                    }
                }
            };

            //----------------------------------------------------------------------------------------------------------

            var onFooterExpand = function(expand, height) {
                aperture.pubsub.publish(chan.FOOTER_STATE, {
                    expanded : expand,
                    height : height
                });
            };

            //----------------------------------------------------------------------------------------------------------

            var initialize = function() {

                var footer = $('#footer');



                var footerTitle = $('<h3></h3>');
                footerTitle.addClass('footer-title');
                footerTitle.html('Details');
                footer.append(footerTitle);

                var footerContentDiv = $('<div></div>');
                footerContentDiv.attr('id', 'footer-content');
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
                tableHref.html('Table');
                tableTab.append(tableHref);
                tabs.append(tableTab);

                var chartTab = $('<li></li>');
                var chartHref = $('<a></a>');
                chartHref.attr('href', '#chartTab');
                chartHref.html('Chart');
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
                    $('#footer').accordion({
                        collapsible: true,
                        heightStyle: 'auto',
                        change: function (event, ui) {
                            onFooterExpand(
                                (ui.newContent ? true : false),
                                footer.height()
                            );
                        }
                    });
                });

                $(function() {
                    $('#transactions').tabs(
                        {
                            select: function(event, ui) {

                                if (ui.panel.id == 'chartTab') {
                                    aperture.pubsub.publish(chan.REQUEST_CURRENT_STATE);
                                }

                                if (_transactionsState.autoSelecting) {
                                    _transactionsState.autoSelecting = false;
                                } else {
                                    _transactionsState.userSelectedTab = (ui.panel.id == 'chartTab') ?
                                        _transactionsState.userSelectedTab = 'chart' :
                                        _transactionsState.userSelectedTab = 'table';
                                }
                            }
                        }
                    );
                });

                $(function() {
                    onFooterExpand(true, footer.height());
                });
            };

            //----------------------------------------------------------------------------------------------------------

            var _initializeModule = function(eventChannel) {

                if (eventChannel != chan.ALL_MODULES_STARTED) {
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