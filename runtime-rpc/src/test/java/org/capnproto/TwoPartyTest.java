package org.capnproto;

/*
import org.capnproto.demo.Demo;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

class TestCap0Impl extends Demo.TestCap0.Server {

    final Demo.TestCap1.Client testCap1a = new Demo.TestCap1.Client(new TestCap1Impl());
    final Demo.TestCap1.Client testCap1b = new Demo.TestCap1.Client(new TestCap1Impl());

    public CompletableFuture<java.lang.Void> testMethod0(CallContext<Demo.TestParams0.Reader, Demo.TestResults0.Builder>  ctx) {
        var params = ctx.getParams();
        var results = ctx.getResults();
        results.setResult0(params.getParam0());
        ctx.releaseParams();
        return CompletableFuture.completedFuture(null);
    }

    public CompletableFuture<java.lang.Void> testMethod1(CallContext<Demo.TestParams1.Reader, Demo.TestResults1.Builder>  ctx) {
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

class Tap implements org.capnproto.TwoPartyVatNetwork.MessageTap {

    final RpcDumper dumper = new RpcDumper();

    @Override
    public void incoming(IncomingRpcMessage message, RpcTwoPartyProtocol.Side side) {
        var text = this.dumper.dump(message.getBody().getAs(RpcProtocol.Message.factory), side);
        if (text.length() > 0) {
            System.out.println(text);
        }
    }
}

public class TwoPartyTest {

    private Thread runServer(org.capnproto.TwoPartyVatNetwork network) {
        var thread = new Thread(() -> {
            try {
                network.onDisconnect().get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }, "Server");

        thread.start();
        return thread;
    }

    AsynchronousServerSocketChannel serverSocket;
    AsynchronousSocketChannel clientSocket;
    TwoPartyClient client;
    org.capnproto.TwoPartyVatNetwork serverNetwork;
    Thread serverThread;

    @Before
    public void setUp() throws Exception {
        this.serverSocket = AsynchronousServerSocketChannel.open();
        this.serverSocket.bind(null);

        this.clientSocket = AsynchronousSocketChannel.open();
        this.clientSocket.connect(this.serverSocket.getLocalAddress()).get();
        this.client = new TwoPartyClient(clientSocket);
        this.client.getNetwork().setTap(new Tap());

        var socket = serverSocket.accept().get();
        this.serverNetwork = new org.capnproto.TwoPartyVatNetwork(socket, RpcTwoPartyProtocol.Side.SERVER);
        this.serverNetwork.setTap(new Tap());
        //this.serverNetwork.dumper.addSchema(Demo.TestCap1);
        this.serverThread = runServer(this.serverNetwork);
    }

    @After
    public void tearDown() throws Exception {
        this.clientSocket.close();
        this.serverSocket.close();
        this.serverThread.join();
        this.client = null;
    }

    @Test
    public void testNullCap() throws ExecutionException, InterruptedException {
        var server = new RpcSystem<>(this.serverNetwork, new Capability.Client());
        var cap = this.client.bootstrap();
        var resolved = cap.whenResolved().toCompletableFuture();
        resolved.get();
    }

    @Test
    public void testBasic() throws ExecutionException, InterruptedException, IOException {
        var server = new RpcSystem<>(this.serverNetwork, new TestCap0Impl());

        var demo = new Demo.TestCap0.Client(this.client.bootstrap());
        var request = demo.testMethod0Request();
        var params = request.getParams();
        params.setParam0(4321);
        var response = request.send();
        response.get();
        Assert.assertTrue(response.isDone());
        var results = response.get();
        Assert.assertEquals(params.getParam0(), results.getResult0());
        this.clientSocket.shutdownOutput();
        serverThread.join();
    }

    @Test
    public void testBasicCleanup() throws ExecutionException, InterruptedException, TimeoutException {
        var server = new RpcSystem<>(this.serverNetwork, new TestCap0Impl());
        var demo = new Demo.TestCap0.Client(this.client.bootstrap());
        var request = demo.testMethod0Request();
        var params = request.getParams();
        params.setParam0(4321);
        var response = request.send();
        response.get();
        Assert.assertTrue(response.isDone());
        var results = response.get();
        Assert.assertEquals(params.getParam0(), results.getResult0());

        demo = null;
    }

    @Test
    public void testShutdown() throws InterruptedException, IOException {
        var server = new RpcSystem<>(this.serverNetwork, new TestCap0Impl());
        var demo = new Demo.TestCap0.Client(this.client.bootstrap());
        this.clientSocket.shutdownOutput();
        serverThread.join();
    }

    @Test
    public void testCallThrows() throws ExecutionException, InterruptedException {
        var impl = new Demo.TestCap0.Server() {
            public CompletableFuture<java.lang.Void> testMethod0(CallContext<Demo.TestParams0.Reader, Demo.TestResults0.Builder>  ctx) {
                return CompletableFuture.failedFuture(new RuntimeException("Call to testMethod0 failed"));
            }
            public CompletableFuture<java.lang.Void> testMethod1(CallContext<Demo.TestParams1.Reader, Demo.TestResults1.Builder>  ctx) {
                return CompletableFuture.completedFuture(null);
            }
        };

        var rpcSystem = new RpcSystem<>(this.serverNetwork, impl);

        var demoClient = new Demo.TestCap0.Client(this.client.bootstrap());
        {
            var request = demoClient.testMethod0Request();
            var response = request.send();
            while (!response.isDone()) {
                CompletableFuture.anyOf(response).exceptionally(exc -> { return null; });
            }
            Assert.assertTrue(response.isCompletedExceptionally());
        }

        // test that the system is still valid
        {
            var request = demoClient.testMethod1Request();
            var response = request.send();
            response.get();
            Assert.assertFalse(response.isCompletedExceptionally());
        }
    }

    @Test
    public void testReturnCap() throws ExecutionException, InterruptedException {
        // send a capability back from the server to the client
        var capServer = new TestCap0Impl();
        var rpcSystem = new RpcSystem<>(this.serverNetwork, capServer);
        var demoClient = new Demo.TestCap0.Client(this.client.bootstrap());
        var request = demoClient.testMethod1Request();
        var response = request.send();
        response.get();
        Assert.assertTrue(response.isDone());

        var results = response.get();
        var cap0 = results.getResult0();
        Assert.assertFalse(cap0.isNull());
        var cap1 = results.getResult1();
        Assert.assertFalse(cap1.isNull());
        var cap2 = results.getResult2();
        Assert.assertFalse(cap2.isNull());
    }
}
*/
