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
package influent.server.spi;

import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * We provide two methods of configuring connection details
 * and both are currently valid options. This is one of them.
 * 
 * @author djonker
 */
public class SQLServerConnectionUrl implements SQLConnectionUrl {
	
	private String  serverName;
	private Integer portNumber;
	private String  databaseName;


	public SQLServerConnectionUrl() {
	}
	public SQLServerConnectionUrl(String serverName, Integer portNumber, String databaseName) {
		this.serverName = serverName;
		this.portNumber = portNumber;
		this.databaseName = databaseName;
	}
	
	/**
	 * Optional guice injections.
	 */
	@Inject(optional=true)
	public void setServerName(@Named("influent.midtier.server.name") String serverName) {
		this.serverName = serverName;
	}
	@Inject(optional=true)
	public void setPort(@Named("influent.midtier.server.port") Integer portNumber) {
		this.portNumber = portNumber;
	}
	@Inject(optional=true)
	public void setDatabaseName(@Named("influent.midtier.database.name") String databaseName) {
		this.databaseName = databaseName;
	}
	
	
	/* (non-Javadoc)
	 * @see influent.server.spi.SQLConnectionUrl#getUrl()
	 */
	@Override
	public String getUrl() {
		if (!isValid()) {
			throw new IllegalArgumentException(
				"Invalid MSSQL url: serverName=" + serverName +
				" portNumber=" + portNumber +
				" databaseName=" + databaseName
			);
		}
		
		return formUrl(serverName, portNumber, databaseName);
	}

	/* (non-Javadoc)
	 * @see influent.server.spi.SQLConnectionUrl#getDriver()
	 */
	@Override
	public String getDriver() {
		return "net.sourceforge.jtds.jdbc.Driver";
	}
	
	/* (non-Javadoc)
	 * @see influent.server.spi.SQLConnectionUrl#isValid()
	 */
	@Override
	public boolean isValid() {
		return serverName != null && portNumber != null && databaseName != null;
	}
	
	/**
	 * Forms a SQLServer connection url from the parameters given.
	 */
	public static String formUrl(String serverName, Integer portNumber, String databaseName) {
		return "jdbc:jtds:sqlserver://" + 
				serverName + 
				":" + 
				portNumber + 
				";databaseName=" + 
				databaseName + 
				";selectMethod=cursor;";
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return formUrl(serverName, portNumber, databaseName);
	}
	
	
}
