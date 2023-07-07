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

import oculus.aperture.spi.common.Properties;
import influent.idl.FL_EntitySearch;
import influent.idl.FL_Geocoding;
import influent.kiva.server.search.KivaAnonEntitySearch;
import influent.server.dataaccess.DataNamespaceHandler;
import influent.server.utilities.SQLConnectionPool;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;


/**
 * This class is used by the Kiva *server* implementation.
 * It binds Solr to an implementation for that server.
 *
 */
public class KivaFLAnonEntitySearchModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(FL_EntitySearch.class).to(KivaAnonEntitySearch.class);
	}
	
	/*
	 * Provide the database service
	 */
	@Provides @Singleton
	public KivaAnonEntitySearch connect (
		@Named("influent.midtier.solr.url") String solrUrl,
		@Named("influent.midtier.solr.descriptor") String solrDescriptor,
		@Named("aperture.server.config") Properties config,
		FL_Geocoding geocoding,
		SQLConnectionPool connectionPool,
		DataNamespaceHandler namespaceHandler
	) {

		try {
			return new KivaAnonEntitySearch(solrUrl, solrDescriptor, config, geocoding, connectionPool, namespaceHandler);
		} catch (Exception e) {
			System.err.println(e.getMessage());
			addError("Failed to EntitySearch", e);
			return null;
		}
	}
	
}