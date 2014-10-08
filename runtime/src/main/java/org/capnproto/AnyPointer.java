package org.capnproto;

public final class AnyPointer {

    public final static class Reader {
        final SegmentReader segment;
        final int pointer;
        final int nestingLimit;

        public Reader(SegmentReader segment, int pointer, int nestingLimit) {
            this.segment = segment;
            this.pointer = pointer;
            this.nestingLimit = nestingLimit;
        }

        public final <T> T getAs(FromPointerReader<T> factory) {
            return factory.fromPointerReader(this.segment, this.pointer, this.nestingLimit);
        }
    }

    public static final class Builder {
        final SegmentBuilder segment;
        final int pointer;

        public Builder(SegmentBuilder segment, int pointer) {
            this.segment = segment;
            this.pointer = pointer;
        }

        public final <T> T initAs(InitFromPointerBuilder<T> factory) {
            return factory.initFromPointerBuilder(this.segment, this.pointer);
        }

        public final <T> T initAs(InitSizedFromPointerBuilder<T> factory, int elementCount) {
            return factory.initSizedFromPointerBuilder(this.segment, this.pointer, elementCount);
        }

        public final void clear() {
            WireHelpers.zeroObject(this.segment, this.pointer);
            this.segment.buffer.putLong(this.pointer * 8, 0L);
        }
    }

}
