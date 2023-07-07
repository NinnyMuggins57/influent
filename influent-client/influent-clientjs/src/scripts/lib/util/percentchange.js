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
define([], function() {

	return {

		/**
		 * Calculates and returns a percent change from two values
		 */
		calc : function (from, to) {
			if (from == null || to == null) {
				return 0;
			}

			var chg= from === 0? (to === 0? 0 : (to > 0? 1000 : -1000))
					: (100* (to - from) / Math.abs(from));

			return chg;
		},

		/**
		 * Returns true if pct change is nominal (very close to zero).
		 */
		isNominal : function (pctChange) {
			return (pctChange && Math.abs(pctChange).toFixed(1) == '0.0');
		},

		/**
		 * Formats a percent change with consistent treatment
		 */
		format : function (pctChange) {
			if (this.isNominal(pctChange)) {
				return '~UNCH';
			}
			if (!pctChange) {
				return 'UNCH';
			}

			// either one or zero decimals, that's it.
			var s = pctChange.toFixed(Math.abs(pctChange) < 1? 1 : 0);

			// if positive
			if (pctChange > 0) {
				if (pctChange > 1000)
					return '+1000+%';

				return '+' + s + '%';
			}

			if (pctChange < -1000)
				return '-1000+%';

			return s + '%';
		}
	};
});
