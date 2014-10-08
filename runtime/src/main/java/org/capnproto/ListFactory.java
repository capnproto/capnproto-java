package org.capnproto;

public abstract class ListFactory<Builder, Reader extends ListReader>
    implements ListBuilder.Factory<Builder>,
    FromPointerBuilder<Builder>,
    FromPointerBuilderRefDefault<Builder>,
    InitSizedFromPointerBuilder<Builder>,
    SetPointerBuilder<Reader>,
    ListReader.Factory<Reader>,
    FromPointerReader<Reader>,
    FromPointerReaderRefDefault<Reader> {

    final byte elementSize;
    ListFactory(byte elementSize) {this.elementSize = elementSize;}

    public final Reader fromPointerReaderRefDefault(SegmentReader segment, int pointer,
                                                    SegmentReader defaultSegment, int defaultOffset,
                                                    int nestingLimit) {
        return WireHelpers.readListPointer(this,
                                           segment,
                                           pointer,
                                           defaultSegment,
                                           defaultOffset,
                                           this.elementSize,
                                           nestingLimit);
    }

    public final Reader fromPointerReader(SegmentReader segment, int pointer, int nestingLimit) {
        return fromPointerReaderRefDefault(segment, pointer, null, 0, nestingLimit);
    }

    public Builder fromPointerBuilderRefDefault(SegmentBuilder segment, int pointer,
                                                SegmentReader defaultSegment, int defaultOffset) {
        return WireHelpers.getWritableListPointer(this,
                                                  pointer,
                                                  segment,
                                                  this.elementSize,
                                                  defaultSegment,
                                                  defaultOffset);
    }

    public Builder fromPointerBuilder(SegmentBuilder segment, int pointer) {
        return WireHelpers.getWritableListPointer(this,
                                                  pointer,
                                                  segment,
                                                  this.elementSize,
                                                  null, 0);
    }

    public Builder initSizedFromPointerBuilder(SegmentBuilder segment, int pointer, int elementCount) {
        return WireHelpers.initListPointer(this, pointer, segment, elementCount, this.elementSize);
    }

    public final void setPointerBuilder(SegmentBuilder segment, int pointer, Reader value) {
        WireHelpers.setListPointer(segment, pointer, value);
    }
}
