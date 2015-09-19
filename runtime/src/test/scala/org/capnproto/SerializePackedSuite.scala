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

import org.scalatest.FunSuite
import org.scalatest.Matchers._
import java.nio.ByteBuffer

class SerializePackedSuite extends FunSuite {

  def expectPacksTo(unpacked : Array[Byte], packed : Array[Byte]) {
    // ----
    // write
    {
      val bytes = new Array[Byte](packed.length)
      val writer = new ArrayOutputStream(ByteBuffer.wrap(bytes))
      val packedOutputStream = new PackedOutputStream(writer)
      packedOutputStream.write(ByteBuffer.wrap(unpacked))

      (bytes) should equal (packed)
    }

    // ------
    // read
    {
      val reader = new ArrayInputStream(ByteBuffer.wrap(packed))
      val packedInputStream = new PackedInputStream(reader)
      val bytes = new Array[Byte](unpacked.length)
      val n = packedInputStream.read(ByteBuffer.wrap(bytes))

      (n) should equal (unpacked.length)

      (bytes) should equal (unpacked)
    }
  }

  test("SimplePacking") {
    expectPacksTo(Array(), Array())
    expectPacksTo(Array(0,0,0,0,0,0,0,0), Array(0,0))
    expectPacksTo(Array(0,0,12,0,0,34,0,0), Array(0x24,12,34))
    expectPacksTo(Array(1,3,2,4,5,7,6,8), Array(0xff.toByte,1,3,2,4,5,7,6,8,0))
    expectPacksTo(Array(0,0,0,0,0,0,0,0, 1,3,2,4,5,7,6,8),
                  Array(0,0,0xff.toByte,1,3,2,4,5,7,6,8,0))
    expectPacksTo(Array(0,0,12,0,0,34,0,0, 1,3,2,4,5,7,6,8),
                  Array(0x24, 12, 34, 0xff.toByte,1,3,2,4,5,7,6,8,0))
    expectPacksTo(Array(1,3,2,4,5,7,6,8, 8,6,7,4,5,2,3,1),
                  Array(0xff.toByte,1,3,2,4,5,7,6,8,1,8,6,7,4,5,2,3,1))

    expectPacksTo(Array(1,2,3,4,5,6,7,8, 1,2,3,4,5,6,7,8, 1,2,3,4,5,6,7,8, 1,2,3,4,5,6,7,8, 0,2,4,0,9,0,5,1),
                  Array(0xff.toByte,1,2,3,4,5,6,7,8, 3, 1,2,3,4,5,6,7,8, 1,2,3,4,5,6,7,8, 1,2,3,4,5,6,7,8,
                        0xd6.toByte,2,4,9,5,1))

    expectPacksTo(Array(1,2,3,4,5,6,7,8, 1,2,3,4,5,6,7,8, 6,2,4,3,9,0,5,1, 1,2,3,4,5,6,7,8, 0,2,4,0,9,0,5,1),
                  Array(0xff.toByte,1,2,3,4,5,6,7,8, 3, 1,2,3,4,5,6,7,8, 6,2,4,3,9,0,5,1, 1,2,3,4,5,6,7,8,
                        0xd6.toByte,2,4,9,5,1))

    expectPacksTo(Array(8,0,100,6,0,1,1,2, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,1,0,2,0,3,1),
                  Array(0xed.toByte,8,100,6,1,1,2, 0,2, 0xd4.toByte,1,2,3,1))

    expectPacksTo(Array(0,0,0,0,2,0,0,0, 0,0,0,0,0,0,1,0, 0,0,0,0,0,0,0,0),
                  Array(0x10,2, 0x40,1, 0,0))

    expectPacksTo(Array.tabulate[Byte](8 * 200)((n) => 0),
                  Array(0, 199.toByte))

    expectPacksTo(Array.tabulate[Byte](8 * 200)((n) => 1),
                  Array.concat(Array(0xff.toByte, 1,1,1,1,1,1,1,1, 199.toByte),
                               Array.tabulate[Byte](8 * 199)((n) => 1)))
  }
}
