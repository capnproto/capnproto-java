package org.capnproto;

import org.capnproto.test.Test._;



object TestUtil {
  def initTestMessage(builder : TestAllTypes.Builder) {
    builder.setVoidField(org.capnproto.Void.VOID);
    builder.setBoolField(true);
    builder.setInt8Field(-123);
    builder.setInt16Field(-12345);
    builder.setInt32Field(-12345678);
    builder.setInt64Field(-123456789012345L);
    builder.setUInt8Field(0xef.toByte);
    builder.setUInt16Field(0x4567);
    builder.setUInt32Field(0x34567890);
    builder.setUInt64Field(0x1234567890123456L);
    builder.setFloat32Field(1234.5f);
    builder.setFloat64Field(-123e45);
    builder.setTextField(new Text.Reader("foo"));
//    builder.setDataField(data("bar"));
    {

      val subBuilder = builder.initStructField();
      subBuilder.setVoidField(org.capnproto.Void.VOID);
      subBuilder.setBoolField(true);
      subBuilder.setInt8Field(-12);
      subBuilder.setInt16Field(3456);
      subBuilder.setInt32Field(-78901234);
      subBuilder.setInt64Field(56789012345678L);
      subBuilder.setUInt8Field(90);
      subBuilder.setUInt16Field(1234);
      subBuilder.setUInt32Field(56789012);
      subBuilder.setUInt64Field(345678901234567890L);
      subBuilder.setFloat32Field(-1.25e-10f);
      subBuilder.setFloat64Field(345);
      subBuilder.setTextField(new Text.Reader("baz"));

      {
        val subSubBuilder = subBuilder.initStructField();
        subSubBuilder.setTextField(new Text.Reader("nested"));
        subSubBuilder.initStructField().setTextField(new Text.Reader("really nested"));
      }
    }

    builder.setEnumField(TestEnum.CORGE);
    builder.initVoidList(6);

    val textList = builder.initTextList(3);
    textList.set(0, new Text.Reader("plugh"));
    textList.set(1, new Text.Reader("xyzzy"));
    textList.set(2, new Text.Reader("thud"));

  }


  def checkTestMessage(builder : TestAllTypes.Builder) {
    builder.getVoidField();
    assert(builder.getBoolField() == true);
    assert(builder.getInt8Field() == -123);
    assert(builder.getInt16Field() == -12345);
    assert(builder.getInt32Field() == -12345678);
    assert(builder.getInt64Field() == -123456789012345L);
    assert(builder.getUInt8Field() == 0xef.toByte);
    assert(builder.getUInt16Field() == 0x4567);
    assert(builder.getUInt32Field() == 0x34567890);
    assert(builder.getUInt64Field() == 0x1234567890123456L);
    assert(builder.getFloat32Field() == 1234.5f);
    assert(builder.getFloat64Field() == -123e45);
    assert(builder.getTextField().toString() == "foo");

    {
      val subBuilder = builder.getStructField();
      subBuilder.getVoidField();
      assert(subBuilder.getBoolField() == true);
      assert(subBuilder.getInt8Field() == -12);
      assert(subBuilder.getInt16Field() == 3456);
      assert(subBuilder.getInt32Field() == -78901234);
      assert(subBuilder.getInt64Field() == 56789012345678L);
      assert(subBuilder.getUInt8Field() == 90);
      assert(subBuilder.getUInt16Field() == 1234);
      assert(subBuilder.getUInt32Field() == 56789012);
      assert(subBuilder.getUInt64Field() == 345678901234567890L);
      assert(subBuilder.getFloat32Field() == -1.25e-10f);
      assert(subBuilder.getFloat64Field() == 345);

      {
        val subSubBuilder = subBuilder.getStructField();
        assert(subSubBuilder.getTextField().toString() == "nested")
      }
    }

    assert(builder.getVoidList().size() == 6);

    val textList = builder.getTextList();
    assert(textList.size() == 3);
    assert(textList.get(0).toString() == "plugh");
    assert(textList.get(1).toString() == "xyzzy");
    assert(textList.get(2).toString() == "thud");
  }

  def checkTestMessage(reader : TestAllTypes.Reader) {
    reader.getVoidField();
    assert(reader.getBoolField() == true);
    assert(reader.getInt8Field() == -123);
    assert(reader.getInt16Field() == -12345);
    assert(reader.getInt32Field() == -12345678);
    assert(reader.getInt64Field() == -123456789012345L);
    assert(reader.getUInt8Field() == 0xef.toByte);
    assert(reader.getUInt16Field() == 0x4567);
    assert(reader.getUInt32Field() == 0x34567890);
    assert(reader.getUInt64Field() == 0x1234567890123456L);
    assert(reader.getFloat32Field() == 1234.5f);
    assert(reader.getFloat64Field() == -123e45);
    assert(reader.getTextField().toString() == "foo");

    {
      val subReader = reader.getStructField();
      subReader.getVoidField();
      assert(subReader.getBoolField() == true);
      assert(subReader.getInt8Field() == -12);
      assert(subReader.getInt16Field() == 3456);
      assert(subReader.getInt32Field() == -78901234);
      assert(subReader.getInt64Field() == 56789012345678L);
      assert(subReader.getUInt8Field() == 90);
      assert(subReader.getUInt16Field() == 1234);
      assert(subReader.getUInt32Field() == 56789012);
      assert(subReader.getUInt64Field() == 345678901234567890L);
      assert(subReader.getFloat32Field() == -1.25e-10f);
      assert(subReader.getFloat64Field() == 345);

      {
        val subSubReader = subReader.getStructField();
        assert(subSubReader.getTextField().toString() == "nested")
      }
    }

    assert(reader.getVoidList().size() == 6);

    val textList = reader.getTextList();
    assert(textList.size() == 3);
    assert(textList.get(0).toString() == "plugh");
    assert(textList.get(1).toString() == "xyzzy");
    assert(textList.get(2).toString() == "thud");

  }


}
