package org.capnproto;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.nio.channels.ClosedChannelException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.logging.*;

final class RpcState<VatId> {

    private static final Logger LOGGER = Logger.getLogger(RpcState.class.getName());

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

    static class DisconnectInfo {

        final CompletableFuture<java.lang.Void> shutdownPromise;
        // Task which is working on sending an abort message and cleanly ending the connection.

        DisconnectInfo(CompletableFuture<java.lang.Void> shutdownPromise) {
            this.shutdownPromise = shutdownPromise;
        }
   }

    private final class Question {

        final int id;
        boolean skipFinish;
        boolean isAwaitingReturn;
        int[] paramExports = new int[0];
        boolean isTailCall = false;
        QuestionRef selfRef;
        private final WeakReference<QuestionRef> disposer;

        Question(int id) {
            this.id = id;
            this.selfRef = new QuestionRef(this.id);
            this.disposer = new QuestionDisposer(this.selfRef);
        }

        void finish() {
            if (isConnected() && !this.skipFinish) {
                var sizeHint = messageSizeHint(RpcProtocol.Finish.factory);
                var message = connection.newOutgoingMessage(sizeHint);
                var builder = message.getBody().getAs(RpcProtocol.Message.factory).initFinish();
                builder.setQuestionId(this.id);
                builder.setReleaseResultCaps(this.isAwaitingReturn);
                LOGGER.fine(() -> RpcState.this.toString() + ": > FINISH question=" + this.id);
                message.send();
            }
            this.skipFinish = true;

            // Check if the question has returned and, if so, remove it from the table.
            // Remove question ID from the table.  Must do this *after* sending `Finish` to ensure that
            // the ID is not re-allocated before the `Finish` message can be sent.
            if (!this.isAwaitingReturn) {
                questions.erase(this.id, this);
            }
        }
    }


    /**
     * A reference to an entry on the question table.
     */
    private final class QuestionRef {

        private final int questionId;
        CompletableFuture<RpcResponse> response = new CompletableFuture<>();

        QuestionRef(int questionId) {
            this.questionId = questionId;
        }

        void fulfill(Throwable exc) {
            this.response.completeExceptionally(exc);
            this.finish();
        }

        void fulfill(RpcResponse response) {
            this.response.complete(response);
            this.finish();
        }

        private void finish() {
            // We no longer need access to the questionRef in order to complete it.
            // Dropping the selfRef releases the question for disposal once all other
            // references are gone.
            var question = questions.find(this.questionId);
            if (question != null) {
                question.selfRef = null;
            }
        }
    }

    private final class QuestionDisposer extends WeakReference<QuestionRef> {
        private final int questionId;

        QuestionDisposer(QuestionRef questionRef) {
            super(questionRef, questionRefs);
            this.questionId = questionRef.questionId;
        }

        void dispose() {
            var question = questions.find(this.questionId);
            if (question != null) {
                question.finish();
            }
        }
    }

    final class Answer {
        final int answerId;
        boolean active = false;
        PipelineHook pipeline;
        CompletableFuture<RpcResponse> redirectedResults;
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
        ImportDisposer disposer;
        FileDescriptor fd;
        int remoteRefCount;
        RpcClient appClient;
        CompletableFuture<ClientHook> promise;
        // If non-null, the import is a promise.

        Import(int importId) {
            this.importId = importId;
        }

        void addRemoteRef() {
            this.remoteRefCount++;
        }

        void setFdIfMissing(FileDescriptor fd) {
            if (this.fd == null) {
                this.fd = fd;
            }
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
                LOGGER.fine(() -> this.toString() + ": > RELEASE import=" + importId);
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

    private final ExportTable<Question> questions = new ExportTable<>() {
        @Override
        Question newExportable(int id) {
            return new Question(id);
        }
    };

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
    private final BootstrapFactory<? super VatId> bootstrapFactory;
    private final VatNetwork.Connection<VatId> connection;
    private final CompletableFuture<? super DisconnectInfo> disconnectFulfiller;
    private Throwable disconnected = null;
    private final CompletableFuture<java.lang.Void> messageLoop = new CompletableFuture<>();
    // completes when the message loop exits
    private final ReferenceQueue<QuestionRef> questionRefs = new ReferenceQueue<>();
    private final ReferenceQueue<ImportRef> importRefs = new ReferenceQueue<>();
    private final Queue<Callable<java.lang.Void>> lastEvals = new ArrayDeque<>();

    RpcState(BootstrapFactory<? super VatId> bootstrapFactory,
             VatNetwork.Connection<VatId> connection,
             CompletableFuture<? super DisconnectInfo> disconnectFulfiller) {
        this.bootstrapFactory = bootstrapFactory;
        this.connection = connection;
        this.disconnectFulfiller = disconnectFulfiller;
    }

    @Override
    public String toString() {
        return super.toString() + ": " + this.connection.toString();
    }

    CompletableFuture<java.lang.Void> onDisconnection() {
        return this.messageLoop;
    }

    void disconnect(Throwable exc) {
        if (isDisconnected()) {
            // Already disconnected.
            return;
        }

        var networkExc = RpcException.disconnected(exc.getMessage());

        // All current questions complete with exceptions.
        for (var question: questions) {
            var questionRef = question.selfRef;
            if (questionRef != null) {
                questionRef.fulfill(networkExc);
            }
        }

        List<PipelineHook> pipelinesToRelease = new ArrayList<>();
        List<ClientHook> clientsToRelease = new ArrayList<>();
        List<CompletionStage<RpcResponse>> tailCallsToRelease = new ArrayList<>();
        List<CompletionStage<java.lang.Void>> resolveOpsToRelease = new ArrayList<>();

        for (var answer: answers) {
            if (answer.redirectedResults != null) {
                tailCallsToRelease.add(answer.redirectedResults);
                answer.redirectedResults = null;
            }

            if (answer.pipeline != null) {
                pipelinesToRelease.add(answer.pipeline);
                answer.pipeline = null;
            }

            if (answer.callContext != null) {
                answer.callContext.requestCancel();
            }
        }

        for (var export: exports) {
            clientsToRelease.add(export.clientHook);
            resolveOpsToRelease.add(export.resolveOp);
            export.clientHook = null;
            export.resolveOp = null;
            export.refcount = 0;
        }

        for (var imp: imports) {
            if (imp.promise != null) {
                imp.promise.completeExceptionally(networkExc);
            }
        }

        for (var embargo: embargos) {
            embargo.disembargo.completeExceptionally(networkExc);
        }

        // Send an abort message, but ignore failure.
        try {
            int sizeHint = messageSizeHint() + exceptionSizeHint(exc);
            var message = this.connection.newOutgoingMessage(sizeHint);
            var abort = message.getBody().getAs(RpcProtocol.Message.factory).initAbort();
            FromException(exc, abort);
            LOGGER.log(Level.FINE, this.toString() + ": > ABORT", exc);
            message.send();
        }
        catch (Exception ignored) {
        }

        var shutdownPromise = this.connection.shutdown()
                .exceptionallyCompose(ioExc -> {

            assert !(ioExc instanceof IOException);

            if (ioExc instanceof RpcException) {
                var rpcExc = (RpcException)exc;

                // Don't report disconnects as an error
                if (rpcExc.getType() == RpcException.Type.DISCONNECTED) {
                    return CompletableFuture.completedFuture(null);
                }
            }
            else if (ioExc instanceof CompletionException) {
                var compExc = (CompletionException)ioExc;
                if (compExc.getCause() instanceof ClosedChannelException) {
                    return CompletableFuture.completedFuture(null);
                }
            }

            return CompletableFuture.failedFuture(ioExc);
        });

        this.disconnected = networkExc;
        this.disconnectFulfiller.complete(new DisconnectInfo(shutdownPromise));

        for (var pipeline: pipelinesToRelease) {
            pipeline.cancel(networkExc);
        }
    }

    final boolean isDisconnected() {
        return this.disconnected != null;
    }

    final boolean isConnected() {
        return !isDisconnected();
    }

    // Run func() before the next IO event.
    private void evalLast(Callable<java.lang.Void> func) {
        this.lastEvals.add(func);
    }

    ClientHook restore() {
        var question = questions.next();
        question.isAwaitingReturn = true;
        var questionRef = question.selfRef;
        var pipeline = new RpcPipeline(questionRef, questionRef.response);

        int sizeHint = messageSizeHint(RpcProtocol.Bootstrap.factory);
        var message = connection.newOutgoingMessage(sizeHint);
        var builder = message.getBody().initAs(RpcProtocol.Message.factory).initBootstrap();
        builder.setQuestionId(question.id);
        LOGGER.fine(() -> this.toString() + ": > BOOTSTRAP question=" + question.id);
        message.send();

        return pipeline.getPipelinedCap(new short[0]);
    }

    /**
     * Returns a CompletableFuture that, when complete, has processed one message.
     */
    public CompletableFuture<java.lang.Void> pollOnce() {
        if (isDisconnected()) {
            this.messageLoop.completeExceptionally(this.disconnected);
            return CompletableFuture.failedFuture(this.disconnected);
        }

        return this.connection.receiveIncomingMessage()
                    .thenAccept(message -> {
                        if (message == null) {
                            this.disconnect(RpcException.disconnected("Peer disconnected"));
                            this.messageLoop.complete(null);
                            return;
                        }
                        try {
                            this.handleMessage(message);
                            while (!this.lastEvals.isEmpty()) {
                                this.lastEvals.remove().call();
                            }
                        }
                        catch (Throwable rpcExc) {
                            // either we received an Abort message from peer
                            // or internal RpcState is bad.
                            this.disconnect(rpcExc);
                        }
                    });
    }

    public void runMessageLoop() {
        this.pollOnce().thenRun(this::runMessageLoop).exceptionally(exc -> {
            LOGGER.log(Level.FINE, "Event loop exited", exc);
            return null;
        });
    }

    private void handleMessage(IncomingRpcMessage message) throws RpcException {
        var reader = message.getBody().getAs(RpcProtocol.Message.factory);
        switch (reader.which()) {
            case UNIMPLEMENTED -> handleUnimplemented(reader.getUnimplemented());
            case ABORT -> handleAbort(reader.getAbort());
            case BOOTSTRAP -> handleBootstrap(reader.getBootstrap());
            case CALL -> handleCall(message, reader.getCall());
            case RETURN -> handleReturn(message, reader.getReturn());
            case FINISH -> handleFinish(reader.getFinish());
            case RESOLVE -> handleResolve(message, reader.getResolve());
            case DISEMBARGO -> handleDisembargo(reader.getDisembargo());
            case RELEASE -> handleRelease(reader.getRelease());
            default -> {
                LOGGER.warning(() -> this.toString() + ": < Unhandled RPC message: " + reader.which().toString());
                if (!isDisconnected()) {
                    // boomin' back atcha
                    var msg = connection.newOutgoingMessage();
                    msg.getBody().initAs(RpcProtocol.Message.factory).setUnimplemented(reader);
                    LOGGER.fine(() -> this.toString() + ": > UNIMPLEMENTED");
                    msg.send();
                }
            }
        }

        this.cleanupImports();
        this.cleanupQuestions();
    }

    void handleUnimplemented(RpcProtocol.Message.Reader message) {
        LOGGER.fine(() -> this.toString() + ": < UNIMPLEMENTED");

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
        var exc = ToException(abort);
        LOGGER.log(Level.FINE, this.toString() + ": < ABORT ", exc);
        throw exc;
    }

    void handleBootstrap(RpcProtocol.Bootstrap.Reader bootstrap) {
        LOGGER.fine(() -> this.toString() + ": < BOOTSTRAP question=" + bootstrap.getQuestionId());
        if (isDisconnected()) {
            return;
        }

        var answerId = bootstrap.getQuestionId();
        var answer = answers.put(answerId);
        if (answer.active) {
            assert false: "bootstrap questionId is already in use: " + answerId;
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
        content.setAs(Capability.factory, cap);
        var caps = capTable.getTable();
        var capHook = caps.length != 0
                ? caps[0]
                : Capability.newNullCap();

        var fds = List.<FileDescriptor>of();
        response.setFds(List.of());

        answer.resultExports = writeDescriptors(caps, payload, fds);
        assert answer.pipeline == null;
        answer.pipeline = ops -> ops.length == 0
                ? capHook
                : Capability.newBrokenCap("Invalid pipeline transform.");

        LOGGER.fine(() -> this.toString() + ": > RETURN answer=" + answerId);
        response.send();

        assert answer.active;
        assert answer.resultExports != null;
        assert answer.pipeline != null;
    }

    void handleCall(IncomingRpcMessage message, RpcProtocol.Call.Reader call) {
        LOGGER.fine(() -> this.toString() + ": < CALL question=" + call.getQuestionId());

        var cap = getMessageTarget(call.getTarget());
        if (cap == null) {
            return;
        }

        boolean redirectResults;
        switch (call.getSendResultsTo().which()) {
            case CALLER -> redirectResults = false;
            case YOURSELF -> redirectResults = true;
            default -> {
                assert false : "Unsupported 'Call.sendResultsTo'.";
                return;
            }
        }

        var payload = call.getParams();
        var capTableArray = receiveCaps(payload.getCapTable(), message.getAttachedFds());
        var answerId = call.getQuestionId();
        var context = new RpcCallContext(
                answerId, message, capTableArray,
                payload.getContent(), redirectResults,
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

        // Things may have changed -- in particular if startCall() immediately called
        // context->directTailCall().

        {
            var answer = answers.find(answerId);
            assert answer != null;
            assert answer.pipeline == null;
            answer.pipeline = pap.pipeline;

            var callReady = pap.promise;

            if (redirectResults) {
                answer.redirectedResults = callReady.thenApply(void_ ->
                        context.consumeRedirectedResponse());
            }
            else {
                callReady.whenComplete((void_, exc) -> {
                    if (exc == null) {
                        context.sendReturn();
                    }
                    else {
                        context.sendErrorReturn(exc);
                    }
                });
            }

            context.whenCancelled().thenRun(() -> {
                callReady.cancel(false);
            });
        }
    }

    private ClientHook.VoidPromiseAndPipeline startCall(long interfaceId, short methodId, ClientHook cap, RpcCallContext context) {
        // TODO gateways...?
        return cap.call(interfaceId, methodId, context);
    }

    void handleReturn(IncomingRpcMessage message, RpcProtocol.Return.Reader callReturn) {
        LOGGER.fine(() -> this.toString() + ": < RETURN answer=" + callReturn.getAnswerId());

        var question = questions.find(callReturn.getAnswerId());
        if (question == null) {
            assert false: "Invalid question ID in Return message.";
            return;
        }

        if (!question.isAwaitingReturn) {
            assert false: "Duplicate Return";
            return;
        }
        question.isAwaitingReturn = false;

        int[] exportsToRelease = null;
        if (callReturn.getReleaseParamCaps()) {
            exportsToRelease = question.paramExports;
            question.paramExports = null;
        }

        var questionRef = question.selfRef;
        if (questionRef == null) {
            if (callReturn.isTakeFromOtherQuestion()) {
                var answer = this.answers.find(callReturn.getTakeFromOtherQuestion());
                if (answer != null) {
                    answer.redirectedResults = null;
                }
            }

            // Looks like this question was canceled earlier, so `Finish` was already sent, with
            // `releaseResultCaps` set true so that we don't have to release them here.  We can go
            // ahead and delete it from the table.
            // TODO Should we do this?
            questions.erase(callReturn.getAnswerId(), question);

            if (exportsToRelease != null) {
                this.releaseExports(exportsToRelease);
            }
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
                var response = new RpcResponseImpl(questionRef, message, capTable, payload.getContent());
                questionRef.fulfill(response);
                break;

            case EXCEPTION:
                if (question.isTailCall) {
                    assert false: "Tail call `Return` must set `resultsSentElsewhere`, not `exception`.";
                    break;
                }
                questionRef.fulfill(ToException(callReturn.getException()));
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
                questionRef.fulfill(() -> null);
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
                questionRef.response = answer.redirectedResults;
                answer.redirectedResults = null;
                break;

            default:
                assert false : "Unknown 'Return' type.";
                break;
        }

        if (exportsToRelease != null) {
            this.releaseExports(exportsToRelease);
        }
    }

    void handleFinish(RpcProtocol.Finish.Reader finish) {
        LOGGER.fine(() -> this.toString() + ": < FINISH question=" + finish.getQuestionId());

        var answer = answers.find(finish.getQuestionId());
        if (answer == null || !answer.active) {
            assert false: "'Finish' for invalid question ID.";
            return;
        }

        var exportsToRelease = finish.getReleaseResultCaps()
                ? answer.resultExports
                : null;

        answer.resultExports = null;

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

        if (exportsToRelease != null) {
            this.releaseExports(exportsToRelease);
        }
    }

    private void handleResolve(IncomingRpcMessage message, RpcProtocol.Resolve.Reader resolve) {
        LOGGER.fine(() -> this.toString() + ": < RESOLVE promise=" + resolve.getPromiseId());

        var importId = resolve.getPromiseId();
        var imp = this.imports.find(importId);
        if (imp == null) {
            return;
        }

        if (imp.promise == null) {
            assert imp.disposer != null: "Import already resolved.";
            // It appears this is a valid entry on the import table, but was not expected to be a
            // promise.
            return;
        }

        // This import is an unfulfilled promise.
        switch (resolve.which()) {
            case CAP -> {
                var cap = receiveCap(resolve.getCap(), message.getAttachedFds());
                imp.promise.complete(cap);
            }
            case EXCEPTION -> {
                var exc = ToException(resolve.getException());
                imp.promise.completeExceptionally(exc);
            }
            default -> {
                assert false : "Unknown 'Resolve' type.";
            }
        }
    }

    private void handleRelease(RpcProtocol.Release.Reader release) {
        LOGGER.fine(() -> this.toString() + ": < RELEASE promise=" + release.getId());
        this.releaseExport(release.getId(), release.getReferenceCount());
    }

    private void handleDisembargo(RpcProtocol.Disembargo.Reader disembargo) {
        LOGGER.fine(() -> this.toString() + ": < DISEMBARGO");

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

                var embargoId = ctx.getSenderLoopback();
                var rpcTarget = (RpcClient) target;

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
                    if (redirect != null) {
                        assert false : "'Disembargo' of type 'senderLoopback' sent to an object that does not appear to have been the subject of a previous 'Resolve' message.";
                        return null;
                    }
                    builder.getContext().setReceiverLoopback(embargoId);
                    LOGGER.fine(() -> this.toString() + ": > DISEMBARGO");
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
                embargo.disembargo.complete(null);
                embargos.erase(ctx.getReceiverLoopback(), embargo);
                break;

            default:
                assert false: "Unimplemented Disembargo type. " + ctx.which();
                break;
        }
    }

    private int[] writeDescriptors(ClientHook[] capTable, RpcProtocol.Payload.Builder payload, List<FileDescriptor> fds) {
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
            if (exportId != null) {
                exports.add(exportId);
            }
        }

        return exports.stream()
                .mapToInt(Integer::intValue)
                .toArray();
    }

    private Integer writeDescriptor(ClientHook cap, RpcProtocol.CapDescriptor.Builder descriptor, List<FileDescriptor> fds) {
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
            export.resolveOp = this.resolveExportedPromise(export.exportId, wrapped);
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
            assert exp != null;
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
            var fds = List.<FileDescriptor>of();
            writeDescriptor(exp.clientHook, resolve.initCap(), fds);
            message.setFds(fds);
            LOGGER.fine(() -> this.toString() + ": > RESOLVE export=" + exportId);
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
            FromException(exc, resolve.initException());
            LOGGER.fine(() -> this.toString() + ": > RESOLVE FAILED export=" + exportId + " msg=" + exc.getMessage());
            message.send();
        });
    }

    void releaseExports(int[] exports) {
        for (var exportId: exports) {
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

    private List<ClientHook> receiveCaps(StructList.Reader<RpcProtocol.CapDescriptor.Reader> capTable, List<FileDescriptor> fds) {
        var result = new ArrayList<ClientHook>();
        for (var cap: capTable) {
            result.add(receiveCap(cap, fds));
        }
        return result;
    }

    private ClientHook receiveCap(RpcProtocol.CapDescriptor.Reader descriptor, List<FileDescriptor> fds) {
        FileDescriptor fd = null;
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

            case RECEIVER_HOSTED: {
                var exp = exports.find(descriptor.getReceiverHosted());
                if (exp == null) {
                    return Capability.newBrokenCap("invalid 'receiverHosted' export ID");
                }
                else if (exp.clientHook.getBrand() == this) {
                    return new TribbleRaceBlocker(exp.clientHook);
                }
                else {
                    return exp.clientHook;
                }
            }

            case RECEIVER_ANSWER: {
                var promisedAnswer = descriptor.getReceiverAnswer();
                var answer = answers.find(promisedAnswer.getQuestionId());
                var ops = ToPipelineOps(promisedAnswer);

                if (answer == null || !answer.active || answer.pipeline == null || ops == null) {
                    return Capability.newBrokenCap("invalid 'receiverAnswer'");
                }

                var result = answer.pipeline.getPipelinedCap(ops);
                if (result == null) {
                    return Capability.newBrokenCap("Unrecognised pipeline ops");
                } else if (result.getBrand() == this) {
                    return new TribbleRaceBlocker(result);
                } else {
                    return result;
                }
            }

            case THIRD_PARTY_HOSTED:
                return Capability.newBrokenCap("Third party caps not supported");

            default:
                return Capability.newBrokenCap("unknown CapDescriptor type");
        }
    }

    private ClientHook importCap(int importId, boolean isPromise, FileDescriptor fd) {
        // Receive a new import.
        var imp = imports.put(importId);

        ImportClient importClient;

        // new import
        if (imp.disposer == null) {
            var importRef = new ImportRef(importId);
            imp.disposer = new ImportDisposer(importRef);
            importClient = new ImportClient(importRef);
        }
        else {
            var importRef = imp.disposer.get();
            if (importRef == null) {
                // Import still exists, but has no references. Resurrect it.
                importRef = new ImportRef(importId);
                imp.disposer = new ImportDisposer(importRef);
                importClient = new ImportClient(importRef);
            }
            else {
                importClient = new ImportClient(importRef);
            }
        }


        imp.setFdIfMissing(fd);
        imp.addRemoteRef();

        if (!isPromise) {
            return importClient;
        }

        if (imp.appClient != null) {
            return imp.appClient;
        }

        imp.promise = new CompletableFuture<>();
        var result = new PromiseClient(importClient, imp.promise, importClient.importRef);
        imp.appClient = result;
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
            case IMPORTED_CAP -> {
                var exp = exports.find(target.getImportedCap());
                if (exp != null) {
                    return exp.clientHook;
                }
                else {
                    assert false: "Message target is not a current export ID.";
                    return null;
                }
            }
            case PROMISED_ANSWER -> {
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
                var ops = ToPipelineOps(promisedAnswer);
                if (ops == null) {
                    return null;
                }
                return pipeline.getPipelinedCap(ops);
            }
            default -> {
                assert false: "Unknown message target type. " + target.which();
                return null;
            }
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

        private final IncomingRpcMessage message;
        private final QuestionRef questionRef;
        private final AnyPointer.Reader results;

        RpcResponseImpl(QuestionRef questionRef,
                        IncomingRpcMessage message,
                        List<ClientHook> capTable,
                        AnyPointer.Reader results) {
            this.questionRef = questionRef;
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
            return this.payload.getContent().imbue(capTable);
        }

        int[] send() {
            var capTable = this.capTable.getTable();
            var fds = List.<FileDescriptor>of();
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
        private final boolean redirectResults;
        private boolean responseSent = false;
        private CompletableFuture<AnyPointer.Pipeline> tailCallPipeline;

        private boolean cancelRequested = false;
        private boolean cancelAllowed = false;

        private final CompletableFuture<java.lang.Void> canceller = new CompletableFuture<>();

        RpcCallContext(int answerId, IncomingRpcMessage request, List<ClientHook> capTable,
                       AnyPointer.Reader params, boolean redirectResults,
                       long interfaceId, short methodId) {
            this.answerId = answerId;
            this.interfaceId = interfaceId;
            this.methodId = methodId;
            this.request = request;
            this.params = params.imbue(new ReaderCapabilityTable(capTable));
            this.redirectResults = redirectResults;
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
            if (this.tailCallPipeline != null) {
                this.tailCallPipeline.complete(new AnyPointer.Pipeline(result.pipeline));
            }
            return result.promise.copy();
        }

        @Override
        public void allowCancellation() {
            boolean previouslyRequestedButNotAllowed = (!this.cancelAllowed && this.cancelRequested);
            this.cancelAllowed = true;

            if (previouslyRequestedButNotAllowed) {
                this.canceller.complete(null);
            }
        }

        @Override
        public CompletableFuture<AnyPointer.Pipeline> onTailCall() {
            assert this.tailCallPipeline == null: "Called onTailCall twice?";
            this.tailCallPipeline = new CompletableFuture<>();
            return this.tailCallPipeline.copy();
        }

        @Override
        public ClientHook.VoidPromiseAndPipeline directTailCall(RequestHook request) {
            assert this.response == null: "Can't call tailCall() after initializing the results struct.";

            if (request.getBrand() == RpcState.this && !this.redirectResults) {
                // The tail call is headed towards the peer that called us in the first place, so we can
                // optimize out the return trip.

                var tailInfo = ((RpcRequest)request).tailSend();
                if (tailInfo != null) {
                    if (isFirstResponder()) {
                        if (isConnected()) {
                            var message = connection.newOutgoingMessage(
                                    messageSizeHint()
                                            + RpcProtocol.Return.factory.structSize().total());
                            var builder = message.getBody().initAs(RpcProtocol.Message.factory).initReturn();
                            builder.setAnswerId(this.answerId);
                            builder.setReleaseParamCaps(false);
                            builder.setTakeFromOtherQuestion(tailInfo.questionId);
                            LOGGER.fine(() -> this.toString() + ": > RETURN answer=" + answerId);
                            message.send();
                        }

                        cleanupAnswerTable(null);
                    }
                    return new ClientHook.VoidPromiseAndPipeline(tailInfo.promise, tailInfo.pipeline);
                }
            }

            // Just forward to another local call
            var response = request.send();
            var promise = response.thenAccept(
                    results -> getResults(0).setAs(AnyPointer.factory, results));
            return new ClientHook.VoidPromiseAndPipeline(promise, response.pipeline().hook);
        }

        RpcResponse consumeRedirectedResponse() {
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

            LOGGER.fine(() -> RpcState.this.toString() + ": > RETURN answer=" + this.answerId);

            int[] exports = null;
            try {
                exports = ((RpcServerResponseImpl) response).send();
            } catch (Throwable exc) {
                this.responseSent = false;
                sendErrorReturn(exc);
            }
            cleanupAnswerTable(exports);
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
                FromException(exc, builder.initException());
                LOGGER.log(Level.FINE, this.toString() + ": > RETURN", exc);
                message.send();
            }

            cleanupAnswerTable(null);
        }

        private boolean isFirstResponder() {
            if (this.responseSent) {
                return false;
            }
            this.responseSent = true;
            return true;
        }

        private void cleanupAnswerTable(int[] resultExports) {
            if (this.cancelRequested) {
                assert resultExports == null || resultExports.length == 0;
                // Already received `Finish` so it's our job to erase the table entry. We shouldn't have
                // sent results if canceled, so we shouldn't have an export list to deal with.
                answers.erase(this.answerId);
            }
            else {
                // We just have to null out callContext and set the exports.
                var answer = answers.find(answerId);
                answer.callContext = null;
                answer.resultExports = resultExports;
            }
        }

        void requestCancel() {
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
                this.canceller.complete(null);
            }
        }

        /** Completed by the call context when a cancellation has been
         * requested and cancellation is allowed
         */
        CompletableFuture<java.lang.Void> whenCancelled() {
            return this.canceller;
        }
    }

    enum PipelineState {
        WAITING, RESOLVED, BROKEN
    }

    private class RpcPipeline implements PipelineHook {

        private final QuestionRef questionRef;
        private PipelineState state = PipelineState.WAITING;
        private RpcResponse resolved;
        private Throwable broken;

        private final HashMap<List<Short>, ClientHook> clientMap = new HashMap<>();
        private final CompletableFuture<RpcResponse> redirectLater;
        private final CompletableFuture<RpcResponse> resolveSelf;

        RpcPipeline(QuestionRef questionRef,
                    CompletableFuture<RpcResponse> redirectLater) {
            this.questionRef = questionRef;
            assert redirectLater != null;
            this.redirectLater = redirectLater;
            this.resolveSelf = this.redirectLater
                    .thenApply(response -> {
                        this.state = PipelineState.RESOLVED;
                        this.resolved = response;
                        return response;
                    })
                    .exceptionally(exc -> {
                        this.state = PipelineState.BROKEN;
                        this.broken = exc;
                        return null;
                    });
        }

        /**
         * Construct a new RpcPipeline that is never expected to resolve.
         */
        RpcPipeline(QuestionRef questionRef) {
            this.questionRef = questionRef;
            this.redirectLater = null;
            this.resolveSelf = null;
        }

        @Override
        public ClientHook getPipelinedCap(short[] ops) {
            var key = new ArrayList<Short>(ops.length);
            for (short op: ops) {
                key.add(op);
            }

            return this.clientMap.computeIfAbsent(key, k -> {
                return switch (state) {
                    case WAITING -> {
                        var pipelineClient = new PipelineClient(this.questionRef, ops);
                        if (this.redirectLater == null) {
                            // This pipeline will never get redirected, so just return the PipelineClient.
                            yield pipelineClient;
                        }

                        assert this.resolveSelf != null;
                        var resolutionPromise = this.resolveSelf.thenApply(
                                response -> response.getResults().getPipelinedCap(ops));
                        yield new PromiseClient(pipelineClient, resolutionPromise, null);
                    }
                    case RESOLVED -> {
                        assert this.resolved != null;
                        yield this.resolved.getResults().getPipelinedCap(ops);
                    }
                    case BROKEN -> {
                        assert this.broken != null;
                        yield Capability.newBrokenCap(broken);
                    }
                };
            });
        }

        @Override
        public void cancel(Throwable exc) {
            this.questionRef.fulfill(exc);
        }
    }

    abstract class RpcClient implements ClientHook {

        public abstract Integer writeDescriptor(RpcProtocol.CapDescriptor.Builder descriptor, List<FileDescriptor> fds);

        public abstract ClientHook writeTarget(RpcProtocol.MessageTarget.Builder target);

        public ClientHook getInnermostClient() {
            return this;
        }

        @Override
        public Request<AnyPointer.Builder> newCall(long interfaceId, short methodId) {
            return newCallNoIntercept(interfaceId, methodId);
        }

        @Override
        public VoidPromiseAndPipeline call(long interfaceId, short methodId, CallContextHook ctx) {
            return this.callNoIntercept(interfaceId, methodId, ctx);
        }

        public VoidPromiseAndPipeline callNoIntercept(long interfaceId, short methodId, CallContextHook ctx) {
            var params = ctx.getParams();
            var request = newCallNoIntercept(interfaceId, methodId);
            ctx.allowCancellation();
            ctx.releaseParams();
            return ctx.directTailCall(request.getHook());
        }

        @Override
        public final Object getBrand() {
            return RpcState.this;
        }

        private Request<AnyPointer.Builder> newCallNoIntercept(long interfaceId, short methodId) {
            if (isDisconnected()) {
                return Capability.newBrokenRequest(disconnected);
            }

            var request = new RpcRequest(this);
            var callBuilder = request.getCall();
            callBuilder.setInterfaceId(interfaceId);
            callBuilder.setMethodId(methodId);
            var root = request.getRoot();
            return Capability.newTypelessRequest(root, request);
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
                var replacement = Capability.newTypelessRequest(paramsBuilder, redirected.getHook());
                return replacement.sendInternal();
            }

            var questionRef = sendInternal(false);

            // The pipeline must get notified of resolution before the app does to maintain ordering.
            var pipeline = new RpcPipeline(questionRef, questionRef.response);

            var appPromise = questionRef.response.thenApply(
                    hook -> new Response<>(hook.getResults(), hook));

            return new RemotePromise<>(appPromise, new AnyPointer.Pipeline(pipeline));
        }

        QuestionRef sendInternal(boolean isTailCall) {
            // TODO refactor
            var fds = List.<FileDescriptor>of();
            var exports = writeDescriptors(capTable.getTable(), callBuilder.getParams(), fds);
            message.setFds(fds);
            var question = questions.next();
            question.isAwaitingReturn = true;
            question.isTailCall = isTailCall;
            question.paramExports = exports;

            var questionRef = question.selfRef;

            callBuilder.setQuestionId(question.id);
            if (isTailCall) {
                callBuilder.getSendResultsTo().getYourself();
            }
            try {
                LOGGER.fine(() -> RpcState.this.toString() + ": > CALL question=" + question.id);
                message.send();
            } catch (Exception exc) {
                question.isAwaitingReturn = false;
                question.skipFinish = true;
                questionRef.fulfill(exc);
            }
            return questionRef;
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

    private class ImportDisposer extends WeakReference<ImportRef> {

        private final int importId;

        ImportDisposer(ImportRef importRef) {
            super(importRef, importRefs);
            this.importId = importRef.importId;
        }

        void dispose() {
            var imp = imports.find(this.importId);
            if (imp != null) {
                imp.dispose();
            }
        }
    }

    private static class ImportRef {

        final int importId;

        ImportRef(int importId) {
            this.importId = importId;
        }
    }

    private class ImportClient extends RpcClient {

        private final ImportRef importRef;

        ImportClient(ImportRef importRef) {
            this.importRef = importRef;
        }

        @Override
        public Integer writeDescriptor(RpcProtocol.CapDescriptor.Builder descriptor, List<FileDescriptor> fds) {
            descriptor.setReceiverHosted(this.importRef.importId);
            return null;
        }

        @Override
        public ClientHook writeTarget(RpcProtocol.MessageTarget.Builder target) {
            target.setImportedCap(this.importRef.importId);
            return null;
        }

        @Override
        public CompletableFuture<ClientHook> whenMoreResolved() {
            return null;
        }

        @Override
        public FileDescriptor getFd() {
            var imp = imports.find(this.importRef.importId);
            return imp != null ? imp.fd : null;
        }
    }

    private void cleanupImports() {
        while (true) {
            var disposer = (ImportDisposer)this.importRefs.poll();
            if (disposer == null) {
                return;
            }
            disposer.dispose();
        }
    }

    private void cleanupQuestions() {
        while (true) {
            var disposer = (QuestionDisposer)this.questionRefs.poll();
            if (disposer == null) {
                break;
            }
            disposer.dispose();
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

        private ClientHook cap;
        private final ImportRef importRef;
        private boolean receivedCall = false;
        private ResolutionType resolutionType = ResolutionType.UNRESOLVED;
        private final CompletableFuture<ClientHook> eventual;

        PromiseClient(RpcClient initial,
                      CompletableFuture<ClientHook> eventual,
                      ImportRef importRef) {
            this.cap = initial;
            this.importRef = importRef;
            this.eventual = eventual.handle((resolution, exc) -> {
                this.cap = exc == null
                        ? this.resolve(resolution)
                        : this.resolve(Capability.newBrokenCap(exc));

                if (this.importRef != null) {
                    var imp = imports.find(this.importRef.importId);
                    if (imp != null && imp.appClient == this) {
                        imp.appClient = null;
                    }
                }

                return this.cap;
            });
        }

        @Override
        public Integer writeDescriptor(RpcProtocol.CapDescriptor.Builder target, List<FileDescriptor> fds) {
            this.receivedCall = true;
            return RpcState.this.writeDescriptor(this.cap, target, fds);
        }

        @Override
        public ClientHook writeTarget(RpcProtocol.MessageTarget.Builder target) {
            this.receivedCall = true;
            return RpcState.this.writeTarget(this.cap, target);
        }

        @Override
        public ClientHook getInnermostClient() {
            this.receivedCall = true;
            return RpcState.this.getInnermostClient(this.cap);
        }

        @Override
        public Request<AnyPointer.Builder> newCall(long interfaceId, short methodId) {
            this.receivedCall = true;
            return this.cap.newCall(interfaceId, methodId);
        }

        @Override
        public VoidPromiseAndPipeline call(long interfaceId, short methodId, CallContextHook ctx) {
            this.receivedCall = true;
            return this.cap.call(interfaceId, methodId, ctx);
        }

        @Override
        public ClientHook getResolved() {
            return this.isResolved()
                    ? this.cap
                    : null;
        }

        @Override
        public CompletableFuture<ClientHook> whenMoreResolved() {
            return this.eventual.copy();
        }

        @Override
        public FileDescriptor getFd() {
            if (this.isResolved()) {
                return this.cap.getFd();
            }
            else {
                // In theory, before resolution, the ImportClient for the promise could have an FD
                // attached, if the promise itself was presented with an attached FD. However, we can't
                // really return that one here because it may be closed when we get the Resolve message
                // later. In theory we could have the PromiseClient itself take ownership of an FD that
                // arrived attached to a promise cap, but the use case for that is questionable. I'm
                // keeping it simple for now.
                return null;
            }
        }

        private boolean isResolved() {
            return resolutionType != ResolutionType.UNRESOLVED;
        }

        private ClientHook resolve(ClientHook replacement) {
            assert !isResolved();

            var replacementBrand = replacement.getBrand();
            boolean isSameConnection = replacementBrand == RpcState.this;
            if (isSameConnection) {
                // We resolved to some other RPC capability hosted by the same peer.
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
                LOGGER.fine(() -> RpcState.this.toString() + ": embargoing reflected capability " + this.toString());
                // The new capability is hosted locally, not on the remote machine.  And, we had made calls
                // to the promise.  We need to make sure those calls echo back to us before we allow new
                // calls to go directly to the local capability, so we need to set a local embargo and send
                // a `Disembargo` to echo through the peer.
                int sizeHint = messageSizeHint(RpcProtocol.Disembargo.factory);
                var message = connection.newOutgoingMessage(sizeHint);
                var disembargo = message.getBody().initAs(RpcProtocol.Message.factory).initDisembargo();
                var redirect = RpcState.this.writeTarget(cap, disembargo.initTarget());
                assert redirect == null: "Original promise target should always be from this RPC connection.";

                var embargo = embargos.next();
                disembargo.getContext().setSenderLoopback(embargo.id);

                ClientHook finalReplacement = replacement;
                var embargoPromise = embargo.disembargo.thenApply(
                        void_ -> finalReplacement);
                LOGGER.fine(() -> RpcState.this.toString() + ": > DISEMBARGO");
                message.send();
                return Capability.newLocalPromiseClient(embargoPromise);
            }
            else {
                return replacement;
            }
        }
    }

    private class PipelineClient extends RpcClient {

        private final QuestionRef questionRef;
        private final short[] ops;

        PipelineClient(QuestionRef questionRef, short[] ops) {
            this.questionRef = questionRef;
            this.ops = ops.clone();
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
        public Integer writeDescriptor(RpcProtocol.CapDescriptor.Builder descriptor, List<FileDescriptor> fds) {
            var promisedAnswer = descriptor.initReceiverAnswer();
            promisedAnswer.setQuestionId(questionRef.questionId);
            FromPipelineOps(ops, promisedAnswer);
            return null;
        }

        @Override
        public ClientHook writeTarget(RpcProtocol.MessageTarget.Builder target) {
            var builder = target.initPromisedAnswer();
            builder.setQuestionId(questionRef.questionId);
            FromPipelineOps(ops, builder);
            return null;
        }
    }

    static void FromPipelineOps(short[] ops, RpcProtocol.PromisedAnswer.Builder builder) {
        var transforms = builder.initTransform(ops.length);
        for (int ii = 0; ii < ops.length; ++ii) {
            var transform = transforms.get(ii);
            var pointerIndex = ops[ii];
            if (pointerIndex < 0) {
                transform.setNoop(null);
            }
            else {
                transform.setGetPointerField(pointerIndex);
            }
        }
    }

    static short[] ToPipelineOps(RpcProtocol.PromisedAnswer.Reader reader) {
        var transforms = reader.getTransform();
        var ops = new short[transforms.size()];
        for (int ii = 0; ii < ops.length; ++ii) {
            var transform = transforms.get(ii);
            switch (transform.which()) {
                case NOOP:
                    ops[ii] = -1;
                    break;
                case GET_POINTER_FIELD:
                    ops[ii] = transform.getGetPointerField();
                    break;
                case _NOT_IN_SCHEMA:
                    return null;
            };
        }
        return ops;
    }

    static void FromException(Throwable exc, RpcProtocol.Exception.Builder builder) {
        var type = RpcProtocol.Exception.Type.FAILED;
        if (exc instanceof RpcException) {
            var rpcExc = (RpcException) exc;
            type = switch (rpcExc.getType()) {
                case FAILED -> RpcProtocol.Exception.Type.FAILED;
                case OVERLOADED -> RpcProtocol.Exception.Type.OVERLOADED;
                case DISCONNECTED -> RpcProtocol.Exception.Type.DISCONNECTED;
                case UNIMPLEMENTED -> RpcProtocol.Exception.Type.UNIMPLEMENTED;
            };
        }
        builder.setType(type);

        var writer = new StringWriter();
        exc.printStackTrace(new PrintWriter(writer));
        builder.setReason(writer.toString());
    }

    static RpcException ToException(RpcProtocol.Exception.Reader reader) {
        var type = switch (reader.getType()) {
            case OVERLOADED -> RpcException.Type.OVERLOADED;
            case DISCONNECTED -> RpcException.Type.DISCONNECTED;
            case UNIMPLEMENTED -> RpcException.Type.UNIMPLEMENTED;
            default -> RpcException.Type.FAILED;
        };
        return new RpcException(type, reader.getReason().toString());
    }

    class TribbleRaceBlocker implements ClientHook {

        final ClientHook inner;

        TribbleRaceBlocker(ClientHook inner) {
            this.inner = inner;
        }

        @Override
        public Request<AnyPointer.Builder> newCall(long interfaceId, short methodId) {
            return this.inner.newCall(interfaceId, methodId);
        }

        @Override
        public VoidPromiseAndPipeline call(long interfaceId, short methodId, CallContextHook ctx) {
            return this.inner.call(interfaceId, methodId, ctx);
        }

        @Override
        public ClientHook getResolved() {
            return null;
        }

        @Override
        public CompletableFuture<ClientHook> whenMoreResolved() {
            return null;
        }

        @Override
        public Object getBrand() {
            return null;
        }

        @Override
        public FileDescriptor getFd() {
            return this.inner.getFd();
        }
    }
}
