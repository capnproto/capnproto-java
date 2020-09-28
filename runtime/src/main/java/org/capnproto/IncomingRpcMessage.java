package org.capnproto;

import java.util.List;

public interface IncomingRpcMessage {

    AnyPointer.Reader getBody();

    default List<Integer> getAttachedFds() {
        return List.of();
    }
}
