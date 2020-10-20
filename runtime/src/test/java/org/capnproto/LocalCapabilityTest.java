package org.capnproto;

import org.capnproto.demo.Demo;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class LocalCapabilityTest {

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
            protected CompletableFuture<java.lang.Void> method0(CallContext<Demo.Iface0.Method0Params.Reader, Demo.Iface0.Method0Results.Builder> ctx) {
                method0called = true;
                return CompletableFuture.completedFuture(null);
            }
        };

        var server1 = new Demo.Iface1.Server() {
            @Override
            protected CompletableFuture<java.lang.Void> method1(CallContext<Demo.Iface1.Method1Params.Reader, Demo.Iface1.Method1Results.Builder> ctx) {
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
