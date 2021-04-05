package org.capnproto;

import java.io.FileDescriptor;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public final class Capability {

    static final Object NULL_CAPABILITY_BRAND = new Object();
    static final Object BROKEN_CAPABILITY_BRAND = new Object();

    public static class BuilderContext {
        public CapTableBuilder capTable;
    }

    public static class ReaderContext {
        public CapTableReader capTable;
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

        default CompletableFuture<java.lang.Void> whenResolved() {
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
        default CompletableFuture<FileDescriptor> getFd() {
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

        default <T> Request<T> newCall(FromPointerBuilder<T> paramsFactory, long interfaceId, short methodId) {
            var request = this.getHook().newCall(interfaceId, methodId);
            return new Request<>() {
                @Override
                public FromPointerBuilder<T> getParamsFactory() {
                    return paramsFactory;
                }

                @Override
                public Request<AnyPointer.Builder> getTypelessRequest() {
                    return request;
                }

                @Override
                public Request<T> getBaseRequest() {
                    return this;
                }
            };
        }

        default <T> StreamingRequest<T> newStreamingCall(FromPointerBuilder<T> paramsFactory, long interfaceId, short methodId) {
            var request =  this.getHook().newCall(interfaceId, methodId);
            var streamingRequest = newTypelessStreamingRequest(request.getParams(), request.getHook());
            return new StreamingRequest<>() {
                @Override
                public FromPointerBuilder<T> getParamsFactory() {
                    return paramsFactory;
                }

                @Override
                public StreamingRequest<AnyPointer.Builder> getTypelessRequest() {
                    return streamingRequest;
                }
            };
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

        public <T extends Server> Client(T server) {
            this(makeLocalClient(server));
        }

        public Client(ClientHook hook) {
            this.hook = hook;
        }

        public Client(CompletionStage<? extends Client> promise) {
            this(Capability.newLocalPromiseClient(
                    promise.thenApply(Client::getHook)));
        }

        public Client(Throwable exc) {
            this(newBrokenCap(exc));
        }

        public ClientHook getHook() {
            return this.hook;
        }

        private static <T extends Server> ClientHook makeLocalClient(T server) {
            return server.makeLocalClient();
        }
    }

    public abstract static class Server {

        public static final CompletableFuture<java.lang.Void> READY_NOW = CompletableFuture.completedFuture(null);
        private static final Object BRAND = new Object();
        private ClientHook hook;

        ClientHook makeLocalClient() {
            return new LocalClient<>();
        }

        <T extends Server> ClientHook makeLocalClient(CapabilityServerSet<T> capServerSet) {
            return new LocalClient<>(capServerSet);
        }

        private final class LocalClient<T extends Server> implements ClientHook {

            private CompletableFuture<java.lang.Void> resolveTask;
            private ClientHook resolved;
            private boolean blocked = false;
            private Throwable brokenException;
            private final Queue<Runnable> blockedCalls = new ArrayDeque<>();
            private final CapabilityServerSet<T> capServerSet;

            LocalClient() {
                this(null);
            }

            LocalClient(CapabilityServerSet<T> capServerSet) {
                Server.this.hook = this;
                this.capServerSet = capServerSet;
                var resolveTask = shortenPath();
                if (resolveTask != null) {
                    this.resolveTask = resolveTask.thenAccept(cap -> {
                        this.resolved = cap.getHook();
                    });
                }
            }

            @Override
            public Request<AnyPointer.Builder> newCall(long interfaceId, short methodId) {
                var hook = new LocalRequest(interfaceId, methodId, this);
                var root = hook.message.getRoot(AnyPointer.factory);
                return newTypelessRequest(root, hook);
            }

            @Override
            public VoidPromiseAndPipeline call(long interfaceId, short methodId, CallContextHook ctx) {
                // Note this comment from the C++ source:
                //
                // "We don't want to actually dispatch the call synchronously, because we don't want the callee
                //  to have any side effects before the promise is returned to the caller.  This helps avoid
                //  race conditions.
                //
                //  So, we do an evalLater() here.
                //
                //  Note also that QueuedClient depends on this evalLater() to ensure that pipelined calls don't
                //  complete before 'whenMoreResolved()' promises resolve."
                //
                // As the Java implementation doesn't (currently) have an evalLater() call, we obtain a promise
                // from the CallContextHook that will be completed by QueuedClient when appropriate.
                var promise = ctx.releaseCall().thenCompose(void_ -> {
                    if (blocked) {
                        var blockedCall = new CompletableFuture<java.lang.Void>();
                        this.blockedCalls.add(() -> callInternal(interfaceId, methodId, ctx).whenComplete((result, exc) -> {
                            if (exc == null) {
                                blockedCall.complete(result);
                            }
                            else {
                                blockedCall.completeExceptionally(exc);
                            }
                        }));
                        return blockedCall;
                    }
                    else {
                        return this.callInternal(interfaceId, methodId, ctx);
                    }
                });

                var pipelinePromise = promise.thenApply(x -> {
                    ctx.releaseParams();
                    return (PipelineHook) new LocalPipeline(ctx);
                });

                var tailCall = ctx.onTailCall().thenApply(pipeline -> pipeline.hook);
                pipelinePromise = tailCall.applyToEither(pipelinePromise, pipeline -> pipeline);

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
                    return null;
                }
                else if (this.resolveTask != null) {
                    return this.resolveTask.thenApply(void_ -> this.resolved);
                }
                else {
                    return null;
                }
            }

            @Override
            public Object getBrand() {
                return BRAND;
            }

            void unblock() {
                this.blocked = false;
                while (!this.blocked) {
                    if (this.blockedCalls.isEmpty()) {
                        break;
                    }
                    var call = this.blockedCalls.remove();
                    call.run();
                }
            }

            CompletableFuture<java.lang.Void> callInternal(long interfaceId, short methodId, CallContextHook ctx) {
                assert !this.blocked;

                if (this.brokenException != null) {
                    return CompletableFuture.failedFuture(this.brokenException);
                }

                var result = dispatchCall(
                        interfaceId, methodId,
                        new CallContext<>(AnyPointer.factory, AnyPointer.factory, ctx));
                if (!result.isStreaming()) {
                    return result.promise;
                }
                else {
                    this.blocked = true;
                    return result.promise.whenComplete((void_, exc) -> {
                        if (exc != null) {
                            this.brokenException = exc;
                        }
                        this.unblock();
                    });
                }
            }

            public CompletableFuture<T> getLocalServer(CapabilityServerSet<T> capServerSet) {
                if (this.capServerSet == capServerSet) {
                    if (this.blocked) {
                        var promise = new CompletableFuture<T>();
                        var server = (T)Server.this;
                        this.blockedCalls.add(() -> promise.complete(server));
                        return promise;
                    }

                    var server = (T)Server.this;
                    return CompletableFuture.completedFuture(server);
                }
                return null;
            }
        }

        public Integer getFd() {
            return null;
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

        private final MessageBuilder message = new MessageBuilder();
        private final long interfaceId;
        private final short methodId;
        private final ClientHook client;
        private final CompletableFuture<java.lang.Void> callRelease = new CompletableFuture<>();

        LocalRequest(long interfaceId, short methodId, ClientHook client) {
            this.interfaceId = interfaceId;
            this.methodId = methodId;
            this.client = client;
        }

        @Override
        public RemotePromise<AnyPointer.Reader> send() {
            var cancel = new CompletableFuture<java.lang.Void>();
            var context = new LocalCallContext(message, client, cancel, this.callRelease);
            var promiseAndPipeline = client.call(interfaceId, methodId, context);
            var promise = promiseAndPipeline.promise.thenApply(x -> {
                context.getResults(); // force allocation
                return context.response;
            });

            this.callRelease.complete(null);
            assert promiseAndPipeline.pipeline != null;
            return new RemotePromise<>(promise, promiseAndPipeline.pipeline);
        }

        @Override
        public Object getBrand() {
            return null;
        }

        void releaseCall() {
            this.callRelease.complete(null);
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
        public final ClientHook getPipelinedCap(short[] ops) {
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

        final CompletableFuture<java.lang.Void> cancelAllowed;
        private final CompletableFuture<java.lang.Void> callRelease;
        CompletableFuture<AnyPointer.Pipeline> tailCallPipeline;
        MessageBuilder request;
        Response<AnyPointer.Reader> response;
        AnyPointer.Builder responseBuilder;
        ClientHook clientRef;

        LocalCallContext(MessageBuilder request,
                         ClientHook clientRef,
                         CompletableFuture<java.lang.Void> cancelAllowed,
                         CompletableFuture<java.lang.Void> callRelease) {
            this.request = request;
            this.clientRef = clientRef;
            this.cancelAllowed = cancelAllowed;
            this.callRelease = callRelease;
        }

        @Override
        public AnyPointer.Reader getParams() {
            return this.request.getRoot(AnyPointer.factory).asReader();
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
            assert this.response != null;
            return this.responseBuilder;
        }

        @Override
        public void allowCancellation() {
            this.cancelAllowed.complete(null);
        }

        @Override
        public CompletableFuture<java.lang.Void> tailCall(RequestHook request) {
            var result = this.directTailCall(request);
            if (this.tailCallPipeline != null) {
                this.tailCallPipeline.complete(new AnyPointer.Pipeline(result.pipeline));
            }
            return result.promise;
        }

        @Override
        public CompletableFuture<AnyPointer.Pipeline> onTailCall() {
            this.tailCallPipeline = new CompletableFuture<>();
            return this.tailCallPipeline.copy();
        }

        @Override
        public ClientHook.VoidPromiseAndPipeline directTailCall(RequestHook request) {
            assert this.response == null : "Can't call tailCall() after initializing the results struct.";
            var promise = request.send();
            var voidPromise = promise.response.thenAccept(tailResponse -> {
                this.response = tailResponse;
            });
            return new ClientHook.VoidPromiseAndPipeline(voidPromise, promise.pipeline().hook);
        }

        @Override
        public CompletableFuture<java.lang.Void> releaseCall() {
            return this.callRelease;
        }
    }

    static Request<AnyPointer.Builder> newTypelessRequest(AnyPointer.Builder params, RequestHook requestHook) {
        return new Request<>() {
            @Override
            public AnyPointer.Builder getParams() {
                return params;
            }

            @Override
            public org.capnproto.Request<AnyPointer.Builder> getTypelessRequest() {
                return this;
            }

            @Override
            public org.capnproto.Request<AnyPointer.Builder> getBaseRequest() {
                return this;
            }

            @Override
            public RequestHook getHook() {
                return requestHook;
            }

            @Override
            public FromPointerBuilder<AnyPointer.Builder> getParamsFactory() {
                return AnyPointer.factory;
            }

            @Override
            public RemotePromise<AnyPointer.Reader> sendInternal() {
                return requestHook.send();
            }
        };
    }

    static StreamingRequest<AnyPointer.Builder> newTypelessStreamingRequest(AnyPointer.Builder params, RequestHook requestHook) {
        return new StreamingRequest<>() {
            @Override
            public AnyPointer.Builder getParams() {
                return params;
            }

            @Override
            public org.capnproto.StreamingRequest<AnyPointer.Builder> getTypelessRequest() {
                return this;
            }

            @Override
            public RequestHook getHook() {
                return requestHook;
            }

            @Override
            public FromPointerBuilder<AnyPointer.Builder> getParamsFactory() {
                return AnyPointer.factory;
            }

            public CompletableFuture<java.lang.Void> send() {
                return requestHook.sendStreaming();
            }
        };
    }

    static PipelineHook newBrokenPipeline(Throwable exc) {
        return ops -> newBrokenCap(exc);
    }

    static Request<AnyPointer.Builder> newBrokenRequest(Throwable exc) {

        var message = new MessageBuilder();
        var params = message.getRoot(AnyPointer.factory);

        var hook = new RequestHook() {
            @Override
            public RemotePromise<AnyPointer.Reader> send() {
                return new RemotePromise<>(CompletableFuture.failedFuture(exc),
                        newBrokenPipeline(exc));
            }

            @Override
            public CompletableFuture<java.lang.Void> sendStreaming() {
                return CompletableFuture.failedFuture(exc);
            }
        };

        return Capability.newTypelessRequest(params, hook);
    }

    public static ClientHook newBrokenCap(String reason) {
        return newBrokenClient(reason, false, BROKEN_CAPABILITY_BRAND);
    }

    public static ClientHook newBrokenCap(Throwable exc) {
        return newBrokenClient(exc, false, BROKEN_CAPABILITY_BRAND);
    }

    public static ClientHook newNullCap() {
        return newBrokenClient(RpcException.failed("Called null capability"), true, NULL_CAPABILITY_BRAND);
    }

    private static ClientHook newBrokenClient(String reason, boolean resolved, Object brand) {
        return newBrokenClient(RpcException.failed(reason), resolved, brand);
    }

    private static ClientHook newBrokenClient(Throwable exc, boolean resolved, Object brand) {
        return new ClientHook() {
            @Override
            public Request<AnyPointer.Builder> newCall(long interfaceId, short methodId) {
                return newBrokenRequest(exc);
            }

            @Override
            public VoidPromiseAndPipeline call(long interfaceId, short methodId, CallContextHook context) {
                return new VoidPromiseAndPipeline(CompletableFuture.failedFuture(exc), newBrokenPipeline(exc));
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
        PipelineHook redirect;
        private final Map<List<Short>, ClientHook> clientMap = new HashMap<>();

        QueuedPipeline(CompletableFuture<PipelineHook> promise) {
            this.promise = promise.whenComplete((pipeline, exc) -> {
                this.redirect = exc == null
                        ? pipeline
                        : newBrokenPipeline(exc);
            });
        }

        @Override
        public final ClientHook getPipelinedCap(short[] ops) {
            if (redirect != null) {
                return redirect.getPipelinedCap(ops);
            }

            var key = new ArrayList<Short>(ops.length);
            for (short op: ops) {
                key.add(op);
            }

            return this.clientMap.computeIfAbsent(key,
                    k -> new QueuedClient(this.promise.thenApply(
                            pipeline -> pipeline.getPipelinedCap(ops))));
        }
    }

    // A ClientHook which simply queues calls while waiting for a ClientHook to which to forward them.
    private static class QueuedClient implements ClientHook {

        private final CompletableFuture<java.lang.Void> selfResolutionOp;
        // Represents the operation which will set `redirect` when possible.

        private final CompletableFuture<ClientHook> promiseForClientResolution = new CompletableFuture<>();
        // whenMoreResolved() returns forks of this promise.  These must resolve *after* queued calls
        // have been initiated (so that any calls made in the whenMoreResolved() handler are correctly
        // delivered after calls made earlier), but *before* any queued calls return (because it might
        // confuse the application if a queued call returns before the capability on which it was made
        // resolves).

        private ClientHook redirect;
        private final List<CompletableFuture<ClientHook>> queuedCalls = new ArrayList<>();
        private final List<LocalRequest> pendingCalls = new ArrayList<>();

        QueuedClient(CompletableFuture<ClientHook> promise) {
            this.selfResolutionOp = promise.handle((inner, exc) -> {
                this.redirect = exc == null
                        ? inner
                        : newBrokenCap(exc);

                // Resolve promises for call forwarding.
                for (var call: this.queuedCalls) {
                    call.complete(this.redirect);
                }

                // Now resolve the promise for client resolution
                this.promiseForClientResolution.complete(this.redirect);

                // Finally, execute any pending calls.
                for (var hook: this.pendingCalls) {
                    hook.releaseCall();
                }

                this.queuedCalls.clear();
                this.pendingCalls.clear();

                return null;
            });
        }

        @Override
        public Request<AnyPointer.Builder> newCall(long interfaceId, short methodId) {
            var hook = new LocalRequest(interfaceId, methodId, this);
            if (this.redirect == null) {
                this.pendingCalls.add(hook);
            }
            else {
                hook.releaseCall();
            }
            var root = hook.message.getRoot(AnyPointer.factory);
            return newTypelessRequest(root, hook);
        }

        @Override
        public VoidPromiseAndPipeline call(long interfaceId, short methodId, CallContextHook ctx) {
            if (this.redirect != null) {
                return this.redirect.call(interfaceId, methodId, ctx);
            }

            var promise = new CompletableFuture<ClientHook>();
            var callResult = promise.thenApply(
                    client -> client.call(interfaceId, methodId, ctx));
            var pipelineResult = callResult.thenApply(result -> result.pipeline);
            var pipeline = new QueuedPipeline(pipelineResult);
            this.queuedCalls.add(promise);
            return new VoidPromiseAndPipeline(pipelineResult.thenRun(() -> {}), pipeline);
        }

        @Override
        public ClientHook getResolved() {
            return redirect;
        }

        @Override
        public CompletableFuture<ClientHook> whenMoreResolved() {
            return this.promiseForClientResolution.copy();
        }

        @Override
        public Object getBrand() {
            return null;
        }
    }

    public static final class CapabilityServerSet<T extends Capability.Server> {

        ClientHook addInternal(T server) {
            return server.makeLocalClient(this);
        }

        CompletableFuture<T> getLocalServerInternal(ClientHook hook) {
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
                var promise = ((Server.LocalClient<T>)hook).getLocalServer(this);
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
