package org.capnproto;

import org.scalatest.FunSuite


class LayoutSuite extends FunSuite {

  class BareStructReader extends StructReader.Factory[StructReader] {
    def constructReader(segment: org.capnproto.SegmentReader, data: Int, pointers: Int,
                        dataSize: Int, pointerCount: Short, bit0Offset: Byte, nestingLimit:Int) : StructReader = {
      return new StructReader(segment,data,pointers,dataSize,pointerCount,bit0Offset,nestingLimit);
    }
  }

  test("SimpleRawDataStruct") {
    val data : Array[Byte] =
      Array(0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00,
            0x01, 0x23, 0x45, 0x67, 0x89.toByte, 0xab.toByte,
            0xcd.toByte, 0xef.toByte);

    val buffer = java.nio.ByteBuffer.wrap(data);
    buffer.order(java.nio.ByteOrder.LITTLE_ENDIAN);

    val arena = new ReaderArena(Array(buffer));

    val reader = WireHelpers.readStructPointer(new BareStructReader(),
                                               arena.tryGetSegment(0),
                                               0, null, 0, 0x7fffffff);

    assert(reader._getLongField(0) === 0xefcdab8967452301L);
    assert(reader._getLongField(1) === 0L);

    assert(reader._getIntField(0) === 0x67452301);
    assert(reader._getIntField(1) === 0xefcdab89);
    assert(reader._getIntField(2) === 0);
    assert(reader._getShortField(0) === 0x2301.toShort);
    assert(reader._getShortField(1) === 0x6745.toShort);
    assert(reader._getShortField(2) === 0xab89.toShort);
    assert(reader._getShortField(3) === 0xefcd.toShort);
    assert(reader._getShortField(4) === 0.toShort);

    // TODO masking

    assert(reader._getBooleanField(0) === true);
    assert(reader._getBooleanField(1) === false);
    assert(reader._getBooleanField(2) === false);

    assert(reader._getBooleanField(3) === false);
    assert(reader._getBooleanField(4) === false);
    assert(reader._getBooleanField(5) === false);
    assert(reader._getBooleanField(6) === false);
    assert(reader._getBooleanField(7) == false);

    assert(reader._getBooleanField(8) === true);
    assert(reader._getBooleanField(9) === true);
    assert(reader._getBooleanField(10) === false);
    assert(reader._getBooleanField(11) === false);
    assert(reader._getBooleanField(12) === false);
    assert(reader._getBooleanField(13) === true);
    assert(reader._getBooleanField(14) === false);
    assert(reader._getBooleanField(15) === false);

    assert(reader._getBooleanField(63) === true);
    assert(reader._getBooleanField(64) === false);

    // TODO masking

  }

  def setupStruct(builder : StructBuilder) = {
    builder._setLongField(0, 0x1011121314151617L);
    builder._setIntField(2, 0x20212223);
    builder._setShortField(6, 0x3031.toShort);
    builder._setByteField(14, 0x40);
    builder._setBooleanField(120, false);
    builder._setBooleanField(121, false);
    builder._setBooleanField(122, true);
    builder._setBooleanField(123, false);
    builder._setBooleanField(124, true);
    builder._setBooleanField(125, true);
    builder._setBooleanField(126, true);
    builder._setBooleanField(127, false);
  }

  def checkStruct(builder : StructBuilder) {
    assert(0x1011121314151617L === builder._getLongField(0));
    assert(0x20212223 == builder._getIntField(2));
    assert(0x3031 === builder._getShortField(6));
    assert(0x40 === builder._getByteField(14));
    assert(false === builder._getBooleanField(120));
    assert(false === builder._getBooleanField(121));
    assert(true === builder._getBooleanField(122));
    assert(false === builder._getBooleanField(123));
    assert(true === builder._getBooleanField(124));
    assert(true === builder._getBooleanField(125));
    assert(true === builder._getBooleanField(126));
    assert(false === builder._getBooleanField(127));
  }


  class BareStructBuilder(structSize : StructSize) extends StructBuilder.Factory[StructBuilder] {

    def constructBuilder(segment: org.capnproto.SegmentBuilder, data: Int, pointers: Int,
                          dataSize: Int, pointerCount: Short, bit0Offset: Byte) : StructBuilder = {
      return new StructBuilder(segment,data,pointers,dataSize,pointerCount,bit0Offset);
    }
    def structSize() : StructSize = {
      return structSize;
    }
  }


  test("StructRoundTrip_OneSegment") {
    val buffer = java.nio.ByteBuffer.allocate(1024 * 8);
    buffer.order(java.nio.ByteOrder.LITTLE_ENDIAN);

    val pointerBuilder = PointerBuilder.getRoot(
      new SegmentBuilder(buffer, new BuilderArena(BuilderArena.SUGGESTED_FIRST_SEGMENT_WORDS,
                                                  BuilderArena.SUGGESTED_ALLOCATION_STRATEGY)),
      0);
    val factory = new BareStructBuilder(new StructSize(2, 4, FieldSize.INLINE_COMPOSITE));
    val builder =  WireHelpers.initStructPointer(factory, pointerBuilder.pointer, pointerBuilder.segment, factory.structSize());
    setupStruct(builder);
    checkStruct(builder);
  }

}

