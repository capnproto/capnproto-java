package org.capnproto.benchmark;

import java.nio.ByteBuffer;

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


    public void passByObject(RequestFactory requestFactory, ResponseFactory responseFactory,
                             long iters) {

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

    static final int SCRATCH_SIZE = 128 * 1024;

    public void passByBytes(RequestFactory requestFactory, ResponseFactory responseFactory,
                            long iters) {

        ByteBuffer requestBytes = ByteBuffer.allocate(SCRATCH_SIZE * 8);
        ByteBuffer responseBytes = ByteBuffer.allocate(SCRATCH_SIZE * 8);
        Common.FastRand rng = new Common.FastRand();

        for (int i = 0; i < iters; ++i) {
            MessageBuilder requestMessage = new MessageBuilder();
            MessageBuilder responseMessage = new MessageBuilder();
            RequestBuilder request = requestMessage.initRoot(requestFactory);
            Expectation expected = this.setupRequest(rng, request);
            ResponseBuilder response = responseMessage.initRoot(responseFactory);

            {
                org.capnproto.ByteBufferWritableByteChannel writer = new org.capnproto.ByteBufferWritableByteChannel(requestBytes);
                //org.capnproto.writeMessage
            }
            // TODO
            throw new Error("unimplemented");
        }
    }


    public void execute(String[] args, RequestFactory requestFactory, ResponseFactory responseFactory) {

        if (args.length != 4) {
            System.out.println("USAGE: TestCase MODE REUSE COMPRESSION ITERATION_COUNT");
            return;
        }

        String mode = args[0];
        String reuse = args[1];
        String compression = args[2];
        long iters = Long.parseLong(args[3]);

        if (mode.equals("object")) {
            passByObject(requestFactory, responseFactory, iters);
        } else if (mode.equals("bytes")) {
            passByBytes(requestFactory, responseFactory, iters);
        } else {
            System.out.println("unknown mode: " + mode);
        }
    }
}
