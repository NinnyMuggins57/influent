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
package influent.server.dataaccess;

import influent.idl.FL_BoundedRange;
import influent.idl.FL_Future;
import influent.idl.FL_PatternDescriptor;
import influent.idl.FL_PatternSearch;
import influent.idl.FL_PatternSearchResults;
import influent.idl.FL_Service;
import influent.idlhelper.SerializationHelper;
import influent.server.utilities.RestClient;

import java.io.IOException;
import java.util.List;

import javax.ws.rs.core.Response.Status.Family;

import org.apache.avro.AvroRemoteException;
import org.apache.avro.Schema;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.representation.Form;

public class QuBEClient extends RestClient implements FL_PatternSearch {
//	private static Logger logger = Logger.getLogger(RestPatternSearch.class);
	
	private static final Schema LONG_SCHEMA = Schema.create(Schema.Type.LONG);
	private static boolean _useHMM = false; 
	
	public QuBEClient(String url, boolean useHMM) {
		super(url);
		_useHMM = useHMM;
	}
	
	@Override
	public Void setTimeout(FL_Future future, long timeout) throws AvroRemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean getCompleted(FL_Future future) throws AvroRemoteException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getError(FL_Future future) throws AvroRemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double getProgress(FL_Future future) throws AvroRemoteException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getExpectedDuration(FL_Future future) throws AvroRemoteException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Void stop(FL_Future future) throws AvroRemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<FL_Future> getFutures() throws AvroRemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object searchByExample(FL_PatternDescriptor example, String service, long start, long max, FL_BoundedRange dateRange, boolean useAptima) throws AvroRemoteException {
		try {
			//init
			Form form = new Form();
			
			// If HMM is specified with only one role QuBE will blow up.
			// There is an entirely different interface for querying on just one interface,
			// but for convenience we are currently using this method.
			if (_useHMM && example.getEntities().size() > 1) {
				form.add("hmm", "true");
			}
			
			// args
			// this is the equivalent of resultLimit and can be removed once the latter works
			form.add("max", SerializationHelper.toJson(new Long(max), LONG_SCHEMA));
			
//			form.add("example", "{\"uid\":\"PD0\",\"name\":\"PatternDescriptor0\",\"description\":null,\"entities\":[{\"uid\":\"E0\",\"role\":{\"string\":\"EntityRole0\"},\"entities\":null,\"tags\":null,\"properties\":null,\"examplars\":{\"array\":[\"12564\"]},\"constraint\":null},{\"uid\":\"E1\",\"role\":{\"string\":\"EntityRole1\"},\"entities\":null,\"tags\":null,\"properties\":null,\"examplars\":{\"array\":[\"28497\"]},\"constraint\":null}],\"links\":[]}");
//			form.add("example", "{\"uid\":\"PD0\",\"name\":\"Pattern Descriptor 0\",\"description\":null,\"entities\":[{\"uid\":\"E0\",\"role\":{\"string\":\"Entity Role 0\"},\"entities\":null,\"tags\":null,\"properties\":null,\"examplars\":{\"array\":[\"5249540\"]},\"constraint\":null},{\"uid\":\"E1\",\"role\":{\"string\":\"Entity Role 1\"},\"entities\":null,\"tags\":null,\"properties\":null,\"examplars\":{\"array\":[\"5249569\"]},\"constraint\":null},{\"uid\":\"E2\",\"role\":{\"string\":\"Entity Role 2\"},\"entities\":null,\"tags\":null,\"properties\":null,\"examplars\":{\"array\":[\"5249535\"]},\"constraint\":null},{\"uid\":\"E3\",\"role\":{\"string\":\"Entity Role 3\"},\"entities\":null,\"tags\":null,\"properties\":null,\"examplars\":{\"array\":[\"5254968\"]},\"constraint\":null},{\"uid\":\"E4\",\"role\":{\"string\":\"Entity Role 4\"},\"entities\":null,\"tags\":null,\"properties\":null,\"examplars\":{\"array\":[\"11\"]},\"constraint\":null}],\"links\":[]}");
			form.add("example", SerializationHelper.toJson(example));
			form.add("resultLimit", SerializationHelper.toJson(new Long(max), LONG_SCHEMA));
			form.add("startIndex", SerializationHelper.toJson(new Long(start), LONG_SCHEMA));
			form.add("dateRange", SerializationHelper.toJson(dateRange));
			if (useAptima) {
				form.add("aptimaQueryIndex", SerializationHelper.toJson(new Long(0), LONG_SCHEMA));
			}
			
			// call
			ClientResponse response  = client
					.resource(baseUrl)
					.type("application/x-www-form-urlencoded")
					.post(ClientResponse.class, form);

			// TODO add code to return an FL_Future object(if required) so that getResults() works; see PatternSearchResource
			if (response.getClientResponseStatus().getFamily() == Family.SUCCESSFUL) {
				String resultJson = response.getEntity(String.class);
				return SerializationHelper.fromJson(resultJson, FL_PatternSearchResults.getClassSchema());
			}
			else {
				String extra = "";
				try {
					extra = "\n\n" + response.getEntity(String.class) + "\nReceived as server response";
				} catch (Exception e) {}
				
				throw new AvroRemoteException(response.getClientResponseStatus().getReasonPhrase() + extra);
			}
		}
		catch(IOException ioe) {
			if (ioe instanceof AvroRemoteException) {
				throw (AvroRemoteException)ioe;
			}
			throw new AvroRemoteException(ioe);
		}
	}

	@Override
	public Object searchByTemplate(String template, String service, long start, long max, FL_BoundedRange dateRange) throws AvroRemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public FL_PatternSearchResults getResults(FL_Future future) throws AvroRemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<FL_PatternDescriptor> getPatternTemplates() throws AvroRemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<FL_Service> getServices() throws AvroRemoteException {
		// TODO Auto-generated method stub
		return null;
	}

}
