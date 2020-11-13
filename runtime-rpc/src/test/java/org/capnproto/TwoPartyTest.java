package org.capnproto;

import org.capnproto.rpctest.*;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

import java.io.IOException;
import java.nio.channels.AsynchronousByteChannel;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

public class TwoPartyTest {

    static final class PipeThread {
        Thread thread;
        AsynchronousByteChannel channel;

        static PipeThread newPipeThread(Consumer<AsynchronousByteChannel> startFunc) throws Exception {
            var pipeThread = new PipeThread();
            var serverAcceptSocket = AsynchronousServerSocketChannel.open();
            serverAcceptSocket.bind(null);
            var clientSocket = AsynchronousSocketChannel.open();

            pipeThread.thread = new Thread(() -> {
                try {
                    var serverSocket = serverAcceptSocket.accept().get();
                    startFunc.accept(serverSocket);
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            });
            pipeThread.thread.start();
            pipeThread.thread.setName("TwoPartyTest server");

            clientSocket.connect(serverAcceptSocket.getLocalAddress()).get();
            pipeThread.channel = clientSocket;
            return pipeThread;
        }
    }

    PipeThread runServer(Capability.Server bootstrapInterface) throws Exception {
        return runServer(new Capability.Client(bootstrapInterface));
    }

    PipeThread runServer(Capability.Client bootstrapInterface) throws Exception {
        return PipeThread.newPipeThread(channel -> {
            var network = new TwoPartyVatNetwork(channel, RpcTwoPartyProtocol.Side.SERVER);
            var system = new RpcSystem<>(network, bootstrapInterface);
            network.onDisconnect().join();
        });
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @org.junit.Test
    public void testNullCap() throws Exception {
        var pipe = runServer(new Capability.Client());
        var rpcClient = new TwoPartyClient(pipe.channel);
        var client = rpcClient.bootstrap();
        var resolved = client.whenResolved();
        resolved.get();
    }

    @org.junit.Test
    public void testBasic() throws Exception {
        var callCount = new Counter();
        var pipe = runServer(new RpcTestUtil.TestInterfaceImpl(callCount));
        var rpcClient = new TwoPartyClient(pipe.channel);
        var client = new Test.TestInterface.Client(rpcClient.bootstrap());
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
    }

    @org.junit.Test
    public void testDisconnect() throws IOException {
        //this.serverSocket.shutdownOutput();
        //this.serverNetwork.close();
        //this.serverNetwork.onDisconnect().join();
    }

    @org.junit.Test
    public void testPipelining() throws Exception {
        var callCount = new Counter();
        var chainedCallCount = new Counter();
        var pipe = runServer(new RpcTestUtil.TestPipelineImpl(callCount));
        var rpcClient = new TwoPartyClient(pipe.channel);
        var client = new Test.TestPipeline.Client(rpcClient.bootstrap());

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

        // disconnect the client
        ((AsynchronousSocketChannel)pipe.channel).shutdownOutput();
        rpcClient.onDisconnect().join();

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
    }

    @org.junit.Test
    public void testAbort() {

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
