package org.capnproto;

import java.io.FileDescriptor;
import java.util.List;

public interface IncomingRpcMessage {

    AnyPointer.Reader getBody();

    default List<FileDescriptor> getAttachedFds() {
        return List.of();
    }
}
