package org.capnproto;

public abstract class StructFactory<Builder, Reader> implements FromPointerBuilder<Builder>, StructBuilder.Factory<Builder>,
                                                     InitFromPointerBuilder<Builder>,
                                                     FromPointerReader<Reader>, StructReader.Factory<Reader> {
    public final Reader fromPointerReader(SegmentReader segment, int pointer,
                                          SegmentReader defaultSegment, int defaultOffset,
                                          int nestingLimit) {
        return WireHelpers.readStructPointer(this,
                                             segment,
                                             pointer,
                                             defaultSegment, defaultOffset,
                                             nestingLimit);
    }
    public final Builder fromPointerBuilder(SegmentBuilder segment, int pointer, SegmentReader defaultSegment, int defaultOffset) {
        return WireHelpers.getWritableStructPointer(this, pointer, segment, this.structSize(),
                                                    defaultSegment, defaultOffset);
    }
    public final Builder initFromPointerBuilder(PointerBuilder builder) {
        return WireHelpers.initStructPointer(this, builder.pointer, builder.segment, this.structSize());
    }

    public abstract Reader asReader(Builder builder);
}
