package org.capnproto;

/**
 * An object that allocates memory for a Cap'n Proto message as it is being built.
 */
public interface Allocator {
    public enum AllocationStrategy {
        FIXED_SIZE,
        GROW_HEURISTICALLY
    }
    /**
     * Allocates a ByteBuffer to be used as a segment in a message. The returned
     * buffer must contain at least `minimumSize` bytes, all of which MUST be
     * set to zero.
     */
   public java.nio.ByteBuffer allocateSegment(int minimumSize);

   /**
    * set the size for the next buffer allocation in bytes
    */
   public void setNextAllocationSizeBytes(int nextSize);
}
