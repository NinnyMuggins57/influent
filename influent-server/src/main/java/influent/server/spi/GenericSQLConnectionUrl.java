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
public class GenericSQLConnectionUrl implements SQLConnectionUrl {

	private String url = null;
	private String driver = null;
	
	/**
	 * 
	 */
	@Inject(optional=true)
	public void setUrl(@Named("influent.midtier.database.url") String url) {
		this.url = url;
	}
	
	/* (non-Javadoc)
	 * @see influent.server.spi.SQLConnectionUrl#getUrl()
	 */
	@Override
	public String getUrl() {
		return url;
	}

	
	/**
	 * 
	 */
	@Inject(optional=true)
	public void setDriver(@Named("influent.midtier.database.driver") String driver) {
		this.driver = driver;
	}
	
	
	/* (non-Javadoc)
	 * @see influent.server.spi.SQLConnectionUrl#getDriver()
	 */
	@Override
	public String getDriver() {
		return driver;
	}

	/* (non-Javadoc)
	 * @see influent.server.spi.SQLConnectionUrl#isValid()
	 */
	@Override
	public boolean isValid() {
		return url != null && driver != null;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return getUrl();
	}
}
