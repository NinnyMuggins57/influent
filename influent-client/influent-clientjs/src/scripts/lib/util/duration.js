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
 * duration is specified in the following form "PnYnMnDTnHnMnS" derived
 * from the XSD data type specification, where:
 *
 *   P indicates the period (required)
 *   nY indicates the number of years
 *   nM indicates the number of months
 *   nD indicates the number of days
 *   T indicates the start of a time section (required if you are going to specify hours, minutes, or seconds)
 *   nH indicates the number of hours
 *   nM indicates the number of minutes
 *   nS indicates the number of seconds
 *
 *   Examples:
 *   <period>P5Y</period>
 *   <period>P5Y2M10D</period>
 *
 *   TODO: consider making this an object?
 *
 */
define([], function() {
	var that = {},

	// some useful constants
	secondInMillis = 1000,
	minuteInMillis = secondInMillis * 60,
	hourInMillis   = minuteInMillis * 60,
	dayInMillis    = hourInMillis * 24;

    //------------------------------------------------------------------------------------------------------------------

	/**
	 * Private implementation : add (or subtract) duration to date and return new date
	 */
	var toDate = function(duration, date, sign) {
		if (typeof duration != 'string' || !date) {
			if (!date)
				aperture.log.error('Invalid date object in addToDate: '+ date);
			else
				aperture.log.error('Invalid duration format in addToDate: ' + duration);

			return date;
		}

		duration = duration.toUpperCase();

		// first character is supposed to be a P
		if (duration.charAt(0) != 'P') {
			aperture.log.error('Invalid duration format in addToDate: ' + duration);
			return date;
		}


		// clone the parameter
		date = new Date(date.getTime());


		var tokens = [];
		var startNum = 1;

		// first tokenize the duration into parts
		for (var i= 1; i< duration.length; i++) {
			var unicode = duration.charCodeAt(i);

			// a number?
			if (unicode >= 48 && unicode <= 57)
				continue;

			// else we hit a symbol.
			// get number part
			var number = (i > startNum)?
					parseInt(duration.substring(startNum, i)) : 0;

			// get symbol part
			tokens.push({
				'symbol' : duration.charAt(i),
				'number' : number
				});

			startNum = i+1;
		}


		var timePart = false;

		// process each part
		for (var p=0; p< tokens.length; p++) {
			var part = tokens[p];

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
							if (m === 12) {
								m = 0;
							}
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

    //------------------------------------------------------------------------------------------------------------------

	/**
	 * Add duration to date and return new date
	 */
	var addToDate = function(duration, date) {
		return toDate(duration, date, 1);
	};

    //------------------------------------------------------------------------------------------------------------------

	/**
	 * Subtract duration from date and return new date
	 */
	var subtractFromDate = function(duration, date) {
		return toDate(duration, date, -1);
	};

    //------------------------------------------------------------------------------------------------------------------

    var roundDateByDuration = function() {
        function iterByDay(date, direction) {
            date.setTime( date.getTime() + ((direction > 0) ? 86400000 : -86400000) );
        }
        function iterByMonth(date, direction) {
            if(direction > 0) {
                if(date.getMonth() == 11) {
                    date.setYear(date.getFullYear() + 1);
                }

                date.setMonth((date.getMonth() + 1)%12);
            }
            else {
                if(date.getMonth() == 0) {
                    date.setYear(date.getFullYear() - 1);
                }

                date.setMonth((date.getMonth() - 1)%12);
            }
        }

        function roundByIteration(date, duration, iterator) {
            if(isDateValidForDuration(date, duration)) {
                return date;
            }

            //var dateIterator = iterators[duration];
            var pastDate = new Date(date.getTime());
            var futureDate = new Date(date.getTime());

            while(!isDateValidForDuration(pastDate, duration) && !isDateValidForDuration(futureDate, duration)) {
                iterator(pastDate,  -1);
                iterator(futureDate, 1);
            }

            if(isDateValidForDuration(pastDate, duration) && isDateValidForDuration(futureDate, duration)) {
                if(date.getTime() - pastDate.getTime() < futureDate.getTime() - date.getTime()) {
                    return pastDate;
                }
                else {
                    return futureDate;
                }
            }
            else if(isDateValidForDuration(pastDate, duration)) {
                return pastDate;
            }
            else {
                return futureDate;
            }
        }

        var iterators = {
            'P14D': iterByDay,
            'P30D': iterByDay,
            'P60D': iterByDay,
            'P112D': iterByDay,
            'P224D': iterByDay,
            'P1Y': iterByMonth,
            'P16M': iterByMonth,
            'P2Y': iterByMonth,
            'P32M': iterByMonth,
            'P4Y': iterByMonth,
            'P8Y': iterByMonth,
            'P16Y': iterByMonth
        };

        return function(date, duration) {
        	
        	// start by stripping time.
        	date = new Date(date.getFullYear(), date.getMonth(), date.getDate());
        	
            if(isDateValidForDuration(date, duration)) {
                return date;
            }

            // need to 'clean' the input date before rounding by iteration will work
            // basically, this involves rounding at a finer resolution.
            switch(duration) {
                case 'P1Y':
                case 'P16M':
                case 'P2Y':
                case 'P32M':
                case 'P4Y':
                case 'P8Y':
                case 'P16Y':
                    date = roundByIteration(date, 'P1Y', iterByDay);  // iterByMonth requires dates centered by forMonth
                    break;
            }

            return roundByIteration(date, duration, iterators[duration]);
        };
    }();

    //------------------------------------------------------------------------------------------------------------------

    var isDateValidForDuration = function() {
        function forDay(date) {
            return date.getHours() === 0 && date.getMinutes() === 0 && date.getSeconds() === 0 && date.getMilliseconds() === 0;
        }
        function forWeek(date) {
            return forDay(date) && date.getDay() === 0;
        }
        function forMonth(date) {
            return forDay(date) && date.getDate() === 1;
        }
        function forQuarter(date) {
            return forDay(date) && date.getDate() === 1 && date.getMonth() %3 === 0;
        }
        function forYear(date) {
            return forDay(date) && date.getMonth() === 0 && date.getDate() === 1;
        }
        var validators = {
            'P14D': forDay,
            'P30D': forDay,
            'P60D': forDay,
            'P112D': forWeek,
            'P224D': forWeek,
            'P1Y': forMonth,
            'P16M': forMonth,
            'P2Y': forMonth,
            'P32M': forQuarter,
            'P4Y': forQuarter,
            'P8Y': forQuarter,
            'P16Y': forYear
        };

        return function(date, duration) {
            return validators[duration](date);
        };
    }();

    //------------------------------------------------------------------------------------------------------------------

	/**
	 * Private fn that expands a part and returns the result
	 */
	var expandPart = function(part, symbol, longForm, adjective, delimiter) {

		// strip out any non meaningful values.
		part = part.replace(new RegExp('([^0-9])0+' + symbol), '$1');

		// special handling for values of one, which are never pluralized
		// even in noun form.
		if (!adjective) {
			part = part.replace(new RegExp('^(0*1)' + symbol), '$1 ' + longForm + delimiter);
			part = part.replace(new RegExp('([^0-9]0*1)' + symbol), '$1 ' + longForm + delimiter);
		}

		// normal handling.
		part = part.replace(new RegExp('([0-9]+)' + symbol), '$1 ' + longForm + (adjective? '' : 's') + delimiter);

		return part;
	};

    //------------------------------------------------------------------------------------------------------------------

	/**
	 * Formats a coded duration string for user consumption, according to
	 * requested specifiers.
	 *
	 * Optional specifiers include:
	 *
	 *   string form: { 'symbol' | 'noun' (default) | 'adjective'}
	 */
	var format = function (duration, spec) {
		var form = spec? spec.form : null;

		var longForm  = form != 'symbol';
		var adjective = form == 'adjective';

		if (typeof duration == 'string') {
			var formatted = duration.toUpperCase();

			if (formatted.charAt(0) == 'P') {
				formatted = formatted.substr(1);
			}

			if (longForm) {
				var delimiter = ', ';
				var timeSection = '';
				var dateSection = formatted;
				var startOfTimeSection = formatted.indexOf('T');

				// found a time part?
				if (startOfTimeSection > -1) {
					dateSection = formatted.substring(0, startOfTimeSection);

					if (formatted.length > startOfTimeSection + 1) {
						timeSection = formatted.substring(startOfTimeSection+1);

						timeSection = expandPart(timeSection, 'H', 'Hour', adjective, delimiter);
						timeSection = expandPart(timeSection, 'M', 'Minute', adjective, delimiter);
						timeSection = expandPart(timeSection, 'S', 'Second', adjective, delimiter);
					}
				}

				dateSection = expandPart(dateSection, 'Y', 'Year', adjective, delimiter);
				dateSection = expandPart(dateSection, 'M', 'Month', adjective, delimiter);
				dateSection = expandPart(dateSection, 'D', 'Day', adjective, delimiter);

				formatted = dateSection + timeSection;

				if (formatted.length <= delimiter.length) {
					formatted = adjective? 'Immediate' : 'None';
				}
				else if (formatted.substring(formatted.length - delimiter.length) == delimiter) {
					formatted = formatted.substring(0, formatted.length - delimiter.length);
				}
			}

			return formatted;
		}

		return longForm? '0 Day' : '0D';
	};

    //------------------------------------------------------------------------------------------------------------------

	that.format = format;
	that.addToDate = addToDate;
	that.subtractFromDate = subtractFromDate;
    that.roundDateByDuration = roundDateByDuration;
    that.isDateValidForDuration = isDateValidForDuration;

	return that;
});

