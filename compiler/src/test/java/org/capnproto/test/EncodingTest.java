package org.capnproto.test;

import org.capnproto.*;
import org.capnproto.Void;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EncodingTest {
    @Test
    public void testAllTypes() {
        MessageBuilder message = new MessageBuilder();
        org.capnproto.test.Test.TestAllTypes.Builder allTypes = message.initRoot(org.capnproto.test.Test.TestAllTypes.factory);
        TestUtil.initTestMessage(allTypes);
        TestUtil.checkTestMessage(allTypes);
        TestUtil.checkTestMessage(allTypes.asReader());
    }

    @Test
    public void testAllTypesMultiSegment() {
        MessageBuilder message = new MessageBuilder(5, BuilderArena.AllocationStrategy.FIXED_SIZE);
        org.capnproto.test.Test.TestAllTypes.Builder allTypes = message.initRoot(org.capnproto.test.Test.TestAllTypes.factory);
        TestUtil.initTestMessage(allTypes);

        TestUtil.checkTestMessage(allTypes);
        TestUtil.checkTestMessage(allTypes.asReader());
    }

    @Test
    public void testSetters() {
        MessageBuilder message = new MessageBuilder();
        org.capnproto.test.Test.TestAllTypes.Builder allTypes = message.initRoot(org.capnproto.test.Test.TestAllTypes.factory);
        TestUtil.initTestMessage(allTypes);

        MessageBuilder message2 = new MessageBuilder();
        org.capnproto.test.Test.TestAllTypes.Builder allTypes2 = message2.initRoot(org.capnproto.test.Test.TestAllTypes.factory);

        allTypes2.setStructField(allTypes.asReader());
        TestUtil.checkTestMessage(allTypes2.getStructField());

        org.capnproto.test.Test.TestAllTypes.Reader reader = allTypes2.asReader().getStructField();
        TestUtil.checkTestMessage(reader);
    }

    @Test
    public void testZeroing() {
        MessageBuilder message = new MessageBuilder();
        org.capnproto.test.Test.TestAllTypes.Builder allTypes = message.initRoot(org.capnproto.test.Test.TestAllTypes.factory);

        StructList.Builder<org.capnproto.test.Test.TestAllTypes.Builder> structList = allTypes.initStructList(3);
        TestUtil.initTestMessage(structList.get(0));

        org.capnproto.test.Test.TestAllTypes.Builder structField = allTypes.initStructField();
        TestUtil.initTestMessage(structField);

        TestUtil.initTestMessage(structList.get(1));
        TestUtil.initTestMessage(structList.get(2));
        TestUtil.checkTestMessage(structList.get(0));
        allTypes.initStructList(0);

        TestUtil.checkTestMessage(allTypes.getStructField());
        org.capnproto.test.Test.TestAllTypes.Reader allTypesReader = allTypes.asReader();
        TestUtil.checkTestMessage(allTypesReader.getStructField());

        AnyPointer.Builder any = message.initRoot(AnyPointer.factory);
        ByteBuffer[] segments = message.getSegmentsForOutput();
        for (ByteBuffer segment : segments) {
            for (int j = 0; j < segment.limit(); j++) {
                assertEquals(segment.get(j), 0);
            }
        }
    }

    @Test
    public void testDoubleFarPointers() throws IOException {
        byte[] bytes = new byte[]{2, 0, 0, 0, 1, 0, 0, 0, 2, 0, 0, 0, 1, 0, 0, 0,
                6, 0, 0, 0, 1, 0, 0, 0, 2, 0, 0, 0, 2, 0, 0, 0,
                0, 0, 0, 0, 1, 0, 0, 0, 1, 7, -1, 127, 0, 0, 0, 0};

        ArrayInputStream input = new ArrayInputStream(java.nio.ByteBuffer.wrap(bytes));
        MessageReader message = org.capnproto.Serialize.read(input);
        org.capnproto.test.Test.TestAllTypes.Reader root = message.getRoot(org.capnproto.test.Test.TestAllTypes.factory);

        assertTrue(root.getBoolField());
        assertEquals(root.getInt8Field(), 7);
        assertEquals(root.getInt16Field(), 32767);
    }

    @Test
    public void testUpgradeStruct() {
      MessageBuilder builder = new MessageBuilder();
      org.capnproto.test.Test.TestAnyPointer.Builder root = builder.initRoot(org.capnproto.test.Test.TestAnyPointer.factory);

      {
        org.capnproto.test.Test.TestOldVersion.Builder oldVersion = root.getAnyPointerField().initAs(org.capnproto.test.Test.TestOldVersion.factory);
        oldVersion.setOld1(123);
        oldVersion.setOld2("foo");
        org.capnproto.test.Test.TestOldVersion.Builder sub = oldVersion.initOld3();
        sub.setOld1(456);
        sub.setOld2("bar");
      }

      {
        org.capnproto.test.Test.TestNewVersion.Reader newVersion = root.getAnyPointerField().asReader().getAs(org.capnproto.test.Test.TestNewVersion.factory);
        assertEquals(newVersion.getOld1(), 123);
        assertEquals(newVersion.getOld2().toString(), "foo");
        assertEquals(newVersion.getNew2().toString(),  "baz");
        assertEquals(newVersion.hasNew2(), false);
        assertEquals(newVersion.hasNew3(), false);
      }
    }

    @Test
    public void testUpgradeStructReadAsOld() {
      MessageBuilder builder = new MessageBuilder();
      org.capnproto.test.Test.TestAnyPointer.Builder root = builder.initRoot(org.capnproto.test.Test.TestAnyPointer.factory);

      {
        org.capnproto.test.Test.TestNewVersion.Builder newVersion = root.getAnyPointerField().initAs(org.capnproto.test.Test.TestNewVersion.factory);
        newVersion.setOld1(123);
        newVersion.setOld2("foo");
        org.capnproto.test.Test.TestNewVersion.Builder sub = newVersion.initOld3();
        sub.setOld1(456);
        sub.setOld2("bar");

        StructList.Builder<org.capnproto.test.Test.TestNewVersion.UpgradedFromText.Builder> names =
          newVersion.initOld4(2);

        names.get(0).setTextField("alice");
        names.get(1).setTextField("bob");
      }

      {
        org.capnproto.test.Test.TestOldVersion.Reader oldVersion = root.getAnyPointerField().asReader().getAs(org.capnproto.test.Test.TestOldVersion.factory);
        assertEquals(oldVersion.getOld1(), 123);
        assertEquals(oldVersion.getOld2().toString(), "foo");

        TextList.Reader names = oldVersion.getOld4();
        assertEquals(names.size(), 2);
        assertEquals("alice", names.get(0).toString());
        assertEquals("bob", names.get(1).toString());
      }
    }

    @Test
    public void testUpgradeStructInBuilder() {
        MessageBuilder builder = new MessageBuilder();
        org.capnproto.test.Test.TestAnyPointer.Builder root = builder.initRoot(org.capnproto.test.Test.TestAnyPointer.factory);

        {
            org.capnproto.test.Test.TestOldVersion.Builder oldVersion = root.getAnyPointerField().initAs(org.capnproto.test.Test.TestOldVersion.factory);
            oldVersion.setOld1(123);
            oldVersion.setOld2("foo");
            org.capnproto.test.Test.TestOldVersion.Builder sub = oldVersion.initOld3();
            sub.setOld1(456);
            sub.setOld2("bar");
        }
        {
            org.capnproto.test.Test.TestNewVersion.Builder newVersion = root.getAnyPointerField().getAs(org.capnproto.test.Test.TestNewVersion.factory);
            assertEquals(newVersion.getOld1(), 123);
            assertEquals(newVersion.getOld2().toString(), "foo");
            assertEquals(newVersion.getNew1(), 987);
            assertEquals(newVersion.getNew2().toString(), "baz");
            org.capnproto.test.Test.TestNewVersion.Builder sub = newVersion.getOld3();
            assertEquals(sub.getOld1(), 456);
            assertEquals(sub.getOld2().toString(), "bar");

            newVersion.setOld1(234);
            newVersion.setOld2("qux");
            newVersion.setNew1(654);
            newVersion.setNew2("quux");
        }
        {
            org.capnproto.test.Test.TestOldVersion.Builder oldVersion = root.getAnyPointerField().getAs(org.capnproto.test.Test.TestOldVersion.factory);
            assertEquals(oldVersion.getOld1(), 234);
            assertEquals(oldVersion.getOld2().toString(), "qux");
        }
    }

    @Test
    public void testStructListUpgrade() {
        MessageBuilder message = new MessageBuilder();
        org.capnproto.test.Test.TestAnyPointer.Builder root = message.initRoot(org.capnproto.test.Test.TestAnyPointer.factory);
        AnyPointer.Builder any = root.getAnyPointerField();

        {
            PrimitiveList.Long.Builder longs = any.initAs(PrimitiveList.Long.factory, 3);
            longs.set(0, 123);
            longs.set(1, 456);
            longs.set(2, 789);
        }
        {
            StructList.Reader<org.capnproto.test.Test.TestOldVersion.Reader> olds = any.asReader().getAs(org.capnproto.test.Test.TestOldVersion.listFactory);
            assertEquals(olds.get(0).getOld1(), 123);
            assertEquals(olds.get(1).getOld1(), 456);
            assertEquals(olds.get(2).getOld1(), 789);
        }
        {
            StructList.Builder<org.capnproto.test.Test.TestOldVersion.Builder> olds = any.getAs(org.capnproto.test.Test.TestOldVersion.listFactory);
            assertEquals(olds.size(), 3);
            assertEquals(olds.get(0).getOld1(), 123);
            assertEquals(olds.get(1).getOld1(), 456);
            assertEquals(olds.get(2).getOld1(), 789);

            olds.get(0).setOld2("zero");
            olds.get(1).setOld2("one");
            olds.get(2).setOld2("two");
        }
        {
            StructList.Builder<org.capnproto.test.Test.TestNewVersion.Builder> news = any.getAs(org.capnproto.test.Test.TestNewVersion.listFactory);
            assertEquals(news.size(), 3);
            assertEquals(news.get(0).getOld1(), 123);
            assertEquals(news.get(0).getOld2().toString(), "zero");

            assertEquals(news.get(1).getOld1(), 456);
            assertEquals(news.get(1).getOld2().toString(), "one");

            assertEquals(news.get(2).getOld1(), 789);
            assertEquals(news.get(2).getOld2().toString(), "two");
        }
    }

    @Test
    public void testStructListUpgradeDoubleFar() {
      byte[] bytes = new byte[]{
            1,0,0,0,0x1f,0,0,0, // list, inline composite, 3 words
            4, 0, 0, 0, 1, 0, 2, 0, // struct tag. 1 element, 1 word data, 2 pointers.
            91,0,0,0,0,0,0,0, // data: 91
            0x05,0,0,0, 0x42,0,0,0, // list pointer, offset 1, type = BYTE, length 8.
            0,0,0,0,0,0,0,0, // null pointer
            0x68,0x65,0x6c,0x6c,0x6f,0x21,0x21,0}; // "hello!!"

      ByteBuffer segment = java.nio.ByteBuffer.wrap(bytes);
      segment.order(java.nio.ByteOrder.LITTLE_ENDIAN);
      MessageReader messageReader = new MessageReader(new ByteBuffer[]{segment}, ReaderOptions.DEFAULT_READER_OPTIONS);

      StructList.Reader<org.capnproto.test.Test.TestOldVersion.Reader> oldVersion = messageReader.getRoot(StructList.newFactory(org.capnproto.test.Test.TestOldVersion.factory));

      assertEquals(oldVersion.size(), 1);
      assertEquals(oldVersion.get(0).getOld1(), 91);
      assertEquals(oldVersion.get(0).getOld2().toString(), "hello!!");

      // Make the first segment exactly large enough to fit the original message.
      // This leaves no room for a far pointer landing pad in the first segment.
      MessageBuilder message = new MessageBuilder(6);
      message.setRoot(StructList.newFactory(org.capnproto.test.Test.TestOldVersion.factory), oldVersion);

      ByteBuffer[] segments = message.getSegmentsForOutput();
      assertEquals(segments.length, 1);
      assertEquals(segments[0].limit(), 6 * 8);

      StructList.Builder<org.capnproto.test.Test.TestNewVersion.Builder> newVersion =
        message.getRoot(new StructList.Factory<org.capnproto.test.Test.TestNewVersion.Builder, org.capnproto.test.Test.TestNewVersion.Reader>(org.capnproto.test.Test.TestNewVersion.factory));
      assertEquals(newVersion.size(), 1);
      assertEquals(newVersion.get(0).getOld1(), 91);
      assertEquals(newVersion.get(0).getOld2().toString(), "hello!!");

      ByteBuffer[] segments1 = message.getSegmentsForOutput();
      assertEquals(segments[0].limit(), 6 * 8);
      for (int ii = 8; ii < (5 * 8) - 1; ++ii) {
        // Check the the old list, including the tag, was zeroed.
        assertEquals(segments[0].get(ii), 0);
      }
    }

    @Test
    public void testListBuilderAsReader() {
      MessageBuilder message = new MessageBuilder();
      org.capnproto.test.Test.TestAllTypes.Builder allTypes = message.initRoot(org.capnproto.test.Test.TestAllTypes.factory);

      allTypes.initVoidList(10);
      assertEquals(allTypes.getVoidList().asReader().size(), 10);

      PrimitiveList.Boolean.Builder boolList = allTypes.initBoolList(7);
      boolList.set(3, true);
      PrimitiveList.Boolean.Reader boolListReader = boolList.asReader();
      assertEquals(boolListReader.size(), 7);
      assertEquals(boolListReader.get(0), false);
      assertEquals(boolListReader.get(1), false);
      assertEquals(boolListReader.get(2), false);
      assertEquals(boolListReader.get(3), true);
      assertEquals(boolListReader.get(4), false);

      PrimitiveList.Byte.Builder int8List = allTypes.initInt8List(9);
      int8List.set(4, (byte)100);
      int8List.set(8, (byte)11);
      PrimitiveList.Byte.Reader int8ListReader = int8List.asReader();
      assertEquals(int8ListReader.size(), 9);
      assertEquals(int8ListReader.get(0), 0);
      assertEquals(int8ListReader.get(4), 100);
      assertEquals(int8ListReader.get(8), 11);

      PrimitiveList.Short.Builder int16List = allTypes.initInt16List(2);
      int16List.set(0, (short)1);
      PrimitiveList.Short.Reader int16ListReader = int16List.asReader();
      assertEquals(int16ListReader.size(), 2);
      assertEquals(int16ListReader.get(0), 1);
      assertEquals(int16ListReader.get(1), 0);

      // TODO other primitive lists

      TextList.Builder textList = allTypes.initTextList(1);
      textList.set(0, new Text.Reader("abcdefg"));
      TextList.Reader textListReader = textList.asReader();
      assertEquals(textListReader.size(), 1);
      assertEquals(textListReader.get(0).toString(), "abcdefg");

      DataList.Builder dataList = allTypes.initDataList(1);
      dataList.set(0, new Data.Reader(new byte[]{1,2,3,4}));
      DataList.Reader dataListReader = dataList.asReader();
      assertEquals(dataListReader.size(), 1);
      assertTrue(java.util.Arrays.equals(dataListReader.get(0).toArray(), new byte[]{1,2,3,4}));

      StructList.Builder<org.capnproto.test.Test.TestAllTypes.Builder> structList = allTypes.initStructList(2);
      structList.get(0).setInt8Field((byte)5);
      structList.get(1).setInt8Field((byte)9);
      StructList.Reader<org.capnproto.test.Test.TestAllTypes.Reader> structListReader = structList.asReader(org.capnproto.test.Test.TestAllTypes.factory);
      assertEquals(structListReader.size(), 2);
      assertEquals(structListReader.get(0).getInt8Field(), 5);
      assertEquals(structListReader.get(1).getInt8Field(), 9);

      EnumList.Builder<org.capnproto.test.Test.TestEnum> enumList = allTypes.initEnumList(3);
      enumList.set(0, org.capnproto.test.Test.TestEnum.FOO);
      enumList.set(1, org.capnproto.test.Test.TestEnum.BAR);
      enumList.set(2, org.capnproto.test.Test.TestEnum.BAZ);
      EnumList.Reader<org.capnproto.test.Test.TestEnum> enumListReader = enumList.asReader();
      assertEquals(enumListReader.size(), 3);
      assertEquals(enumListReader.get(0), org.capnproto.test.Test.TestEnum.FOO);
      assertEquals(enumListReader.get(1), org.capnproto.test.Test.TestEnum.BAR);
      assertEquals(enumListReader.get(2), org.capnproto.test.Test.TestEnum.BAZ);
    }

    @Test
    public void testNestedListBuilderAsReader() {
      MessageBuilder builder = new MessageBuilder();
      org.capnproto.test.Test.TestLists.Builder root = builder.initRoot(org.capnproto.test.Test.TestLists.factory);

      ListList.Builder<StructList.Builder<org.capnproto.test.Test.TestAllTypes.Builder>> structListList = root.initStructListList(3);
      StructList.Builder<org.capnproto.test.Test.TestAllTypes.Builder> structList0 = structListList.init(0, 1);
      structList0.get(0).setInt16Field((short)1);
      // leave structList1 as default
      StructList.Builder<org.capnproto.test.Test.TestAllTypes.Builder> structList2 = structListList.init(2, 3);
      structList2.get(0).setInt16Field((short)22);
      structList2.get(1).setInt16Field((short)333);
      structList2.get(2).setInt16Field((short)4444);

      ListList.Reader<StructList.Reader<org.capnproto.test.Test.TestAllTypes.Reader>> structListListReader =
        structListList.asReader(StructList.newFactory(org.capnproto.test.Test.TestAllTypes.factory));
      assertEquals(structListListReader.size(), 3);
      StructList.Reader<org.capnproto.test.Test.TestAllTypes.Reader> structList0Reader = structListListReader.get(0);
      assertEquals(structList0Reader.size(), 1);
      assertEquals(structList0Reader.get(0).getInt16Field(), 1);
      assertEquals(structListListReader.get(1).size(), 0);
      StructList.Reader<org.capnproto.test.Test.TestAllTypes.Reader> structList2Reader = structListListReader.get(2);
      assertEquals(structList2Reader.size(), 3);
      assertEquals(structList2Reader.get(0).getInt16Field(), 22);
      assertEquals(structList2Reader.get(1).getInt16Field(), 333);
      assertEquals(structList2Reader.get(2).getInt16Field(), 4444);
    }

    @Test
    public void testGenerics() {
        MessageBuilder message = new MessageBuilder();

        org.capnproto.test.Test.TestGenerics.Builder<org.capnproto.test.Test.TestAllTypes.Builder, Text.Builder> root =
            (org.capnproto.test.Test.TestGenerics.Builder<org.capnproto.test.Test.TestAllTypes.Builder, Text.Builder>) message.initRoot(org.capnproto.test.Test.TestGenerics.newFactory(org.capnproto.test.Test.TestAllTypes.factory, Text.factory));
        TestUtil.initTestMessage(root.getFoo());
        root.getDub().setFoo(Text.factory, new Text.Reader("Hello"));

        PrimitiveList.Byte.Builder bar = root.getDub().initBar(1);
        bar.set(0, (byte) 11);
        org.capnproto.test.Test.TestAllTypes.Builder revBar = root.getRev().getBar();
        revBar.setInt8Field((byte) 111);
        PrimitiveList.Boolean.Builder boolList = revBar.initBoolList(2);
        boolList.set(0, false);
        boolList.set(1, true);

        TestUtil.checkTestMessage(root.getFoo());
        org.capnproto.test.Test.TestGenerics.Reader<org.capnproto.test.Test.TestAllTypes.Reader, Text.Reader> rootReader =
            root.asReader(org.capnproto.test.Test.TestGenerics.newFactory(org.capnproto.test.Test.TestAllTypes.factory, Text.factory));
        TestUtil.checkTestMessage(rootReader.getFoo());
        org.capnproto.test.Test.TestGenerics.Builder<Text.Builder, PrimitiveList.Byte.Builder> dubReader = root.getDub();
        assertEquals(dubReader.getFoo().toString(), "Hello");
        PrimitiveList.Byte.Builder barReader = dubReader.getBar();
        assertEquals(1, barReader.size());
        assertEquals(11, barReader.get(0));
    }

    @Test
    public void testUseGenerics() {
        MessageBuilder message = new MessageBuilder();
        org.capnproto.test.Test.TestUseGenerics.Builder root = message.initRoot(org.capnproto.test.Test.TestUseGenerics.factory);
        {
            MessageBuilder message2 = new MessageBuilder();
            org.capnproto.test.Test.TestGenerics.Factory<AnyPointer.Builder, AnyPointer.Reader, AnyPointer.Builder, AnyPointer.Reader> factory2 = org.capnproto.test.Test.TestGenerics.newFactory(AnyPointer.factory, AnyPointer.factory);
            org.capnproto.test.Test.TestGenerics.Builder<AnyPointer.Builder, AnyPointer.Builder> root2 = message2.initRoot(factory2);
            root2.initDub().setFoo(Text.factory, new Text.Reader("foobar"));

            root.setUnspecified(factory2, root2.asReader(factory2));
        }

        assertEquals("foobar", root.getUnspecified().getDub().getFoo().toString());
    }

    @Test
    public void testDefaults() {
        MessageBuilder message = new MessageBuilder();
        org.capnproto.test.Test.TestDefaults.Builder defaults = message.initRoot(org.capnproto.test.Test.TestDefaults.factory);
        TestUtil.checkDefaultMessage(defaults);
        TestUtil.checkDefaultMessage(defaults.asReader());
        TestUtil.setDefaultMessage(defaults);
        TestUtil.checkSettedDefaultMessage(defaults.asReader());
    }

    @Test
    public void testUnions() {
      MessageBuilder builder = new MessageBuilder();
      org.capnproto.test.Test.TestUnion.Builder root = builder.initRoot(org.capnproto.test.Test.TestUnion.factory);
      org.capnproto.test.Test.TestUnion.Union0.Builder u0 = root.initUnion0();
      u0.initU0f1sp(10);
      assertEquals(u0.which(), org.capnproto.test.Test.TestUnion.Union0.Which.U0F1SP);

      u0.initPrimitiveList(10);
      assertEquals(u0.which(), org.capnproto.test.Test.TestUnion.Union0.Which.PRIMITIVE_LIST);
    }

    @Test
    public void testGroups() {
        MessageBuilder builder = new MessageBuilder();
        org.capnproto.test.Test.TestGroups.Builder root = builder.initRoot(org.capnproto.test.Test.TestGroups.factory);
        {
            org.capnproto.test.Test.TestGroups.Groups.Foo.Builder foo = root.getGroups().initFoo();
            foo.setCorge(12345678);
            foo.setGrault(123456789012345L);
            foo.setGarply(new Text.Reader("foobar"));

            assertEquals(12345678, foo.getCorge());
            assertEquals(123456789012345L, foo.getGrault());
            assertEquals("foobar", foo.getGarply().toString());
        }
        {
            org.capnproto.test.Test.TestGroups.Groups.Bar.Builder bar = root.getGroups().initBar();
            bar.setCorge(23456789);
            bar.setGrault(new Text.Reader("barbaz"));
            bar.setGarply(234567890123456L);

            assertEquals(23456789, bar.getCorge());
            assertEquals("barbaz", bar.getGrault().toString());
            assertEquals(234567890123456L, bar.getGarply());
        }
        {
            org.capnproto.test.Test.TestGroups.Groups.Baz.Builder baz = root.getGroups().initBaz();
            baz.setCorge(34567890);
            baz.setGrault(new Text.Reader("bazqux"));
            baz.setGarply(new Text.Reader("quxquux"));

            assertEquals(34567890, baz.getCorge());
            assertEquals("bazqux", baz.getGrault().toString());
            assertEquals("quxquux", baz.getGarply().toString());
        }
    }

    @Test
    public void testNestedLists() {
        MessageBuilder builder = new MessageBuilder();
        org.capnproto.test.Test.TestLists.Builder root = builder.initRoot(org.capnproto.test.Test.TestLists.factory);
        {
            ListList.Builder<PrimitiveList.Int.Builder> intListList = root.initInt32ListList(2);
            PrimitiveList.Int.Builder intList0 = intListList.init(0, 4);
            intList0.set(0, 1);
            intList0.set(1, 2);
            intList0.set(2, 3);
            intList0.set(3, 4);
            PrimitiveList.Int.Builder intList1 = intListList.init(1, 1);
            intList1.set(0, 100);
        }
        {
            org.capnproto.test.Test.TestLists.Reader reader = root.asReader();
            ListList.Reader<PrimitiveList.Int.Reader> intListList = reader.getInt32ListList();
            assertEquals(2, intListList.size());
            PrimitiveList.Int.Reader intList0 = intListList.get(0);
            assertEquals(4, intList0.size());
            assertEquals(1, intList0.get(0));
            assertEquals(2, intList0.get(1));
            assertEquals(3, intList0.get(2));
            assertEquals(4, intList0.get(3));
            PrimitiveList.Int.Reader intList1 = intListList.get(1);
            assertEquals(1, intList1.size());
            assertEquals(100, intList1.get(0));
        }
    }

    @Test
    public void testConstants() {
        assertEquals(Void.VOID, org.capnproto.test.Test.TestConstants.VOID_CONST);
        assertEquals(true, org.capnproto.test.Test.TestConstants.BOOL_CONST);
        assertEquals(-123, org.capnproto.test.Test.TestConstants.INT8_CONST);
        assertEquals(-12345, org.capnproto.test.Test.TestConstants.INT16_CONST);
        assertEquals(-12345678, org.capnproto.test.Test.TestConstants.INT32_CONST);
        assertEquals(-123456789012345L, org.capnproto.test.Test.TestConstants.INT64_CONST);

        assertEquals(-22, org.capnproto.test.Test.TestConstants.UINT8_CONST);
        assertEquals(-19858, org.capnproto.test.Test.TestConstants.UINT16_CONST);
        assertEquals(-838178284, org.capnproto.test.Test.TestConstants.UINT32_CONST);
        assertEquals(-6101065172474983726L, org.capnproto.test.Test.TestConstants.UINT64_CONST);

        assertEquals(1234.5f, org.capnproto.test.Test.TestConstants.FLOAT32_CONST, TestUtil.DELTA);
        assertEquals(-123e45, org.capnproto.test.Test.TestConstants.FLOAT64_CONST, TestUtil.DELTA);

        assertEquals("foo", org.capnproto.test.Test.TestConstants.TEXT_CONST.toString());
        assertArrayEquals(TestUtil.data("bar"), org.capnproto.test.Test.TestConstants.DATA_CONST.toArray());

        assertEquals(org.capnproto.test.Test.TestEnum.CORGE, org.capnproto.test.Test.TestConstants.ENUM_CONST);
        {
            org.capnproto.test.Test.TestAllTypes.Reader subReader = org.capnproto.test.Test.TestConstants.STRUCT_CONST;
            assertEquals(subReader.getBoolField(), true);
            assertEquals(subReader.getInt8Field(), -12);
            assertEquals(subReader.getInt16Field(), 3456);
            assertEquals(subReader.getInt32Field(), -78901234);
            assertEquals(subReader.getInt64Field(), 56789012345678L);
            assertEquals(subReader.getUInt8Field(), 90);
            assertEquals(subReader.getUInt16Field(), 1234);
            assertEquals(subReader.getUInt32Field(), 56789012);
            assertEquals(subReader.getUInt64Field(), 345678901234567890L);
            assertEquals(subReader.getFloat32Field(), -1.25e-10f, TestUtil.DELTA);
            assertEquals(subReader.getFloat64Field(), 345, TestUtil.DELTA);
            assertEquals(subReader.getTextField().toString(), "baz");
        }

        assertEquals(6, org.capnproto.test.Test.TestConstants.VOID_LIST_CONST.size());

        {
            PrimitiveList.Boolean.Reader listReader = org.capnproto.test.Test.TestConstants.BOOL_LIST_CONST;
            assertEquals(4, listReader.size());
            assertEquals(true, listReader.get(0));
            assertEquals(false, listReader.get(1));
            assertEquals(false, listReader.get(2));
            assertEquals(true, listReader.get(3));
        }

        {
            TextList.Reader listReader = org.capnproto.test.Test.TestConstants.TEXT_LIST_CONST;
            assertEquals(3, listReader.size());
            assertEquals("plugh", listReader.get(0).toString());
            assertEquals("xyzzy", listReader.get(1).toString());
            assertEquals("thud", listReader.get(2).toString());
        }

        {
            StructList.Reader<org.capnproto.test.Test.TestAllTypes.Reader> listReader = org.capnproto.test.Test.TestConstants.STRUCT_LIST_CONST;
            assertEquals(3, listReader.size());
            assertEquals("structlist 1", listReader.get(0).getTextField().toString());
            assertEquals("structlist 2", listReader.get(1).getTextField().toString());
            assertEquals("structlist 3", listReader.get(2).getTextField().toString());
        }
    }

    @Test
    public void testGlobalConstants() {
        assertEquals(org.capnproto.test.Test.GLOBAL_INT, 12345);
    }

    @Test
    public void testEmptyStruct() {
        MessageBuilder builder = new MessageBuilder();
        org.capnproto.test.Test.TestAnyPointer.Builder root = builder.initRoot(org.capnproto.test.Test.TestAnyPointer.factory);

        assertFalse(root.hasAnyPointerField());

        AnyPointer.Builder any = root.getAnyPointerField();
        assertTrue(any.isNull());
        any.initAs(org.capnproto.test.Test.TestEmptyStruct.factory);
        assertFalse(any.isNull());
        assertTrue(root.hasAnyPointerField());

        {
            org.capnproto.test.Test.TestAnyPointer.Reader reader = root.asReader();
            assertTrue(reader.hasAnyPointerField());
            assertNotNull(reader.getAnyPointerField());
        }
    }

    @Test
    public void testTextBuilderIntUnderflow() {
        MessageBuilder builder = new MessageBuilder();
        org.capnproto.test.Test.TestAnyPointer.Builder root = builder.initRoot(org.capnproto.test.Test.TestAnyPointer.factory);
        root.getAnyPointerField().initAs(Data.factory, 0);
        assertThrows(DecodeException.class, () -> root.getAnyPointerField().getAs(Text.factory));
    }

    @Test
    public void testInlineCompositeListIntOverflow() throws DecodeException {

        byte[] bytes = new byte[]{0,0,0,0, 0,0,1,0, 1,0,0,0, 0x17,0,0,0, 0,0,0,-128, 16,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,0};

        ByteBuffer segment = ByteBuffer.wrap(bytes);
        segment.order(ByteOrder.LITTLE_ENDIAN);
        MessageReader message = new MessageReader(new ByteBuffer[]{segment}, ReaderOptions.DEFAULT_READER_OPTIONS);

        org.capnproto.test.Test.TestAnyPointer.Reader root = message.getRoot(org.capnproto.test.Test.TestAnyPointer.factory);
        assertThrows(DecodeException.class, () -> root.getAnyPointerField().getAs(StructList.newFactory(org.capnproto.test.Test.TestAllTypes.factory)));
    }

    @Test
    public void testInlineCompositeListIntOverflow2() throws DecodeException {

        byte[] bytes = new byte[]{0,0,0,0, 0,0,1,0, 1,0,0,0, 0x17,0,0,0, 0,0,0,-128, 16,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,0};

        ByteBuffer segment = ByteBuffer.wrap(bytes);
        segment.order(ByteOrder.LITTLE_ENDIAN);
        MessageReader message = new MessageReader(new ByteBuffer[]{segment}, ReaderOptions.DEFAULT_READER_OPTIONS);

        org.capnproto.test.Test.TestAnyPointer.Reader root = message.getRoot(org.capnproto.test.Test.TestAnyPointer.factory);

        MessageBuilder messageBuilder = new MessageBuilder();
        org.capnproto.test.Test.TestAnyPointer.Builder builderRoot = messageBuilder.initRoot(org.capnproto.test.Test.TestAnyPointer.factory);
        assertThrows(DecodeException.class, () -> builderRoot.getAnyPointerField().setAs(org.capnproto.test.Test.TestAnyPointer.factory, root));
    }

    @Test
    public void testVoidListAmplification() throws DecodeException {
        MessageBuilder builder = new MessageBuilder();
        builder.initRoot(org.capnproto.test.Test.TestAnyPointer.factory).getAnyPointerField().initAs(PrimitiveList.Void.factory, 1 << 28);

        ByteBuffer[] segments = builder.getSegmentsForOutput();
        assertEquals(1, segments.length);

        MessageReader reader = new MessageReader(segments, ReaderOptions.DEFAULT_READER_OPTIONS);
        org.capnproto.test.Test.TestAnyPointer.Reader root = reader.getRoot(org.capnproto.test.Test.TestAnyPointer.factory);
        assertThrows(DecodeException.class, () -> root.getAnyPointerField().getAs(StructList.newFactory(org.capnproto.test.Test.TestAllTypes.factory)));
    }

    @Test
    public void testEmptyStructListAmplification() {
        MessageBuilder builder = new MessageBuilder();
        builder.initRoot(org.capnproto.test.Test.TestAnyPointer.factory).getAnyPointerField()
                .initAs(StructList.newFactory(org.capnproto.test.Test.TestEmptyStruct.factory), (1 << 29) - 1);

        ByteBuffer[] segments = builder.getSegmentsForOutput();
        assertEquals(1, segments.length);

        MessageReader reader = new MessageReader(segments, ReaderOptions.DEFAULT_READER_OPTIONS);
        org.capnproto.test.Test.TestAnyPointer.Reader root = reader.getRoot(org.capnproto.test.Test.TestAnyPointer.factory);
        assertThrows(DecodeException.class, () -> root.getAnyPointerField().getAs(StructList.newFactory(org.capnproto.test.Test.TestAllTypes.factory)));
    }

    // Test that we throw an exception on out-of-bounds list pointers.
    // Before v0.1.11, we were vulnerable to a cpu amplification attack:
    // reading an out-of-bounds pointer to list a huge number of elements of size BIT,
    // when read as a struct list, would return without error.
    @Test
    public void testListPointerOutOfBounds() throws DecodeException {
        byte[] bytes = new byte[]
                    {0,0,0,0, 0,0,1,0, // struct, one pointer
                     1, 0x2f, 0, 0, 1, 0, -127, -128}; // list, points out of bounds.
        ByteBuffer segment = ByteBuffer.wrap(bytes);
        segment.order(ByteOrder.LITTLE_ENDIAN);
        MessageReader message = new MessageReader(new ByteBuffer[]{segment},
                                                  ReaderOptions.DEFAULT_READER_OPTIONS);

        org.capnproto.test.Test.TestAnyPointer.Reader root = message.getRoot(org.capnproto.test.Test.TestAnyPointer.factory);
        assertThrows(DecodeException.class, () -> root.getAnyPointerField().getAs(StructList.newFactory(org.capnproto.test.Test.TestAllTypes.factory)));
    }

    @Test
    public void testLongUint8List() {
      MessageBuilder message = new MessageBuilder();
      org.capnproto.test.Test.TestAllTypes.Builder allTypes = message.initRoot(org.capnproto.test.Test.TestAllTypes.factory);
      int length = (1 << 28) + 1;
      PrimitiveList.Byte.Builder list = allTypes.initUInt8List(length);
      assertEquals(list.size(), length);
      list.set(length - 1, (byte)3);
      assertEquals(list.get(length - 1), 3);
      assertEquals(allTypes.asReader().getUInt8List().get(length - 1), 3);
    }

    @Test
    public void testLongUint16List() {
      MessageBuilder message = new MessageBuilder();
      org.capnproto.test.Test.TestAllTypes.Builder allTypes = message.initRoot(org.capnproto.test.Test.TestAllTypes.factory);
      int length = (1 << 27) + 1;
      PrimitiveList.Short.Builder list = allTypes.initUInt16List(length);
      assertEquals(list.size(), length);
      list.set(length - 1, (short)3);
      assertEquals(list.get(length - 1), 3);
      assertEquals(allTypes.asReader().getUInt16List().get(length - 1), 3);
    }

    @Test
    public void testLongUint32List() {
      MessageBuilder message = new MessageBuilder();
      org.capnproto.test.Test.TestAllTypes.Builder allTypes = message.initRoot(org.capnproto.test.Test.TestAllTypes.factory);
      int length = (1 << 26) + 1;
      PrimitiveList.Int.Builder list = allTypes.initUInt32List(length);
      assertEquals(list.size(), length);
      list.set(length - 1, 3);
      assertEquals(list.get(length - 1), 3);
      assertEquals(allTypes.asReader().getUInt32List().get(length - 1), 3);
    }

    @Test
    public void testLongUint64List() {
      MessageBuilder message = new MessageBuilder();
      org.capnproto.test.Test.TestAllTypes.Builder allTypes = message.initRoot(org.capnproto.test.Test.TestAllTypes.factory);
      int length = (1 << 25) + 1;
      PrimitiveList.Long.Builder list = allTypes.initUInt64List(length);
      assertEquals(list.size(), length);
      list.set(length - 1, 3);
      assertEquals(list.get(length - 1), 3);
      assertEquals(allTypes.asReader().getUInt64List().get(length - 1), 3);
    }

    @Test
    public void testLongFloat32List() {
      MessageBuilder message = new MessageBuilder();
      org.capnproto.test.Test.TestAllTypes.Builder allTypes = message.initRoot(org.capnproto.test.Test.TestAllTypes.factory);
      int length = (1 << 26) + 1;
      PrimitiveList.Float.Builder list = allTypes.initFloat32List(length);
      assertEquals(list.size(), length);
      list.set(length - 1, 3.14f);
      assertEquals(list.get(length - 1), 3.14f, 0.0f);
      assertEquals(allTypes.asReader().getFloat32List().get(length - 1), 3.14f, 0.0f);
    }

    @Test
    public void testLongFloat64List() {
      MessageBuilder message = new MessageBuilder();
      org.capnproto.test.Test.TestAllTypes.Builder allTypes = message.initRoot(org.capnproto.test.Test.TestAllTypes.factory);
      int length = (1 << 25) + 1;
      PrimitiveList.Double.Builder list = allTypes.initFloat64List(length);
      assertEquals(list.size(), length);
      list.set(length - 1, 3.14);
      assertEquals(list.get(length - 1), 3.14, 0.0);
      assertEquals(allTypes.asReader().getFloat64List().get(length - 1), 3.14, 0.0);
    }

    @Test
    public void testLongStructList() {
      MessageBuilder message = new MessageBuilder();
      org.capnproto.test.Test.TestAllTypes.Builder allTypes = message.initRoot(org.capnproto.test.Test.TestAllTypes.factory);
      int length = (1 << 21) + 1;
      StructList.Builder<org.capnproto.test.Test.TestAllTypes.Builder> list = allTypes.initStructList(length);
      assertEquals(list.size(), length);
      list.get(length - 1).setUInt8Field((byte)3);
      assertEquals(list.get(length - 1).getUInt8Field(), 3);
      assertEquals(allTypes.asReader().getStructList().get(length - 1).getUInt8Field(), 3);
    }

    @Test
    public void testLongTextList() {
      MessageBuilder message = new MessageBuilder();
      org.capnproto.test.Test.TestAllTypes.Builder allTypes = message.initRoot(org.capnproto.test.Test.TestAllTypes.factory);
      int length = (1 << 25) + 1;
      TextList.Builder list = allTypes.initTextList(length);
      assertEquals(list.size(), length);
      list.set(length - 1, new Text.Reader("foo"));
      assertEquals(list.get(length - 1).toString(), "foo");
      assertEquals(allTypes.asReader().getTextList().get(length - 1).toString(), "foo");
    }

    @Test
    public void testLongListList() {
      MessageBuilder message = new MessageBuilder();
      org.capnproto.test.Test.TestLists.Builder root = message.initRoot(org.capnproto.test.Test.TestLists.factory);
      int length = (1 << 25) + 1;
      ListList.Builder<StructList.Builder<org.capnproto.test.Test.TestAllTypes.Builder>> list = root.initStructListList(length);
      assertEquals(list.size(), length);
      list.init(length - 1, 3);
      assertEquals(list.get(length - 1).size(), 3);
      assertEquals(root.asReader().getStructListList().get(length - 1).size(), 3);
    }

    @Test
    public void testStructSetters() {
      MessageBuilder builder = new MessageBuilder();
      org.capnproto.test.Test.TestAllTypes.Builder root = builder.initRoot(org.capnproto.test.Test.TestAllTypes.factory);
      TestUtil.initTestMessage(root);

      {
        MessageBuilder builder2 = new MessageBuilder();
        builder2.setRoot(org.capnproto.test.Test.TestAllTypes.factory, root.asReader());
        TestUtil.checkTestMessage(builder2.getRoot(org.capnproto.test.Test.TestAllTypes.factory));
      }

      {
        MessageBuilder builder2 = new MessageBuilder();
        org.capnproto.test.Test.TestAllTypes.Builder root2 = builder2.getRoot(org.capnproto.test.Test.TestAllTypes.factory);
        root2.setStructField(root.asReader());
        TestUtil.checkTestMessage(root2.getStructField());
      }

      {
        MessageBuilder builder2 = new MessageBuilder();
        org.capnproto.test.Test.TestAnyPointer.Builder root2 = builder2.getRoot(org.capnproto.test.Test.TestAnyPointer.factory);
        root2.getAnyPointerField().setAs(org.capnproto.test.Test.TestAllTypes.factory, root.asReader());
        TestUtil.checkTestMessage(root2.getAnyPointerField().getAs(org.capnproto.test.Test.TestAllTypes.factory));
      }
    }

    @Test
    public void testSerializedSize() {
      MessageBuilder builder = new MessageBuilder();
      org.capnproto.test.Test.TestAnyPointer.Builder root = builder.initRoot(org.capnproto.test.Test.TestAnyPointer.factory);
      root.getAnyPointerField().setAs(Text.factory, new Text.Reader("12345"));

      // one word for segment table, one for the root pointer,
      // one for the body of the TestAnyPointer struct,
      // and one for the body of the Text.
      assertEquals(Serialize.computeSerializedSizeInWords(builder), 4);
    }

    @Test
    public void testImport() {
      MessageBuilder builder = new MessageBuilder();
      org.capnproto.testimport.TestImport.Foo.Builder root = builder.initRoot(org.capnproto.testimport.TestImport.Foo.factory);
      org.capnproto.test.Test.TestAllTypes.Builder field = root.initImportedStruct();
      TestUtil.initTestMessage(field);
      TestUtil.checkTestMessage(field);
      TestUtil.checkTestMessage(field.asReader());
    }

    @Test
    public void testGenericMap() {
      MessageBuilder builder = new MessageBuilder();
      org.capnproto.test.Test.GenericMap.Factory<Text.Builder, Text.Reader, org.capnproto.test.Test.TestAllTypes.Builder, org.capnproto.test.Test.TestAllTypes.Reader> mapFactory
        = org.capnproto.test.Test.GenericMap.newFactory(Text.factory, org.capnproto.test.Test.TestAllTypes.factory);

      StructList.Factory<org.capnproto.test.Test.GenericMap.Entry.Builder<Text.Builder, org.capnproto.test.Test.TestAllTypes.Builder>,
                         org.capnproto.test.Test.GenericMap.Entry.Reader<Text.Reader, org.capnproto.test.Test.TestAllTypes.Reader>> entryFactory
        = StructList.newFactory(org.capnproto.test.Test.GenericMap.Entry.newFactory(Text.factory, org.capnproto.test.Test.TestAllTypes.factory));

      org.capnproto.test.Test.GenericMap.Builder<Text.Builder, org.capnproto.test.Test.TestAllTypes.Builder> root = builder.initRoot(mapFactory);

      {
        StructList.Builder<org.capnproto.test.Test.GenericMap.Entry.Builder<Text.Builder, org.capnproto.test.Test.TestAllTypes.Builder>> entries = root.initEntries(entryFactory, 3);

        org.capnproto.test.Test.GenericMap.Entry.Builder<Text.Builder, org.capnproto.test.Test.TestAllTypes.Builder> entry0 = entries.get(0);
        entry0.setKey(Text.factory, new Text.Reader("foo"));
        org.capnproto.test.Test.TestAllTypes.Builder value0 = entry0.initValue();
        value0.setInt64Field(101);

        org.capnproto.test.Test.GenericMap.Entry.Builder<Text.Builder, org.capnproto.test.Test.TestAllTypes.Builder> entry1 = entries.get(1);
        entry1.setKey(Text.factory, new Text.Reader("bar"));
        org.capnproto.test.Test.TestAllTypes.Builder value1 = entry1.initValue();
        value1.setInt64Field(202);

        org.capnproto.test.Test.GenericMap.Entry.Builder<Text.Builder, org.capnproto.test.Test.TestAllTypes.Builder> entry2 = entries.get(2);
        entry2.setKey(Text.factory, new Text.Reader("baz"));
        org.capnproto.test.Test.TestAllTypes.Builder value2 = entry2.initValue();
        value2.setInt64Field(303);
      }

      {
         StructList.Reader<org.capnproto.test.Test.GenericMap.Entry.Reader<Text.Reader, org.capnproto.test.Test.TestAllTypes.Reader>> entries =
           root.asReader(mapFactory).getEntries(entryFactory);

         org.capnproto.test.Test.GenericMap.Entry.Reader<Text.Reader, org.capnproto.test.Test.TestAllTypes.Reader> entry0 = entries.get(0);
         assertEquals(entry0.getKey().toString(), "foo");
         assertEquals(entry0.getValue().getInt64Field(), 101);

         org.capnproto.test.Test.GenericMap.Entry.Reader<Text.Reader, org.capnproto.test.Test.TestAllTypes.Reader> entry1 = entries.get(1);
         assertEquals(entry1.getKey().toString(), "bar");
         assertEquals(entry1.getValue().getInt64Field(), 202);

         org.capnproto.test.Test.GenericMap.Entry.Reader<Text.Reader, org.capnproto.test.Test.TestAllTypes.Reader> entry2 = entries.get(2);
         assertEquals(entry2.getKey().toString(), "baz");
         assertEquals(entry2.getValue().getInt64Field(), 303);
      }
    }

    @Test
    public void testSetWithCaveats() {
      MessageBuilder builder = new MessageBuilder();
      org.capnproto.test.Test.TestAllTypes.Builder root = builder.initRoot(org.capnproto.test.Test.TestAllTypes.factory);
      StructList.Builder<org.capnproto.test.Test.TestAllTypes.Builder> list = root.initStructList(2);

      {
        MessageBuilder message1 = new MessageBuilder();
        org.capnproto.test.Test.TestAllTypes.Builder root1 = message1.initRoot(org.capnproto.test.Test.TestAllTypes.factory);
        root1.setInt8Field((byte)11);
        list.setWithCaveats(org.capnproto.test.Test.TestAllTypes.factory, 0, root1.asReader());
      }

      {
        MessageBuilder message2 = new MessageBuilder();
        org.capnproto.test.Test.TestAllTypes.Builder root2 = message2.initRoot(org.capnproto.test.Test.TestAllTypes.factory);
        TestUtil.initTestMessage(root2);
        list.setWithCaveats(org.capnproto.test.Test.TestAllTypes.factory, 1, root2.asReader());
      }

      StructList.Reader<org.capnproto.test.Test.TestAllTypes.Reader> listReader = list.asReader(org.capnproto.test.Test.TestAllTypes.factory);
      assertEquals(listReader.get(0).getInt8Field(), 11);
      TestUtil.checkTestMessage(listReader.get(1));
  }

  @Test
  public void testCopyAnyPointer() {
      MessageBuilder message1 = new MessageBuilder();
      org.capnproto.test.Test.TestAllTypes.Builder root1 = message1.initRoot(org.capnproto.test.Test.TestAllTypes.factory);
      TestUtil.initTestMessage(root1);

      MessageBuilder message2 = new MessageBuilder();
      AnyPointer.Builder root2 = message2.initRoot(AnyPointer.factory);
      root2.setAs(AnyPointer.factory, message1.getRoot(AnyPointer.factory).asReader());

      TestUtil.checkTestMessage(root2.getAs(org.capnproto.test.Test.TestAllTypes.factory));
  }

  @Test
  public void testZeroPointerUnderflow() throws DecodeException {
      byte[] bytes = new byte[8 + 8 * 65535];
      bytes[4] = -1;
      bytes[5] = -1; // struct pointer with 65535 words of data section.
      for (int ii = 0; ii < 8 * 65535; ++ii) {
          bytes[8 + ii] = 101; // populate the data section with sentinel data
      }
      ByteBuffer segment = ByteBuffer.wrap(bytes);
      segment.order(ByteOrder.LITTLE_ENDIAN);
      MessageReader message1 = new MessageReader(new ByteBuffer[]{segment},
                                                ReaderOptions.DEFAULT_READER_OPTIONS);
      org.capnproto.test.Test.TestAnyPointer.Reader message1RootReader = message1.getRoot(org.capnproto.test.Test.TestAnyPointer.factory);

      MessageBuilder message2Builder =
          new MessageBuilder(3 * 65535); // ample space to avoid far pointers
      org.capnproto.test.Test.TestAnyPointer.Builder message2RootBuilder =
          message2Builder.getRoot(org.capnproto.test.Test.TestAnyPointer.factory);

      // Copy the struct that has the sentinel data.
      message2RootBuilder.getAnyPointerField().setAs(org.capnproto.test.Test.TestAnyPointer.factory, message1RootReader);

      // Now clear the struct pointer.
      message2RootBuilder.getAnyPointerField().clear();

      java.nio.ByteBuffer[] outputSegments = message2Builder.getSegmentsForOutput();
      assertEquals(1, outputSegments.length);
      assertEquals(0L, outputSegments[0].getLong(8)); // null because cleared

      assertEquals(16 + 8 * 65535, outputSegments[0].limit());
      for (int ii = 0; ii < 65535; ++ii) {
          // All of the data should have been cleared.
          assertEquals(0L, outputSegments[0].getLong((2 + ii) * 8));
      }
    }

    // This test fails on https://github.com/capnproto/capnproto-java/pull/143,
    // illustrating why https://github.com/capnproto/capnproto-java/commit/28ab5ced
    // is needed.
    @Test
    public void setDataPointer() {
        MessageBuilder message1 = new MessageBuilder();
        org.capnproto.test.Test.TestAllTypes.Builder allTypes1 = message1.initRoot(org.capnproto.test.Test.TestAllTypes.factory);
        TestUtil.initTestMessage(allTypes1);
        MessageBuilder message2 = new MessageBuilder();
        org.capnproto.test.Test.TestAllTypes.Builder allTypes2 = message2.initRoot(org.capnproto.test.Test.TestAllTypes.factory);
        TestUtil.initTestMessage(allTypes2);

        allTypes1.setDataField(allTypes2.asReader().getDataField());
        TestUtil.checkTestMessage(allTypes1);
        TestUtil.checkTestMessage(allTypes1.asReader());
    }
}
