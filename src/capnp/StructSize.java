package org.capnproto;

public final class StructSize {
    public final short data;
    public final short pointers;
    public final byte preferredListEncoding;

    public StructSize(short data, short pointers, byte preferredListEncoding) {
        this.data = data;
        this.pointers = pointers;
        this.preferredListEncoding = preferredListEncoding;
    }
}
