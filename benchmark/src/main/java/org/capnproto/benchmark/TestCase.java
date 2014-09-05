package org.capnproto.benchmark;

import java.io.IOException;
import java.nio.ByteBuffer;

import java.io.FileInputStream;
import java.io.FileDescriptor;

import org.capnproto.FromStructReader;
import org.capnproto.FromStructBuilder;
import org.capnproto.StructFactory;
import org.capnproto.MessageBuilder;
import org.capnproto.MessageReader;

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
                            long iters) throws IOException {

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
                org.capnproto.ArrayOutputStream writer = new org.capnproto.ArrayOutputStream(requestBytes);
                org.capnproto.Serialize.writeMessage(writer, requestMessage);
            }

            {
                org.capnproto.MessageReader messageReader =
                    org.capnproto.ByteChannelMessageReader.create(new org.capnproto.ArrayInputStream(requestBytes));
                this.handleRequest(messageReader.getRoot(requestFactory), response);
            }

            {
                org.capnproto.ArrayOutputStream writer = new org.capnproto.ArrayOutputStream(responseBytes);
                org.capnproto.Serialize.writeMessage(writer, responseMessage);
            }

            {
                org.capnproto.MessageReader messageReader =
                    org.capnproto.ByteChannelMessageReader.create(new org.capnproto.ArrayInputStream(responseBytes));
                if (!this.checkResponse(messageReader.getRoot(responseFactory), expected)) {
                    throw new Error("incorrect response");
                }
            }
        }
    }

    public void syncClient(RequestFactory requestFactory, ResponseFactory responseFactory,
                           long iters) throws IOException {
        throw new Error("unimplemented");
    }

    public void syncServer(RequestFactory requestFactory, ResponseFactory responseFactory,
                           long iters) throws IOException {
        for (int ii = 0; ii < iters; ++ii) {
            MessageBuilder responseMessage = new MessageBuilder();
            {
                ResponseBuilder response = responseMessage.initRoot(responseFactory);

                MessageReader messageReader = org.capnproto.ByteChannelMessageReader.create(
                    (new FileInputStream(FileDescriptor.in)).getChannel());

                RequestReader request = messageReader.getRoot(requestFactory);
                this.handleRequest(request, response);
            }

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

        try {
            if (mode.equals("object")) {
                passByObject(requestFactory, responseFactory, iters);
            } else if (mode.equals("bytes")) {
                passByBytes(requestFactory, responseFactory, iters);
            } else if (mode.equals("client")) {
                syncClient(requestFactory, responseFactory, iters);
            } else if (mode.equals("server")) {
                syncServer(requestFactory, responseFactory, iters);
            } else {
                System.out.println("unknown mode: " + mode);
            }
        } catch (IOException e) {
            System.err.println("IOException: " + e);
        }
    }
}
