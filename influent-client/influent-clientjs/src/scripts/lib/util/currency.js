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
		formatNumber: function(number) {
			var suffix = '';

			number = Math.abs(number);
			
			if (number >= 1000000000) {
				suffix = 'B';
				number *= 0.000000001;
			} else if (number >= 1000000) {
				suffix = 'M';
				number *= 0.000001;
			} else if (number >= 1000) {
				suffix = 'K';
				number *= 0.001;
			}
			
			number = Math.round(number);
			
			var i,
				s = number.toString();
			
			for (i= s.length-3; i>0; i-= 3) {
				s = s.substring(0, i).concat(',').concat(s.substring(i));
			}
			
			return s + suffix;
		},
	
	
	
	
		formatBalance: function(number, prefix) {
			var suffix = '';
			var sign = (number < 0) ? '-' : '';
			
			number = Math.abs(number);
			
			if (number >= 1000000000) {
				suffix = 'B';
				number *= 0.000000001;
			} else if (number >= 1000000) {
				suffix = 'M';
				number *= 0.000001;
			} else if (number >= 1000) {
				suffix = 'K';
				number *= 0.001;
			}
			
			number = Math.round(number);
			if (number === 0) {
				sign = '';
			}
			
			var i,
				s = number.toString();
			
			for (i= s.length-3; i>0; i-= 3) {
				s = s.substring(0, i).concat(',').concat(s.substring(i));
			}
			
			return sign + prefix+ s + suffix;
		}
	};
});
