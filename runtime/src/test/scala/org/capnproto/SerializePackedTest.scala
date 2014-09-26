package org.capnproto;

import org.scalatest.FunSuite;
import org.scalatest.Matchers._;
import java.nio.ByteBuffer;

class SerializePackedSuite extends FunSuite {

  def expectPacksTo(unpacked : Array[Byte], packed : Array[Byte]) {
    // ----
    // write

    val bytes = new Array[Byte](packed.length);
    val writer = new ArrayOutputStream(ByteBuffer.wrap(bytes));
    val packedOutputStream = new PackedOutputStream (writer);
    packedOutputStream.write(ByteBuffer.wrap(unpacked));

    (bytes) should equal (packed);

  }

  test("SimplePacking") {
    expectPacksTo(Array(), Array());
    //expectPacksTo(Array(0,0,12,0,0,34,0,0), Array(0x24,12,34));
  }
}
