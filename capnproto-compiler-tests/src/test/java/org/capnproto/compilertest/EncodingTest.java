package org.capnproto.compilertest;

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
    public void testGenerics() {
        MessageBuilder message = new MessageBuilder();
        Test.TestGenerics.Factory factory = Test.TestGenerics.newFactory(Test.TestAllTypes.factory, Text.factory);

        Test.TestGenerics.Builder<Test.TestAllTypes.Builder, Text.Builder> root = (Test.TestGenerics.Builder<Test.TestAllTypes.Builder, Text.Builder>) message.initRoot(factory);
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
        Test.TestGenerics.Reader<Test.TestAllTypes.Reader, Text.Reader> rootReader = root.asReader(factory);
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
        root.getAnyPointerField().getAs(new StructList.Factory(Test.TestAllTypes.factory));
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
        root.getAnyPointerField().getAs(new StructList.Factory(Test.TestAllTypes.factory));
    }

    @org.junit.Test(expected=DecodeException.class)
    public void testEmptyStructListAmplification() {
        MessageBuilder builder = new MessageBuilder();
        builder.initRoot(Test.TestAnyPointer.factory).getAnyPointerField()
                .initAs(new StructList.Factory(Test.TestEmptyStruct.factory), (1 << 29) - 1);

        ByteBuffer[] segments = builder.getSegmentsForOutput();
        Assert.assertEquals(1, segments.length);

        MessageReader reader = new MessageReader(segments, ReaderOptions.DEFAULT_READER_OPTIONS);
        Test.TestAnyPointer.Reader root = reader.getRoot(Test.TestAnyPointer.factory);
        root.getAnyPointerField().getAs(new StructList.Factory(Test.TestAllTypes.factory));
    }
}


