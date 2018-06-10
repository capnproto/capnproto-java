// Copyright (c) 2018 Sandstorm Development Group, Inc. and contributors
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

import java.nio.ByteBuffer
import org.scalatest.FunSuite
import org.scalatest.Matchers._

class ArrayInputStreamSuite extends FunSuite {
  test("EmptyArray") {
    val stream = new ArrayInputStream(ByteBuffer.allocate(0))
    val dst = ByteBuffer.allocate(10)

    // read() should return -1 at the end of the stream
    // https://docs.oracle.com/javase/7/docs/api/java/nio/channels/ReadableByteChannel.html
    stream.read(dst) should equal (-1)
  }

  test("Request more bytes than are present") {
    val oneByte: Array[Byte] = Array(42)
    val stream = new ArrayInputStream(ByteBuffer.wrap(oneByte))
    val dst = ByteBuffer.allocate(10)
    stream.read(dst) should equal (1)
    stream.read(dst) should equal (-1) // end of stream
  }
}
