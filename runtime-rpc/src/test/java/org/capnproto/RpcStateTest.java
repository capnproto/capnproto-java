package org.capnproto;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;

public class RpcStateTest {

    class TestConnection implements VatNetwork.Connection<RpcTwoPartyProtocol.VatId.Reader> {

        private CompletableFuture<IncomingRpcMessage> nextIncomingMessage = new CompletableFuture<>();
        private final CompletableFuture<RpcState.DisconnectInfo> disconnect = new CompletableFuture<>();

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
                    var msg = new IncomingRpcMessage() {
                        @Override
                        public AnyPointer.Reader getBody() {
                            return message.getRoot(AnyPointer.factory).asReader();
                        }
                    };

                    if (nextIncomingMessage.isDone()) {
                        nextIncomingMessage = CompletableFuture.completedFuture(msg);
                    }
                    else {
                        nextIncomingMessage.complete(msg);
                    }
                }

                @Override
                public int sizeInWords() {
                    return 0;
                }
            };
        }

        @Override
        public CompletableFuture<IncomingRpcMessage> receiveIncomingMessage() {
           return this.nextIncomingMessage;
        }

        @Override
        public CompletableFuture<java.lang.Void> shutdown() {
            this.disconnect.complete(null);
            return this.disconnect.thenRun(() -> {});
        }

        @Override
        public RpcTwoPartyProtocol.VatId.Reader getPeerVatId() {
            return null;
        }

        @Override
        public void close() {
        }
    }

    TestConnection connection;
    Capability.Client bootstrapInterface;
    RpcState rpc;
    final Queue<OutgoingRpcMessage> sent = new ArrayDeque<>();

    @Before
    public void setUp() {
        this.connection = new TestConnection();
        this.bootstrapInterface = new Capability.Client(Capability.newNullCap());
        var bootstrapFactory = new BootstrapFactory<RpcTwoPartyProtocol.VatId.Reader>() {
            @Override
            public FromPointerReader<RpcTwoPartyProtocol.VatId.Reader> getVatIdFactory() {
                return RpcTwoPartyProtocol.VatId.factory;
            }

            @Override
            public Capability.Client createFor(RpcTwoPartyProtocol.VatId.Reader clientId) {
                return bootstrapInterface;
            }
        };

        this.rpc = new RpcState<>(bootstrapFactory, connection, connection.disconnect);
    }

    @After
    public void tearDown() {
        this.connection = null;
        this.rpc = null;
        this.sent.clear();
    }
/*
    @Test
    public void handleUnimplemented() {
        var msg = this.connection.newOutgoingMessage(0);
        var root = msg.getBody().initAs(RpcProtocol.Message.factory).initUnimplemented();
        var resolve = root.initResolve();
        RpcState.FromException(new Exception("foo"), resolve.initException());
        msg.send();
        Assert.assertFalse(sent.isEmpty());
    }
*/
    @Test
    public void handleAbort() {
        var msg = this.connection.newOutgoingMessage(0);
        var builder = msg.getBody().initAs(RpcProtocol.Message.factory);
        RpcState.FromException(RpcException.failed("Test abort"), builder.initAbort());
        msg.send();
    }

    @Test
    public void handleBootstrap() {
        var msg = this.connection.newOutgoingMessage(0);
        var bootstrap = msg.getBody().initAs(RpcProtocol.Message.factory).initBootstrap();
        bootstrap.setQuestionId(0);
        msg.send();
        Assert.assertEquals(2, sent.size());

        sent.remove(); // bootstrap
        var reply = sent.remove(); // return

        var rpcMsg = reply.getBody().getAs(RpcProtocol.Message.factory);
        Assert.assertEquals(RpcProtocol.Message.Which.RETURN, rpcMsg.which());
        var ret = rpcMsg.getReturn();
        Assert.assertEquals(ret.getAnswerId(), 0);
        Assert.assertEquals(RpcProtocol.Return.Which.RESULTS, ret.which());
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