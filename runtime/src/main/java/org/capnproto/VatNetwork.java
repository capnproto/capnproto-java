package org.capnproto;

import java.util.concurrent.CompletableFuture;

public interface VatNetwork {

     interface Connection {

         OutgoingRpcMessage newOutgoingMessage(int firstSegmentWordSize);

         CompletableFuture<IncomingRpcMessage> receiveIncomingMessage();
    }
}
