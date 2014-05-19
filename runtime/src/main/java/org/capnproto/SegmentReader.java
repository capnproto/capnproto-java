package org.capnproto;

import java.nio.ByteBuffer;

public class SegmentReader {
    final ByteBuffer buffer;

    public SegmentReader(ByteBuffer buffer) {
        this.buffer = buffer;
    }
}
