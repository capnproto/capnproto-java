package org.capnproto;

import java.util.List;

public interface OutgoingRpcMessage {

    AnyPointer.Builder getBody();

    default void setFds(List<Integer> fds) {
    }

    void send();

    int sizeInWords();
}
