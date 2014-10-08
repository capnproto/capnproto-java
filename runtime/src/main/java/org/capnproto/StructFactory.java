package org.capnproto;

public abstract class StructFactory<Builder, Reader extends StructReader>
    implements FromPointerBuilder<Builder>,
    FromPointerBuilderRefDefault<Builder>,
    StructBuilder.Factory<Builder>,
    InitFromPointerBuilder<Builder>,
    SetPointerBuilder<Reader>,
    FromPointerReader<Reader>,
    FromPointerReaderRefDefault<Reader>,
    StructReader.Factory<Reader> {
    public final Reader fromPointerReaderRefDefault(SegmentReader segment, int pointer,
                                                    SegmentReader defaultSegment, int defaultOffset,
                                                    int nestingLimit) {
        return WireHelpers.readStructPointer(this,
                                             segment,
                                             pointer,
                                             defaultSegment, defaultOffset,
                                             nestingLimit);
    }
    public final Reader fromPointerReader(SegmentReader segment, int pointer, int nestingLimit) {
        return fromPointerReaderRefDefault(segment, pointer, null, 0, nestingLimit);
    }
    public final Builder fromPointerBuilderRefDefault(SegmentBuilder segment, int pointer,
                                                      SegmentReader defaultSegment, int defaultOffset) {
        return WireHelpers.getWritableStructPointer(this, pointer, segment, this.structSize(),
                                                    defaultSegment, defaultOffset);
    }
    public final Builder fromPointerBuilder(SegmentBuilder segment, int pointer) {
        return WireHelpers.getWritableStructPointer(this, pointer, segment, this.structSize(),
                                                    null, 0);
    }
    public final Builder initFromPointerBuilder(SegmentBuilder segment, int pointer) {
        return WireHelpers.initStructPointer(this, pointer, segment, this.structSize());
    }

    public final void setPointerBuilder(SegmentBuilder segment, int pointer, Reader value) {
        WireHelpers.setStructPointer(segment, pointer, value);
    }

    public abstract Reader asReader(Builder builder);
}
