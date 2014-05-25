package org.capnproto;

import java.nio.ByteBuffer;
import org.junit.Test;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

public class LayoutTest {

    @Test
    public void simpleRawDataStruct() {
        byte[] data = {0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00,
                       0x01, 0x23, 0x45, 0x67, (byte)0x89, (byte)0xab,
                       (byte)0xcd, (byte)0xef};

        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.order(java.nio.ByteOrder.LITTLE_ENDIAN);
        PointerReader pointerReader = new PointerReader(new SegmentReader(buffer), 0, 0x7fffffff);
        StructReader reader = pointerReader.getStruct();

        assertThat(reader.getLongField(0), equalTo(0xefcdab8967452301L));
        assertThat(reader.getLongField(1), equalTo(0L));
        assertThat(reader.getIntField(0), equalTo(0x67452301));
        assertThat(reader.getIntField(1), equalTo(0xefcdab89));
        assertThat(reader.getIntField(2), equalTo(0));
        assertThat(reader.getShortField(0), equalTo((short) 0x2301));
        assertThat(reader.getShortField(1), equalTo((short)0x6745));
        assertThat(reader.getShortField(2), equalTo((short)0xab89));
        assertThat(reader.getShortField(3), equalTo((short)0xefcd));
        assertThat(reader.getShortField(4), equalTo((short)0));

        // TODO masking

    }

}
