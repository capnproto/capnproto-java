package org.capnproto;

import org.capnproto.demo.Demo;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;

class TestCap0 {

    static final class Client extends org.capnproto.Capability.Client {
        public Client() { super(); }

        public Client(ClientHook hook) { super(hook); }

        public Client(Capability.Client cap) { super(cap.hook); }

        public Client(Capability.Server server) { super(server); }

        public org.capnproto.Request<Demo.TestParams0.Builder, Demo.TestResults0.Reader> testMethod0Request() {
            return newCall(Demo.TestParams0.factory, Demo.TestResults0.factory, 0xa65f4a3d7f622e6bL, (short) 0);
        }

        public org.capnproto.Request<Demo.TestParams1.Builder, Demo.TestResults1.Reader> testMethod1Request() {
            return newCall(Demo.TestParams1.factory, Demo.TestResults1.factory, 0xa65f4a3d7f622e6bL, (short) 1);
        }
    }

    static abstract class Server extends org.capnproto.Capability.Server {

        public class TestMethod0Context extends CallContext<Demo.TestParams0.Reader, Demo.TestResults0.Builder> {
            public TestMethod0Context(CallContextHook hook) {
                super(Demo.TestParams0.factory, Demo.TestResults0.factory, hook);
            }
        }

        public class TestMethod1Context extends CallContext<Demo.TestParams1.Reader, Demo.TestResults1.Builder> {
            public TestMethod1Context(CallContextHook hook) {
                super(Demo.TestParams1.factory, Demo.TestResults1.factory, hook);
            }
        }

        @Override
        public DispatchCallResult dispatchCall(long interfaceId, short methodId, CallContext<AnyPointer.Reader, AnyPointer.Builder> context) {
            if (interfaceId == 0xa65f4a3d7f622e6bL) {
                return dispatchCallInternal(methodId, context);
            }
            return internalUnimplemented(Demo.class.getName(), interfaceId);
        }

        private DispatchCallResult dispatchCallInternal(short methodId, CallContext<AnyPointer.Reader, AnyPointer.Builder> ctx) {
            switch (methodId) {
                case 0:
                    return new DispatchCallResult(testMethod0(new TestMethod0Context(ctx.getHook())));
                case 1:
                    return new DispatchCallResult(testMethod1(new TestMethod1Context(ctx.getHook())));
                default:
                    return internalUnimplemented(Demo.class.getName(), 0xa27d3c231c7b9202L, methodId);
            }
        }

        public CompletableFuture<?> testMethod0(TestMethod0Context ctx) {
            return CompletableFuture.failedFuture(RpcException.unimplemented("testMethod0"));
        }

        public CompletableFuture<?> testMethod1(TestMethod1Context ctx) {
            return CompletableFuture.failedFuture(RpcException.unimplemented("testMethod1"));
        }
    }
}

class TestCap1 {

    static final class Client extends org.capnproto.Capability.Client {
        public Client() { super(); }

        public Client(ClientHook hook) { super(hook); }

        public Client(Capability.Client cap) { super(cap.hook); }

        public Client(Capability.Server server) { super(server); }
    }

    static abstract class Server extends org.capnproto.Capability.Server {

        @Override
        public DispatchCallResult dispatchCall(long interfaceId, short methodId, CallContext<AnyPointer.Reader, AnyPointer.Builder> context) {
            if (interfaceId == 0x81da3f8f6079c216L) {
                return dispatchCallInternal(methodId, context);
            }
            return internalUnimplemented(Demo.class.getName(), interfaceId);
        }

        private DispatchCallResult dispatchCallInternal(short methodId, CallContext<AnyPointer.Reader, AnyPointer.Builder> ctx) {
            switch (methodId) {
                default:
                    return internalUnimplemented(Demo.class.getName(), 0x81da3f8f6079c216L, methodId);
            }
        }
    }
}

class TestCap0Impl extends TestCap0.Server {

    final TestCap1.Client testCap1a = new TestCap1.Client(new TestCap1Impl());
    final TestCap1.Client testCap1b = new TestCap1.Client(new TestCap1Impl());

    public CompletableFuture<?> testMethod0(TestCap0.Server.TestMethod0Context ctx) {
        var params = ctx.getParams();
        var results = ctx.getResults();
        results.setResult0(params.getParam0());
        return CompletableFuture.completedFuture(null);
    }

    public CompletableFuture<?> testMethod1(TestCap0.Server.TestMethod1Context ctx) {
        var params = ctx.getParams();
        var results = ctx.getResults();
        var res0 = results.getResult0();
        res0.setAsCapability(testCap1a);
        var res1 = results.getResult1();
        res1.setAsCapability(testCap1b);
        var res2 = results.getResult2();
        res2.setAsCapability(testCap1b);
        return CompletableFuture.completedFuture(null);
    }
}

class TestCap1Impl extends TestCap1.Server {
}

public class TwoPartyTest {

    AsynchronousServerSocketChannel serverSocket;
    AsynchronousSocketChannel clientSocket;
    TwoPartyClient client;

    @Before
    public void setUp() throws Exception {
        this.serverSocket = AsynchronousServerSocketChannel.open();
        this.serverSocket.bind(null);

        this.clientSocket = AsynchronousSocketChannel.open();
        this.clientSocket.connect(this.serverSocket.getLocalAddress()).get();

        this.client = new TwoPartyClient(clientSocket);
    }

    @After
    public void tearDown() throws Exception {
        this.clientSocket.close();
        this.serverSocket.close();
        this.client = null;
    }

    @Test
    public void testNullCap() {

        var server = new TwoPartyServer(new Capability.Client());
        server.listen(serverSocket);
        var cap = this.client.bootstrap();
        var resolved = cap.whenResolved();
        resolved.join();
    }

    @Test
    public void testBasic() throws ExecutionException, InterruptedException {
        var capServer = new TestCap0Impl();
        var server = new TwoPartyServer(new TestCap0.Client(capServer));
        server.listen(serverSocket);
        var demoClient = new TestCap0.Client(this.client.bootstrap());
        var request = demoClient.testMethod0Request();
        var params = request.params();
        params.setParam0(4321);
        var resultsPromise = request.send();
        while (!resultsPromise.isDone()) {
            CompletableFuture.anyOf(resultsPromise, server.runOnce()).join();
        }
        Assert.assertTrue(resultsPromise.isDone());
        var results = resultsPromise.get();
        Assert.assertEquals(params.getParam0(), results.getResult0());
    }

    @Test
    public void testReturnCap() throws ExecutionException, InterruptedException {
        // send a capabilty back from the server to the client
        var capServer = new TestCap0Impl();
        var server = new TwoPartyServer(new TestCap0.Client(capServer));
        server.listen(serverSocket);
        var demoClient = new TestCap0.Client(this.client.bootstrap());
        var request = demoClient.testMethod1Request();
        var params = request.params();
        var resultsPromise = request.send();
        while (!resultsPromise.isDone()) {
            CompletableFuture.anyOf(resultsPromise, server.runOnce(), client.runOnce()).join();
        }
        Assert.assertTrue(resultsPromise.isDone());
        var results = resultsPromise.get();
        var cap0 = results.getResult0().getAsCapability();
        Assert.assertFalse(cap0.hook.isNull());
        Assert.assertFalse(cap0.hook.isError());
        var cap1 = results.getResult1().getAsCapability();
        Assert.assertFalse(cap1.hook.isNull());
        Assert.assertFalse(cap1.hook.isError());
        var cap2 = results.getResult2().getAsCapability();
        Assert.assertFalse(cap2.hook.isNull());
        Assert.assertFalse(cap2.hook.isError());
    }

    @Test
    public void testLocalServer() throws ExecutionException, InterruptedException {
        var demo = new TestCap0Impl();
        var client = new TestCap0.Client(demo);
        var request = client.testMethod0Request();
        var params = request.params();
        params.setParam0(4321);
        var results = request.send().get();
        Assert.assertEquals(params.getParam0(), results.getResult0());
    }
}