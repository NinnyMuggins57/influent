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
package influent.idlhelper;

import influent.idl.FL_Cluster;
import influent.idl.FL_EntityTag;
import influent.idl.FL_Property;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Constructs a simple file cluster. This class is subject to change.
 * 
 * @author djonker
 */
public class FileHelper extends FL_Cluster {

	/**
	 */
	public FileHelper(String uid, List<String> members) {
		super(uid, 
			Collections.singletonList(FL_EntityTag.FILE), 
			null, 
			null, 
			new ArrayList<FL_Property>(), 
			members,
			new ArrayList<String>(), 
			null, 
			null, 
			0);
	}

	/**
	 * 
	 */
	public FileHelper(String uid) {
		this(uid, new ArrayList<String>());
	}
}
