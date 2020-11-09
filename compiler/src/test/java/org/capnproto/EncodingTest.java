package org.capnproto.test;

import org.capnproto.*;
import org.capnproto.Void;
import org.junit.Assert;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class EncodingTest {
    @org.junit.Test
    public void testAllTypes() {
        MessageBuilder message = new MessageBuilder();
        Test.TestAllTypes.Builder allTypes = message.initRoot(Test.TestAllTypes.factory);
        TestUtil.initTestMessage(allTypes);
        TestUtil.checkTestMessage(allTypes);
        TestUtil.checkTestMessage(allTypes.asReader());
    }

    @org.junit.Test
    public void testAllTypesMultiSegment() {
        MessageBuilder message = new MessageBuilder(5, BuilderArena.AllocationStrategy.FIXED_SIZE);
        Test.TestAllTypes.Builder allTypes = message.initRoot(Test.TestAllTypes.factory);
        TestUtil.initTestMessage(allTypes);

        TestUtil.checkTestMessage(allTypes);
        TestUtil.checkTestMessage(allTypes.asReader());
    }

    @org.junit.Test
    public void testSetters() {
        MessageBuilder message = new MessageBuilder();
        Test.TestAllTypes.Builder allTypes = message.initRoot(Test.TestAllTypes.factory);
        TestUtil.initTestMessage(allTypes);

        MessageBuilder message2 = new MessageBuilder();
        Test.TestAllTypes.Builder allTypes2 = message2.initRoot(Test.TestAllTypes.factory);

        allTypes2.setStructField(allTypes.asReader());
        TestUtil.checkTestMessage(allTypes2.getStructField());

        Test.TestAllTypes.Reader reader = allTypes2.asReader().getStructField();
        TestUtil.checkTestMessage(reader);
    }

    @org.junit.Test
    public void testZeroing() {
        MessageBuilder message = new MessageBuilder();
        Test.TestAllTypes.Builder allTypes = message.initRoot(Test.TestAllTypes.factory);

        StructList.Builder<Test.TestAllTypes.Builder> structList = allTypes.initStructList(3);
        TestUtil.initTestMessage(structList.get(0));

        Test.TestAllTypes.Builder structField = allTypes.initStructField();
        TestUtil.initTestMessage(structField);

        TestUtil.initTestMessage(structList.get(1));
        TestUtil.initTestMessage(structList.get(2));
        TestUtil.checkTestMessage(structList.get(0));
        allTypes.initStructList(0);

        TestUtil.checkTestMessage(allTypes.getStructField());
        Test.TestAllTypes.Reader allTypesReader = allTypes.asReader();
        TestUtil.checkTestMessage(allTypesReader.getStructField());

        AnyPointer.Builder any = message.initRoot(AnyPointer.factory);
        ByteBuffer[] segments = message.getSegmentsForOutput();
        for (ByteBuffer segment : segments) {
            for (int j = 0; j < segment.limit(); j++) {
                Assert.assertEquals(segment.get(j), 0);
            }
        }
    }

    @org.junit.Test
    public void testDoubleFarPointers() throws IOException {
        byte[] bytes = new byte[]{2, 0, 0, 0, 1, 0, 0, 0, 2, 0, 0, 0, 1, 0, 0, 0,
                6, 0, 0, 0, 1, 0, 0, 0, 2, 0, 0, 0, 2, 0, 0, 0,
                0, 0, 0, 0, 1, 0, 0, 0, 1, 7, -1, 127, 0, 0, 0, 0};

        ArrayInputStream input = new ArrayInputStream(java.nio.ByteBuffer.wrap(bytes));
        MessageReader message = org.capnproto.Serialize.read(input);
        Test.TestAllTypes.Reader root = message.getRoot(Test.TestAllTypes.factory);

        Assert.assertTrue(root.getBoolField());
        Assert.assertEquals(root.getInt8Field(), 7);
        Assert.assertEquals(root.getInt16Field(), 32767);
    }

    @org.junit.Test
    public void testUpgradeStruct() {
      MessageBuilder builder = new MessageBuilder();
      Test.TestAnyPointer.Builder root = builder.initRoot(Test.TestAnyPointer.factory);

      {
        Test.TestOldVersion.Builder oldVersion = root.getAnyPointerField().initAs(Test.TestOldVersion.factory);
        oldVersion.setOld1(123);
        oldVersion.setOld2("foo");
        Test.TestOldVersion.Builder sub = oldVersion.initOld3();
        sub.setOld1(456);
        sub.setOld2("bar");
      }

      {
        Test.TestNewVersion.Reader newVersion = root.getAnyPointerField().asReader().getAs(Test.TestNewVersion.factory);
        Assert.assertEquals(newVersion.getOld1(), 123);
        Assert.assertEquals(newVersion.getOld2().toString(), "foo");
        Assert.assertEquals(newVersion.getNew2().toString(),  "baz");
        Assert.assertEquals(newVersion.hasNew2(), false);
        Assert.assertEquals(newVersion.hasNew3(), false);
      }
    }

    @org.junit.Test
    public void testUpgradeStructInBuilder() {
        MessageBuilder builder = new MessageBuilder();
        Test.TestAnyPointer.Builder root = builder.initRoot(Test.TestAnyPointer.factory);

        {
            Test.TestOldVersion.Builder oldVersion = root.getAnyPointerField().initAs(Test.TestOldVersion.factory);
            oldVersion.setOld1(123);
            oldVersion.setOld2("foo");
            Test.TestOldVersion.Builder sub = oldVersion.initOld3();
            sub.setOld1(456);
            sub.setOld2("bar");
        }
        {
            Test.TestNewVersion.Builder newVersion = root.getAnyPointerField().getAs(Test.TestNewVersion.factory);
            Assert.assertEquals(newVersion.getOld1(), 123);
            Assert.assertEquals(newVersion.getOld2().toString(), "foo");
            Assert.assertEquals(newVersion.getNew1(), 987);
            Assert.assertEquals(newVersion.getNew2().toString(), "baz");
            Test.TestNewVersion.Builder sub = newVersion.getOld3();
            Assert.assertEquals(sub.getOld1(), 456);
            Assert.assertEquals(sub.getOld2().toString(), "bar");

            newVersion.setOld1(234);
            newVersion.setOld2("qux");
            newVersion.setNew1(654);
            newVersion.setNew2("quux");
        }
        {
            Test.TestOldVersion.Builder oldVersion = root.getAnyPointerField().getAs(Test.TestOldVersion.factory);
            Assert.assertEquals(oldVersion.getOld1(), 234);
            Assert.assertEquals(oldVersion.getOld2().toString(), "qux");
        }
    }

    @org.junit.Test
    public void testStructListUpgrade() {
        MessageBuilder message = new MessageBuilder();
        Test.TestAnyPointer.Builder root = message.initRoot(Test.TestAnyPointer.factory);
        AnyPointer.Builder any = root.getAnyPointerField();

        {
            PrimitiveList.Long.Builder longs = any.initAs(PrimitiveList.Long.factory, 3);
            longs.set(0, 123);
            longs.set(1, 456);
            longs.set(2, 789);
        }
        {
            StructList.Reader<Test.TestOldVersion.Reader> olds = any.asReader().getAs(Test.TestOldVersion.listFactory);
            Assert.assertEquals(olds.get(0).getOld1(), 123);
            Assert.assertEquals(olds.get(1).getOld1(), 456);
            Assert.assertEquals(olds.get(2).getOld1(), 789);
        }
        {
            StructList.Builder<Test.TestOldVersion.Builder> olds = any.getAs(Test.TestOldVersion.listFactory);
            Assert.assertEquals(olds.size(), 3);
            Assert.assertEquals(olds.get(0).getOld1(), 123);
            Assert.assertEquals(olds.get(1).getOld1(), 456);
            Assert.assertEquals(olds.get(2).getOld1(), 789);

            olds.get(0).setOld2("zero");
            olds.get(1).setOld2("one");
            olds.get(2).setOld2("two");
        }
        {
            StructList.Builder<Test.TestNewVersion.Builder> news = any.getAs(Test.TestNewVersion.listFactory);
            Assert.assertEquals(news.size(), 3);
            Assert.assertEquals(news.get(0).getOld1(), 123);
            Assert.assertEquals(news.get(0).getOld2().toString(), "zero");

            Assert.assertEquals(news.get(1).getOld1(), 456);
            Assert.assertEquals(news.get(1).getOld2().toString(), "one");

            Assert.assertEquals(news.get(2).getOld1(), 789);
            Assert.assertEquals(news.get(2).getOld2().toString(), "two");
        }
    }

    @org.junit.Test
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

      StructList.Reader<Test.TestOldVersion.Reader> oldVersion = messageReader.getRoot(StructList.newFactory(Test.TestOldVersion.factory));

      Assert.assertEquals(oldVersion.size(), 1);
      Assert.assertEquals(oldVersion.get(0).getOld1(), 91);
      Assert.assertEquals(oldVersion.get(0).getOld2().toString(), "hello!!");

      // Make the first segment exactly large enough to fit the original message.
      // This leaves no room for a far pointer landing pad in the first segment.
      MessageBuilder message = new MessageBuilder(6);
      message.setRoot(StructList.newFactory(Test.TestOldVersion.factory), oldVersion);

      ByteBuffer[] segments = message.getSegmentsForOutput();
      Assert.assertEquals(segments.length, 1);
      Assert.assertEquals(segments[0].limit(), 6 * 8);

      StructList.Builder<Test.TestNewVersion.Builder> newVersion =
        message.getRoot(new StructList.Factory<Test.TestNewVersion.Builder, Test.TestNewVersion.Reader>(Test.TestNewVersion.factory));
      Assert.assertEquals(newVersion.size(), 1);
      Assert.assertEquals(newVersion.get(0).getOld1(), 91);
      Assert.assertEquals(newVersion.get(0).getOld2().toString(), "hello!!");

      ByteBuffer[] segments1 = message.getSegmentsForOutput();
      Assert.assertEquals(segments[0].limit(), 6 * 8);
      for (int ii = 8; ii < (5 * 8) - 1; ++ii) {
        // Check the the old list, including the tag, was zeroed.
        Assert.assertEquals(segments[0].get(ii), 0);
      }
    }

    @org.junit.Test
    public void testListBuilderAsReader() {
      MessageBuilder message = new MessageBuilder();
      Test.TestAllTypes.Builder allTypes = message.initRoot(Test.TestAllTypes.factory);

      allTypes.initVoidList(10);
      Assert.assertEquals(allTypes.getVoidList().asReader().size(), 10);

      PrimitiveList.Boolean.Builder boolList = allTypes.initBoolList(7);
      boolList.set(3, true);
      PrimitiveList.Boolean.Reader boolListReader = boolList.asReader();
      Assert.assertEquals(boolListReader.size(), 7);
      Assert.assertEquals(boolListReader.get(0), false);
      Assert.assertEquals(boolListReader.get(1), false);
      Assert.assertEquals(boolListReader.get(2), false);
      Assert.assertEquals(boolListReader.get(3), true);
      Assert.assertEquals(boolListReader.get(4), false);

      PrimitiveList.Byte.Builder int8List = allTypes.initInt8List(9);
      int8List.set(4, (byte)100);
      int8List.set(8, (byte)11);
      PrimitiveList.Byte.Reader int8ListReader = int8List.asReader();
      Assert.assertEquals(int8ListReader.size(), 9);
      Assert.assertEquals(int8ListReader.get(0), 0);
      Assert.assertEquals(int8ListReader.get(4), 100);
      Assert.assertEquals(int8ListReader.get(8), 11);

      PrimitiveList.Short.Builder int16List = allTypes.initInt16List(2);
      int16List.set(0, (short)1);
      PrimitiveList.Short.Reader int16ListReader = int16List.asReader();
      Assert.assertEquals(int16ListReader.size(), 2);
      Assert.assertEquals(int16ListReader.get(0), 1);
      Assert.assertEquals(int16ListReader.get(1), 0);

      // TODO other primitive lists

      TextList.Builder textList = allTypes.initTextList(1);
      textList.set(0, new Text.Reader("abcdefg"));
      TextList.Reader textListReader = textList.asReader();
      Assert.assertEquals(textListReader.size(), 1);
      Assert.assertEquals(textListReader.get(0).toString(), "abcdefg");

      DataList.Builder dataList = allTypes.initDataList(1);
      dataList.set(0, new Data.Reader(new byte[]{1,2,3,4}));
      DataList.Reader dataListReader = dataList.asReader();
      Assert.assertEquals(dataListReader.size(), 1);
      Assert.assertTrue(java.util.Arrays.equals(dataListReader.get(0).toArray(), new byte[]{1,2,3,4}));

      StructList.Builder<Test.TestAllTypes.Builder> structList = allTypes.initStructList(2);
      structList.get(0).setInt8Field((byte)5);
      structList.get(1).setInt8Field((byte)9);
      StructList.Reader<Test.TestAllTypes.Reader> structListReader = structList.asReader(Test.TestAllTypes.factory);
      Assert.assertEquals(structListReader.size(), 2);
      Assert.assertEquals(structListReader.get(0).getInt8Field(), 5);
      Assert.assertEquals(structListReader.get(1).getInt8Field(), 9);

      EnumList.Builder<Test.TestEnum> enumList = allTypes.initEnumList(3);
      enumList.set(0, Test.TestEnum.FOO);
      enumList.set(1, Test.TestEnum.BAR);
      enumList.set(2, Test.TestEnum.BAZ);
      EnumList.Reader<Test.TestEnum> enumListReader = enumList.asReader();
      Assert.assertEquals(enumListReader.size(), 3);
      Assert.assertEquals(enumListReader.get(0), Test.TestEnum.FOO);
      Assert.assertEquals(enumListReader.get(1), Test.TestEnum.BAR);
      Assert.assertEquals(enumListReader.get(2), Test.TestEnum.BAZ);
    }

    @org.junit.Test
    public void testNestedListBuilderAsReader() {
      MessageBuilder builder = new MessageBuilder();
      Test.TestLists.Builder root = builder.initRoot(Test.TestLists.factory);

      ListList.Builder<StructList.Builder<Test.TestAllTypes.Builder>> structListList = root.initStructListList(3);
      StructList.Builder<Test.TestAllTypes.Builder> structList0 = structListList.init(0, 1);
      structList0.get(0).setInt16Field((short)1);
      // leave structList1 as default
      StructList.Builder<Test.TestAllTypes.Builder> structList2 = structListList.init(2, 3);
      structList2.get(0).setInt16Field((short)22);
      structList2.get(1).setInt16Field((short)333);
      structList2.get(2).setInt16Field((short)4444);

      ListList.Reader<StructList.Reader<Test.TestAllTypes.Reader>> structListListReader =
        structListList.asReader(StructList.newFactory(Test.TestAllTypes.factory));
      Assert.assertEquals(structListListReader.size(), 3);
      StructList.Reader<Test.TestAllTypes.Reader> structList0Reader = structListListReader.get(0);
      Assert.assertEquals(structList0Reader.size(), 1);
      Assert.assertEquals(structList0Reader.get(0).getInt16Field(), 1);
      Assert.assertEquals(structListListReader.get(1).size(), 0);
      StructList.Reader<Test.TestAllTypes.Reader> structList2Reader = structListListReader.get(2);
      Assert.assertEquals(structList2Reader.size(), 3);
      Assert.assertEquals(structList2Reader.get(0).getInt16Field(), 22);
      Assert.assertEquals(structList2Reader.get(1).getInt16Field(), 333);
      Assert.assertEquals(structList2Reader.get(2).getInt16Field(), 4444);
    }

    @org.junit.Test
    public void testGenerics() {
        MessageBuilder message = new MessageBuilder();

        Test.TestGenerics.Builder<Test.TestAllTypes.Builder, Text.Builder> root =
            (Test.TestGenerics.Builder<Test.TestAllTypes.Builder, Text.Builder>) message.initRoot(Test.TestGenerics.newFactory(Test.TestAllTypes.factory, Text.factory));
        TestUtil.initTestMessage(root.getFoo());
        root.getDub().setFoo(Text.factory, new Text.Reader("Hello"));

        PrimitiveList.Byte.Builder bar = root.getDub().initBar(1);
        bar.set(0, (byte) 11);
        Test.TestAllTypes.Builder revBar = root.getRev().getBar();
        revBar.setInt8Field((byte) 111);
        PrimitiveList.Boolean.Builder boolList = revBar.initBoolList(2);
        boolList.set(0, false);
        boolList.set(1, true);

        TestUtil.checkTestMessage(root.getFoo());
        Test.TestGenerics.Reader<Test.TestAllTypes.Reader, Text.Reader> rootReader =
            root.asReader(Test.TestGenerics.newFactory(Test.TestAllTypes.factory, Text.factory));
        TestUtil.checkTestMessage(rootReader.getFoo());
        Test.TestGenerics.Builder<Text.Builder, PrimitiveList.Byte.Builder> dubReader = root.getDub();
        Assert.assertEquals(dubReader.getFoo().toString(), "Hello");
        PrimitiveList.Byte.Builder barReader = dubReader.getBar();
        Assert.assertEquals(1, barReader.size());
        Assert.assertEquals(11, barReader.get(0));
    }

    @org.junit.Test
    public void testUseGenerics() {
        MessageBuilder message = new MessageBuilder();
        Test.TestUseGenerics.Builder root = message.initRoot(Test.TestUseGenerics.factory);
        {
            MessageBuilder message2 = new MessageBuilder();
            Test.TestGenerics.Factory<AnyPointer.Builder, AnyPointer.Reader, AnyPointer.Builder, AnyPointer.Reader> factory2 = Test.TestGenerics.newFactory(AnyPointer.factory, AnyPointer.factory);
            Test.TestGenerics.Builder<AnyPointer.Builder, AnyPointer.Builder> root2 = message2.initRoot(factory2);
            root2.initDub().setFoo(Text.factory, new Text.Reader("foobar"));

            root.setUnspecified(factory2, root2.asReader(factory2));
        }

        Assert.assertEquals("foobar", root.getUnspecified().getDub().getFoo().toString());
    }

    @org.junit.Test
    public void testDefaults() {
        MessageBuilder message = new MessageBuilder();
        Test.TestDefaults.Builder defaults = message.initRoot(Test.TestDefaults.factory);
        TestUtil.checkDefaultMessage(defaults);
        TestUtil.checkDefaultMessage(defaults.asReader());
        TestUtil.setDefaultMessage(defaults);
        TestUtil.checkSettedDefaultMessage(defaults.asReader());
    }

    @org.junit.Test
    public void testUnions() {
      MessageBuilder builder = new MessageBuilder();
      Test.TestUnion.Builder root = builder.initRoot(Test.TestUnion.factory);
      Test.TestUnion.Union0.Builder u0 = root.initUnion0();
      u0.initU0f1sp(10);
      Assert.assertEquals(u0.which(), Test.TestUnion.Union0.Which.U0F1SP);

      u0.initPrimitiveList(10);
      Assert.assertEquals(u0.which(), Test.TestUnion.Union0.Which.PRIMITIVE_LIST);
    }

    @org.junit.Test
    public void testGroups() {
        MessageBuilder builder = new MessageBuilder();
        Test.TestGroups.Builder root = builder.initRoot(Test.TestGroups.factory);
        {
            Test.TestGroups.Groups.Foo.Builder foo = root.getGroups().initFoo();
            foo.setCorge(12345678);
            foo.setGrault(123456789012345L);
            foo.setGarply(new Text.Reader("foobar"));

            Assert.assertEquals(12345678, foo.getCorge());
            Assert.assertEquals(123456789012345L, foo.getGrault());
            Assert.assertEquals("foobar", foo.getGarply().toString());
        }
        {
            Test.TestGroups.Groups.Bar.Builder bar = root.getGroups().initBar();
            bar.setCorge(23456789);
            bar.setGrault(new Text.Reader("barbaz"));
            bar.setGarply(234567890123456L);

            Assert.assertEquals(23456789, bar.getCorge());
            Assert.assertEquals("barbaz", bar.getGrault().toString());
            Assert.assertEquals(234567890123456L, bar.getGarply());
        }
        {
            Test.TestGroups.Groups.Baz.Builder baz = root.getGroups().initBaz();
            baz.setCorge(34567890);
            baz.setGrault(new Text.Reader("bazqux"));
            baz.setGarply(new Text.Reader("quxquux"));

            Assert.assertEquals(34567890, baz.getCorge());
            Assert.assertEquals("bazqux", baz.getGrault().toString());
            Assert.assertEquals("quxquux", baz.getGarply().toString());
        }
    }

    @org.junit.Test
    public void testNestedLists() {
        MessageBuilder builder = new MessageBuilder();
        Test.TestLists.Builder root = builder.initRoot(Test.TestLists.factory);
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
            Test.TestLists.Reader reader = root.asReader();
            ListList.Reader<PrimitiveList.Int.Reader> intListList = reader.getInt32ListList();
            Assert.assertEquals(2, intListList.size());
            PrimitiveList.Int.Reader intList0 = intListList.get(0);
            Assert.assertEquals(4, intList0.size());
            Assert.assertEquals(1, intList0.get(0));
            Assert.assertEquals(2, intList0.get(1));
            Assert.assertEquals(3, intList0.get(2));
            Assert.assertEquals(4, intList0.get(3));
            PrimitiveList.Int.Reader intList1 = intListList.get(1);
            Assert.assertEquals(1, intList1.size());
            Assert.assertEquals(100, intList1.get(0));
        }
    }

    @org.junit.Test
    public void testConstants() {
        Assert.assertEquals(Void.VOID, Test.TestConstants.VOID_CONST);
        Assert.assertEquals(true, Test.TestConstants.BOOL_CONST);
        Assert.assertEquals(-123, Test.TestConstants.INT8_CONST);
        Assert.assertEquals(-12345, Test.TestConstants.INT16_CONST);
        Assert.assertEquals(-12345678, Test.TestConstants.INT32_CONST);
        Assert.assertEquals(-123456789012345L, Test.TestConstants.INT64_CONST);

        Assert.assertEquals(-22, Test.TestConstants.UINT8_CONST);
        Assert.assertEquals(-19858, Test.TestConstants.UINT16_CONST);
        Assert.assertEquals(-838178284, Test.TestConstants.UINT32_CONST);
        Assert.assertEquals(-6101065172474983726L, Test.TestConstants.UINT64_CONST);

        Assert.assertEquals(1234.5f, Test.TestConstants.FLOAT32_CONST, TestUtil.DELTA);
        Assert.assertEquals(-123e45, Test.TestConstants.FLOAT64_CONST, TestUtil.DELTA);

        Assert.assertEquals("foo", Test.TestConstants.TEXT_CONST.toString());
        Assert.assertArrayEquals(TestUtil.data("bar"), Test.TestConstants.DATA_CONST.toArray());

        Assert.assertEquals(Test.TestEnum.CORGE, Test.TestConstants.ENUM_CONST);
        {
            Test.TestAllTypes.Reader subReader = Test.TestConstants.STRUCT_CONST;
            Assert.assertEquals(subReader.getBoolField(), true);
            Assert.assertEquals(subReader.getInt8Field(), -12);
            Assert.assertEquals(subReader.getInt16Field(), 3456);
            Assert.assertEquals(subReader.getInt32Field(), -78901234);
            Assert.assertEquals(subReader.getInt64Field(), 56789012345678L);
            Assert.assertEquals(subReader.getUInt8Field(), 90);
            Assert.assertEquals(subReader.getUInt16Field(), 1234);
            Assert.assertEquals(subReader.getUInt32Field(), 56789012);
            Assert.assertEquals(subReader.getUInt64Field(), 345678901234567890L);
            Assert.assertEquals(subReader.getFloat32Field(), -1.25e-10f, TestUtil.DELTA);
            Assert.assertEquals(subReader.getFloat64Field(), 345, TestUtil.DELTA);
            Assert.assertEquals(subReader.getTextField().toString(), "baz");
        }

        Assert.assertEquals(6, Test.TestConstants.VOID_LIST_CONST.size());

        {
            PrimitiveList.Boolean.Reader listReader = Test.TestConstants.BOOL_LIST_CONST;
            Assert.assertEquals(4, listReader.size());
            Assert.assertEquals(true, listReader.get(0));
            Assert.assertEquals(false, listReader.get(1));
            Assert.assertEquals(false, listReader.get(2));
            Assert.assertEquals(true, listReader.get(3));
        }

        {
            TextList.Reader listReader = Test.TestConstants.TEXT_LIST_CONST;
            Assert.assertEquals(3, listReader.size());
            Assert.assertEquals("plugh", listReader.get(0).toString());
            Assert.assertEquals("xyzzy", listReader.get(1).toString());
            Assert.assertEquals("thud", listReader.get(2).toString());
        }

        {
            StructList.Reader<Test.TestAllTypes.Reader> listReader = Test.TestConstants.STRUCT_LIST_CONST;
            Assert.assertEquals(3, listReader.size());
            Assert.assertEquals("structlist 1", listReader.get(0).getTextField().toString());
            Assert.assertEquals("structlist 2", listReader.get(1).getTextField().toString());
            Assert.assertEquals("structlist 3", listReader.get(2).getTextField().toString());
        }
    }

    @org.junit.Test
    public void testGlobalConstants() {
        Assert.assertEquals(Test.GLOBAL_INT, 12345);
    }

    @org.junit.Test
    public void testEmptyStruct() {
        MessageBuilder builder = new MessageBuilder();
        Test.TestAnyPointer.Builder root = builder.initRoot(Test.TestAnyPointer.factory);

        Assert.assertFalse(root.hasAnyPointerField());

        AnyPointer.Builder any = root.getAnyPointerField();
        Assert.assertTrue(any.isNull());
        any.initAs(Test.TestEmptyStruct.factory);
        Assert.assertFalse(any.isNull());
        Assert.assertTrue(root.hasAnyPointerField());

        {
            Test.TestAnyPointer.Reader reader = root.asReader();
            Assert.assertTrue(reader.hasAnyPointerField());
            Assert.assertNotNull(reader.getAnyPointerField());
        }
    }

    @org.junit.Test(expected=DecodeException.class)
    public void testTextBuilderIntUnderflow() {
        MessageBuilder builder = new MessageBuilder();
        Test.TestAnyPointer.Builder root = builder.initRoot(Test.TestAnyPointer.factory);
        root.getAnyPointerField().initAs(Data.factory, 0);
        root.getAnyPointerField().getAs(Text.factory);
    }

    @org.junit.Test(expected=DecodeException.class)
    public void testInlineCompositeListIntOverflow() throws DecodeException {

        byte[] bytes = new byte[]{0,0,0,0, 0,0,1,0, 1,0,0,0, 0x17,0,0,0, 0,0,0,-128, 16,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,0};

        ByteBuffer segment = ByteBuffer.wrap(bytes);
        segment.order(ByteOrder.LITTLE_ENDIAN);
        MessageReader message = new MessageReader(new ByteBuffer[]{segment}, ReaderOptions.DEFAULT_READER_OPTIONS);

        Test.TestAnyPointer.Reader root = message.getRoot(Test.TestAnyPointer.factory);
        root.getAnyPointerField().getAs(StructList.newFactory(Test.TestAllTypes.factory));
    }

    @org.junit.Test(expected=DecodeException.class)
    public void testInlineCompositeListIntOverflow2() throws DecodeException {

        byte[] bytes = new byte[]{0,0,0,0, 0,0,1,0, 1,0,0,0, 0x17,0,0,0, 0,0,0,-128, 16,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,0};

        ByteBuffer segment = ByteBuffer.wrap(bytes);
        segment.order(ByteOrder.LITTLE_ENDIAN);
        MessageReader message = new MessageReader(new ByteBuffer[]{segment}, ReaderOptions.DEFAULT_READER_OPTIONS);

        Test.TestAnyPointer.Reader root = message.getRoot(Test.TestAnyPointer.factory);

        MessageBuilder messageBuilder = new MessageBuilder();
        Test.TestAnyPointer.Builder builderRoot = messageBuilder.initRoot(Test.TestAnyPointer.factory);
        builderRoot.getAnyPointerField().setAs(Test.TestAnyPointer.factory, root);
    }

    @org.junit.Test(expected=DecodeException.class)
    public void testVoidListAmplification() throws DecodeException {
        MessageBuilder builder = new MessageBuilder();
        builder.initRoot(Test.TestAnyPointer.factory).getAnyPointerField().initAs(PrimitiveList.Void.factory, 1 << 28);

        ByteBuffer[] segments = builder.getSegmentsForOutput();
        Assert.assertEquals(1, segments.length);

        MessageReader reader = new MessageReader(segments, ReaderOptions.DEFAULT_READER_OPTIONS);
        Test.TestAnyPointer.Reader root = reader.getRoot(Test.TestAnyPointer.factory);
        root.getAnyPointerField().getAs(StructList.newFactory(Test.TestAllTypes.factory));
    }

    @org.junit.Test(expected=DecodeException.class)
    public void testEmptyStructListAmplification() {
        MessageBuilder builder = new MessageBuilder();
        builder.initRoot(Test.TestAnyPointer.factory).getAnyPointerField()
                .initAs(StructList.newFactory(Test.TestEmptyStruct.factory), (1 << 29) - 1);

        ByteBuffer[] segments = builder.getSegmentsForOutput();
        Assert.assertEquals(1, segments.length);

        MessageReader reader = new MessageReader(segments, ReaderOptions.DEFAULT_READER_OPTIONS);
        Test.TestAnyPointer.Reader root = reader.getRoot(Test.TestAnyPointer.factory);
        root.getAnyPointerField().getAs(StructList.newFactory(Test.TestAllTypes.factory));
    }

    @org.junit.Test
    public void testLongUint8List() {
      MessageBuilder message = new MessageBuilder();
      Test.TestAllTypes.Builder allTypes = message.initRoot(Test.TestAllTypes.factory);
      int length = (1 << 28) + 1;
      PrimitiveList.Byte.Builder list = allTypes.initUInt8List(length);
      Assert.assertEquals(list.size(), length);
      list.set(length - 1, (byte)3);
      Assert.assertEquals(list.get(length - 1), 3);
      Assert.assertEquals(allTypes.asReader().getUInt8List().get(length - 1), 3);
    }

    @org.junit.Test
    public void testLongUint16List() {
      MessageBuilder message = new MessageBuilder();
      Test.TestAllTypes.Builder allTypes = message.initRoot(Test.TestAllTypes.factory);
      int length = (1 << 27) + 1;
      PrimitiveList.Short.Builder list = allTypes.initUInt16List(length);
      Assert.assertEquals(list.size(), length);
      list.set(length - 1, (short)3);
      Assert.assertEquals(list.get(length - 1), 3);
      Assert.assertEquals(allTypes.asReader().getUInt16List().get(length - 1), 3);
    }

    @org.junit.Test
    public void testLongUint32List() {
      MessageBuilder message = new MessageBuilder();
      Test.TestAllTypes.Builder allTypes = message.initRoot(Test.TestAllTypes.factory);
      int length = (1 << 26) + 1;
      PrimitiveList.Int.Builder list = allTypes.initUInt32List(length);
      Assert.assertEquals(list.size(), length);
      list.set(length - 1, 3);
      Assert.assertEquals(list.get(length - 1), 3);
      Assert.assertEquals(allTypes.asReader().getUInt32List().get(length - 1), 3);
    }

    @org.junit.Test
    public void testLongUint64List() {
      MessageBuilder message = new MessageBuilder();
      Test.TestAllTypes.Builder allTypes = message.initRoot(Test.TestAllTypes.factory);
      int length = (1 << 25) + 1;
      PrimitiveList.Long.Builder list = allTypes.initUInt64List(length);
      Assert.assertEquals(list.size(), length);
      list.set(length - 1, 3);
      Assert.assertEquals(list.get(length - 1), 3);
      Assert.assertEquals(allTypes.asReader().getUInt64List().get(length - 1), 3);
    }

    @org.junit.Test
    public void testLongFloat32List() {
      MessageBuilder message = new MessageBuilder();
      Test.TestAllTypes.Builder allTypes = message.initRoot(Test.TestAllTypes.factory);
      int length = (1 << 26) + 1;
      PrimitiveList.Float.Builder list = allTypes.initFloat32List(length);
      Assert.assertEquals(list.size(), length);
      list.set(length - 1, 3.14f);
      Assert.assertEquals(list.get(length - 1), 3.14f, 0.0f);
      Assert.assertEquals(allTypes.asReader().getFloat32List().get(length - 1), 3.14f, 0.0f);
    }

    @org.junit.Test
    public void testLongFloat64List() {
      MessageBuilder message = new MessageBuilder();
      Test.TestAllTypes.Builder allTypes = message.initRoot(Test.TestAllTypes.factory);
      int length = (1 << 25) + 1;
      PrimitiveList.Double.Builder list = allTypes.initFloat64List(length);
      Assert.assertEquals(list.size(), length);
      list.set(length - 1, 3.14);
      Assert.assertEquals(list.get(length - 1), 3.14, 0.0);
      Assert.assertEquals(allTypes.asReader().getFloat64List().get(length - 1), 3.14, 0.0);
    }

    @org.junit.Test
    public void testLongStructList() {
      MessageBuilder message = new MessageBuilder();
      Test.TestAllTypes.Builder allTypes = message.initRoot(Test.TestAllTypes.factory);
      int length = (1 << 21) + 1;
      StructList.Builder<Test.TestAllTypes.Builder> list = allTypes.initStructList(length);
      Assert.assertEquals(list.size(), length);
      list.get(length - 1).setUInt8Field((byte)3);
      Assert.assertEquals(list.get(length - 1).getUInt8Field(), 3);
      Assert.assertEquals(allTypes.asReader().getStructList().get(length - 1).getUInt8Field(), 3);
    }

    @org.junit.Test
    public void testLongTextList() {
      MessageBuilder message = new MessageBuilder();
      Test.TestAllTypes.Builder allTypes = message.initRoot(Test.TestAllTypes.factory);
      int length = (1 << 25) + 1;
      TextList.Builder list = allTypes.initTextList(length);
      Assert.assertEquals(list.size(), length);
      list.set(length - 1, new Text.Reader("foo"));
      Assert.assertEquals(list.get(length - 1).toString(), "foo");
      Assert.assertEquals(allTypes.asReader().getTextList().get(length - 1).toString(), "foo");
    }

    @org.junit.Test
    public void testLongListList() {
      MessageBuilder message = new MessageBuilder();
      Test.TestLists.Builder root = message.initRoot(Test.TestLists.factory);
      int length = (1 << 25) + 1;
      ListList.Builder<StructList.Builder<Test.TestAllTypes.Builder>> list = root.initStructListList(length);
      Assert.assertEquals(list.size(), length);
      list.init(length - 1, 3);
      Assert.assertEquals(list.get(length - 1).size(), 3);
      Assert.assertEquals(root.asReader().getStructListList().get(length - 1).size(), 3);
    }

    @org.junit.Test
    public void testStructSetters() {
      MessageBuilder builder = new MessageBuilder();
      Test.TestAllTypes.Builder root = builder.initRoot(Test.TestAllTypes.factory);
      TestUtil.initTestMessage(root);

      {
        MessageBuilder builder2 = new MessageBuilder();
        builder2.setRoot(Test.TestAllTypes.factory, root.asReader());
        TestUtil.checkTestMessage(builder2.getRoot(Test.TestAllTypes.factory));
      }

      {
        MessageBuilder builder2 = new MessageBuilder();
        Test.TestAllTypes.Builder root2 = builder2.getRoot(Test.TestAllTypes.factory);
        root2.setStructField(root.asReader());
        TestUtil.checkTestMessage(root2.getStructField());
      }

      {
        MessageBuilder builder2 = new MessageBuilder();
        Test.TestAnyPointer.Builder root2 = builder2.getRoot(Test.TestAnyPointer.factory);
        root2.getAnyPointerField().setAs(Test.TestAllTypes.factory, root.asReader());
        TestUtil.checkTestMessage(root2.getAnyPointerField().getAs(Test.TestAllTypes.factory));
      }
    }

    @org.junit.Test
    public void testSerializedSize() {
      MessageBuilder builder = new MessageBuilder();
      Test.TestAnyPointer.Builder root = builder.initRoot(Test.TestAnyPointer.factory);
      root.getAnyPointerField().setAs(Text.factory, new Text.Reader("12345"));

      // one word for segment table, one for the root pointer,
      // one for the body of the TestAnyPointer struct,
      // and one for the body of the Text.
      Assert.assertEquals(Serialize.computeSerializedSizeInWords(builder), 4);
    }

    @org.junit.Test
    public void testImport() {
      MessageBuilder builder = new MessageBuilder();
      org.capnproto.testimport.TestImport.Foo.Builder root = builder.initRoot(org.capnproto.testimport.TestImport.Foo.factory);
      Test.TestAllTypes.Builder field = root.initImportedStruct();
      TestUtil.initTestMessage(field);
      TestUtil.checkTestMessage(field);
      TestUtil.checkTestMessage(field.asReader());
    }

    @org.junit.Test
    public void testGenericMap() {
      MessageBuilder builder = new MessageBuilder();
      Test.GenericMap.Factory<Text.Builder, Text.Reader, Test.TestAllTypes.Builder, Test.TestAllTypes.Reader> mapFactory
        = Test.GenericMap.newFactory(Text.factory, Test.TestAllTypes.factory);

      StructList.Factory<Test.GenericMap.Entry.Builder<Text.Builder, Test.TestAllTypes.Builder>,
                         Test.GenericMap.Entry.Reader<Text.Reader, Test.TestAllTypes.Reader>> entryFactory
        = StructList.newFactory(Test.GenericMap.Entry.newFactory(Text.factory, Test.TestAllTypes.factory));

      Test.GenericMap.Builder<Text.Builder, Test.TestAllTypes.Builder> root = builder.initRoot(mapFactory);

      {
        StructList.Builder<Test.GenericMap.Entry.Builder<Text.Builder, Test.TestAllTypes.Builder>> entries = root.initEntries(entryFactory, 3);

        Test.GenericMap.Entry.Builder<Text.Builder, Test.TestAllTypes.Builder> entry0 = entries.get(0);
        entry0.setKey(Text.factory, new Text.Reader("foo"));
        Test.TestAllTypes.Builder value0 = entry0.initValue();
        value0.setInt64Field(101);

        Test.GenericMap.Entry.Builder<Text.Builder, Test.TestAllTypes.Builder> entry1 = entries.get(1);
        entry1.setKey(Text.factory, new Text.Reader("bar"));
        Test.TestAllTypes.Builder value1 = entry1.initValue();
        value1.setInt64Field(202);

        Test.GenericMap.Entry.Builder<Text.Builder, Test.TestAllTypes.Builder> entry2 = entries.get(2);
        entry2.setKey(Text.factory, new Text.Reader("baz"));
        Test.TestAllTypes.Builder value2 = entry2.initValue();
        value2.setInt64Field(303);
      }

      {
         StructList.Reader<Test.GenericMap.Entry.Reader<Text.Reader, Test.TestAllTypes.Reader>> entries =
           root.asReader(mapFactory).getEntries(entryFactory);

         Test.GenericMap.Entry.Reader<Text.Reader, Test.TestAllTypes.Reader> entry0 = entries.get(0);
         Assert.assertEquals(entry0.getKey().toString(), "foo");
         Assert.assertEquals(entry0.getValue().getInt64Field(), 101);

         Test.GenericMap.Entry.Reader<Text.Reader, Test.TestAllTypes.Reader> entry1 = entries.get(1);
         Assert.assertEquals(entry1.getKey().toString(), "bar");
         Assert.assertEquals(entry1.getValue().getInt64Field(), 202);

         Test.GenericMap.Entry.Reader<Text.Reader, Test.TestAllTypes.Reader> entry2 = entries.get(2);
         Assert.assertEquals(entry2.getKey().toString(), "baz");
         Assert.assertEquals(entry2.getValue().getInt64Field(), 303);
      }
    }

    @org.junit.Test
    public void testSetWithCaveats() {
      MessageBuilder builder = new MessageBuilder();
      Test.TestAllTypes.Builder root = builder.initRoot(Test.TestAllTypes.factory);
      StructList.Builder<Test.TestAllTypes.Builder> list = root.initStructList(2);

      {
        MessageBuilder message1 = new MessageBuilder();
        Test.TestAllTypes.Builder root1 = message1.initRoot(Test.TestAllTypes.factory);
        root1.setInt8Field((byte)11);
        list.setWithCaveats(Test.TestAllTypes.factory, 0, root1.asReader());
      }

      {
        MessageBuilder message2 = new MessageBuilder();
        Test.TestAllTypes.Builder root2 = message2.initRoot(Test.TestAllTypes.factory);
        TestUtil.initTestMessage(root2);
        list.setWithCaveats(Test.TestAllTypes.factory, 1, root2.asReader());
      }

      StructList.Reader<Test.TestAllTypes.Reader> listReader = list.asReader(Test.TestAllTypes.factory);
      Assert.assertEquals(listReader.get(0).getInt8Field(), 11);
      TestUtil.checkTestMessage(listReader.get(1));
  }

    @org.junit.Test
    public void testCopyAnyPointer() {
        MessageBuilder message1 = new MessageBuilder();
        Test.TestAllTypes.Builder root1 = message1.initRoot(Test.TestAllTypes.factory);
        TestUtil.initTestMessage(root1);

        MessageBuilder message2 = new MessageBuilder();
        AnyPointer.Builder root2 = message2.initRoot(AnyPointer.factory);
        root2.set(message1.getRoot(AnyPointer.factory).asReader());

        TestUtil.checkTestMessage(root2.getAs(Test.TestAllTypes.factory));
    }
}


