package org.capnproto;

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

import org.capnproto.AnyPointer;
import org.capnproto.CallContext;
import org.capnproto.Capability;
import org.capnproto.RpcException;
import org.capnproto.rpctest.Test;

import org.junit.Assert;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

class Counter {
    private int count = 0;
    int inc() { return count++; }
    int value() { return count; }
}

class TestExtendsImpl extends Test.TestExtends2.Server {

    final Counter counter;

    TestExtendsImpl(Counter counter) {
        this.counter = counter;
    }

    @Override
    protected CompletableFuture<java.lang.Void> foo(CallContext<Test.TestInterface.FooParams.Reader, Test.TestInterface.FooResults.Builder> context) {
        counter.inc();
        var params = context.getParams();
        var result = context.getResults();
        Assert.assertEquals(321, params.getI());
        Assert.assertFalse(params.getJ());
        result.setX("bar");
        return CompletableFuture.completedFuture(null);
    }

    @Override
    protected CompletableFuture<java.lang.Void> grault(CallContext<Test.TestExtends.GraultParams.Reader, Test.TestAllTypes.Builder> context) {
        counter.inc();
        context.releaseParams();
        RpcTestUtil.initTestMessage(context.getResults());
        return CompletableFuture.completedFuture(null);
    }
}

public final class CapabilityTest {

    @org.junit.Test
    public void testBasic() {
        var callCount = new Counter();
        var client = new Test.TestInterface.Client(
                new RpcTestUtil.TestInterfaceImpl(callCount));

        var request1 = client.fooRequest();
        request1.getParams().setI(123);
        request1.getParams().setJ(true);
        var promise1 = request1.send();

        var request2 = client.bazRequest();
        RpcTestUtil.initTestMessage(request2.getParams().initS());
        var promise2 = request2.send();

        boolean barFailed = false;
        var request3 = client.barRequest();
        var promise3 = request3.send().whenComplete((value, exc) -> {
            Assert.assertNotNull(exc);
            Assert.assertTrue(exc instanceof RpcException);
            var rpcExc = (RpcException)exc;
            Assert.assertEquals(RpcException.Type.UNIMPLEMENTED, rpcExc.getType());
        });
    }

    @org.junit.Test
    public void testInheritance() throws ExecutionException, InterruptedException {
        var callCount = new Counter();

        var client1 = new Test.TestExtends.Client(
                new TestExtendsImpl(callCount));

        Test.TestInterface.Client client2 = client1;
        var client = (Test.TestExtends.Client)client2;

        var request1 = client.fooRequest();
        request1.getParams().setI(321);
        var promise1 = request1.send();

        var request2 = client.graultRequest();
        var promise2 = request2.send();

        // Hmm, we have no means to defer the evaluation of callInternal.
        //Assert.assertEquals(0, callCount.value());

        var response2 = promise2.get();
        RpcTestUtil.checkTestMessage(response2);

        var response1 = promise1.get();
        Assert.assertEquals("bar", response1.getX().toString());
        Assert.assertEquals(2, callCount.value());
    }

    @org.junit.Test
    public void testPipelining() throws ExecutionException, InterruptedException {
        var callCount = new Counter();
        var chainedCallCount = new Counter();

        var client = new Test.TestPipeline.Client(
                new RpcTestUtil.TestPipelineImpl(callCount));

        var request = client.getCapRequest();
        var params = request.getParams();
        params.setN(234);
        params.setInCap(new Test.TestInterface.Client(
                new RpcTestUtil.TestInterfaceImpl(chainedCallCount)));

        var promise = request.send();
        var outbox = promise.getOutBox();
        var pipelineRequest = outbox.getCap().fooRequest();
        pipelineRequest.getParams().setI(321);
        var pipelinePromise = pipelineRequest.send();
        var pipelineRequest2 = new Test.TestExtends.Client(promise.getOutBox().getCap()).graultRequest();
        var pipelinePromise2 = pipelineRequest2.send();

        // Hmm, we have no means to defer the evaluation of callInternal. The best we can do is
        // wait for the client to have resolved.

        //Assert.assertEquals(0, callCount.value());
        //Assert.assertEquals(0, chainedCallCount.value());

        var response = pipelinePromise.get();
        Assert.assertEquals("bar", response.getX().toString());
        var response2 = pipelinePromise2.get();
        RpcTestUtil.checkTestMessage(response2);
        Assert.assertEquals(3, callCount.value());
        Assert.assertEquals(1, chainedCallCount.value());
    }

    @org.junit.Test
    public void testTailCall() {
        var calleeCallCount = new Counter();
        var callerCallCount = new Counter();
        var callee = new Test.TestTailCallee.Client(
                new RpcTestUtil.TestTailCalleeImpl(calleeCallCount));

        var caller = new Test.TestTailCaller.Client(
                new RpcTestUtil.TestTailCallerImpl(callerCallCount));

        var request = caller.fooRequest();
        request.getParams().setI(456);
        request.getParams().setCallee(callee);

        var promise = request.send();

        var dependentCall0 = promise.getC().getCallSequenceRequest().send();

        var response = promise.join();
        Assert.assertEquals(456, response.getI());
        Assert.assertEquals(456, response.getI());

        var dependentCall1 = promise.getC().getCallSequenceRequest().send();

        var dependentCall2 = response.getC().getCallSequenceRequest().send();

        Assert.assertEquals(0, dependentCall0.join().getN());
        Assert.assertEquals(1, dependentCall1.join().getN());
        Assert.assertEquals(2, dependentCall2.join().getN());

        Assert.assertEquals(1, calleeCallCount.value());
        Assert.assertEquals(1, callerCallCount.value());
    }

    class TestThisCap extends Test.TestInterface.Server {

        Counter counter;

        TestThisCap(Counter counter) {
            this.counter = counter;
        }

        Test.TestInterface.Client getSelf() {
            return this.thisCap();
        }

        @Override
        protected CompletableFuture<java.lang.Void> bar(CallContext<Test.TestInterface.BarParams.Reader, Test.TestInterface.BarResults.Builder> context) {
            this.counter.inc();
            return READY_NOW;
        }
    }

    @org.junit.Test
    public void testGenerics() {
        var factory = Test.TestGenerics.newFactory(Test.TestAllTypes.factory, AnyPointer.factory);
    }

    @org.junit.Test
    public void thisCap() {
        var callCount = new Counter();
        var server = new TestThisCap(callCount);
        var client = new Test.TestInterface.Client(server);
        client.barRequest().send().join();
        Assert.assertEquals(1, callCount.value());

        var client2 = server.getSelf();
        Assert.assertEquals(1, callCount.value());
        client2.barRequest().send().join();
        Assert.assertEquals(2, callCount.value());
        client = null;
        Assert.assertEquals(2, callCount.value());
        client2.barRequest().send().join();
        Assert.assertEquals(3, callCount.value());
    }

    @org.junit.Test
    public void testStreamingCallsBlockSubsequentCalls() {
        var server = new RpcTestUtil.TestStreamingImpl();
        var cap = new Test.TestStreaming.Client(server);

        CompletableFuture<java.lang.Void> promise1 = null;
        CompletableFuture<java.lang.Void> promise2 = null;
        CompletableFuture<java.lang.Void> promise3 = null;

        {
            var req = cap.doStreamIRequest();
            req.getParams().setI(123);
            promise1 = req.send();
        }

        {
            var req = cap.doStreamJRequest();
            req.getParams().setJ(321);
            promise2 = req.send();
        }

        {
            var req = cap.doStreamIRequest();
            req.getParams().setI(456);
            promise3 = req.send();
        }

        var promise4 = cap.finishStreamRequest().send();

        // Only the first streaming call has executed
        Assert.assertEquals(123, server.iSum);
        Assert.assertEquals(0, server.jSum);

        // Complete first streaming call
        Assert.assertNotNull(server.fulfiller);
        server.fulfiller.complete(null);

        // second streaming call unblocked
        Assert.assertEquals(123, server.iSum);
        Assert.assertEquals(321, server.jSum);

        // complete second streaming call
        Assert.assertNotNull(server.fulfiller);
        server.fulfiller.complete(null);

        // third streaming call unblocked
        Assert.assertEquals(579, server.iSum);
        Assert.assertEquals(321, server.jSum);

        // complete third streaming call
        Assert.assertNotNull(server.fulfiller);
        server.fulfiller.complete(null);

        // last call is unblocked
        var result = promise4.join();
        Assert.assertEquals(579, result.getTotalI());
        Assert.assertEquals(321, result.getTotalJ());
    }
}
