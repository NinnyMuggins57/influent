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
define(['jquery', 'lib/module', 'lib/channels', 'lib/util/duration', 'lib/util/xfUtil', 'lib/ui/xfModalDialog'],
    function($, modules, chan, duration, xfUtil, xfModalDialog) {

        //--------------------------------------------------------------------------------------------------------------
        // Private Variables
        //--------------------------------------------------------------------------------------------------------------

        var MODULE_NAME = 'xfHeader';

        var _UIObjectState = {
            UIType : MODULE_NAME,
            sandbox : undefined,
            inTzStart : new Date(),
            inTzEnd : new Date(2013,0,1), // TODO: fix
            showDetails : true,
            duration : '',
            datasetMax : '',
            datasetMin : '',
            rangepx : 1,
            addLeft : 1,
            subscriberTokens : null
        };

        var bucketsForDuration = (
            function() {
                var buckets = {
                    'P14D': 14, // days
                    'P30D': 15,
                    'P60D': 15,
                    'P112D': 16, // weeks
                    'P224D': 16,
                    'P1Y': 12, // months
                    'P16M': 16, // months
                    'P2Y': 12,
                    'P32M': 16,
                    'P4Y': 16, // quarters
                    'P8Y': 16,
                    'P16Y': 16 // years
                };

                return function(duration) {
                    return buckets[duration];
                };
            }
        )();

        function _utcDate(d) {
        	return new Date(Date.UTC(
    			d.getFullYear(), 
    			d.getMonth(),
    			d.getDate()
			));
        }
        
        //--------------------------------------------------------------------------------------------------------------
        // Private Methods
        //--------------------------------------------------------------------------------------------------------------

        var _initializeDetails = function() {

            var headerDiv = $('#header');

            var displayDiv = $('<div></div>');
            displayDiv.attr('id', 'display');
            headerDiv.append(displayDiv);

            var exportButton = $('<button></button>');
            exportButton.attr('id', 'export-button');
            exportButton.addClass('nocapture');
            exportButton.html('Workspace');
            displayDiv.append(exportButton);

            var exportOptions = $('<ul></ul>');
            exportOptions.attr('id', 'export-options');
            exportOptions.addClass('nocapture');
            displayDiv.append(exportOptions);

            var newWorkspaceOption = $('<li></li>');
            var newWorkspaceHref = $('<a></a>');
            newWorkspaceHref.attr('id', 'new-workspace');
            newWorkspaceHref.attr('href', '#');
            var newWorkspaceIcon = $('<span></span>');
            newWorkspaceIcon.addClass('ui-icon new-chart-icon');
            newWorkspaceHref.append(newWorkspaceIcon);
            newWorkspaceHref.append($(document.createTextNode('New Workspace')));
            newWorkspaceOption.append(newWorkspaceHref);
            exportOptions.append(newWorkspaceOption);

            var exportCaptureOption = $('<li></li>');
            var exportCaptureHref = $('<a></a>');
            exportCaptureHref.attr('id', 'export-capture');
            exportCaptureHref.attr('href', '#');
            exportCaptureHref.addClass('ui-state-disabled');
            var exportCaptureIcon = $('<span></span>');
            exportCaptureIcon.addClass('ui-icon export-capture-icon');
            exportCaptureHref.append(exportCaptureIcon);
            exportCaptureHref.append($(document.createTextNode('Export Image')));
            exportCaptureOption.append(exportCaptureHref);
            exportOptions.append(exportCaptureOption);

            var exportNotebookOption = $('<li></li>');
            var exportNotebookHref = $('<a></a>');
            exportNotebookHref.attr('id', 'export-notebook');
            exportNotebookHref.attr('href', '#');
            exportNotebookHref.addClass('ui-state-disabled');
            var exportNotebookIcon = $('<span></span>');
            exportNotebookIcon.addClass('ui-icon export-notebook-icon');
            exportNotebookHref.append(exportNotebookIcon);
            exportNotebookHref.append($(document.createTextNode('Export Chart (XML)')));
            exportNotebookOption.append(exportNotebookHref);
            exportOptions.append(exportNotebookOption);


            var USE_AUTH = aperture.config.get()['influent.config']['useAuth'] || false;
            if (USE_AUTH) {
                var logoutOption = $('<li></li>');
                var logoutHref = $('<a></a>');
                logoutHref.attr('id', 'logout');
                logoutHref.attr('href', '#');
                var logoutIcon = $('<span></span>');
                logoutIcon.addClass('ui-icon logout-icon');
                logoutHref.append(logoutIcon);
                logoutHref.append($(document.createTextNode('Logout')));
                logoutOption.append(logoutHref);
                exportOptions.append(logoutOption);

                logoutHref.click(
                    function (e) {
                        aperture.pubsub.publish(chan.LOGOUT_REQUEST);
                        e.preventDefault();
                    }
                );
            }

            var displayButton = $('<button></button>');
            displayButton.attr('id', 'display-button');
            displayButton.addClass('nocapture');
            displayButton.html('View');
            displayDiv.append(displayButton);

            var displayOptions = $('<ul></ul>');
            displayOptions.attr('id', 'display-options');
            displayOptions.addClass('nocapture');
            displayDiv.append(displayOptions);

            var entitiesOption = $('<li></li>');
            var entitiesHref = $('<a></a>');
            entitiesHref.attr('id', 'display-entities');
            entitiesHref.attr('href', '#');
            var entitiesIcon = $('<span></span>');
            entitiesIcon.addClass('ui-icon display-entity');
            entitiesHref.append(entitiesIcon);
            entitiesHref.append($(document.createTextNode('Account Holders')));
            entitiesOption.append(entitiesHref);
            displayOptions.append(entitiesOption);

            var chartsOption = $('<li></li>');
            var chartsHref = $('<a></a>');
            chartsHref.attr('id', 'display-charts');
            chartsHref.attr('href', '#');
            var chartsIcon = $('<span></span>');
            chartsIcon.addClass('ui-icon display-chart');
            chartsHref.append(chartsIcon);
            chartsHref.append($(document.createTextNode('Account Activity')));
            chartsOption.append(chartsHref);
            displayOptions.append(chartsOption);

            var filterContainerDiv = $('<div></div>');
            filterContainerDiv.attr('id', 'filter-container');
            headerDiv.append(filterContainerDiv);

            var filterDiv = $('<div></div>');
            filterDiv.attr('id', 'filter');
            filterContainerDiv.append(filterDiv);

            var titleSpan = $('<span></span>');
            titleSpan.addClass('title');
            titleSpan.html('Transaction Flow:');
            filterDiv.append(titleSpan);

            var intervalSelect = $('<select></select>');
            intervalSelect.attr('id', 'interval');
            filterDiv.append(intervalSelect);

            var datePickerFrom = $('<input/>');
            datePickerFrom.attr('type', 'text');
            datePickerFrom.attr('id', 'datepickerfrom');
            datePickerFrom.addClass('dateinput dateinputfrom');
            filterDiv.append(datePickerFrom);

            var datePickerLabel = $('<label></label>');
            datePickerLabel.attr('for', 'datepickerto');
            datePickerLabel.addClass('datelabel');
            datePickerLabel.html('to');
            filterDiv.append(datePickerLabel);

            var datePickerTo = $('<input/>');
            datePickerTo.attr('type', 'text');
            datePickerTo.attr('id', 'datepickerto');
            datePickerTo.addClass('dateinput dateinputto');
            filterDiv.append(datePickerTo);

            var flowImage = $('<img>');
            flowImage.attr('src', 'img/flow.png');
            flowImage.attr('id', 'flow-cue');
            flowImage.attr('alt', '');
            filterDiv.append(flowImage);

            var dateButton = $('<button></button>');
            dateButton.attr('id', 'applydates');
            dateButton.css('display', 'none');
            dateButton.text('Apply');
            filterDiv.append(dateButton);

            // create the display menu
            displayOptions.menu();

            displayButton.button().click(
                function() {
                	exportOptions.hide();
                	
                    var menu = displayOptions.show().position(
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

            chartsHref.click(
                function (e) {
                    e.preventDefault();

                    var showDetails = true;
                    aperture.pubsub.publish(chan.DETAILS_CHANGE_REQUEST, {showDetails : showDetails});
                }
            );

            entitiesHref.click(
                function (e) {
                    e.preventDefault();

                    var showDetails = false;
                    aperture.pubsub.publish(chan.DETAILS_CHANGE_REQUEST, {showDetails : showDetails});
                }
            );

            // create the display menu
            exportOptions.menu();

            exportButton.button({icons: {secondary: 'ui-icon-triangle-1-s'}}).click(
                function() {
                	displayOptions.hide();
                	
                    var menu = exportOptions.show().position(
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

            exportCaptureHref.click(
                function (e) {
                    e.preventDefault();
//                    aperture.pubsub.publish(chan.EXPORT_CAPTURED_IMAGE_REQUEST);
                }
            );

            exportNotebookHref.click(
                function (e) {
                    e.preventDefault();
//                    aperture.pubsub.publish(chan.EXPORT_GRAPH_REQUEST);
                }
            );
            
            newWorkspaceHref.click(
                function (e) {
                    aperture.pubsub.publish(chan.NEW_WORKSPACE_REQUEST);
                    e.preventDefault();
                }
            );
                
            _updateDetails(null, {showDetails: _UIObjectState.showDetails});
        };

        //--------------------------------------------------------------------------------------------------------------

        var sendFilterChangeRequest = function() {
            aperture.pubsub.publish(
                chan.FILTER_CHANGE_REQUEST,
                {
                    start: _utcDate(_UIObjectState.inTzStart),
                    end: _utcDate(_UIObjectState.inTzEnd),
                    duration: _UIObjectState.duration,
                    numBuckets: _UIObjectState.numBuckets
                }
            );
        };

        //--------------------------------------------------------------------------------------------------------------


        var _initializeFilter = function() {

            _UIObjectState.datasetMax = new Date().getTime(); // TODO get from server
            _UIObjectState.datasetMin = duration.subtractFromDate('P16Y', new Date(_UIObjectState.datasetMax)).getTime(); //TODO get from server

            var intervalSelect = $('#interval');
            var dateRangeIntervals = aperture.config.get()['influent.config'].dateRangeIntervals;
            for (var dateRangeKey in dateRangeIntervals) {
                if (dateRangeIntervals.hasOwnProperty(dateRangeKey)) {
                    intervalSelect.append('<option value="' + dateRangeKey + '">' + dateRangeIntervals[dateRangeKey] + '</option>');
                }
            }

            // Initialize the date dropdown from the config if it's present
            var initialDateKey = aperture.config.get()['influent.config']['startingDateRange'];
            if (initialDateKey) {
                _setIntervalSelector(initialDateKey);
            } else {
                intervalSelect.get(0).selectedIndex = 0;
            }



            $("#datepickerfrom").datepicker({
                changeMonth: true,
                changeYear: true,
                showAnim: 'fadeIn',
                duration: 'fast',
                onSelect: function() {
                    _UIObjectState.inTzStart = new Date($(this).val());
                    _UIObjectState.inTzEnd = duration.addToDate(_UIObjectState.duration, _UIObjectState.inTzStart);
                    // this is here to fix time zone switches - should this be a fix to duration?
                    _UIObjectState.inTzEnd.setMinutes(_UIObjectState.inTzEnd.getMinutes() 
                    		- _UIObjectState.inTzStart.getTimezoneOffset()
                    		+ _UIObjectState.inTzEnd.getTimezoneOffset());
                    $('#datepickerto').datepicker("setDate", xfUtil.dayBefore(_UIObjectState.inTzEnd));

                    _adjustRangeBar();
                    // unhide apply button
                    $('#applydates').css('display', '');
                },
                beforeShowDay: function(date) {
                    // validation fn for dates, return valid (bool), css classname
                    return [!_UIObjectState.duration || duration.isDateValidForDuration(date, _UIObjectState.duration), ''];
                }
            });

            $("#datepickerto").datepicker({
                changeMonth: true,
                changeYear: true,
                showAnim: 'fadeIn',
                duration: 'fast',
                onSelect: function() {
                    _UIObjectState.inTzEnd = xfUtil.dayAfter(new Date($(this).val()));
                    _UIObjectState.inTzStart = duration.subtractFromDate(_UIObjectState.duration, _UIObjectState.inTzEnd);
                    // this is here to fix time zone switches - should this be a fix to duration?
                    _UIObjectState.inTzStart.setMinutes(_UIObjectState.inTzStart.getMinutes()
                    		- _UIObjectState.inTzEnd.getTimezoneOffset()
                    		+ _UIObjectState.inTzStart.getTimezoneOffset());
                    $('#datepickerfrom').datepicker("setDate", _UIObjectState.inTzStart);

                    _adjustRangeBar();
                    // unhide apply button
                    $('#applydates').css('display', '');
                },
                beforeShowDay: function(date) {
                    // validation fn for dates, return valid (bool), css classname
                    return [!_UIObjectState.duration || duration.isDateValidForDuration(xfUtil.dayAfter(date), _UIObjectState.duration), ''];
                }
            });

            $('.dateinput').datepicker("option", "dateFormat", "M d, yy");

            var applyDates = $('#applydates');
            applyDates.button().click(function() {
                sendFilterChangeRequest();
                $('#applydates').css('display', 'none');
            });

            intervalSelect.change(function(e) {
                _UIObjectState.duration = $('#interval').val();
                _UIObjectState.inTzEnd = duration.roundDateByDuration(_UIObjectState.inTzEnd, _UIObjectState.duration);
                _UIObjectState.inTzStart = duration.subtractFromDate(_UIObjectState.duration, _UIObjectState.inTzEnd);
                // this is here to fix time zone switches - should this be a fix to duration?
                _UIObjectState.inTzStart.setMinutes(_UIObjectState.inTzStart.getMinutes() 
                		- _UIObjectState.inTzEnd.getTimezoneOffset()
                		+ _UIObjectState.inTzStart.getTimezoneOffset());

                $('#datepickerto').datepicker("setDate", xfUtil.dayBefore(_UIObjectState.inTzEnd));
                $('#datepickerfrom').datepicker("setDate", _UIObjectState.inTzStart);
                _UIObjectState.numBuckets = bucketsForDuration(_UIObjectState.duration);

                _adjustRangeBar();
                // unhide apply button
                $('#applydates').css('display', '');
            });

            applyDates.css('display', 'none');
        };

        //--------------------------------------------------------------------------------------------------------------

        var _adjustRangeBar = function () {
            var filterDiv = $('#filter');

            // need lazy initialization of the date range bar to prevent layout race conditions

            if(filterDiv.find('#daterangeback').length == 0) {
                var backDiv = $('<div></div>');
                backDiv.attr('id', 'daterangeback');
                backDiv.addClass('dateRangeBackground');
                filterDiv.append(backDiv);

                _UIObjectState.rangepx = $('#daterangeback').width();
            }

            if(filterDiv.find('#daterangefront').length == 0) {
                var frontDiv = $('<div></div>');
                frontDiv.attr('id', 'daterangefront');
                frontDiv.addClass('dateRangeForeground');
                filterDiv.append(frontDiv);

                _UIObjectState.addLeft = $('#daterangefront').position().left;
            }

            /*
             * We map X in [A,B] --> Y in [C,D] then subtract to get width
             * Y = (X-A)/(B-A)*(D-C), (B-A) != 0
             * Then add div offset
             */

            if (_UIObjectState.datasetMax - _UIObjectState.datasetMin > 0) {
                var offsetLeft = (_UIObjectState.inTzStart.getTime()-_UIObjectState.datasetMin)/(_UIObjectState.datasetMax-_UIObjectState.datasetMin)*_UIObjectState.rangepx;
                var offsetRight = (_UIObjectState.inTzEnd.getTime()-_UIObjectState.datasetMin)/(_UIObjectState.datasetMax-_UIObjectState.datasetMin)*_UIObjectState.rangepx;
                var barWidth = offsetRight-offsetLeft;

                offsetLeft += _UIObjectState.addLeft;
                $('#daterangefront').animate({left: offsetLeft, width: barWidth}, 300);
            }
        };

        //--------------------------------------------------------------------------------------------------------------

        var _setIntervalSelector = function(dateKey) {
            var intervalSelect = $('#interval');

            var options = intervalSelect.children();
            for (var i = 0; i < options.length; i++) {
                if (options[i].value == dateKey) {
                    intervalSelect.get(0).selectedIndex = i;
                    break;
                }
            }
            if (i == options.length) {
                intervalSelect.get(0).selectedIndex = 0;
            }
        };

        //--------------------------------------------------------------------------------------------------------------

        var _updateDetails = function(chan, data) {
        	_UIObjectState.showDetails = data.showDetails;
        	
            $('#display-button').button(
                'option',
                'icons',
                {
                    primary: data.showDetails ? 'display-chart' : 'display-entity',
                    secondary: 'ui-icon-triangle-1-s'
                }
            );
        };

        //--------------------------------------------------------------------------------------------------------------

        var _updateFilter = function (chan, data) {

            _UIObjectState.inTzStart = xfUtil.displayShiftedDate(data.startDate);
            _UIObjectState.inTzEnd = xfUtil.displayShiftedDate(data.endDate);
            _UIObjectState.duration = data.duration;
            _UIObjectState.numBuckets = bucketsForDuration(data.duration);

            $('#datepickerfrom').datepicker("setDate", _UIObjectState.inTzStart);
            $('#datepickerto').datepicker("setDate", xfUtil.dayBefore(_UIObjectState.inTzEnd));
            _setIntervalSelector(_UIObjectState.duration);
            _adjustRangeBar();
        };

        //--------------------------------------------------------------------------------------------------------------

        var _initializeModule = function(chan, data) {
            _initializeDetails();
            _initializeFilter();
        };

        //--------------------------------------------------------------------------------------------------------------
        // Public
        //--------------------------------------------------------------------------------------------------------------

        var xfHeaderModule = {};

        //--------------------------------------------------------------------------------------------------------------

        xfHeaderModule.start = function(sandbox) {

            _UIObjectState.sandbox = sandbox;

            var subTokens = {};
            subTokens[chan.DETAILS_CHANGE_EVENT] = aperture.pubsub.subscribe(chan.DETAILS_CHANGE_EVENT, _updateDetails);
            subTokens[chan.FILTER_CHANGE_EVENT] = aperture.pubsub.subscribe(chan.FILTER_CHANGE_EVENT, _updateFilter);
            subTokens[chan.ALL_MODULES_STARTED] = aperture.pubsub.subscribe(chan.ALL_MODULES_STARTED, _initializeModule);
            _UIObjectState.subscriberTokens = subTokens;
        };

        //--------------------------------------------------------------------------------------------------------------

        xfHeaderModule.end = function(){
            for (var token in _UIObjectState.subscriberTokens) {
                if (_UIObjectState.subscriberTokens.hasOwnProperty(token)) {
                    aperture.pubsub.unsubscribe(_UIObjectState.subscriberTokens[token]);
                }
            }
        };

        //--------------------------------------------------------------------------------------------------------------

        var headerConstructor = function(sandbox){
            return {
                start : function(sandbox){
                    xfHeaderModule.start(sandbox);
                },
                end : function(){
                    xfHeaderModule.end();
                }
            };
        };

        // Register the module with the system
        modules.register(MODULE_NAME, headerConstructor);

        return xfHeaderModule;
    }
);