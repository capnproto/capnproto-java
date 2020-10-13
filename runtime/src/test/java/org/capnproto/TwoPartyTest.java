package org.capnproto;

import org.capnproto.demo.Demo;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

class TestCap0Impl extends Demo.TestCap0.Server {

    final Demo.TestCap1.Client testCap1a = new Demo.TestCap1.Client(new TestCap1Impl());
    final Demo.TestCap1.Client testCap1b = new Demo.TestCap1.Client(new TestCap1Impl());

    public CompletableFuture<?> testMethod0(CallContext<Demo.TestParams0.Reader, Demo.TestResults0.Builder>  ctx) {
        var params = ctx.getParams();
        var results = ctx.getResults();
        results.setResult0(params.getParam0());
        return CompletableFuture.completedFuture(null);
    }

    public CompletableFuture<?> testMethod1(CallContext<Demo.TestParams1.Reader, Demo.TestResults1.Builder>  ctx) {
        var params = ctx.getParams();
        var results = ctx.getResults();
        var res0 = results.getResult0();
        res0.setAs(Demo.TestCap1.factory, testCap1a);
        var res1 = results.getResult1();
        res1.setAs(Demo.TestCap1.factory, testCap1b);
        var res2 = results.getResult2();
        res2.setAs(Demo.TestCap1.factory, testCap1b);
        return CompletableFuture.completedFuture(null);
    }
}

class TestCap1Impl extends Demo.TestCap1.Server {
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
        var resolved = cap.whenResolved().toCompletableFuture();
        resolved.join();
    }

    @Test
    public void testBasic() throws ExecutionException, InterruptedException {
        var capServer = new TestCap0Impl();
        var server = new TwoPartyServer(new Demo.TestCap0.Client(capServer));
        server.listen(serverSocket);
        var demoClient = new Demo.TestCap0.Client(this.client.bootstrap());
        var request = demoClient.testMethod0Request();
        var params = request.getParams();
        params.setParam0(4321);
        var response = request.send();
        while (!response.isDone()) {
            CompletableFuture.anyOf(response, server.runOnce()).join();
        }
        Assert.assertTrue(response.isDone());
        var results = response.get();
        Assert.assertEquals(params.getParam0(), results.getResult0());
    }

    @Test
    public void testReturnCap() throws ExecutionException, InterruptedException {
        // send a capability back from the server to the client
        var capServer = new TestCap0Impl();
        var server = new TwoPartyServer(new Demo.TestCap0.Client(capServer));
        server.listen(serverSocket);
        var demoClient = new Demo.TestCap0.Client(this.client.bootstrap());
        var request = demoClient.testMethod1Request();
        var params = request.getParams();
        var response = request.send();
        while (!response.isDone()) {
            CompletableFuture.anyOf(response, server.runOnce()).join();
        }
        Assert.assertTrue(response.isDone());

        var results = response.get();
        var cap0 = results.getResult0();
        Assert.assertFalse(cap0.isNull());
        var cap1 = results.getResult1();
        Assert.assertFalse(cap1.isNull());
        var cap2 = results.getResult2();
        Assert.assertFalse(cap2.isNull());
    }

    @Test
    public void testLocalServer() throws ExecutionException, InterruptedException {
        var demo = new TestCap0Impl();
        var client = new Demo.TestCap0.Client(demo);
        var request = client.testMethod0Request();
        var params = request.getParams();
        params.setParam0(4321);
        var response = request.send();
        var results = response.get();
        Assert.assertEquals(params.getParam0(), results.getResult0());
    }

    @Test
    public void testGenericServer() throws ExecutionException, InterruptedException {
        var demo = new TestCap0Impl();
        var client = new Demo.TestCap0.Client(demo);
        var request = client.testMethod0Request();
        var params = request.getParams();
        var response = request.send();
        var results = response.get();
        Assert.assertEquals(params.getParam0(), results.getResult0());
    }

    @Test
    public void testLocalTwoStagePipeline() {

        var server0 = new Demo.Iface0.Server() {
            boolean method0called = false;

            @Override
            protected CompletableFuture<?> method0(CallContext<Demo.Iface0.Method0Params.Reader, Demo.Iface0.Method0Results.Builder> ctx) {
                method0called = true;
                return CompletableFuture.completedFuture(null);
            }
        };

        var server1 = new Demo.Iface1.Server() {
            @Override
            protected CompletableFuture<?> method1(CallContext<Demo.Iface1.Method1Params.Reader, Demo.Iface1.Method1Results.Builder> ctx) {
                ctx.getResults().setResult0(new Demo.Iface0.Client(server0));
                return CompletableFuture.completedFuture(null);
            }
        };

        var iface1Client = new Demo.Iface1.Client(server1);
        var request1 = iface1Client.method1Request();
        var response = request1.send();
        var iface0 = response.getResult0();
        var request0 = iface0.method0Request();
        var response0 = request0.send();
        response0.join();
        Assert.assertTrue(!response0.isCompletedExceptionally());
        Assert.assertTrue(server0.method0called);
    }
}