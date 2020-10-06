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

public final class AnyPointer {
    public static final class Factory implements PointerFactory<Builder, Reader> {
        public final Reader fromPointerReader(SegmentReader segment, CapTableReader capTable, int pointer, int nestingLimit) {
            return new Reader(segment, capTable, pointer, nestingLimit);
        }
        public final Builder fromPointerBuilder(SegmentBuilder segment, CapTableBuilder capTable, int pointer) {
            return new Builder(segment, capTable, pointer);
        }
        public final Builder initFromPointerBuilder(SegmentBuilder segment, CapTableBuilder capTable, int pointer, int elementCount) {
            Builder result = new Builder(segment, capTable, pointer);
            result.clear();
            return result;
        }
    }
    public static final Factory factory = new Factory();

    public final static class Reader extends Capability.ReaderContext {
        final SegmentReader segment;
        final int pointer; // offset in words
        final int nestingLimit;

        public Reader(SegmentReader segment, int pointer, int nestingLimit) {
            this.segment = segment;
            this.pointer = pointer;
            this.nestingLimit = nestingLimit;
        }

        public Reader(SegmentReader segment, CapTableReader capTable, int pointer, int nestingLimit) {
            this.segment = segment;
            this.pointer = pointer;
            this.nestingLimit = nestingLimit;
            this.capTable = capTable;
        }

        final Reader imbue(CapTableReader capTable) {
            var result = new Reader(segment, pointer, nestingLimit);
            result.capTable = capTable;
            return result;
        }

        public final boolean isNull() {
            return WirePointer.isNull(this.segment.buffer.getLong(this.pointer * Constants.BYTES_PER_WORD));
        }

        public final <T> T getAs(FromPointerReader<T> factory) {
            return factory.fromPointerReader(this.segment, this.capTable, this.pointer, this.nestingLimit);
        }

        public final Capability.Client getAsCap() {
            return new Capability.Client(
                    WireHelpers.readCapabilityPointer(this.segment, capTable, this.pointer, 0));
        }

        public final ClientHook getPipelinedCap(PipelineOp[] ops) {
            for (var op: ops) {
                switch (op.type) {
                    case NOOP:
                        break;
                    case GET_POINTER_FIELD:
                        var index = op.pointerIndex;
                        // TODO getpointerfield
                        break;
                }
            }
            // TODO implement getPipelinedCap
            return null;
        }
    }

    public static final class Builder extends Capability.BuilderContext {
        final SegmentBuilder segment;
        final int pointer;

        public Builder(SegmentBuilder segment, int pointer) {
            this.segment = segment;
            this.pointer = pointer;
        }

        Builder(SegmentBuilder segment, CapTableBuilder capTable, int pointer) {
            this.segment = segment;
            this.pointer = pointer;
            this.capTable = capTable;
        }

        final Builder imbue(CapTableBuilder capTable) {
            return new Builder(segment, capTable, pointer);
        }

        public final boolean isNull() {
            return WirePointer.isNull(this.segment.buffer.getLong(this.pointer * Constants.BYTES_PER_WORD));
        }

        public final <T> T getAs(FromPointerBuilder<T> factory) {
            return factory.fromPointerBuilder(this.segment, this.capTable, this.pointer);
        }

        public final <T> T initAs(FromPointerBuilder<T> factory) {
            return factory.initFromPointerBuilder(this.segment, this.capTable, this.pointer, 0);
        }

        public final <T> T initAs(FromPointerBuilder<T> factory, int elementCount) {
            return factory.initFromPointerBuilder(this.segment, this.capTable, this.pointer, elementCount);
        }

        public final <T, U> void setAs(SetPointerBuilder<T, U> factory, U reader) {
            factory.setPointerBuilder(this.segment, this.capTable, this.pointer, reader);
        }

        public final Capability.Client getAsCap() {
            return new Capability.Client(
                    WireHelpers.readCapabilityPointer(this.segment, capTable, this.pointer, 0));
        }

        public final void setAsCap(Capability.Client cap) {
            WireHelpers.setCapabilityPointer(this.segment, capTable, this.pointer, cap.hook);
        }

        public final Reader asReader() {
            return new Reader(segment, this.capTable, pointer, java.lang.Integer.MAX_VALUE);
        }

        public final void clear() {
            WireHelpers.zeroObject(this.segment, this.capTable, this.pointer);
            this.segment.buffer.putLong(this.pointer * 8, 0L);
        }
    }

}
