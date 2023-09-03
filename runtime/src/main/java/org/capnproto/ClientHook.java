package org.capnproto;

public interface ClientHook {

    static final Object NULL_CAPABILITY_BRAND = new Object();
    static final Object BROKEN_CAPABILITY_BRAND = new Object();

    /**
     Returns an opaque object that identifies who made this client. This can be used by an RPC adapter to
     discover when a capability it needs to marshal is one that it created in the first place, and
     therefore it can transfer the capability without proxying.
    */
    Object getBrand();

    /**
     * Returns true if the capability was created as a result of assigning a Client to null or by
     * reading a null pointer out of a Cap'n Proto message.
     */
    default boolean isNull() {
        return getBrand() == NULL_CAPABILITY_BRAND;
    }

    /**
     * Returns true if the capability was created by newBrokenCap().
     */
    default boolean isError() {
        return getBrand() == BROKEN_CAPABILITY_BRAND;
    }
}
