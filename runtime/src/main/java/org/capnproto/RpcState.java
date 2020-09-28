package org.capnproto;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

final class RpcState {

    static final class Question {
        List<Integer> paramExports;
        boolean isAwaitingReturn = false;
        boolean isTailCall = false;
        boolean skipFinish = false;
        CompletableFuture<RpcResponse> response = new CompletableFuture<>();

        void reject(Throwable exc) {
            response.completeExceptionally(exc);
        }

        void answer(RpcResponse response) {
            this.response.complete(response);
        }
    }

    static final class Answer {
        boolean active = false;
        PipelineHook pipeline;
        CompletableFuture<RpcResponse> redirectedResults;
        RpcCallContext callContext;
        List<Integer> resultExports;
    }

    static final class Export {
        int refcount;
        ClientHook clientHook;
        CompletableFuture<java.lang.Void> resolveOp;
    }

    static final class Import {
        ImportClient importClient;
        RpcClient appClient;
        CompletableFuture<ClientHook> promise;
        // If non-null, the import is a promise.
    }

    final static class Embargo {
        CompletableFuture<java.lang.Void> fulfiller;
    }

    private final ExportTable<Export> exports = new ExportTable<Export>() {
        @Override
        protected Export newExportable() {
            return new Export();
        }
    };

    private final ExportTable<Question> questions = new ExportTable<Question>() {
        @Override
        protected Question newExportable() {
            return new Question();
        }
    };

    private final ImportTable<Answer> answers = new ImportTable<Answer>() {
        @Override
        protected Answer newImportable() {
            return new Answer();
        }
    };

    private final ImportTable<Import> imports = new ImportTable<Import>() {
        @Override
        protected Import newImportable() {
            return new Import();
        }
    };

    private final ExportTable<Embargo> embargos = new ExportTable<Embargo>() {
        @Override
        protected Embargo newExportable() {
            return new Embargo();
        }
    };

    private final HashMap<ClientHook, Integer> exportsByCap = new HashMap<>();

    interface RpcResponse extends ResponseHook {
        AnyPointer.Reader getResults();
    }

    class RpcCallContext implements CallContextHook {

        final int answerId;
        final long interfaceId;
        final short methodId;

        // request
        IncomingRpcMessage request;
        final AnyPointer.Reader params;

        // response
        RpcResponse response;
        RpcProtocol.Return.Builder returnMessage;
        boolean redirectResults = false;

        final CompletableFuture<java.lang.Void> cancelled;

        RpcCallContext(int answerId, IncomingRpcMessage request, List<ClientHook> capTable,
                       AnyPointer.Reader params, boolean redirectResults,
                       CompletableFuture<java.lang.Void> cancelled,
                       long interfaceId, short methodId) {
            this.answerId = answerId;
            this.interfaceId = interfaceId;
            this.methodId = methodId;
            this.request = request;
            this.params = params.imbue(new ReaderCapabilityTable(capTable));
            this.redirectResults = redirectResults;
            this.cancelled = cancelled;
        }

        @Override
        public AnyPointer.Reader getParams() {
            return this.params;
        }

        @Override
        public void releaseParams() {
            this.request = null;
        }

        @Override
        public AnyPointer.Builder getResults() {
            return null;
        }

        @Override
        public CompletableFuture<java.lang.Void> tailCall(RequestHook request) {
            return null;
        }

        @Override
        public void allowCancellation() {
        }

        @Override
        public CompletableFuture<PipelineHook> onTailCall() {
            return null;
        }

        @Override
        public ClientHook.VoidPromiseAndPipeline directTailCall(RequestHook request) {
            return null;
        }
    }

    abstract class RpcClient implements ClientHook {

        public abstract Integer writeDescriptor(RpcProtocol.CapDescriptor.Builder descriptor, List<Integer> fds);

        public abstract ClientHook writeTarget(RpcProtocol.MessageTarget.Builder target);

        public ClientHook getInnermostClient() {
            return this;
        }

        @Override
        public Request<AnyPointer.Builder, AnyPointer.Reader> newCall(long interfaceId, short methodId) {
            return null;
        }

        @Override
        public VoidPromiseAndPipeline call(long interfaceId, short methodId, CallContextHook context) {
            return null;
        }
    }

    class ImportClient extends RpcClient {
        @Override
        public Integer writeDescriptor(RpcProtocol.CapDescriptor.Builder descriptor, List<Integer> fds) {
            return null;
        }

        @Override
        public ClientHook writeTarget(RpcProtocol.MessageTarget.Builder target) {
            return null;
        }
    }
}
