package org.capnproto;

import org.capnproto.rpctest.Test;
import org.junit.Assert;

import java.util.concurrent.CompletableFuture;

class RpcTestUtil {

    static void initTestMessage(Test.TestAllTypes.Builder builder) {
        builder.setVoidField(Void.VOID);
        builder.setBoolField(true);
        builder.setInt8Field((byte) -123);
        builder.setInt16Field((short) -12345);
        builder.setInt32Field(-12345678);
        builder.setInt64Field(-123456789012345L);
        builder.setUInt8Field((byte) 234);
        builder.setUInt16Field((short) 45678);
        builder.setUInt32Field((int) 3456789012L);
        builder.setUInt64Field(1234567890123456789L);
        builder.setFloat32Field(1234.5f);
        builder.setFloat64Field(-123e45);
        builder.setTextField("foo");
    }

    static void checkTestMessage(Test.TestAllTypes.Reader reader) {
        Assert.assertEquals(Void.VOID, reader.getVoidField());
        Assert.assertTrue(reader.getBoolField());
        Assert.assertEquals((byte)-123, reader.getInt8Field());
        Assert.assertEquals((short)-12345, reader.getInt16Field());
        Assert.assertEquals(-12345678, reader.getInt32Field());
        Assert.assertEquals(-123456789012345L, reader.getInt64Field());
        Assert.assertEquals((byte)234, reader.getUInt8Field());
        Assert.assertEquals((short)45678, reader.getUInt16Field());
        Assert.assertEquals((int) 3456789012L, reader.getUInt32Field());
        Assert.assertEquals(1234567890123456789L, reader.getUInt64Field());
        Assert.assertEquals(null, 1234.5f, reader.getFloat32Field(), 0.1f);
        Assert.assertEquals(null, -123e45, reader.getFloat64Field(), 0.1f);
        Assert.assertEquals("foo", reader.getTextField().toString());
    }

    static class TestInterfaceImpl extends Test.TestInterface.Server {

        final Counter counter;

        TestInterfaceImpl(Counter counter) {
            this.counter = counter;
        }

        @Override
        protected CompletableFuture<java.lang.Void> foo(CallContext<Test.TestInterface.FooParams.Reader, Test.TestInterface.FooResults.Builder> ctx) {
            this.counter.inc();
            var params = ctx.getParams();
            var result = ctx.getResults();
            Assert.assertEquals(123, params.getI());
            Assert.assertTrue(params.getJ());
            result.setX("foo");
            return Capability.Server.READY_NOW;
        }

        @Override
        protected CompletableFuture<java.lang.Void> baz(CallContext<Test.TestInterface.BazParams.Reader, Test.TestInterface.BazResults.Builder> context) {
            this.counter.inc();
            var params = context.getParams();
            checkTestMessage(params.getS());
            context.releaseParams();
            return Capability.Server.READY_NOW;
        }
    }

    static class TestTailCallerImpl extends Test.TestTailCaller.Server {

        private final Counter count;

        public TestTailCallerImpl(Counter count) {
            this.count = count;
        }

        @Override
        protected CompletableFuture<java.lang.Void> foo(CallContext<Test.TestTailCaller.FooParams.Reader, Test.TestTailCallee.TailResult.Builder> context) {
            this.count.inc();
            var params = context.getParams();
            var tailRequest = params.getCallee().fooRequest();
            tailRequest.getParams().setI(params.getI());
            tailRequest.getParams().setT("from TestTailCaller");
            return context.tailCall(tailRequest);
        }

        public int getCount() {
            return this.count.value();
        }
    }

    static class HandleImpl extends Test.TestHandle.Server {
        final Counter count;

        HandleImpl(Counter count) {
            this.count = count;
            count.inc();
        }
    }

    static class TestMoreStuffImpl extends Test.TestMoreStuff.Server {

        private final Counter callCount;
        private final Counter handleCount;
        private Test.TestInterface.Client clientToHold;

        public TestMoreStuffImpl(Counter callCount, Counter handleCount) {
            this.callCount = callCount;
            this.handleCount = handleCount;
        }

        @Override
        protected CompletableFuture<java.lang.Void> echo(CallContext<Test.TestMoreStuff.EchoParams.Reader, Test.TestMoreStuff.EchoResults.Builder> context) {
            this.callCount.inc();
            var params = context.getParams();
            var result = context.getResults();
            result.setCap(params.getCap());
            return READY_NOW;
        }

        @Override
        protected CompletableFuture<java.lang.Void> getHandle(CallContext<Test.TestMoreStuff.GetHandleParams.Reader, Test.TestMoreStuff.GetHandleResults.Builder> context) {
            context.getResults().setHandle(new HandleImpl(this.handleCount));
            return READY_NOW;
        }

        @Override
        protected CompletableFuture<java.lang.Void> getCallSequence(CallContext<Test.TestCallOrder.GetCallSequenceParams.Reader, Test.TestCallOrder.GetCallSequenceResults.Builder> context) {
            var result = context.getResults();
            result.setN(this.callCount.inc());
            return READY_NOW;
        }

        @Override
        protected CompletableFuture<java.lang.Void> callFoo(CallContext<Test.TestMoreStuff.CallFooParams.Reader, Test.TestMoreStuff.CallFooResults.Builder> context) {
            this.callCount.inc();
            var params = context.getParams();
            var cap = params.getCap();
            var request = cap.fooRequest();
            request.getParams().setI(123);
            request.getParams().setJ(true);

            return request.send().thenAccept(response -> {
                Assert.assertEquals("foo", response.getX().toString());
                context.getResults().setS("bar");
            });
        }

        @Override
        protected CompletableFuture<java.lang.Void> callFooWhenResolved(CallContext<Test.TestMoreStuff.CallFooWhenResolvedParams.Reader, Test.TestMoreStuff.CallFooWhenResolvedResults.Builder> context) {
            this.callCount.inc();
            var params = context.getParams();
            var cap = params.getCap();

            return cap.whenResolved().thenCompose(void_ -> {
                var request = cap.fooRequest();
                request.getParams().setI(123);
                request.getParams().setJ(true);

                return request.send().thenAccept(response -> {
                    Assert.assertEquals("foo", response.getX().toString());
                    context.getResults().setS("bar");
                });
            });
        }

        @Override
        protected CompletableFuture<java.lang.Void> hold(CallContext<Test.TestMoreStuff.HoldParams.Reader, Test.TestMoreStuff.HoldResults.Builder> context) {
            this.callCount.inc();
            var params = context.getParams();
            this.clientToHold = params.getCap();
            return READY_NOW;
        }

        @Override
        protected CompletableFuture<java.lang.Void> callHeld(CallContext<Test.TestMoreStuff.CallHeldParams.Reader, Test.TestMoreStuff.CallHeldResults.Builder> context) {
            this.callCount.inc();
            var request = this.clientToHold.fooRequest();
            request.getParams().setI(123);
            request.getParams().setJ(true);
            return request.send().thenAccept(response -> {
                Assert.assertEquals("foo", response.getX().toString());
                context.getResults().setS("bar");
            });
        }

        @Override
        protected CompletableFuture<java.lang.Void> getHeld(CallContext<Test.TestMoreStuff.GetHeldParams.Reader, Test.TestMoreStuff.GetHeldResults.Builder> context) {
            this.callCount.inc();
            var result = context.getResults();
            result.setCap(this.clientToHold);
            return READY_NOW;
        }
    }

    static class TestTailCalleeImpl extends Test.TestTailCallee.Server {

        private final Counter count;

        public TestTailCalleeImpl(Counter count) {
            this.count = count;
        }

        @Override
        protected CompletableFuture<java.lang.Void> foo(CallContext<Test.TestTailCallee.FooParams.Reader, Test.TestTailCallee.TailResult.Builder> context) {
            this.count.inc();

            var params = context.getParams();
            var results = context.getResults();

            results.setI(params.getI());
            results.setT(params.getT());
            results.setC(new TestCallOrderImpl());
            return READY_NOW;
        }
    }

    static class TestPipelineImpl extends Test.TestPipeline.Server {

        final Counter callCount;

        TestPipelineImpl(Counter callCount) {
            this.callCount = callCount;
        }

        @Override
        protected CompletableFuture<java.lang.Void> getCap(CallContext<Test.TestPipeline.GetCapParams.Reader, Test.TestPipeline.GetCapResults.Builder> ctx) {
            this.callCount.inc();
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

                Test.TestExtends.Server server = new TestExtendsImpl(this.callCount);
                result.initOutBox().setCap(server);
            });
        }

        @Override
        protected CompletableFuture<java.lang.Void> getAnyCap(CallContext<Test.TestPipeline.GetAnyCapParams.Reader, Test.TestPipeline.GetAnyCapResults.Builder> context) {
            this.callCount.inc();
            var params = context.getParams();
            Assert.assertEquals(234, params.getN());

            var cap = params.getInCap();
            context.releaseParams();

            var request = new Test.TestInterface.Client(cap).fooRequest();
            request.getParams().setI(123);
            request.getParams().setJ(true);

            return request.send().thenAccept(response -> {
               Assert.assertEquals("foo", response.getX().toString());

               var result = context.getResults();
               result.setS("bar");
               result.initOutBox().setCap(new TestExtendsImpl(callCount));
            });
        }
    }
}
