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

package org.capnproto;

import java.nio.ByteBuffer;

final class ListPointer {
    public static byte elementSize(long ref) {
        return (byte) (WirePointer.upper32Bits(ref) & 7);
    }

    public static int elementCount(long ref) {
        /* The [ref] List Size (the "D" section) is not specified
           as Signed or Unsigned, but number of elements is inherently non-negative.
           So use unsigned >>> operator. */
        return WirePointer.upper32Bits(ref) >>> 3;
    }

    public static int inlineCompositeWordCount(long ref) {
        return elementCount(ref);
    }

    public static void set(ByteBuffer buffer, int offset, byte elementSize, int elementCount) {
        // TODO length assertion
        buffer.putInt(8 * offset + 4,
                      (elementCount << 3) | elementSize);
    }

    public static void setInlineComposite(ByteBuffer buffer, int offset, int wordCount) {
        // TODO length assertion
        buffer.putInt(8 * offset + 4,
                      (wordCount << 3) | ElementSize.INLINE_COMPOSITE);
    }
}
