package org.capnproto;

import java.util.concurrent.CompletableFuture;

class QueuedClient implements ClientHook {

    final CompletableFuture<ClientHook> promise;
    final CompletableFuture<ClientHook> promiseForCallForwarding;
    final CompletableFuture<ClientHook> promiseForClientResolution;
    final CompletableFuture<java.lang.Void> setResolutionOp;
    ClientHook redirect;

    QueuedClient(CompletableFuture<ClientHook> promise) {
        // TODO revisit futures
        this.promise = promise.copy();
        this.promiseForCallForwarding = promise.copy();
        this.promiseForClientResolution = promise.copy();
        this.setResolutionOp = promise.thenAccept(inner -> {
            this.redirect = inner;
        }).exceptionally(exc -> {
            this.redirect = ClientHook.newBrokenCap(exc);
            return null;
        });
    }

    @Override
    public Request<AnyPointer.Builder, AnyPointer.Reader> newCall(long interfaceId, short methodId) {
        var hook = new Capability.LocalRequest(interfaceId, methodId, this);
        var root = hook.message.getRoot(AnyPointer.factory);
        return new Request<>(root, AnyPointer.factory, hook);
    }

    @Override
    public VoidPromiseAndPipeline call(long interfaceId, short methodId, CallContextHook ctx) {
        return null;
    }

    @Override
    public ClientHook getResolved() {
        return redirect;
    }

    @Override
    public CompletableFuture<ClientHook> whenMoreResolved() {
        return promiseForClientResolution;
    }
}
