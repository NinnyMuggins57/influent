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

import java.util.List;

public class LedgerResult {

	final private Integer numColumns;
	final private Integer numRows;
	final private List<String> columnUnits;
	final private List<List<String>> tableData;
	final private Long totalRows;
	
	public LedgerResult(List<String> columnUnits, List<List<String>> tableData){
		this(columnUnits.size(), tableData.size(), columnUnits, tableData, (long)tableData.size());
	}
	
	public LedgerResult(Integer numColumns,
			Integer numRows,
			List<String> columnUnits,
			List<List<String>> tableData,
			Long totalRows){
		this.numColumns = numColumns;
		this.numRows = numRows;
		this.columnUnits = columnUnits;
		this.tableData = tableData;
		this.totalRows = totalRows;
	}
	
	public Integer getNumColumns() {
		return numColumns;
	}
	
	public Integer getNumRows() {
		return numRows;
	}
	
	public List<String> getColumnUnits() {
		return columnUnits;
	}
	
	public List<List<String>> getTableData() {
		return tableData;
	}
	
	public Long getTotalRows() {
		return totalRows;
	}
}
