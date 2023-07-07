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
package influent.kiva.server.spi;

import influent.idl.FL_DataAccess;
import influent.idl.FL_EntitySearch;
import influent.kiva.server.dataaccess.KivaDataAccess;
import influent.midtier.spi.KivaDataAccessModule;
import influent.server.dataaccess.DataNamespaceHandler;
import influent.server.dataaccess.MSSQLDataNamespaceHandler;
import influent.server.utilities.SQLConnectionPool;

import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;


/**
 * This class is used by the Kiva *server* implementation.
 * It binds the new FL_ implementation for that server.
 *
 */
public class KivaFLDataAccessModule extends AbstractModule {

	private static Logger s_logger = LoggerFactory.getLogger(KivaDataAccessModule.class);
	
	@Override
	protected void configure() {
		bind(FL_DataAccess.class).to(KivaDataAccess.class);
	}
	
	@Provides @Singleton
	public DataNamespaceHandler namespaceHandler(@Named("influent.data.view.tables") String tableNamesJson) {
		try {
			return new MSSQLDataNamespaceHandler(tableNamesJson);
		} catch (JSONException e) {
			s_logger.warn("Failed to load tables from json. ", e);
		}
		
		return new MSSQLDataNamespaceHandler();
	}
	
	/*
	 * Provide the database service
	 */
	@Provides @Singleton
	public KivaDataAccess connect (
		SQLConnectionPool connectionPool,
		FL_EntitySearch search,
		DataNamespaceHandler namespaceHandler
	) {

		try {
			return new KivaDataAccess(
				connectionPool,
				search,
				namespaceHandler
			);
		} catch (Exception e) {
			addError("Failed to load Data Access", e);
			return null;
		}
	}
	
	
	
}
