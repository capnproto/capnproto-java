package org.capnproto;

import java.nio.ByteBuffer;

public class SegmentReader {

    // invariant: buffer's mark is at its beginning.
    final ByteBuffer buffer;

    public SegmentReader(ByteBuffer buffer) {
        this.buffer = buffer;
    }
}
