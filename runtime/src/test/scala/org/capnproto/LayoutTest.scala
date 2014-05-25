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

  test("StructRoundTrip_OneSegment") {
    val message = new MessageBuilder();
    // ...
  }

}

