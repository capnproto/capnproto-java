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

import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

class Counter {
    private int count = 0;

    public void inc() {
        count++;
    }

    public int value() {
        return count;
    }
}

class TestInterfaceImpl extends org.capnproto.test.Test.TestInterface.Server {


    final Counter counter;

    TestInterfaceImpl(Counter counter) {
        this.counter = counter;
    }

    @Override
    protected CompletableFuture<java.lang.Void> foo(CallContext<org.capnproto.test.Test.TestInterface.FooParams.Reader, org.capnproto.test.Test.TestInterface.FooResults.Builder> ctx) {
        this.counter.inc();
        var params = ctx.getParams();
        var result = ctx.getResults();
        Assert.assertEquals(123, params.getI());
        Assert.assertTrue(params.getJ());
        result.setX("foo");
        return CompletableFuture.completedFuture(null);
    }
}

class TestExtendsImpl extends org.capnproto.test.Test.TestExtends2.Server {

    final Counter counter;

    TestExtendsImpl(Counter counter) {
        this.counter = counter;
    }

    @Override
    protected CompletableFuture<java.lang.Void> foo(CallContext<org.capnproto.test.Test.TestInterface.FooParams.Reader, org.capnproto.test.Test.TestInterface.FooResults.Builder> context) {
        counter.inc();
        var params = context.getParams();
        var result = context.getResults();
        Assert.assertEquals(321, params.getI());
        Assert.assertFalse(params.getJ());
        result.setX("bar");
        return CompletableFuture.completedFuture(null);
    }

    @Override
    protected CompletableFuture<java.lang.Void> grault(CallContext<org.capnproto.test.Test.TestExtends.GraultParams.Reader, org.capnproto.test.Test.TestAllTypes.Builder> context) {
        counter.inc();
        context.releaseParams();
        TestUtil.initTestMessage(context.getResults());
        return CompletableFuture.completedFuture(null);
    }
}

class TestPipelineImpl extends org.capnproto.test.Test.TestPipeline.Server {

    final Counter counter;

    TestPipelineImpl(Counter counter) {
        this.counter = counter;
    }

    @Override
    protected CompletableFuture<java.lang.Void> getCap(CallContext<org.capnproto.test.Test.TestPipeline.GetCapParams.Reader, org.capnproto.test.Test.TestPipeline.GetCapResults.Builder> ctx) {
        this.counter.inc();
        var params = ctx.getParams();
        Assert.assertEquals(234, params.getN());
        var cap = params.getInCap();
        ctx.releaseParams();

        var request = cap.fooRequest();
        var fooParams = request.getParams();
        fooParams.setI(123);
        fooParams.setJ(true);

        return request.send().thenAccept(response -> {
            Assert.assertEquals("foo", response.getX().toString());
            var result = ctx.getResults();
            result.setS("bar");

            org.capnproto.test.Test.TestExtends.Server server = new TestExtendsImpl(this.counter);
            result.initOutBox().setCap(server);
        });
    }

    @Override
    protected CompletableFuture<java.lang.Void> getAnyCap(CallContext<org.capnproto.test.Test.TestPipeline.GetAnyCapParams.Reader, org.capnproto.test.Test.TestPipeline.GetAnyCapResults.Builder> context) {
        return super.getAnyCap(context);
    }
}

public class CapabilityTest {

    @Test
    public void testPipelining() throws ExecutionException, InterruptedException {
        var callCount = new Counter();
        var chainedCallCount = new Counter();

        var client = new org.capnproto.test.Test.TestPipeline.Client(
                new TestPipelineImpl(callCount));

        var request = client.getCapRequest();
        var params = request.getParams();
        params.setN(234);
        params.setInCap(new org.capnproto.test.Test.TestInterface.Client(
                new TestInterfaceImpl(chainedCallCount)));

        var promise = request.send();
        var outbox = promise.getOutBox();
        var pipelineRequest = outbox.getCap().fooRequest();
        pipelineRequest.getParams().setI(321);
        var pipelinePromise = pipelineRequest.send();
        var pipelineRequest2 = new org.capnproto.test.Test.TestExtends.Client(promise.getOutBox().getCap()).graultRequest();
        var pipelinePromise2 = pipelineRequest2.send();

        // Hmm, we have no means to defer the evaluation of callInternal. The best we can do is
        // wait for the client to have resolved.

        //Assert.assertEquals(0, callCount.value());
        //Assert.assertEquals(0, chainedCallCount.value());

        var response = pipelinePromise.get();
        Assert.assertEquals("bar", response.getX().toString());
        var response2 = pipelinePromise2.get();
        TestUtil.checkTestMessage(response2);
        Assert.assertEquals(3, callCount.value());
        Assert.assertEquals(1, chainedCallCount.value());
    }

    @Test
    public void testGenerics() {
        var factory = org.capnproto.test.Test.TestGenerics.newFactory(org.capnproto.test.Test.TestAllTypes.factory, AnyPointer.factory);

    }
}