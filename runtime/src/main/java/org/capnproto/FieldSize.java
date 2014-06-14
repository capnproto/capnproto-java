package org.capnproto;

public final class FieldSize {
    public static final byte VOID = 0;
    public static final byte BIT = 1;
    public static final byte BYTE = 2;
    public static final byte TWO_BYTES = 3;
    public static final byte FOUR_BYTES = 4;
    public static final byte EIGHT_BYTES = 5;
    public static final byte POINTER = 6;
    public static final byte INLINE_COMPOSITE = 7;

    public static final int dataBitsPerElement(byte size) {
        switch (size) {
        case VOID: return 0;
        case BIT: return 1;
        case BYTE: return 8;
        case TWO_BYTES: return 16;
        case FOUR_BYTES: return 32;
        case EIGHT_BYTES: return 64;
        case POINTER: return 0;
        case INLINE_COMPOSITE: return 0;
        default : throw new Error("impossible field size: " + size);
        }
    }

    public static final int pointersPerElement(byte size) {
        switch (size) {
        case POINTER: return 1;
        default: return 0;
        }
    }
}
