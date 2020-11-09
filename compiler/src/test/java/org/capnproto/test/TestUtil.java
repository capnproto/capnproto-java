package org.capnproto.test;

import org.capnproto.*;
import org.junit.Assert;

import java.io.UnsupportedEncodingException;

public class TestUtil {
    public static byte[] data(String string) {
        try {
            return string.getBytes("ISO_8859-1");
        } catch (UnsupportedEncodingException e) {
            Assert.fail("Could not decode");
            return null;
        }
    }

    public static final double DELTA = 1e-15;

    public static void initTestMessage(Test.TestAllTypes.Builder builder) {
        builder.setVoidField(org.capnproto.Void.VOID);
        builder.setBoolField(true);
        builder.setInt8Field((byte) -123);
        builder.setInt16Field((short) -12345);
        builder.setInt32Field(-12345678);
        builder.setInt64Field(-123456789012345L);
        builder.setUInt8Field((byte) 0xea);
        builder.setUInt16Field((short) 0x4567);
        builder.setUInt32Field(0x34567890);
        builder.setUInt64Field(0x1234567890123456L);
        builder.setFloat32Field(1234.5f);
        builder.setFloat64Field(-123e45);
        builder.setTextField("foo");
        builder.setDataField(data("bar"));
        {
            Test.TestAllTypes.Builder subBuilder = builder.initStructField();
            subBuilder.setVoidField(org.capnproto.Void.VOID);
            subBuilder.setBoolField(true);
            subBuilder.setInt8Field((byte) -12);
            subBuilder.setInt16Field((short) 3456);
            subBuilder.setInt32Field(-78901234);
            subBuilder.setInt64Field(56789012345678L);
            subBuilder.setUInt8Field((byte) 90);
            subBuilder.setUInt16Field((short) 1234);
            subBuilder.setUInt32Field(56789012);
            subBuilder.setUInt64Field(345678901234567890L);
            subBuilder.setFloat32Field(-1.25e-10f);
            subBuilder.setFloat64Field(345);
            subBuilder.setTextField(new Text.Reader("baz"));
            subBuilder.setDataField(data("qux"));
            {
                Test.TestAllTypes.Builder subSubBuilder = subBuilder.initStructField();
                subSubBuilder.setTextField(new Text.Reader("nested"));
                subSubBuilder.initStructField().setTextField(new Text.Reader("really nested"));
            }
            subBuilder.setEnumField(Test.TestEnum.BAZ);

            PrimitiveList.Boolean.Builder boolList = subBuilder.initBoolList(5);
            boolList.set(0, false);
            boolList.set(1, true);
            boolList.set(2, false);
            boolList.set(3, true);
            boolList.set(4, true);
        }

        builder.setEnumField(Test.TestEnum.CORGE);
        builder.initVoidList(6);

        PrimitiveList.Boolean.Builder boolList = builder.initBoolList(4);
        boolList.set(0, true);
        boolList.set(1, false);
        boolList.set(2, false);
        boolList.set(3, true);

        PrimitiveList.Double.Builder float64List = builder.initFloat64List(4);
        float64List.set(0, 7777.75);
        float64List.set(1, Double.POSITIVE_INFINITY);
        float64List.set(2, Double.NEGATIVE_INFINITY);
        float64List.set(3, Double.NaN);

        TextList.Builder textList = builder.initTextList(3);
        textList.set(0, new Text.Reader("plugh"));
        textList.set(1, new Text.Reader("xyzzy"));
        textList.set(2, new Text.Reader("thud"));

        StructList.Builder<Test.TestAllTypes.Builder> structList = builder.initStructList(3);
        structList.get(0).setTextField(new Text.Reader("structlist 1"));
        structList.get(1).setTextField(new Text.Reader("structlist 2"));
        structList.get(2).setTextField(new Text.Reader("structlist 3"));

        EnumList.Builder<Test.TestEnum> enumList = builder.initEnumList(2);
        enumList.set(0, Test.TestEnum.FOO);
        enumList.set(1, Test.TestEnum.GARPLY);
    }

    public static void checkTestMessage(Test.TestAllTypes.Builder builder) {
        builder.getVoidField();
        Assert.assertEquals(builder.getBoolField(), true);
        Assert.assertEquals(builder.getInt8Field(), -123);
        Assert.assertEquals(builder.getInt16Field(), -12345);
        Assert.assertEquals(builder.getInt32Field(), -12345678);
        Assert.assertEquals(builder.getInt64Field(), -123456789012345L);
        Assert.assertEquals(builder.getUInt8Field(), (byte) 0xea);
        Assert.assertEquals(builder.getUInt16Field(), 0x4567);
        Assert.assertEquals(builder.getUInt32Field(), 0x34567890);
        Assert.assertEquals(builder.getUInt64Field(), 0x1234567890123456L);
        Assert.assertEquals(builder.getFloat32Field(), 1234.5f, DELTA);
        Assert.assertEquals(builder.getFloat64Field(), -123e45, DELTA);
        Assert.assertEquals(builder.getTextField().toString(), "foo");

        {
            Test.TestAllTypes.Builder subBuilder = builder.getStructField();
            subBuilder.getVoidField();
            Assert.assertEquals(subBuilder.getBoolField(), true);
            Assert.assertEquals(subBuilder.getInt8Field(), -12);
            Assert.assertEquals(subBuilder.getInt16Field(), 3456);
            Assert.assertEquals(subBuilder.getInt32Field(), -78901234);
            Assert.assertEquals(subBuilder.getInt64Field(), 56789012345678L);
            Assert.assertEquals(subBuilder.getUInt8Field(), 90);
            Assert.assertEquals(subBuilder.getUInt16Field(), 1234);
            Assert.assertEquals(subBuilder.getUInt32Field(), 56789012);
            Assert.assertEquals(subBuilder.getUInt64Field(), 345678901234567890L);
            Assert.assertEquals(subBuilder.getFloat32Field(), -1.25e-10f, DELTA);
            Assert.assertEquals(subBuilder.getFloat64Field(), 345, DELTA);

            {
                Test.TestAllTypes.Builder subSubBuilder = subBuilder.getStructField();
                Assert.assertEquals(subSubBuilder.getTextField().toString(), "nested");
            }

            Assert.assertEquals(subBuilder.getEnumField(), Test.TestEnum.BAZ);

            PrimitiveList.Boolean.Builder boolList = subBuilder.getBoolList();
            Assert.assertEquals(boolList.get(0), false);
            Assert.assertEquals(boolList.get(1), true);
            Assert.assertEquals(boolList.get(2), false);
            Assert.assertEquals(boolList.get(3), true);
            Assert.assertEquals(boolList.get(4), true);

        }
        Assert.assertEquals(builder.getEnumField(), Test.TestEnum.CORGE);

        Assert.assertEquals(builder.getVoidList().size(), 6);

        PrimitiveList.Boolean.Builder boolList = builder.getBoolList();
        Assert.assertEquals(boolList.get(0), true);
        Assert.assertEquals(boolList.get(1), false);
        Assert.assertEquals(boolList.get(2), false);
        Assert.assertEquals(boolList.get(3), true);

        PrimitiveList.Double.Builder float64List = builder.getFloat64List();
        Assert.assertEquals(float64List.get(0), 7777.75, DELTA);
        Assert.assertEquals(float64List.get(1), Double.POSITIVE_INFINITY, DELTA);
        Assert.assertEquals(float64List.get(2), Double.NEGATIVE_INFINITY, DELTA);
        Assert.assertEquals(float64List.get(3), Double.NaN, DELTA);

        TextList.Builder textList = builder.getTextList();
        Assert.assertEquals(textList.size(), 3);
        Assert.assertEquals(textList.get(0).toString(), "plugh");
        Assert.assertEquals(textList.get(1).toString(), "xyzzy");
        Assert.assertEquals(textList.get(2).toString(), "thud");

        StructList.Builder<Test.TestAllTypes.Builder> structList = builder.getStructList();
        Assert.assertEquals(3, structList.size());
        Assert.assertEquals(structList.get(0).getTextField().toString(), "structlist 1");
        Assert.assertEquals(structList.get(1).getTextField().toString(), "structlist 2");
        Assert.assertEquals(structList.get(2).getTextField().toString(), "structlist 3");

        EnumList.Builder<Test.TestEnum> enumList = builder.getEnumList();
        Assert.assertEquals(enumList.get(0), Test.TestEnum.FOO);
        Assert.assertEquals(enumList.get(1), Test.TestEnum.GARPLY);
    }

    public static void checkTestMessage(Test.TestAllTypes.Reader reader) {
        reader.getVoidField();
        Assert.assertEquals(reader.getBoolField(), true);
        Assert.assertEquals(reader.getInt8Field(), -123);
        Assert.assertEquals(reader.getInt16Field(), -12345);
        Assert.assertEquals(reader.getInt32Field(), -12345678);
        Assert.assertEquals(reader.getInt64Field(), -123456789012345L);
        Assert.assertEquals(reader.getUInt8Field(), (byte)0xea);
        Assert.assertEquals(reader.getUInt16Field(), 0x4567);
        Assert.assertEquals(reader.getUInt32Field(), 0x34567890);
        Assert.assertEquals(reader.getUInt64Field(), 0x1234567890123456L);
        Assert.assertEquals(reader.getFloat32Field(), 1234.5f, DELTA);
        Assert.assertEquals(reader.getFloat64Field(), -123e45, DELTA);
        Assert.assertEquals(reader.getTextField().toString(), "foo");

        {
            Test.TestAllTypes.Reader subReader = reader.getStructField();
            subReader.getVoidField();
            Assert.assertEquals(subReader.getBoolField(), true);
            Assert.assertEquals(subReader.getInt8Field(), -12);
            Assert.assertEquals(subReader.getInt16Field(), 3456);
            Assert.assertEquals(subReader.getInt32Field(), -78901234);
            Assert.assertEquals(subReader.getInt64Field(), 56789012345678L);
            Assert.assertEquals(subReader.getUInt8Field(), 90);
            Assert.assertEquals(subReader.getUInt16Field(), 1234);
            Assert.assertEquals(subReader.getUInt32Field(), 56789012);
            Assert.assertEquals(subReader.getUInt64Field(), 345678901234567890L);
            Assert.assertEquals(subReader.getFloat32Field(), -1.25e-10f, DELTA);
            Assert.assertEquals(subReader.getFloat64Field(), 345, DELTA);

            {
                Test.TestAllTypes.Reader subSubReader = subReader.getStructField();
                Assert.assertEquals(subSubReader.getTextField().toString(), "nested");
            }
            PrimitiveList.Boolean.Reader boolList = subReader.getBoolList();
            Assert.assertEquals(boolList.get(0), false);
            Assert.assertEquals(boolList.get(1), true);
            Assert.assertEquals(boolList.get(2), false);
            Assert.assertEquals(boolList.get(3), true);
            Assert.assertEquals(boolList.get(4), true);

        }

        Assert.assertEquals(reader.getVoidList().size(), 6);

        PrimitiveList.Boolean.Reader boolList = reader.getBoolList();
        Assert.assertEquals(boolList.get(0), true);
        Assert.assertEquals(boolList.get(1), false);
        Assert.assertEquals(boolList.get(2), false);
        Assert.assertEquals(boolList.get(3), true);

        PrimitiveList.Double.Reader float64List = reader.getFloat64List();
        Assert.assertEquals(float64List.get(0), 7777.75, DELTA);
        Assert.assertEquals(float64List.get(1), Double.POSITIVE_INFINITY, DELTA);
        Assert.assertEquals(float64List.get(2), Double.NEGATIVE_INFINITY, DELTA);
        Assert.assertEquals(float64List.get(3), Double.NaN, DELTA);

        TextList.Reader textList = reader.getTextList();
        Assert.assertEquals(textList.size(), 3);
        Assert.assertEquals(textList.get(0).toString(), "plugh");
        Assert.assertEquals(textList.get(1).toString(), "xyzzy");
        Assert.assertEquals(textList.get(2).toString(), "thud");
        StructList.Reader<Test.TestAllTypes.Reader> structList = reader.getStructList();
        Assert.assertEquals(3, structList.size());
        Assert.assertEquals(structList.get(0).getTextField().toString(), "structlist 1");
        Assert.assertEquals(structList.get(1).getTextField().toString(), "structlist 2");
        Assert.assertEquals(structList.get(2).getTextField().toString(), "structlist 3");

        EnumList.Reader<Test.TestEnum> enumList = reader.getEnumList();
        Assert.assertEquals(enumList.get(0), Test.TestEnum.FOO);
        Assert.assertEquals(enumList.get(1), Test.TestEnum.GARPLY);
    }

    public static void checkDefaultMessage(Test.TestDefaults.Builder builder) {
        builder.getVoidField();
        Assert.assertTrue(builder.getBoolField());
        Assert.assertEquals(builder.getInt8Field(), -123);
        Assert.assertEquals(builder.getInt16Field(), -12345);
        Assert.assertEquals(builder.getInt32Field(), -12345678);
        Assert.assertEquals(builder.getInt64Field(), -123456789012345L);
        Assert.assertEquals(builder.getUInt8Field(), (byte)0xea);
        Assert.assertEquals(builder.getUInt16Field(), (short)45678);
        Assert.assertEquals(builder.getUInt32Field(), 0xce0a6a14);
        Assert.assertEquals(builder.getUInt64Field(), 0xab54a98ceb1f0ad2L);
        Assert.assertEquals(builder.getFloat32Field(), 1234.5f, DELTA);
        Assert.assertEquals(builder.getFloat64Field(), -123e45, DELTA);
        Assert.assertEquals(builder.getEnumField(), Test.TestEnum.CORGE);

        Assert.assertEquals(builder.getTextField().toString(), "foo");
        Assert.assertArrayEquals(builder.getDataField().toArray(), new byte[]{0x62, 0x61, 0x72});
    }

    public static void checkDefaultMessage(Test.TestDefaults.Reader reader) {
        reader.getVoidField();
        Assert.assertEquals(reader.getBoolField(), true);
        Assert.assertEquals(reader.getInt8Field(), -123);
        Assert.assertEquals(reader.getInt16Field(), -12345);
        Assert.assertEquals(reader.getInt32Field(), -12345678);
        Assert.assertEquals(reader.getInt64Field(), -123456789012345L);
        Assert.assertEquals(reader.getUInt8Field(), (byte)0xea);
        Assert.assertEquals(reader.getUInt16Field(), (short)45678);
        Assert.assertEquals(reader.getUInt32Field(), 0xce0a6a14);
        Assert.assertEquals(reader.getUInt64Field(), 0xab54a98ceb1f0ad2L);
        Assert.assertEquals(reader.getFloat32Field(), 1234.5f, DELTA);
        Assert.assertEquals(reader.getFloat64Field(), -123e45, DELTA);
        Assert.assertEquals(reader.getTextField().toString(), "foo");
        Assert.assertArrayEquals(reader.getDataField().toArray(), new byte[]{0x62, 0x61, 0x72});
        {
            Test.TestAllTypes.Reader subReader = reader.getStructField();
            subReader.getVoidField();
            Assert.assertTrue(subReader.getBoolField());
            Assert.assertEquals(subReader.getInt8Field(), -12);
            Assert.assertEquals(subReader.getInt16Field(), 3456);
            Assert.assertEquals(subReader.getInt32Field(), -78901234);
            // ...
            Assert.assertEquals(subReader.getTextField().toString(), "baz");

            {
                Test.TestAllTypes.Reader subSubReader = subReader.getStructField();
                Assert.assertEquals(subSubReader.getTextField().toString(), "nested");
            }

        }

        Assert.assertEquals(reader.getEnumField(), Test.TestEnum.CORGE);
        Assert.assertEquals(reader.getVoidList().size(), 6);

        {
            PrimitiveList.Boolean.Reader listReader = reader.getBoolList();
            Assert.assertEquals(listReader.size(), 4);
            Assert.assertTrue(listReader.get(0));
            Assert.assertFalse(listReader.get(1));
            Assert.assertFalse(listReader.get(2));
            Assert.assertTrue(listReader.get(3));
        }

        {
            PrimitiveList.Byte.Reader listReader = reader.getInt8List();
            Assert.assertEquals(listReader.size(), 2);
            Assert.assertEquals(listReader.get(0), 111);
            Assert.assertEquals(listReader.get(1), -111);
        }
    }

    public static void setDefaultMessage(Test.TestDefaults.Builder builder) {
        builder.setBoolField(false);
        builder.setInt8Field((byte) -122);
        builder.setInt16Field((short) -12344);
        builder.setInt32Field(-12345677);
        builder.setInt64Field(-123456789012344L);
        builder.setUInt8Field((byte) 0xe9);
        builder.setUInt16Field((short) 45677);
        builder.setUInt32Field(0xce0a6a13);
        builder.setUInt64Field(0xab54a98ceb1f0ad1L);
        builder.setFloat32Field(1234.4f);
        builder.setFloat64Field(-123e44);
        builder.setTextField(new Text.Reader("bar"));
        builder.setEnumField(Test.TestEnum.QUX);
    }

    public static void checkSettedDefaultMessage(Test.TestDefaults.Reader reader) {
        Assert.assertEquals(reader.getBoolField(), false);
        Assert.assertEquals(reader.getInt8Field(), -122);
        Assert.assertEquals(reader.getInt16Field(), -12344);
        Assert.assertEquals(reader.getInt32Field(), -12345677);
        Assert.assertEquals(reader.getInt64Field(), -123456789012344L);
        Assert.assertEquals(reader.getUInt8Field(), (byte) 0xe9);
        Assert.assertEquals(reader.getUInt16Field(), (short) 45677);
        Assert.assertEquals(reader.getUInt32Field(), 0xce0a6a13);
        Assert.assertEquals(reader.getUInt64Field(), 0xab54a98ceb1f0ad1L);
        Assert.assertEquals(reader.getFloat32Field(), 1234.4f, DELTA);
        Assert.assertEquals(reader.getFloat64Field(), -123e44, DELTA);
        Assert.assertEquals(reader.getEnumField(), Test.TestEnum.QUX);
    }
}
