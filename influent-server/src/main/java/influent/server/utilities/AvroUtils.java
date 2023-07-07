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
package influent.server.utilities;

import java.io.ByteArrayOutputStream;

import org.apache.avro.Schema;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.io.JsonDecoder;
import org.apache.avro.io.JsonEncoder;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecord;
import org.apache.avro.specific.SpecificRecordBase;

public class AvroUtils {
    /**
     * Encodes an Avro object as JSON string.
     * 
     * Note: Just calling record.toString() doesn't encode union types losslessly (since it leaves out their type).
     */
    public static String encodeJSON(SpecificRecordBase record) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(); 
        JsonEncoder encoder = EncoderFactory.get().jsonEncoder(record.getSchema(), baos);
        DatumWriter<SpecificRecord> datumWriter = new SpecificDatumWriter<SpecificRecord>(record.getSchema());
        datumWriter.write(record, encoder);
        encoder.flush();
        return baos.toString();
    }
    
    /**
     * Decodes an Avro object from a JSON string.
     * 
     * Note: This method will fail on strings created by calling record.toString() since that doesn't encode union types losslessly.
     */
    public static SpecificRecord decodeJSON(Schema schema, String json) throws Exception {
        DatumReader<SpecificRecord> datumReader = new SpecificDatumReader<SpecificRecord>(schema);
        JsonDecoder decoder = DecoderFactory.get().jsonDecoder(schema, json);
        SpecificRecord record = datumReader.read(null, decoder);
        return record;
    }
}
