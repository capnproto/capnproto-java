package org.capnproto;

import java.nio.ByteBuffer;

final class ListPointer {
    public static byte elementSize(int elementSizeAndCount) {
        return (byte) (elementSizeAndCount & 7);
    }

    public static int elementCount(int elementSizeAndCount) {
        return elementSizeAndCount >> 3;
    }

    public static int inlineCompositeWordCount(int elementSizeAndCount) {
        return elementCount(elementSizeAndCount);
    }

    public static void setInlineComposite(ByteBuffer buffer, int offset, int wordCount) {
        buffer.putInt(8 * offset + 4,
                      (wordCount << 3) | FieldSize.INLINE_COMPOSITE);
    }
}
