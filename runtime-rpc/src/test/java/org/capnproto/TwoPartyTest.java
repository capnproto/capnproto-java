package org.capnproto;

import org.capnproto.rpctest.*;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.function.ThrowingRunnable;

import java.io.IOException;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.ExecutionException;

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

    AsynchronousServerSocketChannel serverAcceptSocket;
    AsynchronousSocketChannel serverSocket;
    AsynchronousSocketChannel clientSocket;
    TwoPartyClient client;
    org.capnproto.TwoPartyVatNetwork serverNetwork;
    Thread serverThread;

    @Before
    public void setUp() throws Exception {
        this.serverAcceptSocket = AsynchronousServerSocketChannel.open();
        this.serverAcceptSocket.bind(null);

        this.clientSocket = AsynchronousSocketChannel.open();
        this.clientSocket.connect(this.serverAcceptSocket.getLocalAddress()).get();
        this.client = new TwoPartyClient(clientSocket);
        //this.client.getNetwork().setTap(new Tap());

        this.serverSocket = serverAcceptSocket.accept().get();
        this.serverNetwork = new org.capnproto.TwoPartyVatNetwork(this.serverSocket, RpcTwoPartyProtocol.Side.SERVER);
        //this.serverNetwork.setTap(new Tap());
        //this.serverNetwork.dumper.addSchema(Demo.TestCap1);
        this.serverThread = runServer(this.serverNetwork);
    }

    @After
    public void tearDown() throws Exception {
        this.clientSocket.close();
        this.serverSocket.close();
        this.serverAcceptSocket.close();
        this.serverThread.join();
        this.client = null;
    }

    @org.junit.Test
    public void testNullCap() throws ExecutionException, InterruptedException {
        var server = new RpcSystem<>(this.serverNetwork, new Capability.Client());
        var cap = this.client.bootstrap();
        var resolved = cap.whenResolved();
        resolved.get();
    }

    @org.junit.Test
    public void testBasic() throws InterruptedException, IOException {

        var callCount = new Counter();
        var server = new RpcSystem<>(this.serverNetwork, new RpcTestUtil.TestInterfaceImpl(callCount));

        var client = new Test.TestInterface.Client(this.client.bootstrap());
        var request1 = client.fooRequest();
        request1.getParams().setI(123);
        request1.getParams().setJ(true);

        var promise1 = request1.send();

        var request2 = client.bazRequest();
        RpcTestUtil.initTestMessage(request2.getParams().initS());
        var promise2 = request2.send();

        boolean barFailed = false;
        var request3 = client.barRequest();
        var promise3 = request3.send()
                .thenAccept(results -> Assert.fail("Expected bar() to fail"))
                .exceptionally(exc -> null);

        var response1 = promise1.join();
        Assert.assertEquals("foo", response1.getX().toString());

        promise2.join();
        promise3.join();

        Assert.assertEquals(2, callCount.value());
        this.clientSocket.shutdownOutput();
        serverThread.join();
    }

    @org.junit.Test
    public void testDisconnect() throws IOException {
        this.serverSocket.shutdownOutput();
        this.serverNetwork.close();
        this.serverNetwork.onDisconnect().join();
    }

    @org.junit.Test
    public void testPipelining() throws IOException {
        var callCount = new Counter();
        var chainedCallCount = new Counter();

        var server = new RpcSystem<>(this.serverNetwork, new RpcTestUtil.TestPipelineImpl(callCount));
        var client = new Test.TestPipeline.Client(this.client.bootstrap());

        {
            var request = client.getCapRequest();
            request.getParams().setN(234);
            request.getParams().setInCap(new RpcTestUtil.TestInterfaceImpl(chainedCallCount));

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
            RpcTestUtil.checkTestMessage(response2);

            Assert.assertEquals(1, chainedCallCount.value());
        }

        /*
        // disconnect the server
        //this.serverSocket.shutdownOutput();
        this.serverNetwork.close();
        this.serverNetwork.onDisconnect().join();

        {
            // Use the now-broken capability.
            var request = client.getCapRequest();
            request.getParams().setN(234);
            request.getParams().setInCap(new RpcTestUtil.TestInterfaceImpl(chainedCallCount));

            var promise = request.send();

            var pipelineRequest = promise.getOutBox().getCap().fooRequest();
            pipelineRequest.getParams().setI(321);
            var pipelinePromise = pipelineRequest.send();

            var pipelineRequest2 = new Test.TestExtends.Client(promise.getOutBox().getCap()).graultRequest();
            var pipelinePromise2 = pipelineRequest2.send();

            Assert.assertThrows(Exception.class, () -> pipelinePromise.join());
            Assert.assertThrows(Exception.class, () -> pipelinePromise2.join());

            Assert.assertEquals(3, callCount.value());
            Assert.assertEquals(1, chainedCallCount.value());
        }

         */
    }
/*
    @org.junit.Test
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

    @org.junit.Test
    public void testShutdown() throws InterruptedException, IOException {
        var server = new RpcSystem<>(this.serverNetwork, new TestCap0Impl());
        var demo = new Demo.TestCap0.Client(this.client.bootstrap());
        this.clientSocket.shutdownOutput();
        serverThread.join();
    }

    @org.junit.Test
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

    @org.junit.Test
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
    */
}
