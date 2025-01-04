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
        assertEquals(true, builder.getBoolField());
        assertEquals(-123, builder.getInt8Field());
        assertEquals(-12345, builder.getInt16Field());
        assertEquals(-12345678, builder.getInt32Field());
        assertEquals(-123456789012345L, builder.getInt64Field());
        assertEquals((byte) 0xea, builder.getUInt8Field());
        assertEquals(0x4567, builder.getUInt16Field());
        assertEquals(0x34567890, builder.getUInt32Field());
        assertEquals(0x1234567890123456L, builder.getUInt64Field());
        assertEquals(1234.5f, builder.getFloat32Field(), DELTA);
        assertEquals(-123e45, builder.getFloat64Field(), DELTA);
        assertEquals("foo", builder.getTextField().toString());

        {
            org.capnproto.test.Test.TestAllTypes.Builder subBuilder = builder.getStructField();
            subBuilder.getVoidField();
            assertEquals(true, subBuilder.getBoolField());
            assertEquals(-12, subBuilder.getInt8Field());
            assertEquals(3456, subBuilder.getInt16Field());
            assertEquals(-78901234, subBuilder.getInt32Field());
            assertEquals(56789012345678L, subBuilder.getInt64Field());
            assertEquals(90, subBuilder.getUInt8Field());
            assertEquals(1234, subBuilder.getUInt16Field());
            assertEquals(56789012, subBuilder.getUInt32Field());
            assertEquals(345678901234567890L, subBuilder.getUInt64Field());
            assertEquals(-1.25e-10f, subBuilder.getFloat32Field(), DELTA);
            assertEquals(345, subBuilder.getFloat64Field(), DELTA);

            {
                org.capnproto.test.Test.TestAllTypes.Builder subSubBuilder = subBuilder.getStructField();
                assertEquals("nested", subSubBuilder.getTextField().toString());
            }

            assertEquals(subBuilder.getEnumField(), org.capnproto.test.Test.TestEnum.BAZ);

            PrimitiveList.Boolean.Builder boolList = subBuilder.getBoolList();
            assertEquals(false, boolList.get(0));
            assertEquals(true, boolList.get(1));
            assertEquals(false, boolList.get(2));
            assertEquals(true, boolList.get(3));
            assertEquals(true, boolList.get(4));

        }
        assertEquals(builder.getEnumField(), org.capnproto.test.Test.TestEnum.CORGE);

        assertEquals(6, builder.getVoidList().size());

        PrimitiveList.Boolean.Builder boolList = builder.getBoolList();
        assertEquals(true, boolList.get(0));
        assertEquals(false, boolList.get(1));
        assertEquals(false, boolList.get(2));
        assertEquals(true, boolList.get(3));

        PrimitiveList.Double.Builder float64List = builder.getFloat64List();
        assertEquals(7777.75, float64List.get(0), DELTA);
        assertEquals(Double.POSITIVE_INFINITY, float64List.get(1), DELTA);
        assertEquals(Double.NEGATIVE_INFINITY, float64List.get(2), DELTA);
        assertEquals(Double.NaN, float64List.get(3), DELTA);

        TextList.Builder textList = builder.getTextList();
        assertEquals(3, textList.size());
        assertEquals("plugh", textList.get(0).toString());
        assertEquals("xyzzy", textList.get(1).toString());
        assertEquals("thud", textList.get(2).toString());

        StructList.Builder<org.capnproto.test.Test.TestAllTypes.Builder> structList = builder.getStructList();
        assertEquals(3, structList.size());
        assertEquals("structlist 1", structList.get(0).getTextField().toString());
        assertEquals("structlist 2", structList.get(1).getTextField().toString());
        assertEquals("structlist 3", structList.get(2).getTextField().toString());

        EnumList.Builder<org.capnproto.test.Test.TestEnum> enumList = builder.getEnumList();
        assertEquals(enumList.get(0), org.capnproto.test.Test.TestEnum.FOO);
        assertEquals(enumList.get(1), org.capnproto.test.Test.TestEnum.GARPLY);
    }

    public static void checkTestMessage(org.capnproto.test.Test.TestAllTypes.Reader reader) {
        reader.getVoidField();
        assertEquals(true, reader.getBoolField());
        assertEquals(-123, reader.getInt8Field());
        assertEquals(-12345, reader.getInt16Field());
        assertEquals(-12345678, reader.getInt32Field());
        assertEquals(-123456789012345L, reader.getInt64Field());
        assertEquals((byte)0xea, reader.getUInt8Field());
        assertEquals(0x4567, reader.getUInt16Field());
        assertEquals(0x34567890, reader.getUInt32Field());
        assertEquals(0x1234567890123456L, reader.getUInt64Field());
        assertEquals(1234.5f, reader.getFloat32Field(), DELTA);
        assertEquals(-123e45, reader.getFloat64Field(), DELTA);
        assertEquals("foo", reader.getTextField().toString());

        {
            org.capnproto.test.Test.TestAllTypes.Reader subReader = reader.getStructField();
            subReader.getVoidField();
            assertEquals(true, subReader.getBoolField());
            assertEquals(-12, subReader.getInt8Field());
            assertEquals(3456, subReader.getInt16Field());
            assertEquals(-78901234, subReader.getInt32Field());
            assertEquals(56789012345678L, subReader.getInt64Field());
            assertEquals(90, subReader.getUInt8Field());
            assertEquals(1234, subReader.getUInt16Field());
            assertEquals(56789012, subReader.getUInt32Field());
            assertEquals(345678901234567890L, subReader.getUInt64Field());
            assertEquals(-1.25e-10f, subReader.getFloat32Field(), DELTA);
            assertEquals(345, subReader.getFloat64Field(), DELTA);

            {
                org.capnproto.test.Test.TestAllTypes.Reader subSubReader = subReader.getStructField();
                assertEquals("nested", subSubReader.getTextField().toString());
            }
            PrimitiveList.Boolean.Reader boolList = subReader.getBoolList();
            assertEquals(false, boolList.get(0));
            assertEquals(true, boolList.get(1));
            assertEquals(false, boolList.get(2));
            assertEquals(true, boolList.get(3));
            assertEquals(true, boolList.get(4));

        }

        assertEquals(6, reader.getVoidList().size());

        PrimitiveList.Boolean.Reader boolList = reader.getBoolList();
        assertEquals(true, boolList.get(0));
        assertEquals(false, boolList.get(1));
        assertEquals(false, boolList.get(2));
        assertEquals(true, boolList.get(3));

        PrimitiveList.Double.Reader float64List = reader.getFloat64List();
        assertEquals(7777.75, float64List.get(0), DELTA);
        assertEquals(Double.POSITIVE_INFINITY, float64List.get(1), DELTA);
        assertEquals(Double.NEGATIVE_INFINITY, float64List.get(2), DELTA);
        assertEquals(Double.NaN, float64List.get(3), DELTA);

        TextList.Reader textList = reader.getTextList();
        assertEquals(3, textList.size());
        assertEquals("plugh", textList.get(0).toString());
        assertEquals("xyzzy", textList.get(1).toString());
        assertEquals("thud", textList.get(2).toString());
        StructList.Reader<org.capnproto.test.Test.TestAllTypes.Reader> structList = reader.getStructList();
        assertEquals(3, structList.size());
        assertEquals("structlist 1", structList.get(0).getTextField().toString());
        assertEquals("structlist 2", structList.get(1).getTextField().toString());
        assertEquals("structlist 3", structList.get(2).getTextField().toString());

        EnumList.Reader<org.capnproto.test.Test.TestEnum> enumList = reader.getEnumList();
        assertEquals(enumList.get(0), org.capnproto.test.Test.TestEnum.FOO);
        assertEquals(enumList.get(1), org.capnproto.test.Test.TestEnum.GARPLY);
    }

    public static void checkDefaultMessage(org.capnproto.test.Test.TestDefaults.Builder builder) {
        builder.getVoidField();
        assertTrue(builder.getBoolField());
        assertEquals(-123, builder.getInt8Field());
        assertEquals(-12345, builder.getInt16Field());
        assertEquals(-12345678, builder.getInt32Field());
        assertEquals(-123456789012345L, builder.getInt64Field());
        assertEquals((byte)0xea, builder.getUInt8Field());
        assertEquals((short)45678, builder.getUInt16Field());
        assertEquals(0xce0a6a14, builder.getUInt32Field());
        assertEquals(0xab54a98ceb1f0ad2L, builder.getUInt64Field());
        assertEquals(1234.5f, builder.getFloat32Field(), DELTA);
        assertEquals(-123e45, builder.getFloat64Field(), DELTA);
        assertEquals(builder.getEnumField(), org.capnproto.test.Test.TestEnum.CORGE);

        assertEquals("foo", builder.getTextField().toString());
        assertArrayEquals(new byte[]{0x62, 0x61, 0x72}, builder.getDataField().toArray());
    }

    public static void checkDefaultMessage(org.capnproto.test.Test.TestDefaults.Reader reader) {
        reader.getVoidField();
        assertEquals(true, reader.getBoolField());
        assertEquals(-123, reader.getInt8Field());
        assertEquals(-12345, reader.getInt16Field());
        assertEquals(-12345678, reader.getInt32Field());
        assertEquals(-123456789012345L, reader.getInt64Field());
        assertEquals((byte)0xea, reader.getUInt8Field());
        assertEquals((short)45678, reader.getUInt16Field());
        assertEquals(0xce0a6a14, reader.getUInt32Field());
        assertEquals(0xab54a98ceb1f0ad2L, reader.getUInt64Field());
        assertEquals(1234.5f, reader.getFloat32Field(), DELTA);
        assertEquals(-123e45, reader.getFloat64Field(), DELTA);
        assertEquals("foo", reader.getTextField().toString());
        assertArrayEquals(new byte[]{0x62, 0x61, 0x72}, reader.getDataField().toArray());
        {
            org.capnproto.test.Test.TestAllTypes.Reader subReader = reader.getStructField();
            subReader.getVoidField();
            assertTrue(subReader.getBoolField());
            assertEquals(-12, subReader.getInt8Field());
            assertEquals(3456, subReader.getInt16Field());
            assertEquals(-78901234, subReader.getInt32Field());
            // ...
            assertEquals("baz", subReader.getTextField().toString());

            {
                org.capnproto.test.Test.TestAllTypes.Reader subSubReader = subReader.getStructField();
                assertEquals("nested", subSubReader.getTextField().toString());
            }

        }

        assertEquals(reader.getEnumField(), org.capnproto.test.Test.TestEnum.CORGE);
        assertEquals(6, reader.getVoidList().size());

        {
            PrimitiveList.Boolean.Reader listReader = reader.getBoolList();
            assertEquals(4, listReader.size());
            assertTrue(listReader.get(0));
            assertFalse(listReader.get(1));
            assertFalse(listReader.get(2));
            assertTrue(listReader.get(3));
        }

        {
            PrimitiveList.Byte.Reader listReader = reader.getInt8List();
            assertEquals(2, listReader.size());
            assertEquals(111, listReader.get(0));
            assertEquals(-111, listReader.get(1));
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
        assertEquals(false, reader.getBoolField());
        assertEquals(-122, reader.getInt8Field());
        assertEquals(-12344, reader.getInt16Field());
        assertEquals(-12345677, reader.getInt32Field());
        assertEquals(-123456789012344L, reader.getInt64Field());
        assertEquals((byte) 0xe9, reader.getUInt8Field());
        assertEquals((short) 45677, reader.getUInt16Field());
        assertEquals(0xce0a6a13, reader.getUInt32Field());
        assertEquals(0xab54a98ceb1f0ad1L, reader.getUInt64Field());
        assertEquals(1234.4f, reader.getFloat32Field(), DELTA);
        assertEquals(-123e44, reader.getFloat64Field(), DELTA);
        assertEquals(reader.getEnumField(), org.capnproto.test.Test.TestEnum.QUX);
    }
}
