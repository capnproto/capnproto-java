package org.capnproto;

import java.nio.ByteBuffer;

public class SegmentBuilder extends SegmentReader {
    public int pos = 0; // in words

    public static final int FAILED_ALLOCATION = -1;

    public SegmentBuilder(ByteBuffer buf) {
        super(buf);
    }

    // the total number of words the buffer can hold
    private final int capacity() {
        this.buffer.reset();
        return (this.buffer.limit() - this.buffer.position()) / 8;
    }

    // return how many words have already been allocated
    private final int currentSize() {
        return this.pos;
    }

    public final int allocate(int amount) {
        if (amount > this.capacity() - this.currentSize()) {
            return FAILED_ALLOCATION; // no space left;
        } else {
            int result = this.pos;
            this.pos += amount;
            return result;
        }
    }
}
