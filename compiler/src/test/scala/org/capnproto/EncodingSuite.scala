// Copyright (c) 2013-2014 Sandstorm Development Group, Inc. and contributors
// Licensed under the MIT License:
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

package org.capnproto

import org.capnproto.test.Test._
import org.scalatest.FunSuite
import org.scalatest.Matchers._

class EncodingSuite extends FunSuite {
  test("AllTypes") {
    val message = new MessageBuilder()
    val allTypes = message.initRoot(TestAllTypes.factory)
    TestUtil.initTestMessage(allTypes)
    TestUtil.checkTestMessage(allTypes)
    TestUtil.checkTestMessage(allTypes.asReader())
  }

  test("AllTypesMultiSegment") {
    val message = new MessageBuilder(5, BuilderArena.AllocationStrategy.FIXED_SIZE)
    val allTypes = message.initRoot(TestAllTypes.factory)
    TestUtil.initTestMessage(allTypes)

    TestUtil.checkTestMessage(allTypes)
    TestUtil.checkTestMessage(allTypes.asReader())
  }

  test("Setters") {
    val message = new MessageBuilder()
    val allTypes = message.initRoot(TestAllTypes.factory)
    TestUtil.initTestMessage(allTypes)

    val message2 = new MessageBuilder()
    val allTypes2 = message2.initRoot(TestAllTypes.factory)

    allTypes2.setStructField(allTypes.asReader())
    TestUtil.checkTestMessage(allTypes2.getStructField())
    val reader = allTypes2.asReader().getStructField()
    TestUtil.checkTestMessage(reader)
  }

  test("Zeroing") {
    val message = new MessageBuilder()
    val allTypes = message.initRoot(TestAllTypes.factory)

    val structList = allTypes.initStructList(3)
    TestUtil.initTestMessage(structList.get(0))

    val structField = allTypes.initStructField()
    TestUtil.initTestMessage(structField)

    TestUtil.initTestMessage(structList.get(1))
    TestUtil.initTestMessage(structList.get(2))
    TestUtil.checkTestMessage(structList.get(0))
    allTypes.initStructList(0)

    TestUtil.checkTestMessage(allTypes.getStructField())
    val allTypesReader = allTypes.asReader()
    TestUtil.checkTestMessage(allTypesReader.getStructField())

    val any = message.initRoot(AnyPointer.factory)
    val segments = message.getSegmentsForOutput()
    for (segment <- segments) {
      for (jj <- 0 to segment.limit - 1) {
        segment.get(jj) should equal (0)
      }
    }
  }

  test("DoubleFarPointers") {
    val bytes = Array[Byte](2,0,0,0, 1,0,0,0, 2,0,0,0, 1,0,0,0,
                            6,0,0,0, 1,0,0,0, 2,0,0,0, 2,0,0,0,
                            0,0,0,0, 1,0,0,0, 1,7, -1, 127, 0,0,0,0)

    val input = new ArrayInputStream (java.nio.ByteBuffer.wrap(bytes))
    val message = org.capnproto.Serialize.read(input)
    val root = message.getRoot(TestAllTypes.factory)
    root.getBoolField() should equal (true)
    root.getInt8Field() should equal (7)
    root.getInt16Field() should equal (32767)
  }

  test("UpgradeStructInBuilder") {
    val builder = new MessageBuilder()
    val root = builder.initRoot(TestAnyPointer.factory)

    {
      val oldVersion = root.getAnyPointerField().initAs(TestOldVersion.factory)
      oldVersion.setOld1(123)
      oldVersion.setOld2("foo")
      val sub = oldVersion.initOld3()
      sub.setOld1(456)
      sub.setOld2("bar")
    }

    {
      val newVersion = root.getAnyPointerField().getAs(TestNewVersion.factory)
      newVersion.getOld1() should equal (123)
      newVersion.getOld2().toString() should equal ("foo")
      newVersion.getNew1() should equal (987)
      newVersion.getNew2().toString() should equal ("baz")
      val sub = newVersion.getOld3()
      sub.getOld1() should equal (456)
      sub.getOld2().toString() should equal ("bar")

      newVersion.setOld1(234)
      newVersion.setOld2("qux")
      newVersion.setNew1(654)
      newVersion.setNew2("quux")

    }

    {
      val oldVersion = root.getAnyPointerField().getAs(TestOldVersion.factory)
      oldVersion.getOld1() should equal (234)
      oldVersion.getOld2.toString() should equal ("qux")
    }
  }

  test("StructListUpgrade") {
    val message = new MessageBuilder()
    val root = message.initRoot(TestAnyPointer.factory)
    val any = root.getAnyPointerField()

    {
      val longs = any.initAs(PrimitiveList.Long.factory, 3)
      longs.set(0, 123)
      longs.set(1, 456)
      longs.set(2, 789)
    }

    {
      val olds = any.asReader().getAs(TestOldVersion.listFactory)
      olds.get(0).getOld1() should equal (123)
      olds.get(1).getOld1() should equal (456)
      olds.get(2).getOld1() should equal (789)
    }

    {
      val olds = any.getAs(TestOldVersion.listFactory)
      olds.size() should equal (3)
      olds.get(0).getOld1() should equal (123)
      olds.get(1).getOld1() should equal (456)
      olds.get(2).getOld1() should equal (789)

      olds.get(0).setOld2("zero")
      olds.get(1).setOld2("one")
      olds.get(2).setOld2("two")
    }

    {
      val news = any.getAs(TestNewVersion.listFactory)
      news.size() should equal (3)
      news.get(0).getOld1() should equal (123)
      news.get(0).getOld2().toString() should equal ("zero")

      news.get(1).getOld1() should equal (456)
      news.get(1).getOld2().toString() should equal ("one")

      news.get(2).getOld1() should equal (789)
      news.get(2).getOld2().toString() should equal ("two")
    }
  }

  test("Generics") {
    val message = new MessageBuilder()
    val factory = TestGenerics.newFactory(TestAllTypes.factory, Text.factory)
    val root = message.initRoot(factory)
    TestUtil.initTestMessage(root.getFoo())
    root.getDub().setFoo(Text.factory, new Text.Reader("Hello"))
    val bar = root.getDub().initBar(1)
    bar.set(0, 11)
    val revBar = root.getRev().getBar()
    revBar.setInt8Field(111)
    val boolList = revBar.initBoolList(2)
    boolList.set(0, false)
    boolList.set(1, true)

    TestUtil.checkTestMessage(root.getFoo())
    val rootReader = root.asReader(factory)
    TestUtil.checkTestMessage(rootReader.getFoo())
    val dubReader = root.getDub()
    dubReader.getFoo().toString() should equal ("Hello")
    val barReader = dubReader.getBar()
    barReader.size() should equal (1)
    barReader.get(0) should equal (11)
  }

  test("UseGenerics") {
    val message = new MessageBuilder()
    val root = message.initRoot(TestUseGenerics.factory)

    {
      val message2 = new MessageBuilder()
      val factory2 = TestGenerics.newFactory(AnyPointer.factory, AnyPointer.factory)
      val root2 = message2.initRoot(factory2)
      val dub2 = root2.initDub().setFoo(Text.factory, new Text.Reader("foobar"))

      root.setUnspecified(factory2, root2.asReader(factory2))
    }

    val rootReader = root.asReader()
    root.getUnspecified().getDub().getFoo().toString() should equal ("foobar")
  }

  test("Defaults") {
    val message = new MessageBuilder()
    val defaults = message.initRoot(TestDefaults.factory)
    TestUtil.checkDefaultMessage(defaults)
    TestUtil.checkDefaultMessage(defaults.asReader())
    TestUtil.setDefaultMessage(defaults)
    TestUtil.checkSettedDefaultMessage(defaults.asReader())
  }

  test("Groups") {
    val builder = new MessageBuilder()
    val root = builder.initRoot(TestGroups.factory)

    {
      val foo = root.getGroups().initFoo()
      foo.setCorge(12345678)
      foo.setGrault(123456789012345L)
      foo.setGarply(new Text.Reader("foobar"))

      assert(12345678 == foo.getCorge())
      assert(123456789012345L == foo.getGrault())
      assert("foobar" == foo.getGarply().toString())
    }

    {
      val bar = root.getGroups.initBar()
      bar.setCorge(23456789)
      bar.setGrault(new Text.Reader("barbaz"))
      bar.setGarply(234567890123456L)

      assert(23456789 == bar.getCorge())
      assert("barbaz" == bar.getGrault().toString())
      assert(234567890123456L == bar.getGarply())
    }

    {
      val baz = root.getGroups().initBaz()
      baz.setCorge(34567890)
      baz.setGrault(new Text.Reader("bazqux"))
      baz.setGarply(new Text.Reader("quxquux"))

      assert(34567890 == baz.getCorge())
      assert("bazqux" == baz.getGrault().toString())
      assert("quxquux" == baz.getGarply().toString())
    }
  }


  test("NestedLists") {
    val builder = new MessageBuilder()
    val root = builder.initRoot(TestLists.factory)

    {
      val intListList = root.initInt32ListList(2)
      val intList0 = intListList.init(0, 4)
      intList0.set(0, 1)
      intList0.set(1, 2)
      intList0.set(2, 3)
      intList0.set(3, 4)
      val intList1 = intListList.init(1, 1)
      intList1.set(0, 100)
    }

    {
      val reader = root.asReader()
      val intListList = root.getInt32ListList()
      intListList.size() should equal (2)
      val intList0 = intListList.get(0)
      intList0.size() should equal (4)
      intList0.get(0) should equal (1)
      intList0.get(1) should equal (2)
      intList0.get(2) should equal (3)
      intList0.get(3) should equal (4)
      val intList1 = intListList.get(1)
      intList1.size() should equal (1)
      intList1.get(0) should equal(100)
    }
  }

  test("Constants") {
    assert(Void.VOID == TestConstants.VOID_CONST)
    assert(true == TestConstants.BOOL_CONST)
    assert(-123 == TestConstants.INT8_CONST)
    assert(-12345 == TestConstants.INT16_CONST)
    assert(-12345678 == TestConstants.INT32_CONST)
    assert(-123456789012345L == TestConstants.INT64_CONST)

    assert(-22 == TestConstants.UINT8_CONST)
    assert(-19858 == TestConstants.UINT16_CONST)
    assert(-838178284 == TestConstants.UINT32_CONST)
    assert(-6101065172474983726L == TestConstants.UINT64_CONST)

    assert(1234.5f == TestConstants.FLOAT32_CONST)
    (-123e45) should equal (TestConstants.FLOAT64_CONST)

    (TestConstants.TEXT_CONST.toString()) should equal ("foo")
    (TestConstants.DATA_CONST.toArray()) should equal (TestUtil.data("bar"))

    (TestConstants.ENUM_CONST) should equal (TestEnum.CORGE)

    {
      val subReader = TestConstants.STRUCT_CONST
      subReader.getBoolField() should equal (true)
      subReader.getInt8Field() should equal (-12)
      subReader.getInt16Field() should equal(3456)
      subReader.getInt32Field() should equal (-78901234)
      subReader.getInt64Field() should equal (56789012345678L)
      subReader.getUInt8Field() should equal (90)
      subReader.getUInt16Field should equal (1234)
      subReader.getUInt32Field() should equal (56789012)
      subReader.getUInt64Field() should equal (345678901234567890L)
      subReader.getFloat32Field() should equal (-1.25e-10f)
      subReader.getFloat64Field() should equal (345)
      subReader.getTextField().toString() should equal ("baz")
      // ...
    }

    TestConstants.VOID_LIST_CONST.size() should equal (6)

    {
      val listReader = TestConstants.BOOL_LIST_CONST
      listReader.size() should equal (4)
      listReader.get(0) should equal (true)
      listReader.get(1) should equal (false)
      listReader.get(2) should equal (false)
      listReader.get(3) should equal (true)
    }

    // ...
    {
      val listReader = TestConstants.TEXT_LIST_CONST
      listReader.size() should equal(3)
      listReader.get(0).toString() should equal ("plugh")
      listReader.get(1).toString() should equal ("xyzzy")
      listReader.get(2).toString() should equal ("thud")
    }

    {
      val listReader = TestConstants.STRUCT_LIST_CONST
      listReader.size() should equal(3)
      listReader.get(0).getTextField().toString() should equal ("structlist 1")
      listReader.get(1).getTextField().toString() should equal ("structlist 2")
      listReader.get(2).getTextField().toString() should equal ("structlist 3")
    }

  }

  test("GlobalConstants") {
    assert(12345 == GLOBAL_INT)
  }

  test("EmptyStruct") {
    val builder = new MessageBuilder()
    val root = builder.initRoot(TestAnyPointer.factory)
    root.hasAnyPointerField() should equal (false)
    val any = root.getAnyPointerField()
    any.isNull() should equal (true)
    any.initAs(TestEmptyStruct.factory)
    any.isNull() should equal (false)
    root.hasAnyPointerField() should equal (true)

    {
      val rootReader = root.asReader()
      rootReader.hasAnyPointerField() should equal (true)
      rootReader.getAnyPointerField().isNull() should equal (false)
    }
  }

  test("TextBuilderIntUnderflow") {
    val message = new MessageBuilder()
    val root = message.initRoot(TestAnyPointer.factory)
    root.getAnyPointerField.initAs(org.capnproto.Data.factory, 0)
    a [DecodeException] should be thrownBy root.getAnyPointerField.getAs(org.capnproto.Text.factory)
  }

  test("InlineCompositeListIntOverflow") {
    val bytes = Array[Byte](0,0,0,0, 0,0,1,0,
                            1,0,0,0, 0x17,0,0,0, 0,0,0,-128, 16,0,0,0,
                            0,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,0)

    val segment = java.nio.ByteBuffer.wrap(bytes)
    segment.order(java.nio.ByteOrder.LITTLE_ENDIAN)
    val message = new MessageReader(Array(segment), ReaderOptions.DEFAULT_READER_OPTIONS)

    val root = message.getRoot(TestAnyPointer.factory)
    // TODO add this after we impelement totalSize():
    //root.totalSize()

    a [DecodeException] should be thrownBy
      root.getAnyPointerField.getAs(new StructList.Factory(TestAllTypes.factory))

    val messageBuilder = new MessageBuilder()
    val builderRoot = messageBuilder.initRoot(TestAnyPointer.factory)
    a [DecodeException] should be thrownBy
      builderRoot.getAnyPointerField.setAs(TestAnyPointer.factory, root)

  }

  test("VoidListAmplification") {
    val builder = new MessageBuilder()
    builder.initRoot(TestAnyPointer.factory).getAnyPointerField().initAs(PrimitiveList.Void.factory, 1 << 28)

    val segments = builder.getSegmentsForOutput()
    segments.length should equal (1)

    val reader = new MessageReader(segments, ReaderOptions.DEFAULT_READER_OPTIONS)
    val root = reader.getRoot(TestAnyPointer.factory)
    a [DecodeException] should be thrownBy
       root.getAnyPointerField().getAs(new StructList.Factory(TestAllTypes.factory))
  }

  test("EmptyStructListAmplification") {
    val builder = new MessageBuilder()
    builder.initRoot(TestAnyPointer.factory).getAnyPointerField()
           .initAs(new StructList.Factory(TestEmptyStruct.factory), (1 << 29) - 1)

    val segments = builder.getSegmentsForOutput()
    segments.length should equal (1)

    val reader = new MessageReader(segments, ReaderOptions.DEFAULT_READER_OPTIONS)
    val root = reader.getRoot(TestAnyPointer.factory)
    a [DecodeException] should be thrownBy
       root.getAnyPointerField().getAs(new StructList.Factory(TestAllTypes.factory))
  }

  test("LongUint8List") {
    {
      val message = new MessageBuilder()
      val allTypes = message.initRoot(TestAllTypes.factory)
      val length = (1 << 28) + 1
      val list = allTypes.initUInt8List(length)
      list.size() should equal (length)
      list.set(length - 1, 3)
      list.get(length - 1) should equal (3)
      allTypes.asReader().getUInt8List().get(length - 1) should equal (3)
    }
  }


  test("LongUint16List") {
    {
      val message = new MessageBuilder()
      val allTypes = message.initRoot(TestAllTypes.factory)
      val length = (1 << 27) + 1
      val list = allTypes.initUInt16List(length)
      list.size() should equal (length)
      list.set(length - 1, 3)
      list.get(length - 1) should equal (3)
      allTypes.asReader().getUInt16List().get(length - 1) should equal (3)
    }
  }

  test("LongUint32List") {
    {
      val message = new MessageBuilder()
      val allTypes = message.initRoot(TestAllTypes.factory)
      val length = (1 << 26) + 1
      val list = allTypes.initUInt32List(length)
      list.size() should equal (length)
      list.set(length - 1, 3)
      list.get(length - 1) should equal (3)
      allTypes.asReader().getUInt32List().get(length - 1) should equal (3)
    }
  }

  test("LongUint64List") {
    {
      val message = new MessageBuilder()
      val allTypes = message.initRoot(TestAllTypes.factory)
      val length = (1 << 25) + 1
      val list = allTypes.initUInt64List(length)
      list.size() should equal (length)
      list.set(length - 1, 3)
      list.get(length - 1) should equal (3)
      allTypes.asReader().getUInt64List().get(length - 1) should equal (3)
    }
  }

  test("LongFloat32List") {
    {
      val message = new MessageBuilder()
      val allTypes = message.initRoot(TestAllTypes.factory)
      val length = (1 << 26) + 1
      val list = allTypes.initFloat32List(length)
      list.size() should equal (length)
      list.set(length - 1, 3.14f)
      list.get(length - 1) should equal (3.14f)
      allTypes.asReader().getFloat32List().get(length - 1) should equal (3.14f)
    }
  }

  test("LongFloat64List") {
    {
      val message = new MessageBuilder()
      val allTypes = message.initRoot(TestAllTypes.factory)
      val length = (1 << 25) + 1
      val list = allTypes.initFloat64List(length)
      list.size() should equal (length)
      list.set(length - 1, 3.14)
      list.get(length - 1) should equal (3.14)
      allTypes.asReader().getFloat64List().get(length - 1) should equal (3.14)
    }
  }

  test("LongStructList") {
    {
      val message = new MessageBuilder()
      val allTypes = message.initRoot(TestAllTypes.factory)
      val length = (1 << 21) + 1
      val list = allTypes.initStructList(length)
      list.size() should equal (length)
      list.get(length - 1).setUInt8Field(3)
      allTypes.asReader().getStructList().get(length - 1).getUInt8Field() should equal (3)
    }
  }

  test("LongTextList") {
    {
      val message = new MessageBuilder()
      val allTypes = message.initRoot(TestAllTypes.factory)
      val length = (1 << 25) + 1
      val list = allTypes.initTextList(length)
      list.size() should equal (length)
      list.set(length - 1, new Text.Reader("foo"))
      allTypes.asReader().getTextList().get(length - 1).toString() should equal ("foo")
    }
  }


  test("LongListList") {
    {
      val message = new MessageBuilder()
      val root = message.initRoot(TestLists.factory)
      val length = (1 << 25) + 1
      val list = root.initStructListList(length)
      list.size() should equal (length)
      list.init(length - 1, 3)
      list.get(length - 1).size() should equal (3)
      root.asReader().getStructListList().get(length - 1).size() should equal (3)
    }
  }


  // to debug, do this:
  //Serialize.write((new java.io.FileOutputStream("/Users/dwrensha/Desktop/test.dat")).getChannel(),
  //                 message)

}
