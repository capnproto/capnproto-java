package org.capnproto;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public final class Capability {

    public static abstract class BuilderContext {
        CapTableBuilder capTable;
    }

    public static class ReaderContext {
        CapTableReader capTable;
    }

    public static abstract class Factory<T extends ClientBase>
            implements FromPointerReader<T>,
                       FromPointerBuilder<T>,
                       SetPointerBuilder<T, T> {

        public abstract T newClient(ClientHook hook);

        @Override
        public T fromPointerReader(SegmentReader segment, CapTableReader capTable, int pointer, int nestingLimit) {
            return newClient(
                    WireHelpers.readCapabilityPointer(segment, capTable, pointer, 0));
        }

        @Override
        public T fromPointerBuilder(SegmentBuilder segment, CapTableBuilder capTable, int pointer) {
            return newClient(
                    WireHelpers.readCapabilityPointer(segment, capTable, pointer, 0));
       }

        @Override
        public T initFromPointerBuilder(SegmentBuilder segment, CapTableBuilder capTable, int pointer, int elementCount) {
            return newClient(
                    WireHelpers.readCapabilityPointer(segment, capTable, pointer, 0));
        }

        @Override
        public void setPointerBuilder(SegmentBuilder segment, CapTableBuilder capTable, int pointer, T value) {
            WireHelpers.setCapabilityPointer(segment, capTable, pointer, value.getHook());
        }
    }

    public static class CapabilityFactory extends Factory<Client> {
        @Override
        public Client newClient(ClientHook hook) {
            return new Client(hook);
        }
    }

    public static final CapabilityFactory factory = new CapabilityFactory();

    public interface ClientBase {

        ClientHook getHook();

        default CompletionStage<java.lang.Void> whenResolved() {
            return this.getHook().whenResolved();
        }

        /**
         * If the capability's server implemented {@link Server.getFd} returning non-null, and all
         * RPC links between the client and server support FD passing, returns a file descriptor pointing
         * to the same underlying file description as the server did. Returns null if the server provided
         * no FD or if FD passing was unavailable at some intervening link.
         * <p>
         * This returns a Promise to handle the case of an unresolved promise capability, e.g. a
         * pipelined capability. The promise resolves no later than when the capability settles, i.e.
         * the same time `whenResolved()` would complete.
         * <p>
         * The file descriptor will remain open at least as long as the {@link Client} remains alive.
         * If you need it to last longer, you will need to `dup()` it.
         */
        default CompletableFuture<Integer> getFd() {
            var fd = this.getHook().getFd();
            if (fd != null) {
                return CompletableFuture.completedFuture(fd);
            }
            var promise = this.getHook().whenMoreResolved();
            if (promise != null) {
                return promise.thenCompose(newHook -> new Client(newHook).getFd());
            }
            return CompletableFuture.completedFuture(null);
        }

        default Request<AnyPointer.Builder> newCall(long interfaceId, short methodId) {
            return this.getHook().newCall(interfaceId, methodId);
        }

        default <T> StreamingRequest<T> newStreamingCall(FromPointerBuilder<T> paramsBuilder,
                                                           long interfaceId, short methodId) {
            var request = getHook().newCall(interfaceId, methodId);
            return new StreamingRequest<> (paramsBuilder, request.getParams(), request.getHook());
        }
    }

    public static class Client implements ClientBase {

        private final ClientHook hook;

        public Client() {
            this(newNullCap());
        }

        public Client(Client other) {
            this(other.hook);
        }

        public Client(Server server) {
            this(makeLocalClient(server));
        }

        public Client(ClientHook hook) {
            this.hook = hook;
        }

        public <T extends Client> Client(CompletionStage<T> promise) {
            this(Capability.newLocalPromiseClient(
                    promise.thenApply(client -> client.getHook())));
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
    }

    public abstract static class Server {

        public static final CompletableFuture<java.lang.Void> READY_NOW = CompletableFuture.completedFuture(null);
        private static final Object BRAND = new Object();
        private ClientHook hook;

        ClientHook makeLocalClient() {
            return new LocalClient();
        }

        ClientHook makeLocalClient(CapabilityServerSetBase capServerSet) {
            return new LocalClient(capServerSet);
        }

        private final class LocalClient implements ClientHook {
            private final CompletableFuture<java.lang.Void> resolveTask;
            private ClientHook resolved;
            private boolean blocked = false;
            private final CapabilityServerSetBase capServerSet;

            LocalClient() {
                this(null);
            }

            LocalClient(CapabilityServerSetBase capServerSet) {
                Server.this.hook = this;
                this.capServerSet = capServerSet;

                var resolver = shortenPath();
                this.resolveTask = resolver != null
                        ? resolver.thenAccept(client -> this.resolved = client.getHook())
                        : null;
            }

            @Override
            public AnyPointer.Request newCall(long interfaceId, short methodId) {
                var hook = new LocalRequest(interfaceId, methodId, this);
                var root = hook.message.getRoot(AnyPointer.factory);
                return new AnyPointer.Request(root, hook);
            }

            @Override
            public VoidPromiseAndPipeline call(long interfaceId, short methodId, CallContextHook ctx) {
                assert !blocked: "Blocked condition not implemented";
                if (blocked) {
                    // TODO implement blocked behaviour
                    return null;
                }

                var promise = this.whenResolved().thenCompose(
                        x -> this.callInternal(interfaceId, methodId, ctx));

                CompletableFuture<PipelineHook> pipelinePromise = promise.thenApply(x -> {
                    ctx.releaseParams();
                    return new LocalPipeline(ctx);
                });

                var tailCall = ctx.onTailCall();
                if (tailCall != null) {
                    pipelinePromise = tailCall.applyToEither(pipelinePromise, pipeline -> pipeline);
                }

                return new VoidPromiseAndPipeline(
                        promise,
                        new QueuedPipeline(pipelinePromise));
            }

            @Override
            public ClientHook getResolved() {
                return this.resolved;
            }

            @Override
            public CompletableFuture<ClientHook> whenMoreResolved() {
                if (this.resolved != null) {
                    return CompletableFuture.completedFuture(this.resolved);
                }
                else if (this.resolveTask != null) {
                    return this.resolveTask.thenApply(x -> this.resolved);
                }
                else {
                    return null;
                }
            }

            @Override
            public Object getBrand() {
                return BRAND;
            }

            CompletableFuture<java.lang.Void> callInternal(long interfaceId, short methodId, CallContextHook ctx) {
                var result = dispatchCall(
                        interfaceId, methodId,
                        new CallContext<>(AnyPointer.factory, AnyPointer.factory, ctx));
                if (result.isStreaming()) {
                    // TODO streaming
                    return null;
                }
                else {
                    return result.getPromise();
                }
            }

            public CompletableFuture<Server> getLocalServer(CapabilityServerSetBase capServerSet) {
                if (this.capServerSet == capServerSet) {
                    if (this.blocked) {
                        assert false: "Blocked local server not implemented";
                    }

                    return CompletableFuture.completedFuture(Server.this);
                }
                return null;
            }
        }

        /**
         * If this returns non-null, then it is a promise which, when resolved, points to a new
         * capability to which future calls can be sent. Use this in cases where an object implementation
         * might discover a more-optimized path some time after it starts.
         *
         * Implementing this (and returning non-null) will cause the capability to be advertised as a
         * promise at the RPC protocol level. Once the promise returned by shortenPath() resolves, the
         * remote client will receive a `Resolve` message updating it to point at the new destination.
         *
         * `shortenPath()` can also be used as a hack to shut up the client. If shortenPath() returns
         * a promise that resolves to an exception, then the client will be notified that the capability
         * is now broken. Assuming the client is using a correct RPC implemnetation, this should cause
         * all further calls initiated by the client to this capability to immediately fail client-side,
         * sparing the server's bandwidth.
         *
         * The default implementation always returns null.
        */
        public CompletableFuture<Client> shortenPath() {
            return null;
        }

        protected Client thisCap() {
            return new Client(this.hook);
        }

        protected static <Params, Results> CallContext<Params, Results> internalGetTypedContext(
                FromPointerReader<Params> paramsFactory,
                FromPointerBuilder<Results> resultsFactory,
                CallContext<AnyPointer.Reader, AnyPointer.Builder> typeless) {
            return new CallContext<>(paramsFactory, resultsFactory, typeless.getHook());
        }

        protected static <Params> StreamingCallContext<Params> internalGetTypedStreamingContext(
                FromPointerReader<Params> paramsFactory,
                CallContext<AnyPointer.Reader, AnyPointer.Builder> typeless) {
            return new StreamingCallContext<>(paramsFactory, typeless.getHook());
        }

        protected abstract DispatchCallResult dispatchCall(
                long interfaceId, short methodId,
                CallContext<AnyPointer.Reader, AnyPointer.Builder> context);

        protected static DispatchCallResult streamResult(CompletableFuture<java.lang.Void> result) {
            // For streaming calls, we need to add an evalNow() here so that exceptions thrown
            // directly from the call can propagate to later calls. If we don't capture the
            // exception properly then the caller will never find out that this is a streaming
            // call (indicated by the boolean in the return value) so won't know to propagate
            // the exception.
            // TODO the above comment...
            return new DispatchCallResult(result, true);
        }

        protected static DispatchCallResult result(CompletableFuture<java.lang.Void> result) {
            return new DispatchCallResult(result, false);
        }

        protected static CompletableFuture<java.lang.Void> internalUnimplemented(String actualInterfaceName, long requestedTypeId) {
            return CompletableFuture.failedFuture(RpcException.unimplemented(
                    "Method not implemented. " + actualInterfaceName + " " + requestedTypeId));
        }

        protected static CompletableFuture<java.lang.Void> internalUnimplemented(String interfaceName, long typeId, short methodId) {
            return CompletableFuture.failedFuture(RpcException.unimplemented(
                    "Method not implemented. " + interfaceName + " " + typeId + " " + methodId));
        }

        protected static CompletableFuture<java.lang.Void> internalUnimplemented(String interfaceName, String methodName, long typeId, short methodId) {
            return CompletableFuture.failedFuture(RpcException.unimplemented(
                    "Method not implemented. " + interfaceName + " " + typeId + " " + methodName + " " + methodId));
        }
    }

    public static ClientHook newLocalPromiseClient(CompletionStage<ClientHook> promise) {
        return new QueuedClient(promise.toCompletableFuture());
    }

    public static PipelineHook newLocalPromisePipeline(CompletionStage<PipelineHook> promise) {
        return new QueuedPipeline(promise.toCompletableFuture());
    }

    private static class LocalRequest implements RequestHook {

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

            assert promiseAndPipeline.pipeline != null;
            return new RemotePromise<>(promise, new AnyPointer.Pipeline(promiseAndPipeline.pipeline));
        }

        @Override
        public CompletableFuture<?> sendStreaming() {
            // We don't do any special handling of streaming in RequestHook for local requests, because
            // there is no latency to compensate for between the client and server in this case.
            return send();
        }

        @Override
        public Object getBrand() {
            return null;
        }
    }

    private static final class LocalPipeline implements PipelineHook {
        private final CallContextHook ctx;
        private final AnyPointer.Reader results;

        LocalPipeline(CallContextHook ctx) {
            this.ctx = ctx;
            this.results = ctx.getResults().asReader();
        }

        @Override
        public final ClientHook getPipelinedCap(PipelineOp[] ops) {
            return this.results.getPipelinedCap(ops);
        }
    }

    private static final class LocalResponse implements ResponseHook {

        final MessageBuilder message;

        LocalResponse(int sizeHint) {
            this.message = new MessageBuilder(sizeHint);
        }
    }

    private static class LocalCallContext implements CallContextHook {

        final CompletableFuture<?> cancelAllowed;
        MessageBuilder request;
        Response<AnyPointer.Reader> response;
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
        public AnyPointer.Builder getResults(int sizeHint) {
            if (this.response == null) {
                var localResponse = new LocalResponse(sizeHint);
                this.responseBuilder = localResponse.message.getRoot(AnyPointer.factory);
                this.response = new Response<>(this.responseBuilder.asReader(), localResponse);
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
            public AnyPointer.Request newCall(long interfaceId, short methodId) {
                var broken = Request.newBrokenRequest(AnyPointer.factory, exc);
                return new AnyPointer.Request(broken.getParams(), broken.getHook());
            }

            @Override
            public VoidPromiseAndPipeline call(long interfaceId, short methodId, CallContextHook context) {
                return new VoidPromiseAndPipeline(CompletableFuture.failedFuture(exc), PipelineHook.newBrokenPipeline(exc));
            }

            @Override
            public CompletableFuture<ClientHook> whenMoreResolved() {
                return resolved ? null : CompletableFuture.failedFuture(exc);
            }

            @Override
            public Object getBrand() {
                return brand;
            }
        };
    }

    // Call queues
    //
    // These classes handle pipelining in the case where calls need to be queued in-memory until some
    // local operation completes.

    // A PipelineHook which simply queues calls while waiting for a PipelineHook to which to forward them.
    private static final class QueuedPipeline implements PipelineHook {

        private final CompletableFuture<PipelineHook> promise;
        private final CompletableFuture<Void> selfResolutionOp;
        PipelineHook redirect;

        QueuedPipeline(CompletableFuture<PipelineHook> promiseParam) {
            this.promise = promiseParam;
            this.selfResolutionOp = promise.handle((pipeline, exc) -> {
                this.redirect = exc == null
                        ? pipeline
                        : PipelineHook.newBrokenPipeline(exc);
                return null;
            });
        }

        @Override
        public final ClientHook getPipelinedCap(PipelineOp[] ops) {
            return redirect != null
                    ? redirect.getPipelinedCap(ops)
                    : new QueuedClient(this.promise.thenApply(
                        pipeline -> pipeline.getPipelinedCap(ops)));
        }
    }

    // A ClientHook which simply queues calls while waiting for a ClientHook to which to forward them.
    private static class QueuedClient implements ClientHook {

        private final CompletableFuture<ClientHook> promise;
        private final CompletableFuture<ClientHook> promiseForCallForwarding;
        private final CompletableFuture<ClientHook> promiseForClientResolution;
        private final CompletableFuture<java.lang.Void> setResolutionOp;
        private ClientHook redirect;

        QueuedClient(CompletableFuture<ClientHook> promise) {
            // TODO revisit futures
            this.promise = promise;
            this.promiseForCallForwarding = promise.toCompletableFuture().copy();
            this.promiseForClientResolution = promise.toCompletableFuture().copy();
            this.setResolutionOp = promise.thenAccept(inner -> {
                this.redirect = inner;
            }).exceptionally(exc -> {
                this.redirect = newBrokenCap(exc);
                return null;
            });
        }

        @Override
        public AnyPointer.Request newCall(long interfaceId, short methodId) {
            var hook = new LocalRequest(interfaceId, methodId, this);
            var root = hook.message.getRoot(AnyPointer.factory);
            return new AnyPointer.Request(root, hook);
        }

        @Override
        public VoidPromiseAndPipeline call(long interfaceId, short methodId, CallContextHook ctx) {
            var callResult = this.promiseForCallForwarding.thenApply(
                    client -> client.call(interfaceId, methodId, ctx));
            var pipeline = new QueuedPipeline(callResult.thenApply(result -> result.pipeline));
            return new VoidPromiseAndPipeline(callResult.thenAccept(x -> {}), pipeline);
        }

        @Override
        public ClientHook getResolved() {
            return redirect;
        }

        @Override
        public CompletableFuture<ClientHook> whenMoreResolved() {
            return this.promiseForClientResolution.copy();
        }
    }

    static class CapabilityServerSetBase {

        ClientHook addInternal(Server server) {
            return server.makeLocalClient(this);
        }

        CompletableFuture<Server> getLocalServerInternal(ClientHook hook) {
            for (;;) {
                var next = hook.getResolved();
                if (next != null) {
                    hook = next;
                }
                else {
                    break;
                }
            }
            if (hook.getBrand() == Server.BRAND) {
                var promise = ((Server.LocalClient)hook).getLocalServer(this);
                if (promise != null) {
                    return promise;
                }
            }

            // The capability isn't part of this set.
            var resolver = hook.whenMoreResolved();
            if (resolver != null) {
                // This hook is an unresolved promise. It might resolve eventually to a local server, so wait
                // for it.
                return resolver.thenCompose(this::getLocalServerInternal);
            }
            else {
                // Cap is settled, so it definitely will never resolve to a member of this set.
                 return CompletableFuture.completedFuture(null);
            }
        }
    }

    public static final class CapabilityServerSet<T extends Capability.Server> extends CapabilityServerSetBase {

        /**
         *  Create a new capability Client for the given Server and also add this server to the set.
         */
        <U extends Capability.Client> U add(Capability.Factory<U> factory, T server) {
            var hook = this.addInternal(server);
            return factory.newClient(hook);
        }

        CompletableFuture<T> getLocalServer(Client client) {
            return this.getLocalServerInternal(client.getHook())
                    .thenApply(server -> (T)server);
        }
    }
}
