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

public abstract class ListFactory<Builder, Reader extends ListReader>
    implements ListBuilder.Factory<Builder>,
    FromPointerBuilderRefDefault<Builder>,
    SetPointerBuilder<Builder, Reader>,
    ListReader.Factory<Reader>,
    PointerFactory<Builder, Reader>,
    FromPointerReaderRefDefault<Reader> {

    final byte elementSize;
    ListFactory(byte elementSize) {this.elementSize = elementSize;}

    public final Reader fromPointerReaderRefDefault(SegmentDataContainer segment, int pointer,
                                                    SegmentDataContainer defaultSegment, int defaultOffset,
                                                    int nestingLimit) {
        return WireHelpers.readListPointer(this,
                                           segment,
                                           pointer,
                                           defaultSegment,
                                           defaultOffset,
                                           this.elementSize,
                                           nestingLimit);
    }

    public final Reader fromPointerReader(SegmentDataContainer segment, int pointer, int nestingLimit) {
        return fromPointerReaderRefDefault(segment, pointer, null, 0, nestingLimit);
    }

    public Builder fromPointerBuilderRefDefault(GenericSegmentBuilder segment, int pointer,
                                                SegmentDataContainer defaultSegment, int defaultOffset) {
        return WireHelpers.getWritableListPointer(this,
                                                  pointer,
                                                  segment,
                                                  this.elementSize,
                                                  defaultSegment,
                                                  defaultOffset);
    }

    public Builder fromPointerBuilder(GenericSegmentBuilder segment, int pointer) {
        return WireHelpers.getWritableListPointer(this,
                                                  pointer,
                                                  segment,
                                                  this.elementSize,
                                                  null, 0);
    }

    public Builder initFromPointerBuilder(GenericSegmentBuilder segment, int pointer, int elementCount) {
        return WireHelpers.initListPointer(this, pointer, segment, elementCount, this.elementSize);
    }

    public final void setPointerBuilder(GenericSegmentBuilder segment, int pointer, Reader value) {
        WireHelpers.setListPointer(segment, pointer, value);
    }
}
