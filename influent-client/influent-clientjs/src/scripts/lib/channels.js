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

/**
 * Defines the names of pub/sub channels used throughout the app
 */
define(
	{
		// Channel used to publish a request for a state change
		// Typically only a central coordinate will subscribe to this message
		// but many objects may publish it
		STATE_REQUEST : 'state-change-request',

		// Channel used to publish an actual validated state change
		// Typically only a central coordinator will publish this message but
		// many objects may subscribe to it
		STATE : 'state-change',

		// Channel used to publish notifications about changes to the
		// pending request state in the ajax request broker.  Generally, only
		// a broker should publish on this channel, many may listen
		AJAX_RESPONSE : 'ajax-response',

		// Channel used to notify that all modules have been started and can be initialized
		ALL_MODULES_STARTED : 'all-modules-started',

		// broadcast a logout request
		LOGOUT_REQUEST : 'logout',

		// Channel used to publish a footer state change request.
		FOOTER_STATE_REQUEST : 'footer-state-request',

		// called when the graph search workflow is active.
		GRAPH_SEARCH_STATE_CHANGE : 'graph-search-change',

		/* Channel used by xfHeader to request a view change
		 *
		 * data {
		 *      showDetails (boolean)
		 * }
		 */
		DETAILS_CHANGE_REQUEST : 'details-change-request',

		/* Channel used by xfWorkspace to inform xfHeader what the current view is
		 *
		 * data {
		 *      showDetails (boolean)
		 * }
		 */
		DETAILS_CHANGE_EVENT : 'details-change-event',

		/* Channel used by xfHeader to request a filter change
		 *
		 * data {
		 *      startDate, (string)
		 *      endDate, (string)
		 *      duration, (string)
		 *      numBuckets (number)
		 * }
		 */
		FILTER_CHANGE_REQUEST : 'filter-change-request',

		/* Channel used by xfWorkspace to inform xfHeader what the current filter criteria is
		 *
		 * data {
		 *      startDate, (string)
		 *      endDate, (string)
		 *      duration, (string)
		 *      numBuckets (number)
		 * }
		 */
		FILTER_CHANGE_EVENT : 'filter-change-event',


        /*
         * data {
         *    containerId : (string)
         *    cardId: (string)
         *    showSpinner: (boolean)
         * }
         */
		DROP_EVENT : 'drop-event',

        /*
         * data {
         *    xfId : (string)
         * }
         */
		FOCUS_CHANGE_REQUEST : 'focus-change-request',


        /*
         * data {
         *    xfId: (string)
         *    dataId: (string)
         *    entityType: (string)
         *    entityLabel: (string)
         *    entityCount: (number)
         *    contextId: (string)
         *    sessionId : (string)
         * }
         */
		FOCUS_CHANGE_EVENT : 'focus-change-event',

        /*
         * data {
         *    xfId: (string)
         *    selected: (boolean)
         *    noRender: (boolean)

         * }
         */
		SELECTION_CHANGE_REQUEST: 'selection-change-request',

        /*
         * data {
         *    xfId: (string)
         *    dataId: (string)
         *    label: (string)
         *    count: (number)
         *    uiType: (string)
         *    accountType: (string)
         *    contextId : (string)
         * }
         */
		SELECTION_CHANGE_EVENT : 'selection-change-event',

        /*
         * data {
         *    xfId: (string)
         * }
         */
		UI_OBJECT_HOVER_CHANGE_REQUEST : 'ui-object-hover-change-request',

		EXPORT_TRANSACTIONS_REQUEST : 'export-transactions',

        /*
         * data {
         *    xfId: (string)
         *    newTitleText: (string)
         *    noRender: (boolean)
         * }
         */
		CHANGE_FILE_TITLE : 'change-file-title',

        /*
         * data {
         *    xfId: (string)
         * }
         */
		SEARCH_CONTROL_FOCUS_CHANGE_REQUEST : 'search-control-focus-change-request',

        /*
         * data {
         *    xfId: (string)
         *    val: (string)
         *    noRender: (boolean)
         * }
         */
		SEARCH_BOX_CHANGED : 'search-box-changed',

        /*
         * data {
         *    xfId: (string)
         *    isHighlighted: (boolean)
         * }
         */
		HIGHLIGHT_SEARCH_ARGUMENTS : 'highlight-search-arguments',

		/* Channel used to publish a basic search request
		 *
		 * data {
		 *      xfId (string)
		 *      searchTerm (string)
		 * }
		 */
		SEARCH_REQUEST : 'basic-search-request',

        /*
         * data {
         *    fileId: (string)
         *    terms: (string)
         *    dataIds: (strings)
         *    contextId: (string)
         * }
         */
		ADVANCE_SEARCH_DIALOG_REQUEST : 'advance-search-dialog-request',

        ADVANCE_SEARCH_DIALOG_CLOSE_EVENT : 'advance-search-dialog-close-event',

		PATTERN_SEARCH_REQUEST : 'pattern-search-request',

        /*
         * data {
         *    xfId: (string)
         *    direction: (string)
         * }
         */
		BRANCH_REQUEST : 'branch-request',

        BRANCH_RESULTS_RETURNED_EVENT : 'branch-results-returned-event',

        /*
         * data {
         *    xfId: (string)
         * }
         */
		EXPAND_EVENT : 'expand-event',

        /*
         * data {
         *    xfId: (string)
         * }
         */
		COLLAPSE_EVENT : 'collapse-event',

		SUSPEND_RENDERING_REQUEST : 'suspend-rendering-request',

		RESUME_RENDERING_REQUEST : 'resume-rendering-request',

        /*
         * data {
         *    UIObject: (string)
         * }
         */
		RENDER_UPDATE_REQUEST : 'render-update-request',

		/* Channel used to publish a request to remove an object
		 *
		 * data {
		 *      xfId (string)
		 * }
		 */
		REMOVE_REQUEST: 'remove-request',

		/* Channel used to publish a request to re-calculate all elements in the scene
		 *  and update the endpoints of all Sankey flows.
		 * data {
			*      workspace (xfWorkspace)
			*      layoutProvider (xfLayoutProvider)
			* }
		 */
		UPDATE_SANKEY_EVENT : 'update-sankey-event',

		/* Channel used to publish a request to create a new file around the
		 * given uiObject.
		 * data {
		 *     xfId (string)
		 * }
		 */
		CREATE_FILE_REQUEST : 'create-file-request',

		/* Channel used to publish a request the re-parenting of a search result
		 * from an xfMatch to the associated parent xfFile.
		 * data {
		 *     xfId (string),
		 *     fileId (sting)
		 * }
		 */
		ADD_TO_FILE_REQUEST : 'add-to-file-request',

		/* Channel used to publish a request that an xfFile object display
		 * its associated xfMatch object and its search controls.
		 * data {
		 *     xfId (string)
		 * }
		 */
		SHOW_MATCH_REQUEST : 'show-match-request',

		ADD_DROP_TARGETS : 'add-drop-targets',

		REMOVE_DROP_TARGETS : 'remove-drop-targets',

		UPDATE_DROP_TARGETS : 'update-drop-targets',

		RESTORE_STATE : 'restore-state',

        /*
         * data {
         *    xfId: (string)
         * }
         */
		PREV_SEARCH_PAGE_REQUEST : 'prev-search-page-request',

        /*
         * data {
         *    xfId: (string)
         * }
         */
		NEXT_SEARCH_PAGE_REQUEST : 'next-search-page-request',

        /*
         * data {
         *    xfId: (string)
         *    page: (number)
         *    noRender: (boolean)
         * }
         */
		SET_SEARCH_PAGE_REQUEST : 'set-search-page-request',

        /*
         * data {
         *    xfId: (string)
         * }
         */
		CLEAN_COLUMN_REQUEST : 'clean-column-request',

        /*
         * data {
         *    xfId: (string)
         *    sortDescription: (string)
         *    sortFunction: (function)
         * }
         */
		SORT_COLUMN_REQUEST : 'sort-column-request',

		EXPORT_CAPTURED_IMAGE_REQUEST : 'export-captured-image-request',

		EXPORT_GRAPH_REQUEST : 'export-graph-request',

		IMPORT_GRAPH_REQUEST : 'import-graph-request',

		NEW_WORKSPACE_REQUEST : 'new-workspace-request',

        /*
         * data {
         *    link: (string)
         * }
         */
        OPEN_EXTERNAL_LINK_REQUEST : 'open-external-link-request',

        /*
         * data {
         *    tab: (string)
         * }
         */
        FOOTER_CHANGE_DATA_VIEW_EVENT : 'footer-change-data-view-event',

        /*
         * data {
         *    action: (string)
         *    filter: (string)
         *    preCallback: (function)
         *    postCallback: (function)
         * }
         */
		FILE_UPLOAD_REQUEST : 'file-upload-request',

		/* Channel used to publish a request a change in the pinned state of a card or cluster
		 * data {
		 *     xfId (string)
		 * }
		 */
		REQUEST_CURRENT_STATE : 'request-current-state',

		CURRENT_STATE : 'current-state',

        /*
         * data {
         *    xfId: (string)
         * }
         */
		REQUEST_OBJECT_BY_XFID : 'request-object-by-xfid',

        /*
         * data {
         *    clonedObject: (string)
         * }
         */
		OBJECT_FROM_XFID : 'object-from-xfid',

        /*
         * data {
         *    xfId: (string)
         * }
         */
		UPDATE_CHART_REQUEST : 'update-chart-request',

        /*
         * data {
         *    element: (string)
         * }
         */
		TOOLTIP_START_EVENT : 'tooltip-start-event',

        /*
         * data {
         *    element: (string)
         * }
         */
        TOOLTIP_END_EVENT : 'tooltip-end-event',

        /*
         * data {
         *    element: (string)
         * }
         */
        HOVER_START_EVENT : 'hover-start-event',

        /*
         * data {
         *    element: (string)
         * }
         */
        HOVER_END_EVENT : 'hover-end-event',

        /*
         * data {
         *    div: (string)
         * }
         */
        SCROLL_VIEW_EVENT : 'scroll-view-event',

        /*
         * data {
         *    filterHighlighted: (boolean)
         * }
         */
        TRANSACTIONS_FILTER_EVENT : 'transactions-filter-event',

		TRANSACTIONS_PAGE_CHANGE_EVENT : 'transactions-page-change-event',

        /*
         * data {
         *    shownMatches: (number)
         *    totalMatches: (number)
         * }
         */
        SEARCH_RESULTS_RETURNED_EVENT : 'search-results-returned-event',

		SEND_REST_REQUEST : 'queue-rest-request'
	}
);
