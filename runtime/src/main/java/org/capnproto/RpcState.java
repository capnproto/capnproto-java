package org.capnproto;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
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
        CompletionStage<?> resolveOp;
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

    private final ExportTable<Export> exports = new ExportTable<Export>();
    private final ExportTable<Question> questions = new ExportTable<Question>();

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

    private final ExportTable<Embargo> embargos = new ExportTable<Embargo>();

    private final HashMap<ClientHook, Integer> exportsByCap = new HashMap<>();
    private final VatNetwork.Connection connection;
    private final Capability.Client bootstrapInterface;
    private Throwable disconnected = null;

    RpcState(VatNetwork.Connection connection, Capability.Client bootstrapInterface) {
        this.connection = connection;
        this.bootstrapInterface = bootstrapInterface;
    }

    boolean isDisconnected() {
        return this.disconnected != null;
    }

    void handleMessage(IncomingRpcMessage message) {
        var reader = message.getBody().getAs(RpcProtocol.Message.factory);
        
        switch (reader.which()) {
            case UNIMPLEMENTED:
                handleUnimplemented(reader.getUnimplemented());
                break;
            case ABORT:
                handleAbort(reader.getAbort());
                break;
            case BOOTSTRAP:
                handleBootstrap(message, reader.getBootstrap());
                break;
            case CALL:
                handleCall(message, reader.getCall());
                return;
            case RETURN:
                handleReturn(message, reader.getReturn());
                break;
            case FINISH:
                handleFinish(reader.getFinish());
                break;
            case RESOLVE:
                handleResolve(message, reader.getResolve());
                break;
            case DISEMBARGO:
                handleDisembargo(reader.getDisembargo());
                break;
            default:
                // TODO send unimplemented response
                break;
        }
    }

    void handleUnimplemented(RpcProtocol.Message.Reader message) {
    }

    void handleAbort(RpcProtocol.Exception.Reader abort) {
    }

    void handleBootstrap(IncomingRpcMessage message, RpcProtocol.Bootstrap.Reader bootstrap) {
        if (isDisconnected()) {
            return;
        }

        var answerId = bootstrap.getQuestionId();
        var answer = answers.put(answerId);
        if (answer.active) {
            // questionId already in use
            return;
        }
        answer.active = true;

        var capTable = new BuilderCapabilityTable();
        var response = connection.newOutgoingMessage(1024);

        var ret = response.getBody().getAs(RpcProtocol.Message.factory).initReturn();
        ret.setAnswerId(answerId);

        var payload = ret.initResults();
        var content = payload.getContent().imbue(capTable);
        content.setAsCapability(bootstrapInterface);

        var capTableArray = capTable.getTable();
        assert capTableArray.length != 0;

        var capHook = capTableArray[0];
        assert capHook != null;

        var fds = List.<Integer>of();
        response.setFds(List.of());

        answer.resultExports = writeDescriptors(capTableArray, payload, fds);
        answer.pipeline = ops -> ops.length == 0
                ? capHook
                : ClientHook.newBrokenCap("Invalid pipeline transform.");

        response.send();

        assert answer.active;
        assert answer.resultExports != null;
        assert answer.pipeline != null;
    }

    void handleCall(IncomingRpcMessage message, RpcProtocol.Call.Reader call) {
    }

    void handleReturn(IncomingRpcMessage message, RpcProtocol.Return.Reader callReturn) {
    }

    void handleFinish(RpcProtocol.Finish.Reader finish) {
    }

    void handleResolve(IncomingRpcMessage message, RpcProtocol.Resolve.Reader resolve) {
    }

    void handleDisembargo(RpcProtocol.Disembargo.Reader disembargo) {
    }


    private List<Integer> writeDescriptors(ClientHook[] capTable, RpcProtocol.Payload.Builder payload, List<Integer> fds) {
        if (capTable.length == 0) {
            return List.of();
        }

        var capTableBuilder = payload.initCapTable(capTable.length);
        var exports = new ArrayList<Integer>();
        for (int ii = 0; ii < capTable.length; ++ii) {
            var cap = capTable[ii];
            if (cap == null) {
                capTableBuilder.get(ii).setNone(null);
                continue;
            }

            var exportId = writeDescriptor(cap, capTableBuilder.get(ii), fds);
            if (exportId != null) {
                exports.add(exportId);
            }
        }
        return exports;
    }

    private Integer writeDescriptor(ClientHook cap, RpcProtocol.CapDescriptor.Builder descriptor, List<Integer> fds) {
        ClientHook inner = cap;
        for (;;) {
            var resolved = inner.getResolved();
            if (resolved != null) {
                inner = resolved;
            } else {
                break;
            }
        }

        var fd = inner.getFd();
        if (fd != null) {
            fds.add(fd);
        }

        if (inner.getBrand() == this) {
            return ((RpcClient) inner).writeDescriptor(descriptor, fds);
        }

        var exportId = exportsByCap.get(inner);
        if (exportId != null) {
            // We've already seen and exported this capability before.
            var export = exports.find(exportId);
            export.refcount++;
            descriptor.setSenderHosted(exportId);
            return exportId;
        }

        // This is the first time we've seen this capability.
        var export = new Export();
        export.refcount = 1;
        export.clientHook = inner;
        exportId = exports.next(export);

        var wrapped = inner.whenMoreResolved();
        if (wrapped != null) {
            // This is a promise.  Arrange for the `Resolve` message to be sent later.
            export.resolveOp = resolveExportedPromise(exportId, wrapped);
            descriptor.setSenderPromise(exportId);
        }
        else {
            descriptor.setSenderHosted(exportId);
        }
        return exportId;
    }

    CompletionStage<?> resolveExportedPromise(int exportId, CompletionStage<ClientHook> promise) {

        return promise.thenCompose(resolution -> {
            if (isDisconnected()) {
                return CompletableFuture.completedFuture(null);
            }

            resolution = getInnermostClient(resolution);

            var exp = exports.find(exportId);
            exportsByCap.remove(exp.clientHook);
            exp.clientHook = resolution;

            if (exp.clientHook.getBrand() != this) {
                // We're resolving to a local capability.  If we're resolving to a promise, we might be
                // able to reuse our export table entry and avoid sending a message.
                var more = exp.clientHook.whenMoreResolved();
                if (more != null) {
                    // We're replacing a promise with another local promise.  In this case, we might actually
                    // be able to just reuse the existing export table entry to represent the new promise --
                    // unless it already has an entry.  Let's check.

                    var insertResult = exportsByCap.put(exp.clientHook, exportId);
                    // TODO check this behaviour
                    if (insertResult == null) {
                        // The new promise was not already in the table, therefore the existing export table
                        // entry has now been repurposed to represent it.  There is no need to send a resolve
                        // message at all.  We do, however, have to start resolving the next promise.
                        return resolveExportedPromise(exportId, more);
                    }
                }
            }

            // send a Resolve message
            var message = connection.newOutgoingMessage(1024);
            var resolve = message.getBody().initAs(RpcProtocol.Message.factory).initResolve();
            resolve.setPromiseId(exportId);
            var fds = List.<Integer>of();
            writeDescriptor(exp.clientHook, resolve.initCap(), fds);
            message.setFds(fds);
            message.send();
            return CompletableFuture.completedFuture(null);
        }).whenComplete((value, exc) -> {
            if (exc == null) {
                return;
            }
            var message = connection.newOutgoingMessage(1024);
            var resolve = message.getBody().initAs(RpcProtocol.Message.factory).initResolve();
            resolve.setPromiseId(exportId);
            RpcException.fromException(exc, resolve.initException());
            message.send();

            // TODO disconnect?
        });
    }

    ClientHook getInnermostClient(ClientHook client) {
        for (;;) {
            var inner = client.getResolved();
            if (inner != null) {
                client = inner;
            }
            else {
                break;
            }
        }

        if (client.getBrand() == this) {
            return ((RpcClient)client).getInnermostClient();
        }

        return client;
    }

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
