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
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package influent.server.auth;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.FilterChain;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;

import org.apache.shiro.authc.credential.CredentialsMatcher;
import org.apache.shiro.authc.credential.HashedCredentialsMatcher;
import org.apache.shiro.config.Ini;
import org.apache.shiro.guice.web.ShiroWebModule;
import org.apache.shiro.realm.text.IniRealm;
import org.apache.shiro.web.filter.authc.LogoutFilter;
import org.apache.shiro.web.servlet.AbstractFilter;

import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.name.Names;

/**
 * @author rharper
 *
 */
public class SimpleShiroAuthModule extends ShiroWebModule {

	public SimpleShiroAuthModule(ServletContext sc) {
		super(sc);
    }

	// NOTE shiro is configured in properties file, which shiro picks up via guice name bindings.
	
	@SuppressWarnings("unchecked")
	@Override
	protected void configureShiroWeb() {
		// Use IniRealm - a simple Realm implementation that allows us to define
		// users, passwords, and roles in an INI file
		// Must bind an Ini.class implementation
		try {
            bindRealm().toConstructor(IniRealm.class.getConstructor(Ini.class));
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Missing IniRealm class implementation", e);
        }

        // Use the following three lines to instruct Shiro to expect passwords stored
        // in the INI file to be encrypted using MD5
        // Comment out to use plaintext passwords in the INI file
        bind(CredentialsMatcher.class).to(HashedCredentialsMatcher.class);
        bind(HashedCredentialsMatcher.class);

        addFilterChain("/", Key.get(NoCacheFilter.class), AUTHC); 
        addFilterChain("/index.html", Key.get(NoCacheFilter.class), AUTHC); 

        // Configure filters in the order they should be applied
        // Make the /** filter last so that it doesn't catch everything and never to get /logout

        // Configure auto logout when accessing /logout
		// Logout when the user hits this url
        bindConstant().annotatedWith(Names.named("myRedirectUrl")).to("/login.jsp"); 
        Key<MyLogoutFilter> MYLOGOUT = Key.get(MyLogoutFilter.class);	
        addFilterChain("/logout", MYLOGOUT); 

        // Configure all URLs to be protected by AuthC filter
        // You may replace filter chain definition with more complex authentication
        // filters and roles

		// All URLs are protected: require Form-based login
		addFilterChain("/**", AUTHC);

	}


	@Provides
    Ini loadShiroIni() {
		// Load INI file from specified path
        return Ini.fromResourcePath("classpath:shiro.ini");
    }

	public static class NoCacheFilter extends AbstractFilter {

		@Override
		public void doFilter(ServletRequest request, ServletResponse response,
				FilterChain chain) throws IOException, ServletException {
			HttpServletResponse httpResponse = (HttpServletResponse)response;
			httpResponse.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate"); // HTTP 1.1.
			httpResponse.setDateHeader(HttpHeaders.EXPIRES, 0); // Proxies.
			chain.doFilter(request, httpResponse);
		}
		
	}

	public static class MyLogoutFilter extends LogoutFilter {

		@Inject
		@Override
		public void setRedirectUrl(@Named("myRedirectUrl") String redirectUrl) {
			super.setRedirectUrl(redirectUrl);
		}
	}
}
