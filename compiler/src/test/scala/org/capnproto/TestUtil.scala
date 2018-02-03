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

import org.capnproto.Text.Reader
import org.scalatest.Matchers._
import org.capnproto.test.Test._


object TestUtil {

  def data(str : String) : Array[Byte] = {
    try {
      str.getBytes("ISO_8859-1")
    } catch {
      case e: Exception => throw new Error("could not decode")
    }
  }

  def initTestMessage(builder : TestAllTypes.Builder) {
    builder.setVoidField(org.capnproto.Void.VOID)
    builder.setBoolField(true)
    builder.setInt8Field(-123)
    builder.setInt16Field(-12345)
    builder.setInt32Field(-12345678)
    builder.setInt64Field(-123456789012345L)
    builder.setUInt8Field(0xea.toByte)
    builder.setUInt16Field(0x4567)
    builder.setUInt32Field(0x34567890)
    builder.setUInt64Field(0x1234567890123456L)
    builder.setFloat32Field(1234.5f)
    builder.setFloat64Field(-123e45)
    builder.setTextField("foo")
    builder.setDataField(data("bar"))

    {
      val subBuilder = builder.initStructField()
      subBuilder.setVoidField(org.capnproto.Void.VOID)
      subBuilder.setBoolField(true)
      subBuilder.setInt8Field(-12)
      subBuilder.setInt16Field(3456)
      subBuilder.setInt32Field(-78901234)
      subBuilder.setInt64Field(56789012345678L)
      subBuilder.setUInt8Field(90)
      subBuilder.setUInt16Field(1234)
      subBuilder.setUInt32Field(56789012)
      subBuilder.setUInt64Field(345678901234567890L)
      subBuilder.setFloat32Field(-1.25e-10f)
      subBuilder.setFloat64Field(345)
      subBuilder.setTextField(new Text.Reader("baz"))
      subBuilder.setDataField(data("qux"))

      {
        val subSubBuilder = subBuilder.initStructField()
        subSubBuilder.setTextField(new Text.Reader("nested"))
        subSubBuilder.initStructField().setTextField(new Text.Reader("really nested"))
      }

      subBuilder.setEnumField(TestEnum.BAZ)

      val boolList = subBuilder.initBoolList(5)
      boolList.set(0, false)
      boolList.set(1, true)
      boolList.set(2, false)
      boolList.set(3, true)
      boolList.set(4, true)
    }

    builder.setEnumField(TestEnum.CORGE)
    builder.initVoidList(6)

    val boolList = builder.initBoolList(4)
    boolList.set(0, true)
    boolList.set(1, false)
    boolList.set(2, false)
    boolList.set(3, true)

    val float64List = builder.initFloat64List(4)
    float64List.set(0, 7777.75)
    float64List.set(1, Double.PositiveInfinity)
    float64List.set(2, Double.NegativeInfinity)
    float64List.set(3, Double.NaN)

    val textList = builder.initTextList(3)
    textList.set(0, new Text.Reader("plugh"))
    textList.set(1, new Text.Reader("xyzzy"))
    textList.set(2, new Text.Reader("thud"))

    val structList = builder.initStructList(3)
    structList.get(0).setTextField(new Text.Reader("structlist 1"))
    structList.get(1).setTextField(new Text.Reader("structlist 2"))
    structList.get(2).setTextField(new Text.Reader("structlist 3"))


    val enumList = builder.initEnumList(2)
    enumList.set(0, TestEnum.FOO)
    enumList.set(1, TestEnum.GARPLY)
  }

  def checkTestMessage(builder : TestAllTypes.Builder) {
    builder.getVoidField()
    assert(builder.getBoolField() == true)
    assert(builder.getInt8Field() == -123)
    assert(builder.getInt16Field() == -12345)
    assert(builder.getInt32Field() == -12345678)
    assert(builder.getInt64Field() == -123456789012345L)
    assert(builder.getUInt8Field() == 0xea.toByte)
    assert(builder.getUInt16Field() == 0x4567)
    assert(builder.getUInt32Field() == 0x34567890)
    assert(builder.getUInt64Field() == 0x1234567890123456L)
    assert(builder.getFloat32Field() == 1234.5f)
    assert(builder.getFloat64Field() == -123e45)
    assert(builder.getTextField().toString() == "foo")

    {
      val subBuilder = builder.getStructField()
      subBuilder.getVoidField()
      assert(subBuilder.getBoolField() == true)
      assert(subBuilder.getInt8Field() == -12)
      assert(subBuilder.getInt16Field() == 3456)
      assert(subBuilder.getInt32Field() == -78901234)
      assert(subBuilder.getInt64Field() == 56789012345678L)
      assert(subBuilder.getUInt8Field() == 90)
      assert(subBuilder.getUInt16Field() == 1234)
      assert(subBuilder.getUInt32Field() == 56789012)
      assert(subBuilder.getUInt64Field() == 345678901234567890L)
      assert(subBuilder.getFloat32Field() == -1.25e-10f)
      assert(subBuilder.getFloat64Field() == 345)

      {
        val subSubBuilder = subBuilder.getStructField()
        assert(subSubBuilder.getTextField().toString() == "nested")
      }

      subBuilder.getEnumField() should equal (TestEnum.BAZ)

      val boolList = subBuilder.getBoolList()
      assert(boolList.get(0) == false)
      assert(boolList.get(1) == true)
      assert(boolList.get(2) == false)
      assert(boolList.get(3) == true)
      assert(boolList.get(4) == true)

    }
    builder.getEnumField() should equal (TestEnum.CORGE)

    assert(builder.getVoidList().size() == 6)

    val boolList = builder.getBoolList()
    assert(boolList.size() == 4)
    assert(boolList.get(0) == true)
    assert(boolList.get(1) == false)
    assert(boolList.get(2) == false)
    assert(boolList.get(3) == true)

    val float64List = builder.getFloat64List()
    assert(float64List.get(0) == 7777.75)
    assert(float64List.get(1) == Double.PositiveInfinity)
    assert(float64List.get(2) == Double.NegativeInfinity)
    assert(float64List.get(3) != float64List.get(3)); // NaN

    val textList = builder.getTextList()
    assert(textList.size() == 3)
    assert(textList.get(0).toString() == "plugh")
    assert(textList.get(1).toString() == "xyzzy")
    assert(textList.get(2).toString() == "thud")

    val structList = builder.getStructList()
    assert(3 == structList.size())
    assert(structList.get(0).getTextField().toString() == "structlist 1")
    assert(structList.get(1).getTextField().toString() == "structlist 2")
    assert(structList.get(2).getTextField().toString() == "structlist 3")

    val enumList = builder.getEnumList()
    (enumList.get(0)) should equal (TestEnum.FOO)
    (enumList.get(1)) should equal (TestEnum.GARPLY)
  }

  def checkTestMessage(reader : TestAllTypes.Reader) {
    reader.getVoidField()
    assert(reader.getBoolField() == true)
    assert(reader.getInt8Field() == -123)
    assert(reader.getInt16Field() == -12345)
    assert(reader.getInt32Field() == -12345678)
    assert(reader.getInt64Field() == -123456789012345L)
    assert(reader.getUInt8Field() == 0xea.toByte)
    assert(reader.getUInt16Field() == 0x4567)
    assert(reader.getUInt32Field() == 0x34567890)
    assert(reader.getUInt64Field() == 0x1234567890123456L)
    assert(reader.getFloat32Field() == 1234.5f)
    assert(reader.getFloat64Field() == -123e45)
    assert(reader.getTextField().toString() == "foo")

    {
      val subReader = reader.getStructField()
      subReader.getVoidField()
      assert(subReader.getBoolField() == true)
      assert(subReader.getInt8Field() == -12)
      assert(subReader.getInt16Field() == 3456)
      assert(subReader.getInt32Field() == -78901234)
      assert(subReader.getInt64Field() == 56789012345678L)
      assert(subReader.getUInt8Field() == 90)
      assert(subReader.getUInt16Field() == 1234)
      assert(subReader.getUInt32Field() == 56789012)
      assert(subReader.getUInt64Field() == 345678901234567890L)
      assert(subReader.getFloat32Field() == -1.25e-10f)
      assert(subReader.getFloat64Field() == 345)

      {
        val subSubReader = subReader.getStructField()
        assert(subSubReader.getTextField().toString() == "nested")
      }
      val boolList = subReader.getBoolList()
      assert(boolList.get(0) == false)
      assert(boolList.get(1) == true)
      assert(boolList.get(2) == false)
      assert(boolList.get(3) == true)
      assert(boolList.get(4) == true)

    }

    assert(reader.getVoidList().size() == 6)

    val boolList = reader.getBoolList()
    assert(boolList.get(0) == true)
    assert(boolList.get(1) == false)
    assert(boolList.get(2) == false)
    assert(boolList.get(3) == true)

    val float64List = reader.getFloat64List()
    assert(float64List.get(0) == 7777.75)
    assert(float64List.get(1) == Double.PositiveInfinity)
    assert(float64List.get(2) == Double.NegativeInfinity)
    assert(float64List.get(3) != float64List.get(3)); // NaN

    val textList = reader.getTextList()
    assert(textList.size() == 3)
    assert(textList.get(0).toString() == "plugh")
    assert(textList.get(1).toString() == "xyzzy")
    assert(textList.get(2).toString() == "thud")


    val structList = reader.getStructList()
    assert(3 == structList.size())
    assert(structList.get(0).getTextField().toString() == "structlist 1")
    assert(structList.get(1).getTextField().toString() == "structlist 2")
    assert(structList.get(2).getTextField().toString() == "structlist 3")

    val enumList = reader.getEnumList()
    (enumList.get(0)) should equal (TestEnum.FOO)
    (enumList.get(1)) should equal (TestEnum.GARPLY)

  }

  def checkDefaultMessage(builder : TestDefaults.Builder) {
    builder.getVoidField()
    assert(builder.getBoolField() == true)
    assert(builder.getInt8Field() == -123)
    assert(builder.getInt16Field() == -12345)
    assert(builder.getInt32Field() == -12345678)
    assert(builder.getInt64Field() == -123456789012345L)
    assert(builder.getUInt8Field() == 0xea.toByte)
    assert(builder.getUInt16Field() == 45678.toShort)
    assert(builder.getUInt32Field() == 0xce0a6a14)
    assert(builder.getUInt64Field() == 0xab54a98ceb1f0ad2L)
    assert(builder.getFloat32Field() == 1234.5f)
    assert(builder.getFloat64Field() == -123e45)
    assert(builder.getEnumField() == TestEnum.CORGE)

    (builder.getTextField().toString()) should equal ("foo")
    (builder.getDataField().toArray()) should equal (Array(0x62,0x61,0x72))
  }

  def checkDefaultMessage(reader : TestDefaults.Reader) {
    reader.getVoidField()
    assert(reader.getBoolField() == true)
    assert(reader.getInt8Field() == -123)
    assert(reader.getInt16Field() == -12345)
    assert(reader.getInt32Field() == -12345678)
    assert(reader.getInt64Field() == -123456789012345L)
    assert(reader.getUInt8Field() == 0xea.toByte)
    assert(reader.getUInt16Field() == 45678.toShort)
    assert(reader.getUInt32Field() == 0xce0a6a14)
    assert(reader.getUInt64Field() == 0xab54a98ceb1f0ad2L)
    assert(reader.getFloat32Field() == 1234.5f)
    assert(reader.getFloat64Field() == -123e45)
    (reader.getTextField().toString()) should equal ("foo")
    (reader.getDataField().toArray()) should equal (Array(0x62,0x61,0x72))

    {
      val subReader = reader.getStructField()
      subReader.getVoidField()
      subReader.getBoolField() should equal (true)
      subReader.getInt8Field() should equal (-12)
      subReader.getInt16Field() should equal (3456)
      subReader.getInt32Field() should equal (-78901234)
      // ...
      subReader.getTextField().toString() should equal ("baz")

      {
        val subSubReader = subReader.getStructField()
        subSubReader.getTextField().toString() should equal ("nested")
      }

    }

    reader.getEnumField() should equal (TestEnum.CORGE)

    reader.getVoidList().size() should equal (6)

    {
      val listReader = reader.getBoolList()
      listReader.size() should equal (4)
      listReader.get(0) should equal (true)
      listReader.get(1) should equal (false)
      listReader.get(2) should equal (false)
      listReader.get(3) should equal (true)
    }

    {
      val listReader = reader.getInt8List()
      listReader.size() should equal (2)
      listReader.get(0) should equal (111)
      listReader.get(1) should equal (-111)
    }

  }


  def setDefaultMessage(builder : TestDefaults.Builder) {
    builder.setBoolField(false)
    builder.setInt8Field(-122)
    builder.setInt16Field(-12344)
    builder.setInt32Field(-12345677)
    builder.setInt64Field(-123456789012344L)
    builder.setUInt8Field(0xe9.toByte)
    builder.setUInt16Field(45677.toShort)
    builder.setUInt32Field(0xce0a6a13)
    builder.setUInt64Field(0xab54a98ceb1f0ad1L)
    builder.setFloat32Field(1234.4f)
    builder.setFloat64Field(-123e44)
    builder.setTextField(new Reader("bar"))
    builder.setEnumField(TestEnum.QUX)
  }

  def checkSettedDefaultMessage(reader : TestDefaults.Reader) {
    assert(reader.getBoolField() == false)
    assert(reader.getInt8Field() == -122)
    assert(reader.getInt16Field() == -12344)
    assert(reader.getInt32Field() == -12345677)
    assert(reader.getInt64Field() == -123456789012344L)
    assert(reader.getUInt8Field() == 0xe9.toByte)
    assert(reader.getUInt16Field() == 45677.toShort)
    assert(reader.getUInt32Field() == 0xce0a6a13)
    assert(reader.getUInt64Field() == 0xab54a98ceb1f0ad1L)
    assert(reader.getFloat32Field() == 1234.4f)
    assert(reader.getFloat64Field() == -123e44)
    assert(reader.getEnumField() == TestEnum.QUX)
  }
}
