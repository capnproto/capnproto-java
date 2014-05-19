package org.capnproto;

public final class StructSize {
    public final short data;      // number of words in data section
    public final short pointers;  // number of words in pointer section
    public final byte preferredListEncoding; // a FieldSize

    public StructSize(short data, short pointers, byte preferredListEncoding) {
        this.data = data;
        this.pointers = pointers;
        this.preferredListEncoding = preferredListEncoding;
    }

    public final int total() {
        return (int)this.data + (int)this.pointers;
    }

}
