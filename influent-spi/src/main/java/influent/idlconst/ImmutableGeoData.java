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
package influent.idlconst;

import influent.idl.FL_GeoData;

/**
 * Internal immutable versions of country data, safe to return above.
 * 
 * @author djonker
 */
public class ImmutableGeoData extends FL_GeoData {
	public ImmutableGeoData(String text, Double lat, Double lon, String cc) {
		super(text, lat, lon, cc);
	}
	
	@Override
	public void setText(String value) {
		throw new UnsupportedOperationException("Tried to set properties of immutable FL_Geo instance.");
	}
	@Override
	public void setLat(Double value) {
		throw new UnsupportedOperationException("Tried to set properties of immutable FL_Geo instance.");
	}
	@Override
	public void setLon(Double value) {
		throw new UnsupportedOperationException("Tried to set properties of immutable FL_Geo instance.");
	}
	@Override
	public void setCc(String value) {
		throw new UnsupportedOperationException("Tried to set properties of immutable FL_Geo instance.");
	}
}