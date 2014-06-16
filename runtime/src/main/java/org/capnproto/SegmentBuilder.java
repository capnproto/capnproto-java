package org.capnproto;

import java.nio.ByteBuffer;

public final class SegmentBuilder extends SegmentReader {

    public static final int FAILED_ALLOCATION = -1;

    public int pos = 0; // in words
    public int id = 0;

    public SegmentBuilder(ByteBuffer buf, Arena arena) {
        super(buf, arena);
    }

    // the total number of words the buffer can hold
    private final int capacity() {
        this.buffer.reset();
        return this.buffer.remaining() / 8;
    }

    // return how many words have already been allocated
    public final int currentSize() {
        return this.pos;
    }

    /*
       Allocate `amount` words.
     */
    public final int allocate(int amount) {
        if (amount < 0) {
            throw new InternalError("tried to allocate a negative number of words");
        }

        if (amount > this.capacity() - this.currentSize()) {
            return FAILED_ALLOCATION; // no space left;
        } else {
            int result = this.pos;
            this.pos += amount;
            return result;
        }
    }

    public final BuilderArena getArena() {
        return (BuilderArena)this.arena;
    }
}
