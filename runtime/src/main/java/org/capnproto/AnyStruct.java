package org.capnproto;

public class AnyStruct {

    public static final org.capnproto.StructSize STRUCT_SIZE = new org.capnproto.StructSize((short)0,(short)0);

    public static final class Factory extends org.capnproto.StructFactory<Builder, Reader> {
        public Factory() {
        }
        public final Reader constructReader(org.capnproto.SegmentReader segment, int data,int pointers, int dataSize, short pointerCount, int nestingLimit) {
            return new Reader(segment,data,pointers,dataSize,pointerCount,nestingLimit);
        }
        public final Builder constructBuilder(org.capnproto.SegmentBuilder segment, int data,int pointers, int dataSize, short pointerCount) {
            return new Builder(segment, data, pointers, dataSize, pointerCount);
        }
        public final org.capnproto.StructSize structSize() {
            return AnyStruct.STRUCT_SIZE;
        }
        public final Reader asReader(Builder builder) {
            return builder.asReader();
        }
    }

    public static final Factory factory = new Factory();

    public static final org.capnproto.StructList.Factory<Builder,Reader> listFactory =
            new org.capnproto.StructList.Factory<>(factory);

    public static final class Builder extends org.capnproto.StructBuilder {
        Builder(org.capnproto.SegmentBuilder segment, int data, int pointers,int dataSize, short pointerCount){
            super(segment, data, pointers, dataSize, pointerCount);
        }
        public final Reader asReader() {
            return new Reader(segment, data, pointers, dataSize, pointerCount, 0x7fffffff);
        }

        public final <T> T initAs(StructBuilder.Factory<T> factory) {
            return factory.constructBuilder(this.segment, this.capTable, this.data, this.pointers, this.dataSize, this.pointerCount);
        }

        public final <T> T setAs(StructBuilder.Factory<T> factory) {
            return factory.constructBuilder(this.segment, this.capTable, this.data, this.pointers, this.dataSize, this.pointerCount);
        }

    }

    public static final class Reader extends org.capnproto.StructReader {
        Reader(org.capnproto.SegmentReader segment, int data, int pointers,int dataSize, short pointerCount, int nestingLimit){
            super(segment, data, pointers, dataSize, pointerCount, nestingLimit);
        }

        public final <T> T getAs(StructReader.Factory<T> factory) {
            return factory.constructReader(this.segment, this.capTable, this.data, this.pointers, this.dataSize, this.pointerCount, this.nestingLimit);
        }
    }
}
