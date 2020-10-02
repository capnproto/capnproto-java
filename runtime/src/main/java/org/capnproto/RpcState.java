package org.capnproto;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

final class RpcState {

    static final class Question {
        final int id;
        List<Integer> paramExports;
        boolean isAwaitingReturn = false;
        boolean isTailCall = false;
        boolean skipFinish = false;

        Question(int id) {
            this.id = id;
        }

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
        final int id;
        int refcount;
        ClientHook clientHook;
        CompletionStage<?> resolveOp;

        Export(int id) {
            this.id = id;
        }
    }

    static final class Import {
        ImportClient importClient;
        RpcClient appClient;
        CompletableFuture<ClientHook> promise;
        // If non-null, the import is a promise.
    }

    final static class Embargo {
        final int id;
        CompletableFuture<?> disembargo;

        Embargo(int id) {
            this.id = id;
        }
    }

    private final ExportTable<Export> exports = new ExportTable<Export>() {
        @Override
        Export newExportable(int id) {
            return new Export(id);
        }
    };

    private final ExportTable<Question> questions = new ExportTable<Question>() {
        @Override
        Question newExportable(int id) {
            return new Question(id);
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
        Embargo newExportable(int id) {
            return new Embargo(id);
        }
    };

    private final HashMap<ClientHook, Integer> exportsByCap = new HashMap<>();
    private final VatNetwork.Connection connection;
    private final Capability.Client bootstrapInterface;
    private Throwable disconnected = null;
    private CompletableFuture<?> messageReady = CompletableFuture.completedFuture(null);

    RpcState(VatNetwork.Connection connection, Capability.Client bootstrapInterface) {
        this.connection = connection;
        this.bootstrapInterface = bootstrapInterface;
    }

    final boolean isDisconnected() {
        return this.disconnected != null;
    }

    final boolean isConnected() {
        return !isDisconnected();
    }

    ClientHook restore() {
        var question = questions.next();
        question.isAwaitingReturn = true;
        question.paramExports = List.of();
        var message = connection.newOutgoingMessage(64);
        var builder = message.getBody().initAs(RpcProtocol.Message.factory).initBootstrap();
        builder.setQuestionId(question.id);
        message.send();
        var pipeline = new RpcPipeline(question);
        return pipeline.getPipelinedCap(new PipelineOp[0]);
    }

    // run message loop once
    final CompletableFuture<?> runOnce() {

        if (isDisconnected()) {
            return CompletableFuture.failedFuture(disconnected);
        }

        if (!messageReady.isDone()) {
            return messageReady;
        }

        messageReady = connection.receiveIncomingMessage().thenAccept(message -> {
                try {
                    handleMessage(message);
                }
                catch (Exception exc) {
                    this.disconnected = exc;
                }
        }).exceptionally(exc -> {
            this.disconnected = exc;
            return null;
        });

        return messageReady;
    }

    // run message loop until promise is completed
    public final <T> CompletableFuture<T> messageLoop(CompletableFuture<T> done) {
        if (done.isDone()) {
            return done;
        }

        if (isDisconnected()) {
            done.completeExceptionally(disconnected);
            return done;
        }

        return connection.receiveIncomingMessage().thenCompose(message -> {
            try {
                handleMessage(message);
            }
            catch (Exception exc) {
                done.completeExceptionally(exc);
            }
            return messageLoop(done);
        });
    }

    synchronized void handleMessage(IncomingRpcMessage message) throws RpcException {
        var reader = message.getBody().getAs(RpcProtocol.Message.factory);
        System.out.println(reader.which());
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
                : Capability.newBrokenCap("Invalid pipeline transform.");

        response.send();

        assert answer.active;
        assert answer.resultExports != null;
        assert answer.pipeline != null;
    }

    void handleCall(IncomingRpcMessage message, RpcProtocol.Call.Reader call) {
        var cap = getMessageTarget(call.getTarget());
        if (cap == null) {
            return;
        }

        boolean redirectResults;
        switch (call.getSendResultsTo().which()) {
            case CALLER:
                redirectResults = false;
                break;
            case YOURSELF:
                redirectResults = true;
                break;
            default:
                assert false: "Unsupported 'Call.sendResultsTo'.";
                return;
        }

        var payload = call.getParams();
        var capTableArray = receiveCaps(payload.getCapTable(), message.getAttachedFds());
        var answerId = call.getQuestionId();
        var cancel = new CompletableFuture<java.lang.Void>();
        var context = new RpcCallContext(
                answerId, message, capTableArray,
                payload.getContent(), redirectResults, cancel,
                call.getInterfaceId(), call.getMethodId());

        {
            var answer = answers.put(answerId);
            assert !answer.active : "questionId is already in use";
            if (answer.active) {
                return;
            }

            answer.active = true;
            answer.callContext = context;
        }

        var pap = startCall(call.getInterfaceId(), call.getMethodId(), cap, context);
        {
            var answer = answers.find(answerId);
            assert answer != null;
            answer.pipeline = pap.pipeline;

            if (redirectResults) {
                answer.redirectedResults = pap.promise.thenApply(x -> {
                    return context.consumeRedirectedResponse();
                });
                // TODO cancellation deferral
            }
            else {
                pap.promise.thenAccept(x -> {
                    context.sendReturn();
                }).exceptionally(exc -> {
                    context.sendErrorReturn(exc);
                    // TODO wait on the cancellation...
                    return null;
                });
            }
        }
    }

    private ClientHook.VoidPromiseAndPipeline startCall(long interfaceId, short methodId, ClientHook cap, RpcCallContext context) {
        // TODO gateways...?
        return cap.call(interfaceId, methodId, context);
    }

    void handleReturn(IncomingRpcMessage message, RpcProtocol.Return.Reader callReturn) {
        var exportsToRelease = new ArrayList<Integer>();

        var question = questions.find(callReturn.getAnswerId());
        assert question != null : "Invalid question ID in Return message.";
        if (question == null) {
            return;
        }

        assert question.isAwaitingReturn: "Duplicate Return";
        if (!question.isAwaitingReturn) {
            return;
        }

        question.isAwaitingReturn = false;
        if (callReturn.getReleaseParamCaps()) {
            exportsToRelease.addAll(question.paramExports);
            question.paramExports = List.of();
        }

        assert !callReturn.isTakeFromOtherQuestion(): "Not implemented";
        if (callReturn.isTakeFromOtherQuestion()) {
            // TODO process isTakeFromOtherQuestion...
            return;
        }

        switch (callReturn.which()) {
            case RESULTS:
                if (question.isTailCall) {
                    // TODO resultsSentElsewhere
                    return;
                }
                var payload = callReturn.getResults();
                var capTable = receiveCaps(payload.getCapTable(), message.getAttachedFds());
                var response = new RpcResponseImpl(question, message, capTable, payload.getContent());
                question.answer(response);
                break;
            case EXCEPTION:
                assert !question.isTailCall : "Tail call `Return` must set `resultsSentElsewhere`, not `exception`.";
                if (question.isTailCall) {
                    return;
                }
                question.reject(RpcException.toException(callReturn.getException()));
                break;
            case CANCELED:
                assert false : "Return message falsely claims call was canceled.";
                break;
            case RESULTS_SENT_ELSEWHERE:
                assert question.isTailCall : "`Return` had `resultsSentElsewhere` but this was not a tail call.";
                if (!question.isTailCall) {
                    return;
                }
                // Tail calls are fulfilled with a null pointer.
                question.answer(() -> null);
                break;

            case TAKE_FROM_OTHER_QUESTION:
                var other = callReturn.getTakeFromOtherQuestion();
                var answer = answers.find(other);
                assert answer != null : "`Return.takeFromOtherQuestion` had invalid answer ID.";
                if (answer == null) {
                    return;
                }
                assert answer.redirectedResults != null : "`Return.takeFromOtherQuestion` referenced a call that did not use `sendResultsTo.yourself`.";
                if (answer.redirectedResults == null) {
                    return;
                }
                question.response = answer.redirectedResults;
                answer.redirectedResults = null;
                break;
            default:
                assert false : "Unknown 'Return' type.";
                return;
        }

    }

    void handleFinish(RpcProtocol.Finish.Reader finish) {
    }

    void handleResolve(IncomingRpcMessage message, RpcProtocol.Resolve.Reader resolve) {

        ClientHook replacement = null;
        Throwable exc = null;

        switch (resolve.which()) {
            case CAP:
                replacement = receiveCap(resolve.getCap(), message.getAttachedFds());
                break;
            case EXCEPTION:
                exc = new RuntimeException(resolve.getException().getReason().toString());
                break;
            default:
                assert false;
                return;
        }

        var imp = imports.find(resolve.getPromiseId());
        if (imp == null) {
            return;
        }

        var fulfiller = imp.promise;
        if (fulfiller != null) {
            if (exc != null) {
                fulfiller.completeExceptionally(exc);
            }
            else {
                fulfiller.complete(replacement);
            }
        }
        else if (imp.importClient != null) {
            // It appears this is a valid entry on the import table, but was not expected to be a
            // promise.
            assert false;
        }
    }

    void handleDisembargo(RpcProtocol.Disembargo.Reader disembargo) {
        var ctx = disembargo.getContext();
        switch (ctx.which()) {
            case SENDER_LOOPBACK:
                var target = getMessageTarget(disembargo.getTarget());
                if (target == null) {
                    // Exception already reported.
                    return;
                }
                for (; ; ) {
                    var resolved = target.getResolved();
                    if (resolved == null) {
                        break;
                    }
                    target = resolved;
                }

                assert target.getBrand() == RpcState.this : "'Disembargo' of type 'senderLoopback' sent to an object that does not point back to the sender.";
                if (target.getBrand() != this) {
                    return;
                }

                var embargoId = ctx.getSenderLoopback();

                // TODO run this later...
                if (isDisconnected()) {
                    return;
                }

                var rpcTarget = (RpcClient) target;
                var message = connection.newOutgoingMessage(1024);
                var builder = message.getBody().initAs(RpcProtocol.Message.factory).initDisembargo();
                var redirect = rpcTarget.writeTarget(builder.initTarget());
                // Disembargoes should only be sent to capabilities that were previously the subject of
                // a `Resolve` message.  But `writeTarget` only ever returns non-null when called on
                // a PromiseClient.  The code which sends `Resolve` and `Return` should have replaced
                // any promise with a direct node in order to solve the Tribble 4-way race condition.
                // See the documentation of Disembargo in rpc.capnp for more.
                if (redirect == null) {
                    assert false: "'Disembargo' of type 'senderLoopback' sent to an object that does not appear to have been the subject of a previous 'Resolve' message.";
                    return;
                }
                builder.getContext().setReceiverLoopback(embargoId);
                message.send();
                break;

            case RECEIVER_LOOPBACK:
                var embargo = embargos.find(ctx.getReceiverLoopback());
                if (embargo == null) {
                    assert false: "Invalid embargo ID in 'Disembargo.context.receiverLoopback'.";
                    return;
                }
                assert embargo.disembargo != null;
                embargo.disembargo.complete(null);
                embargos.erase(ctx.getReceiverLoopback(), embargo);
                break;

            default:
                assert false: "Unimplemented Disembargo type. " + ctx.which();
                return;
        }
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
        var export = exports.next();
        export.refcount = 1;
        export.clientHook = inner;

        var wrapped = inner.whenMoreResolved();
        if (wrapped != null) {
            // This is a promise.  Arrange for the `Resolve` message to be sent later.
            export.resolveOp = resolveExportedPromise(export.id, wrapped);
            descriptor.setSenderPromise(export.id);
        }
        else {
            descriptor.setSenderHosted(export.id);
        }
        return export.id;
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
                    return Capability.newBrokenCap("invalid 'receiverHosted' export ID");
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
                    return Capability.newBrokenCap("invalid 'receiverAnswer'");
                }

                var result = answer.pipeline.getPipelinedCap(ops);
                if (result == null) {
                    return Capability.newBrokenCap("Unrecognised pipeline ops");
                }

                if (result.getBrand() == this) {
                    // TODO Tribble 4-way race!
                    return result;
                }

                return result;

            case THIRD_PARTY_HOSTED:
                return Capability.newBrokenCap("Third party caps not supported");

            default:
                return Capability.newBrokenCap("unknown CapDescriptor type");
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

    ClientHook getMessageTarget(RpcProtocol.MessageTarget.Reader target) {
        switch (target.which()) {
            case IMPORTED_CAP:
                var exp = exports.find(target.getImportedCap());
                assert exp != null: "Message target is not a current export ID.";
                return exp != null ? exp.clientHook : null;

            case PROMISED_ANSWER:
                var promisedAnswer = target.getPromisedAnswer();
                var base = answers.find(promisedAnswer.getQuestionId());
                assert base != null && base.active: "PromisedAnswer.questionId is not a current question.";
                if (base == null || !base.active) {
                    return null;
                }

                var pipeline = base.pipeline;
                if (pipeline == null) {
                    pipeline = PipelineHook.newBrokenPipeline(
                            RpcException.failed("Pipeline call on a request that returned no capabilities or was already closed."));
                }

                var ops = PipelineOp.ToPipelineOps(promisedAnswer);
                if (ops == null) {
                    return null;
                }
                return pipeline.getPipelinedCap(ops);

            default:
                assert false: "Unknown message target type. " + target.which();
                return null;
        }
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

    interface RpcServerResponse {
        AnyPointer.Builder getResultsBuilder();
    }

    static class RpcResponseImpl implements RpcResponse {
        private final Question question;
        private final IncomingRpcMessage message;
        private final AnyPointer.Reader results;

        RpcResponseImpl(Question question,
                        IncomingRpcMessage message,
                        List<ClientHook> capTable,
                        AnyPointer.Reader results) {
            this.question = question;
            this.message = message;
            this.results = results.imbue(new ReaderCapabilityTable(capTable));
        }

        public AnyPointer.Reader getResults() {
            return results;
        }
    }

    class RpcServerResponseImpl implements RpcServerResponse {

        final OutgoingRpcMessage message;
        final RpcProtocol.Payload.Builder payload;
        final BuilderCapabilityTable capTable = new BuilderCapabilityTable();

        RpcServerResponseImpl(OutgoingRpcMessage message, RpcProtocol.Payload.Builder payload) {
            this.message = message;
            this.payload = payload;
        }

        @Override
        public AnyPointer.Builder getResultsBuilder() {
            return payload.getContent().imbue(capTable);
        }

        List<Integer> send() {
            var capTable = this.capTable.getTable();
            var fds = List.<Integer>of();
            var exports = writeDescriptors(capTable, payload, fds);
            // TODO process FDs
            message.setFds(fds);

            for (int ii = 0; ii < capTable.length; ++ii) {
                var slot = capTable[ii];
                if (slot != null) {
                    capTable[ii] = getInnermostClient(slot);
                }
            }

            message.send();
            return exports;
        }
    }

    private static class LocallyRedirectedRpcResponse implements RpcServerResponse, RpcResponse {

        private final MessageBuilder message = new MessageBuilder();

        @Override
        public AnyPointer.Builder getResultsBuilder() {
            return message.getRoot(AnyPointer.factory);
        }

        @Override
        public AnyPointer.Reader getResults() {
            return getResultsBuilder().asReader();
        }
    }

    class RpcCallContext implements CallContextHook {

        final int answerId;
        final long interfaceId;
        final short methodId;

        // request
        IncomingRpcMessage request;
        final AnyPointer.Reader params;

        // response
        RpcServerResponse response;
        RpcProtocol.Return.Builder returnMessage;
        boolean redirectResults = false;
        boolean responseSent = false;

        boolean cancelRequested = false;
        boolean cancelAllowed = false;

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
            if (response != null) {
                return response.getResultsBuilder();
            }

            if (redirectResults || isDisconnected()) {
                response = new LocallyRedirectedRpcResponse();
            }
            else {
                var message = connection.newOutgoingMessage(1024);
                returnMessage = message.getBody().initAs(RpcProtocol.Message.factory).initReturn();
                response = new RpcServerResponseImpl(message, returnMessage.getResults());
            }

            return response.getResultsBuilder();
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

        RpcResponse consumeRedirectedResponse() {
                assert this.redirectResults;

                if (this.response == null) {
                    getResults(); // force initialization of response
                }

                return ((LocallyRedirectedRpcResponse)this.response);
            }

        void sendReturn() {
            assert !redirectResults;

            if (!this.cancelRequested && isDisconnected()) {
                assert false: "Cancellation should have been requested on disconnect.";
                return;
            }

            if (this.response == null) {
                getResults(); // force initialization
            }

            this.returnMessage.setAnswerId(this.answerId);
            this.returnMessage.setReleaseParamCaps(false);

            List<Integer> exports;
            try {
                exports = ((RpcServerResponseImpl)response).send();
            }
            catch (Throwable exc) {
                this.responseSent = false;
                sendErrorReturn(exc);
            }
        }

        void sendErrorReturn(Throwable exc) {
            assert !redirectResults;

            if (!isFirstResponder()) {
                return;
            }

            if (isConnected()) {
                var message = connection.newOutgoingMessage(1024);
                var builder = message.getBody().initAs(RpcProtocol.Message.factory).initReturn();
                builder.setAnswerId(this.answerId);
                builder.setReleaseParamCaps(false);
                RpcException.fromException(exc, builder.initException());
                message.send();
            }

            cleanupAnswerTable(null, false);
        }

        boolean isFirstResponder() {
            if (this.responseSent) {
                return false;
            }
            this.responseSent = true;
            return true;
        }

        void cleanupAnswerTable(List<Integer> resultExports, boolean shouldFreePipeline) {
            if (this.cancelRequested) {
                assert resultExports.size() == 0;
                answers.erase(this.answerId);
                return;
            }
            else {
                var answer = answers.find(answerId);
                answer.callContext = null;
                answer.resultExports = resultExports;

                if (shouldFreePipeline) {
                    assert resultExports.size() == 0;
                    answer.pipeline = null;
                }
            }
        }
    }

    enum PipelineState {
        WAITING, RESOLVED, BROKEN
    }

    class RpcPipeline implements PipelineHook {

        private final Question question;
        private PipelineState state = PipelineState.WAITING;
        private RpcResponse resolved;
        private Throwable broken;

        final HashMap<List<PipelineOp>, ClientHook> clientMap = new HashMap<>();
        final CompletionStage<RpcResponse> redirectLater;
        final CompletionStage<java.lang.Void> resolveSelf;

        RpcPipeline(Question question,
                    CompletionStage<RpcResponse> redirectLater) {
            this.question = question;
            this.redirectLater = redirectLater;
            this.resolveSelf = this.redirectLater
                    .thenAccept(response -> {
                        this.state = PipelineState.RESOLVED;
                        this.resolved = response;
                    })
                    .exceptionally(exc -> {
                        this.state = PipelineState.BROKEN;
                        this.broken = exc;
                        return null;
                    });
        }

        RpcPipeline(Question question) {
            // never resolves
            this.question = question;
            this.redirectLater = null;
            this.resolveSelf = null;
        }

        @Override
        public ClientHook getPipelinedCap(PipelineOp[] ops) {
            // TODO avoid conversion to/from ArrayList?
            var key = new ArrayList<>(Arrays.<PipelineOp>asList(ops));
            var hook = this.clientMap.computeIfAbsent(key, k -> {
                switch (state) {
                    case WAITING:
                        if (redirectLater != null) {
                            // TODO implement redirect
                            assert false: "redirection not implemented";
                            return null;
                        }
                        return new PipelineClient(question, ops);
                    case RESOLVED:
                        return resolved.getResults().getPipelinedCap(ops);
                    default:
                        return Capability.newBrokenCap(broken);
                }
            });
            return hook;
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
            return newCallNoIntercept(interfaceId, methodId);
        }

        @Override
        public VoidPromiseAndPipeline call(long interfaceId, short methodId, CallContextHook context) {
            return null;
        }

        public VoidPromiseAndPipeline callNoIntercept(long interfaceId, short methodId, CallContextHook context) {
            var params = context.getParams();
            var request = newCallNoIntercept(interfaceId, methodId);
            context.allowCancellation();
            return context.directTailCall(request.hook);
        }

        @Override
        public final Object getBrand() {
            return RpcState.this;
        }

        private Request<AnyPointer.Builder, AnyPointer.Reader> newCallNoIntercept(long interfaceId, short methodId) {
            if (isDisconnected()) {
                return Request.newBrokenRequest(disconnected);
            }

            var request = new RpcRequest(this);
            var callBuilder = request.getCall();
            callBuilder.setInterfaceId(interfaceId);
            callBuilder.setMethodId(methodId);
            var root = request.getRoot();
            return Request.newTypelessRequest(root, request);
        }
    }

    class RpcRequest implements RequestHook {

        final RpcClient target;
        final OutgoingRpcMessage message;
        final BuilderCapabilityTable capTable = new BuilderCapabilityTable();
        final RpcProtocol.Call.Builder callBuilder;
        final AnyPointer.Builder paramsBuilder;

        RpcRequest(RpcClient target) {
            this.target = target;
            this.message = connection.newOutgoingMessage(1024);
            this.callBuilder = message.getBody().getAs(RpcProtocol.Message.factory).initCall();
            this.paramsBuilder = callBuilder.getParams().getContent().imbue(this.capTable);
        }

        AnyPointer.Builder getRoot() {
            return this.paramsBuilder;
        }
        RpcProtocol.Call.Builder getCall() {
            return this.callBuilder;
        }

        @Override
        public RemotePromise<AnyPointer.Reader> send() {
            if (isDisconnected()) {
                return new RemotePromise<>(CompletableFuture.failedFuture(disconnected), null);
            }

            var redirect = this.target.writeTarget(this.callBuilder.getTarget());
            if (redirect != null) {
                var replacement = redirect.newCall(
                        this.callBuilder.getInterfaceId(), this.callBuilder.getMethodId());
                replacement.params = paramsBuilder;
                return replacement.hook.send();
            }

            var question = sendInternal(false);

            // The pipeline must get notified of resolution before the app does to maintain ordering.
            var pipeline = new RpcPipeline(question, question.response);

            var appPromise = question.response.thenApply(response -> {
                var results = response.getResults();
                return new Response(results, response);
            });

            return new RemotePromise<>(appPromise, pipeline);
        }

        Question sendInternal(boolean isTailCall) {
            // TODO refactor
            var fds = List.<Integer>of();
            var exports = writeDescriptors(capTable.getTable(), callBuilder.getParams(), fds);
            message.setFds(fds);
            var question = questions.next();
            question.isAwaitingReturn = true;
            question.isTailCall = isTailCall;
            question.paramExports = exports;

            callBuilder.setQuestionId(question.id);
            if (isTailCall) {
                callBuilder.getSendResultsTo().getYourself();
            }
            try {
                message.send();
            } catch (Exception exc) {
                question.isAwaitingReturn = false;
                question.skipFinish = true;
                question.reject(exc);
            }
            return question;
        }

        @Override
        public final Object getBrand() {
            return RpcState.this;
        }

        final class TailInfo {
            int questionId;
            CompletableFuture<java.lang.Void> promise;
            PipelineHook pipeline;
        }

        TailInfo tailSend() {
            if (isDisconnected()) {
                // Disconnected; fall back to a regular send() which will fail appropriately.
                return null;
            }

            // TODO implement tail-calls
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

                var embargo = embargos.next();
                disembargo.getContext().setSenderLoopback(embargo.id);

                embargo.disembargo = new CompletableFuture<>();

                final ClientHook finalReplacement = replacement;
                var embargoPromise = embargo.disembargo.thenApply(x -> {
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

    class PipelineClient extends RpcClient {

        private final Question question;
        private final PipelineOp[] ops;

        PipelineClient(Question question, PipelineOp[] ops) {
            this.question = question;
            this.ops = ops;
        }

        @Override
        public VoidPromiseAndPipeline call(long interfaceId, short methodId, CallContextHook context) {
            return null;
        }

        @Override
        public CompletableFuture<ClientHook> whenMoreResolved() {
            return null;
        }

        @Override
        public Integer writeDescriptor(RpcProtocol.CapDescriptor.Builder descriptor, List<Integer> fds) {
            var promisedAnswer = descriptor.initReceiverAnswer();
            promisedAnswer.setQuestionId(question.id);
            PipelineOp.FromPipelineOps(ops, promisedAnswer);
            return null;
        }

        @Override
        public ClientHook writeTarget(RpcProtocol.MessageTarget.Builder target) {
            var builder = target.initPromisedAnswer();
            builder.setQuestionId(question.id);
            PipelineOp.FromPipelineOps(ops, builder);
            return null;
        }
    }
}
