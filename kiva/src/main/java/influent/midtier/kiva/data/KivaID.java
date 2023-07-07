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
package influent.midtier.kiva.data;

public class KivaID {
	public final EntityType type;
	public final String id;
//	final String loan;
	
	public KivaID(EntityType type, String id) {
		this.type = type;
		
//		/** PARTNER HACK **/
//		String loan = null;
//		if (type==EntityType.Partner && id.indexOf("-") > -1) {
//			id = id.substring(0, id.indexOf("-"));
//			loan = id.substring(id.indexOf("-")+1);
//		}
		this.id = id;
//		this.loan = loan;
	}
	
	public enum EntityType {
		Borrowers ("B", "b"),
		Team ("T", null),
		Partner ("P", "p"),
		Lender ("L", "l");
		
		String character;
		String financialId;
		EntityType(String character, String idColumnName) { this.character = character; this.financialId = idColumnName; }
		
		static EntityType parse(String character) {
			for (EntityType type : EntityType.values()) {
				if (type.character.equalsIgnoreCase(character)) return type;
			}
			return null;
		}
		public static EntityType parseFinancialId(String financialId) {
			for (EntityType type : EntityType.values()) {
				if (type.financialId != null && type.financialId.equals(financialId)) return type;
			}
			return null;
		}

		public String createID(String id) { return character + id; }
		public String financialId() {
			if (financialId == null) throw new UnsupportedOperationException();
			return financialId;
		}
		
	}
	
	public static boolean isKiva(String id) {
		if (id.startsWith("b")) return true;
		if (id.startsWith("l")) return true;
		if (id.startsWith("p")) return true;
		return false;
	}
	
	public static KivaID parse(String id) {
		if (isKiva(id)) {
			return new KivaID(EntityType.parse(id.substring(0, 1)), id.substring(1));
		}
		return null;
	}
	
	@Override
	public String toString() {
		return id;
	}
}
