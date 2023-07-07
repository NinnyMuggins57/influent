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
		'lib/interfaces/xfUIObject', 'lib/channels', 'lib/util/GUID', 'lib/models/xfColumn', 'lib/models/xfFile',
		'lib/models/xfImmutableCluster', 'lib/models/xfMutableCluster', 'lib/models/xfSummaryCluster',
        'lib/models/xfClusterBase',	'lib/models/xfCard', 'lib/models/xfLink', 'lib/layout/xfLayoutProvider',
        'lib/util/xfUtil', 'lib/util/duration', 'lib/ui/toolbarOperations', 'lib/ui/xfLinkType', 'lib/plugins',
		'modules/xfRenderer', 'lib/ui/xfModalDialog', 'lib/constants', 'lib/extern/cookieUtil',
		'modules/xfRest'
    ],
	function(
		xfUIObject, chan, guid, xfColumn, xfFile,
		xfImmutableCluster, xfMutableCluster, xfSummaryCluster,
        xfClusterBase, xfCard, xfLink, xfLayoutProvider,
        xfUtil, duration, toolbarOp, xfLinkType, plugins,
		xfRenderer, xfModalDialog, constants, cookieUtil,
		xfRest
	) {

		//--------------------------------------------------------------------------------------------------------------
		// Private Variables
		//--------------------------------------------------------------------------------------------------------------

		var MODULE_NAME = 'xfWorkspace';

		var MAX_SEARCH_RESULTS = aperture.config.get()['influent.config']['maxSearchResults'];
		var MAX_PATTERN_SEARCH_RESULTS = aperture.config.get()['influent.config']['searchResultsPerPage'];
		var SEARCH_GROUP_BY = aperture.config.get()['influent.config']['searchGroupBy'];
		var DEFAULT_SORT_FUNCTION = xfUtil.bothDescendingSort;
		var OBJECT_DEGREE_WARNING_COUNT = aperture.config.get()['influent.config']['objectDegreeWarningCount'] || 1000;
		var OBJECT_DEGREE_LIMIT_COUNT = aperture.config.get()['influent.config']['objectDegreeLimitCount'] || 10000;

		
		var _UIObjectState = {
			xfId: '',
			UIType: MODULE_NAME,
			sessionId: '',
			children: [], // This should only ever contain xfColumn objects.
			childModule: undefined,
			focus: null,
			showDetails: true,
			footerDisplay: 'none',
			dates: {startDate: '', endDate: '', numBuckets: 0, duration: ''},
			singleton: undefined,
			selectedUIObject: null,                 // this assumes one selected object, could make this an [] if needed
			subscriberTokens: null,
			graphSearchActive: null,
			scrollStates: [],
			initialEntityId: null
		};

		var xfWorkspaceSpecTemplate = {};

		var xfWorkspaceModule = {};

		//--------------------------------------------------------------------------------------------------------------
		// Private Methods
		//--------------------------------------------------------------------------------------------------------------

		var _initializeModule = function(eventChannel) {

			if (eventChannel !== chan.ALL_MODULES_STARTED) {
				return;
			}

			$(window).bind( 'beforeunload', _onUnload);

			$(window).bind( 'unload',
				function() {
					var cookieId = aperture.config.get()['aperture.io'].restEndpoint.replace('%host%', 'sessionId');
					var cookieExpiryMinutes = aperture.config.get()['influent.config']['sessionTimeoutInMinutes'] || 24*60;
					var sessionId = _UIObjectState.sessionId;

					cookieUtil.createCookie(cookieId, sessionId, cookieExpiryMinutes);
				}
			);

			if(window.callPhantom) {
				$(window).bind( 'load',
					function() {
						if (xfRest.getPendingRequests() !== 0) {
							xfRest.addRestListener(function () {
								if(xfRest.getPendingRequests() === 0) {
									window.callPhantom();
									xfRest.removeRestListener(this);
								}
							});
						} else {
							window.callPhantom();
						}
					}
				);
			}

			// Create a workspace object.
			_initWorkspaceState(_loadWorkspaceCallback);
		};

		var _loadWorkspaceCallback = function() {

			_pruneColumns();

			if (_UIObjectState.focus == null) {
				aperture.pubsub.publish(
					chan.FOCUS_CHANGE_EVENT,
					{
						focus: null,
						noRender: true
					}
				);
			} else {
				aperture.pubsub.publish(
					chan.FOCUS_CHANGE_EVENT,
					{
						focus: {
							xfId: _UIObjectState.focus.xfId,
							dataId: _UIObjectState.focus.dataId,
							entityType: _UIObjectState.focus.entityType,
							entityLabel: _UIObjectState.focus.entityLabel,
							entityCount: _UIObjectState.focus.entityCount,
							contextId: _UIObjectState.focus.contextId,
							sessionId: _UIObjectState.singleton.getSessionId()
						},
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
		};

		//--------------------------------------------------------------------------------------------------------------

		var _initWorkspaceState = function(callback) {

			xfRest.request('/restorestate')
				.withData({sessionId : _UIObjectState.sessionId})
				.then(function (response) {
					var workspaceSpec = null;
					var workspaceUIObj = null;

					if (response.data == null || response.data.length < 1) {

						// Set the flag for displaying/hiding charts.
						var show = aperture.config.get()['influent.config']['defaultShowDetails'];

						_UIObjectState.showDetails = show != null? show : true;

						aperture.pubsub.publish(chan.DETAILS_CHANGE_EVENT, {showDetails : _UIObjectState.showDetails});

						var initDuration = aperture.config.get()['influent.config']['startingDateRange'];
						var initEndDate = aperture.config.get()['influent.config']['defaultEndDate'] || new Date();

						initEndDate = duration.roundDateByDuration(initEndDate, initDuration);
						var initStartDate = duration.roundDateByDuration(duration.subtractFromDate(initDuration, initEndDate), initDuration);
						_UIObjectState.dates = {
							startDate: xfUtil.utcShiftedDate(initStartDate),
							endDate: xfUtil.utcShiftedDate(initEndDate),
							numBuckets: 16, duration: initDuration
						};

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

						var onCardsAddedCallback = function() {
							fileUIObj.showSearchControl(
								true,
								''
							);

							aperture.pubsub.publish(
								chan.SEARCH_CONTROL_FOCUS_CHANGE_REQUEST,
								{
									xfId: fileUIObj.getMatchUIObject().getXfId(),
									noRender: true
								}
							);

							_pruneColumns();
							_checkPatternSearchState();

							// remove the entity id from the url after it has been loaded

							var newPath;
							if (!window.location.origin) {
								newPath = window.location.protocol + '//' + window.location.hostname + (window.location.port ? ':' + window.location.port: '');

							} else {
								newPath = window.location.origin;
							}
							newPath += window.location.pathname;

							window.history.replaceState(
								{},
								aperture.config.get()['influent.config']['title'],
								newPath
							);

							callback();
						};

						var onFileAddedCallback = function() {
							if (_UIObjectState.initialEntityId != null && _UIObjectState.initialEntityId.length > 0) {
								_modifyContext(
									null,
									fileUIObj.getXfId(),
									'insert',
									[_UIObjectState.initialEntityId],
									function (response) {
										_populateFileFromResponse(response, onCardsAddedCallback);
									}
								);
							} else {
								onCardsAddedCallback();
							}
						};

						_modifyContext(
							fileUIObj.getXfId(),
							columnUIObj.getXfId(),
							'create',
							null,
							onFileAddedCallback
						);

					} else {

						var restoreState = JSON.parse(response.data);

						workspaceSpec = xfWorkspaceModule.getSpecTemplate();
						workspaceUIObj = xfWorkspaceModule.createSingleton(workspaceSpec);
						_UIObjectState.singleton = workspaceUIObj;

						_UIObjectState.singleton.cleanState();
						_UIObjectState.singleton.restoreVisualState(restoreState);
						_UIObjectState.singleton.restoreHierarchy(restoreState, _UIObjectState.singleton);

						_pruneColumns();
						_checkPatternSearchState();

						callback();
					}
				}
			);
		};

		//--------------------------------------------------------------------------------------------------------------

		var _populateFileFromResponse = function(response, renderCallback) {

			var fileObj = _UIObjectState.singleton.getUIObjectByXfId(response.contextId);
			if (fileObj == null || fileObj.getUIType() !== constants.MODULE_NAMES.FILE) {
				aperture.log.error(
					'Server Error: ' +
					'Empty or invalid file object was returned from the server. Please contact your system administrator.'
				);
				return;
			}

			if (response.targets.length !== 1) {
				aperture.log.error(
					'Request Error: ' +
					'Invalid number of entities returned from server: expected 1, received ' +
					response.targets.length
				);
				if (renderCallback) {
					renderCallback();
					return;
				}
			}

			if (response.targets[0].members.length === 0 &&
				response.targets[0].subclusters.length === 0
			) {
				aperture.log.error(
					'Request Error: ' +
					'An entity with the supplied Id could not be found.'
				);
				if (renderCallback) {
					renderCallback();
					return;
				}
			}

			var specs = _getPopulatedSpecsFromData(response.targets, fileObj);

			var clusterObj = xfImmutableCluster.createInstance(specs[0]);

			clusterObj.updateToolbar(
				{
					'allowFile': false,
					'allowClose': true,
					'allowFocus': true,
					'allowSearch': true
				},
				true
			);

			clusterObj.showDetails(_UIObjectState.showDetails);
			clusterObj.showToolbar(true);

			fileObj.setClusterUIObject(clusterObj);

			aperture.pubsub.publish(chan.EXPAND_EVENT, { xfId: clusterObj.getXfId() });

			if (_UIObjectState.showDetails) {
				_UIObjectState.childModule.updateCardsWithCharts(specs);
			}

			if (renderCallback) {
				renderCallback();
			}
		};

		//--------------------------------------------------------------------------------------------------------------

        var logActivity = function(activity, eventChannel) {
            var activityData = {
                type: null,
                workflow: null,
                activity: eventChannel,
                description: null,
                data: {}
            };

            _.extend(activityData, activity);

            // If a draper workflow is defined, assume it's a draper user event
            if (activityData.workflow in aperture.log.draperWorkflow) {
                activityData.type = aperture.log.draperType.USER;
            }

            aperture.log.log(activityData);
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

            var _logActivity = function(activity) {
                logActivity(activity, eventChannel);
            };

			switch (eventChannel) {
                case chan.LOGOUT_REQUEST :
                    _logActivity({
                        workflow: aperture.log.draperWorkflow.WF_OTHER,
                        description: 'Logout user'
                    });
                    _onLogout(eventChannel);
                    break;
                case chan.SEARCH_REQUEST :
                    _onSearchRequest(eventChannel, data, renderCallback);
                    break;
                case chan.PATTERN_SEARCH_REQUEST :
                    _onPatternSearchRequest(eventChannel, data, renderCallback);
                    break;
                case chan.ADVANCE_SEARCH_DIALOG_REQUEST :
                    _logActivity({
                        workflow: aperture.log.draperWorkflow.WF_CREATE,
                        activity: 'open_modal_tools',
                        description: 'Open the advanced search modal dialog'
                    });
                    break;
                case chan.ADVANCE_SEARCH_DIALOG_CLOSE_EVENT:
                    _logActivity({
                        workflow: aperture.log.draperWorkflow.WF_CREATE,
                        activity: 'close_modal_tools',
                        description: 'Close the advanced search modal dialog without searching'
                    });
                    break;
                case chan.DETAILS_CHANGE_REQUEST :
                    _logActivity({
                        workflow: aperture.log.draperWorkflow.WF_CREATE,
                        activity: 'select_view',
                        description: (data.showDetails) ?
                            'View both account holder and activity information for all visible cards and stacks of cards' :
                            'View account holder information only for all visible cards and stacks of cards',
                        data: {
                            showDetails: data.showDetails
                        }
                    });
                    _onDetailsChangeRequest(eventChannel, data, renderCallback);
                    break;
                case chan.FILTER_CHANGE_REQUEST :
                    _logActivity({
                        workflow: aperture.log.draperWorkflow.WF_GETDATA,
                        activity: 'execute_query_filter',
                        description: 'Execute date range filter query',
                        data: {
                            startDate: data.startDate,
                            endDate: data.endDate,
                            numBuckets: data.numBuckets,
                            duration: data.duration
                        }
                    });
                    _onFilterChangeRequest(eventChannel, data, renderCallback);
                    break;
                case chan.FILTER_CHANGE_EVENT :
                    if (data.userRequested) {
                        _logActivity({
                            workflow: aperture.log.draperWorkflow.WF_GETDATA,
                            activity: 'select_filter_menu_option',
                            description: 'Change date range menu options',
                            data: {
                                startDate: data.startDate,
                                endDate: data.endDate,
                                numBuckets: data.numBuckets,
                                duration: data.duration
                            }
                        });
                    }

                    break;
                case chan.TRANSACTIONS_FILTER_EVENT :
                    _logActivity({
                        workflow: aperture.log.draperWorkflow.WF_GETDATA,
                        activity: 'select_filter_menu_option',
                        description: 'Change transaction table highlight filter',
                        data: {
                            filterHighlighted: data.filterHighlighted
                        }
                    });
                    break;
				case chan.TRANSACTIONS_PAGE_CHANGE_EVENT :
					_logActivity({
						workflow: aperture.log.draperWorkflow.WF_GETDATA,
						activity: 'select_filter_menu_option',
						description: 'Change transaction table page'
					});
					break;
                case chan.ADD_TO_FILE_REQUEST :
                case chan.DROP_EVENT :
                    _logActivity({
                        workflow: aperture.log.draperWorkflow.WF_ENRICH,
                        activity: 'add_to_workspace',
                        description: 'Add one or more account cards into a file',
                        data: {
                            UIObjectId: data.cardId,
                            UIContainerId: data.containerId,
                            fromDragDropEvent: (eventChannel === chan.DROP_EVENT) ? 'true' : 'false'
                        }
                    });
                    _onAddCardToContainer(eventChannel, data, renderCallback);
                    break;
                case chan.FOCUS_CHANGE_REQUEST :
                    _logActivity({
                        workflow: aperture.log.draperWorkflow.WF_EXPLORE,
                        activity: 'highlight_data',
                        description: 'Highlight a card or stack of cards',
                        data: {
                            UIObjectId: data.xfId
                        }
                    });
                    _onFocusChangeRequest(eventChannel, data);
                    break;
                case chan.FOCUS_CHANGE_EVENT :
                    _onFocusChangeEvent(eventChannel, data, renderCallback);
                    break;
                case chan.SELECTION_CHANGE_REQUEST :
                    if (data.xfId != null) {
                        _logActivity({
                            workflow: aperture.log.draperWorkflow.WF_EXPLORE,
                            activity: 'show_data_info',
                            description: 'Select a card or stack of cards',
                            data: {
                                UIObjectId: data.xfId
                            }
                        });
                    }
                    _onSelectionChangeRequest(eventChannel, data);
                    break;
                case chan.UI_OBJECT_HOVER_CHANGE_REQUEST :
                    _onHoverChangeRequest(eventChannel, data);
                    break;
                case chan.CHANGE_FILE_TITLE :
                    _logActivity({
                        workflow: aperture.log.draperWorkflow.WF_ENRICH,
                        description: 'Change title of a file',
                        data: {
                            fileId: data.xfId
                        }
                    });
                    _onChangeFileTitle(eventChannel, data, renderCallback);
                    break;
                case chan.SEARCH_CONTROL_FOCUS_CHANGE_REQUEST :
                    _setSearchControlFocused(eventChannel, data);
                    break;
                case chan.SEARCH_BOX_CHANGED :
                    _logActivity({
                        workflow: aperture.log.draperWorkflow.WF_GETDATA,
                        activity: 'enter_filter_text',
                        description: 'User entering text in match card search field'
                    });
                    _onSearchBoxChanged(eventChannel, data, renderCallback);
                    break;
                case chan.BRANCH_REQUEST :
                    _logActivity({
                        workflow: aperture.log.draperWorkflow.WF_GETDATA,
                        activity: 'execute_visual_filter',
                        description: 'Branch on a card or stack of cards for related links',
                        data: {
                            UIObjectId: data.xfId,
                            direction: data.direction
                        }
                    });
                    _onBranchRequest(eventChannel, data, renderCallback);
                    break;
                case chan.BRANCH_RESULTS_RETURNED_EVENT :
                    _logActivity({
                        type: aperture.log.draperType.SYSTEM,
                        description: 'Branch results returned'
                    });
                    break;
                case chan.EXPAND_EVENT :
                    _logActivity({
                        workflow: aperture.log.draperWorkflow.WF_EXPLORE,
                        activity: 'expand_data',
                        description: 'Expand stack and show members',
                        data: {
                            UIObjectId: data.xfId
                        }
                    });
                    _onExpandEvent(eventChannel, data, renderCallback);
                    break;
                case chan.COLLAPSE_EVENT :
                    _logActivity({
                        workflow: aperture.log.draperWorkflow.WF_EXPLORE,
                        activity: 'collapse_data',
                        description: 'Collapse stack and hide members',
                        data: {
                            UIObjectId: data.xfId
                        }
                    });
                    _onCollapseEvent(eventChannel, data, renderCallback);
                    break;
                case chan.REMOVE_REQUEST :
                    if (data.userRequested) {
                        if (data.isMatchCard) {
                            _logActivity({
                                workflow: aperture.log.draperWorkflow.WF_CREATE,
                                activity: 'hide_tools',
                                description: 'Remove a match card from a file',
                                data: {
                                    UIObjectIds: data.xfIds
                                }
                            });
                        } else {
                            _logActivity({
                                workflow: aperture.log.draperWorkflow.WF_ENRICH,
                                activity: 'remove_from_workspace',
                                description: 'Remove a card or stack of cards from the workspace',
                                data: {
                                    UIObjectIds: data.xfIds
                                }
                            });
                        }
                    }
                    _onRemoveRequest(eventChannel, data, renderCallback);
                    break;
                case chan.CREATE_FILE_REQUEST :
                    _logActivity({
                        workflow: aperture.log.draperWorkflow.WF_ENRICH,
                        activity: 'create_workspace',
                        description: 'Create a new file',
                        data: {
                            UIObjectId: data.xfId,
                            requestedFromColumn: data.isColumn ? data.isColumn : 'false'
                        }
                    });
                    _onCreateFileRequest(eventChannel, data, renderCallback);
                    break;
                case chan.SHOW_MATCH_REQUEST :
                    _logActivity({
                        workflow: aperture.log.draperWorkflow.WF_CREATE,
                        activity: 'open_modal_tools',
                        description: 'Show search controls for a file',
                        data: {
                            UIObjectId: data.xfId
                        }
                    });
                    _onShowMatchRequest(eventChannel, data, renderCallback);
                    break;
                case chan.SEARCH_RESULTS_RETURNED_EVENT :
                    _logActivity({
                        type: aperture.log.draperType.SYSTEM,
                        description: 'Search results returned',
                        data: {
                            shownMatches: data.shownMatches,
                            totalMatches: data.totalMatches
                        }
                    });
                    break;
                case chan.PREV_SEARCH_PAGE_REQUEST :
                case chan.NEXT_SEARCH_PAGE_REQUEST :
                case chan.SET_SEARCH_PAGE_REQUEST :
                    if (eventChannel === chan.PREV_SEARCH_PAGE_REQUEST ||
                        eventChannel === chan.NEXT_SEARCH_PAGE_REQUEST
                        ) {
                        _logActivity({
                            workflow: aperture.log.draperWorkflow.WF_EXPLORE,
                            description: (eventChannel === chan.PREV_SEARCH_PAGE_REQUEST) ?
                                'Page to previous set of search results' :
                                'Page to next set of search results',
                            data: {
                                UIObjectId: data.xfId,
                                page: data.page
                            }
                        });
                    }
                    _onPageSearch(eventChannel, data, renderCallback);
                    break;
                case chan.CLEAN_COLUMN_REQUEST :
                    _logActivity({
                        workflow: aperture.log.draperWorkflow.WF_ENRICH,
                        activity: 'clear_workspace',
                        description: 'Clear any unfiled cards or stacks of cards from a column',
                        data: {
                            UIObjectId: data.xfId
                        }
                    });
                    _onCleanColumnRequest(eventChannel, data, renderCallback);
                    break;
                case chan.EXPORT_CAPTURED_IMAGE_REQUEST :
                    _logActivity({
                        workflow: aperture.log.draperWorkflow.WF_OTHER,
                        activity: 'export_data',
                        description: 'Export an image capture of the workspace'
                    });
                    _onExportCapture(eventChannel);
                    break;
                case chan.EXPORT_GRAPH_REQUEST :
                    _logActivity({
                        workflow: aperture.log.draperWorkflow.WF_OTHER,
                        activity: 'export_data',
                        description: 'Export a chart of all filed content to the local computer'
                    });
                    _onExportGraph(eventChannel);
                    break;
                case chan.IMPORT_GRAPH_REQUEST :
                    _logActivity({
                        workflow: aperture.log.draperWorkflow.WF_OTHER,
                        activity: 'import_data',
                        description: 'Import a chart of all filed content from the local computer'
                    });
                    _onImportGraph(eventChannel);
                    break;
                case chan.EXPORT_TRANSACTIONS_REQUEST:
                    _logActivity({
                        workflow: aperture.log.draperWorkflow.WF_OTHER,
                        activity: 'export_data',
                        description: 'Export transactions from the entity details data table'
                    });
                    break;
                case chan.NEW_WORKSPACE_REQUEST :
                    _logActivity({
                        workflow: aperture.log.draperWorkflow.WF_CREATE,
                        activity: 'create_workspace',
                        description: 'Create new empty workspace'
                    });
                    _onNewWorkspace(eventChannel);
                    break;
                case chan.OPEN_EXTERNAL_LINK_REQUEST :
                    var isHelplink = data.link === aperture.config.get()['influent.config']['help'];
                    _logActivity({
                        workflow: aperture.log.draperWorkflow.WF_CREATE,
                        activity: (isHelplink) ?
                            'show_instructional_materials' :
                            'show_external_link',
                        description: (isHelplink) ?
                            'Opening help documentation' :
                            'Opening external link: ' + data.link
                    });

					if (data.link) {
						window.open(data.link);
					}
                    break;
                case chan.FOOTER_CHANGE_DATA_VIEW_EVENT :
                    _logActivity({
                        workflow: aperture.log.draperWorkflow.WF_CREATE,
                        activity: (data.tab === 'tableTab') ?
                            'show_table' :
                            'show_chart',
                        description: 'Moving between data view tabs in the entity details panel'
                    });
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
				case chan.UPDATE_CHART_REQUEST :
					_onUpdateCharts(eventChannel,data);
					break;
				case chan.SORT_COLUMN_REQUEST :
                    _logActivity({
                        workflow : aperture.log.draperWorkflow.WF_EXPLORE,
                        activity : 'sort',
                        description : (data.sortDescription === constants.SORT_FUNCTION.INCOMING) ?
                            'Sort all cards and stacks of cards in a column by volume of flow in' :
                            (data.sortDescription === constants.SORT_FUNCTION.OUTGOING) ?
                                'Sort all cards and stacks of cards in a column by volume of flow out' :
                                'Sort all cards and stacks of cards in a column by volume of flow in and out',
                        data : {
                            UIObjectId : data.xfId,
                            sortDescription : data.sortDescription
                        }
                    });
					_onSortColumnRequest(eventChannel, data, renderCallback);
					break;
                case chan.HOVER_START_EVENT :
                case chan.HOVER_END_EVENT :
                    _logActivity({
                        workflow : aperture.log.draperWorkflow.WF_EXPLORE,
                        description : (eventChannel === chan.HOVER_START_EVENT) ?
                            'Hovering over a UI element' :
                            'Stopped hovering over a UI element',
                        data : {
                            element : data.element
                        }
                    });
                    break;
				case chan.TOOLTIP_START_EVENT :
				case chan.TOOLTIP_END_EVENT :
					_logActivity({
						workflow: aperture.log.draperWorkflow.WF_EXPLORE,
						description: (eventChannel === chan.TOOLTIP_START_EVENT) ?
							'Displaying a tooltip' :
							'Stopped displaying a tooltip',
						data: {
							tooltip: data.element
						}
					});
					break;
				case chan.SCROLL_VIEW_EVENT :
					_onScrollView(eventChannel, data);
					break;
				default :
					break;
			}
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

			// create a counter callback so we only call our passed in call back after we have completed
			// links to both sides of the column
			var counter = 0;
			var counterCallback = function() {
				counter++;
				if (counter === numberOfCascades) {
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
			return xfColumn.createInstance(_.isEmpty(columnSpec) ? '' : columnSpec);
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

		var _isPartialQBESearch = function(data) {
			return data && (data.indexOf('like:') !== -1);
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

		var _getAllFiles = function() {
			var columnExtents = _getMinMaxColumnIndex();
			var columnFiles = [];
			var i;

			// Do a preliminary scan to see if it's basic or not
			for (i = columnExtents.min; i <= columnExtents.max; i++) {
				columnFiles = columnFiles.concat(xfUtil.getChildrenByType(_getColumnByIndex(i), constants.MODULE_NAMES.FILE));
			}

			return columnFiles;
		};

		//--------------------------------------------------------------------------------------------------------------

		function _checkPatternSearchState() {
			var active = aperture.util.forEachUntil(_getAllFiles(), function(file) {
				return file.getChildren().length !== 0;
			}, true);

			if (_UIObjectState.graphSearchActive !== active) {
				_UIObjectState.graphSearchActive = active;
				aperture.pubsub.publish(chan.GRAPH_SEARCH_STATE_CHANGE,
						{graphSearchActive : active});
			}
		}

		//--------------------------------------------------------------------------------------------------------------

		var _onSearchRequest = function(eventChannel, data, renderCallback) {

			if (eventChannel !== chan.SEARCH_REQUEST ||
				_UIObjectState.singleton == null
			) {
				return;
			}

			var matchUIObject;
			if (_isAdvancedSearch(data)) {
				matchUIObject = _UIObjectState.singleton.getUIObjectByXfId(data.xfId).getMatchUIObject();
				matchUIObject.setSearchTerm(data.searchTerm);
				matchUIObject.setSearchOperation(data.booleanOperation);
				aperture.pubsub.publish(chan.RENDER_UPDATE_REQUEST, {UIObject : matchUIObject});

				renderCallback();

				if(!data.executeSearch) {
                    // DRAPER Instrumentation for logging a basic search.
                    // Search terms omitted from log for privacy concerns.
                    logActivity({
                        workflow: aperture.log.draperWorkflow.WF_GETDATA,
                        activity: 'enter_filter_text',
                        description: _isPartialQBESearch(data.searchTerm) ?
                            'Apply terms for a pattern search' :
                            'Apply terms for an attribute-based search'
                    });
					return;
				}
			}
			else {
				matchUIObject = _UIObjectState.singleton.getUIObjectByXfId(data.xfId);
			}

			if (_isPartialQBESearch(matchUIObject.getSearchTerm())) {
				_searchByExample(_gatherPatternSearchTerm([matchUIObject]), renderCallback);

			} else {
				_basicSearchOnMatchcard(matchUIObject, renderCallback);
			}
		};

		//--------------------------------------------------------------------------------------------------------------

		var _onPatternSearchRequest = function(eventChannel, data, renderCallback) {

			if (eventChannel !== chan.PATTERN_SEARCH_REQUEST) {
				return;
			}

			// get files.
			var files = _getAllFiles();
			var fileSets = [];

			// open and populate match cards for any file with valid examples in it.
			aperture.util.forEach(files, function(file) {
				var dataIds = xfUtil.getContainedCardDataIds(file.getVisualInfo());
				if (dataIds.length > 0) {
					fileSets.push({
						contextId: file.getVisualInfo().spec.dataId,
						entities: dataIds
					});
				}
			});

			if (fileSets.length > 0) {
				var onEntityResolution = function(data) {
					var matchCardObjs = [];
					var closeTheseFiles = [];

					aperture.util.forEach(data, function(fileData) {
						var file = _UIObjectState.singleton.getUIObjectByXfId(fileData.contextId);

						if (file) {
							if (fileData.entities.length !== 0) {
								file.showSearchControl(true, 'like:'+fileData.entities.toString());
								matchCardObjs.push(file.getMatchUIObject());

							} else {
								closeTheseFiles.push(file);
							}
						}
					});

					// to do: prompt to proceed if there are any closures?

					// if nothing to query, have to exit out.
					if (matchCardObjs.length === 0) {
						window.alert('Too many model accounts to search? Try again with less.');
						renderCallback();
						return;
					}


					// close anything not involved in the search.
					aperture.util.forEach(closeTheseFiles, function(file) {
						file.showSearchControl(false);
					});

					// set involved matchcards in a search state
					aperture.util.forEach(_getAllMatchCards(), function(mcard) {
						mcard.removeAllChildren();
						mcard.setSearchState('searching');
						aperture.pubsub.publish(chan.RENDER_UPDATE_REQUEST, {UIObject : mcard});
					});


					// issue queries.


					// Create groups of pattern searches and basic searches
					var columnExtents = _getMinMaxColumnIndex();
					var columnFiles = '';

					// to do: there is only the possibility of one pattern group using this logic.
					var patternGroups = [];
					var currentPatternGroup = [];
					var i, j;

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
								columnQBE.push(columnMatchcards[j]);
							}
						}
						currentPatternGroup = currentPatternGroup.concat(columnQBE);
						if (columnQBE.length === 0 && currentPatternGroup.length > 0) {
							patternGroups.push(currentPatternGroup);
							currentPatternGroup = [];
						}
					}

					if (currentPatternGroup.length > 0) {
						patternGroups.push(currentPatternGroup);
					}

					for (i = 0; i < patternGroups.length; i++) {
						_searchByExample(_gatherPatternSearchTerm(patternGroups[i]), renderCallback);
					}
				};

				xfRest.request('/containedentities').withData({

						sessionId : _UIObjectState.sessionId,
						entitySets : fileSets,
						details : false

				}).then(function (response) {

						onEntityResolution(response.data);
				});
			}
		};

		//--------------------------------------------------------------------------------------------------------------

		var _basicSearchOnMatchcard = function(matchUIObject, renderCallback) {
			var contextObj = xfUtil.getContextByUIObject(matchUIObject);

			matchUIObject.removeAllChildren();
			matchUIObject.setSearchState('searching');

			aperture.pubsub.publish(chan.RENDER_UPDATE_REQUEST, {UIObject : matchUIObject});

			xfRest.request( '/search' ).inContext( contextObj.getXfId() ).withData({

				sessionId : _UIObjectState.sessionId,
				term : matchUIObject.getSearchTerm(),
				operation : matchUIObject.getSearchOperation(),
				cluster: false,                         // Feature #6467 - searches no longer clustered
				limit : MAX_SEARCH_RESULTS,
				contextId : contextObj.getXfId(),
				groupby : SEARCH_GROUP_BY

			}).then( function( response, restInfo ) {

				if (!restInfo.success) {
					response = {
						data: [],
						scores: {},
						sessionId: '',
						totalResults: 0
					};
				}

				// Remove children again in case a response arrived before this one.
				if (matchUIObject.getVisualInfo() != null) {
					matchUIObject.removeAllChildren();

					var newUIObjects = _populateMatchResults(matchUIObject, response, undefined, renderCallback);
					_updateLinks(true, true, newUIObjects, renderCallback, newUIObjects);
				}
			});


			// DRAPER Instrumentation for logging a basic search.
			// Search terms omitted from log for privacy concerns.
			logActivity({
                workflow: aperture.log.draperWorkflow.WF_GETDATA,
                activity: 'execute_query_filter',
                description: 'Execute an attribute-based search for accounts'
			});
		};

		//--------------------------------------------------------------------------------------------------------------
		function parseDataIds(searchTerm, splitString) {
			var termPieces = searchTerm.split(splitString);
			var dataIds = termPieces[1].split(',');
			for (var j = 0; j < dataIds.length; j++) {
				dataIds[j] = dataIds[j].split('\'').join(' ').trim();
			}
			return dataIds;
		}

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

				var matchCardDataIds = null;
				if (matchCard.getSearchTerm().indexOf('like:') !== -1) {
					matchCardDataIds = parseDataIds(matchCard.getSearchTerm(), 'like:');
				} else {
					aperture.log.error('Unable to determine QuBE search type from search term: ' + matchCard.getSearchTerm());
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
					if (link.getType() === xfLinkType.FILE) {
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

		var _searchByExample = function(term, renderCallback) {

			xfRest.request('/patternsearch').withData({
				sessionId : _UIObjectState.sessionId,
				term: term,
				startDate : _UIObjectState.dates.startDate,
				endDate :  _UIObjectState.dates.endDate,
				cluster: false,
				limit : MAX_PATTERN_SEARCH_RESULTS,
				useAptima : (_getAllMatchCards().length > 3)
			}).then(function(response, restInfo) {
				var parsedResponse = aperture.util.viewOf(response);

				if (!restInfo.success) {
					_populateEmptyResults(_getAllMatchCards());
					renderCallback();

					return;
				}

				var newUIObjects = _populatePatternResults(parsedResponse, renderCallback);

				var counter = 0;
				var totalDataIds = [];
				var callback = function (dataIds) {
					counter++;
					totalDataIds = totalDataIds.concat(dataIds);
					if (counter === newUIObjects.length) {
						renderCallback(totalDataIds);
					}
				};

				for (var i = 0; i < newUIObjects.length; i++) {
					_updateLinks(true, true, newUIObjects[i], callback, newUIObjects[i]);
				}
			});

			// DRAPER Instrumentation for logging a pattern search.
			// Search terms omitted from log for privacy concerns.
            logActivity({
                workflow: aperture.log.draperWorkflow.WF_TRANSFORM,
                activity: 'execute-pattern-search',
                description: 'Execute query by example to find accounts with a similar pattern of activity',
                data: {
                    startDate: _UIObjectState.dates.startDate,
                    endDate: _UIObjectState.dates.endDate
                }
            });
		};

		//--------------------------------------------------------------------------------------------------------------

		var _populateEmptyResults = function(matchcardObjects) {

			for (var i = 0; i < matchcardObjects.length; i++) {

				var matchcard = matchcardObjects[i];

				if (matchcard.getUIType() !== constants.MODULE_NAMES.MATCH) {
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
					if (fileId === workspaceFiles[j].getXfId() && workspaceFiles[j].hasMatchCard()) {

						// Remove children in case a response arrived before this one.
						workspaceFiles[j].getMatchUIObject().removeAllChildren();

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

			var entities = serverResponse.data;
			var firstSearchObject = null;
			var specs = _getPopulatedSpecsFromData(entities, parent);
			var dataIds = [];

			var shownMatches = 0;
            var totalMatches = 0;

			for (var i = 0; i < specs.length; i++) {

				var UIObject = {};

				if (specs[i].accounttype === constants.ACCOUNT_TYPES.ENTITY) {
					UIObject = xfCard.createInstance(specs[i]);
				}
				else if (specs[i].accounttype === constants.ACCOUNT_TYPES.CLUSTER_SUMMARY) {
					UIObject = xfSummaryCluster.createInstance(specs[i]);
				}
				else if (specs[i].accounttype === constants.ACCOUNT_TYPES.ACCOUNT_OWNER) {
					UIObject = xfImmutableCluster.createInstance(specs[i]);
				}
				else if (specs[i].accounttype === constants.ACCOUNT_TYPES.CLUSTER) {
					UIObject = xfMutableCluster.createInstance(specs[i]);
				} else {
					aperture.log.error('Failed to determine UI object from account type');
				}

				shownMatches = shownMatches+1;

				UIObject.showDetails(_UIObjectState.showDetails);
				// determine what the first search object is
				if (i === 0) {
					firstSearchObject = UIObject;
				}

				parent.insert(UIObject, null);

				var dataId = UIObject.getDataId();
				if (dataId != null) {
					dataIds.push(dataId);
				}
				createdUIObjects.push(UIObject);
			}

            totalMatches = serverResponse.totalResults != null ? serverResponse.totalResults : shownMatches;

			//Set the total number of matches, to display
			if (parent.setTotalMatches) {
				parent.setTotalMatches(totalMatches);
			}
			if (parent.setShownMatches) {
				parent.setShownMatches(shownMatches);
			}

            aperture.pubsub.publish(chan.SEARCH_RESULTS_RETURNED_EVENT, {
                shownMatches : shownMatches,
                totalMatches : totalMatches
            });

			if (_UIObjectState.showDetails) {
				_UIObjectState.childModule.updateCardsWithCharts(specs);
			}

			renderCallback(dataIds);

			if (parent.getUIType() === constants.MODULE_NAMES.MATCH) {
				parent.setSearchState('results');
				aperture.pubsub.publish(chan.RENDER_UPDATE_REQUEST, {UIObject : parent});
			}

			return createdUIObjects;
		};

		//--------------------------------------------------------------------------------------------------------------

		var _getPopulatedSpecsFromData = function(data, parent) {

			var specs = [];

			for (var i = 0; i < data.length; i++) {
				var entity = data[i];

				var spec = {};
				spec.showSpinner = false;
				var accounttype = xfUtil.getAccountTypeFromDataId(entity.uid);
				if (accounttype === constants.ACCOUNT_TYPES.CLUSTER || accounttype === constants.ACCOUNT_TYPES.ACCOUNT_OWNER) {
					spec = xfClusterBase.getSpecTemplate();
				} else if (accounttype === constants.ACCOUNT_TYPES.CLUSTER_SUMMARY) {
					spec = xfSummaryCluster.getSpecTemplate();
				} else if (accounttype === constants.ACCOUNT_TYPES.ENTITY) {
					spec = xfCard.getSpecTemplate();
				} else {
					aperture.log.error('Unable to determine UI object from account type');
				}

				_UIObjectState.childModule.populateSpecWithData(entity, spec);

				if (spec.hasOwnProperty('parent')) {
					spec['parent'] = parent;
				} else {
					aperture.log.error('Spec does not contain "parent" element');
				}

				specs.push(spec);
			}

			return specs;
		};

		//--------------------------------------------------------------------------------------------------------------

		var _collectAndShowDetails = function(bOnlyEmptySpecs) {
			// If we are showing details then we need to update all the charts that do not already have
			// chart info
			var specs = _UIObjectState.singleton.getSpecs(bOnlyEmptySpecs);
			_UIObjectState.childModule.updateCardsWithCharts(specs);
		};

		//--------------------------------------------------------------------------------------------------------------

		var _onLogout = function(eventChannel) {

			if (eventChannel !== chan.LOGOUT_REQUEST) {
				return;
			}

			// looked at saving state here but it appears we do successfully save state on 'beforeunload'
			// before the server processes logout.
			window.location = 'logout';
		};

		//--------------------------------------------------------------------------------------------------------------

		var _onUnload = function() {
			var sessionRestorationEnabled = (aperture.config.get()['influent.config']['sessionRestorationEnabled'] === true);

			if (sessionRestorationEnabled) {

				var cookieId = aperture.config.get()['aperture.io'].restEndpoint.replace('%host%', 'sessionId');
				var cookie = cookieUtil.readCookie(cookieId);

				_saveState(null, false);

				if (cookie) {
					return 'Influent can remember this session and attempt to resume it when you return. ' +
						'However, a previous session was found. If you wish to resume the previous session ' +
						'you must open a new browser tab for it before leaving this one.';
				}
			} else {

				return 'You are about to leave the current Influent session.';
			}
		};

		//--------------------------------------------------------------------------------------------------------------

		var _onDetailsChangeRequest = function(eventChannel, data, renderCallback) {

			if (eventChannel !== chan.DETAILS_CHANGE_REQUEST ||
				_UIObjectState.singleton == null
			) {
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
		};

		//--------------------------------------------------------------------------------------------------------------

		var _onFilterChangeRequest = function(eventChannel, data, renderCallback) {

			if (eventChannel !== chan.FILTER_CHANGE_REQUEST ||
				_UIObjectState.singleton == null
			) {
				return;
			}

			_UIObjectState.dates.startDate = data.startDate;
			_UIObjectState.dates.endDate = data.endDate;
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
					startDate : data.startDate,
					endDate : data.endDate,
					numBuckets: data.numBuckets,
					duration: data.duration
				}
			);
		};

		//--------------------------------------------------------------------------------------------------------------

		var _onAddCardToContainer = function(eventChannel, data, renderCallback) {

			if (eventChannel !== chan.DROP_EVENT && eventChannel !== chan.ADD_TO_FILE_REQUEST) {
				aperture.log.error(
					'_onAddCardToContainer: ' +
					'Handler does not support this event: ' +
					eventChannel
				);
				return;
			}

			if (!data.cardId ||
				!data.containerId
			) {
				aperture.log.error(
					'_onAddCardToContainer: ' +
					'Invalid data parameter: Required parameter was either missing or null.'
				);
				return;
			}

			if (_UIObjectState.singleton == null) {
				return;
			}

			var addCard = _UIObjectState.singleton.getUIObjectByXfId(data.cardId);
			var targetContainer = _UIObjectState.singleton.getUIObjectByXfId(data.containerId);
			var cardParent = addCard.getParent();

			// Determine if the resulting target cluster should be expanded or not
			var targetExpanded = cardParent.getUIType() == constants.MODULE_NAMES.FILE &&
									cardParent.hasCluster() ?
										cardParent.getClusterUIObject().getVisualInfo().isExpanded :
										targetContainer.hasCluster() ?
											targetContainer.getClusterUIObject().getVisualInfo().isExpanded :
											true;

			if(addCard == null) {
				aperture.log.error(
					'_onAddCardToContainer: ' +
					'Unable to locate card with id ' +
					data.cardId
				);
				return;
			} else if(targetContainer == null) {
				aperture.log.error(
					'_onAddCardToContainer: ' +
					'Unable to locate container with id ' +
					data.containerId
				);
				return;
			} else if(cardParent == null) {
				aperture.log.error(
					'_onAddCardToContainer: ' +
					'Unable to locate parent of card ' +
					data.cardId
				);
				return;
			}

			var contextObj = xfUtil.getContextByUIObject(addCard);
			var targetContextObj = xfUtil.getContextByUIObject(targetContainer);

			if(contextObj == null) {
				aperture.log.error(
					'_onAddCardToContainer: ' +
					'Unable to determine context id of card ' +
					data.cardId
				);
				return;
			}
			else if(targetContextObj == null) {
				aperture.log.error(
					'_onAddCardToContainer: ' +
					'Unable to determine context id of card ' +
					data.containerId
				);
				return;
			} else if (targetContextObj.getUIType() !== constants.MODULE_NAMES.FILE) {
				aperture.log.error(
					'_onAddCardToContainer: ' +
					'Target context object should be of type ' +
					constants.MODULE_NAMES.FILE +
					', not of type ' +
					targetContextObj.getUIType()
				);
				return;
			}

			var insertedCard = addCard;

			// We need to add children of mutable clusters individually because mutable clusters are not
			// saved on the server and we can not, therefore, send the cluster id to the server to obtain
			// its children.
			var insertedCardDataIds = [];
			if (insertedCard.getUIType() === constants.MODULE_NAMES.MUTABLE_CLUSTER) {
				var children = insertedCard.getChildren();
				for (var i = 0; i < children.length; i++) {
					insertedCardDataIds.push(children[i].getDataId());
				}
			} else {
				insertedCardDataIds.push(insertedCard.getDataId());
			}

			// Start the spinner on the target file
			targetContextObj.showSpinner(true);

			// If this card is the current focus, set the focus after it is moved on the server
			var requiresFocusChange = false;
			if (_UIObjectState.singleton.getFocus() != null) {
				if (_UIObjectState.singleton.getFocus().xfId === insertedCard.getXfId()) {
					requiresFocusChange = true;
				}
			}

			// Temporarily hide the original card before it is removed.
			_UIObjectState.singleton.setHidden(data.cardId, true);

			// Force a redraw
			aperture.pubsub.publish(
				chan.RENDER_UPDATE_REQUEST,
				{
					UIObject :  _UIObjectState.singleton,
					layoutRequest : _getLayoutUpdateRequestMsg()
				}
			);

			// Update the server side cache with the new cluster hierarchy.
			_modifyContext(
				contextObj.getXfId(),
				targetContextObj.getXfId(),
				'insert',
				insertedCardDataIds,
				// Callback to be performed after the modify context operation has completed.
				function(response) {

					var afterRemoveOldObjectCallback = function() {

						// Update the target container with the new cluster hierarchy.
						var specs = _getPopulatedSpecsFromData(response.targets, targetContextObj);

						if (specs.length !== 1) {
							aperture.log.error(
									'_onAddCardToContainer: ' +
									'Invalid number of entity specs returned from server: expected 1, received ' +
									specs.length
							);
							return;
						}

						var uiObject =  xfImmutableCluster.createInstance(specs[0]);

						// Give the file a title if it doesn't already have one
						if (!targetContextObj.getVisualInfo().titleInitialized) {

							var initialFileTitle = '';

							var label = uiObject.getLabel().trim();
							var dataId = uiObject.getDataId().trim();

							if(label.length !== 0) {
								initialFileTitle = label;
							} else if(dataId.length !== 0) {
								initialFileTitle = dataId;
							}

							if (initialFileTitle != null && initialFileTitle.length !== 0) {
								if(initialFileTitle.indexOf(')', this.length - 1) !== -1) {
									initialFileTitle = initialFileTitle.substring(0, initialFileTitle.lastIndexOf('(')).trim();
								}

								targetContextObj.setLabel(initialFileTitle);
							}
						}

						uiObject.updateToolbar(
							{
								'allowFile' : false,
								'allowClose' : true,
								'allowFocus' : true,
								'allowSearch' : true
							},
							true
						);

						uiObject.showDetails(_UIObjectState.showDetails);
						uiObject.showToolbar(true);

						targetContextObj.setClusterUIObject(uiObject);

						// Expand the new file cluster if necessary
						if (targetExpanded) {
							aperture.pubsub.publish(chan.EXPAND_EVENT, { xfId: uiObject.getXfId() });
						} else {
							_updateLinks(
								true,
								true,
								[uiObject],
								renderCallback,
								[uiObject]
							);
						}

						if(requiresFocusChange) {
							aperture.pubsub.publish(chan.FOCUS_CHANGE_REQUEST, {
								xfId: uiObject.getXfId()
							});
						}

						// if we are showing card details then we need to populate the card specs with chart data
						if (_UIObjectState.showDetails) {
							_UIObjectState.childModule.updateCardsWithCharts(specs);
						}

						targetContextObj.showSpinner(false);

						if (data.hasOwnProperty('onAddedCallback')) {
							data.onAddedCallback();
						}

						// pattern search may now be valid if files had no content before.
						_checkPatternSearchState();

					};

					// remove old card
					_removeObject(
						addCard,
						chan.REMOVE_REQUEST,
						true,
						afterRemoveOldObjectCallback
					);
				}
			);
		};

		//--------------------------------------------------------------------------------------------------------------

		var _onFocusChangeRequest = function(eventChannel, data) {

			if (eventChannel !== chan.FOCUS_CHANGE_REQUEST ||
				_UIObjectState.singleton == null
			) {
				return;
			}

			if (data == null || data.xfId == null) {
				aperture.pubsub.publish(chan.FOCUS_CHANGE_EVENT, {focus: null});
				return;
			}

			var xfUiObj = _UIObjectState.singleton.getUIObjectByXfId(data.xfId);
			var focusData;

			if(xfUiObj == null) {
				_UIObjectState.focus = null;
				focusData = null;
			}
			else{
				var contextObj = xfUtil.getContextByUIObject(xfUiObj);

				focusData = {
					xfId: xfUiObj.getXfId(),
					dataId: xfUiObj.getDataId(),
					entityType: xfUiObj.getUIType(),
					entityLabel: xfUiObj.getLabel(),
					entityCount: xfUiObj.getVisualInfo().spec.count,
					contextId: contextObj ? contextObj.getXfId() : null,
                    sessionId : _UIObjectState.singleton.getSessionId()
				};
			}

			aperture.pubsub.publish(chan.FOCUS_CHANGE_EVENT, {focus: focusData});
		};

		//--------------------------------------------------------------------------------------------------------------

		var _onFocusChangeEvent = function(eventChannel, data, renderCallback) {

			if (eventChannel !== chan.FOCUS_CHANGE_EVENT ||
				_UIObjectState.singleton == null
			) {
				return;
			}

			_UIObjectState.singleton.setFocus(data.focus);

			if (_UIObjectState.showDetails) {
				// If we are showing details then we need to update all the charts
				_collectAndShowDetails(false);
			}

			renderCallback();
		};

		//--------------------------------------------------------------------------------------------------------------

		var _onSelectionChangeRequest = function(eventChannel, data) {

			if (eventChannel !== chan.SELECTION_CHANGE_REQUEST ||
				_UIObjectState.singleton == null
			) {
				return;
			}

			var objectToBeSelected = data.xfId && _UIObjectState.singleton.getUIObjectByXfId(data.xfId);
			var previousSelection = null;

			if (_UIObjectState.selectedUIObject != null) {
				previousSelection = _UIObjectState.singleton.getUIObjectByXfId(_UIObjectState.selectedUIObject.xfId);
			}

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
					accountType: objectToBeSelected.getAccountType(),
					contextId: xfUtil.getContextByUIObject(objectToBeSelected).getXfId()
				};

				// Add owner ID for summary clusters and owner clusters
				if (xfUtil.isClusterTypeFromObject(objectToBeSelected)) {
					_UIObjectState.selectedUIObject.ownerId = objectToBeSelected.getOwnerId();
				}

				_UIObjectState.singleton.setSelection(objectToBeSelected.getXfId());

				// Call any selection plugins
				var extensions = plugins.get('cards');
				var plugin = aperture.util.find(extensions, function (e) {
					return e.select !== undefined;
				});

				if (plugin) {

					plugin.select({
						dataId: _UIObjectState.selectedUIObject.dataId,
						type: _UIObjectState.selectedUIObject.accountType,
						label: _UIObjectState.selectedUIObject.label,
						numMembers: _UIObjectState.selectedUIObject.count,

						// is the entity flagged PROMPT_FOR_DETAILS?
						shouldPrompt: objectToBeSelected.getVisualInfo().spec.promptForDetails,

						// Include mouse details in the event
						mouseButton : data.clickEvent.which,
						mouseX : data.clickEvent.pageX,
						mouseY : data.clickEvent.pageY
					});
				}
			}

			aperture.pubsub.publish(chan.SELECTION_CHANGE_EVENT, _UIObjectState.selectedUIObject);

			if (previousSelection !== null) {
				aperture.pubsub.publish(chan.RENDER_UPDATE_REQUEST, {UIObject : previousSelection});
			}

			if (objectToBeSelected !== null) {
				aperture.pubsub.publish(chan.RENDER_UPDATE_REQUEST, {UIObject : objectToBeSelected});
			}

			var expand = objectToBeSelected != null;

			// expand footer only if something selected. this will trigger an event when done.
			// should we just listen to selection here?
			window.setTimeout(function() {
				aperture.pubsub.publish(chan.FOOTER_STATE_REQUEST, {selection: objectToBeSelected, expand: expand});
			}, 100);
		};

		//--------------------------------------------------------------------------------------------------------------

		var _onHoverChangeRequest = function(eventChannel, data) {

			if (eventChannel !== chan.UI_OBJECT_HOVER_CHANGE_REQUEST ||
				_UIObjectState.singleton == null
			) {
				return;
			}

			_UIObjectState.singleton.setHovering(data.xfId);
		};

		//--------------------------------------------------------------------------------------------------------------

		var _onChangeFileTitle = function(eventChannel, data, renderCallback) {

			if (eventChannel !== chan.CHANGE_FILE_TITLE ||
				_UIObjectState.singleton == null
			) {
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

		var _branchRequestHandler = function(eventChannel, data, renderCallback){

			if (eventChannel !== chan.BRANCH_REQUEST) {
				aperture.log.error('Branch request handler does not support this event: ' + eventChannel);
				return;
			}

			var sourceObj = _UIObjectState.singleton.getUIObjectByXfId(data.xfId);
			var isBranchRight = data.direction === 'right';
			var sourceEntity = sourceObj.getDataId();
			var column = _getColumnByUIObject(sourceObj);

            if (isBranchRight) {
                sourceObj.setRightOperation(toolbarOp.WORKING);
            } else {
                sourceObj.setLeftOperation(toolbarOp.WORKING);
            }
            aperture.pubsub.publish(chan.RENDER_UPDATE_REQUEST, {UIObject : sourceObj});

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

			var srcContext = xfUtil.getUITypeAncestor(sourceObj, constants.MODULE_NAMES.FILE);
			if (!srcContext) {
				srcContext = column;
			}

			var srcContextId = srcContext.getDataId();
			var tarContextId = targetColumn.getDataId();

			xfRest.request('/relatedlinks').inContext( tarContextId ).withData({

				sessionId: _UIObjectState.sessionId,
				entity: sourceEntity,
				targets: [],
				linktype: isBranchRight ? 'source' : 'destination',
				type: 'FINANCIAL',
				aggregate: 'FLOW',
				startdate: _UIObjectState.dates.startDate,
				enddate: _UIObjectState.dates.endDate,
				srcContextId: srcContextId,
				tarContextId: tarContextId

			}).then(function (response) {

				if (_UIObjectState.singleton.getUIObjectByXfId(data.xfId) != null) {

					var newUIObjects = _handleBranchingServerResponse(response, sourceObj, isBranchRight, targetColumn);

					if (isBranchRight) {
						sourceObj.setRightOperation(toolbarOp.BRANCH);
					} else {
						sourceObj.setLeftOperation(toolbarOp.BRANCH);
					}
					//renderCallback();

					_updateLinks(true, true, newUIObjects, function () {
						targetColumn.sortChildren(DEFAULT_SORT_FUNCTION);

						if (renderCallback != null) {
							renderCallback();
						}
					}, newUIObjects);
				} else {
					renderCallback();
				}
			});
		};

		//--------------------------------------------------------------------------------------------------------------

		var _handleCascadingLinks = function(cardObjectList, cascadeRight, renderCallback) {

			if (cardObjectList == null || cardObjectList.length === 0 ) {
				renderCallback();
				return;
			}

			// Get all cards and clusters in column to the right/left of uiObj.  Must look not only for 'dangling'
			// UI objects, but also anything that is in a search result (ie/ inside a match card)
			var column = _getColumnByUIObject(cardObjectList[0]);           // all objects in objectList are in the same column
			var adjacentColumn = cascadeRight ? _getNextColumn(column) : _getPrevColumn(column);
			var adjacentChildren = xfUtil.getLinkableChildren(adjacentColumn);

			// Early out
			if (adjacentChildren.length === 0 ) {
				if (renderCallback){
					renderCallback();
				}
				return;
			}

			// remove old links
			for (var i = 0; i < cardObjectList.length; i++) {
				var cardObject = cardObjectList[i];
				var cardObjectLinks = cardObject.getLinks();
				for (var linkId in cardObjectLinks) {
					if (cardObjectLinks.hasOwnProperty(linkId)) {
						var link = cardObjectLinks[linkId];
						if (cascadeRight) {
							if (link.getSource().getXfId() === cardObject.getXfId()) {
								link.remove();
							}
						} else {
							if (link.getDestination().getXfId() === cardObject.getXfId()) {
								link.remove();
							}
						}
					}
				}
			}

			var contextObjectMap = _getContextObjectMap(cardObjectList, adjacentChildren);

			// create a couter callback so we only call our passed in call back after we have completed
			// links to boths sides of the column
			var numberOfContexts = _.size(contextObjectMap.src) * _.size(contextObjectMap.tar);
			var counter = 0;
			var counterCallback = function() {
				counter++;
				if (counter === numberOfContexts) {
					if (renderCallback){
						renderCallback();
					}
				}
			};

			aperture.util.forEach(contextObjectMap.src, function(srcContext, srcContextId) {
				aperture.util.forEach(contextObjectMap.tar, function(tarContext, tarContextId) {
					// Fetch related links between the unlinked objects
					xfRest.request('/aggregatedlinks').inContext( srcContextId ).withData({

						sessionId: _UIObjectState.sessionId,
						sourceIds: srcContext,
						targetIds: tarContext,
						linktype: cascadeRight ? 'source' : 'destination',
						type: 'FINANCIAL',
						aggregate: 'FLOW',
						startdate: _UIObjectState.dates.startDate,
						enddate: _UIObjectState.dates.endDate,
						contextId: srcContextId,
						targetContextId: tarContextId

					}).then(function (response) {

						_handleCascadingLinksResponse(
							response,
							cascadeRight ? 'source' : 'destination',
							column.getXfId(),
							adjacentColumn.getXfId(),
							counterCallback
						);
					});
				});
			});
		};

		//--------------------------------------------------------------------------------------------------------------

		var _getContextObjectMap = function(
			srcObjects,
			tarObjects
		) {

			var i;
			var contextObjectMap = {
				'src' : {},
				'tar' : {}
			};

			for (i = 0; i < srcObjects.length; i++) {
				var srcObj = srcObjects[i];
				var srcContext = xfUtil.getContextByUIObject(srcObj).getXfId();
				if (contextObjectMap.src.hasOwnProperty(srcContext)) {
					contextObjectMap.src[srcContext].push(srcObj.getDataId());
				} else {
					contextObjectMap.src[srcContext] = [srcObj.getDataId()];
				}
			}

			for (i = 0; i < tarObjects.length; i++) {
				var tarObj = tarObjects[i];
				var tarContext = xfUtil.getContextByUIObject(tarObj).getXfId();
				if (contextObjectMap.tar.hasOwnProperty(tarContext)) {
					contextObjectMap.tar[tarContext].push(tarObj.getDataId());
				} else {
					contextObjectMap.tar[tarContext] = [tarObj.getDataId()];
				}
			}

			return contextObjectMap;
		};

		//--------------------------------------------------------------------------------------------------------------

		var _handleCascadingLinksResponse = function(
			serverResponse,
			branchType,
			sourceColumnId,
			targetColumnId,
			renderCallback
		) {
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
						if (branchType === 'destination') {
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
				targetColumn.prepareColumnForServerUpdate();
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
					if (specs[i].type === constants.MODULE_NAMES.SUMMARY_CLUSTER) {
						uiObject =  xfSummaryCluster.createInstance(specs[i]);
					} else if (xfUtil.isClusterTypeFromSpec(specs[i])) {
						uiObject =  xfImmutableCluster.createInstance(specs[i]);
					} else {
						uiObject =  xfCard.createInstance(specs[i]);
					}

					uiObject.updateToolbar(
						{
							'allowFile' : true,
							'allowClose' : true,
							'allowFocus' : true,
							'allowSearch' : true
						},
						true
					);

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

            aperture.pubsub.publish(chan.BRANCH_RESULTS_RETURNED_EVENT);

			return createdUIObjects;
		};

		//--------------------------------------------------------------------------------------------------------------
		var _numberFormatter = aperture.Format.getNumberFormat(1);

		var _verifyBranchRequest = function(uiObject, direction, onOk, onCancel) {
			var degree = direction === 'left' ? uiObject.getInDegree() : uiObject.getOutDegree();
			var sdegree = _numberFormatter.format(degree);

			if (uiObject.getUIType() !== constants.MODULE_NAMES.SUMMARY_CLUSTER &&
				OBJECT_DEGREE_LIMIT_COUNT > 0 && degree > OBJECT_DEGREE_LIMIT_COUNT) {
				xfModalDialog.createInstance({
					title : 'Sorry! Try a More Focused Branch',
					contents : 'This branch operation would retrieve '+ sdegree+ ' accounts, which is more than '+
					'the configured limit of '+_numberFormatter.format(OBJECT_DEGREE_LIMIT_COUNT)+ '.',
					buttons : {
						'Ok' : function() {
							if (onCancel) {
								onCancel();
							}
						}
					}
				});

			} else if (uiObject.getUIType() !== constants.MODULE_NAMES.SUMMARY_CLUSTER &&
						OBJECT_DEGREE_WARNING_COUNT > 0 && degree > OBJECT_DEGREE_WARNING_COUNT) {
				xfModalDialog.createInstance({
					title : 'Warning!',
					contents : 'This branch operation will retrieve '+ sdegree +' linked accounts. Are you ' +
								'sure you want to retrieve ALL of them?',
					buttons : {
						'Branch' : function() {
							if (onOk) {
								onOk();
							}
						},
						'Cancel' : function() {
							if (onCancel) {
								onCancel();
							}
						}
					}
				});

			} else {
				if (onOk) {
					onOk();
				}
			}
		};

		//--------------------------------------------------------------------------------------------------------------

		var _onBranchRequest = function(eventChannel, data, renderCallback) {

			if (eventChannel !== chan.BRANCH_REQUEST ||
				_UIObjectState.singleton == null
			) {
				return;
			}

			var uiObj = _UIObjectState.singleton.getUIObjectByXfId(data.xfId);
			_verifyBranchRequest(uiObj, data.direction, function() {
				_branchRequestHandler(eventChannel, data, renderCallback);
			});

		};

		//--------------------------------------------------------------------------------------------------------------

		var _onExpandEvent = function(eventChannel, data, renderCallback) {

			if (eventChannel !== chan.EXPAND_EVENT ||
				_UIObjectState.singleton == null
			) {
				return;
			}

			var uiObj = _UIObjectState.singleton.getUIObjectByXfId(data.xfId);
			var contextId = xfUtil.getContextByUIObject(uiObj).getXfId();

			if (!xfUtil.isClusterTypeFromObject(uiObj)) {
				aperture.log.error('Expand request for a non cluster object');
			}

			if (uiObj.isExpanded()) {
				renderCallback();
				return;
			}

			// if the cluster is selected we deselect it
			if (uiObj.isSelected) {
				aperture.pubsub.publish(
					chan.SELECTION_CHANGE_REQUEST,
					{
						xfId: null,
						selected: true,
						noRender: true
					}
				);
			}

			uiObj.expand();

			aperture.pubsub.publish(chan.RENDER_UPDATE_REQUEST, {UIObject : uiObj, layoutRequest : _getLayoutUpdateRequestMsg()});

			//*********************************
			// REST call to get actual card data
			//*********************************
			xfRest.request('/entities').inContext( contextId ).withData({

				sessionId : _UIObjectState.sessionId,
				entities : uiObj.getVisibleDataIds(),
				contextid : contextId

			}).then(function (response) {

				_populateExpandResults(uiObj, response, renderCallback);

			});
		};

		//--------------------------------------------------------------------------------------------------------------

		var _populateExpandResults = function(clusterObj, serverResponse, renderCallback) {

			var entities = serverResponse.data;
			var specs = _getPopulatedSpecsFromData(entities, clusterObj);

			for (var i = 0; i < specs.length; i++) {
				var childSpec = specs[i];
				var children = clusterObj.getUIObjectsByDataId(childSpec.dataId);
				if (_.isEmpty(children)) {
					aperture.log.error('_populateExpandResults: Unable to find child for spec');
				} else {
					for (var j = 0; j < children.length; j++) {
						children[j].update(childSpec);
						// Update the highlight state of any children.
						children[j].highlightId((_UIObjectState.singleton.getFocus() == null) ? null : _UIObjectState.singleton.getFocus().xfId);
					}
				}

				// Look for duplicates for the expanded child
				_updateDuplicates([childSpec.dataId]);
			}

			clusterObj.showSpinner(false);
			clusterObj.showToolbar(true);

			// if we are showing card details then we need to populate the card specs with chart data
			if (_UIObjectState.showDetails) {
				_UIObjectState.childModule.updateCardsWithCharts(specs);
			}

			var affectedObjects = xfUtil.getLinkableChildren(clusterObj);
			affectedObjects.push(clusterObj);
			_updateLinks(true, true, xfUtil.getLinkableChildren(clusterObj), function() {
				clusterObj.sortChildren(DEFAULT_SORT_FUNCTION);

				if(renderCallback != null) {
					renderCallback();
				}
			}, affectedObjects);
		};

		//--------------------------------------------------------------------------------------------------------------

		var _onCollapseEvent = function(eventChannel, data, renderCallback) {

			if (eventChannel !== chan.COLLAPSE_EVENT ||
				_UIObjectState.singleton == null
			) {
				return;
			}

			var uiObj = _UIObjectState.singleton.getUIObjectByXfId(data.xfId);

			if (uiObj.getUIType() !== constants.MODULE_NAMES.FILE && !xfUtil.isClusterTypeFromObject(uiObj)) {
				aperture.log.error('Collapse request for a non cluster or file object');
			}

			var affectedObjects = xfUtil.getLinkableChildren(uiObj);
			affectedObjects.push(uiObj);

			if (!uiObj.isExpanded()) {

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

			aperture.pubsub.publish(
				chan.RENDER_UPDATE_REQUEST,
				{
					UIObject :  uiObj,
					layoutRequest : _getLayoutUpdateRequestMsg()
				}
			);

			if (_UIObjectState.showDetails) {
				_UIObjectState.childModule.updateCardsWithCharts(uiObj.getSpecs(false));
			}

			_updateLinks(true, true, [uiObj], renderCallback, affectedObjects);

		};

		//--------------------------------------------------------------------------------------------------------------

		var _selectAdjacentMatchObject = function(columnObj){
			var adjacentMatchs = xfUtil.getChildrenByType(columnObj, constants.MODULE_NAMES.MATCH);
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

		var _removeObject = function(objToRemove, eventChannel, dispose, renderCallback) {

			if (objToRemove === null || objToRemove.getVisualInfo() == null) {
				// Object no longer exists, or it has been disposed

				if (renderCallback) {
					renderCallback();
				}

				return;
			}

			var dataId = objToRemove.getDataId();
			var xfId = objToRemove.getXfId();

			// Find the context ID of the object to be removed
			var contextObj = xfUtil.getContextByUIObject(objToRemove);

			// find the column ID of the object to be removed
			var sourceColumn = _getColumnByUIObject(objToRemove);

			var removeObj;
			var callback;

			// if the object is a column, then we have to delete the column on the server. We check to ensure that
			// the column is removed on the server and then remove the column from the client.
			if (objToRemove.getUIType() === constants.MODULE_NAMES.COLUMN) {

				callback = function() {

					_UIObjectState.singleton.removeChild(xfId, true, false, false);

					// technically we only need to check when files are involved but this check is lightweight
					_checkPatternSearchState();

					if (renderCallback) {
						renderCallback();
					}
				};

				_modifyContext(
					sourceColumn.getDataId(),
					sourceColumn.getDataId(),
					'delete',
					[dataId],
					callback
				);
			}

			// if the object is a match card, then we re-parent the search focus (if required) and remove the
			// match card (and all children) from the client. We do not need to notify the server.
			else if (objToRemove.getUIType() === constants.MODULE_NAMES.MATCH) {

				if (objToRemove.isSearchControlFocused()){
					var currentColumn = xfUtil.getContextByUIObject(objToRemove);
					var colIndex = _getColumnIndex(currentColumn);
					// Search RIGHT.
					var matchFound = false;
					var columnChildren = _UIObjectState.singleton.getChildren();
					var i;
					for (i = colIndex + 1; i < columnChildren.length; i++){
						matchFound = _selectAdjacentMatchObject(columnChildren[i]);
						if (matchFound){
							break;
						}
					}
					// Search LEFT.
					if (!matchFound){
						for (i = colIndex - 1; i >= 0; i--){
							if (_selectAdjacentMatchObject(columnChildren[i])){
								break;
							}
						}
					}
				}

				removeObj = function() {
					objToRemove.remove(eventChannel, dispose);
				};

				_removeObjectAndWiring(
					removeObj,
					dispose,
					eventChannel,
					sourceColumn,
					objToRemove,
					dataId
				);

				if (renderCallback) {
					renderCallback();
				}
			}

			// if the object is a file, then we have to delete the file from its parent column. We check to ensure that
			// the file is removed on the server and then remove the file from the client.
			else if (objToRemove.getUIType() === constants.MODULE_NAMES.FILE){

				callback = function() {
					removeObj = function() {
						objToRemove.remove(eventChannel, dispose);
					};

					_removeObjectAndWiring(
						removeObj,
						dispose,
						eventChannel,
						sourceColumn,
						objToRemove,
						dataId
					);

					// technically we only need to check when files are involved but this check is lightweight
					_checkPatternSearchState();

					if (renderCallback) {
						renderCallback();
					}
				};

				_modifyContext(
					sourceColumn.getDataId(),
					sourceColumn.getDataId(),
					'delete',
					[dataId],
					callback
				);
			}

			// if the object is inside a matchcard file, then we just remove the object from the client.
			// We do not need to notify the server.
			else if (xfUtil.isUITypeDescendant(objToRemove, constants.MODULE_NAMES.MATCH)) {
				removeObj = function() {
					objToRemove.remove(eventChannel, dispose);
				};

				_removeObjectAndWiring(
					removeObj,
					dispose,
					eventChannel,
					sourceColumn,
					objToRemove,
					dataId
				);

				if (renderCallback) {
					renderCallback();
				}
			}

			// if the object is a root object then we have to remove the object from its parent. We check to ensure that
			// the file is removed on the server and then remove the file from the client.
			else if (objToRemove.getParent().getXfId() ===  contextObj.getXfId()) {

				callback = function(response) {
					if (!response.hasOwnProperty('contextId') ||
						response.contextId !== contextObj.getDataId()
						) {
						aperture.log.error(
								'_removeObject: ' +
								'server responded with invalid context ID'
						);
						return;
					}

					if (!response.hasOwnProperty('sessionId') ||
						response.sessionId !== _UIObjectState.sessionId
						) {
						aperture.log.error(
								'_removeObject: ' +
								'server responded with invalid session ID'
						);
						return;
					}

					if (!response.hasOwnProperty('targets')) {
						aperture.log.error(
								'_removeObject: ' +
								'server responded with no cluster information'
						);
						return;
					}

					removeObj = function() {
						objToRemove.remove(eventChannel, dispose);
					};

					_removeObjectAndWiring(
						removeObj,
						dispose,
						eventChannel,
						sourceColumn,
						objToRemove,
						dataId
					);

					// technically we only need to check when files are involved but this check is lightweight
					_checkPatternSearchState();

					if (renderCallback) {
						renderCallback();
					}
				};

				_modifyContext(
					contextObj.getDataId(),
					contextObj.getDataId(),
					'remove',
					[dataId],
					callback
				);
			}

			// otherwise, we need to remove the object on the server and update the client based on the response
			// contents
			else {

				callback = function(response) {
					if (!response.hasOwnProperty('contextId') ||
						response.contextId !== contextObj.getDataId()
					) {
						aperture.log.error(
							'_removeObject: ' +
							'server responded with invalid context ID'
						);
						return;
					}

					if (!response.hasOwnProperty('sessionId') ||
						response.sessionId !== _UIObjectState.sessionId
					) {
						aperture.log.error(
							'_removeObject: ' +
							'server responded with invalid session ID'
						);
						return;
					}

					if (!response.hasOwnProperty('targets')) {
						aperture.log.error(
							'_removeObject: ' +
							'server responded with no cluster information'
						);
						return;
					}

					if (response.targets.length === 0) {

						var rootNode = xfUtil.getTopLevelEntity(objToRemove);
						var children = rootNode.getChildren();
						if (children.length > 0) {
							aperture.util.forEach(children, function(child) {
								if (child.getXfId() === xfId) {
									rootNode.removeChild(child.getXfId(), true, false, false);
								} else {
									rootNode.removeChild(child.getXfId(), false, true, false);
									contextObj.insert(child);
								}
							});
						}

						if (rootNode != null) {
							// If the root was the focus, unset the focus
							if (_UIObjectState.focus != null &&
								_UIObjectState.focus.xfId === rootNode.getXfId()) {

								aperture.pubsub.publish(
									chan.FOCUS_CHANGE_REQUEST,
									{xfId: null}
								);
							}

							rootNode.getParent().removeChild(rootNode.getXfId(), true, false, false);
						}

						_checkPatternSearchState();

						if (renderCallback) {
							renderCallback();
						}

					} else {

						var i;
						var specs = [];
						for (i = 0; i < response.targets.length; i++) {
							var target = response.targets[i];
							if (contextObj.getUIType() === constants.MODULE_NAMES.FILE &&
								!target.isRoot
							) {
								specs = specs.concat(_getPopulatedSpecsFromData([target], contextObj.getClusterUIObject()));
							} else {
								specs = specs.concat(_getPopulatedSpecsFromData([target], contextObj));
							}
						}

						var addedCards = [];

						for (i = 0; i < specs.length; i++) {
							var uiObjects = contextObj.getUIObjectsByDataId(specs[i].dataId);
							for (var j = 0; j < uiObjects.length; j++) {
								var updateResult = uiObjects[j].update(specs[i]);
								if (updateResult != null && updateResult.length > 0) {
									for (var k = 0; k < updateResult.length; k++) {
										addedCards.push(updateResult[k]);
									}
								}

								// Check to see if the spec update caused us to invalidate our focus.
								// This probably means we just had it deleted by the server. Unset it now.
								if (_UIObjectState.focus != null && _UIObjectState.singleton.getUIObjectByXfId(_UIObjectState.focus.xfId) == null) {
									aperture.pubsub.publish(chan.FOCUS_CHANGE_REQUEST, {
										xfId: null
									});
								}
							}
						}

						if (addedCards.length > 0) {
							xfRest.request('/entities').inContext(contextObj.getDataId()).withData({

								sessionId: _UIObjectState.sessionId,
								entities: addedCards,
								contextid: contextObj.getDataId()

							}).then(function(response) {

								var specs = _getPopulatedSpecsFromData(response.data, contextObj);

								for (var i = 0; i < specs.length; i++) {
									var childSpec = specs[i];

									// we don't want to set the parent on this update so we delete it
									delete childSpec.parent;

									var children = contextObj.getUIObjectsByDataId(childSpec.dataId);
									if (_.isEmpty(children)) {
										aperture.log.error('_populateExpandResults: Unable to find child for spec');
									} else {
										for (var j = 0; j < children.length; j++) {
											children[j].update(childSpec);
											children[j].highlightId((_UIObjectState.singleton.getFocus() == null) ? null : _UIObjectState.singleton.getFocus().xfId);
										}
									}
								}

								// if we are showing card details then we need to populate the card specs with chart data
								if (_UIObjectState.showDetails) {
									_UIObjectState.childModule.updateCardsWithCharts(specs);
								}

								// technically we only need to check when files are involved but this check is lightweight
								_checkPatternSearchState();

								if (renderCallback) {
									renderCallback();
								}
							});

						} else {
							// technically we only need to check when files are involved but this check is lightweight
							_checkPatternSearchState();

							if (renderCallback) {
								renderCallback();
							}
						}
					}
				};

				_modifyContext(
					contextObj.getDataId(),
					contextObj.getDataId(),
					'remove',
					[dataId],
					callback
				);
			}
		};

		//--------------------------------------------------------------------------------------------------------------

		var _removeObjectAndWiring = function(
			removalFunction,
			dispose,
			eventChannel,
			sourceColumn,
			objToRemove,
			dataId
		) {

			if (dispose &&
				_UIObjectState.selectedUIObject != null &&
				(_UIObjectState.selectedUIObject.xfId === objToRemove.getXfId() ||
				objToRemove.getUIObjectByXfId(_UIObjectState.selectedUIObject.xfId) != null)
			) {
				_UIObjectState.selectedUIObject = null;
			}

			// Remove this object from the position map before it's disposed.
			xfLayoutProvider.removeUIObject(objToRemove);

			// remove the object using the removal function
			removalFunction();

			// check to see if this remove operation removed any duplicates
			if(dataId != null) {
				_updateDuplicates([dataId]);
			}
		};

		//--------------------------------------------------------------------------------------------------------------

		var _onRemoveRequest = function(eventChannel, data, renderCallback) {

			if (eventChannel !== chan.REMOVE_REQUEST ||
				_UIObjectState.singleton == null
			) {
				return;
			}

			var numberOfIds = data.xfIds.length;
			var counter = 0;
			var deletedFocusedObject = false;

			var counterCallback = function() {
				counter++;
				if (counter === numberOfIds) {

					if (deletedFocusedObject) {
						aperture.pubsub.publish(
							chan.FOCUS_CHANGE_REQUEST,
							{xfId: null}
						);
					}

					if (renderCallback){
						renderCallback();
					}
				}
			};

			for (var i=0; i < data.xfIds.length; i++) {

				if (_UIObjectState.focus != null &&
					_UIObjectState.focus.xfId === data.xfIds[i]
				) {
					deletedFocusedObject = true;
				}

				// Temporarily hide the card before it is removed
				_UIObjectState.singleton.setHidden(data.xfIds[i], true);

				_removeObject(
					_UIObjectState.singleton.getUIObjectByXfId(data.xfIds[i]),
					eventChannel,
					data.dispose,
					counterCallback
				);
			}
		};

		//--------------------------------------------------------------------------------------------------------------
		var _modifyContext = function (sourceContextId, targetContextId, editStr, entityIds, responseCallback) {

			xfRest.request( '/modifycontext' ).inContext( targetContextId ).withData({
				sessionId: _UIObjectState.sessionId,
				sourceContextId: sourceContextId,
				targetContextId: targetContextId,
				edit: editStr,
				entityIds: entityIds
			}).then(responseCallback);
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

			if ((eventChannel !== chan.PREV_SEARCH_PAGE_REQUEST &&
				eventChannel !== chan.NEXT_SEARCH_PAGE_REQUEST &&
				eventChannel !== chan.SET_SEARCH_PAGE_REQUEST) ||
				_UIObjectState.singleton == null
			) {
				return;
			}

			var matchObject = _UIObjectState.singleton.getUIObjectByXfId(data.xfId);
			if (eventChannel === chan.PREV_SEARCH_PAGE_REQUEST) {
				matchObject.pageLeft();
			} else if (eventChannel === chan.NEXT_SEARCH_PAGE_REQUEST){
				matchObject.pageRight();
			} else if (eventChannel === chan.SET_SEARCH_PAGE_REQUEST) {
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

			if (eventChannel !== chan.CLEAN_COLUMN_REQUEST ||
				_UIObjectState.singleton == null
			) {
				return;
			}

			var columnUIObject = _UIObjectState.singleton.getUIObjectByXfId(data.xfId);

			var contextId = columnUIObject.getXfId();
			var dataIds = columnUIObject.cleanColumn();
			if (dataIds.length > 0) {
				_modifyContext(contextId, contextId, 'remove', dataIds);
				renderCallback();
			}
		};

		//--------------------------------------------------------------------------------------------------------------

		var _onSortColumnRequest = function(eventChannel, data, renderCallback) {

			if (eventChannel !== chan.SORT_COLUMN_REQUEST ||
				_UIObjectState.singleton == null
			) {
				return;
			}

			var columnUIObject = _UIObjectState.singleton.getUIObjectByXfId(data.xfId);
			columnUIObject.sortChildren(data.sortFunction);

			renderCallback();
		};

		//--------------------------------------------------------------------------------------------------------------

		var _onCreateFileRequest = function(eventChannel, data, renderCallback) {

			if (eventChannel !== chan.CREATE_FILE_REQUEST ||
				_UIObjectState.singleton == null
			) {
				return;
			}

			var sourceObj = _UIObjectState.singleton.getUIObjectByXfId(data.xfId);
			var	columnUIObj;

			if (!data.isColumn) {
				columnUIObj = xfUtil.getContextByUIObject(sourceObj);
			} else {
				columnUIObj = sourceObj;
			}

			// Create a new file uiObject.
			var fileSpec = xfFile.getSpecTemplate();
			var fileUIObj = xfFile.createInstance(fileSpec);
			fileUIObj.showDetails(_UIObjectState.singleton.showDetails());
			fileUIObj.showSpinner(data.showSpinner);
			// Add the file to the very top of the column.
			var topObj = null;
			if (columnUIObj.getVisualInfo().children.length > 0){
				topObj = columnUIObj.getVisualInfo().children[0];
			}
			columnUIObj.insert(fileUIObj, topObj);

			if (data.showSpinner) {
				aperture.pubsub.publish(chan.RENDER_UPDATE_REQUEST, {UIObject : fileUIObj});
			}

			// Link it to all files to the columns to the left and right of our column
			_addFileLinks(fileUIObj, columnUIObj, true, true);

			_modifyContext(
				fileUIObj.getXfId(),
				columnUIObj.getXfId(),
				'create'
			);

			if (!data.isColumn) {

				var pubData = {
					containerId : fileUIObj.getXfId(),
					cardId: sourceObj.getXfId()
				};

				// If the matchcard has been requested, append a callback
				// to display it once the file has been populated
				if (data.showMatchCard) {

					pubData.onAddedCallback = function() {
						aperture.pubsub.publish(
							chan.SHOW_MATCH_REQUEST,
							{
								xfId : fileUIObj.getXfId()
							}
						);

						aperture.pubsub.publish(
							chan.ADVANCE_SEARCH_DIALOG_REQUEST,
							{
								fileId : fileUIObj.getXfId(),
								terms : null,
								dataIds : [fileUIObj.getClusterUIObject().getDataId()],
								contextId : fileUIObj.getDataId()
							}
						);
					};
				}

				aperture.pubsub.publish(chan.ADD_TO_FILE_REQUEST, pubData);

			} else {
				_pruneColumns();
				renderCallback();
			}
		};

		//--------------------------------------------------------------------------------------------------------------

		var _onShowMatchRequest = function(eventChannel, data, renderCallback) {

			if (eventChannel !== chan.SHOW_MATCH_REQUEST ||
				_UIObjectState.singleton == null
			) {
				return;
			}

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
						dataIds : fileObj.getContainedCardDataIds(),
						contextId : fileObj.getDataId()
					}
				);
			}

			renderCallback();
		};

		//--------------------------------------------------------------------------------------------------------------

		var _onSearchBoxChanged = function(eventChannel, data, renderCallback) {

			if (eventChannel !== chan.SEARCH_BOX_CHANGED ||
				_UIObjectState.singleton == null
			) {
				return;
			}

			var xfId = data.xfId;
			var newVal = data.val;
			var matchCard = _UIObjectState.singleton.getUIObjectByXfId(xfId);
			if (matchCard) {
				matchCard.setSearchTerm(newVal);
			}

			renderCallback();
		};

		//--------------------------------------------------------------------------------------------------------------

		var _setSearchControlFocused = function(eventChannel, data) {
			for (var i = 0; i < _UIObjectState.children.length; i++) {
				_UIObjectState.children[i].setSearchControlFocused(data.xfId);
			}
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

			if (async == null || async !== false) {
				async = true;
			}

			xfRest.request('/savestate', 'POST', async).withData({

				sessionId : _UIObjectState.sessionId,
				data : (currentState) ? JSON.stringify(currentState) : ''

			}).then(function (response) {

				if (callback != null) {
					callback(response.persistenceState);
				}

			});
		};

		//--------------------------------------------------------------------------------------------------------------

		var _onExportCapture = function(eventChannel) {

			if (eventChannel !== chan.EXPORT_CAPTURED_IMAGE_REQUEST ||
				_UIObjectState.singleton == null
			) {
				return;
			}

			var callback = function(response) {

				var exportCallback = function() {
					$.blockUI({
						theme: true,
						title: 'Capture In Progress',
						message: '<img src="' + constants.AJAX_SPINNER_FILE + '" style="display:block;margin-left:auto;margin-right:auto"/>'
					});

					var timestamp = (new Date().getTime());

					var dimensions = xfRenderer.getCaptureDimensions();

					var settings = {
						format : 'PNG',
						captureWidth : dimensions.width,
						captureHeight : dimensions.height,
						renderDelay : 4000,
						reload : true,
						downloadToken : timestamp,
					};


					var auth = aperture.config.get()['influent.config']['captureAuthentication'];
					if (auth != null) {
						if (auth.username != null && auth.password != null) {
							settings.username = auth.username;
							settings.password = auth.password;
						}
					}

					var sessionUrl = window.location.href;

					if (sessionUrl.indexOf('?sessionId=') === -1) {
						sessionUrl += '?sessionId=' + _UIObjectState.sessionId;
					}

					aperture.capture.store(
						sessionUrl + '&capture=true',
						settings,
						null,
						function(response){
							var a = document.createElement('a');

							a.href = aperture.store.url(response, 'pop', 'influent-snapshot.png');

							document.body.appendChild(a);

							setTimeout(
								function() {
									$(window).unbind('beforeunload');
									a.click();
									document.body.removeChild(a);
									$.unblockUI();
									setTimeout(
										function() {
											$(window).bind('beforeunload', _onUnload);
										},
										0
									);
								},
								0
							);
						}
					);
				};

				if (response === 'NONE' || response === 'ERROR') {
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

			if (eventChannel !== chan.EXPORT_GRAPH_REQUEST ||
				_UIObjectState.singleton == null
			) {
				return;
			}

			var callback = function(response) {

				var exportCallback = function() {
					$.blockUI({
						theme: true,
						title: 'Export In Progress',
						message: '<img src="' + constants.AJAX_SPINNER_FILE + '" style="display:block;margin-left:auto;margin-right:auto"/>'
					});

					var exportState = _UIObjectState.singleton.saveState();

					xfRest.request('/export').withData({

						sessionId: _UIObjectState.sessionId,
						data: (exportState) ? JSON.stringify(exportState) : ''

					}).then(function (response) {

						var a = document.createElement('a');
						a.href = aperture.store.url(response, 'pop', 'influent-saved.infml');

						document.body.appendChild(a);

						setTimeout(
							function () {
								$(window).unbind('beforeunload');
								a.click();
								document.body.removeChild(a);
								$.unblockUI();
								setTimeout(
									function () {
										$(window).bind('beforeunload', _onUnload);
									},
									0
								);
							},
							0
						);
					});
				};

				if (response === 'NONE' || response === 'ERROR') {
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

		var _onImportGraph = function(eventChannel) {

			if (eventChannel !== chan.IMPORT_GRAPH_REQUEST ||
				_UIObjectState.singleton == null
			) {
				return;
			}

			aperture.pubsub.publish(chan.FILE_UPLOAD_REQUEST, {
				action : '/import',
				filter: '.infml',

				preCallback : function() {
					$.blockUI({
						theme: true,
						title: 'Import In Progress',
						message: '<img src="' + constants.AJAX_SPINNER_FILE + '" style="display:block;margin-left:auto;margin-right:auto"/>'
					});
				},
				postCallback : function(data) {

					setTimeout(
						function() {
							$.unblockUI();
						},
						0
					);

					var restoreState = JSON.parse(data);

					if(restoreState.message && !restoreState.ok) {
						aperture.log.error('Server Error' + (restoreState? (' : ' + JSON.stringify(restoreState)): ''));
						return;
					}

					var workspaceSpec = xfWorkspaceModule.getSpecTemplate();
					_UIObjectState.singleton = xfWorkspaceModule.createSingleton(workspaceSpec);

					_UIObjectState.singleton.cleanState();
					_UIObjectState.singleton.restoreVisualState(restoreState);
					_UIObjectState.singleton.restoreHierarchy(restoreState, _UIObjectState.singleton);

					_pruneColumns();

					_loadWorkspaceCallback();

					_checkPatternSearchState();
				}
			});

		};

		//--------------------------------------------------------------------------------------------------------------

		var _onNewWorkspace = function(eventChannel) {

			if (eventChannel !== chan.NEW_WORKSPACE_REQUEST) {
				return;
			}

			window.open(window.location.href, '_blank');
		};

        //--------------------------------------------------------------------------------------------------------------

        var _onScrollView = function(eventChannel, data) {

			if (eventChannel !== chan.SCROLL_VIEW_EVENT) {
				return;
			}

            // Handle logging of scrolling events
            var div = data.div;
            var divId = div.id;

            // Collate all scrolling events in a given time frame
            var collateEvery = 500; // ms

            var state = _UIObjectState.scrollStates[divId];
            if (!state) {
                state = {
                    topCurrent: 0,
                    leftCurrent: 0
                };
            }

            if (!state.timer) {

                state.timer = setTimeout(function () {

                    logActivity({
                        workflow : aperture.log.draperWorkflow.WF_EXPLORE,
                        description : 'Scrolling view',
                        data : {
                            view : divId,
                            topStart : state.topStart,
                            leftStart : state.leftStart,
                            topEnd : state.topCurrent,
                            leftEnd : state.leftCurrent
                        }
                    }, eventChannel);

                    state.timer = null;
                }, collateEvery);

                state.topStart = state.topCurrent;
                state.leftStart = state.leftCurrent;
                state.topCurrent = div.scrollTop;
                state.leftCurrent = div.scrollLeft;
            } else {
                state.topCurrent = div.scrollTop;
                state.leftCurrent = div.scrollLeft;
            }

            _UIObjectState.scrollStates[divId] = state;
        };

		//--------------------------------------------------------------------------------------------------------------

		var _onHighlightSearchArguments = function(eventChannel, data) {

			if (eventChannel !== chan.HIGHLIGHT_SEARCH_ARGUMENTS) {
				return;
			}

			var files = _getAllFiles();

			// highlight files
			aperture.util.forEach(files, function(file) {
				var hi = data.isHighlighted && file.getChildren().length !== 0;

				file.setMatchHighlighted(hi);
				aperture.pubsub.publish(chan.RENDER_UPDATE_REQUEST, {UIObject : file});
			});

			// highlight file links
			aperture.util.forEach(_UIObjectState.singleton.getLinks(), function(link) {
				if(link.getType() === xfLinkType.FILE) {
					var hi = data.isHighlighted
						&& link.getSource().getChildren().length !== 0
						&& link.getDestination().getChildren().length !== 0;

					link.setSelected(hi);
				}
			});

			// hmmm. heavyweight
			var layoutUpdateRequestMsg = _getLayoutUpdateRequestMsg();		// rerender the sankeys
			aperture.pubsub.publish(
				chan.UPDATE_SANKEY_EVENT,
				{
					workspace : layoutUpdateRequestMsg.layoutInfo.workspace,
					layoutProvider : layoutUpdateRequestMsg.layoutProvider
				}
			);
		};

		//--------------------------------------------------------------------------------------------------------------

		var _onUpdateCharts = function(eventChannel, data) {

			if (eventChannel !== chan.UPDATE_CHART_REQUEST ||
				_UIObjectState.singleton == null
			) {
				return;
			}

			var id = data.xfId;
			if (id) {
				var card = _UIObjectState.singleton.getUIObjectByXfId(id);
				if (card) {
					var specs = card.getSpecs();
					if (specs &&
						_UIObjectState.childModule != null
					) {
						_UIObjectState.childModule.updateCardsWithCharts(specs);
					}
				}
			}
		};

		//--------------------------------------------------------------------------------------------------------------

		var _onCurrentStateRequest = function(eventChannel) {

			if (eventChannel !== chan.REQUEST_CURRENT_STATE) {
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

			if (eventChannel !== chan.REQUEST_OBJECT_BY_XFID ||
				_UIObjectState.singleton == null
			) {
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

		var _getColumnByIndex = function(index) {
			if (0 <= _UIObjectState.children.length && index < _UIObjectState.children.length) {
				return _UIObjectState.children[index];
			} else {
				return null;
			}
		};

		//--------------------------------------------------------------------------------------------------------------

		// Returns the column that the UIObject belongs to.
		var _getColumnByUIObject = function(uiObject){
			var columnObj = xfUtil.getUITypeAncestor(uiObject, constants.MODULE_NAMES.COLUMN);
			if (columnObj == null){
				aperture.log.error('The UIObjectId: ' + uiObject.getXfId() + ' does not have a valid parent column.');
			}
			return columnObj;
		};

		//--------------------------------------------------------------------------------------------------------------

		// Returns the index of the given column if it
		// exists; otherwise returns -1.
		var _getColumnIndex = function(columnObj){
			for (var i=0; i < _UIObjectState.children.length; i++){
				var nextColumnObj = _UIObjectState.children[i];
				if (columnObj.getXfId() === nextColumnObj.getXfId()){
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
			return (columnIndex === -1 || nextColumn == null)? null : nextColumn;
		};

		//--------------------------------------------------------------------------------------------------------------

		// Returns the column to the left of the given
		// one, if it exists; otherwise returns NULL.
		var _getPrevColumn = function(columnObj){
			var columnIndex = _getColumnIndex(columnObj);
			var prevColumn = _UIObjectState.children[columnIndex-1];
			return (columnIndex === -1 || prevColumn == null)? null : prevColumn;
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

		var initModule = function(module){

			module.clone = function() {
				aperture.log.error('Unable to clone workspace');
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
				if (xfId != null && _UIObjectState.xfId === xfId) {
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
				aperture.log.error(MODULE_NAME + ': call to unimplemented method "collapseLinks".');
			};

			//----------------------------------------------------------------------------------------------------------

			module.remove = function() {
				// Workspaces cannot be removed, so we throw an error to indicate this.
				aperture.log.error(MODULE_NAME + ': call to unimplemented method "remove".');
			};

			//----------------------------------------------------------------------------------------------------------

			module.removeChild = function(xfId, disposeObject, preserveLinks, removeIfEmpty) {

				for (var i = 0; i < _UIObjectState.children.length; i++) {
					if (_UIObjectState.children[i].getXfId() === xfId) {

						_UIObjectState.children[i].setParent(null);

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

				if (_UIObjectState.children.length === 0) {
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
						if (_UIObjectState.children[i].getXfId() === beforeXfUIObj00.getXfId()) {
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
				return _UIObjectState != null ? _.clone(_UIObjectState) : null;
			};

			//----------------------------------------------------------------------------------------------------------

			module.getFocus = function(){
				var focus = _UIObjectState.focus;

				if (focus) {
					var focusUIObj = this.getUIObjectByXfId(focus.xfId);

					// update context, etc
					if (focusUIObj != null) {
						var contextObj = xfUtil.getContextByUIObject(focusUIObj) ;
						if (contextObj != null) {
							focus.contextId = contextObj.getXfId();
						}
					}
				}

				return focus;
			};

			//----------------------------------------------------------------------------------------------------------

			module.setFocus = function(data) {
				var dataXfId;
				if(data == null) {
					_UIObjectState.focus = null;
					dataXfId = null;
				}
				else {
					_UIObjectState.focus = _.clone(data);
					dataXfId = data.xfId;
				}

				for (var i = 0; i < _UIObjectState.children.length; i++) {
					_UIObjectState.children[i].highlightId(dataXfId);
				}
			};
			//----------------------------------------------------------------------------------------------------------

			module.isSelected = function() {
				// Workspaces cannot be selected, so we throw an error to indicate this.
				aperture.log.error(MODULE_NAME + ': call to unimplemented method "isSelected".');
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

			module.setHidden = function(xfId, state) {
				for (var i = 0; i < _UIObjectState.children.length; i++) {
					_UIObjectState.children[i].setHidden(xfId, state);
				}
			};

			//----------------------------------------------------------------------------------------------------------

			module.expand = function() {
				// Workspace objects cannot be expanded, so we throw an error to indicate this.
				aperture.log.error(MODULE_NAME + ': call to unimplemented method "expand".');
			};

			//----------------------------------------------------------------------------------------------------------

			module.collapse = function() {
				// Workspace objects cannot be collapsed, so we throw an error to indicate this.
				aperture.log.error(MODULE_NAME + ': call to unimplemented method "collapse".');
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
				_UIObjectState.focus = null;
				_UIObjectState.showDetails = false;
				_UIObjectState.dates = {startDate: '', endDate: '', numBuckets: 0, duration: ''};
			};

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
					if (state.children[i].UIType === constants.MODULE_NAMES.COLUMN) {
						var columnSpec = xfColumn.getSpecTemplate();
						var columnUIObj = _createColumn(columnSpec);
						columnUIObj.cleanState();
						columnUIObj.restoreVisualState(state.children[i]);
						this.insert(columnUIObj, null);
					} else {
						aperture.log.error('workspace children should only be of type ' + constants.MODULE_NAMES.COLUMN + '.');
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
			// Workspace specific functions
			//----------------------------------------------------------------------------------------------------------

			module.getContextIds = function() {

				var contextIds = [];
				for (var i = 0; i < _UIObjectState.children.length; i++) {

					var containedContextIds =  _UIObjectState.children[i].getContextIds();
					for (var j = 0; j < containedContextIds.length; j++) {
						contextIds.push(containedContextIds[j]);
					}
				}

				return contextIds;
			};

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
			return (dataElement.accounttype === constants.ACCOUNT_TYPES.ENTITY) ? 1 : dataElement['properties'].count.value;
		};

		//--------------------------------------------------------------------------------------------------------------

		xfWorkspaceModule.getValueByTag = function(dataElement, tag) {
			for (var propKey in dataElement.properties ) {
				if (dataElement.properties.hasOwnProperty(propKey)) {
					var property = dataElement.properties[propKey];
					if (property.tags) {
						for (var j = 0; j < property.tags.length; j++) {
							if (property.tags[j] === tag) {
								return property.value; // handles singleton values only
							}
						}
					}
				}
			}
			return null;
		};

		//--------------------------------------------------------------------------------------------------------------

		xfWorkspaceModule.getPropertiesByTag = function(dataElement, tag) {
			var ret = [];
			for (var propKey in dataElement.properties ) {
				if (dataElement.properties.hasOwnProperty(propKey)) {
					var property = dataElement.properties[propKey];
					if (property.tags) {
						for (var j = 0; j < property.tags.length; j++) {
							if (property.tags[j] === tag) {
								ret.push(property);
							}
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

		xfWorkspaceModule.start = function(sessionId, entityId) {

			_UIObjectState.sessionId = sessionId;
			_UIObjectState.initialEntityId = entityId;

			var subTokens = {};

			// Subscribe to state changes of interest
			subTokens[chan.ALL_MODULES_STARTED] = aperture.pubsub.subscribe(chan.ALL_MODULES_STARTED, _initializeModule);

			subTokens[chan.LOGOUT_REQUEST] = aperture.pubsub.subscribe(chan.LOGOUT_REQUEST, _pubsubHandler);
			subTokens[chan.SEARCH_REQUEST] = aperture.pubsub.subscribe(chan.SEARCH_REQUEST, _pubsubHandler);
			subTokens[chan.PATTERN_SEARCH_REQUEST] = aperture.pubsub.subscribe(chan.PATTERN_SEARCH_REQUEST, _pubsubHandler);
            subTokens[chan.ADVANCE_SEARCH_DIALOG_REQUEST] = aperture.pubsub.subscribe(chan.ADVANCE_SEARCH_DIALOG_REQUEST, _pubsubHandler);
            subTokens[chan.ADVANCE_SEARCH_DIALOG_CLOSE_EVENT] = aperture.pubsub.subscribe(chan.ADVANCE_SEARCH_DIALOG_CLOSE_EVENT, _pubsubHandler);
			subTokens[chan.DETAILS_CHANGE_REQUEST] = aperture.pubsub.subscribe(chan.DETAILS_CHANGE_REQUEST, _pubsubHandler);
			subTokens[chan.FILTER_CHANGE_REQUEST] = aperture.pubsub.subscribe(chan.FILTER_CHANGE_REQUEST, _pubsubHandler);
            subTokens[chan.FILTER_CHANGE_EVENT] = aperture.pubsub.subscribe(chan.FILTER_CHANGE_EVENT, _pubsubHandler);
			subTokens[chan.DROP_EVENT] = aperture.pubsub.subscribe(chan.DROP_EVENT, _pubsubHandler);
			subTokens[chan.FOCUS_CHANGE_REQUEST] = aperture.pubsub.subscribe(chan.FOCUS_CHANGE_REQUEST, _pubsubHandler);
			subTokens[chan.FOCUS_CHANGE_EVENT] = aperture.pubsub.subscribe(chan.FOCUS_CHANGE_EVENT, _pubsubHandler);
			subTokens[chan.SELECTION_CHANGE_REQUEST] = aperture.pubsub.subscribe(chan.SELECTION_CHANGE_REQUEST, _pubsubHandler);
			subTokens[chan.UI_OBJECT_HOVER_CHANGE_REQUEST] = aperture.pubsub.subscribe(chan.UI_OBJECT_HOVER_CHANGE_REQUEST, _pubsubHandler);
			subTokens[chan.CHANGE_FILE_TITLE] = aperture.pubsub.subscribe(chan.CHANGE_FILE_TITLE, _pubsubHandler);
			subTokens[chan.SEARCH_CONTROL_FOCUS_CHANGE_REQUEST] = aperture.pubsub.subscribe(chan.SEARCH_CONTROL_FOCUS_CHANGE_REQUEST, _pubsubHandler);
			subTokens[chan.SEARCH_BOX_CHANGED] = aperture.pubsub.subscribe(chan.SEARCH_BOX_CHANGED, _pubsubHandler);
			subTokens[chan.BRANCH_REQUEST] = aperture.pubsub.subscribe(chan.BRANCH_REQUEST, _pubsubHandler);
            subTokens[chan.BRANCH_RESULTS_RETURNED_EVENT] = aperture.pubsub.subscribe(chan.BRANCH_RESULTS_RETURNED_EVENT, _pubsubHandler);
			subTokens[chan.EXPAND_EVENT] = aperture.pubsub.subscribe(chan.EXPAND_EVENT, _pubsubHandler);
			subTokens[chan.COLLAPSE_EVENT] = aperture.pubsub.subscribe(chan.COLLAPSE_EVENT, _pubsubHandler);
			subTokens[chan.REMOVE_REQUEST] = aperture.pubsub.subscribe(chan.REMOVE_REQUEST, _pubsubHandler);
			subTokens[chan.CREATE_FILE_REQUEST] = aperture.pubsub.subscribe(chan.CREATE_FILE_REQUEST, _pubsubHandler);
			subTokens[chan.ADD_TO_FILE_REQUEST] = aperture.pubsub.subscribe(chan.ADD_TO_FILE_REQUEST, _pubsubHandler);
			subTokens[chan.SHOW_MATCH_REQUEST] = aperture.pubsub.subscribe(chan.SHOW_MATCH_REQUEST, _pubsubHandler);
            subTokens[chan.SEARCH_RESULTS_RETURNED_EVENT] = aperture.pubsub.subscribe(chan.SEARCH_RESULTS_RETURNED_EVENT, _pubsubHandler);
			subTokens[chan.PREV_SEARCH_PAGE_REQUEST] = aperture.pubsub.subscribe(chan.PREV_SEARCH_PAGE_REQUEST, _pubsubHandler);
			subTokens[chan.NEXT_SEARCH_PAGE_REQUEST] = aperture.pubsub.subscribe(chan.NEXT_SEARCH_PAGE_REQUEST, _pubsubHandler);
			subTokens[chan.SET_SEARCH_PAGE_REQUEST] = aperture.pubsub.subscribe(chan.SET_SEARCH_PAGE_REQUEST, _pubsubHandler);
			subTokens[chan.CLEAN_COLUMN_REQUEST] = aperture.pubsub.subscribe(chan.CLEAN_COLUMN_REQUEST, _pubsubHandler);
			subTokens[chan.EXPORT_CAPTURED_IMAGE_REQUEST] = aperture.pubsub.subscribe(chan.EXPORT_CAPTURED_IMAGE_REQUEST, _pubsubHandler);
			subTokens[chan.EXPORT_GRAPH_REQUEST] = aperture.pubsub.subscribe(chan.EXPORT_GRAPH_REQUEST, _pubsubHandler);
			subTokens[chan.IMPORT_GRAPH_REQUEST] = aperture.pubsub.subscribe(chan.IMPORT_GRAPH_REQUEST, _pubsubHandler);
            subTokens[chan.EXPORT_TRANSACTIONS_REQUEST] = aperture.pubsub.subscribe(chan.EXPORT_TRANSACTIONS_REQUEST, _pubsubHandler);
			subTokens[chan.NEW_WORKSPACE_REQUEST] = aperture.pubsub.subscribe(chan.NEW_WORKSPACE_REQUEST, _pubsubHandler);
            subTokens[chan.OPEN_EXTERNAL_LINK_REQUEST] = aperture.pubsub.subscribe(chan.OPEN_EXTERNAL_LINK_REQUEST, _pubsubHandler);
            subTokens[chan.FOOTER_CHANGE_DATA_VIEW_EVENT] = aperture.pubsub.subscribe(chan.FOOTER_CHANGE_DATA_VIEW_EVENT, _pubsubHandler);
			subTokens[chan.HIGHLIGHT_SEARCH_ARGUMENTS] = aperture.pubsub.subscribe(chan.HIGHLIGHT_SEARCH_ARGUMENTS, _pubsubHandler);
			subTokens[chan.REQUEST_CURRENT_STATE] = aperture.pubsub.subscribe(chan.REQUEST_CURRENT_STATE, _pubsubHandler);
			subTokens[chan.REQUEST_OBJECT_BY_XFID] = aperture.pubsub.subscribe(chan.REQUEST_OBJECT_BY_XFID, _pubsubHandler);
			subTokens[chan.SORT_COLUMN_REQUEST] = aperture.pubsub.subscribe(chan.SORT_COLUMN_REQUEST, _pubsubHandler);
			subTokens[chan.UPDATE_CHART_REQUEST] = aperture.pubsub.subscribe(chan.UPDATE_CHART_REQUEST, _pubsubHandler);
			subTokens[chan.TOOLTIP_START_EVENT] = aperture.pubsub.subscribe(chan.TOOLTIP_START_EVENT, _pubsubHandler);
            subTokens[chan.TOOLTIP_END_EVENT] = aperture.pubsub.subscribe(chan.TOOLTIP_END_EVENT, _pubsubHandler);
            subTokens[chan.HOVER_START_EVENT] = aperture.pubsub.subscribe(chan.HOVER_START_EVENT, _pubsubHandler);
            subTokens[chan.HOVER_END_EVENT] = aperture.pubsub.subscribe(chan.HOVER_END_EVENT, _pubsubHandler);
			subTokens[chan.SCROLL_VIEW_EVENT] = aperture.pubsub.subscribe(chan.SCROLL_VIEW_EVENT, _pubsubHandler);
            subTokens[chan.TRANSACTIONS_FILTER_EVENT] = aperture.pubsub.subscribe(chan.TRANSACTIONS_FILTER_EVENT, _pubsubHandler);
			subTokens[chan.TRANSACTIONS_PAGE_CHANGE_EVENT] = aperture.pubsub.subscribe(chan.TRANSACTIONS_PAGE_CHANGE_EVENT, _pubsubHandler);

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
