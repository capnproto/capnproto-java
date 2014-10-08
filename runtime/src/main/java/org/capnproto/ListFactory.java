package org.capnproto;

public abstract class ListFactory<Builder, Reader extends ListReader>
    implements ListBuilder.Factory<Builder>, FromPointerBuilder<Builder>,
    InitSizedFromPointerBuilder<Builder>,
    SetPointerBuilder<Reader>,
    ListReader.Factory<Reader>, FromPointerReader<Reader> {

    final byte elementSize;
    ListFactory(byte elementSize) {this.elementSize = elementSize;}

    public final Reader fromPointerReader(SegmentReader segment, int pointer,
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

    public Builder fromPointerBuilder(SegmentBuilder segment, int pointer, SegmentReader defaultSegment, int defaultOffset) {
        return WireHelpers.getWritableListPointer(this,
                                                  pointer,
                                                  segment,
                                                  this.elementSize,
                                                  defaultSegment,
                                                  defaultOffset);
    }

    public Builder initSizedFromPointerBuilder(SegmentBuilder segment, int pointer, int elementCount) {
        return WireHelpers.initListPointer(this, pointer, segment, elementCount, this.elementSize);
    }

    public final void setPointerBuilder(SegmentBuilder segment, int pointer, Reader value) {
        WireHelpers.setListPointer(segment, pointer, value);
    }
}
