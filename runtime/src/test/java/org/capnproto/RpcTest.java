// Copyright (c) 2018 Sandstorm Development Group, Inc. and contributors
// Licensed under the MIT License:
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

package org.capnproto;

import org.capnproto.test.Test;

import org.junit.Assert;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;

class TestNetwork {

    final Map<String, TestNetworkAdapter> map = new HashMap<>();
    int received = 0;

    TestNetworkAdapter add(String name) {
        return this.map.computeIfAbsent(
                name, key -> new TestNetworkAdapter(this, name));
    }

    TestNetworkAdapter find(String name) {
        return this.map.get(name);
    }
}

class TestNetworkAdapter
    implements VatNetwork<Test.TestSturdyRef.Reader> {

    @Override
    public CompletableFuture<VatNetwork.Connection<Test.TestSturdyRef.Reader>> baseAccept() {
        return this.accept().thenApply(conn -> conn);
    }

    class Connection implements VatNetwork.Connection<Test.TestSturdyRef.Reader> {

        Throwable networkException;
        Connection partner;
        final Queue<IncomingRpcMessage> messages = new ArrayDeque<>();
        final Queue<CompletableFuture<IncomingRpcMessage>> fulfillers = new ArrayDeque<>();
        CompletableFuture<java.lang.Void> fulfillOnEnd;
        final boolean isClient;
        final Test.TestSturdyRef.Reader peerId;

        Connection(boolean isClient, Test.TestSturdyRef.Reader peerId) {
            this.isClient = isClient;
            this.peerId = peerId;
        }

        void attach(Connection other) {
            Assert.assertNull(this.partner);
            Assert.assertNull(other.partner);
            this.partner = other;
            other.partner = this;
        }

        TestNetwork getNetwork() {
            return network;
        }

        @Override
        public OutgoingRpcMessage newOutgoingMessage(int firstSegmentWordSize) {
            var message = new MessageBuilder(firstSegmentWordSize);

            return new OutgoingRpcMessage() {
                @Override
                public AnyPointer.Builder getBody() {
                    return message.getRoot(AnyPointer.factory);
                }

                @Override
                public void send() {
                    if (networkException != null) {
                        return;
                    }

                    var incomingMessage = new IncomingRpcMessage() {
                        @Override
                        public AnyPointer.Reader getBody() {
                            return message.getRoot(AnyPointer.factory).asReader();
                        }
                    };

                    if (partner == null) {
                        return;
                    }

                    if (partner.fulfillers.isEmpty()) {
                        partner.messages.add(incomingMessage);
                    }
                    else {
                        partner.getNetwork().received++;
                        var front = partner.fulfillers.remove();
                        front.complete(incomingMessage);
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
            if (this.networkException != null) {
                return CompletableFuture.failedFuture(this.networkException);
            }

            if (this.messages.isEmpty()) {
                if (this.fulfillOnEnd != null) {
                    this.fulfillOnEnd.complete(null);
                    return CompletableFuture.completedFuture(null);
                }
                else {
                    var promise = new CompletableFuture<IncomingRpcMessage>();
                    this.fulfillers.add(promise);
                    return promise.copy();
                }
            }
            else {
                this.getNetwork().received++;
                var result = this.messages.remove();
                return CompletableFuture.completedFuture(result);
            }
        }

        @Override
        public CompletableFuture<java.lang.Void> onDisconnect() {
            return null;
        }

        @Override
        public CompletableFuture<java.lang.Void> shutdown() {
            if (this.partner == null) {
                return CompletableFuture.completedFuture(null);
            }
            var promise = new CompletableFuture<java.lang.Void>();
            this.partner.fulfillOnEnd = promise;
            return promise.copy();
        }

        public Test.TestSturdyRef.Reader getPeerVatId() {
            return this.peerId;
        }
    }

    final TestNetwork network;
    private final String self;
    int sent = 0;
    int received = 0;
    Map<TestNetworkAdapter, Connection> connections = new HashMap<>();
    Queue<CompletableFuture<Connection>> fulfillerQueue = new ArrayDeque<>();
    Queue<Connection> connectionQueue = new ArrayDeque<>();

    TestNetworkAdapter(TestNetwork network, String self) {
        this.network = network;
        this.self = self;
    }

    Connection newConnection(boolean isClient, Test.TestSturdyRef.Reader peerId) {
        return new Connection(isClient, peerId);
    }

    @Override
    public VatNetwork.Connection<Test.TestSturdyRef.Reader> connect(Test.TestSturdyRef.Reader refId) {
        var hostId = refId.getHostId().getHost().toString();
        if (hostId.equals(self)) {
            return null;
        }

        var dst = this.network.find(hostId);
        Assert.assertNotNull(dst);

        var connnection = this.connections.get(dst);
        if (connnection != null) {
            return connnection;
        }

        var local = this.newConnection(true, refId);
        var remote = dst.newConnection(false, refId);
        local.attach(remote);

        this.connections.put(dst, local);
        dst.connections.put(this, remote);

        if (dst.fulfillerQueue.isEmpty()) {
            dst.fulfillerQueue.add(CompletableFuture.completedFuture(remote));
        } else {
            dst.fulfillerQueue.remove().complete(remote);
        }
        return local;
    }

    public CompletableFuture<Connection> accept() {
        if (this.connections.isEmpty()) {
            var promise = new CompletableFuture<Connection>();
            this.fulfillerQueue.add(promise);
            return promise.thenApply(conn -> conn);
        }
        else {
            return CompletableFuture.completedFuture(this.connectionQueue.remove());
        }
    }
}

class TestContext {
    final TestNetwork network = new TestNetwork();
    final TestNetworkAdapter clientNetwork;
    final TestNetworkAdapter serverNetwork;

    final RpcSystem<Test.TestSturdyRef.Reader> rpcClient;
    final RpcSystem<Test.TestSturdyRef.Reader> rpcServer;

    TestContext(Capability.Client bootstrapInterface) {
        this.clientNetwork = this.network.add("client");
        this.serverNetwork = this.network.add("server");
        this.rpcClient = RpcSystem.makeRpcClient(this.clientNetwork);
        this.rpcServer = RpcSystem.makeRpcServer(this.serverNetwork, bootstrapInterface);
    }

    TestContext(BootstrapFactory<Test.TestSturdyRef.Reader> bootstrapFactory) {
        this.clientNetwork = this.network.add("client");
        this.serverNetwork = this.network.add("server");
        this.rpcClient = RpcSystem.makeRpcClient(this.clientNetwork);
        this.rpcServer = RpcSystem.makeRpcServer(this.serverNetwork, bootstrapFactory);
    }

    Capability.Client connect(Test.TestSturdyRefObjectId.Tag tag) {
        var message = new MessageBuilder();
        var ref = message.initRoot(Test.TestSturdyRef.factory);
        var hostId = ref.initHostId();
        hostId.setHost("server");
        ref.getObjectId().initAs(Test.TestSturdyRefObjectId.factory).setTag(tag);
        return rpcClient.bootstrap(ref.asReader());
    }
}

public class RpcTest {

    static BootstrapFactory<Test.TestSturdyRef.Reader> bootstrapFactory = new BootstrapFactory<>() {
        @Override
        public FromPointerReader<Test.TestSturdyRef.Reader> getVatIdFactory() {
            return Test.TestSturdyRef.factory;
        }

        @Override
        public Capability.Client createFor(Test.TestSturdyRef.Reader refId) {
            var callCount = new Counter();
            var handleCount = new Counter();

            var objectId = refId.getObjectId().getAs(Test.TestSturdyRefObjectId.factory);
            var tag = objectId.getTag();
            switch (tag) {
                case TEST_INTERFACE:
                    return new Capability.Client(new TestUtil.TestInterfaceImpl(callCount));
                case TEST_EXTENDS:
                    return new Capability.Client(Capability.newBrokenCap("No TestExtends implemented."));
                case TEST_PIPELINE:
                    return new Capability.Client(new TestUtil.TestPipelineImpl(callCount));
                case TEST_TAIL_CALLEE:
                    return new Capability.Client(new TestUtil.TestTailCalleeImpl(callCount));
                case TEST_TAIL_CALLER:
                    return new Capability.Client(new TestUtil.TestTailCallerImpl(callCount));
                case TEST_MORE_STUFF:
                    return new Capability.Client(new TestUtil.TestMoreStuffImpl(callCount, handleCount));
                default:
                    return new Capability.Client();
            }
        }
    };

    @org.junit.Test
    public void testBasic() {
        var context = new TestContext(bootstrapFactory);
        var client = new Test.TestInterface.Client(context.connect(Test.TestSturdyRefObjectId.Tag.TEST_INTERFACE));
        var request1 = client.fooRequest();
        request1.getParams().setI(123);
        request1.getParams().setJ(true);
        var promise1 = request1.send();

        final var ref = new Object() {
            boolean barFailed = false;
        };
        var request3 = client.barRequest();
        var promise3 = request3.send().exceptionally(exc -> {
            ref.barFailed = true;
            return null;
        });

        var request2 = client.bazRequest();
        TestUtil.initTestMessage(request2.getParams().initS());
        var promise2 = request2.send();

        var response1 = promise1.join();
        Assert.assertEquals("foo", response1.getX().toString());

        var response2 = promise2.join();
        promise3.join();

        Assert.assertTrue(ref.barFailed);
    }

    @org.junit.Test
    public void testPipelining() {
        var context = new TestContext(bootstrapFactory);
        var client = new Test.TestPipeline.Client(context.connect(Test.TestSturdyRefObjectId.Tag.TEST_PIPELINE));

        var chainedCallCount = new Counter();

        var request = client.getCapRequest();
        request.getParams().setN(234);
        request.getParams().setInCap(new TestUtil.TestInterfaceImpl(chainedCallCount));

        var promise = request.send();

        var pipelineRequest = promise.getOutBox().getCap().fooRequest();
        pipelineRequest.getParams().setI(321);

        var pipelinePromise = pipelineRequest.send();

        var pipelineRequest2 = new Test.TestExtends.Client(promise.getOutBox().getCap()).graultRequest();
        var pipelinePromise2 = pipelineRequest2.send();

        promise = null;

        //Assert.assertEquals(0, chainedCallCount.value());

        var response = pipelinePromise.join();
        Assert.assertEquals("bar", response.getX().toString());

        var response2 = pipelinePromise2.join();
        TestUtil.checkTestMessage(response2);

        Assert.assertEquals(1, chainedCallCount.value());
    }

    @org.junit.Test
    public void testRelease() {
        var context = new TestContext(bootstrapFactory);
        var client = new Test.TestMoreStuff.Client(context.connect(Test.TestSturdyRefObjectId.Tag.TEST_MORE_STUFF));

        var handle1 = client.getHandleRequest().send().join().getHandle();
        var promise = client.getHandleRequest().send();
        var handle2 = promise.join().getHandle();

        handle1 = null;
        handle2 = null;


    }
}

