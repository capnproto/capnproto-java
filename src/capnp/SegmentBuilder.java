package org.capnproto;

import java.nio.ByteBuffer;

public class SegmentBuilder extends SegmentReader {
    public int pos = 0;

    public SegmentBuilder(ByteBuffer buf) {
        super(buf);
    }

    private final int currentSize() {
        throw new Error("unimplemented");
    }

    public final int allocate(int amount) {
        throw new Error("unimplemented");
        //        if (amount > ... this.currentSize()
    }
}
