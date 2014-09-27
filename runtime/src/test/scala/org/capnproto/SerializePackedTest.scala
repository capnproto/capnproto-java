package org.capnproto;

import org.scalatest.FunSuite;
import org.scalatest.Matchers._;
import java.nio.ByteBuffer;

class SerializePackedSuite extends FunSuite {

  def expectPacksTo(unpacked : Array[Byte], packed : Array[Byte]) {
    // ----
    // write
    {
      val bytes = new Array[Byte](packed.length);
      val writer = new ArrayOutputStream(ByteBuffer.wrap(bytes));
      val packedOutputStream = new PackedOutputStream(writer);
      packedOutputStream.write(ByteBuffer.wrap(unpacked));

      (bytes) should equal (packed);
    }

    // ------
    // read
    {
      val reader = new ArrayInputStream(ByteBuffer.wrap(packed));
      val packedInputStream = new PackedInputStream(reader);
      val bytes = new Array[Byte](unpacked.length);
      val n = packedInputStream.read(ByteBuffer.wrap(bytes));

      //(n) should equal (unpacked.length);

      //(bytes) should equal (unpacked);
    }


  }

  test("SimplePacking") {
    expectPacksTo(Array(), Array());
    expectPacksTo(Array(0,0,12,0,0,34,0,0), Array(0x24,12,34));
    expectPacksTo(Array(1,3,2,4,5,7,6,8), Array(0xff.toByte,1,3,2,4,5,7,6,8,0));
    expectPacksTo(Array(0,0,0,0,0,0,0,0, 1,3,2,4,5,7,6,8),
                  Array(0,0,0xff.toByte,1,3,2,4,5,7,6,8,0));
    expectPacksTo(Array(0,0,12,0,0,34,0,0, 1,3,2,4,5,7,6,8),
                  Array(0x24, 12, 34, 0xff.toByte,1,3,2,4,5,7,6,8,0));
    expectPacksTo(Array(1,3,2,4,5,7,6,8, 8,6,7,4,5,2,3,1),
                  Array(0xff.toByte,1,3,2,4,5,7,6,8,1,8,6,7,4,5,2,3,1));

    expectPacksTo(Array(1,2,3,4,5,6,7,8, 1,2,3,4,5,6,7,8, 1,2,3,4,5,6,7,8, 1,2,3,4,5,6,7,8, 0,2,4,0,9,0,5,1),
                  Array(0xff.toByte,1,2,3,4,5,6,7,8, 3, 1,2,3,4,5,6,7,8, 1,2,3,4,5,6,7,8, 1,2,3,4,5,6,7,8,
                        0xd6.toByte,2,4,9,5,1));

    expectPacksTo(Array(1,2,3,4,5,6,7,8, 1,2,3,4,5,6,7,8, 6,2,4,3,9,0,5,1, 1,2,3,4,5,6,7,8, 0,2,4,0,9,0,5,1),
                  Array(0xff.toByte,1,2,3,4,5,6,7,8, 3, 1,2,3,4,5,6,7,8, 6,2,4,3,9,0,5,1, 1,2,3,4,5,6,7,8,
                        0xd6.toByte,2,4,9,5,1));

    expectPacksTo(Array(8,0,100,6,0,1,1,2, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,1,0,2,0,3,1),
                  Array(0xed.toByte,8,100,6,1,1,2, 0,2, 0xd4.toByte,1,2,3,1));
  }
}
