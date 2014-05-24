package org.capnproto;

public final class MessageBuilder {

    private final BuilderArena arena;

    public MessageBuilder() {
        this.arena = new BuilderArena();
    }

    public <T> T getRoot(FromStructBuilder<T> factory) {
        throw new Error("unimplemented");
    }

    public <T> T initRoot(FromStructBuilder<T> factory) {
        SegmentBuilder rootSegment = this.arena.segments.get(0);
        int location = rootSegment.allocate(1);
        if (location == SegmentBuilder.FAILED_ALLOCATION) {
            throw new Error("could not allocate root pointer");
        }

        AnyPointer.Builder ptr = new AnyPointer.Builder(PointerBuilder.getRoot(rootSegment, location));
        return ptr.initAsStruct(factory);
    }

    //public final getSegmentsForOutput()
}
