package org.capnproto;

import java.util.concurrent.CompletableFuture;

public interface VatNetwork<VatId>
{
    interface Connection<VatId> extends AutoCloseable {
        default OutgoingRpcMessage newOutgoingMessage() {
            return newOutgoingMessage(BuilderArena.SUGGESTED_FIRST_SEGMENT_WORDS);
        }
        OutgoingRpcMessage newOutgoingMessage(int firstSegmentWordSize);
        CompletableFuture<IncomingRpcMessage> receiveIncomingMessage();
        CompletableFuture<java.lang.Void> shutdown();
        VatId getPeerVatId();
        void close();
    }

    CompletableFuture<Connection<VatId>> accept();
    Connection<VatId> connect(VatId hostId);
}
