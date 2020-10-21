package org.capnproto;

import java.util.List;

public interface OutgoingRpcMessage {

    AnyPointer.Builder getBody();

    default void setFds(List<Integer> fds) {
    }

    default List<Integer> getFds() {
        return List.of();
    }

    void send();

    int sizeInWords();
}
