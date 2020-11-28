package org.capnproto;

import java.io.FileDescriptor;
import java.util.List;

public interface OutgoingRpcMessage {

    AnyPointer.Builder getBody();

    default void setFds(List<FileDescriptor> fds) {
    }

    default List<Integer> getFds() {
        return List.of();
    }

    void send();

    int sizeInWords();
}
