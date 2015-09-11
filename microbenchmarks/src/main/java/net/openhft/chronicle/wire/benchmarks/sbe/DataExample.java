/*
 *     Copyright (C) 2015  higherfrequencytrading.com
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.openhft.chronicle.wire.benchmarks.sbe;

import baseline.DataEncoder;
import net.openhft.chronicle.wire.benchmarks.Data;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;
import uk.co.real_logic.sbe.ir.Ir;
import uk.co.real_logic.sbe.ir.IrDecoder;
import uk.co.real_logic.sbe.ir.IrEncoder;
import uk.co.real_logic.sbe.ir.Token;
import uk.co.real_logic.sbe.ir.generated.MessageHeaderEncoder;
import uk.co.real_logic.sbe.otf.OtfHeaderDecoder;
import uk.co.real_logic.sbe.otf.OtfMessageDecoder;
import uk.co.real_logic.sbe.xml.IrGenerator;
import uk.co.real_logic.sbe.xml.MessageSchema;
import uk.co.real_logic.sbe.xml.ParserOptions;
import uk.co.real_logic.sbe.xml.XmlSchemaParser;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.util.List;

import static net.openhft.chronicle.wire.benchmarks.sbe.ExampleUsingGeneratedStub.encode;

public class DataExample {
    private static final MessageHeaderEncoder MESSAGE_HEADER = new MessageHeaderEncoder();
    private static final DataEncoder DATA_ENCODER = new DataEncoder();
    private static final int MSG_BUFFER_CAPACITY = 4 * 1024;
    private static final int SCHEMA_BUFFER_CAPACITY = 16 * 1024;

    public static void main(final String[] args) throws Exception {
        System.out.println("\n*** Data Example ***\n");

        // Encode up message and schema as if we just got them off the wire.
        final ByteBuffer encodedSchemaBuffer = ByteBuffer.allocateDirect(SCHEMA_BUFFER_CAPACITY);
        encodeSchema(encodedSchemaBuffer);

        final ByteBuffer encodedMsgBuffer = ByteBuffer.allocateDirect(MSG_BUFFER_CAPACITY);
        encodeTestMessage(encodedMsgBuffer);

        // Now lets decode the schema IR so we have IR objects.
        encodedSchemaBuffer.flip();
        final Ir ir = decodeIr(encodedSchemaBuffer);

        // From the IR we can create OTF decoder for message headers.
        final OtfHeaderDecoder headerDecoder = new OtfHeaderDecoder(ir.headerStructure());

        // Now we have IR we can read the message header
        int bufferOffset = 0;
        final UnsafeBuffer buffer = new UnsafeBuffer(encodedMsgBuffer);

        final int templateId = headerDecoder.getTemplateId(buffer, bufferOffset);
        final int schemaId = headerDecoder.getSchemaId(buffer, bufferOffset);
        final int actingVersion = headerDecoder.getSchemaVersion(buffer, bufferOffset);
        final int blockLength = headerDecoder.getBlockLength(buffer, bufferOffset);

        bufferOffset += headerDecoder.size();

        // Given the header information we can select the appropriate message template to do the decode.
        // The OTF Java classes are thread safe so the same instances can be reused across multiple threads.

        final List<Token> msgTokens = ir.getMessage(templateId);

        bufferOffset = OtfMessageDecoder.decode(
                buffer,
                bufferOffset,
                actingVersion,
                blockLength,
                msgTokens,
                new ExampleTokenListener(new PrintWriter(System.out, true)));

        if (bufferOffset != encodedMsgBuffer.position()) {
            throw new IllegalStateException("Message not fully decoded");
        }
    }

    private static void encodeSchema(final ByteBuffer byteBuffer)
            throws Exception {
        try (final InputStream in = new FileInputStream("examples/resources/example-data.xml")) {
            final MessageSchema schema = XmlSchemaParser.parse(in, ParserOptions.DEFAULT);
            final Ir ir = new IrGenerator().generate(schema);
            try (final IrEncoder irEncoder = new IrEncoder(byteBuffer, ir)) {
                irEncoder.encode();
            }
        }
    }

    private static void encodeTestMessage(final ByteBuffer byteBuffer) {
        final UnsafeBuffer buffer = new UnsafeBuffer(byteBuffer);

        int bufferOffset = 0;
        MESSAGE_HEADER
                .wrap(buffer, bufferOffset)
                .blockLength(DATA_ENCODER.sbeBlockLength())
                .templateId(DATA_ENCODER.sbeTemplateId())
                .schemaId(DATA_ENCODER.sbeSchemaId())
                .version(DATA_ENCODER.sbeSchemaVersion());

        bufferOffset += MESSAGE_HEADER.encodedLength();

        bufferOffset += encode(DATA_ENCODER, buffer, bufferOffset, new Data(), ByteBuffer.allocate(64), new UnsafeBuffer(ByteBuffer.allocate(64)));

        byteBuffer.position(bufferOffset);
    }

    private static Ir decodeIr(final ByteBuffer buffer)
            throws IOException {
        try (final IrDecoder irDecoder = new IrDecoder(buffer)) {
            return irDecoder.decode();
        }
    }
}
