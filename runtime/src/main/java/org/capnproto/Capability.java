package org.capnproto;

import static org.capnproto.ClientHook.BROKEN_CAPABILITY_BRAND;
import static org.capnproto.ClientHook.NULL_CAPABILITY_BRAND;

public final class Capability {

    public static ClientHook newBrokenCap(String reason) {
        return newBrokenClient(BROKEN_CAPABILITY_BRAND);
    }

    public static ClientHook newNullCap() {
        return newBrokenClient(NULL_CAPABILITY_BRAND);
    }

    private static ClientHook newBrokenClient(Object brand) {
        return new ClientHook() {
            @Override
            public Object getBrand() {
                return brand;
            }
        };
    }
}
