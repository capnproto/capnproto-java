package org.capnproto;

public abstract class ListFactory<Builder, Reader> implements ListBuilder.Factory<Builder>, FromPointerBuilder<Builder>,
                                              InitSizedFromPointerBuilder<Builder>,
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

    public Builder initSizedFromPointerBuilder(PointerBuilder builder, int elementCount) {
        return WireHelpers.initListPointer(this, builder.pointer, builder.segment, elementCount, this.elementSize);
    }
}
