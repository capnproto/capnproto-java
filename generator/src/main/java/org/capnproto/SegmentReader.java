package org.capnproto;

import java.nio.ByteBuffer;

public class SegmentReader {
    ByteBuffer buffer;

    public SegmentReader(ByteBuffer buffer) {
        this.buffer = buffer;
    }
}
