package org.capnproto;

public interface ClientHook {

    Object NULL_CAPABILITY_BRAND = new Object();
    Object BROKEN_CAPABILITY_BRAND = new Object();

    default ClientHook getResolved() {
        return null;
    }

    default Object getBrand() {
        return NULL_CAPABILITY_BRAND;
    }

    default boolean isNull() {
        return getBrand() == NULL_CAPABILITY_BRAND;
    }

    default boolean isError() {
        return getBrand() == BROKEN_CAPABILITY_BRAND;
    }

    default Integer getFd() {
        return null;
    }

    static ClientHook newBrokenCap(String reason) {
        return newBrokenClient(reason, false, BROKEN_CAPABILITY_BRAND);
    }

    static ClientHook newBrokenCap(Throwable exc) {
        return newBrokenClient(exc, false, BROKEN_CAPABILITY_BRAND);
    }

    static ClientHook newNullCap() {
        return newBrokenClient(new RuntimeException("Called null capability"), true, NULL_CAPABILITY_BRAND);
    }

    static private ClientHook newBrokenClient(String reason, boolean resolved, Object brand) {
        return newBrokenClient(new RuntimeException(reason), resolved, brand);
    }

    static private ClientHook newBrokenClient(Throwable exc, boolean resolved, Object brand) {
        return new ClientHook() {
            @Override
            public Object getBrand() {
                return brand;
            }
        };
    }
}
