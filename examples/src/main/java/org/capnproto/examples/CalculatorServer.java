package org.capnproto.examples;

import org.capnproto.*;

import java.io.IOException;
import java.lang.Void;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;


public class CalculatorServer {

    // https://www.nurkiewicz.com/2013/05/java-8-completablefuture-in-action.html
    private static <T> CompletableFuture<List<T>> sequence(List<? extends CompletableFuture<T>> futures) {
        var done = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        return done.thenApply(void_ ->
                futures.stream().
                        map(CompletableFuture::join).
                        collect(Collectors.<T>toList())
        );
    }

    static CompletableFuture<Double> readValue(org.capnproto.examples.Calc.Calculator.Value.Client value) {
        return value.readRequest().send().thenApply(result -> result.getValue());
    }

    static CompletableFuture<Double> evaluateImpl(org.capnproto.examples.Calc.Calculator.Expression.Reader expression,
            PrimitiveList.Double.Reader params) {
        switch (expression.which()) {
            case LITERAL:
                return CompletableFuture.completedFuture(expression.getLiteral());

            case PREVIOUS_RESULT:
                return readValue(expression.getPreviousResult());

            case PARAMETER:
                return CompletableFuture.completedFuture(params.get(expression.getParameter()));

            case CALL: {
                var call = expression.getCall();
                var func = call.getFunction();

                // Evaluate each parameter.
                var paramPromises = new ArrayList<CompletableFuture<Double>>();
                for (var param: call.getParams()) {
                    paramPromises.add(evaluateImpl(param, params));
                }

                // When the parameters are complete, call the function.
                var joinedParams = sequence(paramPromises);

                // When the parameters are complete, call the function.
                return joinedParams.thenCompose(paramValues -> {
                    var request = func.callRequest();
                    var funcParams = request.getParams().initParams(paramValues.size());
                    for (int ii = 0; ii < paramValues.size(); ++ii) {
                        funcParams.set(ii, paramValues.get(ii));
                    }
                    return request.send().thenApply(result -> result.getValue());
                });
            }

            default:
                return CompletableFuture.failedFuture(RpcException.failed("Unknown expression type."));
        }
    }

    static class ValueImpl extends Calc.Calculator.Value.Server {
        private final double value;

        public ValueImpl(double value) {
            this.value = value;
        }

        @Override
        protected CompletableFuture<Void> read(CallContext<Calc.Calculator.Value.ReadParams.Reader, Calc.Calculator.Value.ReadResults.Builder> context) {
            context.getResults().setValue(this.value);
            return READY_NOW;
        }
    }

    static class FunctionImpl extends Calc.Calculator.Function.Server {

        private final int paramCount;
        private final Calc.Calculator.Expression.Reader expression;

        public FunctionImpl(int paramCount, Calc.Calculator.Expression.Reader body) {
            this.paramCount = paramCount;
            this.expression = body;
        }

        @Override
        protected CompletableFuture<Void> call(CallContext<Calc.Calculator.Function.CallParams.Reader, Calc.Calculator.Function.CallResults.Builder> context) {
            var params = context.getParams().getParams();
            if (params.size() != this.paramCount) {
                return CompletableFuture.failedFuture(RpcException.failed("Wrong number of parameters"));
            }

            return evaluateImpl(expression, params)
                    .thenAccept(value -> context.getResults().setValue(value));
        }
    }

    static class OperatorImpl extends Calc.Calculator.Function.Server {
        private final Calc.Calculator.Operator op;

        public OperatorImpl(Calc.Calculator.Operator op) {
            this.op = op;
        }

        @Override
        protected CompletableFuture<Void> call(CallContext<Calc.Calculator.Function.CallParams.Reader, Calc.Calculator.Function.CallResults.Builder> context) {
            var params = context.getParams().getParams();
            if (params.size() != 2) {
                return CompletableFuture.failedFuture(RpcException.failed("Wrong number of parameters"));
            }

            var x = params.get(0);
            var y = params.get(1);

            double result;
            switch (op) {
                case ADD:
                    result = x + y;
                    break;
                case SUBTRACT:
                    result = x - y;
                    break;
                case MULTIPLY:
                    result = x * y;
                    break;
                case DIVIDE:
                    result = x / y;
                    break;
                default:
                    result = Double.NaN;
            };

            context.getResults().setValue(result);
            return READY_NOW;
        }
    }

    static class CalculatorImpl extends Calc.Calculator.Server {
        @Override
        protected CompletableFuture<Void> evaluate(CallContext<Calc.Calculator.EvaluateParams.Reader, Calc.Calculator.EvaluateResults.Builder> context) {
            return evaluateImpl(context.getParams().getExpression(), null).thenAccept(value -> {
                context.getResults().setValue(new ValueImpl(value));
            });
        }

        @Override
        protected CompletableFuture<Void> defFunction(CallContext<Calc.Calculator.DefFunctionParams.Reader, Calc.Calculator.DefFunctionResults.Builder> context) {
            var params = context.getParams();
            context.getResults().setFunc(new FunctionImpl(params.getParamCount(), params.getBody()));
            return READY_NOW;
        }

        @Override
        protected CompletableFuture<Void> getOperator(CallContext<Calc.Calculator.GetOperatorParams.Reader, Calc.Calculator.GetOperatorResults.Builder> context) {
            context.getResults().setFunc(new OperatorImpl(context.getParams().getOp()));
            return READY_NOW;
        }
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            return;
        }

        var hostPort = args[0].split(":");
        var address = new InetSocketAddress(hostPort[0], Integer.parseInt(hostPort[1]));
        try {
            var server = new EzRpcServer(new CalculatorImpl(), address);
            var port = server.getPort();
            System.out.println("Listening on port " + port + "...");
            server.start().join();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
