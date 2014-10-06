package org.capnproto;

import java.nio.ByteBuffer;

public class SegmentReader {

    public final ByteBuffer buffer;
    final Arena arena;

    public SegmentReader(ByteBuffer buffer, Arena arena) {
        this.buffer = buffer;
        this.arena = arena;
    }

    public static final SegmentReader EMPTY = new SegmentReader(ByteBuffer.allocate(8), null);
}
