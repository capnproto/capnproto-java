package org.capnproto;

import java.nio.ByteBuffer;

public final class MessageReader {
    final ReaderArena arena;

    public MessageReader(ByteBuffer[] segmentSlices) {
        this.arena = new ReaderArena(segmentSlices);
    }

    public <T> T getRoot(FromPointerReader<T> factory) {
        SegmentReader segment = this.arena.tryGetSegment(0);
        PointerReader pointerReader = PointerReader.getRoot(segment, 0,
                                                            0x7fffffff /* XXX */);
        AnyPointer.Reader any = new AnyPointer.Reader(pointerReader);
        return any.getAs(factory);
    }
}
