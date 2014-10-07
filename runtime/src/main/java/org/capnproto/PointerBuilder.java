package org.capnproto;

public final class PointerBuilder {
    final SegmentBuilder segment;
    final int pointer; // word offset

    public PointerBuilder(SegmentBuilder segment, int pointer) {
        this.segment = segment;
        this.pointer = pointer;
    }

    public static PointerBuilder getRoot(SegmentBuilder segment, int location) {
        return new PointerBuilder(segment, location);
    }

    public final boolean isNull() {
        return this.segment.buffer.getLong(this.pointer) == 0;
    }


    public final <T> T getStruct(FromStructBuilder<T> factory) {
        return WireHelpers.getWritableStructPointer(factory, this.pointer, this.segment, factory.structSize(), null, 0);
    }

    public final <T> T getStruct(FromStructBuilder<T> factory, SegmentReader defaultReader, int defaultOffset) {
        return WireHelpers.getWritableStructPointer(factory, this.pointer, this.segment, factory.structSize(),
                                                    defaultReader, defaultOffset);
    }

    public final Text.Builder getText() {
        return WireHelpers.getWritableTextPointer(
            this.pointer, this.segment, null, 0, 0);
    }

    public Text.Builder getText(java.nio.ByteBuffer defaultBuffer, int defaultOffset, int defaultSize) {
        return WireHelpers.getWritableTextPointer(this.pointer,
                                                  this.segment,
                                                  defaultBuffer,
                                                  defaultOffset,
                                                  defaultSize);
    }

    public final Data.Builder getData() {
        return WireHelpers.getWritableDataPointer(this.pointer,
                                                  this.segment,
                                                  null, 0, 0);
    }

    public Data.Builder getData(java.nio.ByteBuffer defaultBuffer, int defaultOffset, int defaultSize) {
        return WireHelpers.getWritableDataPointer(this.pointer,
                                                  this.segment,
                                                  defaultBuffer,
                                                  defaultOffset,
                                                  defaultSize);
    }

    public final <T> T initStruct(FromStructBuilder<T> factory) {
        return WireHelpers.initStructPointer(factory, this.pointer, this.segment, factory.structSize());
    }

    public final Text.Builder initText(int size) {
        return WireHelpers.initTextPointer(this.pointer, this.segment, size);
    }

    public final Data.Builder initData(int size) {
        return WireHelpers.initDataPointer(this.pointer, this.segment, size);
    }

    public final void setList(ListReader value) {
        WireHelpers.setListPointer(this.segment, this.pointer, value);
    }

    public final void setStruct(StructReader value) {
        WireHelpers.setStructPointer(this.segment, this.pointer, value);
    }

    public final void setText(Text.Reader value) {
        WireHelpers.setTextPointer(this.pointer, this.segment, value);
    }

    public final void setData(Data.Reader value) {
        WireHelpers.setDataPointer(this.pointer, this.segment, value);
    }

    public final void clear() {
        WireHelpers.zeroObject(this.segment, this.pointer);
        this.segment.buffer.putLong(this.pointer * 8, 0L);
    }
}
