package org.capnproto;

import java.util.concurrent.CompletableFuture;

public interface VatNetwork<VatId> {

     interface Connection {
         OutgoingRpcMessage newOutgoingMessage(int firstSegmentWordSize);
         CompletableFuture<IncomingRpcMessage> receiveIncomingMessage();
         CompletableFuture<java.lang.Void> onDisconnect();
         CompletableFuture<java.lang.Void> shutdown();
     }

     Connection baseConnect(VatId hostId);
     CompletableFuture<Connection> baseAccept();
}
