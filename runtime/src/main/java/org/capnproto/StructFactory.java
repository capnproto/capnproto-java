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

public abstract class StructFactory<Builder, Reader extends StructReader>
    implements PointerFactory<Builder, Reader>,
    FromPointerBuilderRefDefault<Builder>,
    StructBuilder.Factory<Builder>,
    SetPointerBuilder<Builder, Reader>,
    FromPointerReaderRefDefault<Reader>,
    StructReader.Factory<Reader> {
    @Override
    public final Reader fromPointerReaderRefDefault(SegmentDataContainer segment, int pointer,
                                                    SegmentDataContainer defaultSegment, int defaultOffset,
                                                    int nestingLimit) {
        return WireHelpers.readStructPointer(this,
                                             segment,
                                             pointer,
                                             defaultSegment, defaultOffset,
                                             nestingLimit);
    }
    @Override
    public final Reader fromPointerReader(SegmentDataContainer segment, int pointer, int nestingLimit) {
        return fromPointerReaderRefDefault(segment, pointer, null, 0, nestingLimit);
    }
    @Override
    public final Builder fromPointerBuilderRefDefault(GenericSegmentBuilder segment, int pointer,
                                                      SegmentDataContainer defaultSegment, int defaultOffset) {
        return WireHelpers.getWritableStructPointer(this, pointer, segment, this.structSize(),
                                                    defaultSegment, defaultOffset);
    }
    @Override
    public final Builder fromPointerBuilder(GenericSegmentBuilder segment, int pointer) {
        return WireHelpers.getWritableStructPointer(this, pointer, segment, this.structSize(),
                                                    null, 0);
    }
    @Override
    public final Builder initFromPointerBuilder(GenericSegmentBuilder segment, int pointer, int elementCount) {
        return WireHelpers.initStructPointer(this, pointer, segment, this.structSize());
    }

    @Override
    public final void setPointerBuilder(GenericSegmentBuilder segment, int pointer, Reader value) {
        WireHelpers.setStructPointer(segment, pointer, value);
    }

    public abstract Reader asReader(Builder builder);
}
