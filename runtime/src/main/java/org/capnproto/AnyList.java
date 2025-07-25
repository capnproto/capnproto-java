package org.capnproto;

public class AnyList {

    public static final class Factory extends ListFactory<Builder, Reader> {

        Factory() {
            super(ElementSize.VOID);
        }

        public final Reader asReader(Builder builder) {
            return builder.asReader();
        }

        @Override
        public Builder constructBuilder(SegmentBuilder segment, int ptr, int elementCount, int step, int structDataSize, short structPointerCount) {
            return new Builder(segment, ptr, elementCount, step, structDataSize, structPointerCount);
        }

        @Override
        public Reader constructReader(SegmentReader segment, int ptr, int elementCount, int step, int structDataSize, short structPointerCount, int nestingLimit) {
            return new Reader(segment, ptr, elementCount, step, structDataSize, structPointerCount, nestingLimit);
        }
    }

    public static final Factory factory = new Factory();

    public static final ListList.Factory<Builder,Reader> listFactory =
            new ListList.Factory<>(factory);

    public static final class Builder extends ListBuilder {
        Builder(SegmentBuilder segment, int ptr, int elementCount, int step, int structDataSize, short structPointerCount){
            super(segment, ptr, elementCount, step, structDataSize, structPointerCount);
        }

        public final Reader asReader() {
            return new Reader(this.segment, this.ptr, this.elementCount, this.step, this.structDataSize, this.structPointerCount, 0x7fffffff);
        }

        public final <T> T initAs(Factory<T> factory) {
            return factory.constructBuilder(this.segment, this.ptr, this.elementCount, this.step, this.structDataSize, this.structPointerCount);
        }

        public final <T> T setAs(Factory<T> factory) {
            return factory.constructBuilder(this.segment, this.ptr, this.elementCount, this.step, this.structDataSize, this.structPointerCount);
        }
    }

    public static final class Reader extends ListReader {
        Reader(SegmentReader segment, int ptr, int elementCount, int step, int structDataSize, short structPointerCount, int nestingLimit){
            super(segment, ptr, elementCount, step, structDataSize, structPointerCount, nestingLimit);
        }
        
        public final <T> T getAs(Factory<T> factory) {
            return factory.constructReader(this.segment, this.ptr, this.elementCount, this.step, this.structDataSize, this.structPointerCount, this.nestingLimit);
        }
    }
}
