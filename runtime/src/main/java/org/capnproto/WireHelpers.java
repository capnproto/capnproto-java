package org.capnproto;

final class WireHelpers {

    public static int roundBytesUpToWords(int bytes) {
        return (bytes + 7) / 8;
    }

    public static int roundBitsUpToWords(long bits) {
        //# This code assumes 64-bit words.
        return (int)((bits + 63) / ((long) Constants.BITS_PER_WORD));
    }

    static class AllocateResult {
        public final int ptr;
        public final int refOffset;
        public final SegmentBuilder segment;
        AllocateResult(int ptr, int refOffset, SegmentBuilder segment) {
            this.ptr = ptr; this.refOffset = refOffset; this.segment = segment;
        }
    }

    public static AllocateResult allocate(int refOffset,
                                          SegmentBuilder segment,
                                          int amount, // in words
                                          byte kind) {

        // TODO check for nullness, amount == 0 case.

        int ptr = segment.allocate(amount);
        if (ptr == SegmentBuilder.FAILED_ALLOCATION) {
            //# Need to allocate in a new segment. We'll need to
            //# allocate an extra pointer worth of space to act as
            //# the landing pad for a far pointer.

            int amountPlusRef = amount + Constants.POINTER_SIZE_IN_WORDS;
            BuilderArena.AllocateResult allocation = segment.getArena().allocate(amountPlusRef);

            throw new Error("unimplemented");
        } else {
            WirePointer.setKindAndTarget(segment.buffer, refOffset, kind, ptr);
            return new AllocateResult(ptr, refOffset, segment);
        }
    }

    public static StructBuilder initStructPointer(int refOffset,
                                                  SegmentBuilder segment,
                                                  StructSize size) {
        AllocateResult allocation = allocate(refOffset, segment, size.total(), WirePointer.STRUCT);
        StructPointer.setFromStructSize(allocation.segment.buffer, allocation.refOffset, size);
        return new StructBuilder(allocation.segment, allocation.ptr * Constants.BYTES_PER_WORD,
                                 allocation.ptr + size.data,
                                 size.data * 64, size.pointers, (byte)0);
    }

    public static StructBuilder getWritableStructPointer(int refOffset,
                                                         SegmentBuilder segment,
                                                         StructSize size) {
        long ref = WirePointer.get(segment.buffer, refOffset);
        int target = WirePointer.target(refOffset, ref);
        if (WirePointer.isNull(ref)) {
            return initStructPointer(refOffset, segment, size);
        }
        long oldRef = ref;
        SegmentBuilder oldSegment = segment;
        // TODO follow fars.
        int oldPtrOffset = target;

        short oldDataSize = StructPointer.dataSize(WirePointer.structPointer(oldRef));
        short oldPointerCount = StructPointer.ptrCount(WirePointer.structPointer(oldRef));
        int oldPointerSectionOffset = oldPtrOffset + oldDataSize;

        if (oldDataSize < size.data || oldPointerCount < size.pointers) {
            throw new Error("unimplemented");
        } else {
            return new StructBuilder(oldSegment, oldPtrOffset * 8,
                                     oldPointerSectionOffset, oldDataSize * 64,
                                     oldPointerCount, (byte)0);
        }

    }

    public static ListBuilder initListPointer(int refOffset,
                                              SegmentBuilder segment,
                                              int elementCount,
                                              byte elementSize) {
        if (elementSize == FieldSize.INLINE_COMPOSITE) {
            throw new InternalError("Should have called initStructListPointer instead");
        }

        int dataSize = FieldSize.dataBitsPerElement(elementSize);
        int pointerCount = FieldSize.pointersPerElement(elementSize);
        int step = dataSize + pointerCount * Constants.BITS_PER_POINTER;
        int wordCount = roundBitsUpToWords((long)elementCount * (long)step);
        AllocateResult allocation = allocate(refOffset, segment, wordCount, WirePointer.LIST);

        ListPointer.set(allocation.segment.buffer, allocation.refOffset, elementSize, elementCount);

        return new ListBuilder(allocation.segment,
                               allocation.ptr * Constants.BYTES_PER_WORD,
                               elementCount, step, dataSize, (short)pointerCount);
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
        AllocateResult allocation = allocate(refOffset, segment, Constants.POINTER_SIZE_IN_WORDS + wordCount,
                                             WirePointer.LIST);

        //# Initialize the pointer.
        ListPointer.setInlineComposite(allocation.segment.buffer, allocation.refOffset, wordCount);
        WirePointer.setKindAndInlineCompositeListElementCount(allocation.segment.buffer, allocation.ptr,
                                                              WirePointer.STRUCT, elementCount);
        StructPointer.setFromStructSize(allocation.segment.buffer, allocation.ptr, elementSize);

        return new ListBuilder(allocation.segment,
                               (allocation.ptr + 1) * Constants.BYTES_PER_WORD,
                               elementCount, wordsPerElement * Constants.BITS_PER_WORD,
                               elementSize.data * Constants.BITS_PER_WORD, elementSize.pointers);
    }

    public static ListBuilder getWritableListPointer(int origRefOffset,
                                                     SegmentBuilder origSegment,
                                                     byte elementSize) {
        if (elementSize == FieldSize.INLINE_COMPOSITE) {
            throw new InternalError("Use getStructList{Element,Field} for structs");
        }

        long origRef = WirePointer.get(origSegment.buffer, origRefOffset);
        int origRefTarget = WirePointer.target(origRefOffset, origRef);

        if (WirePointer.isNull(origRef)) {
            throw new Error("unimplemented");
        }

        //# We must verify that the pointer has the right size. Unlike
        //# in getWritableStructListReference(), we never need to
        //# "upgrade" the data, because this method is called only for
        //# non-struct lists, and there is no allowed upgrade path *to*
        //# a non-struct list, only *from* them.

        long ref = origRef;
        SegmentBuilder segment = origSegment;
        int ptr = origRefTarget; // TODO follow fars.

        if (WirePointer.kind(ref) != WirePointer.LIST) {
            throw new DecodeException("Called getList{Field,Element}() but existing pointer is not a list");
        }

        byte oldSize = ListPointer.elementSize(WirePointer.listPointer(ref));

        if (oldSize == FieldSize.INLINE_COMPOSITE) {
            //# The existing element size is InlineComposite, which
            //# means that it is at least two words, which makes it
            //# bigger than the expected element size. Since fields can
            //# only grow when upgraded, the existing data must have
            //# been written with a newer version of the protocol. We
            //# therefore never need to upgrade the data in this case,
            //# but we do need to validate that it is a valid upgrade
            //# from what we expected.
            throw new Error("unimplemented");
        } else {
            int dataSize = FieldSize.dataBitsPerElement(oldSize);
            int pointerCount = FieldSize.pointersPerElement(oldSize);

            if (dataSize < FieldSize.dataBitsPerElement(elementSize)) {
                throw new DecodeException("Existing list value is incompatible with expected type.");
            }
            if (pointerCount < FieldSize.pointersPerElement(elementSize)) {
                throw new DecodeException("Existing list value is incompatible with expected type.");
            }

            int step = dataSize + pointerCount * Constants.BITS_PER_POINTER;

            return new ListBuilder(segment, ptr * Constants.BYTES_PER_WORD,
                                   ListPointer.elementCount(WirePointer.listPointer(ref)),
                                   step, dataSize, (short) pointerCount);
        }
    }

    // size is in bytes
    public static Text.Builder initTextPointer(int refOffset,
                                               SegmentBuilder segment,
                                               int size) {
        //# The byte list must include a NUL terminator.
        int byteSize = size + 1;

        //# Allocate the space.
        AllocateResult allocation = allocate(refOffset, segment, roundBytesUpToWords(byteSize),
                                             WirePointer.LIST);

        //# Initialize the pointer.
        ListPointer.set(allocation.segment.buffer, allocation.refOffset, FieldSize.BYTE, byteSize);

        return new Text.Builder(allocation.segment.buffer, allocation.ptr * Constants.BYTES_PER_WORD, size);
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

    public static Text.Builder getWritableTextPointer(int refOffset,
                                                      SegmentBuilder segment) {
        long ref = WirePointer.get(segment.buffer, refOffset);

        if (WirePointer.isNull(ref)) {
            // TODO default values
            return new Text.Builder(null, 0, 0);
        }

        int refTarget = WirePointer.target(refOffset, ref);
        int ptr = refTarget;

        if (WirePointer.kind(ref) != WirePointer.LIST) {
            throw new DecodeException("Called getText{Field,Element} but existing pointer is not a list.");
        }
        if (ListPointer.elementSize(WirePointer.listPointer(ref)) != FieldSize.BYTE) {
            throw new DecodeException(
                "Called getText{Field,Element} but existing list pointer is not byte-sized.");
        }


        //# Subtract 1 from the size for the NUL terminator.
        return new Text.Builder(segment.buffer, ptr * 8,
                                ListPointer.elementCount(WirePointer.listPointer(ref)) - 1);

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
