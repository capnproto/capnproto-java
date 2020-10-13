package org.capnproto;

import java.util.concurrent.CompletionStage;

class QueuedClient implements ClientHook {

    final CompletionStage<ClientHook> promise;
    final CompletionStage<ClientHook> promiseForCallForwarding;
    final CompletionStage<ClientHook> promiseForClientResolution;
    final CompletionStage<java.lang.Void> setResolutionOp;
    ClientHook redirect;

    QueuedClient(CompletionStage<ClientHook> promise) {
        // TODO revisit futures
        this.promise = promise.toCompletableFuture().copy();
        this.promiseForCallForwarding = promise.toCompletableFuture().copy();
        this.promiseForClientResolution = promise.toCompletableFuture().copy();
        this.setResolutionOp = promise.thenAccept(inner -> {
            this.redirect = inner;
        }).exceptionally(exc -> {
            this.redirect = Capability.newBrokenCap(exc);
            return null;
        });
    }

    @Override
    public Request<AnyPointer.Builder, AnyPointer.Pipeline> newCall(long interfaceId, short methodId) {
        var hook = new Capability.LocalRequest(interfaceId, methodId, this);
        var root = hook.message.getRoot(AnyPointer.factory);
        return new Request<>(root, AnyPointer.factory, hook);
    }

    @Override
    public VoidPromiseAndPipeline call(long interfaceId, short methodId, CallContextHook ctx) {
        var callResultPromise = this.promiseForCallForwarding.thenApply(client -> client.call(interfaceId, methodId, ctx));
        var pipelinePromise = callResultPromise.thenApply(callResult -> callResult.pipeline);
        var pipeline = new QueuedPipeline(pipelinePromise);
        return new VoidPromiseAndPipeline(callResultPromise, pipeline);
    }

    @Override
    public ClientHook getResolved() {
        return redirect;
    }

    @Override
    public CompletionStage<ClientHook> whenMoreResolved() {
        return promiseForClientResolution;
    }
}
