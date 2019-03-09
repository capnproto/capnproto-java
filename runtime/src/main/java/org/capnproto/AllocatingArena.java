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
import java.util.List;

/**
 * The Arena used for allocating new Segments.
 */
public interface AllocatingArena extends Arena {

    /**
     * Allocate a new Segment in case the previous Segment is not big enough for
     * the requested data.
     *
     * @param amountPlusRef the number of words needed.
     * @return The result of the allocation.
     */
    BuilderArena.AllocateResult allocate(int amountPlusRef);

    /**
     * Provides the {@link GenericSegmentBuilder} for the given segment ID.
     *
     * @param segmentId the segment ID
     * @return the segment.
     */
    @Override
    GenericSegmentBuilder tryGetSegment(int segmentId);

    /**
     * Retrieve the ByteBuffers for Serialization.
     *
     * @return the buffers.
     */
    ByteBuffer[] getSegmentsForOutput();

    /**
     * Access all currently existing segments.
     *
     * @return the segments.
     */
    List<? extends GenericSegmentBuilder> getSegments();
}
