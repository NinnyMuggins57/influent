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

import influent.server.utilities.BoneCPConnectionPool;
import influent.server.utilities.SQLConnectionPool;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;


/**
 */
public class SQLConnectionPoolModule extends AbstractModule {
	
	@Override
	protected void configure() {
		bind(SQLConnectionUrl.class).annotatedWith(Names.named("influent.midtier.dbconnection.url"))
			.to(GenericSQLConnectionUrl.class);
		
		bind(SQLConnectionUrl.class).annotatedWith(Names.named("influent.midtier.dbconnection.mssql"))
		.to(SQLServerConnectionUrl.class);
	}
	
	/*
	 * Provide the database service
	 */
	@Provides @Singleton
	public SQLConnectionPool connect (
		@Named("influent.midtier.dbconnection.url") SQLConnectionUrl genericUrl,
		@Named("influent.midtier.dbconnection.mssql") SQLConnectionUrl mssqlUrl,
		@Named("influent.midtier.user.name") String username,
		@Named("influent.midtier.user.password") String password,
		@Named("influent.midtier.ehcache.config") String ehCacheConfig
	) {
		final SQLConnectionUrl url = genericUrl.isValid()? genericUrl : mssqlUrl;
		
		if (!url.isValid()) {
			throw new IllegalArgumentException("SQLConnectionPool missing a connection configuration!");
		}
		
		return new BoneCPConnectionPool(url, username, password, null);
	}
}
