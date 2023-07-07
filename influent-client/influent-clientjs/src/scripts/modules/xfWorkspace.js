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
        'jquery', 'lib/interfaces/xfUIObject', 'lib/channels', 'lib/util/GUID', 'lib/models/xfColumn', 'lib/models/xfFile',
        'lib/models/xfImmutableCluster', 'lib/models/xfMutableCluster', 'lib/models/xfSummaryCluster', 'lib/models/xfClusterBase',
        'lib/models/xfCard', 'lib/models/xfLink', 'lib/layout/xfLayoutProvider', 'lib/util/xfUtil',
        'lib/util/duration', 'lib/ui/toolbarOperations', 'lib/extern/ActivityLogger', 'lib/ui/xfLinkType',
        'modules/xfRenderer', 'lib/ui/xfModalDialog', 'lib/constants', 'lib/extern/cookieUtil'
    ],
    function(
        $, xfUIObject, chan, guid, xfColumn, xfFile,
        xfImmutableCluster, xfMutableCluster, xfSummaryCluster, xfClusterBase,
        xfCard, xfLink, xfLayoutProvider, xfUtil,
        duration, toolbarOp, activityLogger, xfLinkType,
        xfRenderer, xfModalDialog, constants, cookieUtil
    ) {

        //--------------------------------------------------------------------------------------------------------------
        // Private Variables
        //--------------------------------------------------------------------------------------------------------------

        var MODULE_NAME = 'xfWorkspace';

        var MAX_SEARCH_RESULTS = aperture.config.get()['influent.config']['maxSearchResults'];
        var MAX_PATTERN_SEARCH_RESULTS = aperture.config.get()['influent.config']['searchResultsPerPage'];
        var SEARCH_GROUP_BY = aperture.config.get()['influent.config']['searchGroupBy'];
        var USE_PATTERN_SEARCH = aperture.config.get()['influent.config']['usePatternSearch'];

        var _UIObjectState = {
            xfId : '',
            UIType : MODULE_NAME,
            sessionId : '',
            children : [], // This should only ever contain xfColumn objects.
            childModule : undefined,
            focus : undefined,
            showDetails : true,
            footerDisplay : 'block',
            dates : {startDate: '', endDate: '', numBuckets: 0, duration: ''},
            singleton : undefined,
            selectedUIObject : null,                 // this assumes one selected object, could make this an [] if needed
            subscriberTokens : null
        };

        var logging = aperture.config.get()['influent.config'].activityLogging;
        
        var _loggerState = {
            logger : null,
            enableLogging : !!(logging && logging.address && logging.enabled),
            type : {
                WF_SEARCH : 'search',
                WF_MARSHAL : 'marshal',
                WF_EXAMINE : 'examine'
            }
        };

        var xfWorkspaceSpecTemplate = {};

        var xfWorkspaceModule = {};

        //--------------------------------------------------------------------------------------------------------------
        // DRAPER Instrumentation
        //--------------------------------------------------------------------------------------------------------------

        var _getLogger = function(){
            // If the Draper logger is available, return it.
            if (_loggerState.enableLogging) {
            	if (_loggerState.provider == 'draper' && activityLogger) {
	                var logger = new activityLogger();
	                if (logger){
	                    logger.registerActivityLogger(logging.address, 'Initializing Draper instrumentation', '3.04',
	                    		_UIObjectState.sessionId);
	                    for (var key in _loggerState.type){
	                        if (_loggerState.type.hasOwnProperty(key)) {
	                            _loggerState.type[key] = logger[key];
	                        }
	                    }
	
	                    return logger;
	                }
            	} 
        		var apertureActivityLogger = {
        			logUserActivity : function(description, eventChannel, type, metadata) {
        				aperture.log.info({
                            sessionId: _UIObjectState.sessionId,
        					activityType: 'user',
        					activity: {
            					description: description,
            					eventChannel: eventChannel,
            					type: type,
            					metadata: metadata
        					}
        				});
        			},
        			logSystemActivity : function(description) {
        				aperture.log.info({
                            sessionId: _UIObjectState.sessionId,
        					activityType: 'system',
        					activity: {
            					description: description
        					}
        				});
        			}
        		};
        		
        		apertureActivityLogger.logSystemActivity('Initializing aperture instrumentation...');
        		
        		return apertureActivityLogger;
            }
            return null;
        };

        //--------------------------------------------------------------------------------------------------------------

        var _log = function(activityType, eventChannel, description, type, metadata){
            if (!_loggerState.enableLogging){
                return;
            }
            
            if (!_loggerState.logger) {
            	_loggerState.logger = _getLogger();
            }            
            if (activityType == 'user'){
                _loggerState.logger.logUserActivity(description, eventChannel, type, metadata);
            }
            else if (activityType == 'system'){
                _loggerState.logger.logSystemActivity(description || eventChannel);
            }
        };

        //--------------------------------------------------------------------------------------------------------------
        // Private Methods
        //--------------------------------------------------------------------------------------------------------------

        var _initializeModule = function(eventChannel) {

            if (eventChannel != chan.ALL_MODULES_STARTED) {
                return;
            }

            $(window).bind( 'beforeunload',
                function() {
                    var cookieId = aperture.config.get()['aperture.io'].restEndpoint.replace('%host%', 'sessionId');
	                var cookie = cookieUtil.readCookie(cookieId);
	                
                    _saveState(null, false);
                    
	                if (cookie) {
	                	var prompt = 'Influent can remember this session and attempt to resume it when you return. '+
	                    	'However, a previous session was found. If you wish to resume the previous session '+
	                    	'you must open a new browser tab for it before leaving this one.';
	                	
	                	return prompt;
	                }
                }
            );

            $(window).bind( 'unload',
                function() {
                    var cookieId = aperture.config.get()['aperture.io'].restEndpoint.replace('%host%', 'sessionId');
                    var cookieExpiryMinutes = aperture.config.get()['influent.config']['sessionTimeoutInMinutes'] || 24*60;
	                var sessionId = _UIObjectState.sessionId;
	                
                	cookieUtil.createCookie(cookieId, sessionId, cookieExpiryMinutes);
                }
            );

            
            // aperture.capture.initialize();

            // Create a workspace object.
            _initWorkspaceState(
                function() {

                    _log('system', 'Workspace modules successfully initialized.');

                    _pruneColumns();

                    // publish selection event if the footer expands
                    $('#footer').bind('accordionchangestart',
                        function( event, ui ) {
                            var footerContent = $('#footer-content');
                            _UIObjectState.footerDisplay = (footerContent.css('display') == 'none') ? 'block' : 'none';

                            if(footerContent.css('display') == 'none' && _UIObjectState.selectedUIObject != null) {
                                aperture.pubsub.publish(chan.SELECTION_CHANGE_EVENT, {
                                    xfId: _UIObjectState.selectedUIObject.xfId,
                                    dataId: _UIObjectState.selectedUIObject.dataId,
                                    uiType: _UIObjectState.selectedUIObject.uiType,
                                    uiSubtype: _UIObjectState.selectedUIObject.uiSubtype,
                                    contextId: _UIObjectState.selectedUIObject.contextId
                                });
                            }
                        }
                    );

                    if (_UIObjectState.focus != null) {
                        aperture.pubsub.publish(
                            chan.FOCUS_CHANGE_EVENT,
                            {
                                xfId: _UIObjectState.focus.xfId,
                                dataId: _UIObjectState.focus.dataId,
                                entityType: _UIObjectState.focus.entityType,
                                entityLabel: _UIObjectState.focus.entityLabel,
                                entityCount: _UIObjectState.focus.entityCount,
                                contextId: _UIObjectState.focus.contextId,
                                noRender: true
                            }
                        );
                    }

                    if (_UIObjectState.selectedUIObject != null) {
                        aperture.pubsub.publish(
                            chan.SELECTION_CHANGE_REQUEST,
                            {
                                xfId: _UIObjectState.selectedUIObject.xfId,
                                selected : true,
                                noRender: true
                            }
                        );
                    }

                    aperture.pubsub.publish(
                        chan.DETAILS_CHANGE_EVENT,
                        {
                            showDetails: _UIObjectState.showDetails,
                            noRender: true
                        }
                    );

                    aperture.pubsub.publish(
                        chan.FILTER_CHANGE_EVENT,
                        {
                            startDate: _UIObjectState.dates.startDate,
                            endDate: _UIObjectState.dates.endDate,
                            numBuckets: _UIObjectState.dates.numBuckets,
                            duration: _UIObjectState.dates.duration,
                            noRender: true
                        }
                    );

                    var renderRequest = function() {
                        aperture.pubsub.publish(
                            chan.RENDER_UPDATE_REQUEST,
                            {
                                UIObject :  _UIObjectState.singleton,
                                layoutRequest : _getLayoutUpdateRequestMsg()
                            }
                        );
                    };

                    _updateAllColumnSankeys(renderRequest);

                    _updateAllColumnFileSankeys(renderRequest);
                }
            );
        };

        //--------------------------------------------------------------------------------------------------------------

        var _initWorkspaceState = function(callback) {
            aperture.io.rest(
                '/persist',
                'GET',
                function (response) {

                    var workspaceSpec = null;
                    var workspaceUIObj = null;

                    if (response.data == null || response.data.length < 1) {
                    	
                        _log('system', 'New session started.');
                        
                        // Set the flag for displaying/hiding charts.
                        var show = aperture.config.get()['influent.config']['defaultShowDetails'];

                        _UIObjectState.showDetails = show != null? show : true;

                        aperture.pubsub.publish(chan.DETAILS_CHANGE_EVENT, {showDetails : _UIObjectState.showDetails});

                        var initDuration = aperture.config.get()['influent.config']['startingDateRange'];
                        var initEndDate = aperture.config.get()['influent.config']['defaultEndDate'] || new Date();
                        
                        initEndDate = duration.roundDateByDuration(initEndDate, initDuration);
                        var initStartDate = duration.roundDateByDuration(duration.subtractFromDate(initDuration, initEndDate), initDuration);
                        _UIObjectState.dates = {startDate: initStartDate, endDate: initEndDate, numBuckets: 16, duration: initDuration};

                        workspaceSpec = xfWorkspaceModule.getSpecTemplate();
                        workspaceUIObj = xfWorkspaceModule.createSingleton(workspaceSpec);
                        _UIObjectState.singleton = workspaceUIObj;

                        // Create an empty column container and add to the workspace
                        var columnSpec = xfColumn.getSpecTemplate();
                        var columnUIObj = _createColumn(columnSpec);
                        workspaceUIObj.insert(columnUIObj, null);

                        // Create an initial file and add to the above column
                        var fileSpec = xfFile.getSpecTemplate();
                        var fileUIObj = xfFile.createInstance(fileSpec);
                        columnUIObj.insert(fileUIObj, null);

                        fileUIObj.showSearchControl(true, '');
                        aperture.pubsub.publish(
                            chan.SEARCH_CONTROL_FOCUS_CHANGE_REQUEST,
                            {
                                xfId: fileUIObj.getMatchUIObject().getXfId(),
                                noRender: true
                            }
                        );

                    } else {
                        _log('system', 'Restoring existing session...');

                        var restoreState = JSON.parse(response.data);

                        workspaceSpec = xfWorkspaceModule.getSpecTemplate();
                        workspaceUIObj = xfWorkspaceModule.createSingleton(workspaceSpec);
                        _UIObjectState.singleton = workspaceUIObj;

                        _UIObjectState.singleton.cleanState();
                        _UIObjectState.singleton.restoreVisualState(restoreState);
                        _UIObjectState.singleton.restoreHierarchy(restoreState, _UIObjectState.singleton);
                    }

                    _pruneColumns();
                    
                    callback();
                },
                {
                    params : {
                        sessionId : _UIObjectState.sessionId
                    },
                    contentType: 'application/json'
                }
            );
        };

        //--------------------------------------------------------------------------------------------------------------

        var _pubsubHandler = function(eventChannel, data) {

            var renderCallback = function(duplicateDataIds) {

                if (duplicateDataIds != null && duplicateDataIds.length > 0) {
                    _updateDuplicates(duplicateDataIds);
                }

                if (data == null || !data.noRender) {
                    aperture.pubsub.publish(
                        chan.RENDER_UPDATE_REQUEST,
                        {
                            UIObject :  _UIObjectState.singleton,
                            layoutRequest : _getLayoutUpdateRequestMsg()
                        }
                    );
                }
            };

            switch (eventChannel) {
	            case chan.LOGOUT_REQUEST : 
	            	_onLogout();
	            	break;
                case chan.SEARCH_REQUEST :
                    _onSearchRequest(eventChannel, data, renderCallback);
                    break;
                case chan.DETAILS_CHANGE_REQUEST :
                    _onDetailsChangeRequest(eventChannel, data, renderCallback);
                    break;
                case chan.FILTER_CHANGE_REQUEST :
                    _onFilterChangeRequest(eventChannel, data, renderCallback);
                    break;
                case chan.ADD_TO_FILE_REQUEST :
                case chan.DROP_EVENT :
                	_onAddCardToContainer(eventChannel, data, renderCallback);
                    break;
                case chan.FOCUS_CHANGE_REQUEST :
                    _onFocusChangeRequest(eventChannel, data);
                    break;
                case chan.FOCUS_CHANGE_EVENT :
                    _onFocusChangeEvent(eventChannel, data, renderCallback);
                    break;
                case chan.SELECTION_CHANGE_REQUEST :
                    _onSelectionChangeRequest(eventChannel, data, renderCallback);
                    break;
                case chan.HOVER_CHANGE_REQUEST :
                    _onHoverChangeRequest(eventChannel, data);
                    break;
                case chan.CHANGE_FILE_TITLE :
                    _onChangeFileTitle(eventChannel, data, renderCallback);
                    break;
                case chan.SEARCH_CONTROL_FOCUS_CHANGE_REQUEST :
                    _setSearchControlFocused(eventChannel, data, renderCallback);
                    break;
                case chan.SEARCH_BOX_CHANGED :
                    _onSearchBoxChanged(eventChannel, data, renderCallback);
                    break;
                case chan.APPLY_PATTERN_SEARCH_TERM :
                    _applyPatternSearchTerm(eventChannel, data, renderCallback);
                    break;
                case chan.BRANCH_LEFT_EVENT :
                    _onBranchLeftEvent(eventChannel, data, renderCallback);
                    break;
                case chan.BRANCH_RIGHT_EVENT :
                    _onBranchRightEvent(eventChannel, data, renderCallback);
                    break;
                case chan.EXPAND_EVENT :
                    _onExpandEvent(eventChannel, data, renderCallback);
                    break;
                case chan.COLLAPSE_EVENT :
                    _onCollapseEvent(eventChannel, data, renderCallback);
                    break;
                case chan.REMOVE_REQUEST :
                    _onRemoveRequest(eventChannel, data, renderCallback);
                    break;
                case chan.CREATE_FILE_REQUEST :
                    _onCreateFileRequest(eventChannel, data, renderCallback);
                    break;
                case chan.SHOW_MATCH_REQUEST :
                    _onShowMatchRequest(eventChannel, data, renderCallback);
                    break;
                case chan.PREV_SEARCH_PAGE_REQUEST :
                case chan.NEXT_SEARCH_PAGE_REQUEST :
                case chan.SET_SEARCH_PAGE_REQUEST :
                    _onPageSearch(eventChannel, data, renderCallback);
                    break;
                case chan.CLEAN_COLUMN_REQUEST :
                    _onCleanColumnRequest(eventChannel, data, renderCallback);
                    break;
                case chan.EXPORT_CAPTURED_IMAGE_REQUEST :
                    _onExportCapture(eventChannel);
                    break;
                case chan.EXPORT_GRAPH_REQUEST :
                    _onExportGraph(eventChannel);
                    break;
                case chan.NEW_WORKSPACE_REQUEST :
                	_onNewWorkspace(eventChannel);
                	break;
                case chan.HIGHLIGHT_SEARCH_ARGUMENTS :
                	_onHighlightSearchArguments(eventChannel, data);
                    break;
                case chan.REQUEST_CURRENT_STATE :
                    _onCurrentStateRequest(eventChannel);
					break;
                case chan.REQUEST_OBJECT_BY_XFID :
                    _onObjectRequest(eventChannel, data);
                    break;
                case chan.FOOTER_STATE :
                    _onFooterChange(eventChannel, data);
                    break;
                case chan.SORT_COLUMN_REQUEST :
                    _onSortColumnRequest(eventChannel, data, renderCallback);
                    break;
                default :
                   break;
            }
        };

        //--------------------------------------------------------------------------------------------------------------

        var _onFooterChange = function(eventChannel, data) {
            $('#workspace').css('bottom', data.height);
        };

        //--------------------------------------------------------------------------------------------------------------

        var _updateLinks = function(performCascadeRight, performCascadeLeft, linkingObjects, callback, affectedObjects) {

            // remove all links from the affected object and determine
            // dataIds for duplication calculations
            var affectedDataIds = [];
            for (var i = 0; i < affectedObjects.length; i++) {
                affectedObjects[i].removeAllLinks(xfLinkType.FLOW);
                affectedDataIds.push(affectedObjects[i].getDataId());
            }

            var numberOfCascades = 0;
            if (performCascadeRight && performCascadeLeft) {
                numberOfCascades = 2;
            } else if (performCascadeRight || performCascadeLeft) {
                numberOfCascades = 1;
            }

            // create a couter callback so we only call our passed in call back after we have completed
            // links to boths sides of the column
            var counter = 0;
            var counterCallback = function() {
                counter++;
                if (counter == numberOfCascades) {
                    callback(affectedDataIds);
                }
            };

            // determine new links
            if (performCascadeRight && performCascadeLeft) {
                _handleCascadingLinks(linkingObjects,true, counterCallback);
                _handleCascadingLinks(linkingObjects, false, counterCallback);
            } else if (performCascadeRight) {
                _handleCascadingLinks(linkingObjects, true, counterCallback);
            } else if (performCascadeLeft) {
                _handleCascadingLinks(linkingObjects, false, counterCallback);
            } else {
                callback(affectedDataIds);
            }
        };

        //--------------------------------------------------------------------------------------------------------------
        var _createColumn = function(columnSpec){
            var columnObj = xfColumn.createInstance(_.isEmpty(columnSpec)?'':columnSpec);
            _log(
                'user',
                'create-column-event',
                'Create new column',
                _loggerState.type.WF_SEARCH,
                {
                    sessionId : _UIObjectState.sessionId,
                    xfId : columnObj.getXfId(),
                    totalColumns : _UIObjectState.singleton.getChildren().length
                }
            );
            return columnObj;
        };
        //--------------------------------------------------------------------------------------------------------------

        // check for column additions and removals
        var _pruneColumns = function() {
            var changed = false;
            var columns = _UIObjectState.children;
            var workspace = _UIObjectState.singleton;
            var column;

            // we should always have an empty column to the far left.
            if (!columns[0].isEmpty()) {
                column = _createColumn();
                column.setParent(workspace);
                columns.splice(0, 0, column);
                changed = true;
                // but not two
            } else if (columns.length > 2 && columns[1].isEmpty()) {
                columns.splice(0, 1);
                changed = true;
            }

            // same for right.
            if (!columns[columns.length-1].isEmpty()) {
                column = _createColumn();
                column.setParent(workspace);
                columns.push(column);
                changed = true;
            } else if (columns.length > 2 && columns[columns.length-2].isEmpty()) {
                columns.splice(columns.length-1, 1);
                changed = true;
            }

            return changed;
        };

        //--------------------------------------------------------------------------------------------------------------

        var _applyPatternSearchTerm = function(eventChannel, data, renderCallback) {

            var searchTerm = data.searchTerm;
            if(searchTerm == null) {
                searchTerm = _getDefaultPatternSearchTerm(data.xfId);
            }

            var fileObject = _UIObjectState.singleton.getUIObjectByXfId(data.xfId);

            if(!fileObject) {
                console.error('Unable to apply pattern search; file with XfId ' + data.xfId + ' does not exist');
                return;
            }

            var matchObject = fileObject.getMatchUIObject();

            if(!matchObject) {
                console.error('Unable to apply pattern search; match card  from file with XfId ' + data.xfId + ' does not exist');
                return;
            }

            matchObject.setSearchTerm(searchTerm);

            renderCallback();
        };

        //--------------------------------------------------------------------------------------------------------------

        var _getDefaultPatternSearchTerm = function(fileXfId) {

            var i;

            var fileObject = _UIObjectState.singleton.getUIObjectByXfId(fileXfId);

            if (!fileObject) {
                console.error('Unable to apply pattern search; file with XfId ' + fileXfId + ' does not exist');
                return '';
            }

            var fileChildren = fileObject.getChildren();
            var allDataIds = [];

            if (fileChildren.length == 0) {
                return '';
            }

            // Add all the card ids
            for (i = 0; i < fileChildren.length; i++) {
                var id = fileChildren[i].getDataId();
                if (allDataIds.indexOf(id) == -1) {
                    allDataIds.push(id);
                }
            }

            // Transform all the ids and create the string
            var str = "like:";
            for (i = 0; i < allDataIds.length; i++) {
                if ( _UIObjectState.childModule.processDataId) {
                    str += "'" + _UIObjectState.childModule.processDataId(allDataIds[i]) + "'";
                } else {
                    str += "'" + allDataIds[i] + "'";
                }
                str += ",";
            }

            return str.substring(0, str.length - 1);
        };

        //--------------------------------------------------------------------------------------------------------------

        var _isPartialQBESearch = function(data) {
            return data && (data.indexOf('like:') != -1);
        };

        //--------------------------------------------------------------------------------------------------------------

        var _isAdvancedSearch = function(data) {
            var propCount = 0;
            for (var prop in data) {
                if (data.hasOwnProperty(prop)) {
                    propCount++;
                }
            }

            return propCount > 2;
        };

        //--------------------------------------------------------------------------------------------------------------
        
        var _getAllMatchCards = function() {
            var columnExtents = _getMinMaxColumnIndex();
            var columnFiles = '';
            var matchcardObjects = [];
        	var i, j;
        	
            // Do a preliminary scan to see if it's basic or not
            for (i = columnExtents.min; i <= columnExtents.max; i++) {
                columnFiles = xfUtil.getChildrenByType(_getColumnByIndex(i), constants.MODULE_NAMES.FILE);
                for (j = 0; j < columnFiles.length; j++ ) {
                    if (columnFiles[j].hasMatchCard()) {
                        var matchCardObject = columnFiles[j].getMatchUIObject();
                        matchcardObjects.push(matchCardObject);
                    }
                }
            }
            
            return matchcardObjects;
        };

        
        //--------------------------------------------------------------------------------------------------------------

        var _onSearchRequest = function(eventChannel, data, renderCallback) {

            var i, j;

            if (eventChannel != chan.SEARCH_REQUEST) {
                return;
            }

            if (_isAdvancedSearch(data)) {
                var matchUIObject = _UIObjectState.singleton.getUIObjectByXfId(data.xfId).getMatchUIObject();
                matchUIObject.setSearchTerm(data.searchTerm);
                aperture.pubsub.publish(chan.RENDER_UPDATE_REQUEST, {UIObject : matchUIObject});

                renderCallback();

                // DRAPER Instrumentation for logging an advanced search.
                // Search terms omitted from log for privacy concerns.
                _log(
                    'user',
                    eventChannel,
                    'Advanced search',
                    _loggerState.type.WF_SEARCH,
                    {
                        sessionId : _UIObjectState.sessionId
                    }
                );

                return;
            }

            // Create groups of pattern searches and basic searches
            var columnExtents = _getMinMaxColumnIndex();
            var columnFiles = '';
            var matchcardObjects = _getAllMatchCards();

            // if it's basic, do it, otherwise we do an example search, converting any 'basic' matches to 'example' as needed
            if (!USE_PATTERN_SEARCH ||
                (matchcardObjects.length == 1 && !_isPartialQBESearch(matchcardObjects[0].getSearchTerm()))
            ) {
                _basicSearchOnMatchcard(_UIObjectState.singleton.getUIObjectByXfId(data.xfId), renderCallback, eventChannel);
            } else {
                var patternGroups = [];
                var currentPatternGroup = [];

                for (i = 0; i < matchcardObjects.length; i++) {
                	matchcardObjects[i].removeAllChildren();
                	matchcardObjects[i].setSearchState('searching');
                    aperture.pubsub.publish(chan.RENDER_UPDATE_REQUEST, {UIObject : matchcardObjects[i]});
                }
                
                for (i = columnExtents.min; i <= columnExtents.max; i++) {
                    columnFiles = xfUtil.getChildrenByType(_getColumnByIndex(i), constants.MODULE_NAMES.FILE);
                    var columnMatchcards = [];
                    for(j = 0; j < columnFiles.length; j++ ) {
                        if (columnFiles[j].hasMatchCard()) {
                            columnMatchcards.push(columnFiles[j].getMatchUIObject());
                        }
                    }

                    var columnQBE = [];
                    for (j = 0; j < columnMatchcards.length; j++) {
                        if(columnMatchcards[j].getSearchTerm().length > 0) {
                            if (!_isPartialQBESearch(columnMatchcards[j].getSearchTerm())) {
                                _populateEmptyResults(matchcardObjects);
                                return;
                            }
                            columnQBE.push(columnMatchcards[j]);
                        }
                    }
                    currentPatternGroup = currentPatternGroup.concat(columnQBE);
                    if (columnQBE.length == 0 && currentPatternGroup.length > 0) {
                        patternGroups.push(currentPatternGroup);
                        currentPatternGroup = [];
                    }
                }
                if (currentPatternGroup.length > 0) {
                    patternGroups.push(currentPatternGroup);
                }

                for (i = 0; i < patternGroups.length; i++) {
                    _searchByExample(_gatherPatternSearchTerm(patternGroups[i]), data.xfId, renderCallback, eventChannel);
                }
            }
        };

        //--------------------------------------------------------------------------------------------------------------

        var _basicSearchOnMatchcard = function(matchUIObject, renderCallback, eventChannel) {
            var column = _getColumnByUIObject(matchUIObject);

            matchUIObject.removeAllChildren();
            matchUIObject.setSearchState('searching');

            aperture.pubsub.publish(chan.RENDER_UPDATE_REQUEST, {UIObject : matchUIObject});

            aperture.io.rest(
                '/search',
                'POST',
                function(response){
                    var newUIObjects = _populateMatchResults(matchUIObject, response, undefined, renderCallback);
                    _updateLinks(true, true, newUIObjects, renderCallback, newUIObjects);
                },
                {
                    postData : {
                    	sessionId : _UIObjectState.sessionId,
                        term : matchUIObject.getSearchTerm(),
                        queryId: (new Date()).getTime(),
                        cluster: false,                         // Feature #6467 - searches no longer clustered
                        limit : MAX_SEARCH_RESULTS,
                        contextid : column.getXfId(),
                        groupby : SEARCH_GROUP_BY
                    },
                    contentType: 'application/json'
                }
            );
            // DRAPER Instrumentation for logging a basic search.
            // Search terms omitted from log for privacy concerns.
            _log(
                'user',
                eventChannel,
                'Basic search on match card',
                _loggerState.type.WF_SEARCH,
                {
                    sessionId : _UIObjectState.sessionId
                }
            );
        };

        //--------------------------------------------------------------------------------------------------------------

        var _gatherPatternSearchTerm = function(matchGroup) {
            var patternSearchObject = {};
            patternSearchObject.uid = guid.generateGuid();
            patternSearchObject.name = 'Pattern Search Descriptor';
            patternSearchObject.description = null;
            patternSearchObject.entities = [];

            // Create entities.   Each file is an entity with its children being exemplars
            // TODO:  right now we only get xfCards.   Need to extract data ids of all cards contained in a cluster
            for (var i = 0; i < matchGroup.length; i++) {
                var matchCard = matchGroup[i];
                var psEntity = {};
                psEntity.uid = matchCard.getParent().getXfId();
                psEntity.role = {
                    'string' : matchCard.getSearchTerm()
                };
                psEntity.entities = null;
                psEntity.tags = null;
                psEntity.properties = null;

                function parseDataIds(searchTerm, splitString) {
                    var termPieces = searchTerm.split(splitString);
                    var dataIds = termPieces[1].split(',');
                    for (var j = 0; j < dataIds.length; j++) {
                        dataIds[j] = dataIds[j].split('\'').join(' ').trim();
                    }
                    return dataIds;
                }

                var matchCardDataIds = null;
                if (matchCard.getSearchTerm().indexOf('like:') != -1) {
                    matchCardDataIds = parseDataIds(matchCard.getSearchTerm(), 'like:');
                } else {
                    console.error('Unable to determine QuBE search type from search term: ' + matchCard.getSearchTerm());
                }

                psEntity.examplars = {
                    array : matchCardDataIds
                };

                psEntity.constraint = null;
                patternSearchObject.entities.push(psEntity);
            }

            // Create links
            var workspaceLinks = _UIObjectState.singleton.getLinks();
            patternSearchObject.links = [];
            for (var linkId in workspaceLinks) {
                if (workspaceLinks.hasOwnProperty(linkId)) {
                    var link = workspaceLinks[linkId];
                    if (link.getType() == xfLinkType.FILE ) {
                        link.uid = guid.generateGuid();
                        link.source = {
                            string : link.getSource().getXfId()
                        };
                        link.target = {
                            string : link.getDestination().getXfId()
                        };
                        link.role = {
                            string : link.getSource().getLabel() + '->' + link.getDestination().getLabel()
                        };
                        link.tags = null;
                        link.properties = null;
                        link.stage = -1;
                        link.constraint = null;

                        patternSearchObject.links.push(link);
                    }
                }
            }

            return JSON.stringify(patternSearchObject);
        };

        //--------------------------------------------------------------------------------------------------------------

        var _searchByExample = function(term, contextId, renderCallback, eventChannel) {

            aperture.io.rest(
                '/patternsearch',
                'POST',
                function(response){

                	var parsedResponse= aperture.util.viewOf(response);
                    var newUIObjects = _populatePatternResults(parsedResponse, renderCallback);

                    var counter = 0;
                    var totalDataIds = [];
                    var callback = function(dataIds) {
                        counter++;
                        totalDataIds = totalDataIds.concat(dataIds);
                        if (counter == newUIObjects.length) {
                            renderCallback(totalDataIds);
                        }
                    };

                    for (var i = 0; i < newUIObjects.length; i++) {
                        _updateLinks(true, true, newUIObjects[i], callback, newUIObjects[i]);
                    }
                },
                {
                    postData : {
                    	sessionId : _UIObjectState.sessionId,
                        term: term,
                        startDate : _UIObjectState.dates.startDate,
                        endDate :  _UIObjectState.dates.endDate,
                        queryId: (new Date()).getTime(),
                        cluster: false,
                        limit : MAX_PATTERN_SEARCH_RESULTS,
                        contextid : contextId,
                        useAptima : (_getAllMatchCards().length > 3)
                    },
                    contentType: 'application/json'
                }
            );
            // DRAPER Instrumentation for logging a pattern search.
            // Search terms omitted from log for privacy concerns.
            _log(
                'user',
                eventChannel,
                'Search by example',
                _loggerState.type.WF_SEARCH,
                {
                    sessionId : _UIObjectState.sessionId,
                    startDate : _UIObjectState.dates.startDate,
                    endDate :  _UIObjectState.dates.endDate//,
                    //useAptima : (_getAllMatchCards().length > 3)
                }
            );
        };

        //--------------------------------------------------------------------------------------------------------------

        var _populateEmptyResults = function(matchcardObjects) {

            for (var i = 0; i < matchcardObjects.length; i++) {

                var matchcard = matchcardObjects[i];

                if (matchcard.getUIType() != constants.MODULE_NAMES.MATCH) {
                    continue;
                }

                matchcard.setSearchState('results');
                matchcard.setTotalMatches(0);
                matchcard.setShownMatches(0);
            }
        };

        //--------------------------------------------------------------------------------------------------------------

        var _populatePatternResults = function(serverResponse, renderCallback) {

            var j, k;

            // For each match entity (file) get a set of unique entities that belong in that file
            var workspaceFiles = xfUtil.getChildrenByType(_UIObjectState.singleton, constants.MODULE_NAMES.FILE);

            //Create an xfCard for each entity in each file
            var newMatchResults = [];
            for (k=0; k< serverResponse.roleResults.length; k++) {
            	var roleResult = serverResponse.roleResults[k];
            	var fileId = roleResult.uid;
                
                for (j = 0; j < workspaceFiles.length; j++) {
                    // HACK:  remove this or clause when we actually get this working.   This is only
                    // to allow searching by using examples provided. // commented out latter - suspect. DJ
                    if (fileId == workspaceFiles[j].getXfId() /*|| fileId == workspaceFiles[j].getLabel()*/) {
                        workspaceFiles[j].showSearchControl(true, workspaceFiles[j].getMatchUIObject().getSearchTerm()); // pull from previous
                        
                        var resultUIObjects = _populateMatchResults(
                            workspaceFiles[j].getMatchUIObject(),
                        	{data: roleResult.results},
                            true,
                            renderCallback
                        );
                        
                        newMatchResults.push(resultUIObjects);
                    }
                }
            }

            return newMatchResults;
        };

        //--------------------------------------------------------------------------------------------------------------

        var _populateMatchResults = function(parent, serverResponse, noRemove, renderCallback) {

            var createdUIObjects = [];

            if (parent.getUIType() == constants.MODULE_NAMES.MATCH) {
                parent.setSearchState('results');
                aperture.pubsub.publish(chan.RENDER_UPDATE_REQUEST, {UIObject : parent});
            }

            var entities = serverResponse.data;
            var firstSearchObject = null;
            var specs = _getPopulatedSpecsFromData(entities, parent);
            var dataIds = [];

            var shownMatches = 0;
            for (var i = 0; i < specs.length; i++) {

                var UIObject = {};

                if (specs[i].type === constants.MODULE_NAMES.MUTABLE_CLUSTER) {
                    UIObject = xfMutableCluster.createInstance(specs[i]);
                } else if (specs[i].type === constants.MODULE_NAMES.CLUSTER_BASE) {
                    if (specs[i].subtype === constants.SUBTYPES.ACCOUNT_OWNER) {
                        UIObject = xfImmutableCluster.createInstance(specs[i]);
                    } else {
                        UIObject = xfMutableCluster.createInstance(specs[i]);
                    }
                } else if (specs[i].type === constants.MODULE_NAMES.SUMMARY_CLUSTER) {
                    UIObject = xfSummaryCluster.createInstance(specs[i]);
                } else {
                    UIObject =  xfCard.createInstance(specs[i]);
                }

                shownMatches = shownMatches+1;

                UIObject.showDetails(_UIObjectState.showDetails);
                // determine what the first search object is
                if (i == 0) {
                    firstSearchObject = UIObject;
                }

                parent.insert(UIObject, null);

                var dataId = UIObject.getDataId();
                if (dataId != null) {
                    dataIds.push(dataId);
                }
                createdUIObjects.push(UIObject);
            }

            //Set the total number of matches, to display
            if (parent.setTotalMatches) {
                parent.setTotalMatches(serverResponse.totalResults != null ? serverResponse.totalResults : shownMatches);
            }
            if (parent.setShownMatches) {
                parent.setShownMatches(shownMatches);
            }

            // if there is no currently focused object, then we set the first search object as the
            // focused object
            if (_UIObjectState.focus == null && firstSearchObject != null) {
                var columnObj = xfUtil.getUITypeAncestor(firstSearchObject, constants.MODULE_NAMES.COLUMN);

                // note this will trigger card chart updates as well, which is why we have an else below
                aperture.pubsub.publish(chan.FOCUS_CHANGE_EVENT, {
                    xfId: firstSearchObject.getXfId(),
                    dataId: firstSearchObject.getDataId(),
                    entityType: firstSearchObject.getUIType(),
                    entityLabel: firstSearchObject.getLabel(),
                    entityLabel: firstSearchObject.getVisualInfo().spec.count,
                    contextId: columnObj? columnObj.getXfId() : null
                });

            // else if we are showing card details then we need to populate the card specs with chart data
            } else if (_UIObjectState.showDetails) {
                _UIObjectState.childModule.updateCardsWithCharts(specs);
            }

            renderCallback(dataIds);

            return createdUIObjects;
        };

        //--------------------------------------------------------------------------------------------------------------

        var _getPopulatedSpecsFromData = function(data, parent) {

            var specs = [];

            for (var i = 0; i < data.length; i++) {
                var entity = data[i];

                var spec = {};
                spec.showSpinner = false;
                if (entity.entitytype == constants.SUBTYPES.ENTITY_CLUSTER || entity.entitytype == constants.SUBTYPES.ACCOUNT_OWNER) {
                    spec = xfClusterBase.getSpecTemplate();
                } else if (entity.entitytype == constants.SUBTYPES.CLUSTER_SUMMARY) {
                    spec = xfSummaryCluster.getSpecTemplate();
                } else {
                    spec = xfCard.getSpecTemplate();
                }
                _UIObjectState.childModule.populateSpecWithData(entity, spec);

                if (spec.hasOwnProperty('parent')) {
                    spec['parent'] = parent;
                } else {
                    console.error('Spec does not contain "parent" element');
                }

                specs.push(spec);
            }

            return specs;
        };

        //--------------------------------------------------------------------------------------------------------------

        var _flattenCluster = function(cluster, contextid, parentObj, renderCallback){
            var specs = cluster.getSpecs(false);
            if (specs == null || specs.length != 1) {
                console.error('Failed to get cluster specs.');
            }

            // we need to create all the children of the cluster from the member specs
            // and insert them into the cluster
            var memberSpecs = specs[0].members;
            var memberIds = [];
            for (var i = 0; i < memberSpecs.length; i++) {
                memberIds.push(memberSpecs[i].dataId);
            }

            aperture.io.rest(
                '/entities',
                'POST',
                function (response) {
                    var entities = response.data;
                    var specs = _getPopulatedSpecsFromData(entities, null);

                    // Check if this cluster has focus. If it does, we need
                    // to assign something else the focus. By default, we'll
                    // choose the first element in the flattened list.
                    var hasFocus = _UIObjectState.singleton.getFocus().xfId == cluster.getXfId();

                    var cardObjects = [];
                    var dataIds = [];
                    for (var i = 0; i < specs.length; i++) {

                        var uiObject = {};
                        if (specs[i].type == constants.MODULE_NAMES.SUMMARY_CLUSTER) {
                            uiObject = xfSummaryCluster.createInstance(specs[i]);
                        } else if (specs[i].type == constants.MODULE_NAMES.CLUSTER_BASE || specs[i].type == constants.MODULE_NAMES.IMMUTABLE_CLUSTER) {
                            uiObject = xfImmutableCluster.createInstance(specs[i]);
                        } else if (specs[i].type == constants.MODULE_NAMES.ENTITY) {
                            uiObject =  xfCard.createInstance(specs[i]);
                        } else {
                            continue;
                        }

                        uiObject.showDetails(_UIObjectState.showDetails);
                        parentObj.insert(uiObject, null);

                        cardObjects.push(uiObject);
                        dataIds.push(uiObject.getDataId());

                        if (parentObj.getUIType() != constants.MODULE_NAMES.COLUMN){
                            uiObject.showToolbar(parentObj.showToolbar());
                        }
                    }
                    if (hasFocus && cardObjects.length > 0){
                        aperture.pubsub.publish(chan.FOCUS_CHANGE_REQUEST, {xfId:cardObjects[0].getXfId()});
                    }

                    _updateLinks(true, true, cardObjects, renderCallback, cardObjects);
                },
                {
                    postData : {
                    	sessionId : _UIObjectState.sessionId,
                        queryId: (new Date()).getTime(),
                        entities : memberIds,
                        contextid : contextid,
                        isFlattened : true
                    },
                    contentType: 'application/json'
                }
            );
        };
        //--------------------------------------------------------------------------------------------------------------

        var _collectAndShowDetails = function(bOnlyEmptySpecs) {
            // If we are showing details then we need to update all the charts that do not already have
            // chart info
            var specs = _UIObjectState.singleton.getSpecs(bOnlyEmptySpecs);
            _UIObjectState.childModule.updateCardsWithCharts(specs);
        };


        var _onLogout = function() {
        	// looked at saving state here but it appears we do successfully save state on 'beforeunload'
        	// before the server processes logout.        	
            window.location = 'logout';
        };

        var _onDetailsChangeRequest = function(eventChannel, data, renderCallback) {

            if (eventChannel != chan.DETAILS_CHANGE_REQUEST) {
                return;
            }

            _UIObjectState.showDetails = data.showDetails;

            if (_UIObjectState.showDetails) {
                _collectAndShowDetails();
            }

            _UIObjectState.singleton.showDetails(_UIObjectState.showDetails);

            aperture.pubsub.publish(chan.DETAILS_CHANGE_EVENT, {showDetails : _UIObjectState.showDetails});

            // Now issue a rendering request to update the cards of the workspace to
            // reflect the new "showDetails" state.
            renderCallback();

            _log(
                'user',
                eventChannel,
                'Toggle details (e.g. charts) of all visible items',
                _loggerState.type.WF_MARSHAL,
                {
                    sessionId : _UIObjectState.sessionId,
                    showDetails : data.showDetails
                }
            );
        };

        //--------------------------------------------------------------------------------------------------------------

        var _onFilterChangeRequest = function(eventChannel, data, renderCallback) {

            if (eventChannel != chan.FILTER_CHANGE_REQUEST) {
                return;
            }

            _UIObjectState.dates.startDate = data.start;
            _UIObjectState.dates.endDate = data.end;
            _UIObjectState.dates.numBuckets = data.numBuckets;
            _UIObjectState.dates.duration = data.duration;

            if (_UIObjectState.showDetails) {
                // If we are showing details then we need to update all the charts
                var specs = _UIObjectState.singleton.getSpecs(false);
                _UIObjectState.childModule.updateCardsWithCharts(specs);
            }

            // Update the Sankeys.
            _updateAllColumnSankeys(renderCallback);

            // pass on event
            aperture.pubsub.publish(
                chan.FILTER_CHANGE_EVENT,
                {
                    startDate : data.start,
                    endDate : data.end,
                    numBuckets: data.numBuckets,
                    duration: data.duration
                }
            );
        };

        //--------------------------------------------------------------------------------------------------------------

        var _onAddCardToContainer = function(eventChannel, data, renderCallback) {

            if (eventChannel != chan.DROP_EVENT && eventChannel != chan.ADD_TO_FILE_REQUEST) {
                console.error('Drop event handler does not support this event: ' + eventChannel);
                return;
            }

            var addCard = _UIObjectState.singleton.getUIObjectByXfId(data.cardId);
            var targetContainer = _UIObjectState.singleton.getUIObjectByXfId(data.containerId);
            var cardParent = addCard.getParent();

            if(addCard == null) {
                console.error('DropEvent addCard is null, id : ' + data.cardId); return;
            }
            else if(targetContainer == null) {
                console.error('DropEvent targetContainer is null, id : ' + data.containerId); return;
            }
            else if(cardParent == null) {
                console.error('DropEvent addCard parent is null, addCard id : ' + data.cardId); return;
            }

            var cardColumn = _getColumnByUIObject(addCard);
            var containerColumn = _getColumnByUIObject(targetContainer);

            if(cardColumn == null) {
                console.error('DropEvent cardColumn is null, addCard id : ' + data.cardId); return;
            }
            else if(containerColumn == null) {
                console.error('DropEvent containerColumn is null, targetContainer id : ' + data.containerId); return;
            }

            var insertedCard;
            if (_useCopyMethod(addCard)) {
                insertedCard = addCard.clone();
            } else {
                insertedCard = addCard;
                _removeObject(
                    insertedCard,
                    chan.REMOVE_REQUEST,
                    false,
                    false
                );
            }

            // If this is a cluster, flatten out the cluster hierarchy
            // and create a flattened out list of all child cards.
            if (xfUtil.isClusterTypeFromObject(insertedCard) && insertedCard.getUIType() != constants.MODULE_NAMES.SUMMARY_CLUSTER) {
                _flattenCluster(
                    insertedCard,
                    cardColumn.getXfId(),
                    targetContainer,
                    function(dataIds) {

                        _pruneColumns();

                        var specs = (insertedCard.getSpecs(false))[0].members;
                        _UIObjectState.childModule.updateCardsWithCharts(specs);

                        if (targetContainer.getUIType() == constants.MODULE_NAMES.FILE){
                        	targetContainer = targetContainer.getClusterUIObject();
                        	
                        	if(targetContainer.getUIType() != constants.MODULE_NAMES.MUTABLE_CLUSTER) {
                        		console.error('DropEvent containerColumn is null, targetContainer id : ' + data.containerId); return;
                        	}
                            _modifyContext(containerColumn.getXfId(), 'insert', targetContainer.getDataId(), dataIds);

                            // If the file is expanded and we're adding a cluster, collapse the file.
                            if (targetContainer.isExpanded()) {
                                targetContainer.collapse();
                            }

                            // If this is a collapsed file cluster then update the cluster spec.
                            if (!targetContainer.isExpanded()) {
                                _updateClusterSpec(targetContainer);
                                if (_UIObjectState.showDetails) {
                                    _UIObjectState.childModule.updateCardsWithCharts(targetContainer.getSpecs(false));
                                }
                            }
                        }

                        renderCallback(dataIds);
                    }
                );
            } else {
                targetContainer.insert(insertedCard);
                insertedCard.showToolbar(true);

                _pruneColumns();

                if (targetContainer.getUIType() == constants.MODULE_NAMES.FILE){
                    targetContainer = targetContainer.getClusterUIObject();

                    if(targetContainer.getUIType() != constants.MODULE_NAMES.MUTABLE_CLUSTER) {
                        console.error('DropEvent containerColumn is null, targetContainer id : ' + data.containerId); return;
                    }
                }

                // If this is the first item added to the file, expand the file
                if (targetContainer.getChildren().length == 1) {
                    targetContainer.expand();
                }

                // If this is a collapsed file cluster then update the cluster spec.
                if (xfUtil.isFileCluster(targetContainer) && !targetContainer.isExpanded()) {
                    _updateClusterSpec(targetContainer);
                    if (_UIObjectState.showDetails) {
                        _UIObjectState.childModule.updateCardsWithCharts(targetContainer.getSpecs(false));
                    }
                }

                // Update the server side cache with the new cluster hierarchy.
                _modifyContext(containerColumn.getXfId(), 'insert', targetContainer.getDataId(), targetContainer.getContainedCardDataIds(),
                    // Callback to be performed after the modify context operation has completed.
                    function(){
                        // Update any incoming/outgoing links.
                        var isCollapsedCluster = xfUtil.isClusterTypeFromObject(targetContainer) && !targetContainer.isExpanded();
                        _updateLinks(true, true, [isCollapsedCluster?targetContainer:insertedCard], renderCallback, [targetContainer, insertedCard]);
                    });
            }
            
            //Draper instrumentation
            _log('user', eventChannel, 'Add to file', _loggerState.type.WF_MARSHAL,
                {
                    sessionId : _UIObjectState.sessionId,
                    fileId : targetContainer.getXfId(),
                    //fileLabel : targetContainer.getLabel(),
                    xfId : addCard.getXfId(),
                    //dataId : addCard.getDataId(),
                    contextId : containerColumn.getXfId(),
                    childCount: targetContainer.getChildren().length
                }
            );
        };

        //--------------------------------------------------------------------------------------------------------------

        var _useCopyMethod = function(addCard) {
            return addCard.getParent().getUIType() == constants.MODULE_NAMES.IMMUTABLE_CLUSTER;
        };

        //--------------------------------------------------------------------------------------------------------------

        var _onFocusChangeRequest = function(eventChannel, data) {

            if (eventChannel != chan.FOCUS_CHANGE_REQUEST) {
                return;
            }

            var xfUiObj = _UIObjectState.singleton.getUIObjectByXfId(data.xfId);

            if (xfUiObj) {
                var columnObj = xfUtil.getUITypeAncestor(xfUiObj, constants.MODULE_NAMES.COLUMN);

                var focusData = {
                    xfId: xfUiObj.getXfId(),
                    dataId: xfUiObj.getDataId(),
                    entityType: xfUiObj.getUIType(),
                    entityLabel: xfUiObj.getLabel(),
                    entityCount: xfUiObj.getVisualInfo().spec.count,
                    contextId: columnObj? columnObj.getXfId() : null
                };

                aperture.pubsub.publish(chan.FOCUS_CHANGE_EVENT, focusData);
            }
        };

        //--------------------------------------------------------------------------------------------------------------

        var _onFocusChangeEvent = function(eventChannel, data, renderCallback) {

            if (eventChannel != chan.FOCUS_CHANGE_EVENT) {
                return;
            }

            _UIObjectState.singleton.setFocus(data);

            if (_UIObjectState.showDetails) {
                // If we are showing details then we need to update all the charts
                _collectAndShowDetails(false);
            }

            renderCallback();
        };

        //--------------------------------------------------------------------------------------------------------------

        var _onSelectionChangeRequest = function(eventChannel, data, renderCallback) {

            if (eventChannel != chan.SELECTION_CHANGE_REQUEST) {
                return;
            }

            var objectToBeSelected = data.xfId && _UIObjectState.singleton.getUIObjectByXfId(data.xfId);

            if (objectToBeSelected == null) {
                _UIObjectState.selectedUIObject = null;
                _UIObjectState.singleton.setSelection(null);
                
            } else {
                _UIObjectState.selectedUIObject = {
                    xfId: objectToBeSelected.getXfId(),
                    dataId: objectToBeSelected.getDataId(),
                    label: objectToBeSelected.getLabel(),
                    count: objectToBeSelected.getVisualInfo().spec.count,
                    uiType: objectToBeSelected.getUIType(),
                    uiSubtype: objectToBeSelected.getUISubtype(),
                    contextId: xfUtil.getUITypeAncestor(objectToBeSelected, 'xfColumn').getXfId()
                };
                _UIObjectState.singleton.setSelection(objectToBeSelected.getXfId());
            }

            // only publish change event if we can see the results of said change
            if($('#footer-content').css('display') != 'none') {
                aperture.pubsub.publish(chan.SELECTION_CHANGE_EVENT, _UIObjectState.selectedUIObject);
            }

            renderCallback();
        };

        //--------------------------------------------------------------------------------------------------------------

        var _onHoverChangeRequest = function(eventChannel, data) {
            if (eventChannel != chan.HOVER_CHANGE_REQUEST) {
                return;
            }

            _UIObjectState.singleton.setHovering(data.xfId);
        };

        //--------------------------------------------------------------------------------------------------------------

        var _onChangeFileTitle = function(eventChannel, data, renderCallback) {

            if (eventChannel != chan.CHANGE_FILE_TITLE) {
                return;
            }

            var file = _UIObjectState.singleton.getUIObjectByXfId(data.xfId);
            file.setLabel(data.newTitleText);

            renderCallback();
        };

        //--------------------------------------------------------------------------------------------------------------

        var _updateAllColumnSankeys = function(renderCallback) {

            var columns = _UIObjectState.children;
            if (columns.length > 3) {

                for (var i = 1; i < columns.length - 2; i++) {
                    var currentColumn = columns[i];
                    var cardObjects =  xfUtil.getLinkableChildren(currentColumn);
                    _updateLinks(true, false, cardObjects, renderCallback, cardObjects);
                }
            } else {
                renderCallback();
            }
        };

        //--------------------------------------------------------------------------------------------------------------

        var _updateAllColumnFileSankeys = function(renderCallback) {

            var columns = _UIObjectState.children;
            if (columns.length > 3) {

                for (var i = 1; i < columns.length - 2; i++) {
                    var currentColumn = columns[i];
                    var fileObjects =  xfUtil.getChildrenByType(currentColumn, [constants.MODULE_NAMES.FILE]);
                    for (var j = 0; j < fileObjects.length; j++) {
                        var fileObject = fileObjects[j];
                        _addFileLinks(fileObject, currentColumn, false, true);
                    }
                }
            } 

            renderCallback();
        };

        //--------------------------------------------------------------------------------------------------------------

        var _branchEventHandler = function(eventChannel, data, renderCallback){

            if (eventChannel != chan.BRANCH_RIGHT_EVENT && eventChannel != chan.BRANCH_LEFT_EVENT) {
                console.error('Branch event handler does not support this event: ' + eventChannel);
                return;
            }

            var sourceObj = _UIObjectState.singleton.getUIObjectByXfId(data.xfId);
            var isBranchRight = data.direction == 'right';
            var sourceEntity = sourceObj.getDataId();
            var column = _getColumnByUIObject(sourceObj);

            //Figure out the target column.  If it exists, use it as the target context id
            //If it doesn't create an id for it to use as the target context id, and then
            //assign that id to the created column in the response.
            var targetColumn;
            if (isBranchRight) {
                targetColumn = _getNextColumn(column);
            } else {
                targetColumn = _getPrevColumn(column);
            }

            if (targetColumn == null) {
                targetColumn = _createColumn();
            }

            _log(
                'user',
                eventChannel,
                'Branch for related links',
                _loggerState.type.WF_SEARCH,
                {
                    sessionId : _UIObjectState.sessionId,
                    xfId : data.xfId//,
                    //dataId : sourceObj.getDataId()
                }
            );
            aperture.io.rest(
                '/relatedlinks',
                'POST',
                function(response){
                    if(_UIObjectState.singleton.getUIObjectByXfId(data.xfId) != null) {

                        var newUIObjects = _handleBranchingServerResponse(response, sourceObj, isBranchRight, targetColumn);

                        if (isBranchRight) {
                            sourceObj.setRightOperation(toolbarOp.BRANCH);
                        } else {
                            sourceObj.setLeftOperation(toolbarOp.BRANCH);
                        }

                        _updateLinks(true, true, newUIObjects, renderCallback, newUIObjects);
                    } else {
                        renderCallback();
                    }
                },
                {
                    postData : {
                    	sessionId : _UIObjectState.sessionId,
                        queryId: (new Date()).getTime(),
                        entity : sourceEntity,
                        targets : [],
                        linktype : isBranchRight ? 'source' : 'destination',
                        type: 'FINANCIAL',
                        aggregate: 'FLOW',
                        startdate: _UIObjectState.dates.startDate,
                        enddate: _UIObjectState.dates.endDate,
                        contextid:  column.getXfId(),
                        targetcontextid: targetColumn.getXfId()
                    },
                    contentType: 'application/json'
                }
            );
        };

        //--------------------------------------------------------------------------------------------------------------

        var _handleCascadingLinks = function(cardObjectList, cascadeRight, renderCallback) {

            var i;

            if (cardObjectList == null || cardObjectList.length == 0 ) {
                renderCallback();
                return;
            }

            // Get all cards and clusters in column to the right/left of uiObj.  Must look not only for 'dangling'
            // UI objects, but also anything that is in a search result (ie/ inside a match card)
            var column = _getColumnByUIObject(cardObjectList[0]);           // all objects in objectList are in the same column
            var adjacentColumn = cascadeRight ? _getNextColumn(column) : _getPrevColumn(column);
            var adjacentChildren = xfUtil.getLinkableChildren(adjacentColumn);

            // Early out
            if (adjacentChildren.length == 0 ) {
                if (renderCallback){
                    renderCallback();
                }
                return;
            }

            var adjacentChildrenDataIds = [];
            for (i = 0; i < adjacentChildren.length; i++) {
                adjacentChildrenDataIds.push(adjacentChildren[i].getDataId());
            }

            var sourceDataIds = [];
            for (i = 0; i < cardObjectList.length; i++) {
                var cardObject = cardObjectList[i];
                if (sourceDataIds.indexOf(cardObject.getDataId()) < 0){
                    sourceDataIds.push(cardObject.getDataId());
                }

                var cardObjectLinks = cardObject.getLinks();
                for (var linkId in cardObjectLinks) {
                    if (cardObjectLinks.hasOwnProperty(linkId)) {
                        var link = cardObjectLinks[linkId];
                        if (cascadeRight) {
                            if (link.getSource().getXfId() == cardObject.getXfId()) {
                                link.remove();
                            }
                        } else {
                            if (link.getDestination().getXfId() == cardObject.getXfId()) {
                                link.remove();
                            }
                        }
                    }
                }
            }

            // Fetch related links between the unlinked objects
            aperture.io.rest(
                '/aggregatedlinks',
                'POST',
                function(response){
                    _handleCascadingLinksResponse(
                        response,
                        cascadeRight ? 'source' : 'destination',
                        column.getXfId(),
                        adjacentColumn.getXfId(),
                        renderCallback
                    );
                },
                {
                    postData : {
                    	sessionId : _UIObjectState.sessionId,
                        queryId: (new Date()).getTime(),
                        sourceIds : sourceDataIds,
                        targetIds : adjacentChildrenDataIds,
                        linktype : cascadeRight ? 'source' : 'destination',
                        type: 'FINANCIAL',
                        aggregate: 'FLOW',
                        startdate: _UIObjectState.dates.startDate,
                        enddate: _UIObjectState.dates.endDate,
                        contextid:  column.getXfId(),
                        targetcontextid: adjacentColumn.getXfId()
                    },
                    contentType: 'application/json'
                }
            );
        };

        //--------------------------------------------------------------------------------------------------------------

        var _handleCascadingLinksResponse = function(serverResponse, branchType, sourceColumnId, targetColumnId, renderCallback) {
            for (var dataId in serverResponse.data) {
                if (serverResponse.data.hasOwnProperty(dataId)) {
                    var serverLinks = serverResponse.data[dataId];
                    for (var i = 0; i < serverLinks.length; i++) {
                        var serverLink = serverLinks[i];
                        var sourceDataId = serverLink.source;
                        var destDataId = serverLink.target;
                        var linkAmount = xfWorkspaceModule.getValueByTag(serverLink, 'AMOUNT');
                        var linkCount = xfWorkspaceModule.getValueByTag(serverLink, 'CONSTRUCTED');

                        // Figure out the left and right column based on the branch type.   UILinks always
                        // flow left to right
                        var rightColumn = null, leftColumn = null;
                        if (branchType == 'destination') {
                            rightColumn = _UIObjectState.singleton.getUIObjectByXfId(sourceColumnId);
                            leftColumn = _UIObjectState.singleton.getUIObjectByXfId(targetColumnId);
                        } else {
                            rightColumn = _UIObjectState.singleton.getUIObjectByXfId(targetColumnId);
                            leftColumn = _UIObjectState.singleton.getUIObjectByXfId(sourceColumnId);
                        }


                        var sourceUIObjects = leftColumn.getUIObjectsByDataId(sourceDataId);
                        var destUIObjects = rightColumn.getUIObjectsByDataId(destDataId);

                        // Create links between all pairs in sourceUIObjects and destUIObjects
                        for (var sourceIdx = 0; sourceIdx < sourceUIObjects.length; sourceIdx++) {
                            for (var destIdx = 0; destIdx < destUIObjects.length; destIdx++) {
                                var sourceObj = sourceUIObjects[sourceIdx];
                                var destObj = destUIObjects[destIdx];

                                xfLink.createInstance(sourceObj, destObj, linkAmount, linkCount);
                            }
                        }
                    }
                }
            }

            if (renderCallback){
                renderCallback();
            }
        };

        //--------------------------------------------------------------------------------------------------------------

        var _handleBranchingServerResponse = function(serverResponse, sourceObj, isBranchRight, prebuiltTargetColumn) {

            var i;

            var entities = serverResponse.targets;
            var createdUIObjects = [];

            // If there are no related links for the given card, return.
//            if (entities == null || srLinkArray == null || srLinkArray.length == 0){
            if (entities == null){
                return createdUIObjects;
            }
            // Find the source column.
            var sourceColumn = _getColumnByUIObject(sourceObj);
            // Check if there is an existing target column, if not, create one.
            var targetColumn = isBranchRight ? _getNextColumn(sourceColumn) : _getPrevColumn(sourceColumn);

            if (targetColumn == null){
                targetColumn = prebuiltTargetColumn;
                _UIObjectState.singleton.insert(prebuiltTargetColumn, isBranchRight ? null : sourceColumn);
            } else {
                targetColumn.cleanColumn(false);
            }
            
            var specs = _getPopulatedSpecsFromData(entities, targetColumn);

            var firstUiObject = null;

            var dataIds = [];

            var toCollapseIds = [];

            var adjacentInfo = xfUtil.getAdjacentObjectInfo(xfLayoutProvider.getPositionMap(), sourceObj, targetColumn);
            var adjacentObj = adjacentInfo.adjacentObj;
            var parentObj = null;
            if (adjacentObj != null){
                parentObj = adjacentObj.getParent();
            }
            else {
                parentObj = targetColumn;
            }

            for (i = 0; i < specs.length; i++) {
                var uiObject = {};

                //Check to see if there is an existing object in this column
                uiObject = targetColumn.getUIObjectsByDataId(specs[i].dataId)[0];

                if (!uiObject) {
                    if (specs[i].type == constants.MODULE_NAMES.SUMMARY_CLUSTER) {
                        uiObject =  xfSummaryCluster.createInstance(specs[i]);
                    } else if (xfUtil.isClusterTypeFromSpec(specs[i])) {
                        uiObject =  xfImmutableCluster.createInstance(specs[i]);
                    } else {
                        uiObject =  xfCard.createInstance(specs[i]);
                    }

                    uiObject.updateToolbar({
                        'allowFile' : true,
                        'allowClose' : true,
                        'allowFocus' : true,
                        'allowSearch' : true
                    });

                    uiObject.showDetails(_UIObjectState.showDetails);
                    uiObject.showToolbar(true);
                    parentObj.insert(uiObject, adjacentObj);
                } else {
                    if (xfUtil.isClusterTypeFromObject(uiObject)) {
                        uiObject.update(specs[i]);

                        toCollapseIds.push(uiObject.getXfId());
                    }
                }
                createdUIObjects.push(uiObject);

                if (firstUiObject == null){
                    firstUiObject = uiObject;
                }

                var dataId = uiObject.getDataId();
                if (dataId != null) {
                    dataIds.push(dataId);
                }
            }

            // if we are showing card details then we need to populate the card specs with chart data
            if (_UIObjectState.showDetails) {
                _UIObjectState.childModule.updateCardsWithCharts(specs);
            }

            _pruneColumns();
            // check to see if our branching produced duplicates
            _updateDuplicates(dataIds);

            for (i = 0; i < toCollapseIds.length; i++) {
                aperture.pubsub.publish(chan.COLLAPSE_EVENT, {xfId : toCollapseIds[i]});
            }

            return createdUIObjects;
        };

        //--------------------------------------------------------------------------------------------------------------

        var _onBranchLeftEvent = function(eventChannel, data, renderCallback) {
            var uiObj = _UIObjectState.singleton.getUIObjectByXfId(data.xfId);
            uiObj.setLeftOperation(toolbarOp.WORKING);
            aperture.pubsub.publish(chan.RENDER_UPDATE_REQUEST, {UIObject : uiObj});
            _branchEventHandler(eventChannel, data, renderCallback);
        };

        //--------------------------------------------------------------------------------------------------------------

        var _onBranchRightEvent = function(eventChannel, data, renderCallback) {
            var uiObj = _UIObjectState.singleton.getUIObjectByXfId(data.xfId);
            uiObj.setRightOperation(toolbarOp.WORKING);
            aperture.pubsub.publish(chan.RENDER_UPDATE_REQUEST, {UIObject : uiObj});
            _branchEventHandler(eventChannel, data, renderCallback);
        };

        //--------------------------------------------------------------------------------------------------------------

        var _onExpandEvent = function(eventChannel, data, renderCallback) {
            var uiObj = _UIObjectState.singleton.getUIObjectByXfId(data.xfId);

            var column = _getColumnByUIObject(uiObj);

            if (!xfUtil.isClusterTypeFromObject(uiObj)) {
                console.error('Expand request for a non cluster object');
            }

            if (uiObj.isExpanded()) {
                renderCallback();
                return;
            }

            uiObj.expand();

            aperture.pubsub.publish(chan.RENDER_UPDATE_REQUEST, {UIObject : uiObj, layoutRequest : _getLayoutUpdateRequestMsg()});

            //*********************************
            // REST call to get actual card data
            //*********************************
            aperture.io.rest(
                '/entities',
                'POST',
                function (response) {
                    _populateExpandResults(uiObj, response, renderCallback);
                },
                {
                    postData : {
                    	sessionId : _UIObjectState.sessionId,
                        queryId: (new Date()).getTime(),
                        entities : uiObj.getVisibleDataIds(),
                        contextid : column.getXfId()
                    },
                    contentType: 'application/json'
                }
            );

            //Draper instrumentation
            _log('user', eventChannel, 'Expand and show cluster membership', _loggerState.type.WF_EXAMINE,
                {
                    sessionId : _UIObjectState.sessionId,
                    clusterId : data.xfId,
                    //dataId : uiObj.getDataId(),
                    isExpanded : uiObj.isExpanded(),
                    contextId : column.getXfId()
                }
            );
        };

        //--------------------------------------------------------------------------------------------------------------

        var _populateExpandResults = function(clusterObj, serverResponse, renderCallback) {

            var entities = serverResponse.data;
            var specs = _getPopulatedSpecsFromData(entities, clusterObj);

            for (var i = 0; i < specs.length; i++) {
                var childSpec = specs[i];
                var children = clusterObj.getUIObjectsByDataId(childSpec.dataId);
                if (_.isEmpty(children)) {
                    console.error('_populateExpandResults: Unable to find child for spec');
                } else {
                    for (var j = 0; j < children.length; j++) {
                        children[j].update(childSpec);
						// Update the highlight state of any children.
                        children[j].highlightId(_UIObjectState.singleton.getFocus().xfId);
                    }
                }
            }

            clusterObj.showSpinner(false);
            clusterObj.showToolbar(true);

            // if we are showing card details then we need to populate the card specs with chart data
            if (_UIObjectState.showDetails) {
                _UIObjectState.childModule.updateCardsWithCharts(specs);
            }

            var affectedObjects = clusterObj.getChildren();
            affectedObjects.push(clusterObj);
            _updateLinks(true, true, clusterObj.getChildren(), renderCallback, affectedObjects);
        };

        //--------------------------------------------------------------------------------------------------------------

        var _onCollapseEvent = function(eventChannel, data, renderCallback) {

            var uiObj = _UIObjectState.singleton.getUIObjectByXfId(data.xfId);

            if (uiObj.getUIType() != constants.MODULE_NAMES.FILE && !xfUtil.isClusterTypeFromObject(uiObj)) {
                console.error('Collapse request for a non cluster or file object');
            }

            var affectedObjects = uiObj.getChildren();
            affectedObjects.push(uiObj);

            if (!uiObj.isExpanded()) {

                // HACK for #7077
                // Force an update of the cluster face specifically when creating a file from a cluster
                _updateClusterSpec(uiObj);
                if (_UIObjectState.showDetails) {
                    _UIObjectState.childModule.updateCardsWithCharts(uiObj.getSpecs(false));
                }

                renderCallback();
                return;
            }

            if (uiObj.hasSelectedChild()) {
                aperture.pubsub.publish(
                    chan.SELECTION_CHANGE_REQUEST,
                    {
                        xfId: null,
                        selected : true,
                        noRender: true
                    }
                );
            }

            uiObj.collapse();

            if (xfUtil.isFileCluster(uiObj)) {
                _updateClusterSpec(uiObj);
            }

            if (_UIObjectState.showDetails) {
                _collectAndShowDetails(true);
            }

            _updateLinks(true, true, [uiObj], renderCallback, affectedObjects);

            var column = _getColumnByUIObject(uiObj);
            //Draper instrumentation
            _log('user', eventChannel, 'Collapse and hide cluster membership', _loggerState.type.WF_MARSHAL,
                {
                    sessionId : _UIObjectState.sessionId,
                    clusterId : data.xfId,
                    //dataId : uiObj.getDataId(),
                    isExpanded : uiObj.isExpanded(),
                    contextId: column.getXfId()
                }
            );
        };

        //--------------------------------------------------------------------------------------------------------------
        var _selectAdjacentMatchObject = function(columnObj){
            var adjacentMatchColumn = columnObj;
            var adjacentMatchs = xfUtil.getChildrenByType(adjacentMatchColumn, constants.MODULE_NAMES.MATCH);
            var adjacentMatch = _.isEmpty(adjacentMatchs)?null:adjacentMatchs[0];

            if (adjacentMatch){
                // Find the global search button for this match card and make it visible.
                var xfId = adjacentMatch.getXfId();
                adjacentMatch.setSearchControlFocused(xfId);
                return true;
            }
            return false;
        };
        //--------------------------------------------------------------------------------------------------------------

        var _removeObject = function(objToRemove, eventChannel, removeEmptyColumn, dispose) {
            var parent = objToRemove.getParent();
            var dataId = objToRemove.getDataId();

            // Find the source column.
            var sourceColumn = _getColumnByUIObject(objToRemove);

            _log('user', eventChannel, 'Remove UI Object', _loggerState.type.WF_MARSHAL, {
                sessionId : _UIObjectState.sessionId,
                xfId : objToRemove.getXfId(),
                //dataId : dataId,
                type : objToRemove.getUIType(),
                contextId : sourceColumn.getXfId()
            });

            if (objToRemove.getUIType() == constants.MODULE_NAMES.MATCH) {
                
                // If the match card being deleted has the search focus, re-parent the
                // search focus with the closest adjacent xfMatchcard, if possible.
                if (objToRemove.isSearchControlFocused()){
                    var currentColumn = _getColumnByUIObject(objToRemove);
                    var colIndex = _getColumnIndex(currentColumn);
                    // Search RIGHT.
                    var matchFound = false;
                    var columnChildren = _UIObjectState.singleton.getChildren();
                    for (var i=colIndex+1; i < columnChildren.length; i++){
                        matchFound = _selectAdjacentMatchObject(columnChildren[i]);
                        if (matchFound){
                            break;
                        }
                    }
                    // Search LEFT.
                    if (!matchFound){
                        for (var i=colIndex-1; i >= 0; i--){
                            if (_selectAdjacentMatchObject(columnChildren[i])){
                                break;
                            }
                        }
                    }
                }
            }
            else if (parent.getUIType() === constants.MODULE_NAMES.MUTABLE_CLUSTER){
                _modifyContext(sourceColumn.getXfId(), 'remove', parent.getDataId(), [dataId]);
            }

            if (_UIObjectState.selectedUIObject != null &&
                (_UIObjectState.selectedUIObject.xfId == objToRemove.getXfId() ||
                objToRemove.getUIObjectByXfId(_UIObjectState.selectedUIObject.xfId) != null)
            ) {
                _UIObjectState.selectedUIObject = null;
            }

            // Remove this object from the position map before it's disposed.
            xfLayoutProvider.removeUIObject(objToRemove);

            objToRemove.remove(eventChannel, dispose);

            // Check if the source column is empty; if so, we can nuke it
            if (removeEmptyColumn &&
                sourceColumn != null &&
                _.size(sourceColumn.getVisualInfo().children) == 0 &&
                _.size(sourceColumn.getLinks()) == 0
            ) {
                _UIObjectState.singleton.removeChild(sourceColumn.getXfId(), true, false);
            }

            // TODO when all the cards/clusters branching off a particular node expansion have been removed, we should transform
            // TODO the state of that expansion to 'collapsed' (at present, you need to manually collapse these 'dead' branches)

            // check to see if this remove operation removed any duplicates
            if(dataId != null) {
                _updateDuplicates([dataId]);
            }
        };

        //--------------------------------------------------------------------------------------------------------------

        var _onRemoveRequest = function(eventChannel, data, renderCallback) {

            for (var i=0; i < data.xfIds.length; i++){
                _removeObject(
                    _UIObjectState.singleton.getUIObjectByXfId(data.xfIds[i]),
                    eventChannel,
                    data.removeEmptyColumn,
                    data.dispose
                );
            }

            renderCallback();
        };

        //--------------------------------------------------------------------------------------------------------------

        var _modifyContext = function(columnId, editStr, fileId, childIds, callback) {
            aperture.io.rest(
                '/modifycontext',
                'POST',
                function(response){
                    if (callback != null){
                        callback();
                    }
                },
                {
                    postData : {
                    	sessionId : _UIObjectState.sessionId,
                        contextId: columnId,
                        edit : editStr,
                        fileId : fileId,
                        childIds : childIds
                    },
                    contentType: 'application/json'
                }
            );
        };

        //--------------------------------------------------------------------------------------------------------------

        var _addFileLinks = function(fileObject, sourceColumn, linkLeft, linkRight) {
            var sourceColumnIdx = _getColumnIndex(sourceColumn);

            if (linkLeft) {
                var leftColumn = _getColumnByIndex(sourceColumnIdx-1);
                if (leftColumn) {
                    leftColumn.addFileLinksTo(fileObject, false);
                }
            }

            if (linkRight) {
                var rightColumn = _getColumnByIndex(sourceColumnIdx+1);
                if (rightColumn) {
                    rightColumn.addFileLinksTo(fileObject, true);
                }
            }

        };

        //--------------------------------------------------------------------------------------------------------------

        var _onPageSearch = function(eventChannel, data, renderCallback) {
            var matchObject = _UIObjectState.singleton.getUIObjectByXfId(data.xfId);
            if (eventChannel == chan.PREV_SEARCH_PAGE_REQUEST) {
                matchObject.pageLeft();
            } else if (eventChannel == chan.NEXT_SEARCH_PAGE_REQUEST){
                matchObject.pageRight();
            } else if (eventChannel == chan.SET_SEARCH_PAGE_REQUEST) {
                matchObject.setPage(data.page);
                return;
            }
            var syncUpdateCallback = function(){
                renderCallback();

                if (_UIObjectState.showDetails) {
                    _UIObjectState.childModule.updateCardsWithCharts(matchObject.getSpecs());
                }
            };

            _updateLinks(true, true, matchObject.getChildren(), syncUpdateCallback, matchObject.getChildren());
        };

        //--------------------------------------------------------------------------------------------------------------

        var _onCleanColumnRequest = function(eventChannel, data, renderCallback) {
            if (eventChannel != chan.CLEAN_COLUMN_REQUEST) {
                return;
            }

            var columnUIObject = _UIObjectState.singleton.getUIObjectByXfId(data.xfId);

            var contextId = columnUIObject.getXfId();
            var dataIds = columnUIObject.cleanColumn(data.removeEmptyColumn);
            if (dataIds.length > 0) {
                _modifyContext(contextId, 'remove', contextId, dataIds);
                renderCallback();
            }
        };

        //--------------------------------------------------------------------------------------------------------------

        var _onSortColumnRequest = function(eventChannel, data, renderCallback) {
            if (eventChannel != chan.SORT_COLUMN_REQUEST) {
                return;
            }

            var columnUIObject = _UIObjectState.singleton.getUIObjectByXfId(data.xfId);
            columnUIObject.sortChildren(data.sortFunction);

            renderCallback();
        };

        //--------------------------------------------------------------------------------------------------------------

        var _onCreateFileRequest = function(eventChannel, data, renderCallback) {
            var sourceObj = _UIObjectState.singleton.getUIObjectByXfId(data.xfId);
            var	columnUIObj;
            var sourceObjOriginalParent = sourceObj.getParent();
            var collapseFile = false;
            var copied = false;

            if (!data.isColumn) {
            	columnUIObj = _getColumnByUIObject(sourceObj);
            } else {
            	columnUIObj = sourceObj;
            }
            
            // Create a new file uiObject.            
            var fileSpec = xfFile.getSpecTemplate();
            var fileUIObj = xfFile.createInstance(fileSpec);
            fileUIObj.showDetails(_UIObjectState.singleton.showDetails());
            // Add the file to the very top of the column.
            var topObj = null;
            if (columnUIObj.getVisualInfo().children.length > 0){
                topObj = columnUIObj.getVisualInfo().children[0];
            }
            columnUIObj.insert(fileUIObj, topObj);

            // Link it to all files to the columns to the left and right of our column
            _addFileLinks(fileUIObj, columnUIObj, true, true);

            var linkCallback = function() {
                if(sourceObjOriginalParent != null && sourceObjOriginalParent.getChildren().length == 0) {
                    aperture.pubsub.publish(
                        chan.REMOVE_REQUEST,
                        {
                            xfIds : [sourceObjOriginalParent.getXfId()],
                            removeEmptyColumn : true,
                            dispose : true
                        }
                    );
                }

                var linkingObjects = [];
                if (fileUIObj.getClusterUIObject().isExpanded()) {
                    linkingObjects = fileUIObj.getClusterUIObject().getChildren();

                } else {
                    linkingObjects.push(fileUIObj.getClusterUIObject());
                }

                var affectedObjects = fileUIObj.getClusterUIObject().getChildren();
                affectedObjects.push(fileUIObj.getClusterUIObject());
                if (copied) {
                    affectedObjects.push(sourceObj);
                }

                _updateLinks(true, true, linkingObjects, renderCallback, affectedObjects);
            };

            var showMatchcardCallback = function() {
                if(data.showMatchCard) {
                    _onShowMatchRequest(
                        chan.SHOW_MATCH_REQUEST,
                        {
                            xfId : fileUIObj.getXfId(),
                            startSearch : true
                        },
                        linkCallback
                    );
                } else {
                    linkCallback();
                }
            };

            var modifyContextCallback = function(){
                _modifyContext(columnUIObj.getXfId(), 'insert', fileUIObj.getClusterUIObject().getDataId(),
                    fileUIObj.getClusterUIObject().getContainedCardDataIds(), showMatchcardCallback);
            };

            var collapseCallback = function() {
                if (collapseFile) {
                    _onCollapseEvent(chan.COLLAPSE_EVENT, {xfId : fileUIObj.getClusterUIObject().getXfId()}, modifyContextCallback);
                } else {
                    if (fileUIObj.getClusterUIObject()){
                        _onExpandEvent(chan.EXPAND_EVENT, {xfId : fileUIObj.getClusterUIObject().getXfId()}, modifyContextCallback);
                    }
                    else {
                        _pruneColumns();
                        renderCallback();
                    }
                }
            };
            _log(
                'user',
                eventChannel,
                'Create new file',
                _loggerState.type.WF_SEARCH,
                {
                    sessionId : _UIObjectState.sessionId,
                    xfId : fileUIObj.getXfId(),
                    contextId : columnUIObj.getXfId()
                }
            );
            if (!data.isColumn) {
                var insertedCard;
                if (_useCopyMethod(sourceObj)) {
                    copied = true;
                    insertedCard = sourceObj.clone();
                } else {
                    insertedCard = sourceObj;
                    _removeObject(
                        insertedCard,
                        chan.REMOVE_REQUEST,
                        true,
                        false
                    );
                }

                if (xfUtil.isClusterTypeFromObject(insertedCard)){
                    collapseFile = true;
                    _flattenCluster(insertedCard, columnUIObj.getXfId(), fileUIObj, collapseCallback);
                    return;
                }
                else {
                    // Add the source uiObject into the file.
                    collapseFile = false;
                    fileUIObj.insert(insertedCard, null);
                    insertedCard.showToolbar(true);
                }
            }

            collapseCallback();
        };

        //--------------------------------------------------------------------------------------------------------------

        var _onShowMatchRequest = function(eventChannel, data, renderCallback){
            var fileObj = _UIObjectState.singleton.getUIObjectByXfId(data.xfId);
            fileObj.showSearchControl(true, '');       // show with default
            aperture.pubsub.publish(
                chan.SEARCH_CONTROL_FOCUS_CHANGE_REQUEST,
                {
                    xfId: fileObj.getMatchUIObject().getXfId(),
                    noRender: true
                }
            );

            if (data.startSearch) {
                aperture.pubsub.publish(
                    chan.ADVANCE_SEARCH_DIALOG_REQUEST,
                    {
                        fileId : fileObj.getXfId(),
                        terms : null,
                        dataIds : fileObj.getContainedCardDataIds()
                    }
                );
            }

            renderCallback();
        };

        //--------------------------------------------------------------------------------------------------------------

        var _onSearchBoxChanged = function(eventChannel, data, renderCallback) {
            var xfId = data.xfId;
            var newVal = data.val;
            var matchCard = _UIObjectState.singleton.getUIObjectByXfId(xfId);
            if (matchCard) {
                matchCard.setSearchTerm(newVal);
            }

            renderCallback();
        };


        //--------------------------------------------------------------------------------------------------------------

        var _setSearchControlFocused = function(eventChannel, data, renderCallback) {
            for (var i = 0; i < _UIObjectState.children.length; i++) {
                _UIObjectState.children[i].setSearchControlFocused(data.xfId);
            }

            renderCallback();
        };

        //--------------------------------------------------------------------------------------------------------------        

        var _updateDuplicates = function(dataIds) {

            for (var i = 0; i < dataIds.length; i++) {

                var objectsFromDataId = _UIObjectState.singleton.getUIObjectsByDataId(dataIds[i]);
                if (objectsFromDataId.length > 0) {
                    for (var j = 0; j < objectsFromDataId.length; j++) {
                        var currentObject = objectsFromDataId[j];
                        currentObject.setDuplicateCount(objectsFromDataId.length);
                    }
                }
            }
        };

        //--------------------------------------------------------------------------------------------------------------

        var _saveState = function(callback, async) {

            var currentState = _UIObjectState.singleton.saveState();

            if (async == null || async == undefined || async !== false) {
                async = true;
            }

            aperture.io.rest(
                '/persist',
                'POST',
                function (response) {
                    if (callback != null) {
                        callback(response.persistenceState);
                    }
                },
                {
                    postData : {
                    	sessionId : _UIObjectState.sessionId,
                        queryId: (new Date()).getTime(),
                        data : (currentState) ? JSON.stringify(currentState) : ""
                    },
                    contentType: 'application/json',
                    async : async
                }
            );
        };

        //--------------------------------------------------------------------------------------------------------------

        var _onExportCapture = function(eventChannel) {

            if (eventChannel != chan.EXPORT_CAPTURED_IMAGE_REQUEST) {
                return;
            }

            var callback = function(response) {

                var exportCallback = function() {
                    $.blockUI({
                        theme: true,
                        title: 'Capture In Progress',
                        message: '<img src="img/ajax-loader.gif" style="display:block;margin-left:auto;margin-right:auto"/>'
                    });

                    var timestamp = (new Date().getTime());

                    var dimensions = xfRenderer.getCaptureDimensions();

                    var settings = {
                        format : 'PNG',
                        captureWidth : dimensions.width,
                        captureHeight : dimensions.height,
                        renderDelay : 4000,
                        reload : true,
                        downloadToken : timestamp
                    };


                    var sessionUrl = window.location.href;

                    if (sessionUrl.indexOf('?sessionId=') == -1) {
                        sessionUrl += '?sessionId=' + _UIObjectState.sessionId;
                    }

                    aperture.capture.store(
                        sessionUrl + '&capture=true',
                        settings,
                        null,
                        function(response){
                            var a = document.createElement('a');
                            a.href = response;

                            a.download = 'capture_' +
                                _UIObjectState.sessionId +
                                '_' +
                                timestamp +
                                '.png';
                            document.body.appendChild(a);

                            setTimeout(
                                function() {
                                    a.click();
                                    document.body.removeChild(a);
                                    $.unblockUI();
                                },
                                0
                            );
                        }
                    );

                     _log(
                		'user',
                		eventChannel,
                		'Export graph to an image.',
                		_loggerState.type.WF_MARSHAL,
                		{
                    		sessionId : _UIObjectState.sessionId
                		}
            		);
                };

                if (response == 'NONE' || response == 'ERROR') {
                    // we could not save the state... warn the user
                    xfModalDialog.createInstance({
                        title : 'Warning',
                        contents : 'There was an issue when saving the application state to the server. The resulting capture will not represent the current state. Do you wish to continue?',
                        buttons : {
                            'Continue' : exportCallback,
                            'Cancel' : function() {}
                        }
                    });
                } else {
                    exportCallback();
                }
            };

            _saveState(callback, true);
        };

        //--------------------------------------------------------------------------------------------------------------

        var _onExportGraph = function(eventChannel) {

            if (eventChannel != chan.EXPORT_GRAPH_REQUEST) {
                return;
            }

            var callback = function(response) {

                var exportCallback = function() {
                    $.blockUI({
                        theme: true,
                        title: 'Export In Progress',
                        message: '<img src="img/ajax-loader.gif" style="display:block;margin-left:auto;margin-right:auto"/>'
                    });

                    var timestamp = (new Date().getTime());

                    var exportState = _UIObjectState.singleton.exportState();

                    aperture.io.rest(
                        '/export',
                        'POST',
                        function (response, info) {

                            var redirectURI = info.xhr && info.xhr.getResponseHeader && info.xhr.getResponseHeader("Location");

                            var a = document.createElement('a');
                            a.href = redirectURI;

                            a.download = 'export_' +
                                _UIObjectState.sessionId +
                                '_' +
                                timestamp +
                                '.xml';
                            document.body.appendChild(a);

                            setTimeout(
                                function() {
                                    a.click();
                                    document.body.removeChild(a);
                                    $.unblockUI();
                                },
                                0
                            );
                        },
                        {
                            postData : {
                                sessionId : _UIObjectState.sessionId,
                                queryId: timestamp,
                                data : (exportState) ? JSON.stringify(exportState) : ""
                            },
                            contentType: 'application/json'
                        }
                    );

                    _log(
                		'user',
                		eventChannel,
                		'Export graph to as XML',
                		_loggerState.type.WF_MARSHAL,
                		{
                    		sessionId : _UIObjectState.sessionId
                		}
            		);
                };

                if (response == 'NONE' || response == 'ERROR') {
                    // we could not save the state... warn the user
                    xfModalDialog.createInstance({
                        title : 'Warning',
                        contents : 'There was an issue when saving the application state to the server. The resulting export may not represent the current state. Do you wish to continue?',
                        buttons : {
                            'Continue' : exportCallback,
                            'Cancel' : function() {}
                        }
                    });
                } else {
                    exportCallback();
                }
            };

            _saveState(callback, true);
        };

        //--------------------------------------------------------------------------------------------------------------

        var _onNewWorkspace = function(eventChannel) {
        	window.open(window.location.href, '_blank');
        };

        //--------------------------------------------------------------------------------------------------------------

        var _onHighlightSearchArguments = function(eventChannel, data) {
            if (eventChannel != chan.HIGHLIGHT_SEARCH_ARGUMENTS) {
                return;
            }
            
            var matchcardObjects = _getAllMatchCards();
            
            // if it's a pattern search, we proceed to highlight files, match cards, and file links
            if (matchcardObjects.length != 1 || _isPartialQBESearch(matchcardObjects[0].getSearchTerm())) {
            	var linkMap;
            	var link;
            	var updatedFiles = [];
                
                // go through all the file links and select them, and all files/match cards assocated with them
                linkMap = _UIObjectState.singleton.getLinks();
            	for (var linkId in linkMap) {
                    if (linkMap.hasOwnProperty(linkId)) {
                        link = linkMap[linkId];
                        if(link.getType() == xfLinkType.FILE) {
                            if(link.getSource().hasMatchCard() && link.getDestination().hasMatchCard()) {
                                link.setSelected(data.isHighlighted);			// set highlighting on link
                            }

                            if(!_.contains(updatedFiles, link.getSource().getXfId()) && link.getSource().hasMatchCard()) {		// set highlighting on source file/match
                                link.getSource().getMatchUIObject().setSearchControlHighlighted(data.isHighlighted);
                                aperture.pubsub.publish(chan.RENDER_UPDATE_REQUEST, {UIObject : link.getSource().getMatchUIObject()});

                                // MUST render file AFTER match on a separate request to maintain search button hovering
                                link.getSource().setMatchHighlighted(data.isHighlighted);
                                aperture.pubsub.publish(chan.RENDER_UPDATE_REQUEST, {UIObject : link.getSource()});

                                updatedFiles.push(link.getSource().getXfId());
                            }

                            if(!_.contains(updatedFiles, link.getDestination().getXfId()) && link.getDestination().hasMatchCard()) {	// set highlighting on dest file/match
                                link.getDestination().getMatchUIObject().setSearchControlHighlighted(data.isHighlighted);
                                aperture.pubsub.publish(chan.RENDER_UPDATE_REQUEST, {UIObject : link.getDestination().getMatchUIObject()});

                                // MUST render file AFTER match on a separate request to maintain search button hovering
                                link.getDestination().setMatchHighlighted(data.isHighlighted);
                                aperture.pubsub.publish(chan.RENDER_UPDATE_REQUEST, {UIObject : link.getDestination()});

                                updatedFiles.push(link.getDestination().getXfId());
                            }
                        }
                    }
            	}

                var layoutUpdateRequestMsg = _getLayoutUpdateRequestMsg();		// rerender the sankeys
                aperture.pubsub.publish(
                    chan.UPDATE_SANKEY_EVENT,
                    {
                        workspace : layoutUpdateRequestMsg.layoutInfo.workspace,
                        layoutProvider : layoutUpdateRequestMsg.layoutProvider
                    }
                );
            }
        };

        //--------------------------------------------------------------------------------------------------------------

        var _onCurrentStateRequest = function(eventChannel) {
            if (eventChannel != chan.REQUEST_CURRENT_STATE) {
                return;
            }

            var currentState = {
                sessionId : _UIObjectState.sessionId,
                focusData : _UIObjectState.focus,
                dates : _UIObjectState.dates
            };

            if (_UIObjectState.selectedUIObject != null) {
                currentState.selectedEntity = {
                    entityType : _UIObjectState.selectedUIObject.uiType,
                    contextId : _UIObjectState.selectedUIObject.contextId,
                    dataId : _UIObjectState.selectedUIObject.dataId,
                    label : _UIObjectState.selectedUIObject.label,
                    count : _UIObjectState.selectedUIObject.count,
                    xfId : _UIObjectState.selectedUIObject.xfId
                };
            } else {
                currentState.selectedEntity = null;
            }

            aperture.pubsub.publish(
                chan.CURRENT_STATE,
                currentState
            );
        };

        //--------------------------------------------------------------------------------------------------------------

        var _onObjectRequest = function(eventChannel, data) {

            if (eventChannel != chan.REQUEST_OBJECT_BY_XFID) {
                return;
            }

            var actualObject = _UIObjectState.singleton.getUIObjectByXfId(data.xfId);

            if (actualObject != null) {
                var clonedObject = {};
                $.extend(true, clonedObject, actualObject);

                aperture.pubsub.publish(
                    chan.OBJECT_FROM_XFID,
                    {clonedObject: clonedObject}
                );
            }
        };
        
        //--------------------------------------------------------
        // Workspace Column Management Methods
        //--------------------------------------------------------

        // Returns the column that the UIObject belongs to.
        var _getColumnByIndex = function(index) {
            if (0 <= _UIObjectState.children.length && index < _UIObjectState.children.length) {
                return _UIObjectState.children[index];
            } else {
                return null;
            }
        };

        //--------------------------------------------------------------------------------------------------------------

        var _getColumnByUIObject = function(uiObject){
            var columnObj = xfUtil.getUITypeAncestor(uiObject, constants.MODULE_NAMES.COLUMN);
            if (columnObj == null){
                console.error('The UIObjectId: ' + uiObject.getXfId() + ' does not have a valid parent column.');
            }
            return columnObj;
        };

        //--------------------------------------------------------------------------------------------------------------

        // Returns the index of the given column if it
        // exists; otherwise returns -1.
        var _getColumnIndex = function(columnObj){
            for (var i=0; i < _UIObjectState.children.length; i++){
                var nextColumnObj = _UIObjectState.children[i];
                if (columnObj.getXfId() == nextColumnObj.getXfId()){
                    return i;
                }
            }
            return -1;
        };

        //--------------------------------------------------------------------------------------------------------------

        /**
         * Constructs the object required for triggering an update of the
         * calculated layout positions and Sankey links.
         * @returns {{layoutInfo: {type: string, workspace: {}}, layoutProvider: *}}
         * @private
         */
        var _getLayoutUpdateRequestMsg = function(){
            return xfUtil.constructLayoutRequest('update', xfWorkspaceModule, xfLayoutProvider);
        };

        //--------------------------------------------------------------------------------------------------------------

        // Returns the column to the right of the given
        // one, if it exists; otherwise returns NULL.
        var _getNextColumn = function(columnObj){
            var columnIndex = _getColumnIndex(columnObj);
            var nextColumn = _UIObjectState.children[columnIndex+1];
            return (columnIndex == -1 || nextColumn == null)? null : nextColumn;
        };

        //--------------------------------------------------------------------------------------------------------------

        // Returns the column to the left of the given
        // one, if it exists; otherwise returns NULL.
        var _getPrevColumn = function(columnObj){
            var columnIndex = _getColumnIndex(columnObj);
            var prevColumn = _UIObjectState.children[columnIndex-1];
            return (columnIndex == -1 || prevColumn == null)? null : prevColumn;
        };

        //--------------------------------------------------------------------------------------------------------------

        var _getMinMaxColumnIndex = function() {
            var minIndex = 0;
            var maxIndex = 0;
            var col = _getColumnByIndex(0);
            while (_getNextColumn(col)) {
                col = _getNextColumn(col);
                maxIndex++;
            }
            col = _getColumnByIndex(0);
            while(_getPrevColumn(col)) {
                col = _getPrevColumn(col);
                minIndex--;
            }
            return {
                min : minIndex+1,           // there are always 'empty' columns on the edges
                max : maxIndex-1
            };
        };

        //--------------------------------------------------------------------------------------------------------------

        var _updateClusterSpec = function(clusterUIObject) {
            var childObjects = clusterUIObject.getChildren();
            var childCount = childObjects.length;

            var confidenceInSrc = 0;
            var confidenceInAge = 0;

            var iconTypes = [];
            var iconLabelMap = {};
            iconTypes.getIndex = function(key) {
                for (var i = 0; i < iconTypes.length; i++) {
                    if (iconTypes[i].key.imgUrl == key) {
                        return i;
                    }
                }
                return -1;
            };

            for (var i = 0; i < childCount; i++){
                var memberSpec = {};
                $.extend(true, memberSpec, childObjects[i].getVisualInfo().spec);

                // Process icons
                for (var j = 0; j < memberSpec.icons.length; j++) {
                    var icon = _.clone(memberSpec.icons[j]);
                    var keyIdx = iconTypes.getIndex(icon.imgUrl);
                    if (keyIdx == -1) {
                        iconTypes.push({key : icon, value : 1});
                    } else {
                        iconTypes[keyIdx].value++;
                    }
                    if (iconLabelMap[icon.imgUrl] == null){
                        iconLabelMap[icon.imgUrl] = icon.title;
                    }
                }

                // Process confidence
                confidenceInAge += memberSpec.confidenceInAge;
                confidenceInSrc += memberSpec.confidenceInSrc;
            }
            confidenceInAge /= childCount;
            confidenceInSrc /= childCount;

            var sortedIconTypes = iconTypes.sort(function(a,b) {
                var diff = b.value - a.value;
                if (diff == 0) {
                    return b.key.imgUrl - a.key.imgUrl;
                } else {
                    return b.value - a.value;
                }
            });

            // Update the face spec.
            var clusterUpdateSpec = {};
            clusterUpdateSpec.confidenceInSrc = confidenceInSrc;
            clusterUpdateSpec.confidenceInAge = confidenceInAge;
            clusterUpdateSpec.count = childCount;
            // Clear out the graph url since we'll want to
            // regenerate any charts each time to account
            // for any changes to the cluster children.
            clusterUpdateSpec.graphUrl = '';

            // Clear out any existing icons.
            clusterUpdateSpec.icons = [];

            // Set the cluster title to be that of the first child's.
            clusterUpdateSpec.label = childCount>0?childObjects[0].getLabel():'';

            for (var i = 0; i < sortedIconTypes.length; i++) {     // TODO:  max icons stored somewhere on client?
                var iconCount = sortedIconTypes[i].value;
                icon = sortedIconTypes[i].key;
                icon['title'] = iconLabelMap[icon.imgUrl] + (iconCount>1?' (+' + (iconCount-1) + ')':'');
                icon['score'] = iconCount / childCount;
                clusterUpdateSpec.icons.push(icon);
            }

            clusterUIObject.showToolbar(true);
            clusterUIObject.update(clusterUpdateSpec);
            clusterUIObject.updateToolbar({
                'allowClose' : true,
                'allowFocus' : true,
                'allowSearch' : true
            });
        };

        //--------------------------------------------------------------------------------------------------------------

        var initModule = function(module){

            module.clone = function() {
                console.error('Unable to clone workspace');
            };

            //----------------------------------------------------------------------------------------------------------

            module.getXfId = function() {
                return _UIObjectState.xfId;
            };

            //----------------------------------------------------------------------------------------------------------

            module.getDataId = function() {
                return null;
            };

            //----------------------------------------------------------------------------------------------------------

            module.getUIType = function() {
                return _UIObjectState.UIType;
            };

            //----------------------------------------------------------------------------------------------------------

            module.getUIObjectByXfId = function(xfId) {
                if (xfId != null && _UIObjectState.xfId == xfId) {
                    return module;
                }

                for (var i = 0; i < _UIObjectState.children.length; i++) {
                    var object = _UIObjectState.children[i].getUIObjectByXfId(xfId);
                    if (object != null) {
                        return object;
                    }
                }

                return null;
            };

            //----------------------------------------------------------------------------------------------------------

            module.getUIObjectsByDataId = function(dataId) {
                var objectList = [];

                for (var i = 0; i < _UIObjectState.children.length; i++) {
                    var membersList = _UIObjectState.children[i].getUIObjectsByDataId(dataId);
                    for (var j = 0; j < membersList.length; j++) {
                        objectList.push(membersList[j]);
                    }
                }

                return objectList;
            };

            //----------------------------------------------------------------------------------------------------------

            module.getParent = function() {
                return null;
            };

            //----------------------------------------------------------------------------------------------------------

            module.getLinks = function() {

                var links = {};

                for (var i = 0; i < _UIObjectState.children.length; i++) {
                    var membersLinksMap = _UIObjectState.children[i].getLinks();
                    for (var linkId in membersLinksMap) {
                        if (membersLinksMap.hasOwnProperty(linkId)) {
                            links[linkId] = membersLinksMap[linkId];
                        }
                    }
                }

                return links;
            };

            //----------------------------------------------------------------------------------------------------------

            module.collapseLinks = function(direction) {
                console.error(MODULE_NAME + ': call to unimplemented method "collapseLinks".');
            };

            //----------------------------------------------------------------------------------------------------------

            module.remove = function() {
                // Workspaces cannot be removed, so we throw an error to indicate this.
                console.error(MODULE_NAME + ': call to unimplemented method "remove".');
            };

            //----------------------------------------------------------------------------------------------------------

            module.removeChild = function(xfId, disposeObject, preserveLinks) {

                for (var i = 0; i < _UIObjectState.children.length; i++) {
                    if (_UIObjectState.children[i].getXfId() == xfId) {

                        if (disposeObject) {
                            _UIObjectState.children[i].dispose();
                            _UIObjectState.children[i] = null;
                        }
                        _UIObjectState.children.splice(i, 1);
                        break;
                    }
                }
            };

            //----------------------------------------------------------------------------------------------------------

            module.removeAllChildren = function() {

                if (_UIObjectState.children.length == 0) {
                    return;
                }

                for (var i = 0; i < _UIObjectState.children.length; i++) {
                    _UIObjectState.children[i].dispose();
                    _UIObjectState.children[i] = null;
                }

                _UIObjectState.children.length = 0;
            };

            //----------------------------------------------------------------------------------------------------------

            module.insert = function(xfUIObj, beforeXfUIObj00) {

                if (beforeXfUIObj00 == null) {
                    _UIObjectState.children.push(xfUIObj);
                } else {
                    var inserted = false;
                    for (var i = 0; i < _UIObjectState.children.length; i++) {
                        if (_UIObjectState.children[i].getXfId() == beforeXfUIObj00.getXfId()) {
                            _UIObjectState.children.splice(i, 0, xfUIObj);
                            inserted = true;
                            break;
                        }
                    }
                    if (!inserted) {
                        _UIObjectState.children.push(xfUIObj);
                    }
                }

                xfUIObj.setParent(this);
            };

            //----------------------------------------------------------------------------------------------------------

            module.update = function(spec) {
                for (var key in spec) {
                    if ( spec.hasOwnProperty(key) ) {
                        _UIObjectState.spec[key] = spec[key];
                    }
                }
            };

            //----------------------------------------------------------------------------------------------------------

            module.replace = function(spec) {
                _UIObjectState.spec = this.getSpecTemplate();
                for (var key in spec) {
                    if ( spec.hasOwnProperty(key) ) {
                        _UIObjectState.spec[key] = spec[key];
                    }
                }
            };

            //----------------------------------------------------------------------------------------------------------

            module.showDetails = function(bShow) {
                if (bShow == null){
                    return _UIObjectState.showDetails;
                }
                for (var i = 0; i < _UIObjectState.children.length; i++) {
                    _UIObjectState.children[i].showDetails(bShow);
                }
            };

            //--------------------------------------------------------------------------------------------------------------

            module.getChildren = function() {
                return _.clone(_UIObjectState.children);
            };

            //----------------------------------------------------------------------------------------------------------

            module.getSpecs = function(bOnlyEmptySpecs) {

                var specs = [];

                for (var i = 0; i < _UIObjectState.children.length; i++) {
                    var childSpecs = _UIObjectState.children[i].getSpecs(bOnlyEmptySpecs);
                    for (var j = 0; j < childSpecs.length; j++) {
                        specs.push(childSpecs[j]);
                    }
                }

                return specs;
            };

            //----------------------------------------------------------------------------------------------------------

            module.getVisualInfo = function() {
                return _.clone(_UIObjectState);
            };

            //----------------------------------------------------------------------------------------------------------

            module.getFocus = function(){
            	var focus = _UIObjectState.focus;
            	
            	if (focus) {
	                var focusUIObj = this.getUIObjectByXfId(focus.xfId);
	
			        // update context, etc
			        if (focusUIObj != null) {
			            var columnObj = xfUtil.getUITypeAncestor(focusUIObj, constants.MODULE_NAMES.COLUMN);
			            if (columnObj != null) {
			            	focus.contextId = columnObj.getXfId();
			            }
			        }
            	}
            	
                return focus;
            };

            //----------------------------------------------------------------------------------------------------------

            module.setFocus = function(data) {
                _UIObjectState.focus = _.clone(data);

                for (var i = 0; i < _UIObjectState.children.length; i++) {
                    _UIObjectState.children[i].highlightId(data.xfId);
                }
            };
            //----------------------------------------------------------------------------------------------------------

            module.isSelected = function() {
                // Workspaces cannot be selected, so we throw an error to indicate this.
                console.error(MODULE_NAME + ': call to unimplemented method "isSelected".');
            };

            //----------------------------------------------------------------------------------------------------------

            module.setSelection = function(xfId) {
                for (var i = 0; i < _UIObjectState.children.length; i++) {
                    _UIObjectState.children[i].setSelection(xfId);
                }
            };

            //----------------------------------------------------------------------------------------------------------

            module.setHovering = function(xfId) {
                for (var i = 0; i < _UIObjectState.children.length; i++) {
                    _UIObjectState.children[i].setHovering(xfId);
                }
            };

            //----------------------------------------------------------------------------------------------------------

            module.expand = function() {
                // Workspace objects cannot be expanded, so we throw an error to indicate this.
                console.error(MODULE_NAME + ': call to unimplemented method "expand".');
            };

            //----------------------------------------------------------------------------------------------------------

            module.collapse = function() {
                // Workspace objects cannot be collapsed, so we throw an error to indicate this.
                console.error(MODULE_NAME + ': call to unimplemented method "collapse".');
            };

            //----------------------------------------------------------------------------------------------------------

            module.setDuplicateCount = function(count) {
                // do nothing
            };

            //----------------------------------------------------------------------------------------------------------

            module.getVisibleDataIds = function() {

                var containedDataIds = [];

                for (var i = 0; i < _UIObjectState.children.length; i++) {

                    var childDataId =  _UIObjectState.children[i].getVisibleDataIds();
                    for (var j = 0; j < childDataId.length; j++) {
                        containedDataIds.push(childDataId[j]);
                    }
                }

                return containedDataIds;
            };

            //----------------------------------------------------------------------------------------------------------

            module.cleanState = function() {
                _UIObjectState.xfId = '';
                _UIObjectState.UIType = MODULE_NAME;
                _UIObjectState.children = [];
                _UIObjectState.focus = undefined;
                _UIObjectState.showDetails = false;
                _UIObjectState.dates = {startDate: '', endDate: '', numBuckets: 0, duration: ''};
            };

            //----------------------------------------------------------------------------------------------------------

            module.exportState = function() {

                var state = {};

                state['columns'] = [];
                for (var i = 1; i < _UIObjectState.children.length - 1; i++) {
                    state['columns'].push(_UIObjectState.children[i].exportState());
                }

                return state;
            };

            //----------------------------------------------------------------------------------------------------------

            module.saveState = function() {

                // clone the whole state
                var state = _.clone(_UIObjectState);

                // delete any state that we do not want cloned
                delete state['sessionId'];
                delete state['childModule'];
                delete state['singleton'];
                delete state['subscriberTokens'];

                // add the spec
                state['spec'] = {};
                $.extend(true, state['spec'], _UIObjectState.spec);

                // add the selected UI object if not null
                state['selectedUIObject'] = null;
                if (_UIObjectState.selectedUIObject != null) {
                    state['selectedUIObject'] = _UIObjectState.selectedUIObject;
                }

                // add children
                state['children'] = [];
                for (var i = 0; i < _UIObjectState.children.length; i++) {
                    state['children'].push(_UIObjectState.children[i].saveState());
                }

                return state;
            };

            //----------------------------------------------------------------------------------------------------------

            module.restoreVisualState = function(state) {

                _UIObjectState.xfId = state.xfId;
                _UIObjectState.UIType = state.UIType;
                _UIObjectState.focus = state.focus;
                _UIObjectState.showDetails = state.showDetails;
                _UIObjectState.footerDisplay = state.footerDisplay;

                _UIObjectState.dates.startDate = new Date(state.dates.startDate);
                _UIObjectState.dates.endDate = new Date(state.dates.endDate);
                _UIObjectState.dates.numBuckets = state.dates.numBuckets;
                _UIObjectState.dates.duration = state.dates.duration;

                _UIObjectState.selectedUIObject = state.selectedUIObject;

                _UIObjectState.children = [];
                for (var i = 0; i < state.children.length; i++) {
                    if (state.children[i].UIType == constants.MODULE_NAMES.COLUMN) {
                        var columnSpec = xfColumn.getSpecTemplate();
                        var columnUIObj = _createColumn(columnSpec);
                        columnUIObj.cleanState();
                        columnUIObj.restoreVisualState(state.children[i]);
                        this.insert(columnUIObj, null);
                    } else {
                        console.error("workspace children should only be of type " + constants.MODULE_NAMES.COLUMN + ".");
                    }
                }
            };

            //----------------------------------------------------------------------------------------------------------

            module.restoreHierarchy = function(state, workspace) {

                for (var i = 0; i < state.children.length; i++) {

                    var childState = state.children[i];
                    var childObject = workspace.getUIObjectByXfId(childState.xfId);
                    childObject.restoreHierarchy(childState, workspace);
                }
            };

            //----------------------------------------------------------------------------------------------------------

            module.dispose = function() {

                for (var i = 0; i < _UIObjectState.children.length; i++) {
                    _UIObjectState.children[i].dispose();
                    _UIObjectState.children[i] = null;
                }
                _UIObjectState.children = null;
            };

            //----------------------------------------------------------------------------------------------------------

            return module;
        };

        //--------------------------------------------------------------------------------------------------------------
        // Public
        //--------------------------------------------------------------------------------------------------------------

        xfWorkspaceModule.createSingleton = function(spec){

            //---------------
            // Initialization
            //---------------

            // set the xfId
            _UIObjectState.xfId = guid.generateGuid();

            // populate UI object state spec with passed in spec
            _UIObjectState.spec = this.getSpecTemplate();
            for (var key in spec) {
                if ( spec.hasOwnProperty(key) ) {
                    _UIObjectState.spec[key] = spec[key];
                }
            }

            // create new object instance
            xfUIObject.implementedBy(this, MODULE_NAME);


            xfUIObject.getLabel = function() {
                return 'XF Workspace';
            };

            initModule(this);

            return this;
        };

        //--------------------------------------------------------------------------------------------------------------

        xfWorkspaceModule.getChildren = function() {
            return _.clone(_UIObjectState.children);
        };

        //--------------------------------------------------------------------------------------------------------------

        xfWorkspaceModule.getColumnIndex = function(columnObject) {
            return _getColumnIndex(columnObject);
        };

        //--------------------------------------------------------------------------------------------------------------

        xfWorkspaceModule.getColumnByUIObject = function(uiObject) {
            return _getColumnByUIObject(uiObject);
        };
        //--------------------------------------------------------------------------------------------------------------

        xfWorkspaceModule.getPrevColumn = function(columnObject) {
            return _getPrevColumn(columnObject);
        };
        //--------------------------------------------------------------------------------------------------------------

        xfWorkspaceModule.getNextColumn = function(columnObject) {
            return _getNextColumn(columnObject);
        };
        //--------------------------------------------------------------------------------------------------------------

        xfWorkspaceModule.getSpecTemplate = function() {

            var template = {};
            _.extend(template, xfWorkspaceSpecTemplate);

            return template;
        };

        //--------------------------------------------------------------------------------------------------------------

        xfWorkspaceModule.getModuleName = function() {
            return MODULE_NAME;
        };

        //-----------------------------------
        // Methods specific to xfWorkspace
        //-----------------------------------
        
        xfWorkspaceModule.getSessionId = function() {
            return _UIObjectState.sessionId;
        };

        //--------------------------------------------------------------------------------------------------------------

        xfWorkspaceModule.registerChildModule = function(childModule) {
            _UIObjectState.childModule = childModule;
        };

        //--------------------------------------------------------------------------------------------------------------

        xfWorkspaceModule.getEntityCount = function(dataElement) {
            return (dataElement.entitytype == 'entity') ? 1 : dataElement['properties'].count.value;
        };

        //--------------------------------------------------------------------------------------------------------------

        xfWorkspaceModule.getValueByTag = function(dataElement, tag) {
            for (var propKey in dataElement.properties )
                if ( dataElement.properties.hasOwnProperty(propKey) ) {
                    var property = dataElement.properties[propKey];
                    if ( property.tags ) {
                        for (var j = 0; j < property.tags.length; j++ ) {
                            if ( property.tags[j] == tag ) {
                                return property.value; // handles singleton values only
                            }
                        }
                    }
                }
            return null;
        };

        //--------------------------------------------------------------------------------------------------------------

        xfWorkspaceModule.getPropertiesByTag = function(dataElement, tag) {
            var ret = [];
            for (var propKey in dataElement.properties )
                if ( dataElement.properties.hasOwnProperty(propKey) ) {
                    var property = dataElement.properties[propKey];
                    if ( property.tags ) {
                        for (var j = 0; j < property.tags.length; j++ ) {
                            if ( property.tags[j] == tag ) {
                                ret.push(property);
                            }
                        }
                    }
                }
            return ret;
        };
       
        //--------------------------------------------------------------------------------------------------------------

        xfWorkspaceModule.getFilterDates = function(){
            return _.clone(_UIObjectState.dates);
        };

        //--------------------------------------------------------------------------------------------------------------

        xfWorkspaceModule.allowHover = function(bAllowHover) {
            // Workspace doesn't have any hover behaviour, so just pass this on
            for (var i = 0; i < _UIObjectState.children.length; i++) {
                _UIObjectState.children[i].allowHover(bAllowHover);
            }
        };

        //--------------------------------------------------------------------------------------------------------------
        
        xfWorkspaceModule.start = function(sessionId) {

            _UIObjectState.sessionId = sessionId;

            var subTokens = {};

            // Subscribe to state changes of interest
            subTokens[chan.ALL_MODULES_STARTED] = aperture.pubsub.subscribe(chan.ALL_MODULES_STARTED, _initializeModule);

            subTokens[chan.LOGOUT_REQUEST] = aperture.pubsub.subscribe(chan.LOGOUT_REQUEST, _pubsubHandler);
            subTokens[chan.SEARCH_REQUEST] = aperture.pubsub.subscribe(chan.SEARCH_REQUEST, _pubsubHandler);
            subTokens[chan.DETAILS_CHANGE_REQUEST] = aperture.pubsub.subscribe(chan.DETAILS_CHANGE_REQUEST, _pubsubHandler);
            subTokens[chan.FILTER_CHANGE_REQUEST] = aperture.pubsub.subscribe(chan.FILTER_CHANGE_REQUEST, _pubsubHandler);
            subTokens[chan.DROP_EVENT] = aperture.pubsub.subscribe(chan.DROP_EVENT, _pubsubHandler);
            subTokens[chan.FOCUS_CHANGE_REQUEST] = aperture.pubsub.subscribe(chan.FOCUS_CHANGE_REQUEST, _pubsubHandler);
            subTokens[chan.FOCUS_CHANGE_EVENT] = aperture.pubsub.subscribe(chan.FOCUS_CHANGE_EVENT, _pubsubHandler);
            subTokens[chan.SELECTION_CHANGE_REQUEST] = aperture.pubsub.subscribe(chan.SELECTION_CHANGE_REQUEST, _pubsubHandler);
            subTokens[chan.HOVER_CHANGE_REQUEST] = aperture.pubsub.subscribe(chan.HOVER_CHANGE_REQUEST, _pubsubHandler);
            subTokens[chan.CHANGE_FILE_TITLE] = aperture.pubsub.subscribe(chan.CHANGE_FILE_TITLE, _pubsubHandler);
            subTokens[chan.SEARCH_CONTROL_FOCUS_CHANGE_REQUEST] = aperture.pubsub.subscribe(chan.SEARCH_CONTROL_FOCUS_CHANGE_REQUEST, _pubsubHandler);
            subTokens[chan.SEARCH_BOX_CHANGED] = aperture.pubsub.subscribe(chan.SEARCH_BOX_CHANGED, _pubsubHandler);
            subTokens[chan.APPLY_PATTERN_SEARCH_TERM] = aperture.pubsub.subscribe(chan.APPLY_PATTERN_SEARCH_TERM, _pubsubHandler);
            subTokens[chan.BRANCH_LEFT_EVENT] = aperture.pubsub.subscribe(chan.BRANCH_LEFT_EVENT, _pubsubHandler);
            subTokens[chan.BRANCH_RIGHT_EVENT] = aperture.pubsub.subscribe(chan.BRANCH_RIGHT_EVENT, _pubsubHandler);
            subTokens[chan.EXPAND_EVENT] = aperture.pubsub.subscribe(chan.EXPAND_EVENT, _pubsubHandler);
            subTokens[chan.COLLAPSE_EVENT] = aperture.pubsub.subscribe(chan.COLLAPSE_EVENT, _pubsubHandler);
            subTokens[chan.REMOVE_REQUEST] = aperture.pubsub.subscribe(chan.REMOVE_REQUEST, _pubsubHandler);
            subTokens[chan.CREATE_FILE_REQUEST] = aperture.pubsub.subscribe(chan.CREATE_FILE_REQUEST, _pubsubHandler);
            subTokens[chan.ADD_TO_FILE_REQUEST] = aperture.pubsub.subscribe(chan.ADD_TO_FILE_REQUEST, _pubsubHandler);
            subTokens[chan.SHOW_MATCH_REQUEST] = aperture.pubsub.subscribe(chan.SHOW_MATCH_REQUEST, _pubsubHandler);
            subTokens[chan.PREV_SEARCH_PAGE_REQUEST] = aperture.pubsub.subscribe(chan.PREV_SEARCH_PAGE_REQUEST, _pubsubHandler);
            subTokens[chan.NEXT_SEARCH_PAGE_REQUEST] = aperture.pubsub.subscribe(chan.NEXT_SEARCH_PAGE_REQUEST, _pubsubHandler);
            subTokens[chan.SET_SEARCH_PAGE_REQUEST] = aperture.pubsub.subscribe(chan.SET_SEARCH_PAGE_REQUEST, _pubsubHandler);
            subTokens[chan.CLEAN_COLUMN_REQUEST] = aperture.pubsub.subscribe(chan.CLEAN_COLUMN_REQUEST, _pubsubHandler);
            subTokens[chan.EXPORT_CAPTURED_IMAGE_REQUEST] = aperture.pubsub.subscribe(chan.EXPORT_CAPTURED_IMAGE_REQUEST, _pubsubHandler);
            subTokens[chan.EXPORT_GRAPH_REQUEST] = aperture.pubsub.subscribe(chan.EXPORT_GRAPH_REQUEST, _pubsubHandler);
            subTokens[chan.NEW_WORKSPACE_REQUEST] = aperture.pubsub.subscribe(chan.NEW_WORKSPACE_REQUEST, _pubsubHandler);
            subTokens[chan.HIGHLIGHT_SEARCH_ARGUMENTS] = aperture.pubsub.subscribe(chan.HIGHLIGHT_SEARCH_ARGUMENTS, _pubsubHandler);
            subTokens[chan.REQUEST_CURRENT_STATE] = aperture.pubsub.subscribe(chan.REQUEST_CURRENT_STATE, _pubsubHandler);
            subTokens[chan.REQUEST_OBJECT_BY_XFID] = aperture.pubsub.subscribe(chan.REQUEST_OBJECT_BY_XFID, _pubsubHandler);
            subTokens[chan.FOOTER_STATE] = aperture.pubsub.subscribe(chan.FOOTER_STATE, _pubsubHandler);
            subTokens[chan.SORT_COLUMN_REQUEST] = aperture.pubsub.subscribe(chan.SORT_COLUMN_REQUEST, _pubsubHandler);

            _UIObjectState.subscriberTokens = subTokens;
        };

        //--------------------------------------------------------------------------------------------------------------

        xfWorkspaceModule.end = function(){
            // unsubscribe to all channels
            for (var token in _UIObjectState.subscriberTokens) {
                if (_UIObjectState.subscriberTokens.hasOwnProperty(token)) {
                    aperture.pubsub.unsubscribe(_UIObjectState.subscriberTokens[token]);
                }
            }
        };

        return xfWorkspaceModule;
    }
);
