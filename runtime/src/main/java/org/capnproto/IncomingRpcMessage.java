package org.capnproto;

import java.util.List;

public interface IncomingRpcMessage {

    AnyPointer.Reader getBody();

    List<Integer> getAttachedFds();
}
