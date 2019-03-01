package org.capnproto;

import java.nio.ByteBuffer;

/**
 * The AllocatingArena defines methods required for the Builder.
 */
public interface AllocatingArena extends Arena {

    /**
     * Allocate a new Segment in case the previous Segment is not big enough for
     * the requested data.
     *
     * @param amountPlusRef the number of words needed.
     * @return The result of the allocation.
     */
    public BuilderArena.AllocateResult allocate(int amountPlusRef);

    /**
     * Retrieve the SegmentBuilder for given segmentId.
     *
     * @param segmentId the given segment ID
     * @return the segmentBuilder.
     */
    public SegmentBuilder getSegment(int segmentId);

    /**
     * Retrieve the ByteBuffers for Serialization.
     *
     * @return the buffers.
     */
    public ByteBuffer[] getSegmentsForOutput();

}
