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

 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.

 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

define(
    ['jquery', 'lib/module', 'lib/channels', 'lib/util/xfUtil', 'lib/util/currency', 'lib/util/duration', 'lib/constants'],
    function($, modules, chan, xfUtil, currency, duration, constants) {
	
        var transactionGraphConstructor = function(sandbox) {

            var MULTIPLE = 5;

            var _state = {
                sessionId: null,
                filterDates: null,
                focusData: null,
                selectedEntity: null,
                selectedColumn: null,
                subscriberTokens: null
            };

            var MONTH_NAMES = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
            var SCREEN_TYPES = {};
            SCREEN_TYPES[constants.MODULE_NAMES.ENTITY] = 'Account';
            SCREEN_TYPES[constants.MODULE_NAMES.MUTABLE_CLUSTER] = 'File';
            SCREEN_TYPES[constants.MODULE_NAMES.CLUSTER_BASE] = 'Cluster';
            SCREEN_TYPES[constants.MODULE_NAMES.IMMUTABLE_CLUSTER] = 'Cluster';
            SCREEN_TYPES[constants.MODULE_NAMES.SUMMARY_CLUSTER] = 'Cluster';
            SCREEN_TYPES[constants.MODULE_NAMES.FILE] = 'File';

            var CHART_COLOR = 'rgb(128, 128, 128)';
            var BASELINE_COLOR = 'rgb(107, 89, 53)';
            var BASE_CREDIT_COLOR = 'rgb(107, 89, 53)';
            var BASE_DEBIT_COLOR = 'rgb(107, 89, 53)';
            var FOCUS_CREDIT_COLOR = 'rgb(246, 140, 13)';
            var FOCUS_DEBIT_COLOR = 'rgb(176, 97, 0)';
            
            var _numberFormatter = aperture.Format.getNumberFormat(0.01);
            var _ajaxSpinner = 'url("img/ajax-loader.gif") no-repeat center center';

            //----------------------------------------------------------------------------------------------------------

            var _createXAxis = function(plot, order){
                var axisLayer = plot.xAxis(order);
                axisLayer.map('margin').asValue(40);
                axisLayer.map('visible').asValue(true);
                axisLayer.map('text-anchor').asValue('start');
                axisLayer.map('text-anchor-y').asValue('bottom');
                axisLayer.map('tick-length').asValue(10);
                axisLayer.map('label-offset-y').asValue(4);
                axisLayer.map('label-offset-x').asValue(2);
                axisLayer.map('font-size').asValue(10);
                return axisLayer;
            };

            //----------------------------------------------------------------------------------------------------------

            var _createTooltipHtml = function(event, isInflowing){

                var index = event.index[0];
                var baseValue = event.data.data[index].base;
                var focusValue = event.data.data[index].focus;
                var startDate = event.data.data[index].startDate;
                var endDate = xfUtil.dayBefore(event.data.data[index].endDate);
                var focusType = SCREEN_TYPES[_state.focusData.entityType];
                var focusLabel = _state.focusData.entityLabel;
                var focusCount = _state.focusData.entityCount;
                var focusDataId = _state.focusData.entityType === constants.MODULE_NAMES.ENTITY? _state.focusData.dataId.substr(2): '';
                var selectionXfId = _state.selectedEntity.xfId;
                var focusXfId = _state.focusData.xfId;
                var label = _state.selectedEntity.label;
                var count = _state.selectedEntity.count;

                // HACK: append cluster count to selected entity label
                if (count && count > 1 &&
                    _state.selectedEntity.entityType == constants.MODULE_NAMES.MUTABLE_CLUSTER) {

                    // Some clusters may already have a count appended
                    if (!(label.indexOf("(+")!==-1 && label.charAt(label.length - 1) == ")")) {
                        label += " (+" + (count - 1) + ")";
                    }
                }

                var dataId = _state.selectedEntity.entityType === constants.MODULE_NAMES.ENTITY? _state.selectedEntity.dataId.substr(2): '';
                var entityType = SCREEN_TYPES[_state.selectedEntity.entityType];

                if (!isInflowing) {
                    baseValue *= -1;
                    focusValue *= -1;
                }

                var html = '<div class="influent-tooltip"><B>';
                html += MONTH_NAMES[startDate.getMonth()] + ' ' + startDate.getDate() + ', ' + startDate.getFullYear() + ' ';
                html += '</B>';
                if (startDate.getTime() !== endDate.getTime()) {
	                html += 'to<B> ';
	                html += MONTH_NAMES[endDate.getMonth()] + ' ' + endDate.getDate() + ', ' + endDate.getFullYear() + ' ';
	                html += '</B>';
                }
                html += '<br><br><span class="tooltip-selected-entity">';
                html += entityType + ': ';
                if (dataId) {
                    html += dataId + ', ';
                }
                html += label;
                html += '</span><br>';

                if (isInflowing) {
                    html += 'Total Inflowing: <B>';
                } else {
                    html += 'Total Outflowing: <B>';
                }
                html += _numberFormatter.format(baseValue);
                html += '</B>';
                
                if (focusValue != 0 && selectionXfId != focusXfId) {

                    // HACK: append cluster count to focus label
                    if (focusCount && focusCount > 1 &&
                        _state.focusData.entityType == constants.MODULE_NAMES.MUTABLE_CLUSTER) {

                        // Some clusters may already have a count appended
                        if (!(focusLabel.indexOf("(+")!==-1 && focusLabel.charAt(focusLabel.length - 1) == ")")) {
                            focusLabel += " (+" + (focusCount - 1) + ")";
                        }
                    }

                    html += '<br><br>';
                    html += '<span class="tooltip-focus-entity">Subset Involving ';
                    html += focusType + ': ';
                    if (focusDataId) {
                        html += focusDataId + ', ';
                    }
                    html += focusLabel + '</span><br>';

                    if (isInflowing) {
                        html += 'Total Inflowing: <B>';
                    } else {
                        html += 'Total Outflowing: <B>';
                    }
                    html += _numberFormatter.format(focusValue);
                    html += '</B>';
                }
                html += '</div>';
                
                return html;
            };

            //----------------------------------------------------------------------------------------------------------

            var _createBarChart = function(graphParams){

                var graphDiv = $('#chartTab');
                graphDiv.empty();
                graphDiv.css('background', 'none');

                var SPACER = 1;
                var BAR_WIDTH = 4 * MULTIPLE;

                var width = graphDiv.width();
                var height = graphDiv.height();

                if (!graphParams ||
                    graphParams == 'blank' ||
                    width == 0 ||
                    height == 0
                ) {
                    return;
                }

                var rangeX = new aperture.TimeScalar('date');
                var rangeY = new aperture.Scalar('value');
                rangeY = rangeY.symmetric();
                
                var units = graphParams.units;

                if (graphParams.credits.length != graphParams.debits.length) {
                    return;
                }

                var dataSources = {
                    'series' : [[
                        {
                            'type': 'baseCredits',
                            'data' : []
                        }],
                        [{
                            'type': 'baseDebits',
                            'data' : []
                        }],
                        [{
                            'type': 'focusCredits',
                            'data' : []
                        }],
                        [{
                            'type': 'focusDebits',
                            'data' : []
                        }]
                    ]
                };

                var maxAbsoluteFocusValueCredit = Number.NEGATIVE_INFINITY;

                for (var i = 0; i < graphParams.dates.length; i++) {
                    var baseCredit = graphParams.credits[i];
                    var baseDebit = graphParams.debits[i];
                    var focusCredit = graphParams.focusCredits[i];
                    var focusDebit = graphParams.focusDebits[i];
                    var startDate = new Date(graphParams.dates[i]);
                    var endDate = new Date(
                        ((i + 1) == graphParams.dates.length) ? startDate.getTime() + (startDate.getTime() - graphParams.dates[i-1]) 
                        		: graphParams.dates[i + 1]
                    );
                    
                    // now shift it for time zone. times come in utc so if we don't shift them they may display a different date in local
                    startDate = xfUtil.displayShiftedDate(startDate);
                    endDate = xfUtil.displayShiftedDate(endDate);
                    
                    var midDate = new Date(startDate.getTime() + (endDate.getTime() - startDate.getTime())/2);
                    
                    rangeX.expand(startDate);
                    rangeX.expand(endDate);

                    rangeY.expand(baseCredit);
                    rangeY.expand(baseDebit);
                    rangeY.expand(focusCredit);
                    rangeY.expand(focusDebit);

                    if (Math.abs(focusCredit) > maxAbsoluteFocusValueCredit) {
                        maxAbsoluteFocusValueCredit = Math.abs(focusCredit);
                    }
                    if (Math.abs(focusDebit) > maxAbsoluteFocusValueCredit) {
                        maxAbsoluteFocusValueCredit = Math.abs(focusDebit);
                    }

                    dataSources.series[0][0].data.push(
                        {
                            'bucket' : i,
                            'startDate' : startDate,
                            'midDate' : midDate,
                            'endDate' : endDate,
                            'base' : baseCredit,
                            'focus' : focusCredit
                        }
                    );

                    dataSources.series[1][0].data.push(
                        {
                            'bucket' : i,
                            'startDate' : startDate,
                            'midDate' : midDate,
                            'endDate' : endDate,
                            'base' : -baseDebit,
                            'focus' : -focusDebit
                        }
                    );

                    dataSources.series[2][0].data.push(
                        {
                            'bucket' : i,
                            'startDate' : startDate,
                            'midDate' : midDate,
                            'endDate' : endDate,
                            'base' : baseCredit,
                            'focus' : focusCredit
                        }
                    );

                    dataSources.series[3][0].data.push(
                        {
                            'bucket' : i,
                            'startDate' : startDate,
                            'midDate' : midDate,
                            'endDate' : endDate,
                            'base' : -baseDebit,
                            'focus' : -focusDebit
                        }
                    );
                }

                rangeY.expand(0);
                rangeY.expand(-1);
                rangeY.expand(1);
                var chart = new aperture.chart.Chart(graphDiv[0]);
                chart.all(dataSources);
                chart.map('width').asValue(width);
                chart.map('height').asValue(height);
                chart.map('font-family').asValue('Verdana');
                
                var bandedX = rangeX.banded(16, false);
                var mapKeyX = bandedX.mapKey([0,1]);
                
                var primeXAxis = _createXAxis(chart, 0);
                primeXAxis.all(mapKeyX);
                primeXAxis.map('rule-width').asValue(1);

                var nextOrder = bandedX.formatter().nextOrder();
                if (nextOrder) {
	                var secBandedX = bandedX.banded({
	                    span : 1,
	                    units : nextOrder
	                });
	                
	                var secXAxis = _createXAxis(chart,1);
	                secXAxis.all(secBandedX.mapKey([0,1]));
	                secXAxis.map('rule-width').asValue(0);
	                secXAxis.map('tick-offset').asValue(10);
	                secXAxis.map('tick-first').asValue('edge');
	                secXAxis.map('font-outline').asValue('#ffffff');	                
                }
                
                chart.map('x').using(mapKeyX);

                rangeY.formatter(aperture.Format.getCurrencyFormat(1));
                chart.map('y').using(rangeY.banded(4).mapKey([1,0]));
                
                // Hide the chart border.
                chart.map('border-width').asValue(0);
                chart.map('background-color').asValue('none');

                // Configure and define the main title of the chart.
                chart.map('stroke').asValue(CHART_COLOR);
                chart.map('opacity').asValue(1.0);

                // common to all bars
                chart.map('spacer').asValue(SPACER);
                chart.map('point-count').from('data.length');

                chart.yAxis().mapAll({
                    'title' : 'Amount (' + units + ')',
                    'margin' : 60,
                    'tick-length' : 6,
                    'label-offset-x' : 2,
                    'rule-width': 1,
                    'font-size': 11
                });

                var types = ['baseCredits', 'baseDebits', 'focusCredits', 'focusDebits'];
                var fillKey = new aperture.Ordinal(
                    'fill', types
                ).mapKey(
                        [BASE_CREDIT_COLOR, BASE_DEBIT_COLOR, FOCUS_CREDIT_COLOR, FOCUS_DEBIT_COLOR]
                    );
                var opacityKey = new aperture.Ordinal(
                    'opacity', types
                ).mapKey(
                        [0.3, 0.5, 1.0, 1.0]
                    );

                var selection = new aperture.Set('data[]');

                var ruleLayer = chart.ruleLayer(0);
                ruleLayer.map('rule').asValue(0.5);
                ruleLayer.map('axis').asValue('y');
                ruleLayer.map('stroke-width').asValue('1');
                ruleLayer.map('stroke-style').asValue('solid');
                ruleLayer.map('fill').asValue(BASELINE_COLOR);
                ruleLayer.map('opacity').asValue(1.0);
                ruleLayer.toFront();

                // base credit series
                var baseCreditSeries = chart.addLayer(aperture.chart.BarSeriesLayer);
                baseCreditSeries.all(
                    function(data){
                        return data.series[0];
                    }
                );
                baseCreditSeries.map('width').asValue(BAR_WIDTH);
                baseCreditSeries.map('x').from('data[].midDate');
                baseCreditSeries.map('y').from('data[].base');
                baseCreditSeries.map('stroke').asValue('none');
                baseCreditSeries.map('fill').from('type').using(fillKey).filter(
                    selection.constant(
                        aperture.palette.color('selected')
                    )
                );
                baseCreditSeries.map('opacity').from('type').using(opacityKey);
                baseCreditSeries.on(
                    'mouseover',
                    function(event){
                        var html = _createTooltipHtml(event, true);
                        aperture.tooltip.showTooltip({event:event, html:html});
                        return true;
                    }
                );
                baseCreditSeries.on('mouseout', function(event){
                    aperture.tooltip.hideTooltip();

                    // handled.
                    return true;
                });

                // base debit series
                var baseDebitSeries = chart.addLayer(aperture.chart.BarSeriesLayer);
                baseDebitSeries.all(
                    function(data){
                        return data.series[1];
                    }
                );
                baseDebitSeries.map('width').asValue(BAR_WIDTH);
                baseDebitSeries.map('x').from('data[].midDate');
                baseDebitSeries.map('y').from('data[].base');
                baseDebitSeries.map('stroke').asValue('none');
                baseDebitSeries.map('fill').from('type').using(fillKey).filter(
                    selection.constant(
                        aperture.palette.color('selected')
                    )
                );
                baseDebitSeries.map('opacity').from('type').using(opacityKey);
                baseDebitSeries.on('mouseover', function(event){
                    var html = _createTooltipHtml(event, false);
                    aperture.tooltip.showTooltip({event:event, html:html});
                    return true;
                });
                baseDebitSeries.on('mouseout', function(event){
                    aperture.tooltip.hideTooltip();

                    // handled.
                    return true;
                });

                // focus credit series
                var focusCreditSeries = chart.addLayer(aperture.chart.BarSeriesLayer);
                focusCreditSeries.all(
                    function(data){
                        return data.series[2];
                    }
                );
                focusCreditSeries.map('width').asValue(BAR_WIDTH);
                focusCreditSeries.map('x').from('data[].midDate');
                focusCreditSeries.map('y').from('data[].focus');
                focusCreditSeries.map('stroke').asValue('none');
                focusCreditSeries.map('fill').from('type').using(fillKey).filter(
                    selection.constant(
                        aperture.palette.color('selected')
                    )
                );
                focusCreditSeries.map('opacity').from('type').using(opacityKey);
                focusCreditSeries.on('mouseover', function(event){
                    var html = _createTooltipHtml(event, true);
                    aperture.tooltip.showTooltip({event:event, html:html});
                    return true;
                });
                focusCreditSeries.on('mouseout', function(event){
                    aperture.tooltip.hideTooltip();

                    // handled.
                    return true;
                });

                // focus debit series
                var focusDebitSeries = chart.addLayer(aperture.chart.BarSeriesLayer);
                focusDebitSeries.all(
                    function(data){
                        return data.series[3];
                    }
                );
                focusDebitSeries.map('width').asValue(BAR_WIDTH);
                focusDebitSeries.map('x').from('data[].midDate');
                focusDebitSeries.map('y').from('data[].focus');
                focusDebitSeries.map('stroke').asValue('none');
                focusDebitSeries.map('fill').from('type').using(fillKey).filter(
                    selection.constant(
                        aperture.palette.color('selected')
                    )
                );
                focusDebitSeries.map('opacity').from('type').using(opacityKey);
                focusDebitSeries.on('mouseover', function(event){
                    var html = _createTooltipHtml(event, false);
                    aperture.tooltip.showTooltip({event:event, html:html});
                    return true;
                });
                focusDebitSeries.on('mouseout', function(event){
                    aperture.tooltip.hideTooltip();

                    // handled.
                    return true;
                });

                chart.all().redraw();
            };

            //----------------------------------------------------------------------------------------------------------

            var _updateGraph = function(state) {

                _getMaxDebitCredit(
                    state,
                    function(focusMDC) {
                        _getGraphData(state, focusMDC);
                    }
                );
            };

            //----------------------------------------------------------------------------------------------------------

            function _getMaxDebitCredit(state, onReturn) {

                if (state.focusData == null) {
                    aperture.log.warn('no focus account in chart request. bailing out of request!');
                    return;
                }

                var focusId = state.focusData.dataId;
                var focusEntity = {
                    contextId : state.focusData.contextId,
                    dataId : focusId
                };
                
                aperture.io.rest(
                    '/bigchart',
                    'POST',
                    function(response) {
                        // pull MDC out of response
                        var maxCreditDebit = 0;
                        
                        if (response[focusId]) {
                            var credit = Math.abs(new Number(response[focusId].maxCredit));
                            var debit = Math.abs(new Number(response[focusId].maxDebit));

                            if (credit > maxCreditDebit) {
                                maxCreditDebit = credit;
                            }
                            if (debit > maxCreditDebit) {
                                maxCreditDebit = debit;
                            }
                        }

                        onReturn(maxCreditDebit);
                    },
                    {
                        postData: {
                            sessionId : state.sessionId,
                            entities : [focusEntity],
                            startDate : state.filterDates.startDate,
                            endDate :  state.filterDates.endDate,
                            focusId : [focusId],
                            focusMaxDebitCredit : "",
                            focuscontextid : state.focusData.contextId
                        },
                        contentType: 'application/json'
                    }
                );
            }

            //----------------------------------------------------------------------------------------------------------

            var _getGraphData = function(state, focusMDC) {
                aperture.io.rest(
                    '/bigchart',
                    'POST',
                    function(response) {
                        _createBarChart(response[state.selectedEntity.dataId]);
                    },
                    {
                        postData: {
                            sessionId : state.sessionId,
                            entities : [state.selectedEntity],
                            startDate : state.filterDates.startDate,
                            endDate :  state.filterDates.endDate,
                            focusId : [state.focusData.dataId],
                            focusMaxDebitCredit : focusMDC.toString(),
                            focuscontextid : state.focusData.contextId
                        },
                        contentType: 'application/json'
                    }
                );
            };

            //----------------------------------------------------------------------------------------------------------

            var _requestState = function() {
                aperture.pubsub.publish(
                    chan.REQUEST_CURRENT_STATE,
                    {}
                );
            };

            //----------------------------------------------------------------------------------------------------------

            var _onUpdate = function(eventChannel, data) {

                if (eventChannel != chan.CURRENT_STATE) {
                    return;
                }

                var graphDiv = $('#chartTab');
                graphDiv.empty();
                graphDiv.css('background', 'none');

                if (data == null) {
                    aperture.log.warn('current state data is null');
                    return;
                }

                _state.sessionId = data.sessionId;
                _state.focusData = (data.focusData == null) ? null :_.clone(data.focusData);
                _state.filterDates = (data.dates == null) ? null :_.clone(data.dates);
                _state.selectedEntity = (data.selectedEntity == null) ? null : _.clone(data.selectedEntity);

                if (!_.isNull(_state.selectedEntity)) {
                    graphDiv.empty();
                    graphDiv.css('background', _ajaxSpinner);
                    _updateGraph(_.clone(_state));
                }
            };

            //----------------------------------------------------------------------------------------------------------

            var _initializeBigGraph = function() {

            };

            //----------------------------------------------------------------------------------------------------------

            var _initializeModule = function() {
                aperture.log.debug('_initializeModule xfTransactionGraph');
                _initializeBigGraph();
            };

            //----------------------------------------------------------------------------------------------------------

            var start = function() {
                var subTokens = {};

                subTokens[chan.SELECTION_CHANGE_EVENT] = aperture.pubsub.subscribe(chan.SELECTION_CHANGE_EVENT, _requestState);
                subTokens[chan.FOCUS_CHANGE_EVENT] = aperture.pubsub.subscribe(chan.FOCUS_CHANGE_EVENT, _requestState);
                subTokens[chan.SEARCH_REQUEST] = aperture.pubsub.subscribe(chan.SEARCH_REQUEST, _requestState);
                subTokens[chan.FILTER_CHANGE_EVENT] = aperture.pubsub.subscribe(chan.FILTER_CHANGE_EVENT, _requestState);
                subTokens[chan.CURRENT_STATE] = aperture.pubsub.subscribe(chan.CURRENT_STATE, _onUpdate);
                subTokens[chan.ALL_MODULES_STARTED] = aperture.pubsub.subscribe(chan.ALL_MODULES_STARTED, _initializeModule);

                _state.subscriberTokens = subTokens;
            };

            //----------------------------------------------------------------------------------------------------------

            var end = function() {
                for (var token in _state.subscriberTokens) {
                    if (_state.subscriberTokens.hasOwnProperty(token)) {
                        aperture.pubsub.unsubscribe(_state.subscriberTokens[token]);
                    }
                }
            };

            //----------------------------------------------------------------------------------------------------------

            return {
                start : start,
                end : end
            };
        };

        modules.register('xfTransactionGraph', transactionGraphConstructor);
    }
);