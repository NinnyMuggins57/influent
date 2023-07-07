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
define(['jquery', 'lib/module', 'lib/channels', 'lib/extern/jquery.dataTables'], function($, modules, chan) {
	
	var transactionsConstructor = function(sandbox) {
		
		var _transactionsState = {
			table: '',
			curEntity : '',
			focus : null,
			startDate : null,
			endDate : null,
            subscriberTokens : null,
            bFilterFocused : false
		};

        var _transactionsTableInitialized = false;

        //--------------------------------------------------------------------------------------------------------------

        var processRowData = function(nRow, aData, iDisplayIndex, iDisplayIndexFull){
            var srcId = aData[5];
            var dstId = aData[6];
            var focusId = _transactionsState.focus.dataId;
            
            if (srcId == focusId || dstId == focusId){
                $(nRow).addClass('transactionsHighlight-'+ (iDisplayIndex % 2));
            }
        };

        //--------------------------------------------------------------------------------------------------------------

        var onSearch = function(channel, data) {
        	aperture.log.debug('onSearch transactions : clear transactions table');
            if (_transactionsTableInitialized) {
                $('#transactions-table tbody').empty();
            }
        };

        //--------------------------------------------------------------------------------------------------------------

        var onUpdate = function(channel, data) {
            if (data != null) {
			    aperture.log.debug('onUpdate transactions : ' + data.dataId);
            }

			_transactionsState.curEntity = data && data.dataId;

            if (!_transactionsTableInitialized) {
            	if (_transactionsState.curEntity && _transactionsState.startDate && _transactionsState.endDate) {
                    initializeTransactionsTable();
            	} else {
            		return;
            	}
            }

			_transactionsState.table.fnDraw();
		};

        //--------------------------------------------------------------------------------------------------------------

        var onFocusChange = function(channel, data) {
			_transactionsState.focus = data;

			if (_transactionsState.table) {
                if(_transactionsState.bFilterFocused) {
                    _transactionsState.table.fnDraw();
                }
                else {
                    // avoid using fnDraw where possible, as it will reset the page/scroll location on the table view
                    $('tr', _transactionsState.table).each(function(i, e){
                        var rowData = _transactionsState.table.fnGetData( this );
                        if(rowData != null && (rowData[5] == data.dataId || rowData[6] == data.dataId)) {
                            $(e).addClass('transactionsHighlight-'+ ((i+1) % 2));
                        }
                        else {
                            $(e).removeClass('transactionsHighlight-'+ ((i+1) % 2));
                        }
                    });
                }
			}
		};

        //--------------------------------------------------------------------------------------------------------------

        var _onFilter = function(channel, data) {
			_transactionsState.startDate = data.startDate.getUTCFullYear()+'-'+(data.startDate.getUTCMonth()+1)+'-'+data.startDate.getUTCDate();
			_transactionsState.endDate = data.endDate.getUTCFullYear()+'-'+(data.endDate.getUTCMonth()+1)+'-'+data.endDate.getUTCDate();
			
			aperture.log.debug('onFilter transactions : '+_transactionsState.startDate+" -> "+_transactionsState.endDate);
			
			if (!_transactionsTableInitialized) {
            	if (_transactionsState.curEntity && _transactionsState.startDate && _transactionsState.endDate) {
                    initializeTransactionsTable();
            	} else {
            		return;
            	}
			}
			
			_transactionsState.table.fnDraw();
		};

        //--------------------------------------------------------------------------------------------------------------

        var initializeTransactionsTable = function() {

            var ajaxSource = aperture.io.restUrl('/transactions');

        	aperture.log.debug('initializeTransactionsTable called');
            _transactionsState.table = $('#transactions-table');
            _transactionsState.table.dataTable({
                'bProcessing':true,
                'bServerSide':true,
                'bDeferRender':true,
                'iDisplayLength':100,
                'bLengthChange':false,
                'sScrollY' : '151px',
                'bScrollCollapse' : true,
                'bFilter':false,
                'bDestroy':true,
                'sAjaxSource':ajaxSource,
                'fnServerParams':function(aoData) {
                    aoData.push({'name':'entityId', 'value':_transactionsState.curEntity});
                    aoData.push({'name':'startDate', 'value':_transactionsState.startDate});
                    aoData.push({'name':'endDate', 'value':_transactionsState.endDate});
                    if (_transactionsState.bFilterFocused && (_transactionsState.curEntity != _transactionsState.focus.dataId)) {                    	
                		//if (!_transactionsState.focus.isCluster) {
                			aoData.push({'name':'focusIds', 'value':_transactionsState.focus.dataId});			//TODO:  Send list of comma separated entities in clusters
                		//}
                    }
                },
				'aaSorting': [],				//No sorting initially - results are in order from server
				'aoColumnDefs': [
				    // TODO: # column needs to be sortable but it doesn't actually exist yet.
				    {'bSortable':false, 'aTargets':[0,2]},		//No sorting on # column or comment column
				    {'sWidth': '40px', 'sName': '#', 'aTargets':[0]},				//Column name 0
				    {'sWidth': '80px', 'sName': 'Date', 'aTargets':[1]},			//Column name 1
				    {'sName': 'Comment', 'aTargets':[2]},		//Column name 2
				    {'sClass': 'currency-table-column', 'sWidth': '100px', 'sName': 'Inflowing', 'aTargets':[3]},		//Column name 3
				    {'sClass': 'currency-table-column', 'sWidth': '100px', 'sName': 'Outflowing', 'aTargets':[4]}		//Column name 4
                    //Column 5: Reserved for source entity ID
                    //Column 6: Reserved for target entity ID
				],
				'fnServerData': function ( sSource, aoData, fnCallback, oSettings ) {
				      oSettings.jqXHR = $.ajax( {
				        "dataType": 'json',
				        "type": "GET",
				        "url": sSource,
				        "data": aoData,
				        "success": function ( data ) {	
				        	if (data.aoColumnUnits) {
				        		if (data.aoColumnUnits[2].sUnits) {
				        			oSettings.aoColumns[3].sTitle = 'In (' + data.aoColumnUnits[2].sUnits + ')';
				        			oSettings.aoColumns[3].nTh.innerText = 'In (' + data.aoColumnUnits[2].sUnits + ')';
				        		}
				        		if (data.aoColumnUnits[3].sUnits) {
				        			oSettings.aoColumns[4].sTitle = 'Out (' + data.aoColumnUnits[3].sUnits + ')';
				        			oSettings.aoColumns[4].nTh.innerText = 'Out (' + data.aoColumnUnits[3].sUnits + ')';
				        		}
				        	}
				        	fnCallback(data);
				        }
				      } );
				},
                'fnRowCallback' : processRowData
            });

            $('#exportTransactions').click(
                function() {
                    aperture.pubsub.publish(chan.EXPORT_TRANSACTIONS);
                }
            );
            
            $('#filterHighlighted').click(
	        	function() {
	                _transactionsState.bFilterFocused = $('#filterHighlighted')[0].checked;
	                _transactionsState.table.fnDraw();
	        	}
            );

            _transactionsTableInitialized = true;
        };

        //--------------------------------------------------------------------------------------------------------------

        var _onExportTransactions = function() {
            var numRows = _transactionsState.table.fnGetData().length;
            if(numRows == 0) {
                alert('No transaction data to export!');
                return;
            }

            var a = document.createElement('a');
            a.href = aperture.io.restUrl(
                '/exporttransactions?entityId=' +
                _transactionsState.curEntity +
                '&startDate=' +
                _transactionsState.startDate +
                '&endDate=' +
                _transactionsState.endDate
            );
            a.download = 'transactions_' + _transactionsState.curEntity + '.csv';
            document.body.appendChild(a);
            setTimeout(
                function() {
                    a.click();
                    document.body.removeChild(a);
                },
                0
            );
        };

        //--------------------------------------------------------------------------------------------------------------

        var _initializeModule = function() {
        	aperture.log.debug('_initializeModule transactions');
        	if (_transactionsState.curEntity && _transactionsState.startDate && _transactionsState.endDate) {
            	initializeTransactionsTable();
            }
        };

        //--------------------------------------------------------------------------------------------------------------

        var start = function() {
            var subTokens = {};
			
			//TODO : use the div passed in to place the transactions, instead of hardcoding the div name above.
			subTokens[chan.SELECTION_CHANGE_EVENT] = aperture.pubsub.subscribe(chan.SELECTION_CHANGE_EVENT, onUpdate);
            subTokens[chan.FOCUS_CHANGE_EVENT] = aperture.pubsub.subscribe(chan.FOCUS_CHANGE_EVENT, onFocusChange);
            subTokens[chan.SEARCH_REQUEST] = aperture.pubsub.subscribe(chan.SEARCH_REQUEST, onSearch);
            subTokens[chan.FILTER_CHANGE_EVENT] = aperture.pubsub.subscribe(chan.FILTER_CHANGE_EVENT, _onFilter);
            subTokens[chan.ALL_MODULES_STARTED] = aperture.pubsub.subscribe(chan.ALL_MODULES_STARTED, _initializeModule);
            subTokens[chan.EXPORT_TRANSACTIONS] = aperture.pubsub.subscribe(chan.EXPORT_TRANSACTIONS, _onExportTransactions);

            _transactionsState.subscriberTokens = subTokens;
		};

        //--------------------------------------------------------------------------------------------------------------

        var end = function() {
			for (var token in _transactionsState.subscriberTokens) {
                if (_transactionsState.subscriberTokens.hasOwnProperty(token)) {
                    aperture.pubsub.unsubscribe(_transactionsState.subscriberTokens[token]);
                }
            }
		};

        //--------------------------------------------------------------------------------------------------------------

        return {
			start : start,
			end : end
		};
	};
		
	modules.register('xfTransactionTable',transactionsConstructor);
});