package org.capnproto.benchmark;

import org.capnproto.FromStructReader;
import org.capnproto.FromStructBuilder;

public interface TestCase<RequestBuilder, RequestReader, ResponseBuilder, ResponseReader, Expectation> {
    public Expectation setupRequest(Common.FastRand rng, RequestBuilder request);
    public void handleRequest(RequestReader request, ResponseBuilder response);
    public boolean checkResponse(ResponseReader response, Expectation expected);
}
