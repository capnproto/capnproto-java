// Copyright (c) 2013-2014 Sandstorm Development Group, Inc. and contributors
// Licensed under the MIT License:
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

package org.capnproto;

import java.nio.ByteBuffer;

final class WireHelpers {

    static int roundBytesUpToWords(int bytes) {
        return (bytes + 7) / 8;
    }

    static int roundBitsUpToBytes(int bits) {
        return (bits + 7) / Constants.BITS_PER_BYTE;
    }

    static int roundBitsUpToWords(long bits) {
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

    static AllocateResult allocate(int refOffset,
                                   SegmentBuilder segment,
                                   int amount, // in words
                                   byte kind) {

        long ref = segment.get(refOffset);
        if (!WirePointer.isNull(ref)) {
            zeroObject(segment, refOffset);
        }

        if (amount == 0 && kind == WirePointer.STRUCT) {
            WirePointer.setKindAndTargetForEmptyStruct(segment.buffer, refOffset);
            return new AllocateResult(refOffset, refOffset, segment);
        }

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

    static FollowBuilderFarsResult followBuilderFars(long ref, int refTarget,
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
            long pad = resultSegment.get(padOffset);
            if (! FarPointer.isDoubleFar(ref)) {
                return new FollowBuilderFarsResult(WirePointer.target(padOffset, pad), pad, resultSegment);
            }

            //# Landing pad is another far pointer. It is followed by a
            //# tag describing the pointed-to object.
            int refOffset = padOffset + 1;
            ref = resultSegment.get(refOffset);

            resultSegment = resultSegment.getArena().getSegment(FarPointer.getSegmentId(pad));
            return new FollowBuilderFarsResult(FarPointer.positionInSegment(pad), ref, resultSegment);
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

    static FollowFarsResult followFars(long ref, int refTarget, SegmentReader segment) {
        //# If the segment is null, this is an unchecked message,
        //# so there are no FAR pointers.
        if (segment != null && WirePointer.kind(ref) == WirePointer.FAR) {
            SegmentReader resultSegment = segment.arena.tryGetSegment(FarPointer.getSegmentId(ref));
            int padOffset = FarPointer.positionInSegment(ref);
            long pad = resultSegment.get(padOffset);

            int padWords = FarPointer.isDoubleFar(ref) ? 2 : 1;
            // TODO read limiting

            if (!FarPointer.isDoubleFar(ref)) {

                return new FollowFarsResult(WirePointer.target(padOffset, pad),
                                            pad, resultSegment);
            } else {
                //# Landing pad is another far pointer. It is
                //# followed by a tag describing the pointed-to
                //# object.

                long tag = resultSegment.get(padOffset + 1);
                resultSegment = resultSegment.arena.tryGetSegment(FarPointer.getSegmentId(pad));
                return new FollowFarsResult(FarPointer.positionInSegment(pad), tag, resultSegment);
            }
        } else {
            return new FollowFarsResult(refTarget, ref, segment);
        }
    }

    static void zeroObject(SegmentBuilder segment, int refOffset) {
        //# Zero out the pointed-to object. Use when the pointer is
        //# about to be overwritten making the target object no longer
        //# reachable.

        //# We shouldn't zero out external data linked into the message.
        if (!segment.isWritable()) return;

        long ref = segment.get(refOffset);

        switch (WirePointer.kind(ref)) {
        case WirePointer.STRUCT:
        case WirePointer.LIST:
            zeroObject(segment, ref, WirePointer.target(refOffset, ref));
            break;
        case WirePointer.FAR: {
            segment = segment.getArena().getSegment(FarPointer.getSegmentId(ref));
            if (segment.isWritable()) { //# Don't zero external data.
                int padOffset = FarPointer.positionInSegment(ref);
                long pad = segment.get(padOffset);
                if (FarPointer.isDoubleFar(ref)) {
                    SegmentBuilder otherSegment = segment.getArena().getSegment(FarPointer.getSegmentId(ref));
                    if (otherSegment.isWritable()) {
                        zeroObject(otherSegment, padOffset + 1, FarPointer.positionInSegment(pad));
                    }
                    segment.buffer.putLong(padOffset * 8, 0L);
                    segment.buffer.putLong((padOffset + 1) * 8, 0L);

                } else {
                    zeroObject(segment, padOffset);
                    segment.buffer.putLong(padOffset * 8, 0L);
                }
            }

            break;
        }
        case WirePointer.OTHER: {
            // TODO
        }
        }
    }

    static void zeroObject(SegmentBuilder segment, long tag, int ptr) {
        //# We shouldn't zero out external data linked into the message.
        if (!segment.isWritable()) return;

        switch (WirePointer.kind(tag)) {
        case WirePointer.STRUCT: {
            int pointerSection = ptr + StructPointer.dataSize(tag);
            int count = StructPointer.ptrCount(tag);
            for (int ii = 0; ii < count; ++ii) {
                zeroObject(segment, pointerSection + ii);
            }
            memset(segment.buffer, ptr * Constants.BYTES_PER_WORD, (byte)0,
                   StructPointer.wordSize(tag) * Constants.BYTES_PER_WORD);
            break;
        }
        case WirePointer.LIST: {
            switch (ListPointer.elementSize(tag)) {
            case ElementSize.VOID: break;
            case ElementSize.BIT:
            case ElementSize.BYTE:
            case ElementSize.TWO_BYTES:
            case ElementSize.FOUR_BYTES:
            case ElementSize.EIGHT_BYTES: {
                memset(segment.buffer, ptr * Constants.BYTES_PER_WORD, (byte)0,
                       roundBitsUpToWords(
                           ListPointer.elementCount(tag) *
                           ElementSize.dataBitsPerElement(ListPointer.elementSize(tag))) * Constants.BYTES_PER_WORD);
                break;
            }
            case ElementSize.POINTER: {
                int count = ListPointer.elementCount(tag);
                for (int ii = 0; ii < count; ++ii) {
                    zeroObject(segment, ptr + ii);
                }
                memset(segment.buffer, ptr * Constants.BYTES_PER_WORD, (byte)0,
                       count * Constants.BYTES_PER_WORD);
                break;
            }
            case ElementSize.INLINE_COMPOSITE: {
                long elementTag = segment.get(ptr);
                if (WirePointer.kind(elementTag) != WirePointer.STRUCT) {
                    throw new Error("Don't know how to handle non-STRUCT inline composite.");
                }
                int dataSize = StructPointer.dataSize(elementTag);
                int pointerCount = StructPointer.ptrCount(elementTag);

                int pos = ptr + Constants.POINTER_SIZE_IN_WORDS;
                int count = WirePointer.inlineCompositeListElementCount(elementTag);
                for (int ii = 0; ii < count; ++ii) {
                    pos += dataSize;
                    for (int jj = 0; jj < pointerCount; ++jj) {
                        zeroObject(segment, pos);
                        pos += Constants.POINTER_SIZE_IN_WORDS;
                    }
                }

                memset(segment.buffer, ptr * Constants.BYTES_PER_WORD, (byte)0,
                       (StructPointer.wordSize(elementTag) * count + Constants.POINTER_SIZE_IN_WORDS) * Constants.BYTES_PER_WORD);
                break;
            }
            }
            break;
        }
        case WirePointer.FAR:
            throw new Error("Unexpected FAR pointer.");
        case WirePointer.OTHER:
            throw new Error("Unexpected OTHER pointer.");
        }
    }

    static void zeroPointerAndFars(SegmentBuilder segment, int refOffset) {
        //# Zero out the pointer itself and, if it is a far pointer, zero the landing pad as well,
        //# but do not zero the object body. Used when upgrading.

        long ref = segment.get(refOffset);
        if (WirePointer.kind(ref) == WirePointer.FAR) {
            SegmentBuilder padSegment = segment.getArena().getSegment(FarPointer.getSegmentId(ref));
            if (padSegment.isWritable()) { //# Don't zero external data.
                int padOffset = FarPointer.positionInSegment(ref);
                padSegment.buffer.putLong(padOffset * Constants.BYTES_PER_WORD, 0L);
                if (FarPointer.isDoubleFar(ref)) {
                    padSegment.buffer.putLong(padOffset * Constants.BYTES_PER_WORD + 1, 0L);
                }
            }
        }
        segment.put(refOffset, 0L);
    }

    static void transferPointer(SegmentBuilder dstSegment, int dstOffset,
                                SegmentBuilder srcSegment, int srcOffset) {
        //# Make *dst point to the same object as *src. Both must reside in the same message, but can
        //# be in different segments.
        //#
        //# Caller MUST zero out the source pointer after calling this, to make sure no later code
        //# mistakenly thinks the source location still owns the object.  transferPointer() doesn't do
        //# this zeroing itself because many callers transfer several pointers in a loop then zero out
        //# the whole section.

        long src = srcSegment.get(srcOffset);
        if (WirePointer.isNull(src)) {
            dstSegment.put(dstOffset, 0L);
        } else if (WirePointer.kind(src) == WirePointer.FAR) {
            //# Far pointers are position-independent, so we can just copy.
            dstSegment.put(dstOffset, srcSegment.get(srcOffset));
        } else {
            transferPointer(dstSegment, dstOffset, srcSegment, srcOffset,
                            WirePointer.target(srcOffset, src));
        }
    }

    static void transferPointer(SegmentBuilder dstSegment, int dstOffset,
                                SegmentBuilder srcSegment, int srcOffset, int srcTargetOffset) {
        //# Like the other overload, but splits src into a tag and a target. Particularly useful for
        //# OrphanBuilder.

        long src = srcSegment.get(srcOffset);
        long srcTarget = srcSegment.get(srcTargetOffset);

        if (dstSegment == srcSegment) {
            //# Same segment, so create a direct pointer.

            if (WirePointer.kind(src) == WirePointer.STRUCT && StructPointer.wordSize(src) == 0) {
                WirePointer.setKindAndTargetForEmptyStruct(dstSegment.buffer, dstOffset);
            } else {
                WirePointer.setKindAndTarget(dstSegment.buffer, dstOffset,
                                             WirePointer.kind(src), srcTargetOffset);
            }
            // We can just copy the upper 32 bits.
            dstSegment.buffer.putInt(dstOffset * Constants.BYTES_PER_WORD + 4,
                                     srcSegment.buffer.getInt(srcOffset * Constants.BYTES_PER_WORD + 4));

        } else {
            //# Need to create a far pointer. Try to allocate it in the same segment as the source,
            //# so that it doesn't need to be a double-far.

            int landingPadOffset = srcSegment.allocate(1);
            if (landingPadOffset == SegmentBuilder.FAILED_ALLOCATION) {
                //# Darn, need a double-far.

                BuilderArena.AllocateResult allocation = srcSegment.getArena().allocate(2);
                SegmentBuilder farSegment = allocation.segment;
                landingPadOffset = allocation.offset;

                FarPointer.set(farSegment.buffer, landingPadOffset, false, srcTargetOffset);
                FarPointer.setSegmentId(farSegment.buffer, landingPadOffset, srcSegment.id);

                WirePointer.setKindWithZeroOffset(farSegment.buffer, landingPadOffset + 1,
                                                  WirePointer.kind(srcTarget));

                farSegment.buffer.putInt((landingPadOffset + 1) * Constants.BYTES_PER_WORD + 4,
                                         srcSegment.buffer.getInt(srcOffset * Constants.BYTES_PER_WORD + 4));

                FarPointer.set(dstSegment.buffer, dstOffset,
                               true, landingPadOffset);
                FarPointer.setSegmentId(dstSegment.buffer, dstOffset,
                                        farSegment.id);
            } else {
                //# Simple landing pad is just a pointer.
                WirePointer.setKindAndTarget(srcSegment.buffer, landingPadOffset,
                                             WirePointer.kind(srcTarget), srcTargetOffset);
                srcSegment.buffer.putInt(landingPadOffset * Constants.BYTES_PER_WORD + 4,
                                         srcSegment.buffer.getInt(srcOffset * Constants.BYTES_PER_WORD + 4));

                FarPointer.set(dstSegment.buffer, dstOffset,
                               false, landingPadOffset);
                FarPointer.setSegmentId(dstSegment.buffer, dstOffset,
                                        srcSegment.id);
            }
        }

    }

    static <T> T initStructPointer(StructBuilder.Factory<T> factory,
                                   int refOffset,
                                   SegmentBuilder segment,
                                   StructSize size) {
        AllocateResult allocation = allocate(refOffset, segment, size.total(), WirePointer.STRUCT);
        StructPointer.setFromStructSize(allocation.segment.buffer, allocation.refOffset, size);
        return factory.constructBuilder(allocation.segment, allocation.ptr * Constants.BYTES_PER_WORD,
                                         allocation.ptr + size.data,
                                         size.data * 64, size.pointers);
    }

    static <T> T getWritableStructPointer(StructBuilder.Factory<T> factory,
                                          int refOffset,
                                          SegmentBuilder segment,
                                          StructSize size,
                                          SegmentReader defaultSegment,
                                          int defaultOffset) {
        long ref = segment.get(refOffset);
        int target = WirePointer.target(refOffset, ref);
        if (WirePointer.isNull(ref)) {
            if (defaultSegment == null) {
                return initStructPointer(factory, refOffset, segment, size);
            } else {
                throw new Error("unimplemented");
            }
        }
        FollowBuilderFarsResult resolved = followBuilderFars(ref, target, segment);

        short oldDataSize = StructPointer.dataSize(resolved.ref);
        short oldPointerCount = StructPointer.ptrCount(resolved.ref);
        int oldPointerSection = resolved.ptr + oldDataSize;

        if (oldDataSize < size.data || oldPointerCount < size.pointers) {
            //# The space allocated for this struct is too small. Unlike with readers, we can't just
            //# run with it and do bounds checks at access time, because how would we handle writes?
            //# Instead, we have to copy the struct to a new space now.

            short newDataSize = (short)Math.max(oldDataSize, size.data);
            short newPointerCount = (short)Math.max(oldPointerCount, size.pointers);
            int totalSize = newDataSize + newPointerCount * Constants.WORDS_PER_POINTER;

            //# Don't let allocate() zero out the object just yet.
            zeroPointerAndFars(segment, refOffset);

            AllocateResult allocation = allocate(refOffset, segment,
                                                 totalSize, WirePointer.STRUCT);

            StructPointer.set(allocation.segment.buffer, allocation.refOffset,
                              newDataSize, newPointerCount);

            //# Copy data section.
            memcpy(allocation.segment.buffer, allocation.ptr * Constants.BYTES_PER_WORD,
                   resolved.segment.buffer, resolved.ptr * Constants.BYTES_PER_WORD,
                   oldDataSize * Constants.BYTES_PER_WORD);

            //# Copy pointer section.
            int newPointerSection = allocation.ptr + newDataSize;
            for (int ii = 0; ii < oldPointerCount; ++ii) {
                transferPointer(allocation.segment, newPointerSection + ii,
                                resolved.segment, oldPointerSection + ii);
            }

            //# Zero out old location.  This has two purposes:
            //# 1) We don't want to leak the original contents of the struct when the message is written
            //#    out as it may contain secrets that the caller intends to remove from the new copy.
            //# 2) Zeros will be deflated by packing, making this dead memory almost-free if it ever
            //#    hits the wire.
            memset(resolved.segment.buffer, resolved.ptr * Constants.BYTES_PER_WORD, (byte)0,
                   (oldDataSize + oldPointerCount * Constants.WORDS_PER_POINTER) * Constants.BYTES_PER_WORD);

            return factory.constructBuilder(allocation.segment, allocation.ptr * Constants.BYTES_PER_WORD,
                                            newPointerSection, newDataSize * Constants.BITS_PER_WORD,
                                            newPointerCount);
        } else {
            return factory.constructBuilder(resolved.segment, resolved.ptr * Constants.BYTES_PER_WORD,
                                            oldPointerSection, oldDataSize * Constants.BITS_PER_WORD,
                                            oldPointerCount);
        }

    }

    static <T> T initListPointer(ListBuilder.Factory<T> factory,
                                 int refOffset,
                                 SegmentBuilder segment,
                                 int elementCount,
                                 byte elementSize) {
        assert elementSize != ElementSize.INLINE_COMPOSITE : "Should have called initStructListPointer instead";

        int dataSize = ElementSize.dataBitsPerElement(elementSize);
        int pointerCount = ElementSize.pointersPerElement(elementSize);
        int step = dataSize + pointerCount * Constants.BITS_PER_POINTER;
        int wordCount = roundBitsUpToWords((long)elementCount * (long)step);
        AllocateResult allocation = allocate(refOffset, segment, wordCount, WirePointer.LIST);

        ListPointer.set(allocation.segment.buffer, allocation.refOffset, elementSize, elementCount);

        return factory.constructBuilder(allocation.segment,
                                        allocation.ptr * Constants.BYTES_PER_WORD,
                                        elementCount, step, dataSize, (short)pointerCount);
    }

    static <T> T initStructListPointer(ListBuilder.Factory<T> factory,
                                       int refOffset,
                                       SegmentBuilder segment,
                                       int elementCount,
                                       StructSize elementSize) {
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

        return factory.constructBuilder(allocation.segment,
                                        (allocation.ptr + 1) * Constants.BYTES_PER_WORD,
                                        elementCount, wordsPerElement * Constants.BITS_PER_WORD,
                                        elementSize.data * Constants.BITS_PER_WORD, elementSize.pointers);
    }

    static <T> T getWritableListPointer(ListBuilder.Factory<T> factory,
                                        int origRefOffset,
                                        SegmentBuilder origSegment,
                                        byte elementSize,
                                        SegmentReader defaultSegment,
                                        int defaultOffset) {
        assert elementSize != ElementSize.INLINE_COMPOSITE : "Use getStructList{Element,Field} for structs";

        long origRef = origSegment.get(origRefOffset);
        int origRefTarget = WirePointer.target(origRefOffset, origRef);

        if (WirePointer.isNull(origRef)) {
            throw new Error("unimplemented");
        }

        //# We must verify that the pointer has the right size. Unlike
        //# in getWritableStructListPointer(), we never need to
        //# "upgrade" the data, because this method is called only for
        //# non-struct lists, and there is no allowed upgrade path *to*
        //# a non-struct list, only *from* them.

        FollowBuilderFarsResult resolved = followBuilderFars(origRef, origRefTarget, origSegment);

        if (WirePointer.kind(resolved.ref) != WirePointer.LIST) {
            throw new DecodeException("Called getList{Field,Element}() but existing pointer is not a list");
        }

        byte oldSize = ListPointer.elementSize(resolved.ref);

        if (oldSize == ElementSize.INLINE_COMPOSITE) {
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
            int dataSize = ElementSize.dataBitsPerElement(oldSize);
            int pointerCount = ElementSize.pointersPerElement(oldSize);

            if (dataSize < ElementSize.dataBitsPerElement(elementSize)) {
                throw new DecodeException("Existing list value is incompatible with expected type.");
            }
            if (pointerCount < ElementSize.pointersPerElement(elementSize)) {
                throw new DecodeException("Existing list value is incompatible with expected type.");
            }

            int step = dataSize + pointerCount * Constants.BITS_PER_POINTER;

            return factory.constructBuilder(resolved.segment, resolved.ptr * Constants.BYTES_PER_WORD,
                                            ListPointer.elementCount(resolved.ref),
                                            step, dataSize, (short) pointerCount);
        }
    }

    static <T> T getWritableStructListPointer(ListBuilder.Factory<T> factory,
                                              int origRefOffset,
                                              SegmentBuilder origSegment,
                                              StructSize elementSize,
                                              SegmentReader defaultSegment,
                                              int defaultOffset) {
        long origRef = origSegment.get(origRefOffset);
        int origRefTarget = WirePointer.target(origRefOffset, origRef);

        if (WirePointer.isNull(origRef)) {
            throw new Error("unimplemented");
        }

        //# We must verify that the pointer has the right size and potentially upgrade it if not.

        FollowBuilderFarsResult resolved = followBuilderFars(origRef, origRefTarget, origSegment);
        if (WirePointer.kind(resolved.ref) != WirePointer.LIST) {
            throw new DecodeException("Called getList{Field,Element}() but existing pointer is not a list");
        }

        byte oldSize = ListPointer.elementSize(resolved.ref);

        if (oldSize == ElementSize.INLINE_COMPOSITE) {
            //# Existing list is INLINE_COMPOSITE, but we need to verify that the sizes match.
            long oldTag = resolved.segment.get(resolved.ptr);
            int oldPtr = resolved.ptr + Constants.POINTER_SIZE_IN_WORDS;
            if (WirePointer.kind(oldTag) != WirePointer.STRUCT) {
                throw new DecodeException("INLINE_COMPOSITE list with non-STRUCT elements not supported.");
            }
            int oldDataSize = StructPointer.dataSize(oldTag);
            short oldPointerCount = StructPointer.ptrCount(oldTag);
            int oldStep = (oldDataSize + oldPointerCount * Constants.POINTER_SIZE_IN_WORDS);
            int elementCount = WirePointer.inlineCompositeListElementCount(oldTag);

            if (oldDataSize >= elementSize.data && oldPointerCount >= elementSize.pointers) {
                //# Old size is at least as large as we need. Ship it.
                return factory.constructBuilder(resolved.segment, oldPtr * Constants.BYTES_PER_WORD,
                                                elementCount,
                                                oldStep * Constants.BITS_PER_WORD,
                                                oldDataSize * Constants.BITS_PER_WORD, oldPointerCount);
            }

            //# The structs in this list are smaller than expected, probably written using an older
            //# version of the protocol. We need to make a copy and expand them.

            short newDataSize = (short)Math.max(oldDataSize, elementSize.data);
            short newPointerCount = (short)Math.max(oldPointerCount, elementSize.pointers);
            int newStep = newDataSize + newPointerCount * Constants.WORDS_PER_POINTER;
            int totalSize = newStep * elementCount;

            //# Don't let allocate() zero out the object just yet.
            zeroPointerAndFars(origSegment, origRefOffset);

            AllocateResult allocation = allocate(origRefOffset, origSegment,
                                                 totalSize + Constants.POINTER_SIZE_IN_WORDS,
                                                 WirePointer.LIST);

            ListPointer.setInlineComposite(allocation.segment.buffer, allocation.refOffset, totalSize);

            long tag = allocation.segment.get(allocation.ptr);
            WirePointer.setKindAndInlineCompositeListElementCount(
                allocation.segment.buffer, allocation.ptr,
                WirePointer.STRUCT, elementCount);
            StructPointer.set(allocation.segment.buffer, allocation.ptr,
                              newDataSize, newPointerCount);
            int newPtr = allocation.ptr + Constants.POINTER_SIZE_IN_WORDS;

            int src = oldPtr;
            int dst = newPtr;
            for (int ii = 0; ii < elementCount; ++ii) {
                //# Copy data section.
                memcpy(allocation.segment.buffer, dst * Constants.BYTES_PER_WORD,
                       resolved.segment.buffer, src * Constants.BYTES_PER_WORD,
                       oldDataSize * Constants.BYTES_PER_WORD);

                //# Copy pointer section.
                int newPointerSection = dst + newDataSize;
                int oldPointerSection = src + oldDataSize;
                for (int jj = 0; jj < oldPointerCount; ++jj) {
                    transferPointer(allocation.segment, newPointerSection + jj,
                                    resolved.segment, oldPointerSection + jj);
                }

                dst += newStep;
                src += oldStep;
            }

            //# Zero out old location. See explanation in getWritableStructPointer().
            memset(resolved.segment.buffer, resolved.ptr * Constants.BYTES_PER_WORD,
                   (byte)0, oldStep * elementCount * Constants.BYTES_PER_WORD);

            return factory.constructBuilder(allocation.segment, newPtr * Constants.BYTES_PER_WORD,
                                            elementCount,
                                            newStep * Constants.BITS_PER_WORD,
                                            newDataSize * Constants.BITS_PER_WORD,
                                            newPointerCount);
        } else {
            //# We're upgrading from a non-struct list.

            int oldDataSize = ElementSize.dataBitsPerElement(oldSize);
            int oldPointerCount = ElementSize.pointersPerElement(oldSize);
            int oldStep = oldDataSize + oldPointerCount * Constants.BITS_PER_POINTER;
            int elementCount = ListPointer.elementCount(origRef);

            if (oldSize == ElementSize.VOID) {
                //# Nothing to copy, just allocate a new list.
                return initStructListPointer(factory, origRefOffset, origSegment,
                                             elementCount, elementSize);
            } else {
                //# Upgrading to an inline composite list.

                if (oldSize == ElementSize.BIT) {
                    throw new Error("Found bit list where struct list was expected; " +
                                    "upgrading boolean lists to struct is no longer supported.");
                }

                short newDataSize = elementSize.data;
                short newPointerCount = elementSize.pointers;

                if (oldSize == ElementSize.POINTER) {
                    newPointerCount = (short)Math.max(newPointerCount, 1);
                } else {
                    //# Old list contains data elements, so we need at least 1 word of data.
                    newDataSize = (short)Math.max(newDataSize, 1);
                }

                int newStep = (newDataSize + newPointerCount * Constants.WORDS_PER_POINTER);
                int totalWords = elementCount * newStep;

                //# Don't let allocate() zero out the object just yet.
                zeroPointerAndFars(origSegment, origRefOffset);

                AllocateResult allocation = allocate(origRefOffset, origSegment,
                                                     totalWords + Constants.POINTER_SIZE_IN_WORDS,
                                                     WirePointer.LIST);

                ListPointer.setInlineComposite(allocation.segment.buffer, allocation.refOffset, totalWords);

                long tag = allocation.segment.get(allocation.ptr);
                WirePointer.setKindAndInlineCompositeListElementCount(
                    allocation.segment.buffer, allocation.ptr,
                    WirePointer.STRUCT, elementCount);
                StructPointer.set(allocation.segment.buffer, allocation.ptr,
                                  newDataSize, newPointerCount);
                int newPtr = allocation.ptr + Constants.POINTER_SIZE_IN_WORDS;

                if (oldSize == ElementSize.POINTER) {
                    int dst = newPtr + newDataSize;
                    int src = resolved.ptr;
                    for (int ii = 0; ii < elementCount; ++ii) {
                        transferPointer(origSegment, dst, resolved.segment, src);
                        dst += newStep / Constants.WORDS_PER_POINTER;
                        src += 1;
                    }
                } else {
                    int dst = newPtr;
                    int srcByteOffset = resolved.ptr * Constants.BYTES_PER_WORD;
                    int oldByteStep = oldDataSize / Constants.BITS_PER_BYTE;
                    for (int ii = 0; ii < elementCount; ++ii) {
                        memcpy(allocation.segment.buffer, dst * Constants.BYTES_PER_WORD,
                               resolved.segment.buffer, srcByteOffset, oldByteStep);
                        srcByteOffset += oldByteStep;
                        dst += newStep;
                    }
                }

                //# Zero out old location. See explanation in getWritableStructPointer().
                memset(resolved.segment.buffer, resolved.ptr * Constants.BYTES_PER_WORD,
                       (byte)0, roundBitsUpToBytes(oldStep * elementCount));

                return factory.constructBuilder(allocation.segment, newPtr * Constants.BYTES_PER_WORD,
                                                elementCount,
                                                newStep * Constants.BITS_PER_WORD,
                                                newDataSize * Constants.BITS_PER_WORD,
                                                newPointerCount);
            }
        }
    }

    // size is in bytes
    static Text.Builder initTextPointer(int refOffset,
                                        SegmentBuilder segment,
                                        int size) {
        //# The byte list must include a NUL terminator.
        int byteSize = size + 1;

        //# Allocate the space.
        AllocateResult allocation = allocate(refOffset, segment, roundBytesUpToWords(byteSize),
                                             WirePointer.LIST);

        //# Initialize the pointer.
        ListPointer.set(allocation.segment.buffer, allocation.refOffset, ElementSize.BYTE, byteSize);

        return new Text.Builder(allocation.segment.buffer, allocation.ptr * Constants.BYTES_PER_WORD, size);
    }

    static Text.Builder setTextPointer(int refOffset,
                                       SegmentBuilder segment,
                                       Text.Reader value) {
        Text.Builder builder = initTextPointer(refOffset, segment, value.size);

        ByteBuffer slice = value.buffer.duplicate();
        slice.position(value.offset);
        slice.limit(value.offset + value.size);
        builder.buffer.position(builder.offset);
        builder.buffer.put(slice);
        return builder;
    }

    static Text.Builder getWritableTextPointer(int refOffset,
                                               SegmentBuilder segment,
                                               ByteBuffer defaultBuffer,
                                               int defaultOffset,
                                               int defaultSize) {
        long ref = segment.get(refOffset);

        if (WirePointer.isNull(ref)) {
            if (defaultBuffer == null) {
                return new Text.Builder();
            } else {
                Text.Builder builder = initTextPointer(refOffset, segment, defaultSize);
                // TODO is there a way to do this with bulk methods?
                for (int i = 0; i < builder.size; ++i) {
                    builder.buffer.put(builder.offset + i, defaultBuffer.get(defaultOffset * 8 + i));
                }
                return builder;
            }
        }

        int refTarget = WirePointer.target(refOffset, ref);
        FollowBuilderFarsResult resolved = followBuilderFars(ref, refTarget, segment);

        if (WirePointer.kind(resolved.ref) != WirePointer.LIST) {
            throw new DecodeException("Called getText{Field,Element} but existing pointer is not a list.");
        }
        if (ListPointer.elementSize(resolved.ref) != ElementSize.BYTE) {
            throw new DecodeException(
                "Called getText{Field,Element} but existing list pointer is not byte-sized.");
        }


        int size = ListPointer.elementCount(resolved.ref);
        if (size == 0 ||
            resolved.segment.buffer.get(resolved.ptr * Constants.BYTES_PER_WORD + size - 1) != 0) {
            throw new DecodeException("Text blob missing NUL terminator.");
        }
        return new Text.Builder(resolved.segment.buffer, resolved.ptr * Constants.BYTES_PER_WORD,
                                size - 1);

    }

    // size is in bytes
    static Data.Builder initDataPointer(int refOffset,
                                        SegmentBuilder segment,
                                        int size) {
        //# Allocate the space.
        AllocateResult allocation = allocate(refOffset, segment, roundBytesUpToWords(size),
                                             WirePointer.LIST);

        //# Initialize the pointer.
        ListPointer.set(allocation.segment.buffer, allocation.refOffset, ElementSize.BYTE, size);

        return new Data.Builder(allocation.segment.buffer, allocation.ptr * Constants.BYTES_PER_WORD, size);
    }

    static Data.Builder setDataPointer(int refOffset,
                                       SegmentBuilder segment,
                                       Data.Reader value) {
        Data.Builder builder = initDataPointer(refOffset, segment, value.size);

        // TODO is there a way to do this with bulk methods?
        for (int i = 0; i < builder.size; ++i) {
            builder.buffer.put(builder.offset + i, value.buffer.get(value.offset + i));
        }
        return builder;
    }

    static Data.Builder getWritableDataPointer(int refOffset,
                                               SegmentBuilder segment,
                                               ByteBuffer defaultBuffer,
                                               int defaultOffset,
                                               int defaultSize) {
        long ref = segment.get(refOffset);

        if (WirePointer.isNull(ref)) {
            if (defaultBuffer == null) {
                return new Data.Builder();
            } else {
                Data.Builder builder = initDataPointer(refOffset, segment, defaultSize);
                // TODO is there a way to do this with bulk methods?
                for (int i = 0; i < builder.size; ++i) {
                    builder.buffer.put(builder.offset + i, defaultBuffer.get(defaultOffset * 8 + i));
                }
                return builder;
            }
        }

        int refTarget = WirePointer.target(refOffset, ref);
        FollowBuilderFarsResult resolved = followBuilderFars(ref, refTarget, segment);

        if (WirePointer.kind(resolved.ref) != WirePointer.LIST) {
            throw new DecodeException("Called getData{Field,Element} but existing pointer is not a list.");
        }
        if (ListPointer.elementSize(resolved.ref) != ElementSize.BYTE) {
            throw new DecodeException(
                "Called getData{Field,Element} but existing list pointer is not byte-sized.");
        }

        return new Data.Builder(resolved.segment.buffer, resolved.ptr * Constants.BYTES_PER_WORD,
                                ListPointer.elementCount(resolved.ref));

    }

    static <T> T readStructPointer(StructReader.Factory<T> factory,
                                   SegmentReader segment,
                                   int refOffset,
                                   SegmentReader defaultSegment,
                                   int defaultOffset,
                                   int nestingLimit) {
        long ref = segment.get(refOffset);
        if (WirePointer.isNull(ref)) {
            if (defaultSegment == null) {
                return factory.constructReader(SegmentReader.EMPTY, 0, 0, 0, (short) 0, 0x7fffffff);
            } else {
                segment = defaultSegment;
                refOffset = defaultOffset;
                ref = segment.get(refOffset);
            }
        }

        if (nestingLimit <= 0) {
            throw new DecodeException("Message is too deeply nested or contains cycles.");
        }

        int refTarget = WirePointer.target(refOffset, ref);
        FollowFarsResult resolved = followFars(ref, refTarget, segment);

        int dataSizeWords = StructPointer.dataSize(resolved.ref);

        if (WirePointer.kind(resolved.ref) != WirePointer.STRUCT) {
            throw new DecodeException("Message contains non-struct pointer where struct pointer was expected.");
        }

        resolved.segment.arena.checkReadLimit(StructPointer.wordSize(resolved.ref));

        return factory.constructReader(resolved.segment,
                                        resolved.ptr * Constants.BYTES_PER_WORD,
                                        (resolved.ptr + dataSizeWords),
                                        dataSizeWords * Constants.BITS_PER_WORD,
                                        StructPointer.ptrCount(resolved.ref),
                                        nestingLimit - 1);

    }

    static SegmentBuilder setStructPointer(SegmentBuilder segment, int refOffset, StructReader value) {
        short dataSize = (short)roundBitsUpToWords(value.dataSize);
        int totalSize = dataSize + value.pointerCount * Constants.POINTER_SIZE_IN_WORDS;

        AllocateResult allocation = allocate(refOffset, segment, totalSize, WirePointer.STRUCT);
        StructPointer.set(allocation.segment.buffer, allocation.refOffset,
                          dataSize, value.pointerCount);

        if (value.dataSize == 1) {
            throw new Error("single bit case not handled");
        } else {
            memcpy(allocation.segment.buffer, allocation.ptr * Constants.BYTES_PER_WORD,
                   value.segment.buffer, value.data, value.dataSize / Constants.BITS_PER_BYTE);
        }

        int pointerSection = allocation.ptr + dataSize;
        for (int i = 0; i < value.pointerCount; ++i) {
            copyPointer(allocation.segment, pointerSection + i, value.segment, value.pointers + i,
                        value.nestingLimit);
        }
        return allocation.segment;
    };

    static SegmentBuilder setListPointer(SegmentBuilder segment, int refOffset, ListReader value) {
        int totalSize = roundBitsUpToWords(value.elementCount * value.step);

        if (value.step <= Constants.BITS_PER_WORD) {
            //# List of non-structs.
            AllocateResult allocation = allocate(refOffset, segment, totalSize, WirePointer.LIST);

            if (value.structPointerCount == 1) {
                //# List of pointers.
                ListPointer.set(allocation.segment.buffer, allocation.refOffset, ElementSize.POINTER, value.elementCount);
                for (int i = 0; i < value.elementCount; ++i) {
                    copyPointer(allocation.segment, allocation.ptr + i,
                                value.segment, value.ptr / Constants.BYTES_PER_WORD + i, value.nestingLimit);
                }
            } else {
                //# List of data.
                byte elementSize = ElementSize.VOID;
                switch (value.step) {
                case 0: elementSize = ElementSize.VOID; break;
                case 1: elementSize = ElementSize.BIT; break;
                case 8: elementSize = ElementSize.BYTE; break;
                case 16: elementSize = ElementSize.TWO_BYTES; break;
                case 32: elementSize = ElementSize.FOUR_BYTES; break;
                case 64: elementSize = ElementSize.EIGHT_BYTES; break;
                default:
                    throw new Error("invalid list step size: " + value.step);
                }

                ListPointer.set(allocation.segment.buffer, allocation.refOffset, elementSize, value.elementCount);
                memcpy(allocation.segment.buffer, allocation.ptr * Constants.BYTES_PER_WORD,
                       value.segment.buffer, value.ptr, totalSize * Constants.BYTES_PER_WORD);
            }
            return allocation.segment;
        } else {
            //# List of structs.
            AllocateResult allocation = allocate(refOffset, segment, totalSize + Constants.POINTER_SIZE_IN_WORDS, WirePointer.LIST);
            ListPointer.setInlineComposite(allocation.segment.buffer, allocation.refOffset, totalSize);
            short dataSize = (short)roundBitsUpToWords(value.structDataSize);
            short pointerCount = value.structPointerCount;

            WirePointer.setKindAndInlineCompositeListElementCount(allocation.segment.buffer, allocation.ptr,
                                                                  WirePointer.STRUCT, value.elementCount);
            StructPointer.set(allocation.segment.buffer, allocation.ptr,
                              dataSize, pointerCount);

            int dstOffset = allocation.ptr + Constants.POINTER_SIZE_IN_WORDS;
            int srcOffset = value.ptr / Constants.BYTES_PER_WORD;

            for (int i = 0; i < value.elementCount; ++i) {
                memcpy(allocation.segment.buffer, dstOffset * Constants.BYTES_PER_WORD,
                       value.segment.buffer, srcOffset * Constants.BYTES_PER_WORD,
                       value.structDataSize / Constants.BITS_PER_BYTE);
                dstOffset += dataSize;
                srcOffset += dataSize;

                for (int j = 0; j < pointerCount; ++j) {
                    copyPointer(allocation.segment, dstOffset, value.segment, srcOffset, value.nestingLimit);
                    dstOffset += Constants.POINTER_SIZE_IN_WORDS;
                    srcOffset += Constants.POINTER_SIZE_IN_WORDS;
                }
            }
            return allocation.segment;
        }
    }

    static void memset(ByteBuffer dstBuffer, int dstByteOffset, byte value, int length) {
        // TODO we can probably do this faster
        for (int ii = dstByteOffset; ii < dstByteOffset + length; ++ii) {
            dstBuffer.put(ii, value);
        }
    }

    static void memcpy(ByteBuffer dstBuffer, int dstByteOffset, ByteBuffer srcBuffer, int srcByteOffset, int length) {
        ByteBuffer dstDup = dstBuffer.duplicate();
        dstDup.position(dstByteOffset);
        dstDup.limit(dstByteOffset + length);
        ByteBuffer srcDup = srcBuffer.duplicate();
        srcDup.position(srcByteOffset);
        srcDup.limit(srcByteOffset + length);
        dstDup.put(srcDup);
    }

    static SegmentBuilder copyPointer(SegmentBuilder dstSegment, int dstOffset,
                                      SegmentReader srcSegment, int srcOffset, int nestingLimit) {
        // Deep-copy the object pointed to by src into dst.  It turns out we can't reuse
        // readStructPointer(), etc. because they do type checking whereas here we want to accept any
        // valid pointer.

        long srcRef = srcSegment.get(srcOffset);

        if (WirePointer.isNull(srcRef)) {
            dstSegment.buffer.putLong(dstOffset * 8, 0L);
            return dstSegment;
        }

        int srcTarget = WirePointer.target(srcOffset, srcRef);
        FollowFarsResult resolved = followFars(srcRef, srcTarget, srcSegment);

        switch (WirePointer.kind(resolved.ref)) {
        case WirePointer.STRUCT :
            if (nestingLimit <= 0) {
                throw new DecodeException("Message is too deeply nested or contains cycles. See org.capnproto.ReaderOptions.");
            }
            resolved.segment.arena.checkReadLimit(StructPointer.wordSize(resolved.ref));
            return setStructPointer(dstSegment, dstOffset,
                                    new StructReader(resolved.segment,
                                                     resolved.ptr * Constants.BYTES_PER_WORD,
                                                     resolved.ptr + StructPointer.dataSize(resolved.ref),
                                                     StructPointer.dataSize(resolved.ref) * Constants.BITS_PER_WORD,
                                                     StructPointer.ptrCount(resolved.ref),
                                                     nestingLimit - 1));
        case WirePointer.LIST :
            byte elementSize = ListPointer.elementSize(resolved.ref);
            if (nestingLimit <= 0) {
                throw new DecodeException("Message is too deeply nested or contains cycles. See org.capnproto.ReaderOptions.");
            }
            if (elementSize == ElementSize.INLINE_COMPOSITE) {
                int wordCount = ListPointer.inlineCompositeWordCount(resolved.ref);
                long tag = resolved.segment.get(resolved.ptr);
                int ptr = resolved.ptr + 1;

                resolved.segment.arena.checkReadLimit(wordCount + 1);

                if (WirePointer.kind(tag) != WirePointer.STRUCT) {
                    throw new DecodeException("INLINE_COMPOSITE lists of non-STRUCT type are not supported.");
                }

                int elementCount = WirePointer.inlineCompositeListElementCount(tag);
                int wordsPerElement = StructPointer.wordSize(tag);
                if ((long)wordsPerElement * elementCount > wordCount) {
                    throw new DecodeException("INLINE_COMPOSITE list's elements overrun its word count.");
                }

                if (wordsPerElement == 0) {
                    // Watch out for lists of zero-sized structs, which can claim to be arbitrarily
                    // large without having sent actual data.
                    resolved.segment.arena.checkReadLimit(elementCount);
                }

                return setListPointer(dstSegment, dstOffset,
                                      new ListReader(resolved.segment,
                                                     ptr * Constants.BYTES_PER_WORD,
                                                     elementCount,
                                                     wordsPerElement * Constants.BITS_PER_WORD,
                                                     StructPointer.dataSize(tag) * Constants.BITS_PER_WORD,
                                                     StructPointer.ptrCount(tag),
                                                     nestingLimit - 1));
            } else {
                int dataSize = ElementSize.dataBitsPerElement(elementSize);
                short pointerCount = ElementSize.pointersPerElement(elementSize);
                int step = dataSize + pointerCount * Constants.BITS_PER_POINTER;
                int elementCount = ListPointer.elementCount(resolved.ref);
                int wordCount = roundBitsUpToWords((long) elementCount * step);

                resolved.segment.arena.checkReadLimit(wordCount);

                if (elementSize == ElementSize.VOID) {
                    // Watch out for lists of void, which can claim to be arbitrarily large without
                    // having sent actual data.
                    resolved.segment.arena.checkReadLimit(elementCount);
                }

                return setListPointer(dstSegment, dstOffset,
                                      new ListReader(resolved.segment,
                                                     resolved.ptr * Constants.BYTES_PER_WORD,
                                                     elementCount,
                                                     step,
                                                     dataSize,
                                                     pointerCount,
                                                     nestingLimit - 1));
            }

        case WirePointer.FAR :
            throw new Error("Far pointer should have been handled above.");
        case WirePointer.OTHER :
            throw new Error("copyPointer is unimplemented");
        }
        throw new Error("unreachable");
    }

    static <T> T readListPointer(ListReader.Factory<T> factory,
                                 SegmentReader segment,
                                 int refOffset,
                                 SegmentReader defaultSegment,
                                 int defaultOffset,
                                 byte expectedElementSize,
                                 int nestingLimit) {

        long ref = segment.get(refOffset);

        if (WirePointer.isNull(ref)) {
            if (defaultSegment == null) {
                return factory.constructReader(SegmentReader.EMPTY, 0, 0, 0, 0, (short) 0, 0x7fffffff);
            } else {
                segment = defaultSegment;
                refOffset = defaultOffset;
                ref = segment.get(refOffset);
            }
        }

        if (nestingLimit <= 0) {
            throw new Error("nesting limit exceeded");
        }

        int refTarget = WirePointer.target(refOffset, ref);

        FollowFarsResult resolved = followFars(ref, refTarget, segment);

        byte elementSize = ListPointer.elementSize(resolved.ref);
        switch (elementSize) {
        case ElementSize.INLINE_COMPOSITE : {
            int wordCount = ListPointer.inlineCompositeWordCount(resolved.ref);

            long tag = resolved.segment.get(resolved.ptr);
            int ptr = resolved.ptr + 1;

            resolved.segment.arena.checkReadLimit(wordCount + 1);

            int size = WirePointer.inlineCompositeListElementCount(tag);

            int wordsPerElement = StructPointer.wordSize(tag);

            if ((long)size * wordsPerElement > wordCount) {
                throw new DecodeException("INLINE_COMPOSITE list's elements overrun its word count.");
            }

            if (wordsPerElement == 0) {
                // Watch out for lists of zero-sized structs, which can claim to be arbitrarily
                // large without having sent actual data.
                resolved.segment.arena.checkReadLimit(size);
            }

            // TODO check whether the size is compatible

            return factory.constructReader(resolved.segment,
                                             ptr * Constants.BYTES_PER_WORD,
                                             size,
                                             wordsPerElement * Constants.BITS_PER_WORD,
                                             StructPointer.dataSize(tag) * Constants.BITS_PER_WORD,
                                             StructPointer.ptrCount(tag),
                                             nestingLimit - 1);
        }
        default : {
            //# This is a primitive or pointer list, but all such
            //# lists can also be interpreted as struct lists. We
            //# need to compute the data size and pointer count for
            //# such structs.
            int dataSize = ElementSize.dataBitsPerElement(ListPointer.elementSize(resolved.ref));
            int pointerCount = ElementSize.pointersPerElement(ListPointer.elementSize(resolved.ref));
            int elementCount = ListPointer.elementCount(resolved.ref);
            int step = dataSize + pointerCount * Constants.BITS_PER_POINTER;

            resolved.segment.arena.checkReadLimit(
                roundBitsUpToWords(elementCount * step));

            if (elementSize == ElementSize.VOID) {
                // Watch out for lists of void, which can claim to be arbitrarily large without
                // having sent actual data.
                resolved.segment.arena.checkReadLimit(elementCount);
            }

            //# Verify that the elements are at least as large as
            //# the expected type. Note that if we expected
            //# InlineComposite, the expected sizes here will be
            //# zero, because bounds checking will be performed at
            //# field access time. So this check here is for the
            //# case where we expected a list of some primitive or
            //# pointer type.

            int expectedDataBitsPerElement = ElementSize.dataBitsPerElement(expectedElementSize);
            int expectedPointersPerElement = ElementSize.pointersPerElement(expectedElementSize);

            if (expectedDataBitsPerElement > dataSize) {
                throw new DecodeException("Message contains list with incompatible element type.");
            }

            if (expectedPointersPerElement > pointerCount) {
                throw new DecodeException("Message contains list with incompatible element type.");
            }

            return factory.constructReader(resolved.segment,
                                             resolved.ptr * Constants.BYTES_PER_WORD,
                                             ListPointer.elementCount(resolved.ref),
                                             step,
                                             dataSize,
                                             (short)pointerCount,
                                             nestingLimit - 1);
        }
        }
    }

    static Text.Reader readTextPointer(SegmentReader segment,
                                       int refOffset,
                                       ByteBuffer defaultBuffer,
                                       int defaultOffset,
                                       int defaultSize) {
        long ref = segment.get(refOffset);

        if (WirePointer.isNull(ref)) {
            if (defaultBuffer == null) {
                return new Text.Reader();
            } else {
                return new Text.Reader(defaultBuffer, defaultOffset, defaultSize);
            }
        }

        int refTarget = WirePointer.target(refOffset, ref);

        FollowFarsResult resolved = followFars(ref, refTarget, segment);

        int size = ListPointer.elementCount(resolved.ref);

        if (WirePointer.kind(resolved.ref) != WirePointer.LIST) {
            throw new DecodeException("Message contains non-list pointer where text was expected.");
        }

        if (ListPointer.elementSize(resolved.ref) != ElementSize.BYTE) {
            throw new DecodeException("Message contains list pointer of non-bytes where text was expected.");
        }

        resolved.segment.arena.checkReadLimit(roundBytesUpToWords(size));

        if (size == 0 || resolved.segment.buffer.get(8 * resolved.ptr + size - 1) != 0) {
            throw new DecodeException("Message contains text that is not NUL-terminated.");
        }

        return new Text.Reader(resolved.segment.buffer, resolved.ptr, size - 1);
    }

    static Data.Reader readDataPointer(SegmentReader segment,
                                       int refOffset,
                                       ByteBuffer defaultBuffer,
                                       int defaultOffset,
                                       int defaultSize) {
        long ref = segment.get(refOffset);

        if (WirePointer.isNull(ref)) {
            if (defaultBuffer == null) {
                return new Data.Reader();
            } else {
                return new Data.Reader(defaultBuffer, defaultOffset, defaultSize);
            }
        }

        int refTarget = WirePointer.target(refOffset, ref);

        FollowFarsResult resolved = followFars(ref, refTarget, segment);

        int size = ListPointer.elementCount(resolved.ref);

        if (WirePointer.kind(resolved.ref) != WirePointer.LIST) {
            throw new DecodeException("Message contains non-list pointer where data was expected.");
        }

        if (ListPointer.elementSize(resolved.ref) != ElementSize.BYTE) {
            throw new DecodeException("Message contains list pointer of non-bytes where data was expected.");
        }

        resolved.segment.arena.checkReadLimit(roundBytesUpToWords(size));

        return new Data.Reader(resolved.segment.buffer, resolved.ptr, size);
    }

}
