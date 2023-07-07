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

define(['lib/module', 'lib/constants', 'lib/extern/underscore'], function(modules, constants) {

	var statusDialog = (function() {
		var status = $('<div id="ajax-status">'
				+ '<img id="ajax-status-icon" src="' + constants.AJAX_SPINNER_FILE + '"/>'
				+ '<div id="ajax-status-message"></div>'
				+ '<div id="ajax-status-error" style="display: none;">'
				+ '<div id="ajax-status-retry"></div>'
				+ '<div id="ajax-status-ok"></div>'
				+ '<div id="ajax-status-reset"></div>'
				+ '</div></div>');

		// add the retry button
		var retryButton = $('<button type="button">Retry</button>').appendTo($('#ajax-status-retry', status));
		var okButton = $('<button type="button">Ok</button>').appendTo($('#ajax-status-ok', status));
		var that = {},

		mode = 'UNINITIALIZED',
		LOADING_MODE = 'loading',
		ERROR_MODE = 'error',

		initDialog = function(statusSpec) {

			// instantiate the dialog but don't open it yet.
			status.dialog({
				modal: true,
				autoOpen: false,
				draggable: false,
				resizable: false,
				close: statusSpec.closeFn
//				title: '<div style="font-weight: bold;">Load Status</div>'
			});

			var resetHTML = '<a href="'
				+ statusSpec.resetUrl
				+ '">' + statusSpec.resetLabel + '</a>';

			$('#ajax-status-reset', status).append(resetHTML);

			/**
			 * Setup up retry handler
			 */
			retryButton.click(statusSpec.retryFn ? statusSpec.retryFn : function(){
				if (status.dialog('isOpen')) {
					status.dialog('close');
				}
			});
			/**
			 * Setup up "ok" button handler
			 */
			okButton.click(statusSpec.okFn ? statusSpec.okFn : function() {
				status.dialog('close');
			});
		},

		/**
		 * Closes the dialog box.
		 * @param forceClose Forces the dialog box to close. If this flag is
		 * FALSE, the dialog will not close if an error is currently displayed.
		 * Since ajax calls are asynchronous, we don't want an error dialog to
		 * be closed by subsequent calls that are successful.
		 * We only ever want to force the dialog to close on an app state change
		 * (e.g. on a browser BACK/FORWARD).
		 */
		close = function(forceClose){
			if (forceClose || mode !== ERROR_MODE){
				mode = '';
				if (status.dialog('isOpen')) {
					status.dialog('close');
				}
			}
		},

		loading = function(message) {
			mode = LOADING_MODE;
			if (!status.dialog('isOpen')) {
				$('#ajax-status-message', status).html(message);
				$('#ajax-status-error', status).css('display', 'none');
				status.dialog('open');
			}
		},

		error = function(errorSpec) {
			mode = ERROR_MODE;
			// Display the reset link (e.g. navigate to the homepage)
			var allowReset = errorSpec.allowReset;
			if (_.isUndefined(allowReset)){
				allowReset = false;
			}

			$('#ajax-status-reset', status).css('display', allowReset? '' : 'none');

			var errorElement = $('#ajax-status-error', status);
			// if error part is hidden, show
			if (errorElement.css('display') == 'none') {
				errorElement.css('display', '');

				// but only ever show the first error that comes back,
				// even if we accumulate more.
				$('#ajax-status-message', status).html(errorSpec.message);
			}

			// Hide the retry button if appropriate.
			// Enable it by default.
			var allowRetry = errorSpec.allowRetry;
			if (_.isUndefined(allowRetry)){
				allowRetry = true;
			}

			var retryElement = $('#ajax-status-retry', status);
			var okElement = $('#ajax-status-ok', status);
			retryElement.css('display', allowRetry? '' : 'none');
			okElement.css('display', allowRetry? 'none' : '');

			// make sure the dialog is showing as well.
			if (!status.dialog('isOpen')) {
				status.dialog('open');
			}
		},
		isOpen = function(){
			return status.dialog('isOpen');
		},

		getMode = function(){
			return mode;
		};

		that.initDialog = initDialog;
		that.close = close;
		that.loading = loading;
		that.error = error;
		that.isOpen = isOpen;
		that.getMode = getMode;
		that.LOADING_MODE = LOADING_MODE;
		that.ERROR_MODE = ERROR_MODE;
		return that;
	})();
	return statusDialog;
});
