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
package influent.kiva.server.search;

import oculus.aperture.spi.common.Properties;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import influent.idl.FL_Geocoding;
import influent.server.dataaccess.DataNamespaceHandler;
import influent.server.utilities.SQLConnectionPool;

public class KivaAnonEntitySearch extends KivaEntitySearch {

	private static final Logger s_logger = LoggerFactory.getLogger(KivaAnonEntitySearch.class);
	
	public KivaAnonEntitySearch(String solrURL, String solrDescriptor, Properties config,
			FL_Geocoding geocoding, SQLConnectionPool connectionPool,
			DataNamespaceHandler namespaceHandler) {
		super(solrURL, solrDescriptor, config, geocoding, connectionPool, namespaceHandler);
	}

	@Override
	public KivaEntitySearchIterator buildKivaEntitySearchIterator(SolrServer solr, SolrQuery query, Properties config, FL_Geocoding geocoding) {
		return new KivaAnonEntitySearchIterator(solr, query, config, geocoding);
	}
	
	@Override
	protected Logger getLogger() {
		return s_logger;
	}
}
