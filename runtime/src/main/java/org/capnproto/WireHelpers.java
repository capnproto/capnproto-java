package org.capnproto;

final class WireHelpers {

    public static int roundBytesUpToWords(int bytes) {
        return (bytes + 7) / 8;
    }

    public static int allocate(int refOffset,
                               SegmentBuilder segment,
                               int amount,
                               byte kind) {

        // TODO check for nullness, amount == 0 case.

        int allocation = segment.allocate(amount);
        if (allocation == SegmentBuilder.FAILED_ALLOCATION) {
            //# Need to allocate in a new segment. We'll need to
            //# allocate an extra pointer worth of space to act as
            //# the landing pad for a far pointer.
            throw new Error("unimplemented");
        } else {
            WirePointer.setKindAndTarget(segment.buffer, refOffset, kind, allocation);
            return allocation;
        }
    }

    public static StructBuilder initStructPointer(int refOffset,
                                                  SegmentBuilder segment,
                                                  StructSize size) {
        int ptrOffset = allocate(refOffset, segment, size.total(), WirePointer.STRUCT);
        StructPointer.setFromStructSize(segment.buffer, refOffset, size);
        return new StructBuilder(segment, ptrOffset * 8, ptrOffset + size.data,
                                 size.data * 64, size.pointers, (byte)0);
    }

    public static StructBuilder getWritableStructPointer(int refOffset,
                                                         SegmentBuilder segment,
                                                         StructSize size) {
        throw new Error("unimplemented");
    }

    public static ListBuilder initListPointer(int refOffset,
                                              SegmentBuilder segment,
                                              int elementCount,
                                              byte elementSize) {
        throw new Error("unimplemented");
    }

    public static ListBuilder initStructListPointer(int refOffset,
                                                    SegmentBuilder segment,
                                                    int elementCount,
                                                    StructSize elementSize) {
        if (elementSize.preferredListEncoding != FieldSize.INLINE_COMPOSITE) {
            //# Small data-only struct. Allocate a list of primitives instead.
            return initListPointer(refOffset, segment, elementCount,
                                   elementSize.preferredListEncoding);
        }

        int wordsPerElement = elementSize.total();

        //# Allocate the list, prefixed by a single WirePointer.
        int wordCount = elementCount * wordsPerElement;
        int ptrOffset = allocate(refOffset, segment, 1 + wordCount, WirePointer.LIST);

        //# Initialize the pointer.
        ListPointer.setInlineComposite(segment.buffer, refOffset, wordCount);
        WirePointer.setKindAndInlineCompositeListElementCount(segment.buffer, ptrOffset,
                                                              WirePointer.STRUCT, elementCount);
        StructPointer.setFromStructSize(segment.buffer, ptrOffset, elementSize);

        ptrOffset += 1;

        return new ListBuilder(segment, ptrOffset * 8, elementCount, wordsPerElement * 64,
                               elementSize.data * 64, elementSize.pointers);
    }

    // size is in bytes
    public static Text.Builder initTextPointer(int refOffset,
                                               SegmentBuilder segment,
                                               int size) {
        //# The byte list must include a NUL terminator.
        int byteSize = size + 1;

        //# Allocate the space.
        int ptrOffset = allocate(refOffset, segment, roundBytesUpToWords(byteSize), WirePointer.LIST);

        //# Initialize the pointer.
        ListPointer.set(segment.buffer, refOffset, FieldSize.BYTE, byteSize);

        return new Text.Builder(segment.buffer, ptrOffset * 8, size);
    }

    public static Text.Builder setTextPointer(int refOffset,
                                              SegmentBuilder segment,
                                              Text.Reader value) {
        Text.Builder builder = initTextPointer(refOffset, segment, value.size);

        // TODO is there a way to do this with bulk methods?
        for (int i = 0; i < builder.size; ++i) {
            builder.buffer.put(builder.offset + i, value.buffer.get(value.offset + i));
        }
        return builder;
    }

    public static StructReader readStructPointer(SegmentReader segment,
                                                 int refOffset,
                                                 int nestingLimit) {

        // TODO error handling

        if (nestingLimit < 0) {
            throw new DecodeException("Message is too deeply nested or contains cycles.");
        }

        long ref = WirePointer.get(segment.buffer, refOffset);
        int ptrOffset = WirePointer.target(refOffset, ref);
        int structPtr = WirePointer.structPointer(ref);
        int dataSizeWords = StructPointer.dataSize(structPtr);

        return new StructReader(segment,
                                ptrOffset * 8,
                                (ptrOffset + dataSizeWords),
                                dataSizeWords * 64,
                                StructPointer.ptrCount(structPtr),
                                (byte)0,
                                nestingLimit - 1);

    }


    public static ListReader readListPointer(SegmentReader segment,
                                             int refOffset,
                                             byte expectedElementSize,
                                             int nestingLimit) {

        long ref = WirePointer.get(segment.buffer, refOffset);

        // TODO check for null, follow fars, nestingLimit
        if (WirePointer.isNull(ref)) {
            return new ListReader();
        }

        int listPtr = WirePointer.listPointer(ref);

        int ptrOffset = WirePointer.target(refOffset, ref);
        long ptr = WirePointer.get(segment.buffer, ptrOffset);

        switch (ListPointer.elementSize(listPtr)) {
        case FieldSize.INLINE_COMPOSITE : {
            int wordCount = ListPointer.inlineCompositeWordCount(listPtr);

            long tag = ptr;
            ptrOffset += 1;

            // TODO bounds check

            int size = WirePointer.inlineCompositeListElementCount(tag);

            int structPtr = WirePointer.structPointer(tag);
            int wordsPerElement = StructPointer.wordSize(structPtr);

            // TODO check that elemements do not overrun word count

            // TODO check whether the size is compatible

            return new ListReader(segment,    // TODO follow fars
                                  ptrOffset * 8, //
                                  size,
                                  wordsPerElement * 64,
                                  StructPointer.dataSize(structPtr) * 64,
                                  StructPointer.ptrCount(structPtr),
                                  nestingLimit - 1);
        }
        case FieldSize.VOID : break;
        default :
            throw new Error("unrecognized element size");
        }

        throw new Error();
    }

    public static Text.Reader readTextPointer(SegmentReader segment,
                                              int refOffset) {
        long ref = WirePointer.get(segment.buffer, refOffset);

        if (WirePointer.isNull(ref)) {
            // XXX should use the default value
            return new Text.Reader(java.nio.ByteBuffer.wrap(new byte[0]), 0, 0);
        }

        int ptrOffset = WirePointer.target(refOffset, ref);
        int listPtr = WirePointer.listPointer(ref);
        int size = ListPointer.elementCount(listPtr);

        if (WirePointer.kind(ref) != WirePointer.LIST) {
            throw new DecodeException("Message contains non-list pointer where text was expected.");
        }

        if (ListPointer.elementSize(listPtr) != FieldSize.BYTE) {
            throw new DecodeException("Message contains list pointer of non-bytes where text was expected.");
        }

        // TODO bounds check?

        if (size == 0 || segment.buffer.get(8 * ptrOffset + size - 1) != 0) {
            throw new DecodeException("Message contains text that is not NUL-terminated.");
        }

        return new Text.Reader(segment.buffer, ptrOffset, size - 1);
    }
}
