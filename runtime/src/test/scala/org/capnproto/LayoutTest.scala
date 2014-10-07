package org.capnproto;

import org.scalatest.FunSuite

class LayoutSuite extends FunSuite {

  test("SimpleRawDataStruct") {
    val data : Array[Byte] =
      Array(0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00,
            0x01, 0x23, 0x45, 0x67, 0x89.toByte, 0xab.toByte,
            0xcd.toByte, 0xef.toByte);

    val buffer = java.nio.ByteBuffer.wrap(data);
    buffer.order(java.nio.ByteOrder.LITTLE_ENDIAN);

    val arena = new ReaderArena(Array(buffer));
    val pointerReader = new PointerReader(arena.tryGetSegment(0), 0, 0x7fffffff);
    val reader = pointerReader.getStruct();

    assert(reader._getLongField(0) === 0xefcdab8967452301L);
    assert(reader._getLongField(1) === 0L);

    assert(reader.getIntField(0) === 0x67452301);
    assert(reader.getIntField(1) === 0xefcdab89);
    assert(reader.getIntField(2) === 0);
    assert(reader.getShortField(0) === 0x2301.toShort);
    assert(reader.getShortField(1) === 0x6745.toShort);
    assert(reader.getShortField(2) === 0xab89.toShort);
    assert(reader.getShortField(3) === 0xefcd.toShort);
    assert(reader.getShortField(4) === 0.toShort);

    // TODO masking

    assert(reader.getBooleanField(0) === true);
    assert(reader.getBooleanField(1) === false);
    assert(reader.getBooleanField(2) === false);

    assert(reader.getBooleanField(3) === false);
    assert(reader.getBooleanField(4) === false);
    assert(reader.getBooleanField(5) === false);
    assert(reader.getBooleanField(6) === false);
    assert(reader.getBooleanField(7) == false);

    assert(reader.getBooleanField(8) === true);
    assert(reader.getBooleanField(9) === true);
    assert(reader.getBooleanField(10) === false);
    assert(reader.getBooleanField(11) === false);
    assert(reader.getBooleanField(12) === false);
    assert(reader.getBooleanField(13) === true);
    assert(reader.getBooleanField(14) === false);
    assert(reader.getBooleanField(15) === false);

    assert(reader.getBooleanField(63) === true);
    assert(reader.getBooleanField(64) === false);

    // TODO masking

  }

  def setupStruct(builder : StructBuilder) = {
    builder.setLongField(0, 0x1011121314151617L);
    builder.setIntField(2, 0x20212223);
    builder.setShortField(6, 0x3031.toShort);
    builder.setByteField(14, 0x40);
    builder.setBooleanField(120, false);
    builder.setBooleanField(121, false);
    builder.setBooleanField(122, true);
    builder.setBooleanField(123, false);
    builder.setBooleanField(124, true);
    builder.setBooleanField(125, true);
    builder.setBooleanField(126, true);
    builder.setBooleanField(127, false);
  }

  def checkStruct(builder : StructBuilder) {
    assert(0x1011121314151617L === builder.getLongField(0));
    assert(0x20212223 == builder.getIntField(2));
    assert(0x3031 === builder.getShortField(6));
    assert(0x40 === builder.getByteField(14));
    assert(false === builder.getBooleanField(120));
    assert(false === builder.getBooleanField(121));
    assert(true === builder.getBooleanField(122));
    assert(false === builder.getBooleanField(123));
    assert(true === builder.getBooleanField(124));
    assert(true === builder.getBooleanField(125));
    assert(true === builder.getBooleanField(126));
    assert(false === builder.getBooleanField(127));
  }

  test("StructRoundTrip_OneSegment") {
    val buffer = java.nio.ByteBuffer.allocate(1024 * 8);
    buffer.order(java.nio.ByteOrder.LITTLE_ENDIAN);

    val pointerBuilder = PointerBuilder.getRoot(
      new SegmentBuilder(buffer, new BuilderArena(BuilderArena.SUGGESTED_FIRST_SEGMENT_WORDS,
                                                  BuilderArena.SUGGESTED_ALLOCATION_STRATEGY)),
      0);
    val builder = pointerBuilder.initStruct(new StructSize(2, 4, FieldSize.INLINE_COMPOSITE));
    setupStruct(builder);

    checkStruct(builder);
  }

}

