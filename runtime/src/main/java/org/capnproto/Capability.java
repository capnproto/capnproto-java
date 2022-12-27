package org.capnproto;

import static org.capnproto.ClientHook.BROKEN_CAPABILITY_BRAND;
import static org.capnproto.ClientHook.NULL_CAPABILITY_BRAND;

public final class Capability {

    public static ClientHook newBrokenCap(String reason) {
        return newBrokenClient(reason, false, BROKEN_CAPABILITY_BRAND);
    }

    public static ClientHook newBrokenCap(Throwable exc) {
        return newBrokenClient(exc, false, BROKEN_CAPABILITY_BRAND);
    }

    public static ClientHook newNullCap() {
        return newBrokenClient(RpcException.failed("Called null capability"), true, NULL_CAPABILITY_BRAND);
    }

    private static ClientHook newBrokenClient(String reason, boolean resolved, Object brand) {
        return newBrokenClient(RpcException.failed(reason), resolved, brand);
    }

    private static ClientHook newBrokenClient(Throwable exc, boolean resolved, Object brand) {
        return new ClientHook() {
            @Override
            public Object getBrand() {
                return brand;
            }
        };
    }
}
