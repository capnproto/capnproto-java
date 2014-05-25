package org.capnproto;

import java.nio.ByteBuffer;
import org.junit.Test;

import static org.hamcrest.core.IsEqual.equalTo;
import org.junit.Assert;

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

        Assert.assertThat(reader.getLongField(0), equalTo(0xefcdab8967452301L));
        Assert.assertThat(reader.getLongField(1), equalTo(0L));
        Assert.assertThat(reader.getIntField(0), equalTo(0x67452301));
        Assert.assertThat(reader.getIntField(1), equalTo(0xefcdab89));
        Assert.assertThat(reader.getIntField(2), equalTo(0));
        Assert.assertThat(reader.getShortField(0), equalTo((short) 0x2301));
        Assert.assertThat(reader.getShortField(1), equalTo((short)0x6745));
        Assert.assertThat(reader.getShortField(2), equalTo((short)0xab89));
        Assert.assertThat(reader.getShortField(3), equalTo((short)0xefcd));
        Assert.assertThat(reader.getShortField(4), equalTo((short)0));

        // TODO masking

        Assert.assertTrue(reader.getBoolField(0));
        Assert.assertFalse(reader.getBoolField(1));
        Assert.assertFalse(reader.getBoolField(2));
        Assert.assertFalse(reader.getBoolField(3));
        Assert.assertFalse(reader.getBoolField(4));
        Assert.assertFalse(reader.getBoolField(5));
        Assert.assertFalse(reader.getBoolField(6));
        Assert.assertFalse(reader.getBoolField(7));

        Assert.assertTrue(reader.getBoolField(8));
        Assert.assertTrue(reader.getBoolField(9));
    }

}
