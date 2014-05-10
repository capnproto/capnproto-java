package capnp;

class WireHelpers {
    public static Text.Reader readTextPointer(SegmentReader segment,
                                         WirePointer ref) {
        ref.target();
        ListPointer listPtr = (ListPointer)ref;
        return new Text.Reader(listPtr);
    }
}
