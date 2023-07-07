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

 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.

 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package influent.server.utilities;

import influent.idl.FL_DateInterval;
import influent.idlhelper.DateRangeHelper;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;

/**
 * @author djonker
 *
 */
public class ConstrainedDateRange extends DateRangeHelper {

	
	/**
	 * Constructs a date range that is constrained to begin and end on instances of the specified
	 * interval, and that is guaranteed to include the range specified.
	 * 
	 * @param start
	 * @param interval
	 * @param numBins
	 */
	public ConstrainedDateRange(DateTime start, FL_DateInterval interval, long numBins) {
		super(start.getMillis(), interval, numBins);

		DateTime constrained= start;
		
		// constrain to start of interval.
		switch (interval) {
		
		case SECONDS:
			constrained = new DateTime(start.getYear(), start.getMonthOfYear(), start.getDayOfMonth(), start.getHourOfDay(), start.getMinuteOfHour(), start.getSecondOfMinute(), DateTimeZone.UTC);
			break;
			
		case HOURS:
			constrained = new DateTime(start.getYear(), start.getMonthOfYear(), start.getDayOfMonth(), start.getHourOfDay(), 0, DateTimeZone.UTC);
			break;
			
		case DAYS:
			constrained = new DateTime(start.getYear(), start.getMonthOfYear(), start.getDayOfMonth(), 0, 0, DateTimeZone.UTC);
			break;
			
		case WEEKS:
			constrained = new DateTime(start.getYear(), start.getMonthOfYear(), start.getDayOfMonth(), 0, 0, DateTimeZone.UTC);
			
			final int days = start.getDayOfWeek() % 7;
			final long rewindMillis = days * DateTimeConstants.MILLIS_PER_DAY;

			constrained = new DateTime(constrained.getMillis() - rewindMillis, constrained.getZone());
			
			break;
			
		case MONTHS:
			constrained = new DateTime(start.getYear(), start.getMonthOfYear(), 1, 0, 0, DateTimeZone.UTC);
			break;
			
		case QUARTERS:
			constrained = new DateTime(start.getYear(), 1 + 3*((start.getMonthOfYear() - 1)/3), 1, 0, 0, DateTimeZone.UTC);
			break;
			
		case YEARS:
			constrained = new DateTime(start.getYear(), start.getMonthOfYear(), 1, 0, 0, DateTimeZone.UTC);
			break;
		}
		
		// add an extra partial interval at the end when not large enough.
		if (!start.equals(constrained)) {
			//System.out.println(start + " -> " + constrained);

			setStartDate(constrained.getMillis());
			setNumBins(numBins + 1);
		}
	}

}
