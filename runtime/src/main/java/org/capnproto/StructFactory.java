package org.capnproto;

public abstract class StructFactory<Builder, Reader> implements FromPointerBuilder<Builder>, StructBuilder.Factory<Builder>,
                                                     InitFromPointerBuilder<Builder>,
                                                     FromPointerReader<Reader>, StructReader.Factory<Reader> {
    public Builder fromPointerBuilder(PointerBuilder builder, SegmentReader defaultSegment, int defaultOffset) {
        return WireHelpers.getWritableStructPointer(this, builder.pointer, builder.segment, this.structSize(),
                                                    defaultSegment, defaultOffset);
    }
    public Builder initFromPointerBuilder(PointerBuilder builder) {
        return WireHelpers.initStructPointer(this, builder.pointer, builder.segment, this.structSize());
    }
    public Reader fromPointerReader(PointerReader reader, SegmentReader defaultSegment, int defaultOffset) {
        return WireHelpers.readStructPointer(this,
                                             reader.segment,
                                             reader.pointer,
                                             defaultSegment, defaultOffset,
                                             reader.nestingLimit);
    }
    public abstract Reader asReader(Builder builder);
}
