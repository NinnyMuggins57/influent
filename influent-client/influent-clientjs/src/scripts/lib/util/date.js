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
define(['lib/util/duration'], function(mDuration) {

	var months = ['January', 'February', 'March', 'April', 'May', 'June', 'July',
				  'August', 'September', 'October', 'November', 'December'
	],

	// some useful constants
	secondInMillis = 1000,
	minuteInMillis = secondInMillis * 60,
	hourInMillis   = minuteInMillis * 60,
	dayInMillis    = hourInMillis * 24,

	/**
	 * Private implementation : add (or subtract) duration to date and return new date
	 */
	add = function(date, duration, sign) {
		if (!duration) {
			return date;
		}
		if (!duration.tokens) {
			duration = mDuration.parse(duration);

			if (!duration) {
				return null;
			}
		}

		// clone the parameter
		date = new Date(date.getTime());

		var timePart = false;

		// process each part
		for (var p = 0; p < duration.tokens.length; p++) {
			var part = duration.tokens[p];

			if (part.symbol == 'T') {
				timePart = true;
				continue;
			}

			// else only process if it a valid and non-zero number.
			if (part.number) {
				switch (part.symbol) {
				case 'Y': // years
					// length of year varies but is unbounded so just set it in years
					date.setFullYear(date.getFullYear() + sign * part.number);
					break;

				case 'M':
					if (!timePart) { // months
						// month is the only trick bit because the duration of a month varies
						var m = date.getMonth()/*0-11*/ + sign * part.number;

						// handle rollover
						if (m > 11) {
							date.setFullYear(date.getFullYear() + Math.floor(m/12));
							m = m % 12;
						}
						else if (m < 0) {
							date.setFullYear(date.getFullYear() - Math.ceil(-m/12));
							m = 12 - ((-m) % 12);
						}

						date.setMonth(m);
					}
					else { // minutes
						date.setTime(date.getTime() + minuteInMillis * sign * part.number);
					}
					break;

				case 'D': // days
					date.setTime(date.getTime() + dayInMillis * sign * part.number);
					break;

				case 'H': // hours
					date.setTime(date.getTime() + hourInMillis * sign * part.number);
					break;

				case 'S': // seconds
					date.setTime(date.getTime() + secondInMillis * sign * part.number);
					break;
				}
			}
		}

		return date;
	};

	return {

		/**
		 * ISO date comparisons (yyyy-mm-dd)
		 *  1: d1 > d2
		 *  0: d1 = d2
		 * -1: d1 < d2
		 */
		compare : function (d1, d2) {
			d1 = $.trim(d1).split('-');
			d2 = $.trim(d2).split('-');

			if (d1[0] == d2[0]){
				if (d1[1] == d2[1])
					return d1[2] == d2[2] ? 0 : ((d1[2] > d2[2]) ? 1 : -1);
				else
					return (d1[1] > d2[1]) ? 1 : -1;
			}
			else
				return (d1[0] > d2[0]) ? 1 : -1;
		},

		toUTC : function (d) {
			var da = d.split('-');
			return Date.UTC(parseInt(da[0]), parseInt(da[1])-1, parseInt(da[2])+1, 0, 0, 0);
		},

		/**
		 * Formats in displayable form
		 */
		format : function( date, shortForm ) {

			if (shortForm) {
				var month = String((date.getMonth())+1);

				// Pad the month if it's only a single digit.
				if (month.length === 1){
					month = '0' + month;
				}
				var day = String(date.getDate());

				if (day.length === 1){
					day = '0' + day;
				}
				return date.getFullYear() + '-' + month + '-' + day;
			}

			return months[date.getMonth()] + ' ' +
				date.getDate() + ', ' +
				date.getFullYear();
		},

		/**
		 * Add duration to date and return new date
		 */
		after : function(date, duration) {
			return add(date, duration, 1);
		},

		/**
		 * Subtract duration from date and return new date
		 */
		before : function(date, duration) {
			return add(date, duration, -1);
		}

	};
});
