package org.capnproto;

import org.junit.Assert;

class TestUtil {
    static void initTestMessage(org.capnproto.test.Test.TestAllTypes.Builder builder) {
        builder.setVoidField(Void.VOID);
        builder.setBoolField(true);
        builder.setInt8Field((byte) -123);
        builder.setInt16Field((short) -12345);
        builder.setInt32Field(-12345678);
        builder.setInt64Field(-123456789012345L);
        builder.setUInt8Field((byte) 234);
        builder.setUInt16Field((short) 45678);
        builder.setUInt32Field((int) 3456789012l);
        builder.setUInt64Field(1234567890123456789L);
        builder.setFloat32Field(1234.5f);
        builder.setFloat64Field(-123e45);
        builder.setTextField("foo");
    }

    static void checkTestMessage(org.capnproto.test.Test.TestAllTypes.Reader reader) {
        Assert.assertEquals(Void.VOID, reader.getVoidField());
        Assert.assertTrue(reader.getBoolField());
        Assert.assertEquals((byte)-123, reader.getInt8Field());
        Assert.assertEquals((short)-12345, reader.getInt16Field());
        Assert.assertEquals(-12345678, reader.getInt32Field());
        Assert.assertEquals(-123456789012345l, reader.getInt64Field());
        Assert.assertEquals((byte)234, reader.getUInt8Field());
        Assert.assertEquals((short)45678, reader.getUInt16Field());
        Assert.assertEquals((int)3456789012l, reader.getUInt32Field());
        Assert.assertEquals(1234567890123456789l, reader.getUInt64Field());
        Assert.assertEquals(null, 1234.5f, reader.getFloat32Field(), 0.1f);
        Assert.assertEquals(null, -123e45, reader.getFloat64Field(), 0.1f);
        Assert.assertEquals("foo", reader.getTextField().toString());
    }
}
