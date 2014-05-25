package org.capnproto;

import java.nio.ByteBuffer;
import org.junit.Test;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

public class LayoutTest {

    @Test
    public void simpleRawDataStruct() {
        byte[] data = {0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00,
                       0x01, 0x23, 0x45, 0x67,
                       (byte)(0x89 & 0xff), (byte)(0xab & 0xff),
                       (byte)(0xcd & 0xff), (byte)(0xef & 0xff)};

        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.order(java.nio.ByteOrder.LITTLE_ENDIAN);
        PointerReader pointerReader = new PointerReader(new SegmentReader(buffer), 0, 0x7fffffff);
        StructReader reader = pointerReader.getStruct();

        assertThat(reader.getLongField(0), equalTo(0xefcdab8967452301L));

    }

}
