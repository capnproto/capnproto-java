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

    static FollowFarsResult followFars(long ref, int refTarget, SegmentReader segment) {
        //# If the segment is null, this is an unchecked message,
        //# so there are no FAR pointers.
        if (segment != null && WirePointer.kind(ref) == WirePointer.FAR) {
            SegmentReader resultSegment = segment.arena.tryGetSegment(FarPointer.getSegmentId(ref));

            int padOffset = FarPointer.positionInSegment(ref);
            long pad = WirePointer.get(resultSegment.buffer, padOffset);

            int padWords = FarPointer.isDoubleFar(ref) ? 2 : 1;
            // TODO read limiting

            if (!FarPointer.isDoubleFar(ref)) {

                return new FollowFarsResult(WirePointer.target(padOffset, pad),
                                            pad, resultSegment);
            } else {
                //# Landing pad is another far pointer. It is
                //# followed by a tag describing the pointed-to
                //# object.
                throw new Error("unimplemented");
            }

        } else {
            return new FollowFarsResult(refTarget, ref, segment);
        }
    }

    static void zeroObject(SegmentBuilder segment, int refOffset) {
        //# Zero out the pointed-to object. Use when the pointer is
        //# about to be overwritten making the target object no longer
        //# reachable.

        // TODO
    }


    static <T> T initStructPointer(StructBuilder.Factory<T> factory,
                                   int refOffset,
                                   SegmentBuilder segment,
                                   StructSize size) {
        AllocateResult allocation = allocate(refOffset, segment, size.total(), WirePointer.STRUCT);
        StructPointer.setFromStructSize(allocation.segment.buffer, allocation.refOffset, size);
        return factory.constructBuilder(allocation.segment, allocation.ptr * Constants.BYTES_PER_WORD,
                                         allocation.ptr + size.data,
                                         size.data * 64, size.pointers, (byte)0);
    }

    static <T> T getWritableStructPointer(StructBuilder.Factory<T> factory,
                                          int refOffset,
                                          SegmentBuilder segment,
                                          StructSize size,
                                          SegmentReader defaultSegment,
                                          int defaultOffset) {
        long ref = WirePointer.get(segment.buffer, refOffset);
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
        int oldPointerSectionOffset = resolved.ptr + oldDataSize;

        if (oldDataSize < size.data || oldPointerCount < size.pointers) {
            throw new Error("unimplemented");
        } else {
            return factory.constructBuilder(resolved.segment, resolved.ptr * Constants.BYTES_PER_WORD,
                                            oldPointerSectionOffset, oldDataSize * Constants.BITS_PER_WORD,
                                            oldPointerCount, (byte)0);
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
        if (elementSize.preferredListEncoding != ElementSize.INLINE_COMPOSITE) {
            //# Small data-only struct. Allocate a list of primitives instead.
            return initListPointer(factory, refOffset, segment, elementCount,
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
        throw new Error("getWritableStructListPointer is unimplemented");
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
        long ref = WirePointer.get(segment.buffer, refOffset);

        if (WirePointer.isNull(ref)) {
            if (defaultBuffer == null) {
                return new Text.Builder(null, 0, 0);
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


        //# Subtract 1 from the size for the NUL terminator.
        return new Text.Builder(resolved.segment.buffer, resolved.ptr * Constants.BYTES_PER_WORD,
                                ListPointer.elementCount(resolved.ref) - 1);

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
        long ref = WirePointer.get(segment.buffer, refOffset);

        if (WirePointer.isNull(ref)) {
            if (defaultBuffer == null) {
                return new Data.Builder(ByteBuffer.allocate(0), 0, 0);
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
        long ref = WirePointer.get(segment.buffer, refOffset);
        if (WirePointer.isNull(ref)) {
            if (defaultSegment == null) {
                return factory.constructReader(SegmentReader.EMPTY, 0, 0, 0, (short) 0, (byte) 0, 0x7fffffff);
            } else {
                segment = defaultSegment;
                refOffset = defaultOffset;
                ref = WirePointer.get(segment.buffer, refOffset);
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
                                        (byte)0,
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

        long srcRef = WirePointer.get(srcSegment.buffer, srcOffset);

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
                                                     (byte) 0, nestingLimit - 1));
        case WirePointer.LIST :
            byte elementSize = ListPointer.elementSize(resolved.ref);
            if (nestingLimit <= 0) {
                throw new DecodeException("Message is too deeply nested or contains cycles. See org.capnproto.ReaderOptions.");
            }
            if (elementSize == ElementSize.INLINE_COMPOSITE) {
                int wordCount = ListPointer.inlineCompositeWordCount(resolved.ref);
                long tag = WirePointer.get(resolved.segment.buffer, resolved.ptr);
                int ptr = resolved.ptr + 1;

                resolved.segment.arena.checkReadLimit(wordCount + 1);

                if (WirePointer.kind(tag) != WirePointer.STRUCT) {
                    throw new DecodeException("INLINE_COMPOSITE lists of non-STRUCT type are not supported.");
                }

                int elementCount = WirePointer.inlineCompositeListElementCount(tag);
                int wordsPerElement = StructPointer.wordSize(tag);
                if (wordsPerElement * elementCount > wordCount) {
                    throw new DecodeException("INLINE_COMPOSITE list's elements overrun its word count.");
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

        long ref = WirePointer.get(segment.buffer, refOffset);

        if (WirePointer.isNull(ref)) {
            if (defaultSegment == null) {
                factory.constructReader(SegmentReader.EMPTY, 0, 0, 0, 0, (short) 0, 0x7fffffff);
            } else {
                segment = defaultSegment;
                refOffset = defaultOffset;
                ref = WirePointer.get(segment.buffer, refOffset);
            }
        }

        if (nestingLimit <= 0) {
            throw new Error("nesting limit exceeded");
        }

        int refTarget = WirePointer.target(refOffset, ref);

        FollowFarsResult resolved = followFars(ref, refTarget, segment);

        switch (ListPointer.elementSize(resolved.ref)) {
        case ElementSize.INLINE_COMPOSITE : {
            int wordCount = ListPointer.inlineCompositeWordCount(resolved.ref);

            long tag = WirePointer.get(resolved.segment.buffer, resolved.ptr);
            int ptr = resolved.ptr + 1;

            resolved.segment.arena.checkReadLimit(wordCount + 1);

            int size = WirePointer.inlineCompositeListElementCount(tag);

            int wordsPerElement = StructPointer.wordSize(tag);

            // TODO check that elemements do not overrun word count

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
            int step = dataSize + pointerCount * Constants.BITS_PER_POINTER;

            resolved.segment.arena.checkReadLimit(
                roundBitsUpToWords(ListPointer.elementCount(resolved.ref) * step));

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
        long ref = WirePointer.get(segment.buffer, refOffset);

        if (WirePointer.isNull(ref)) {
            if (defaultBuffer == null) {
                // XXX -- what about null terminator?
                return new Text.Reader(ByteBuffer.wrap(new byte[0]), 0, 0);
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
        long ref = WirePointer.get(segment.buffer, refOffset);

        if (WirePointer.isNull(ref)) {
            if (defaultBuffer == null) {
                return new Data.Reader(ByteBuffer.wrap(new byte[0]), 0, 0);
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
