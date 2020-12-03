package org.capnproto.examples;

import org.capnproto.*;
import org.junit.Assert;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.ExecutionException;

public class CalculatorClient {

    public static void usage() {
        System.out.println("usage: host:port");
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            usage();
            return;
        }

        var endpoint = args[0].split(":");
        var address = new InetSocketAddress(endpoint[0], Integer.parseInt(endpoint[1]));
        try {
            var clientSocket = AsynchronousSocketChannel.open();
            clientSocket.connect(address).get();
            var rpcClient = new TwoPartyClient(clientSocket);
            var calculator = new org.capnproto.examples.Calc.Calculator.Client(rpcClient.bootstrap());

            {
                System.out.println("Evaluating a literal...");
                var request = calculator.evaluateRequest();
                request.getParams().getExpression().setLiteral(123);
                var evalPromise = request.send();
                var readPromise = evalPromise.getValue().readRequest().send();

                var response = rpcClient.runUntil(readPromise);
                Assert.assertTrue(response.get().getValue() == 123);
            }

            {
                // Make a request to evaluate 123 + 45 - 67.
                //
                // The Calculator interface requires that we first call getOperator() to
                // get the addition and subtraction functions, then call evaluate() to use
                // them.  But, once again, we can get both functions, call evaluate(), and
                // then read() the result -- four RPCs -- in the time of *one* network
                // round trip, because of promise pipelining.

                System.out.println("Using add and subtract... ");

                Calc.Calculator.Function.Client add;
                Calc.Calculator.Function.Client subtract;

                {
                    // Get the "add" function from the server.
                    var request = calculator.getOperatorRequest();
                    request.getParams().setOp(Calc.Calculator.Operator.ADD);
                    add = request.send().getFunc();
                }

                {
                    // Get the "subtract" function from the server.
                    var request = calculator.getOperatorRequest();
                    request.getParams().setOp(Calc.Calculator.Operator.SUBTRACT);
                    subtract = request.send().getFunc();
                }

                // Build the request to evaluate 123 + 45 - 67.
                var request = calculator.evaluateRequest();

                var subtractCall = request.getParams().getExpression().initCall();
                subtractCall.setFunction(subtract);
                var subtractParams = subtractCall.initParams(2);
                subtractParams.get(1).setLiteral(67);

                var addCall = subtractParams.get(0).initCall();
                addCall.setFunction(add);
                var addParams = addCall.initParams(2);
                addParams.get(0).setLiteral(123);
                addParams.get(1).setLiteral(45);

                var evalPromise = request.send();
                var readPromise = evalPromise.getValue().readRequest().send();

                // run the RPC system until the read request completes.
                var response = rpcClient.runUntil(readPromise).join();
                Assert.assertEquals(101, response.getValue(), 0.0001);

                System.out.println("PASS");
            }
        }
        catch (IOException | InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }
}
