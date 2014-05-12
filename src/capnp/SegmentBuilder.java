package org.capnproto;

import java.nio.ByteBuffer;

public class SegmentBuilder extends SegmentReader {
    public SegmentBuilder(ByteBuffer buf) {
        super(buf);
    }
}
