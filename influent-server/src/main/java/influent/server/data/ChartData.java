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
package influent.server.data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

public class ChartData implements Serializable{

	private static final long serialVersionUID = 4784998023567326099L;
	
	private final Double startValue;
	private final Double endValue;
	private final List<Double> credits;
	private final List<Double> debits;
	private final Double maxCredit;
	private final Double maxDebit;
	private final Double maxBalance;
	private final Double minBalance;
	private final String units;
	private final List<Double> focusCredits;
	private final List<Double> focusDebits;
	private final List<Date> dates;
	private final String imageHash;
	
	public ChartData(
		Double startValue,
		Double endValue,
		String units,
		List<Double> credits,
		List<Double> debits,
		Double maxCredit,
		Double maxDebit,
		Double maxBalance,
		Double minBalance,
		List<Double> focusCredits,
		List<Double> focusDebits,
		List<Date> dates,
		String imageHash
	) {
		this.startValue = startValue;
		this.endValue = endValue;
		this.units = units;
		this.credits = credits;
		this.debits = debits;
		this.maxCredit = maxCredit;
		this.maxDebit = maxDebit;
		this.maxBalance = maxBalance;
		this.minBalance = minBalance;
		this.focusCredits = focusCredits;
		this.focusDebits = focusDebits;
		this.dates = dates;
		this.imageHash = imageHash;
	}

	public String getUnits() {
		return this.units;
	}
		
	public Double getStartValue() {
		return this.startValue;
	}
	
	public Double getEndValue() {
		return this.endValue;
	}
	
	public List<Double> getCredits() {
		return this.credits;
	}
	
	public List<Double> getDebits() {
		return this.debits;
	}
	
	public Double getMaxCredit() {
		return this.maxCredit;
	}
	
	public Double getMaxDebit() {
		return this.maxDebit;
	}
	
	public Double getMaxBalance() {
		return this.maxBalance;
	}
	
	public Double getMinBalance() {
		return this.minBalance;
	}
	
	public List<Double> getFocusCredits() {
		return this.focusCredits;
	}
	
	public List<Double> getFocusDebits() {
		return this.focusDebits;
	}	
	
	public Double getTotalCredits() { 
		Double ret = 0.0;
		for (int i = 0; i < this.credits.size(); i++) {
			ret += this.credits.get(i);
		}
		return ret;
	}
	
	public Double getTotalDebits() { 
		Double ret = 0.0;
		for (int i = 0; i < this.debits.size(); i++) {
			ret += this.debits.get(i);
		}
		return ret;
	}
	
	public List<Date> getDates() {
		return this.dates;
	}
	
	public String getImageHash() {
		return this.imageHash;
	}
}
