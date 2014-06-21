package org.capnproto.benchmark;

import org.capnproto.FromStructReader;
import org.capnproto.FromStructBuilder;
import org.capnproto.StructFactory;
import org.capnproto.MessageBuilder;

public abstract class TestCase<RequestFactory extends StructFactory<RequestBuilder, RequestReader>,
                          RequestBuilder, RequestReader,
                          ResponseFactory extends StructFactory<ResponseBuilder, ResponseReader>,
                          ResponseBuilder, ResponseReader, Expectation> {
    public abstract Expectation setupRequest(Common.FastRand rng, RequestBuilder request);
    public abstract void handleRequest(RequestReader request, ResponseBuilder response);
    public abstract boolean checkResponse(ResponseReader response, Expectation expected);

    public void execute(String[] args, RequestFactory requestFactory, ResponseFactory responseFactory) {

        if (args.length != 4) {
            System.out.println("USAGE: TestCase MODE REUSE COMPRESSION ITERATION_COUNT");
            return;
        }

        long iters = Long.parseLong(args[3]);

        Common.FastRand rng = new Common.FastRand();

        for (int i = 0; i < iters; ++i) {
            MessageBuilder requestMessage = new MessageBuilder();
            MessageBuilder responseMessage = new MessageBuilder();
            RequestBuilder request = requestMessage.initRoot(requestFactory);
            Expectation expected = this.setupRequest(rng, request);
            ResponseBuilder response = responseMessage.initRoot(responseFactory);
            this.handleRequest(requestFactory.asReader(request), response);
            if (!this.checkResponse(responseFactory.asReader(response), expected)) {
                System.out.println("mismatch!");
            }
        }


    }
}
