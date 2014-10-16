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

public final class ElementSize {
    public static final byte VOID = 0;
    public static final byte BIT = 1;
    public static final byte BYTE = 2;
    public static final byte TWO_BYTES = 3;
    public static final byte FOUR_BYTES = 4;
    public static final byte EIGHT_BYTES = 5;
    public static final byte POINTER = 6;
    public static final byte INLINE_COMPOSITE = 7;

    public static final int dataBitsPerElement(byte size) {
        switch (size) {
        case VOID: return 0;
        case BIT: return 1;
        case BYTE: return 8;
        case TWO_BYTES: return 16;
        case FOUR_BYTES: return 32;
        case EIGHT_BYTES: return 64;
        case POINTER: return 0;
        case INLINE_COMPOSITE: return 0;
        default : throw new Error("impossible field size: " + size);
        }
    }

    public static final short pointersPerElement(byte size) {
        switch (size) {
        case POINTER: return 1;
        default: return 0;
        }
    }
}
