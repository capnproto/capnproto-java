package org.capnproto;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;

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
    Capability.Client bootstrapInterface;
    RpcState rpc;
    final Queue<OutgoingRpcMessage> sent = new ArrayDeque<>();

    @Before
    public void setUp() throws Exception {
        connection = new TestConnection();
        bootstrapInterface = new Capability.Client(ClientHook.newNullCap());
        rpc = new RpcState(connection, bootstrapInterface);
    }

    @After
    public void tearDown() throws Exception {
        connection = null;
        rpc = null;
        sent.clear();
    }

    @Test
    public void handleUnimplemented() throws RpcException {
        var msg = new TestMessage();
        msg.builder.getRoot(RpcProtocol.Message.factory).initUnimplemented();
        rpc.handleMessage(msg);
        Assert.assertTrue(sent.isEmpty());
    }

    @Test
    public void handleAbort() {
        var msg = new TestMessage();
        var builder = msg.builder.getRoot(RpcProtocol.Message.factory);
        RpcException.fromException(RpcException.failed("Test abort"), builder.initAbort());
        Assert.assertThrows(RpcException.class, () -> rpc.handleMessage(msg));
    }

    @Test
    public void handleBootstrap() throws RpcException {
        var msg = new TestMessage();
        var bootstrap = msg.builder.getRoot(RpcProtocol.Message.factory).initBootstrap();
        bootstrap.setQuestionId(0);
        rpc.handleMessage(msg);
        Assert.assertFalse(sent.isEmpty());
        var reply = sent.remove();
        var rpcMsg = reply.getBody().getAs(RpcProtocol.Message.factory);
        Assert.assertEquals(rpcMsg.which(),  RpcProtocol.Message.Which.RETURN);
        var ret = rpcMsg.getReturn();
        Assert.assertEquals(ret.getAnswerId(), 0);
        Assert.assertEquals(ret.which(), RpcProtocol.Return.Which.RESULTS);
        var results = ret.getResults();
        Assert.assertEquals(results.getCapTable().size(), 1); // got a capability!
        Assert.assertTrue(results.hasContent());
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