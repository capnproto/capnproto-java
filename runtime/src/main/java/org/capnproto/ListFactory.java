package org.capnproto;

public abstract class ListFactory<Builder, Reader> implements ListBuilder.Factory<Builder>, FromPointerBuilder<Builder>,
                                              InitSizedFromPointerBuilder<Builder>,
                                              ListReader.Factory<Reader>, FromPointerReader<Reader> {
    final byte elementSize;
    ListFactory(byte elementSize) {this.elementSize = elementSize;}

    public final Reader fromPointerReader(PointerReader reader, SegmentReader defaultSegment, int defaultOffset) {
        return WireHelpers.readListPointer(this,
                                           reader.segment,
                                           reader.pointer,
                                           defaultSegment,
                                           defaultOffset,
                                           this.elementSize,
                                           reader.nestingLimit);
    }

    public Builder fromPointerBuilder(PointerBuilder builder, SegmentReader defaultSegment, int defaultOffset) {
        return WireHelpers.getWritableListPointer(this,
                                                  builder.pointer,
                                                  builder.segment,
                                                  this.elementSize,
                                                  defaultSegment,
                                                  defaultOffset);
    }

    public Builder initSizedFromPointerBuilder(PointerBuilder builder, int elementCount) {
        return WireHelpers.initListPointer(this, builder.pointer, builder.segment, elementCount, this.elementSize);
    }
}
