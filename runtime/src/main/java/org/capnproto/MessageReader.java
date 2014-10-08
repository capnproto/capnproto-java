package org.capnproto;

import java.nio.ByteBuffer;

public final class MessageReader {
    final ReaderArena arena;
    final int nestingLimit;

    final static long DEFAULT_TRAVERSAL_LIMIT_IN_WORDS = 8 * 1024 * 1024;
    final static int DEFAULT_NESTING_LIMIT = 64;

    public MessageReader(ByteBuffer[] segmentSlices, long traversalLimitInWords, int nestingLimit) {
        this.nestingLimit = nestingLimit;
        this.arena = new ReaderArena(segmentSlices, traversalLimitInWords);
    }

    public <T> T getRoot(FromPointerReader<T> factory) {
        SegmentReader segment = this.arena.tryGetSegment(0);
        AnyPointer.Reader any = new AnyPointer.Reader(segment, 0, this.nestingLimit);
        return any.getAs(factory);
    }
}
