package org.capnproto;

import java.util.concurrent.CompletableFuture;

public final class Capability {

    public static abstract class BuilderContext {
        CapTableBuilder capTable;
    }

    public static class ReaderContext {
        CapTableReader capTable;
    }

    public static abstract class Factory<T>
            implements FromPointerReader<T>,
                       FromPointerBuilder<T> {
        @Override
        public T fromPointerReader(SegmentReader segment, CapTableReader capTable, int pointer, int nestingLimit) {
            var hook = WireHelpers.readCapabilityPointer(segment, capTable, pointer, 0);
            return newClient(hook);
        }

        @Override
        public T fromPointerBuilder(SegmentBuilder segment, CapTableBuilder capTable, int pointer) {
            var hook = WireHelpers.readCapabilityPointer(segment, capTable, pointer, 0);
            return newClient(hook);
        }

        @Override
        public T initFromPointerBuilder(SegmentBuilder segment, CapTableBuilder capTable, int pointer, int elementCount) {
            var hook = WireHelpers.readCapabilityPointer(segment, capTable, pointer, 0);
            return newClient(hook);
        }

        public abstract T newClient(ClientHook hook);
    }

    public static class Client {

        final ClientHook hook;

        public Client() {
            this.hook = null;
        }

        public Client(ClientHook hook) {
            this.hook = hook;
        }

        public Client(Server server) {
            this(makeLocalClient(server));
        }

        public Client(CompletableFuture<ClientHook> promise) {
            this(Capability.newLocalPromiseClient(promise));
        }

        public Client(Throwable exc) {
            this(newBrokenCap(exc));
        }

        public ClientHook getHook() {
            return this.hook;
        }

        private static ClientHook makeLocalClient(Server server) {
            return server.makeLocalClient();
        }

        CompletableFuture<?> whenResolved() {
            return hook.whenResolved();
        }

        Request<AnyPointer.Builder, AnyPointer.Reader> typelessRequest(
                long interfaceId,
                short methodId) {
            return hook.newCall(interfaceId, methodId);
        }

        protected <T, U> Request<T, U> newCall(FromPointerBuilder<T> builder,
                                            FromPointerReader<U> reader,
                                            long interfaceId, short methodId) {
            var request = hook.newCall(interfaceId, methodId);
            return new Request<T, U> (builder, reader, request.params, request.hook);
        }
    }

    public abstract static class Server {

        private static final Object BRAND = new Object();
        private ClientHook hook;

        ClientHook makeLocalClient() {
            return new LocalClient();
        }

        private final class LocalClient implements ClientHook {

            private CompletableFuture<java.lang.Void> resolveTask;
            private ClientHook resolved;
            private boolean blocked = false;
            private Exception brokenException;

            LocalClient() {
                Server.this.hook = this;
                startResolveTask();
            }

            @Override
            public Request<AnyPointer.Builder, AnyPointer.Reader> newCall(long interfaceId, short methodId) {
                var hook = new LocalRequest(interfaceId, methodId, this);
                var root = hook.message.getRoot(AnyPointer.factory);
                return new Request<>(AnyPointer.factory, AnyPointer.factory, root, hook);
            }

            @Override
            public VoidPromiseAndPipeline call(long interfaceId, short methodId, CallContextHook ctx) {
                assert !blocked: "Blocked condition not implemented";
                if (blocked) {
                    // TODO implement blocked behaviour
                    return null;
                }

                // TODO re-visit promises
                var promise = callInternal(interfaceId, methodId, ctx);
                var forked = promise.copy();

                CompletableFuture<PipelineHook> pipelinePromise = promise.thenApply(x -> {
                    ctx.releaseParams();
                    return new LocalPipeline(ctx);
                });

                var tailCall = ctx.onTailCall();
                // TODO implement tailCall
                if (tailCall != null) {
                    pipelinePromise = tailCall.applyToEither(pipelinePromise, pipeline -> pipeline);
                }

                return new VoidPromiseAndPipeline(forked, new QueuedPipeline(pipelinePromise));
            }

            @Override
            public CompletableFuture<java.lang.Void> whenResolved() {
                return null;
            }

            @Override
            public Object getBrand() {
                return BRAND;
            }

            CompletableFuture<?> callInternal(long interfaceId, short methodId, CallContextHook context) {
                var result = dispatchCall(
                        interfaceId,
                        methodId,
                        new CallContext<>(AnyPointer.factory, AnyPointer.factory, context));
                if (result.isStreaming()) {
                    // TODO streaming
                    return null;
                }
                else {
                    return result.getPromise();
                }
            }

            void startResolveTask() {
                var resolver = Server.this.shortenPath();
                if (resolver == null) {
                    return;
                }
                this.resolveTask = resolver.thenAccept(client -> {
                    this.resolved = client.hook;
                });
            }
        }

        public CompletableFuture<Client> shortenPath() {
            return null;
        }

        protected Client thisCap() {
            return new Client(this.hook);
        }

        protected static <Params, Results> CallContext<Params, Results> typedContext(
                FromPointerReader<Params> paramsFactory,
                FromPointerBuilder<Results> resultsFactory,
                CallContext<AnyPointer.Reader, AnyPointer.Builder> typeless) {
            return new CallContext<>(paramsFactory, resultsFactory, typeless.hook);
        }

        protected abstract DispatchCallResult dispatchCall(
                long interfaceId, short methodId,
                CallContext<AnyPointer.Reader, AnyPointer.Builder> context);

        protected static DispatchCallResult streamResult(CompletableFuture<?> result) {
            // For streaming calls, we need to add an evalNow() here so that exceptions thrown
            // directly from the call can propagate to later calls. If we don't capture the
            // exception properly then the caller will never find out that this is a streaming
            // call (indicated by the boolean in the return value) so won't know to propagate
            // the exception.
            // TODO the above comment...
            return new DispatchCallResult(result, true);
        }

        protected static DispatchCallResult result(CompletableFuture<?> result) {
            return new DispatchCallResult(result, false);
        }

        protected static CompletableFuture<?> internalUnimplemented(String actualInterfaceName, long requestedTypeId) {
            return CompletableFuture.failedFuture(RpcException.unimplemented(
                    "Method not implemented. " + actualInterfaceName + " " + requestedTypeId));
        }

        protected static CompletableFuture<?> internalUnimplemented(String interfaceName, long typeId, short methodId) {
            return CompletableFuture.failedFuture(RpcException.unimplemented(
                    "Method not implemented. " + interfaceName + " " + typeId + " " + methodId));
        }

        protected static CompletableFuture<?> internalUnimplemented(String interfaceName, String methodName, long typeId, short methodId) {
            return CompletableFuture.failedFuture(RpcException.unimplemented(
                    "Method not implemented. " + interfaceName + " " + typeId + " " + methodName + " " + methodId));
        }
    }

    public static ClientHook newLocalPromiseClient(CompletableFuture<ClientHook> promise) {
        return new QueuedClient(promise);
    }

    public static PipelineHook newLocalPromisePipeline(CompletableFuture<PipelineHook> promise) {
        return new QueuedPipeline(promise);
    }

    static class LocalRequest implements RequestHook {

        final MessageBuilder message = new MessageBuilder();
        final long interfaceId;
        final short methodId;
        ClientHook client;

        LocalRequest(long interfaceId, short methodId, ClientHook client) {
            this.interfaceId = interfaceId;
            this.methodId = methodId;
            this.client = client;
        }

        @Override
        public RemotePromise<AnyPointer.Reader> send() {
            var cancelPaf = new CompletableFuture<java.lang.Void>();
            var context = new LocalCallContext(message, client, cancelPaf);
            var promiseAndPipeline = client.call(interfaceId, methodId, context);
            var promise = promiseAndPipeline.promise.thenApply(x -> {
                context.getResults(); // force allocation
                return context.response;
            });

            return new RemotePromise<AnyPointer.Reader>(promise, promiseAndPipeline.pipeline);
        }

        @Override
        public Object getBrand() {
            return null;
        }
    }

    static final class LocalPipeline implements PipelineHook {
        final CallContextHook context;
        final AnyPointer.Reader results;

        public LocalPipeline(CallContextHook context) {
            this.context = context;
            this.results = context.getResults().asReader();
        }

        @Override
        public final ClientHook getPipelinedCap(PipelineOp[] ops) {
            return this.results.getPipelinedCap(ops);
        }
    }

    static class LocalResponse implements ResponseHook {
        final MessageBuilder message = new MessageBuilder();
    }

    static class LocalCallContext implements CallContextHook {

        final CompletableFuture<?> cancelAllowed;
        MessageBuilder request;
        Response response;
        AnyPointer.Builder responseBuilder;
        ClientHook clientRef;

        LocalCallContext(MessageBuilder request,
                         ClientHook clientRef,
                         CompletableFuture<?> cancelAllowed) {
            this.request = request;
            this.clientRef = clientRef;
            this.cancelAllowed = cancelAllowed;
        }

        @Override
        public AnyPointer.Reader getParams() {
            return request.getRoot(AnyPointer.factory).asReader();
        }

        @Override
        public void releaseParams() {
            this.request = null;
        }

        @Override
        public AnyPointer.Builder getResults() {
            if (this.response == null) {
                var localResponse = new LocalResponse();
                this.responseBuilder = localResponse.message.getRoot(AnyPointer.factory);
                this.response = new Response(this.responseBuilder.asReader(), localResponse);
            }
            return this.responseBuilder;
        }

        @Override
        public void allowCancellation() {
            this.cancelAllowed.complete(null);
        }

        @Override
        public CompletableFuture<java.lang.Void> tailCall(RequestHook request) {
            // TODO implement tailCall
            return null;
        }

        @Override
        public CompletableFuture<PipelineHook> onTailCall() {
            // TODO implement onTailCall
            return null;
        }

        @Override
        public ClientHook.VoidPromiseAndPipeline directTailCall(RequestHook request) {
            // TODO implement directTailCall
            return null;
        }
    }

    public static ClientHook newBrokenCap(String reason) {
        return newBrokenClient(reason, false, ClientHook.BROKEN_CAPABILITY_BRAND);
    }

    public static ClientHook newBrokenCap(Throwable exc) {
        return newBrokenClient(exc, false, ClientHook.BROKEN_CAPABILITY_BRAND);
    }

    public static ClientHook newNullCap() {
        return newBrokenClient(new RuntimeException("Called null capability"), true, ClientHook.NULL_CAPABILITY_BRAND);
    }

    static private ClientHook newBrokenClient(String reason, boolean resolved, Object brand) {
        return newBrokenClient(new RuntimeException(reason), resolved, brand);
    }

    static private ClientHook newBrokenClient(Throwable exc, boolean resolved, Object brand) {
        return new ClientHook() {
            @Override
            public Request<AnyPointer.Builder, AnyPointer.Reader> newCall(long interfaceId, short methodId) {
                return Request.newBrokenRequest(exc);
            }

            @Override
            public VoidPromiseAndPipeline call(long interfaceId, short methodId, CallContextHook context) {
                return new VoidPromiseAndPipeline(CompletableFuture.failedFuture(exc), null);
            }

            @Override
            public CompletableFuture<ClientHook> whenMoreResolved() {
                if (resolved) {
                    return null;
                } else {
                    return CompletableFuture.failedFuture(exc);
                }
            }

            @Override
            public Object getBrand() {
                return brand;
            }
        };
    }

}
