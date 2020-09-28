package org.capnproto;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.*;

public class RpcStateTest {

    class TestMessage implements IncomingRpcMessage {

        MessageBuilder builder = new MessageBuilder();

        @Override
        public AnyPointer.Reader getBody() {
            return builder.getRoot(AnyPointer.factory).asReader();
        }
    }

    class TestConnection implements VatNetwork.Connection {

        @Override
        public OutgoingRpcMessage newOutgoingMessage(int firstSegmentWordSize) {
            var message = new MessageBuilder();

            return new OutgoingRpcMessage() {
                @Override
                public AnyPointer.Builder getBody() {
                    return message.getRoot(AnyPointer.factory);
                }

                @Override
                public void send() {
                    sent.add(this);
                }

                @Override
                public int sizeInWords() {
                    return 0;
                }
            };
        }

        @Override
        public CompletableFuture<IncomingRpcMessage> receiveIncomingMessage() {
            return null;
        }
    }

    TestConnection connection;
    RpcState rpc;
    final Queue<OutgoingRpcMessage> sent = new ArrayDeque<>();

    @Before
    public void setUp() throws Exception {
        connection = new TestConnection();
        rpc = new RpcState(connection);
    }

    @After
    public void tearDown() throws Exception {
        connection = null;
        rpc = null;
        sent.clear();
    }

    @Test
    public void handleUnimplemented() {
        var msg = new TestMessage();
        msg.builder.getRoot(RpcProtocol.Message.factory).initUnimplemented();
        rpc.handleMessage(msg);
        Assert.assertTrue(sent.isEmpty());
    }

    @Test
    public void handleAbort() {
    }

    @Test
    public void handleBootstrap() {
    }

    @Test
    public void handleCall() {
    }

    @Test
    public void handleReturn() {
    }

    @Test
    public void handleFinish() {
    }

    @Test
    public void handleResolve() {
    }

    @Test
    public void handleDisembargo() {
    }

}