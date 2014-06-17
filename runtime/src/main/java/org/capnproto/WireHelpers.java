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

            //# Set up the original pointer to be a far pointer to
            //# the new segment.
            FarPointer.set(segment.buffer, refOffset, false, allocation.offset);
            FarPointer.setSegmentId(segment.buffer, refOffset, allocation.segment.id);

            //# Initialize the landing pad to indicate that the
            //# data immediately follows the pad.
            int resultRefOffset = allocation.offset;
            int ptr1 = allocation.offset + Constants.POINTER_SIZE_IN_WORDS;

            WirePointer.setKindAndTarget(allocation.segment.buffer, resultRefOffset, kind,
                                         ptr1);

            return new AllocateResult(ptr1, resultRefOffset, allocation.segment);
        } else {
            WirePointer.setKindAndTarget(segment.buffer, refOffset, kind, ptr);
            return new AllocateResult(ptr, refOffset, segment);
        }
    }

    static class FollowBuilderFarsResult {
        public final int ptr;
        public final long ref;
        public final SegmentBuilder segment;
        FollowBuilderFarsResult(int ptr, long ref, SegmentBuilder segment) {
            this.ptr = ptr; this.ref = ref; this.segment = segment;
        }
    }

    public static FollowBuilderFarsResult followBuilderFars(long ref, int refTarget,
                                                            SegmentBuilder segment) {
        //# If `ref` is a far pointer, follow it. On return, `ref` will
        //# have been updated to point at a WirePointer that contains
        //# the type information about the target object, and a pointer
        //# to the object contents is returned. The caller must NOT use
        //# `ref->target()` as this may or may not actually return a
        //# valid pointer. `segment` is also updated to point at the
        //# segment which actually contains the object.
        //#
        //# If `ref` is not a far pointer, this simply returns
        //# `refTarget`. Usually, `refTarget` should be the same as
        //# `ref->target()`, but may not be in cases where `ref` is
        //# only a tag.

        if (WirePointer.kind(ref) == WirePointer.FAR) {
            SegmentBuilder resultSegment = segment.getArena().getSegment(FarPointer.getSegmentId(ref));

            int padOffset = FarPointer.positionInSegment(ref);
            long pad = WirePointer.get(resultSegment.buffer, padOffset);
            if (! FarPointer.isDoubleFar(ref)) {
                return new FollowBuilderFarsResult(WirePointer.target(padOffset, pad), pad, resultSegment);
            }

            //# Landing pad is another far pointer. It is followed by a
            //# tag describing the pointed-to object.
            throw new Error("unimplemented");

        } else {
            return new FollowBuilderFarsResult(refTarget, ref, segment);
        }
    }

    static class FollowFarsResult {
        public final int ptr;
        public final long ref;
        public final SegmentReader segment;
        FollowFarsResult(int ptr, long ref, SegmentReader segment) {
            this.ptr = ptr; this.ref = ref; this.segment = segment;
        }
    }

    public static FollowFarsResult followFars(long ref, int refTarget, SegmentReader segment) {
        //# If the segment is null, this is an unchecked message,
        //# so there are no FAR pointers.
        if (segment != null && WirePointer.kind(ref) == WirePointer.FAR) {

            throw new Error("unimplemented");

        } else {
            return new FollowFarsResult(refTarget, ref, segment);
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
        FollowBuilderFarsResult resolved = followBuilderFars(ref, target, segment);

        short oldDataSize = StructPointer.dataSize(resolved.ref);
        short oldPointerCount = StructPointer.ptrCount(resolved.ref);
        int oldPointerSectionOffset = resolved.ptr + oldDataSize;

        if (oldDataSize < size.data || oldPointerCount < size.pointers) {
            throw new Error("unimplemented");
        } else {
            return new StructBuilder(resolved.segment, resolved.ptr * Constants.BYTES_PER_WORD,
                                     oldPointerSectionOffset, oldDataSize * Constants.BITS_PER_WORD,
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

        FollowBuilderFarsResult resolved = followBuilderFars(origRef, origRefTarget, origSegment);

        if (WirePointer.kind(resolved.ref) != WirePointer.LIST) {
            throw new DecodeException("Called getList{Field,Element}() but existing pointer is not a list");
        }

        byte oldSize = ListPointer.elementSize(resolved.ref);

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

            return new ListBuilder(resolved.segment, resolved.ptr * Constants.BYTES_PER_WORD,
                                   ListPointer.elementCount(resolved.ref),
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
        FollowBuilderFarsResult resolved = followBuilderFars(ref, refTarget, segment);

        if (WirePointer.kind(resolved.ref) != WirePointer.LIST) {
            throw new DecodeException("Called getText{Field,Element} but existing pointer is not a list.");
        }
        if (ListPointer.elementSize(resolved.ref) != FieldSize.BYTE) {
            throw new DecodeException(
                "Called getText{Field,Element} but existing list pointer is not byte-sized.");
        }


        //# Subtract 1 from the size for the NUL terminator.
        return new Text.Builder(resolved.segment.buffer, resolved.ptr * Constants.BYTES_PER_WORD,
                                ListPointer.elementCount(resolved.ref) - 1);

    }

    public static StructReader readStructPointer(SegmentReader segment,
                                                 int refOffset,
                                                 int nestingLimit) {

        // TODO error handling. is_null

        if (nestingLimit <= 0) {
            throw new DecodeException("Message is too deeply nested or contains cycles.");
        }

        long ref = WirePointer.get(segment.buffer, refOffset);
        int refTarget = WirePointer.target(refOffset, ref);
        FollowFarsResult resolved = followFars(ref, refTarget, segment);

        int dataSizeWords = StructPointer.dataSize(resolved.ref);

        if (WirePointer.kind(resolved.ref) != WirePointer.STRUCT) {
            throw new DecodeException("Message contains non-struct pointer where struct pointer was expected.");
        }

        // TODO "bounds_check" (read limiting)

        return new StructReader(resolved.segment,
                                resolved.ptr * Constants.BYTES_PER_WORD,
                                (resolved.ptr + dataSizeWords),
                                dataSizeWords * Constants.BITS_PER_WORD,
                                StructPointer.ptrCount(resolved.ref),
                                (byte)0,
                                nestingLimit - 1);

    }


    public static ListReader readListPointer(SegmentReader segment,
                                             int refOffset,
                                             byte expectedElementSize,
                                             int nestingLimit) {

        long ref = WirePointer.get(segment.buffer, refOffset);

        if (WirePointer.isNull(ref)) {
            return new ListReader();
        }

        if (nestingLimit <= 0) {
            throw new Error("nesting limit exceeded");
        }

        int refTarget = WirePointer.target(refOffset, ref);

        FollowFarsResult resolved = followFars(ref, refTarget, segment);

        switch (ListPointer.elementSize(resolved.ref)) {
        case FieldSize.INLINE_COMPOSITE : {
            int wordCount = ListPointer.inlineCompositeWordCount(resolved.ref);

            long tag = WirePointer.get(resolved.segment.buffer, resolved.ptr);
            int ptr = resolved.ptr + 1;

            // TODO bounds check

            int size = WirePointer.inlineCompositeListElementCount(tag);

            int wordsPerElement = StructPointer.wordSize(tag);

            // TODO check that elemements do not overrun word count

            // TODO check whether the size is compatible

            return new ListReader(resolved.segment,
                                  ptr * Constants.BYTES_PER_WORD,
                                  size,
                                  wordsPerElement * Constants.BITS_PER_WORD,
                                  StructPointer.dataSize(tag) * Constants.BITS_PER_WORD,
                                  StructPointer.ptrCount(tag),
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

        int refTarget = WirePointer.target(refOffset, ref);

        FollowFarsResult resolved = followFars(ref, refTarget, segment);

        int size = ListPointer.elementCount(resolved.ref);

        if (WirePointer.kind(resolved.ref) != WirePointer.LIST) {
            throw new DecodeException("Message contains non-list pointer where text was expected.");
        }

        if (ListPointer.elementSize(resolved.ref) != FieldSize.BYTE) {
            throw new DecodeException("Message contains list pointer of non-bytes where text was expected.");
        }

        // TODO bounds check?

        if (size == 0 || resolved.segment.buffer.get(8 * resolved.ptr + size - 1) != 0) {
            throw new DecodeException("Message contains text that is not NUL-terminated.");
        }

        return new Text.Reader(resolved.segment.buffer, resolved.ptr, size - 1);
    }
}
