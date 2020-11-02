package org.capnproto;

import java.util.concurrent.CompletableFuture;

public interface VatNetwork<VatId>
{
    interface Connection<VatId> {
        default OutgoingRpcMessage newOutgoingMessage() {
            return newOutgoingMessage(0);
        }
        OutgoingRpcMessage newOutgoingMessage(int firstSegmentWordSize);
        CompletableFuture<IncomingRpcMessage> receiveIncomingMessage();
        CompletableFuture<java.lang.Void> onDisconnect();
        CompletableFuture<java.lang.Void> shutdown();
        VatId getPeerVatId();
    }

    CompletableFuture<Connection<VatId>> baseAccept();

    //FromPointerReader<VatId> getVatIdFactory();

    Connection<VatId> connect(VatId hostId);
}

