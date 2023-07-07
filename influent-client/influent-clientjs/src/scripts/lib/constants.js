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
		MODULE_NAMES : {
			WORKSPACE : 'xfWorkspace',
			COLUMN : 'xfColumn',
			FILE : 'xfFile',
			ENTITY : 'xfEntity',
			CLUSTER_BASE : 'xfClusterBase',
			IMMUTABLE_CLUSTER : 'xfImmutableCluster',
			MUTABLE_CLUSTER : 'xfMutableCluster',
			SUMMARY_CLUSTER : 'xfSummaryCluster',
			MATCH : 'xfMatch',
			LINK : 'xfLink'
		},
		ACCOUNT_TYPES : {
			CLUSTER : 'cluster',
			CLUSTER_SUMMARY : 'cluster_summary',
			ACCOUNT_OWNER : 'account_owner',
			ENTITY : 'entity',
			FILE : 'file'
		},
		AJAX_SPINNER_FILE : 'img/ajax-loader.gif',
		AJAX_SPINNER_BG	: 'url("img/ajax-loader.gif") no-repeat center center',
		AJAX_SPINNER_LARGE_BG	: 'url("img/ajax-loader-large.gif") no-repeat center center',
		SORT_FUNCTION : {
			INCOMING : 'incoming',
			OUTGOING : 'outgoing',
			BOTH : 'both'
		}
	}
);
