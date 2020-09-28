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

    void handleMessage(IncomingRpcMessage message) throws RpcException {
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
                if (!isDisconnected()) {
                    // boomin' back atcha
                    var msg = connection.newOutgoingMessage(1024);
                    msg.getBody().initAs(RpcProtocol.Message.factory).setUnimplemented(reader);
                    msg.send();
                }
                break;
        }
    }

    void handleUnimplemented(RpcProtocol.Message.Reader message) {
        switch (message.which()) {
            case RESOLVE:
                var resolve = message.getResolve();
                switch (resolve.which()) {
                    case CAP:
                        var cap = resolve.getCap();
                        switch (cap.which()) {
                            case NONE:
                                break;
                            case SENDER_HOSTED:
                                releaseExport(cap.getSenderHosted(), 1);
                                break;
                            case SENDER_PROMISE:
                                releaseExport(cap.getSenderPromise(), 1);
                                break;
                            case RECEIVER_ANSWER:
                                break;
                            case RECEIVER_HOSTED:
                                break;
                            case THIRD_PARTY_HOSTED:
                                releaseExport(cap.getThirdPartyHosted().getVineId(), 1);
                                break;
                        }
                        break;
                    case EXCEPTION:
                        break;
                }
                break;
            default:
                // Peer unimplemented
                break;
        }
    }

    void handleAbort(RpcProtocol.Exception.Reader abort) throws RpcException {
        throw RpcException.toException(abort);
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

    void releaseExport(int exportId, int refcount) {
        var export = exports.find(exportId);
        assert export != null;
        if (export == null) {
            return;
        }
        assert export.refcount <= refcount;
        if (export.refcount <= refcount) {
            return;
        }
        export.refcount -= refcount;
        if (export.refcount == 0) {
            exportsByCap.remove(exportId, export.clientHook);
            exports.erase(exportId, export);
        }
    }

    private List<ClientHook> receiveCaps(StructList.Reader<RpcProtocol.CapDescriptor.Reader> capTable, List<Integer> fds) {
        var result = new ArrayList<ClientHook>();
        for (var cap: capTable) {
            result.add(receiveCap(cap, fds));
        }
        return result;
    }

    private ClientHook receiveCap(RpcProtocol.CapDescriptor.Reader descriptor, List<Integer> fds) {
        // TODO AutoCloseFd
        Integer fd = null;

        int fdIndex = descriptor.getAttachedFd();
        if (fdIndex >= 0 && fdIndex < fds.size()) {
            fd = fds.get(fdIndex);
            if (fd != null) {
                fds.set(fdIndex, null);
            }
        }

        switch (descriptor.which()) {
            case NONE:
                return null;

            case SENDER_HOSTED:
                return importCap(descriptor.getSenderHosted(), false, fd);

            case SENDER_PROMISE:
                return importCap(descriptor.getSenderPromise(), true, fd);

            case RECEIVER_HOSTED:
                var exp = exports.find(descriptor.getReceiverHosted());
                if (exp == null) {
                    return ClientHook.newBrokenCap("invalid 'receiverHosted' export ID");
                }
                if (exp.clientHook.getBrand() == this) {
                    // TODO Tribble 4-way race!
                    return exp.clientHook;
                }

                return exp.clientHook;

            case RECEIVER_ANSWER:
                var promisedAnswer = descriptor.getReceiverAnswer();
                var answer = answers.find(promisedAnswer.getQuestionId());
                var ops = PipelineOp.ToPipelineOps(promisedAnswer);

                if (answer == null || !answer.active || answer.pipeline == null || ops == null) {
                    return ClientHook.newBrokenCap("invalid 'receiverAnswer'");
                }

                var result = answer.pipeline.getPipelinedCap(ops);
                if (result == null) {
                    return ClientHook.newBrokenCap("Unrecognised pipeline ops");
                }

                if (result.getBrand() == this) {
                    // TODO Tribble 4-way race!
                    return result;
                }

                return result;

            case THIRD_PARTY_HOSTED:
                return ClientHook.newBrokenCap("Third party caps not supported");

            default:
                return ClientHook.newBrokenCap("unknown CapDescriptor type");
        }
    }


    private ClientHook importCap(int importId, boolean isPromise, Integer fd) {
        // Receive a new import.

        var imp = imports.put(importId);

        if (imp.importClient == null) {
            imp.importClient = new ImportClient(importId, fd);
        }
        else {
            imp.importClient.setFdIfMissing(fd);
        }
        imp.importClient.addRemoteRef();

        if (!isPromise) {
            imp.appClient = imp.importClient;
            return imp.importClient;
        }

        if (imp.appClient != null) {
            return imp.appClient;
        }

        imp.promise = new CompletableFuture<ClientHook>();
        var result = new PromiseClient(imp.importClient, imp.promise, importId);
        imp.appClient = result;
        return result;
    }

    ClientHook writeTarget(ClientHook cap, RpcProtocol.MessageTarget.Builder target) {
        return cap.getBrand() == this
                ? ((RpcClient)cap).writeTarget(target)
                : cap;
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

        final int importId;
        int remoteRefCount = 0;
        Integer fd;

        ImportClient(int importId, Integer fd) {
            this.importId = importId;
            this.fd = fd;
        }

        void addRemoteRef() {
            this.remoteRefCount++;
        }

        void setFdIfMissing(Integer fd) {
            if (this.fd == null) {
                this.fd = fd;
            }
        }

        public void dispose() {
            // TODO manage destruction...
            var imp = imports.find(importId);
            if (imp != null) {
                if (imp.importClient == this) {
                    imports.erase(importId, imp);
                }
            }

            if (remoteRefCount > 0 && !isDisconnected()) {
                var message = connection.newOutgoingMessage(1024);
                var builder = message.getBody().initAs(RpcProtocol.Message.factory).initRelease();
                builder.setId(importId);
                builder.setReferenceCount(remoteRefCount);
                message.send();
            }
        }

        @Override
        public Integer writeDescriptor(RpcProtocol.CapDescriptor.Builder descriptor, List<Integer> fds) {
            descriptor.setReceiverHosted(importId);
            return null;
        }

        @Override
        public ClientHook writeTarget(RpcProtocol.MessageTarget.Builder target) {
            target.setImportedCap(importId);
            return null;
        }

        @Override
        public VoidPromiseAndPipeline call(long interfaceId, short methodId, CallContextHook context) {
            return null;
        }

        @Override
        public CompletableFuture<ClientHook> whenMoreResolved() {
            return null;
        }
    }

    enum ResolutionType {
        UNRESOLVED,
        REMOTE,
        REFLECTED,
        MERGED,
        BROKEN
    }

    class PromiseClient extends RpcClient {
        final ClientHook cap;
        final Integer importId;
        final CompletableFuture<ClientHook> promise;
        boolean receivedCall = false;
        ResolutionType resolutionType = ResolutionType.UNRESOLVED;

        public PromiseClient(RpcClient initial,
                             CompletableFuture<ClientHook> eventual,
                             Integer importId) {
            this.cap = initial;
            this.importId = importId;
            this.promise = eventual.thenApply(resolution -> {
                return resolve(resolution);
            });
        }

        public boolean isResolved() {
            return resolutionType != ResolutionType.UNRESOLVED;
        }

        private ClientHook resolve(ClientHook replacement) {
            assert !isResolved();

            var replacementBrand = replacement.getBrand();
            boolean isSameConnection = replacementBrand == RpcState.this;
            if (isSameConnection) {
                var promise = replacement.whenMoreResolved();
                if (promise != null) {
                    var other = (PromiseClient)replacement;
                    while (other.resolutionType == ResolutionType.MERGED) {
                        replacement = other.cap;
                        other = (PromiseClient)replacement;
                        assert replacement.getBrand() == replacementBrand;
                    }

                    if (other.isResolved()) {
                        resolutionType = other.resolutionType;
                    }
                    else {
                        other.receivedCall = other.receivedCall || receivedCall;
                        resolutionType = ResolutionType.MERGED;
                    }
                }
                else {
                    resolutionType = ResolutionType.REMOTE;
                }
            }
            else {
                if (replacementBrand == NULL_CAPABILITY_BRAND ||
                        replacementBrand == BROKEN_CAPABILITY_BRAND) {
                    resolutionType = ResolutionType.BROKEN;
                }
                else {
                    resolutionType = ResolutionType.REFLECTED;
                }
            }

            assert isResolved();

            // TODO Flow control

            if (resolutionType == ResolutionType.REFLECTED && receivedCall && !isDisconnected()) {
                var message = connection.newOutgoingMessage(1024);
                var disembargo = message.getBody().initAs(RpcProtocol.Message.factory).initDisembargo();
                {
                    var redirect = RpcState.this.writeTarget(cap, disembargo.initTarget());
                    assert redirect == null;
                }

                var embargo = new Embargo();
                var embargoId = embargos.next(embargo);
                disembargo.getContext().setSenderLoopback(embargoId);

                embargo.fulfiller = new CompletableFuture<>();

                final ClientHook finalReplacement = replacement;
                var embargoPromise = embargo.fulfiller.thenApply(x -> {
                    return finalReplacement;
                });

                replacement = Capability.newLocalPromiseClient(embargoPromise);
                message.send();

            }
            return replacement;
        }

        ClientHook writeTarget(ClientHook cap, RpcProtocol.MessageTarget.Builder target) {
            if (cap.getBrand() == this) {
                return ((RpcClient)cap).writeTarget(target);
            }
            else {
                return cap;
            }
        }

        @Override
        public Integer writeDescriptor(RpcProtocol.CapDescriptor.Builder target, List<Integer> fds) {
            receivedCall = true;
            return RpcState.this.writeDescriptor(cap, target, fds);
        }

        @Override
        public ClientHook writeTarget(RpcProtocol.MessageTarget.Builder target) {
            receivedCall = true;
            return RpcState.this.writeTarget(cap, target);
        }

        @Override
        public ClientHook getInnermostClient() {
            receivedCall = true;
            return RpcState.this.getInnermostClient(cap);
        }

        @Override
        public VoidPromiseAndPipeline call(long interfaceId, short methodId, CallContextHook context) {
            return null;
        }
    }

}
