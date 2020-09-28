package org.capnproto;

import java.util.concurrent.CompletableFuture;

public final class Capability {

    public static class Client {

        final ClientHook hook;

        public Client(ClientHook hook) {
            this.hook = hook;
        }
    }

    static ClientHook newLocalPromiseClient(CompletableFuture<ClientHook> promise) {
        return new QueuedClient(promise);
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
}
