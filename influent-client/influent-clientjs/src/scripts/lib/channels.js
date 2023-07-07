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
define({

	// Channel used to publish a request for a state change
	// Typically only a central coordinate will subscribe to this message
	// but many objects may publish it
	STATE_REQUEST : "state-change-request",

	// Channel used to publish an actual validated state change
	// Typically only a central coordinator will publish this message but
	// many objects may subscribe to it
	STATE : "state-change",

	// Channel used to publish notifications about changes to the
	// pending request state in the ajax request broker.  Generally, only
	// a broker should publish on this channel, many may listen
	AJAX_RESPONSE : "ajax-response",

    // Channel used to notify that all modules have been started and can be initialized
    ALL_MODULES_STARTED : 'all-modules-started',

    // broadcast a logout request
    LOGOUT_REQUEST : 'logout',

    // Channel used to publish a footer state change.
    FOOTER_STATE : "footer-state",

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
     *      start, (string)
     *      end, (string)
     *      duration, (string)
     *      numBuckets (number)
     * }
     */
	FILTER_CHANGE_REQUEST : 'filter-change-request',

    /* Channel used by xfWorkspace to inform xfHeader what the current filter criteria is
     *
     * data {
     *      start, (string)
     *      end, (string)
     *      duration, (string)
     *      numBuckets (number)
     * }
     */
    FILTER_CHANGE_EVENT : 'filter-change-event',

	DROP_EVENT : 'drop-event',
    FOCUS_CHANGE_REQUEST : 'focus-change-request',
    FOCUS_CHANGE_EVENT : 'focus-change-event',
    SELECTION_CHANGE_REQUEST: 'selection-change-request',
    SELECTION_CHANGE_EVENT : 'selection-change-event',
    HOVER_CHANGE_REQUEST : 'hover-change-request',
    EXPORT_TRANSACTIONS : 'export-transactions',
    CHANGE_FILE_TITLE : 'change-file-title',
    SEARCH_CONTROL_FOCUS_CHANGE_REQUEST : 'search-control-focus-change-request',
    SEARCH_BOX_CHANGED : 'search-box-changed',
    APPLY_PATTERN_SEARCH_TERM : 'apply-pattern-search-term',
    HIGHLIGHT_SEARCH_ARGUMENTS : 'highlight-search-arguments',

    /* Channel used to publish a basic search request
     *
     * data {
     *      xfId (string)
     *      searchTerm (string)
     * }
     */
    SEARCH_REQUEST : 'basic-search-request',
    ADVANCE_SEARCH_DIALOG_REQUEST : 'advance-search-dialog-request',

    BRANCH_LEFT_EVENT : "branch-left-event",
    BRANCH_RIGHT_EVENT : "branch-right-event",

    EXPAND_EVENT : 'expand-event',
    COLLAPSE_EVENT : 'collapse-event',

    SUSPEND_RENDERING : 'suspend-rendering',
    RESUME_RENDERING : 'resume-rendering',
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

    ADD_DROP_TARGETS : 'add_drop_targets',
    
    REMOVE_DROP_TARGETS : 'remove-drop-targets',
    
    UPDATE_DROP_TARGETS : 'update-drop-targets',
    
    SHOW_MATCH_REQUEST : 'show-match-request',

    RESTORE_STATE : 'restore-state',

    PREV_SEARCH_PAGE_REQUEST : 'prev-search-page-request',

    NEXT_SEARCH_PAGE_REQUEST : 'next-search-page-request',

    SET_SEARCH_PAGE_REQUEST : 'set-search-page-request',

    CLEAN_COLUMN_REQUEST : 'clean-column-request',

    SORT_COLUMN_REQUEST : 'sort-column-request',

    EXPORT_CAPTURED_IMAGE_REQUEST : 'export-captured-image-request',

    EXPORT_GRAPH_REQUEST : 'export-graph-request',
    
    NEW_WORKSPACE_REQUEST : 'new-workspace-request',

    /* Channel used to publish a request a change in the pinned state of a card or cluster
     * data {
     *     xfId (string)
     * }
     */
    REQUEST_CURRENT_STATE : 'request-current-state',
    CURRENT_STATE : 'current-state',

    REQUEST_OBJECT_BY_XFID : 'request-object-by-xfid',
    OBJECT_FROM_XFID : 'object-from-xfid'
});
