package org.capnproto.test;

import org.capnproto.*;
import org.junit.jupiter.api.Assertions;

import java.io.UnsupportedEncodingException;

public class TestUtil {
    public static byte[] data(String string) {
        try {
            return string.getBytes("ISO_8859-1");
        } catch (UnsupportedEncodingException e) {
            Assertions.fail("Could not decode");
            return null;
        }
    }

    public static final double DELTA = 1e-15;

    public static void initTestMessage(org.capnproto.test.Test.TestAllTypes.Builder builder) {
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
            org.capnproto.test.Test.TestAllTypes.Builder subBuilder = builder.initStructField();
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
                org.capnproto.test.Test.TestAllTypes.Builder subSubBuilder = subBuilder.initStructField();
                subSubBuilder.setTextField(new Text.Reader("nested"));
                subSubBuilder.initStructField().setTextField(new Text.Reader("really nested"));
            }
            subBuilder.setEnumField(org.capnproto.test.Test.TestEnum.BAZ);

            PrimitiveList.Boolean.Builder boolList = subBuilder.initBoolList(5);
            boolList.set(0, false);
            boolList.set(1, true);
            boolList.set(2, false);
            boolList.set(3, true);
            boolList.set(4, true);
        }

        builder.setEnumField(org.capnproto.test.Test.TestEnum.CORGE);
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

        StructList.Builder<org.capnproto.test.Test.TestAllTypes.Builder> structList = builder.initStructList(3);
        structList.get(0).setTextField(new Text.Reader("structlist 1"));
        structList.get(1).setTextField(new Text.Reader("structlist 2"));
        structList.get(2).setTextField(new Text.Reader("structlist 3"));

        EnumList.Builder<org.capnproto.test.Test.TestEnum> enumList = builder.initEnumList(2);
        enumList.set(0, org.capnproto.test.Test.TestEnum.FOO);
        enumList.set(1, org.capnproto.test.Test.TestEnum.GARPLY);
    }

    public static void checkTestMessage(org.capnproto.test.Test.TestAllTypes.Builder builder) {
        builder.getVoidField();
        Assertions.assertEquals(builder.getBoolField(), true);
        Assertions.assertEquals(builder.getInt8Field(), -123);
        Assertions.assertEquals(builder.getInt16Field(), -12345);
        Assertions.assertEquals(builder.getInt32Field(), -12345678);
        Assertions.assertEquals(builder.getInt64Field(), -123456789012345L);
        Assertions.assertEquals(builder.getUInt8Field(), (byte) 0xea);
        Assertions.assertEquals(builder.getUInt16Field(), 0x4567);
        Assertions.assertEquals(builder.getUInt32Field(), 0x34567890);
        Assertions.assertEquals(builder.getUInt64Field(), 0x1234567890123456L);
        Assertions.assertEquals(builder.getFloat32Field(), 1234.5f, DELTA);
        Assertions.assertEquals(builder.getFloat64Field(), -123e45, DELTA);
        Assertions.assertEquals(builder.getTextField().toString(), "foo");

        {
            org.capnproto.test.Test.TestAllTypes.Builder subBuilder = builder.getStructField();
            subBuilder.getVoidField();
            Assertions.assertEquals(subBuilder.getBoolField(), true);
            Assertions.assertEquals(subBuilder.getInt8Field(), -12);
            Assertions.assertEquals(subBuilder.getInt16Field(), 3456);
            Assertions.assertEquals(subBuilder.getInt32Field(), -78901234);
            Assertions.assertEquals(subBuilder.getInt64Field(), 56789012345678L);
            Assertions.assertEquals(subBuilder.getUInt8Field(), 90);
            Assertions.assertEquals(subBuilder.getUInt16Field(), 1234);
            Assertions.assertEquals(subBuilder.getUInt32Field(), 56789012);
            Assertions.assertEquals(subBuilder.getUInt64Field(), 345678901234567890L);
            Assertions.assertEquals(subBuilder.getFloat32Field(), -1.25e-10f, DELTA);
            Assertions.assertEquals(subBuilder.getFloat64Field(), 345, DELTA);

            {
                org.capnproto.test.Test.TestAllTypes.Builder subSubBuilder = subBuilder.getStructField();
                Assertions.assertEquals(subSubBuilder.getTextField().toString(), "nested");
            }

            Assertions.assertEquals(subBuilder.getEnumField(), org.capnproto.test.Test.TestEnum.BAZ);

            PrimitiveList.Boolean.Builder boolList = subBuilder.getBoolList();
            Assertions.assertEquals(boolList.get(0), false);
            Assertions.assertEquals(boolList.get(1), true);
            Assertions.assertEquals(boolList.get(2), false);
            Assertions.assertEquals(boolList.get(3), true);
            Assertions.assertEquals(boolList.get(4), true);

        }
        Assertions.assertEquals(builder.getEnumField(), org.capnproto.test.Test.TestEnum.CORGE);

        Assertions.assertEquals(builder.getVoidList().size(), 6);

        PrimitiveList.Boolean.Builder boolList = builder.getBoolList();
        Assertions.assertEquals(boolList.get(0), true);
        Assertions.assertEquals(boolList.get(1), false);
        Assertions.assertEquals(boolList.get(2), false);
        Assertions.assertEquals(boolList.get(3), true);

        PrimitiveList.Double.Builder float64List = builder.getFloat64List();
        Assertions.assertEquals(float64List.get(0), 7777.75, DELTA);
        Assertions.assertEquals(float64List.get(1), Double.POSITIVE_INFINITY, DELTA);
        Assertions.assertEquals(float64List.get(2), Double.NEGATIVE_INFINITY, DELTA);
        Assertions.assertEquals(float64List.get(3), Double.NaN, DELTA);

        TextList.Builder textList = builder.getTextList();
        Assertions.assertEquals(textList.size(), 3);
        Assertions.assertEquals(textList.get(0).toString(), "plugh");
        Assertions.assertEquals(textList.get(1).toString(), "xyzzy");
        Assertions.assertEquals(textList.get(2).toString(), "thud");

        StructList.Builder<org.capnproto.test.Test.TestAllTypes.Builder> structList = builder.getStructList();
        Assertions.assertEquals(3, structList.size());
        Assertions.assertEquals(structList.get(0).getTextField().toString(), "structlist 1");
        Assertions.assertEquals(structList.get(1).getTextField().toString(), "structlist 2");
        Assertions.assertEquals(structList.get(2).getTextField().toString(), "structlist 3");

        EnumList.Builder<org.capnproto.test.Test.TestEnum> enumList = builder.getEnumList();
        Assertions.assertEquals(enumList.get(0), org.capnproto.test.Test.TestEnum.FOO);
        Assertions.assertEquals(enumList.get(1), org.capnproto.test.Test.TestEnum.GARPLY);
    }

    public static void checkTestMessage(org.capnproto.test.Test.TestAllTypes.Reader reader) {
        reader.getVoidField();
        Assertions.assertEquals(reader.getBoolField(), true);
        Assertions.assertEquals(reader.getInt8Field(), -123);
        Assertions.assertEquals(reader.getInt16Field(), -12345);
        Assertions.assertEquals(reader.getInt32Field(), -12345678);
        Assertions.assertEquals(reader.getInt64Field(), -123456789012345L);
        Assertions.assertEquals(reader.getUInt8Field(), (byte)0xea);
        Assertions.assertEquals(reader.getUInt16Field(), 0x4567);
        Assertions.assertEquals(reader.getUInt32Field(), 0x34567890);
        Assertions.assertEquals(reader.getUInt64Field(), 0x1234567890123456L);
        Assertions.assertEquals(reader.getFloat32Field(), 1234.5f, DELTA);
        Assertions.assertEquals(reader.getFloat64Field(), -123e45, DELTA);
        Assertions.assertEquals(reader.getTextField().toString(), "foo");

        {
            org.capnproto.test.Test.TestAllTypes.Reader subReader = reader.getStructField();
            subReader.getVoidField();
            Assertions.assertEquals(subReader.getBoolField(), true);
            Assertions.assertEquals(subReader.getInt8Field(), -12);
            Assertions.assertEquals(subReader.getInt16Field(), 3456);
            Assertions.assertEquals(subReader.getInt32Field(), -78901234);
            Assertions.assertEquals(subReader.getInt64Field(), 56789012345678L);
            Assertions.assertEquals(subReader.getUInt8Field(), 90);
            Assertions.assertEquals(subReader.getUInt16Field(), 1234);
            Assertions.assertEquals(subReader.getUInt32Field(), 56789012);
            Assertions.assertEquals(subReader.getUInt64Field(), 345678901234567890L);
            Assertions.assertEquals(subReader.getFloat32Field(), -1.25e-10f, DELTA);
            Assertions.assertEquals(subReader.getFloat64Field(), 345, DELTA);

            {
                org.capnproto.test.Test.TestAllTypes.Reader subSubReader = subReader.getStructField();
                Assertions.assertEquals(subSubReader.getTextField().toString(), "nested");
            }
            PrimitiveList.Boolean.Reader boolList = subReader.getBoolList();
            Assertions.assertEquals(boolList.get(0), false);
            Assertions.assertEquals(boolList.get(1), true);
            Assertions.assertEquals(boolList.get(2), false);
            Assertions.assertEquals(boolList.get(3), true);
            Assertions.assertEquals(boolList.get(4), true);

        }

        Assertions.assertEquals(reader.getVoidList().size(), 6);

        PrimitiveList.Boolean.Reader boolList = reader.getBoolList();
        Assertions.assertEquals(boolList.get(0), true);
        Assertions.assertEquals(boolList.get(1), false);
        Assertions.assertEquals(boolList.get(2), false);
        Assertions.assertEquals(boolList.get(3), true);

        PrimitiveList.Double.Reader float64List = reader.getFloat64List();
        Assertions.assertEquals(float64List.get(0), 7777.75, DELTA);
        Assertions.assertEquals(float64List.get(1), Double.POSITIVE_INFINITY, DELTA);
        Assertions.assertEquals(float64List.get(2), Double.NEGATIVE_INFINITY, DELTA);
        Assertions.assertEquals(float64List.get(3), Double.NaN, DELTA);

        TextList.Reader textList = reader.getTextList();
        Assertions.assertEquals(textList.size(), 3);
        Assertions.assertEquals(textList.get(0).toString(), "plugh");
        Assertions.assertEquals(textList.get(1).toString(), "xyzzy");
        Assertions.assertEquals(textList.get(2).toString(), "thud");
        StructList.Reader<org.capnproto.test.Test.TestAllTypes.Reader> structList = reader.getStructList();
        Assertions.assertEquals(3, structList.size());
        Assertions.assertEquals(structList.get(0).getTextField().toString(), "structlist 1");
        Assertions.assertEquals(structList.get(1).getTextField().toString(), "structlist 2");
        Assertions.assertEquals(structList.get(2).getTextField().toString(), "structlist 3");

        EnumList.Reader<org.capnproto.test.Test.TestEnum> enumList = reader.getEnumList();
        Assertions.assertEquals(enumList.get(0), org.capnproto.test.Test.TestEnum.FOO);
        Assertions.assertEquals(enumList.get(1), org.capnproto.test.Test.TestEnum.GARPLY);
    }

    public static void checkDefaultMessage(org.capnproto.test.Test.TestDefaults.Builder builder) {
        builder.getVoidField();
        Assertions.assertTrue(builder.getBoolField());
        Assertions.assertEquals(builder.getInt8Field(), -123);
        Assertions.assertEquals(builder.getInt16Field(), -12345);
        Assertions.assertEquals(builder.getInt32Field(), -12345678);
        Assertions.assertEquals(builder.getInt64Field(), -123456789012345L);
        Assertions.assertEquals(builder.getUInt8Field(), (byte)0xea);
        Assertions.assertEquals(builder.getUInt16Field(), (short)45678);
        Assertions.assertEquals(builder.getUInt32Field(), 0xce0a6a14);
        Assertions.assertEquals(builder.getUInt64Field(), 0xab54a98ceb1f0ad2L);
        Assertions.assertEquals(builder.getFloat32Field(), 1234.5f, DELTA);
        Assertions.assertEquals(builder.getFloat64Field(), -123e45, DELTA);
        Assertions.assertEquals(builder.getEnumField(), org.capnproto.test.Test.TestEnum.CORGE);

        Assertions.assertEquals(builder.getTextField().toString(), "foo");
        Assertions.assertArrayEquals(builder.getDataField().toArray(), new byte[]{0x62, 0x61, 0x72});
    }

    public static void checkDefaultMessage(org.capnproto.test.Test.TestDefaults.Reader reader) {
        reader.getVoidField();
        Assertions.assertEquals(reader.getBoolField(), true);
        Assertions.assertEquals(reader.getInt8Field(), -123);
        Assertions.assertEquals(reader.getInt16Field(), -12345);
        Assertions.assertEquals(reader.getInt32Field(), -12345678);
        Assertions.assertEquals(reader.getInt64Field(), -123456789012345L);
        Assertions.assertEquals(reader.getUInt8Field(), (byte)0xea);
        Assertions.assertEquals(reader.getUInt16Field(), (short)45678);
        Assertions.assertEquals(reader.getUInt32Field(), 0xce0a6a14);
        Assertions.assertEquals(reader.getUInt64Field(), 0xab54a98ceb1f0ad2L);
        Assertions.assertEquals(reader.getFloat32Field(), 1234.5f, DELTA);
        Assertions.assertEquals(reader.getFloat64Field(), -123e45, DELTA);
        Assertions.assertEquals(reader.getTextField().toString(), "foo");
        Assertions.assertArrayEquals(reader.getDataField().toArray(), new byte[]{0x62, 0x61, 0x72});
        {
            org.capnproto.test.Test.TestAllTypes.Reader subReader = reader.getStructField();
            subReader.getVoidField();
            Assertions.assertTrue(subReader.getBoolField());
            Assertions.assertEquals(subReader.getInt8Field(), -12);
            Assertions.assertEquals(subReader.getInt16Field(), 3456);
            Assertions.assertEquals(subReader.getInt32Field(), -78901234);
            // ...
            Assertions.assertEquals(subReader.getTextField().toString(), "baz");

            {
                org.capnproto.test.Test.TestAllTypes.Reader subSubReader = subReader.getStructField();
                Assertions.assertEquals(subSubReader.getTextField().toString(), "nested");
            }

        }

        Assertions.assertEquals(reader.getEnumField(), org.capnproto.test.Test.TestEnum.CORGE);
        Assertions.assertEquals(reader.getVoidList().size(), 6);

        {
            PrimitiveList.Boolean.Reader listReader = reader.getBoolList();
            Assertions.assertEquals(listReader.size(), 4);
            Assertions.assertTrue(listReader.get(0));
            Assertions.assertFalse(listReader.get(1));
            Assertions.assertFalse(listReader.get(2));
            Assertions.assertTrue(listReader.get(3));
        }

        {
            PrimitiveList.Byte.Reader listReader = reader.getInt8List();
            Assertions.assertEquals(listReader.size(), 2);
            Assertions.assertEquals(listReader.get(0), 111);
            Assertions.assertEquals(listReader.get(1), -111);
        }
    }

    public static void setDefaultMessage(org.capnproto.test.Test.TestDefaults.Builder builder) {
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
        builder.setEnumField(org.capnproto.test.Test.TestEnum.QUX);
    }

    public static void checkSettedDefaultMessage(org.capnproto.test.Test.TestDefaults.Reader reader) {
        Assertions.assertEquals(reader.getBoolField(), false);
        Assertions.assertEquals(reader.getInt8Field(), -122);
        Assertions.assertEquals(reader.getInt16Field(), -12344);
        Assertions.assertEquals(reader.getInt32Field(), -12345677);
        Assertions.assertEquals(reader.getInt64Field(), -123456789012344L);
        Assertions.assertEquals(reader.getUInt8Field(), (byte) 0xe9);
        Assertions.assertEquals(reader.getUInt16Field(), (short) 45677);
        Assertions.assertEquals(reader.getUInt32Field(), 0xce0a6a13);
        Assertions.assertEquals(reader.getUInt64Field(), 0xab54a98ceb1f0ad1L);
        Assertions.assertEquals(reader.getFloat32Field(), 1234.4f, DELTA);
        Assertions.assertEquals(reader.getFloat64Field(), -123e44, DELTA);
        Assertions.assertEquals(reader.getEnumField(), org.capnproto.test.Test.TestEnum.QUX);
    }
}
