package org.capnproto;

import java.nio.ByteBuffer;

final class ListPointer {
    public static byte elementSize(long ref) {
        return (byte) (WirePointer.upper32Bits(ref) & 7);
    }

    public static int elementCount(long ref) {
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
                      (wordCount << 3) | FieldSize.INLINE_COMPOSITE);
    }
}
