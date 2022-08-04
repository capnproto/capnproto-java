package org.capnproto;

import java.nio.channels.AsynchronousByteChannel;
import java.nio.channels.CompletionHandler;

public interface AsynchronousByteListenChannel {
    public abstract <A> void accept(A attachment, CompletionHandler<AsynchronousByteChannel,? super A> handler);
}
