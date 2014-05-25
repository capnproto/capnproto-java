package org.capnproto;

import org.scalatest.FunSuite

class ExampleSuite extends FunSuite {

  test("SimpleRawDataStruct") {
    val data : Array[Byte] =
      Array(0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00,
            0x01, 0x23, 0x45, 0x67, 0x89.toByte, 0xab.toByte,
            0xcd.toByte, 0xef.toByte);

    val buffer = java.nio.ByteBuffer.wrap(data);
    buffer.order(java.nio.ByteOrder.LITTLE_ENDIAN);

    val pointerReader = new PointerReader(new SegmentReader(buffer), 0, 0x7fffffff);
    val reader = pointerReader.getStruct();

    assert(reader.getLongField(0) === 0xefcdab8967452301L);
    assert(reader.getLongField(1) === 0L);

    assert(reader.getIntField(0) === 0x67452301);
    assert(reader.getIntField(1) === 0xefcdab89);
    assert(reader.getIntField(2) === 0);
    assert(reader.getShortField(0) === 0x2301.toShort);
    assert(reader.getShortField(1) === 0x6745.toShort);
    assert(reader.getShortField(2) === 0xab89.toShort);
    assert(reader.getShortField(3) === 0xefcd.toShort);
    assert(reader.getShortField(4) === 0.toShort);

    // TODO masking

    assert(reader.getBoolField(0) === true);
    assert(reader.getBoolField(1) === false);
    assert(reader.getBoolField(2) === false);

    assert(reader.getBoolField(3) === false);
    assert(reader.getBoolField(4) === false);
    assert(reader.getBoolField(5) === false);
    assert(reader.getBoolField(6) === false);
    assert(reader.getBoolField(7) == false);

    assert(reader.getBoolField(8) === true);
    assert(reader.getBoolField(9) === true);
    assert(reader.getBoolField(10) === false);
    assert(reader.getBoolField(11) === false);
    assert(reader.getBoolField(12) === false);
    assert(reader.getBoolField(13) === true);
    assert(reader.getBoolField(14) === false);
    assert(reader.getBoolField(15) === false);

    assert(reader.getBoolField(63) === true);
    assert(reader.getBoolField(64) === false);

    // TODO masking

  }

  def setupStruct(builder : StructBuilder) = {
    builder.setLongField(0, 0x1011121314151617L);
    builder.setIntField(2, 0x20212223);
    builder.setShortField(6, 0x3031.toShort);
    builder.setByteField(14, 0x40);
    builder.setBoolField(120, false);
    builder.setBoolField(121, false);
    builder.setBoolField(122, true);
    builder.setBoolField(123, false);
    builder.setBoolField(124, true);
    builder.setBoolField(125, true);
    builder.setBoolField(126, true);
    builder.setBoolField(127, false);
  }

  def checkStruct(builder : StructBuilder) {
    assert(0x1011121314151617L === builder.getLongField(0));
    assert(0x20212223 == builder.getIntField(2));
    assert(0x3031 === builder.getShortField(6));
    assert(0x40 === builder.getByteField(14));
    assert(false === builder.getBoolField(120));
    assert(false === builder.getBoolField(121));
    assert(true === builder.getBoolField(122));
    assert(false === builder.getBoolField(123));
    assert(true === builder.getBoolField(124));
    assert(true === builder.getBoolField(125));
    assert(true === builder.getBoolField(126));
    assert(false === builder.getBoolField(127));
  }

  test("StructRoundTrip_OneSegment") {
    val buffer = java.nio.ByteBuffer.allocate(1024 * 8);
    buffer.order(java.nio.ByteOrder.LITTLE_ENDIAN);
    buffer.mark();

    val pointerBuilder = PointerBuilder.getRoot(new SegmentBuilder(buffer), 0);
    val builder = pointerBuilder.initStruct(new StructSize(2, 4, FieldSize.INLINE_COMPOSITE));
    setupStruct(builder);

    checkStruct(builder);
  }

}

