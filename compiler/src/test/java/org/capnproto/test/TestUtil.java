package org.capnproto.test;

import org.capnproto.*;
import org.junit.jupiter.api.Assertions;

import java.io.UnsupportedEncodingException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class TestUtil {
    public static byte[] data(String string) {
        try {
            return string.getBytes("ISO_8859-1");
        } catch (UnsupportedEncodingException e) {
            fail("Could not decode");
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
        assertEquals(builder.getBoolField(), true);
        assertEquals(builder.getInt8Field(), -123);
        assertEquals(builder.getInt16Field(), -12345);
        assertEquals(builder.getInt32Field(), -12345678);
        assertEquals(builder.getInt64Field(), -123456789012345L);
        assertEquals(builder.getUInt8Field(), (byte) 0xea);
        assertEquals(builder.getUInt16Field(), 0x4567);
        assertEquals(builder.getUInt32Field(), 0x34567890);
        assertEquals(builder.getUInt64Field(), 0x1234567890123456L);
        assertEquals(builder.getFloat32Field(), 1234.5f, DELTA);
        assertEquals(builder.getFloat64Field(), -123e45, DELTA);
        assertEquals(builder.getTextField().toString(), "foo");

        {
            org.capnproto.test.Test.TestAllTypes.Builder subBuilder = builder.getStructField();
            subBuilder.getVoidField();
            assertEquals(subBuilder.getBoolField(), true);
            assertEquals(subBuilder.getInt8Field(), -12);
            assertEquals(subBuilder.getInt16Field(), 3456);
            assertEquals(subBuilder.getInt32Field(), -78901234);
            assertEquals(subBuilder.getInt64Field(), 56789012345678L);
            assertEquals(subBuilder.getUInt8Field(), 90);
            assertEquals(subBuilder.getUInt16Field(), 1234);
            assertEquals(subBuilder.getUInt32Field(), 56789012);
            assertEquals(subBuilder.getUInt64Field(), 345678901234567890L);
            assertEquals(subBuilder.getFloat32Field(), -1.25e-10f, DELTA);
            assertEquals(subBuilder.getFloat64Field(), 345, DELTA);

            {
                org.capnproto.test.Test.TestAllTypes.Builder subSubBuilder = subBuilder.getStructField();
                assertEquals(subSubBuilder.getTextField().toString(), "nested");
            }

            assertEquals(subBuilder.getEnumField(), org.capnproto.test.Test.TestEnum.BAZ);

            PrimitiveList.Boolean.Builder boolList = subBuilder.getBoolList();
            assertEquals(boolList.get(0), false);
            assertEquals(boolList.get(1), true);
            assertEquals(boolList.get(2), false);
            assertEquals(boolList.get(3), true);
            assertEquals(boolList.get(4), true);

        }
        assertEquals(builder.getEnumField(), org.capnproto.test.Test.TestEnum.CORGE);

        assertEquals(builder.getVoidList().size(), 6);

        PrimitiveList.Boolean.Builder boolList = builder.getBoolList();
        assertEquals(boolList.get(0), true);
        assertEquals(boolList.get(1), false);
        assertEquals(boolList.get(2), false);
        assertEquals(boolList.get(3), true);

        PrimitiveList.Double.Builder float64List = builder.getFloat64List();
        assertEquals(float64List.get(0), 7777.75, DELTA);
        assertEquals(float64List.get(1), Double.POSITIVE_INFINITY, DELTA);
        assertEquals(float64List.get(2), Double.NEGATIVE_INFINITY, DELTA);
        assertEquals(float64List.get(3), Double.NaN, DELTA);

        TextList.Builder textList = builder.getTextList();
        assertEquals(textList.size(), 3);
        assertEquals(textList.get(0).toString(), "plugh");
        assertEquals(textList.get(1).toString(), "xyzzy");
        assertEquals(textList.get(2).toString(), "thud");

        StructList.Builder<org.capnproto.test.Test.TestAllTypes.Builder> structList = builder.getStructList();
        assertEquals(3, structList.size());
        assertEquals(structList.get(0).getTextField().toString(), "structlist 1");
        assertEquals(structList.get(1).getTextField().toString(), "structlist 2");
        assertEquals(structList.get(2).getTextField().toString(), "structlist 3");

        EnumList.Builder<org.capnproto.test.Test.TestEnum> enumList = builder.getEnumList();
        assertEquals(enumList.get(0), org.capnproto.test.Test.TestEnum.FOO);
        assertEquals(enumList.get(1), org.capnproto.test.Test.TestEnum.GARPLY);
    }

    public static void checkTestMessage(org.capnproto.test.Test.TestAllTypes.Reader reader) {
        reader.getVoidField();
        assertEquals(reader.getBoolField(), true);
        assertEquals(reader.getInt8Field(), -123);
        assertEquals(reader.getInt16Field(), -12345);
        assertEquals(reader.getInt32Field(), -12345678);
        assertEquals(reader.getInt64Field(), -123456789012345L);
        assertEquals(reader.getUInt8Field(), (byte)0xea);
        assertEquals(reader.getUInt16Field(), 0x4567);
        assertEquals(reader.getUInt32Field(), 0x34567890);
        assertEquals(reader.getUInt64Field(), 0x1234567890123456L);
        assertEquals(reader.getFloat32Field(), 1234.5f, DELTA);
        assertEquals(reader.getFloat64Field(), -123e45, DELTA);
        assertEquals(reader.getTextField().toString(), "foo");

        {
            org.capnproto.test.Test.TestAllTypes.Reader subReader = reader.getStructField();
            subReader.getVoidField();
            assertEquals(subReader.getBoolField(), true);
            assertEquals(subReader.getInt8Field(), -12);
            assertEquals(subReader.getInt16Field(), 3456);
            assertEquals(subReader.getInt32Field(), -78901234);
            assertEquals(subReader.getInt64Field(), 56789012345678L);
            assertEquals(subReader.getUInt8Field(), 90);
            assertEquals(subReader.getUInt16Field(), 1234);
            assertEquals(subReader.getUInt32Field(), 56789012);
            assertEquals(subReader.getUInt64Field(), 345678901234567890L);
            assertEquals(subReader.getFloat32Field(), -1.25e-10f, DELTA);
            assertEquals(subReader.getFloat64Field(), 345, DELTA);

            {
                org.capnproto.test.Test.TestAllTypes.Reader subSubReader = subReader.getStructField();
                assertEquals(subSubReader.getTextField().toString(), "nested");
            }
            PrimitiveList.Boolean.Reader boolList = subReader.getBoolList();
            assertEquals(boolList.get(0), false);
            assertEquals(boolList.get(1), true);
            assertEquals(boolList.get(2), false);
            assertEquals(boolList.get(3), true);
            assertEquals(boolList.get(4), true);

        }

        assertEquals(reader.getVoidList().size(), 6);

        PrimitiveList.Boolean.Reader boolList = reader.getBoolList();
        assertEquals(boolList.get(0), true);
        assertEquals(boolList.get(1), false);
        assertEquals(boolList.get(2), false);
        assertEquals(boolList.get(3), true);

        PrimitiveList.Double.Reader float64List = reader.getFloat64List();
        assertEquals(float64List.get(0), 7777.75, DELTA);
        assertEquals(float64List.get(1), Double.POSITIVE_INFINITY, DELTA);
        assertEquals(float64List.get(2), Double.NEGATIVE_INFINITY, DELTA);
        assertEquals(float64List.get(3), Double.NaN, DELTA);

        TextList.Reader textList = reader.getTextList();
        assertEquals(textList.size(), 3);
        assertEquals(textList.get(0).toString(), "plugh");
        assertEquals(textList.get(1).toString(), "xyzzy");
        assertEquals(textList.get(2).toString(), "thud");
        StructList.Reader<org.capnproto.test.Test.TestAllTypes.Reader> structList = reader.getStructList();
        assertEquals(3, structList.size());
        assertEquals(structList.get(0).getTextField().toString(), "structlist 1");
        assertEquals(structList.get(1).getTextField().toString(), "structlist 2");
        assertEquals(structList.get(2).getTextField().toString(), "structlist 3");

        EnumList.Reader<org.capnproto.test.Test.TestEnum> enumList = reader.getEnumList();
        assertEquals(enumList.get(0), org.capnproto.test.Test.TestEnum.FOO);
        assertEquals(enumList.get(1), org.capnproto.test.Test.TestEnum.GARPLY);
    }

    public static void checkDefaultMessage(org.capnproto.test.Test.TestDefaults.Builder builder) {
        builder.getVoidField();
        assertTrue(builder.getBoolField());
        assertEquals(builder.getInt8Field(), -123);
        assertEquals(builder.getInt16Field(), -12345);
        assertEquals(builder.getInt32Field(), -12345678);
        assertEquals(builder.getInt64Field(), -123456789012345L);
        assertEquals(builder.getUInt8Field(), (byte)0xea);
        assertEquals(builder.getUInt16Field(), (short)45678);
        assertEquals(builder.getUInt32Field(), 0xce0a6a14);
        assertEquals(builder.getUInt64Field(), 0xab54a98ceb1f0ad2L);
        assertEquals(builder.getFloat32Field(), 1234.5f, DELTA);
        assertEquals(builder.getFloat64Field(), -123e45, DELTA);
        assertEquals(builder.getEnumField(), org.capnproto.test.Test.TestEnum.CORGE);

        assertEquals(builder.getTextField().toString(), "foo");
        assertArrayEquals(builder.getDataField().toArray(), new byte[]{0x62, 0x61, 0x72});
    }

    public static void checkDefaultMessage(org.capnproto.test.Test.TestDefaults.Reader reader) {
        reader.getVoidField();
        assertEquals(reader.getBoolField(), true);
        assertEquals(reader.getInt8Field(), -123);
        assertEquals(reader.getInt16Field(), -12345);
        assertEquals(reader.getInt32Field(), -12345678);
        assertEquals(reader.getInt64Field(), -123456789012345L);
        assertEquals(reader.getUInt8Field(), (byte)0xea);
        assertEquals(reader.getUInt16Field(), (short)45678);
        assertEquals(reader.getUInt32Field(), 0xce0a6a14);
        assertEquals(reader.getUInt64Field(), 0xab54a98ceb1f0ad2L);
        assertEquals(reader.getFloat32Field(), 1234.5f, DELTA);
        assertEquals(reader.getFloat64Field(), -123e45, DELTA);
        assertEquals(reader.getTextField().toString(), "foo");
        assertArrayEquals(reader.getDataField().toArray(), new byte[]{0x62, 0x61, 0x72});
        {
            org.capnproto.test.Test.TestAllTypes.Reader subReader = reader.getStructField();
            subReader.getVoidField();
            assertTrue(subReader.getBoolField());
            assertEquals(subReader.getInt8Field(), -12);
            assertEquals(subReader.getInt16Field(), 3456);
            assertEquals(subReader.getInt32Field(), -78901234);
            // ...
            assertEquals(subReader.getTextField().toString(), "baz");

            {
                org.capnproto.test.Test.TestAllTypes.Reader subSubReader = subReader.getStructField();
                assertEquals(subSubReader.getTextField().toString(), "nested");
            }

        }

        assertEquals(reader.getEnumField(), org.capnproto.test.Test.TestEnum.CORGE);
        assertEquals(reader.getVoidList().size(), 6);

        {
            PrimitiveList.Boolean.Reader listReader = reader.getBoolList();
            assertEquals(listReader.size(), 4);
            assertTrue(listReader.get(0));
            assertFalse(listReader.get(1));
            assertFalse(listReader.get(2));
            assertTrue(listReader.get(3));
        }

        {
            PrimitiveList.Byte.Reader listReader = reader.getInt8List();
            assertEquals(listReader.size(), 2);
            assertEquals(listReader.get(0), 111);
            assertEquals(listReader.get(1), -111);
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
        assertEquals(reader.getBoolField(), false);
        assertEquals(reader.getInt8Field(), -122);
        assertEquals(reader.getInt16Field(), -12344);
        assertEquals(reader.getInt32Field(), -12345677);
        assertEquals(reader.getInt64Field(), -123456789012344L);
        assertEquals(reader.getUInt8Field(), (byte) 0xe9);
        assertEquals(reader.getUInt16Field(), (short) 45677);
        assertEquals(reader.getUInt32Field(), 0xce0a6a13);
        assertEquals(reader.getUInt64Field(), 0xab54a98ceb1f0ad1L);
        assertEquals(reader.getFloat32Field(), 1234.4f, DELTA);
        assertEquals(reader.getFloat64Field(), -123e44, DELTA);
        assertEquals(reader.getEnumField(), org.capnproto.test.Test.TestEnum.QUX);
    }
}
