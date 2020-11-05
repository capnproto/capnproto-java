package org.capnproto;

import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

final class RpcState<VatId> {

    private static int messageSizeHint() {
        return 1 + RpcProtocol.Message.factory.structSize().total();
    }

    private static <B extends StructBuilder, R extends StructReader> int messageSizeHint(StructFactory<B, R> factory) {
        return messageSizeHint() + factory.structSize().total();
    }

    private static int exceptionSizeHint(Throwable exc) {
        return RpcProtocol.Exception.factory.structSize().total() + exc.getMessage().length();
    }

    private static final int MESSAGE_TARGET_SIZE_HINT
            = RpcProtocol.MessageTarget.factory.structSize().total()
            + RpcProtocol.PromisedAnswer.factory.structSize().total()
            + 16;

    private static final int CAP_DESCRIPTOR_SIZE_HINT
            = RpcProtocol.CapDescriptor.factory.structSize().total()
            + RpcProtocol.PromisedAnswer.factory.structSize().total();

    private final class QuestionDisposer {

        final int id;
        boolean skipFinish;
        boolean isAwaitingReturn;

        QuestionDisposer(int id) {
            this.id = id;
        }

        void dispose() {
            var ref = questions.find(this.id);
            if (ref == null) {
                assert false: "Question ID no longer on table?";
                return;
            }

            if (isConnected() && !this.skipFinish) {
                var sizeHint = messageSizeHint(RpcProtocol.Finish.factory);
                var message = connection.newOutgoingMessage(sizeHint);
                var builder = message.getBody().getAs(RpcProtocol.Message.factory).initFinish();
                builder.setQuestionId(this.id);
                builder.setReleaseResultCaps(this.isAwaitingReturn);
                message.send();
            }

            // Check if the question has returned and, if so, remove it from the table.
            // Remove question ID from the table.  Must do this *after* sending `Finish` to ensure that
            // the ID is not re-allocated before the `Finish` message can be sent.
            assert !this.isAwaitingReturn;
            questions.erase(id);
        }
    }

    private final class QuestionRef extends WeakReference<Question> {

        private final QuestionDisposer disposer;

        QuestionRef(Question question, ReferenceQueue<Question> queue) {
            super(question, queue);
            this.disposer = question.disposer;
        }

        void dispose() {
            this.disposer.dispose();
        }
    }

    private class Question {

        CompletableFuture<RpcResponse> response = new CompletableFuture<>();
        int[] paramExports = new int[0];
        private final QuestionDisposer disposer;
        boolean isTailCall = false;

        Question(int id) {
            this.disposer = new QuestionDisposer(id);
        }

        int getId() {
            return this.disposer.id;
        }

        boolean isAwaitingReturn() {
            return this.disposer.isAwaitingReturn;
        }

        public void setAwaitingReturn(boolean value) {
            this.disposer.isAwaitingReturn = value;
        }

        void reject(Throwable exc) {
            this.response.completeExceptionally(exc);
        }

        void answer(RpcResponse response) {
            this.response.complete(response);
        }

        void setSkipFinish(boolean value) {
            this.disposer.skipFinish = value;
        }
    }

    class QuestionExportTable implements Iterable<Question> {
        private final HashMap<Integer, WeakReference<Question>> slots = new HashMap<>();
        private final Queue<Integer> freeIds = new PriorityQueue<>();
        private int max = 0;

        public Question find(int id) {
            var ref = this.slots.get(id);
            return ref == null ? null : ref.get();
        }

        public Question erase(int id) {
            var value = this.slots.get(id);
            if (value != null) {
                freeIds.add(id);
                this.slots.remove(id);
                return value.get();
            } else {
                return null;
            }
        }

        public Question next() {
            int id = freeIds.isEmpty() ? max++ : freeIds.remove();
            var value = new Question(id);
            var prev = slots.put(id, new QuestionRef(value, questionRefs));
            assert prev == null;
            return value;
        }

        @Override
        public Iterator<Question> iterator() {
            return this.slots.values()
                    .stream()
                    .map(Reference::get)
                    .filter(Objects::nonNull)
                    .iterator();
        }

        @Override
        public void forEach(Consumer<? super Question> action) {
            var iter = this.iterator();
            while (iter.hasNext()) {
                action.accept(iter.next());
            }
        }
    }

    final class Answer {
        final int answerId;
        boolean active = false;
        PipelineHook pipeline;
        CompletionStage<RpcResponse> redirectedResults;
        RpcCallContext callContext;
        int[] resultExports;

        Answer(int answerId) {
            this.answerId = answerId;
        }
    }

    static final class Export {
        final int exportId;
        int refcount;
        ClientHook clientHook;
        CompletionStage<java.lang.Void> resolveOp;

        Export(int exportId) {
            this.exportId = exportId;
        }
    }

    final class Import {
        final int importId;
        ImportRef importClient;
        int remoteRefCount;
        WeakReference<RpcClient> appClient;
        CompletableFuture<ClientHook> promise;
        // If non-null, the import is a promise.

        Import(int importId) {
            this.importId = importId;
        }

        void addRemoteRef() {
            this.remoteRefCount++;
        }

        public void dispose() {
            // Remove self from the import table.
            var imp = imports.find(importId);
            if (imp == this) {
                imports.erase(importId, imp);
            }

            // Send a message releasing our remote references.
            if (this.remoteRefCount > 0 && isConnected()) {
                int sizeHint = messageSizeHint(RpcProtocol.Release.factory);
                var message = connection.newOutgoingMessage(sizeHint);
                var builder = message.getBody().initAs(RpcProtocol.Message.factory).initRelease();
                builder.setId(importId);
                builder.setReferenceCount(remoteRefCount);
                message.send();
            }
        }
    }

    final static class Embargo {
        final int id;
        final CompletableFuture<java.lang.Void> disembargo = new CompletableFuture<>();

        Embargo(int id) {
            this.id = id;
        }
    }

    private final ExportTable<Export> exports = new ExportTable<>() {
        @Override
        Export newExportable(int id) {
            return new Export(id);
        }
    };

    private final QuestionExportTable questions = new QuestionExportTable();

    private final ImportTable<Answer> answers = new ImportTable<>() {
        @Override
        protected Answer newImportable(int answerId) {
            return new Answer(answerId);
        }
    };

    private final ImportTable<Import> imports = new ImportTable<>() {
        @Override
        protected Import newImportable(int importId) {
            return new Import(importId);
        }
    };

    private final ExportTable<Embargo> embargos = new ExportTable<>() {
        @Override
        Embargo newExportable(int id) {
            return new Embargo(id);
        }
    };

    private final Map<ClientHook, Integer> exportsByCap = new HashMap<>();
    private final BootstrapFactory<VatId> bootstrapFactory;
    private final VatNetwork.Connection<VatId> connection;
    private final CompletableFuture<java.lang.Void> onDisconnect;
    private Throwable disconnected = null;
    private CompletableFuture<java.lang.Void> messageReady = CompletableFuture.completedFuture(null);
    private final CompletableFuture<java.lang.Void> messageLoop;
    private final ReferenceQueue<Question> questionRefs = new ReferenceQueue<>();
    private final ReferenceQueue<ImportClient> importRefs = new ReferenceQueue<>();

    RpcState(BootstrapFactory<VatId>  bootstrapFactory,
             VatNetwork.Connection<VatId> connection,
             CompletableFuture<java.lang.Void> onDisconnect) {
        this.bootstrapFactory = bootstrapFactory;
        this.connection = connection;
        this.onDisconnect = onDisconnect;
        this.messageLoop = this.doMessageLoop();
    }

    public CompletableFuture<java.lang.Void> getMessageLoop() {
        return this.messageLoop;
    }

    public CompletableFuture<java.lang.Void> onDisconnect() {
        return this.messageLoop;
    }

    CompletableFuture<java.lang.Void> disconnect(Throwable exc) {
        if (isDisconnected()) {
            return CompletableFuture.failedFuture(this.disconnected);
        }

        var networkExc = RpcException.disconnected(exc.getMessage());

        // All current questions complete with exceptions.
        for (var question: questions) {
            question.reject(networkExc);
        }

        List<PipelineHook> pipelinesToRelease = new ArrayList<>();
        List<ClientHook> clientsToRelease = new ArrayList<>();
        List<CompletionStage<RpcResponse>> tailCallsToRelease = new ArrayList<>();
        List<CompletionStage<java.lang.Void>> resolveOpsToRelease = new ArrayList<>();

        for (var answer : answers) {
            if (answer.pipeline != null) {
                pipelinesToRelease.add(answer.pipeline);
                answer.pipeline = null;
            }

            if (answer.redirectedResults != null) {
                tailCallsToRelease.add(answer.redirectedResults);
                answer.redirectedResults = null;
            }

            if (answer.callContext != null) {
                answer.callContext.requestCancel();
            }
        }

        for (var export : exports) {
            clientsToRelease.add(export.clientHook);
            resolveOpsToRelease.add(export.resolveOp);
            export.clientHook = null;
            export.resolveOp = null;
            export.refcount = 0;
        }

        for (var imp : imports) {
            if (imp.promise != null) {
                imp.promise.completeExceptionally(networkExc);
            }
        }

        for (var embargo : embargos) {
            if (embargo.disembargo != null) {
                embargo.disembargo.completeExceptionally(networkExc);
            }
        }

        try {
            int sizeHint = messageSizeHint() + exceptionSizeHint(exc);
            var message = this.connection.newOutgoingMessage(sizeHint);
            var abort = message.getBody().getAs(RpcProtocol.Message.factory).initAbort();
            RpcException.fromException(exc, abort);
            message.send();
        }
        catch (Throwable abortFailed) {
        }

        var onShutdown = this.connection.shutdown().handle((x, ioExc) -> {
            if (ioExc == null) {
                return CompletableFuture.completedFuture(null);
            }

            // TODO IOException?
            assert !(ioExc instanceof IOException);

            if (ioExc instanceof RpcException) {
                var rpcExc = (RpcException)exc;
                if (rpcExc.getType() == RpcException.Type.DISCONNECTED) {
                    return CompletableFuture.completedFuture(null);
                }
            }
            return CompletableFuture.failedFuture(ioExc);
        });

        this.disconnected = networkExc;
        return onShutdown.thenCompose(x -> CompletableFuture.failedFuture(networkExc));
    }

    final boolean isDisconnected() {
        return this.disconnected != null;
    }

    final boolean isConnected() {
        return !isDisconnected();
    }

    // Run func() before the next IO event.
    private <T> void evalLast(Callable<T> func) {
        this.messageReady = this.messageReady.thenCompose(x -> {
            try {
                func.call();
            }
            catch (java.lang.Exception exc) {
                return CompletableFuture.failedFuture(exc);
            }
            return CompletableFuture.completedFuture(null);
        });
    }

    ClientHook restore() {
        var question = questions.next();
        question.setAwaitingReturn(true);

        // Run the message loop until the boostrap promise is resolved.
        var promise = new CompletableFuture<RpcResponse>();
        var loop = CompletableFuture.anyOf(
                getMessageLoop(), promise).thenCompose(x -> promise);

        int sizeHint = messageSizeHint(RpcProtocol.Bootstrap.factory);
        var message = connection.newOutgoingMessage(sizeHint);
        var builder = message.getBody().initAs(RpcProtocol.Message.factory).initBootstrap();
        builder.setQuestionId(question.getId());
        message.send();
        var pipeline = new RpcPipeline(question, promise);
        return pipeline.getPipelinedCap(new PipelineOp[0]);
    }

    private CompletableFuture<java.lang.Void> doMessageLoop() {
        if (isDisconnected()) {
            return CompletableFuture.failedFuture(this.disconnected);
        }

        return connection.receiveIncomingMessage().thenCompose(message -> {
            try {
                this.handleMessage(message);
            } catch (Exception rpcExc) {
                // either we received an Abort message from peer
                // or internal RpcState is bad.
                return this.disconnect(rpcExc);
            }
            this.cleanupImports();
            this.cleanupQuestions();
            return this.doMessageLoop();

        }).exceptionallyCompose(exc -> this.disconnect(exc));
    }

    private void handleMessage(IncomingRpcMessage message) throws RpcException {
        var reader = message.getBody().getAs(RpcProtocol.Message.factory);
        //System.out.println(reader.which());
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
            case RELEASE:
                handleRelease(reader.getRelease());
                break;
            default:
                if (!isDisconnected()) {
                    // boomin' back atcha
                    var msg = connection.newOutgoingMessage();
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
                            case SENDER_HOSTED:
                                releaseExport(cap.getSenderHosted(), 1);
                                break;
                            case SENDER_PROMISE:
                                releaseExport(cap.getSenderPromise(), 1);
                                break;
                            case THIRD_PARTY_HOSTED:
                                releaseExport(cap.getThirdPartyHosted().getVineId(), 1);
                                break;
                            case NONE:
                                // Should never happen.
                            case RECEIVER_ANSWER:
                            case RECEIVER_HOSTED:
                                // Nothing to do.
                                break;
                        }
                        break;
                    case EXCEPTION:
                        // Nothing to do
                        break;
                }
                break;
            default:
                assert false: "Peer did not implement required RPC message type. " + message.which().name();
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

        final var answerId = bootstrap.getQuestionId();
        var answer = answers.put(answerId);
        if (answer.active) {
            assert false: "questionId is already in use: " + answerId;
            return;
        }
        answer.active = true;

        var capTable = new BuilderCapabilityTable();
        int sizeHint = messageSizeHint(RpcProtocol.Return.factory)
                + RpcProtocol.Payload.factory.structSize().total();
        var response = connection.newOutgoingMessage(sizeHint);

        var ret = response.getBody().getAs(RpcProtocol.Message.factory).initReturn();
        ret.setAnswerId(answerId);

        var payload = ret.initResults();
        var content = payload.getContent().imbue(capTable);
        var cap = this.bootstrapFactory.createFor(connection.getPeerVatId());
        content.setAsCap(cap);
        var caps = capTable.getTable();
        var capHook = caps.length != 0
                ? caps[0]
                : Capability.newNullCap();

        var fds = List.<Integer>of();
        response.setFds(List.of());

        answer.resultExports = writeDescriptors(caps, payload, fds);
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
            if (answer.active) {
                assert false: "questionId is already in use";
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

        var question = questions.find(callReturn.getAnswerId());
        if (question == null) {
            assert false: "Invalid question ID in Return message.";
            return;
        }

        if (!question.isAwaitingReturn()) {
            assert false: "Duplicate Return";
            return;
        }
        question.setAwaitingReturn(false);

        var exportsToRelease = new int[0];
        if (callReturn.getReleaseParamCaps()) {
            exportsToRelease = question.paramExports;
            question.paramExports = new int[0];
        }

        if (callReturn.isTakeFromOtherQuestion()) {
            var answer = this.answers.find(callReturn.getTakeFromOtherQuestion());
            if (answer != null) {
                answer.redirectedResults = null;
            }
            //this.questions.erase(callReturn.getAnswerId());
            this.releaseExports(exportsToRelease);
            return;
        }

        switch (callReturn.which()) {
            case RESULTS:
                if (question.isTailCall) {
                    assert false: "Tail call `Return` must set `resultsSentElsewhere`, not `results`.";
                    break;
                }

                var payload = callReturn.getResults();
                var capTable = receiveCaps(payload.getCapTable(), message.getAttachedFds());
                // TODO question, message unused in RpcResponseImpl
                var response = new RpcResponseImpl(question, message, capTable, payload.getContent());
                question.answer(response);
                break;

            case EXCEPTION:
                if (question.isTailCall) {
                    assert false: "Tail call `Return` must set `resultsSentElsewhere`, not `exception`.";
                    break;
                }
                question.reject(RpcException.toException(callReturn.getException()));
                break;

            case CANCELED:
                assert false : "Return message falsely claims call was canceled.";
                break;

            case RESULTS_SENT_ELSEWHERE:
                if (!question.isTailCall) {
                    assert false: "`Return` had `resultsSentElsewhere` but this was not a tail call.";
                    break;
                }
                // Tail calls are fulfilled with a null pointer.
                question.answer(() -> null);
                break;

            case TAKE_FROM_OTHER_QUESTION:
                var other = callReturn.getTakeFromOtherQuestion();
                var answer = answers.find(other);
                if (answer == null) {
                    assert false: "`Return.takeFromOtherQuestion` had invalid answer ID.";
                    break;
                }
                if (answer.redirectedResults == null) {
                    assert false: "`Return.takeFromOtherQuestion` referenced a call that did not use `sendResultsTo.yourself`.";
                    break;
                }
                question.response = answer.redirectedResults.toCompletableFuture();
                answer.redirectedResults = null;
                break;

            default:
                assert false : "Unknown 'Return' type.";
                break;
        }

        this.releaseExports(exportsToRelease);
    }

    void handleFinish(RpcProtocol.Finish.Reader finish) {
        var answer = answers.find(finish.getQuestionId());
        if (answer == null || !answer.active) {
            assert false: "'Finish' for invalid question ID.";
            return;
        }

        var exportsToRelease = finish.getReleaseResultCaps()
                ? answer.resultExports
                : null;

        answer.resultExports = null;
        answer.pipeline = null;

        // If the call isn't actually done yet, cancel it.  Otherwise, we can go ahead and erase the
        // question from the table.
        var ctx = answer.callContext;
        if (ctx != null) {
            ctx.requestCancel();
        }
        else {
            var questionId = finish.getQuestionId();
            answers.erase(questionId);
        }

        this.releaseExports(exportsToRelease);
    }

    private void handleResolve(IncomingRpcMessage message, RpcProtocol.Resolve.Reader resolve) {
        var imp = this.imports.find(resolve.getPromiseId());
        if (imp == null) {
            return;
        }

        assert imp.importClient == null : "Import already resolved.";

        switch (resolve.which()) {
            case CAP:
                var cap = receiveCap(resolve.getCap(), message.getAttachedFds());
                imp.promise.complete(cap);
                break;
            case EXCEPTION:
                var exc = RpcException.toException(resolve.getException());
                imp.promise.completeExceptionally(exc);
                break;
            default:
                assert false;
                return;
        }
    }

    private void handleRelease(RpcProtocol.Release.Reader release) {
        this.releaseExport(release.getId(), release.getReferenceCount());
    }

    private void handleDisembargo(RpcProtocol.Disembargo.Reader disembargo) {
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

                if (target.getBrand() != this) {
                    assert false: "'Disembargo' of type 'senderLoopback' sent to an object that does not point back to the sender.";
                    return;
                }

                final var embargoId = ctx.getSenderLoopback();
                final var rpcTarget = (RpcClient) target;

                Callable<java.lang.Void> sendDisembargo = () -> {
                    if (isDisconnected()) {
                        return null;
                    }

                    int sizeHint = messageSizeHint(RpcProtocol.Disembargo.factory) + MESSAGE_TARGET_SIZE_HINT;
                    var message = connection.newOutgoingMessage(sizeHint);
                    var builder = message.getBody().initAs(RpcProtocol.Message.factory).initDisembargo();
                    var redirect = rpcTarget.writeTarget(builder.initTarget());
                    // Disembargoes should only be sent to capabilities that were previously the subject of
                    // a `Resolve` message.  But `writeTarget` only ever returns non-null when called on
                    // a PromiseClient.  The code which sends `Resolve` and `Return` should have replaced
                    // any promise with a direct node in order to solve the Tribble 4-way race condition.
                    // See the documentation of Disembargo in rpc.capnp for more.
                    if (redirect == null) {
                        assert false : "'Disembargo' of type 'senderLoopback' sent to an object that does not appear to have been the subject of a previous 'Resolve' message.";
                        return null;
                    }
                    builder.getContext().setReceiverLoopback(embargoId);
                    message.send();
                    return null;
                };
                this.evalLast(sendDisembargo);
                break;

            case RECEIVER_LOOPBACK:
                var embargo = this.embargos.find(ctx.getReceiverLoopback());
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

    private int[] writeDescriptors(ClientHook[] capTable, RpcProtocol.Payload.Builder payload, List<Integer> fds) {
        if (capTable.length == 0) {
            return new int[0];
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
            exports.add(exportId);
        }

        return exports.stream()
                .mapToInt(Integer::intValue)
                .toArray();
    }

    private int writeDescriptor(ClientHook cap, RpcProtocol.CapDescriptor.Builder descriptor, List<Integer> fds) {
        ClientHook inner = cap;
        for (;;) {
            var resolved = inner.getResolved();
            if (resolved != null) {
                inner = resolved;
            }
            else {
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
            // We've already seen and exported this capability.
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
            export.resolveOp = resolveExportedPromise(export.exportId, wrapped);
            descriptor.setSenderPromise(export.exportId);
        }
        else {
            descriptor.setSenderHosted(export.exportId);
        }
        return export.exportId;
    }

    CompletionStage<java.lang.Void> resolveExportedPromise(int exportId, CompletionStage<ClientHook> promise) {
        return promise.thenCompose(resolution -> {
            if (isDisconnected()) {
                return CompletableFuture.completedFuture(null);
            }

            resolution = this.getInnermostClient(resolution);

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
                        return this.resolveExportedPromise(exportId, more);
                    }
                }
            }

            // send a Resolve message
            int sizeHint = messageSizeHint(RpcProtocol.Resolve.factory) + CAP_DESCRIPTOR_SIZE_HINT;
            var message = connection.newOutgoingMessage(sizeHint);
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
            int sizeHint = messageSizeHint(RpcProtocol.Resolve.factory) + exceptionSizeHint(exc);
            var message = connection.newOutgoingMessage(sizeHint);
            var resolve = message.getBody().initAs(RpcProtocol.Message.factory).initResolve();
            resolve.setPromiseId(exportId);
            RpcException.fromException(exc, resolve.initException());
            message.send();

            // TODO disconnect?
        });
    }

    void releaseExports(int[] exports) {
        for (var exportId : exports) {
            this.releaseExport(exportId, 1);
        }
    }

    void releaseExport(int exportId, int refcount) {
        var export = exports.find(exportId);
        if (export == null) {
            assert false: "Cannot release unknown export";
            return;
        }

        if (export.refcount < refcount) {
            assert false: "Over-reducing export refcount. exported=" + export.refcount + ", requested=" + refcount;
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
        ImportClient importClient = null;
        if (imp.importClient != null) {
            importClient = imp.importClient.get();
        }
        if (importClient == null) {
            importClient = new ImportClient(imp, fd);
            imp.importClient = new ImportRef(importId, importClient);
        }
        else {
            importClient.setFdIfMissing(fd);
        }

        imp.addRemoteRef();

        if (!isPromise) {
            imp.appClient = new WeakReference<>(importClient);
            return importClient;
        }

        if (imp.appClient != null) {
            var tmp = imp.appClient.get();
            if (tmp != null) {
                return tmp;
            }
        }

        imp.promise = new CompletableFuture<>();
        var result = new PromiseClient(importClient, imp.promise, importId);
        imp.appClient = new WeakReference<>(result);
        return result;
    }

    ClientHook writeTarget(ClientHook cap, RpcProtocol.MessageTarget.Builder target) {
        // If calls to the given capability should pass over this connection, fill in `target`
        // appropriately for such a call and return nullptr.  Otherwise, return a `ClientHook` to which
        // the call should be forwarded; the caller should then delegate the call to that `ClientHook`.
        //
        // The main case where this ends up returning non-null is if `cap` is a promise that has
        // recently resolved.  The application might have started building a request before the promise
        // resolved, and so the request may have been built on the assumption that it would be sent over
        // this network connection, but then the promise resolved to point somewhere else before the
        // request was sent.  Now the request has to be redirected to the new target instead.

        return cap.getBrand() == this
                ? ((RpcClient)cap).writeTarget(target)
                : cap;
    }

    ClientHook getMessageTarget(RpcProtocol.MessageTarget.Reader target) {
        switch (target.which()) {
            case IMPORTED_CAP:
                var exp = exports.find(target.getImportedCap());
                if (exp != null) {
                    return exp.clientHook;
                }
                else {
                    assert false: "Message target is not a current export ID.";
                    return null;
                }

            case PROMISED_ANSWER:
                var promisedAnswer = target.getPromisedAnswer();
                var questionId = promisedAnswer.getQuestionId();
                var base = answers.put(questionId);
                if (!base.active) {
                    assert false: "PromisedAnswer.questionId is not a current question.";
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

    class RpcResponseImpl implements RpcResponse {
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

        int[] send() {
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

    private static final class LocallyRedirectedRpcResponse
            implements RpcServerResponse,
                       RpcResponse {

        private final MessageBuilder message = new MessageBuilder();

        @Override
        public AnyPointer.Builder getResultsBuilder() {
            return this.message.getRoot(AnyPointer.factory);
        }

        @Override
        public AnyPointer.Reader getResults() {
            return this.getResultsBuilder().asReader();
        }
    }

    private final class RpcCallContext implements CallContextHook {

        private final int answerId;
        private final long interfaceId;
        private final short methodId;

        // request
        private IncomingRpcMessage request;
        private final AnyPointer.Reader params;

        // response
        private RpcServerResponse response;
        private RpcProtocol.Return.Builder returnMessage;
        private boolean redirectResults = false;
        private boolean responseSent = false;
        private CompletableFuture<PipelineHook> tailCallPipelineFuture;

        private boolean cancelRequested = false;
        private boolean cancelAllowed = false;

        private final CompletableFuture<java.lang.Void> whenCancelled;

        RpcCallContext(int answerId, IncomingRpcMessage request, List<ClientHook> capTable,
                       AnyPointer.Reader params, boolean redirectResults,
                       CompletableFuture<java.lang.Void> whenCancelled,
                       long interfaceId, short methodId) {
            this.answerId = answerId;
            this.interfaceId = interfaceId;
            this.methodId = methodId;
            this.request = request;
            this.params = params.imbue(new ReaderCapabilityTable(capTable));
            this.redirectResults = redirectResults;
            this.whenCancelled = whenCancelled;
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
        public AnyPointer.Builder getResults(int sizeHint) {
            if (this.response == null) {

                if (this.redirectResults || isDisconnected()) {
                    this.response = new LocallyRedirectedRpcResponse();
                }
                else {
                    sizeHint += messageSizeHint(RpcProtocol.Return.factory)
                             + RpcProtocol.Payload.factory.structSize().total();
                    var message = connection.newOutgoingMessage(sizeHint);
                    this.returnMessage = message.getBody().initAs(RpcProtocol.Message.factory).initReturn();
                    this.response = new RpcServerResponseImpl(message, returnMessage.getResults());
                }
            }

            return this.response.getResultsBuilder();
        }

        @Override
        public CompletableFuture<java.lang.Void> tailCall(RequestHook request) {
            var result = this.directTailCall(request);
            if (this.tailCallPipelineFuture != null) {
                this.tailCallPipelineFuture.complete(result.pipeline);
            }
            return result.promise.toCompletableFuture().copy();
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

        private RpcResponse consumeRedirectedResponse() {
            assert this.redirectResults;

            if (this.response == null) {
                getResults(); // force initialization of response
            }

            return ((LocallyRedirectedRpcResponse) this.response);
        }

        private void sendReturn() {
            assert !redirectResults;

            if (!this.cancelRequested && isDisconnected()) {
                assert false : "Cancellation should have been requested on disconnect.";
                return;
            }

            if (this.response == null) {
                getResults(); // force initialization
            }

            this.returnMessage.setAnswerId(this.answerId);
            this.returnMessage.setReleaseParamCaps(false);

            var exports = new int[0];
            try {
                exports = ((RpcServerResponseImpl) response).send();
            } catch (Throwable exc) {
                this.responseSent = false;
                sendErrorReturn(exc);
            }

            // If no caps in the results, the pipeline is irrelevant.
            boolean shouldFreePipeline = exports.length == 0;
            cleanupAnswerTable(exports, shouldFreePipeline);
        }

        private void sendErrorReturn(Throwable exc) {
            assert !redirectResults;

            if (!isFirstResponder()) {
                return;
            }

            if (isConnected()) {
                var message = connection.newOutgoingMessage();
                var builder = message.getBody().initAs(RpcProtocol.Message.factory).initReturn();
                builder.setAnswerId(this.answerId);
                builder.setReleaseParamCaps(false);
                RpcException.fromException(exc, builder.initException());
                message.send();
            }

            cleanupAnswerTable(new int[0], false);
        }

        private boolean isFirstResponder() {
            if (this.responseSent) {
                return false;
            }
            this.responseSent = true;
            return true;
        }

        private void cleanupAnswerTable(int[] resultExports, boolean shouldFreePipeline) {
            if (this.cancelRequested) {
                assert resultExports.length == 0;
                answers.erase(this.answerId);
                return;
            }
            else {
                var answer = answers.find(answerId);
                answer.callContext = null;
                answer.resultExports = resultExports;

                if (shouldFreePipeline) {
                    assert resultExports.length == 0;
                    answer.pipeline = null;
                }
            }
        }

        public void requestCancel() {
            // Hints that the caller wishes to cancel this call.  At the next time when cancellation is
            // deemed safe, the RpcCallContext shall send a canceled Return -- or if it never becomes
            // safe, the RpcCallContext will send a normal return when the call completes.  Either way
            // the RpcCallContext is now responsible for cleaning up the entry in the answer table, since
            // a Finish message was already received.

            boolean previouslyAllowedButNotRequested = (this.cancelAllowed && !this.cancelRequested);
            this.cancelRequested = true;

            if (previouslyAllowedButNotRequested) {
                // We just set CANCEL_REQUESTED, and CANCEL_ALLOWED was already set previously.  Initiate
                // the cancellation.
                this.whenCancelled.complete(null);
            }
            // TODO do we care about cancelRequested if further completions are effectively ignored?
        }
    }

    enum PipelineState {
        WAITING, RESOLVED, BROKEN
    }

    private class RpcPipeline implements PipelineHook {

        private final Question question;
        private PipelineState state = PipelineState.WAITING;
        private RpcResponse resolved;
        private Throwable broken;

        final HashMap<List<PipelineOp>, ClientHook> clientMap = new HashMap<>();
        final CompletableFuture<RpcResponse> redirectLater;
        final CompletableFuture<java.lang.Void> resolveSelf;

        RpcPipeline(Question question,
                    CompletableFuture<RpcResponse> redirectLater) {
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
            var key = new ArrayList<>(Arrays.asList(ops));
            var hook = this.clientMap.computeIfAbsent(key, k -> {
                switch (state) {
                    case WAITING: {
                        var pipelineClient = new PipelineClient(this.question, ops);
                        if (this.redirectLater == null) {
                            // This pipeline will never get redirected, so just return the PipelineClient.
                            return pipelineClient;
                        }

                        var resolutionPromise = this.redirectLater.thenApply(
                                response -> response.getResults().getPipelinedCap(ops));
                        return new PromiseClient(pipelineClient, resolutionPromise, null);
                    }

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
        public Request<AnyPointer.Builder> newCall(long interfaceId, short methodId) {
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
            return context.directTailCall(request.getHook());
        }

        @Override
        public final Object getBrand() {
            return RpcState.this;
        }

        private Request<AnyPointer.Builder> newCallNoIntercept(long interfaceId, short methodId) {
            if (isDisconnected()) {
                return Request.newBrokenRequest(AnyPointer.factory, disconnected);
            }

            var request = new RpcRequest(this);
            var callBuilder = request.getCall();
            callBuilder.setInterfaceId(interfaceId);
            callBuilder.setMethodId(methodId);
            var root = request.getRoot();
            return new AnyPointer.Request(root, request);
        }
    }

    class RpcRequest implements RequestHook {

        private final RpcClient target;
        private final OutgoingRpcMessage message;
        private final BuilderCapabilityTable capTable = new BuilderCapabilityTable();
        private final RpcProtocol.Call.Builder callBuilder;
        private final AnyPointer.Builder paramsBuilder;

        RpcRequest(RpcClient target) {
            this(target, 0);
        }

        RpcRequest(RpcClient target, int sizeHint) {
            this.target = target;
            sizeHint += RpcProtocol.Call.factory.structSize().total()
                    + RpcProtocol.Payload.factory.structSize().total()
                    + MESSAGE_TARGET_SIZE_HINT;
            this.message = connection.newOutgoingMessage(sizeHint);
            this.callBuilder = message.getBody().getAs(RpcProtocol.Message.factory).initCall();
            this.paramsBuilder = callBuilder.getParams().getContent().imbue(this.capTable);
        }

        private AnyPointer.Builder getRoot() {
            return this.paramsBuilder;
        }

        private RpcProtocol.Call.Builder getCall() {
            return this.callBuilder;
        }

        @Override
        public RemotePromise<AnyPointer.Reader> send() {
            if (isDisconnected()) {
                return new RemotePromise<>(CompletableFuture.failedFuture(disconnected), null);
            }

            var redirect = this.target.writeTarget(this.callBuilder.getTarget());
            if (redirect != null) {
                var redirected = redirect.newCall(
                        this.callBuilder.getInterfaceId(), this.callBuilder.getMethodId());
                //replacement.params = paramsBuilder;
                var replacement = new AnyPointer.Request(paramsBuilder, redirected.getHook());
                return replacement.send();
            }

            final var question = sendInternal(false);

            // The pipeline must get notified of resolution before the app does to maintain ordering.
            var pipeline = new RpcPipeline(question, question.response);

            var appPromise = question.response.thenApply(
                    hook -> new Response<>(hook.getResults(), hook));

            // complete when either the message loop completes (exceptionally) or
            // the appPromise is fulfilled
            var loop = CompletableFuture.anyOf(
                    getMessageLoop(), appPromise).thenCompose(x -> appPromise);

            return new RemotePromise<>(loop, new AnyPointer.Pipeline(pipeline));
        }

        @Override
        public CompletionStage<?> sendStreaming() {
            // TODO falling back to regular send for now...
            return send();
        }

        Question sendInternal(boolean isTailCall) {
            // TODO refactor
            var fds = List.<Integer>of();
            var exports = writeDescriptors(capTable.getTable(), callBuilder.getParams(), fds);
            message.setFds(fds);
            var question = questions.next();
            question.setAwaitingReturn(true);
            question.isTailCall = isTailCall;
            question.paramExports = exports;

            callBuilder.setQuestionId(question.getId());
            if (isTailCall) {
                callBuilder.getSendResultsTo().getYourself();
            }
            try {
                message.send();
            } catch (Exception exc) {
                question.setAwaitingReturn(false);
                question.setSkipFinish(true);
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

    private class ImportRef extends WeakReference<ImportClient> {

        final int importId;

        ImportRef(int importId, ImportClient hook) {
            super(hook, importRefs);
            this.importId = importId;
        }
    }

    private class ImportClient extends RpcClient {

        final Import imp;
        Integer fd;

        ImportClient(Import imp, Integer fd) {
            this.imp = imp;
            this.fd = fd;
        }

        void setFdIfMissing(Integer fd) {
            if (this.fd == null) {
                this.fd = fd;
            }
        }

        @Override
        public Integer writeDescriptor(RpcProtocol.CapDescriptor.Builder descriptor, List<Integer> fds) {
            descriptor.setReceiverHosted(this.imp.importId);
            return null;
        }

        @Override
        public ClientHook writeTarget(RpcProtocol.MessageTarget.Builder target) {
            target.setImportedCap(this.imp.importId);
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

    private void cleanupImports() {
        while (true) {
            var ref = (ImportRef) this.importRefs.poll();
            if (ref == null) {
                return;
            }
            var imp = this.imports.find(ref.importId);
            assert imp != null;
            if (imp != null) {
                imp.dispose();
            }
        }
    }

    private void cleanupQuestions() {
        while (true) {
            var ref = (QuestionRef)this.questionRefs.poll();
            if (ref == null) {
                break;
            }
            ref.dispose();
        }
    }

    enum ResolutionType {
        UNRESOLVED,
        REMOTE,
        REFLECTED,
        MERGED,
        BROKEN
    }

    private class PromiseClient extends RpcClient {

        private final ClientHook cap;
        private final Integer importId;
        private final CompletableFuture<ClientHook> promise;
        private boolean receivedCall = false;
        private ResolutionType resolutionType = ResolutionType.UNRESOLVED;

        PromiseClient(RpcClient initial,
                      CompletableFuture<ClientHook> eventual,
                      Integer importId) {
            this.cap = initial;
            this.importId = importId;
            this.promise = eventual.thenApply(resolution -> resolve(resolution));
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
                if (replacementBrand == NULL_CAPABILITY_BRAND
                        || replacementBrand == BROKEN_CAPABILITY_BRAND) {
                    resolutionType = ResolutionType.BROKEN;
                }
                else {
                    resolutionType = ResolutionType.REFLECTED;
                }
            }

            assert isResolved();

            // TODO Flow control

            if (resolutionType == ResolutionType.REFLECTED && receivedCall && !isDisconnected()) {
                int sizeHint = messageSizeHint(RpcProtocol.Disembargo.factory);
                var message = connection.newOutgoingMessage(sizeHint);
                var disembargo = message.getBody().initAs(RpcProtocol.Message.factory).initDisembargo();
                var redirect = RpcState.this.writeTarget(cap, disembargo.initTarget());
                assert redirect == null;

                var embargo = embargos.next();
                disembargo.getContext().setSenderLoopback(embargo.id);

                final ClientHook finalReplacement = replacement;
                var embargoPromise = embargo.disembargo.thenApply(x -> finalReplacement);
                replacement = Capability.newLocalPromiseClient(embargoPromise);
                message.send();
            }

            return replacement;
        }

        @Override
        public Integer writeDescriptor(RpcProtocol.CapDescriptor.Builder target, List<Integer> fds) {
            this.receivedCall = true;
            return RpcState.this.writeDescriptor(cap, target, fds);
        }

        @Override
        public ClientHook writeTarget(RpcProtocol.MessageTarget.Builder target) {
            this.receivedCall = true;
            return RpcState.this.writeTarget(this.cap, target);
        }

        @Override
        public ClientHook getInnermostClient() {
            this.receivedCall = true;
            return RpcState.this.getInnermostClient(cap);
        }

        @Override
        public VoidPromiseAndPipeline call(long interfaceId, short methodId, CallContextHook context) {
            return null;
        }
    }

    private class PipelineClient extends RpcClient {

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
            promisedAnswer.setQuestionId(question.getId());
            PipelineOp.FromPipelineOps(ops, promisedAnswer);
            return null;
        }

        @Override
        public ClientHook writeTarget(RpcProtocol.MessageTarget.Builder target) {
            var builder = target.initPromisedAnswer();
            builder.setQuestionId(question.getId());
            PipelineOp.FromPipelineOps(ops, builder);
            return null;
        }
    }
}
