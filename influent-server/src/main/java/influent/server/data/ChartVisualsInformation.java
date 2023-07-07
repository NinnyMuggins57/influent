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



public class ChartVisualsInformation {


	private final Double maxCredit;
	private final Double maxDebit;
	private final Double totalCredit;
	private final Double totalDebit;
	private final Double currentBalance;
	private final Double startingBalance;
	private final String imageHash;
	
	
	
	
	public ChartVisualsInformation(
		Double maxCredit,
		Double maxDebit,
		Double totalDebit,
		Double totalCredit,
		Double startingBalance,
		Double currentBalance,
		String imageHash
	) {
		this.maxCredit = maxCredit;
		this.maxDebit = maxDebit;
		this.totalDebit = totalDebit;
		this.totalCredit = totalCredit;
		this.startingBalance = startingBalance;
		this.currentBalance = currentBalance;
		this.imageHash = imageHash;
	}
	
	
	
	
	public Double getMaxCredit() {
		return this.maxCredit;
	}
	
	
	
	
	public Double getMaxDebit() {
		return this.maxDebit;
	}
	
	public Double getTotalCredit() { 
		return this.totalCredit;
	}
	
	public Double getTotalDebit() { 
		return this.totalDebit;
	}
	
	
	
	
	public Double getBalance() {
		return this.currentBalance;
	}
	
	
	public Double getStartingBalance() {
		return startingBalance;
	}




	public String getImageHash() {
		return this.imageHash;
	}
}
