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

    public void execute(RequestFactory requestFactory, ResponseFactory responseFactory) {
        Common.FastRand rng = new Common.FastRand();

        for (int i = 0; i < 50000; ++i) {
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
