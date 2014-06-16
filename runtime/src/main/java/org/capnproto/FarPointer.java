package org.capnproto;

import java.nio.ByteBuffer;

final class FarPointer {
    public static void set(ByteBuffer buffer, int offset, int segmentId) {
        buffer.putInt(8 * offset + 4, segmentId);
    }
}
