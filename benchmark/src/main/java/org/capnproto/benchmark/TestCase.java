package org.capnproto.benchmark;

import java.io.IOException;
import java.nio.ByteBuffer;

import java.io.FileInputStream;
import java.io.FileOutputStream;
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
                             Compression compression, long iters) {

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
                            Compression compression, long iters) throws IOException {

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
                compression.writeBuffered(writer, requestMessage);
            }

            {
                org.capnproto.MessageReader messageReader =
                    compression.newBufferedReader(new org.capnproto.ArrayInputStream(requestBytes));
                this.handleRequest(messageReader.getRoot(requestFactory), response);
            }

            {
                org.capnproto.ArrayOutputStream writer = new org.capnproto.ArrayOutputStream(responseBytes);
                compression.writeBuffered(writer, responseMessage);
            }

            {
                org.capnproto.MessageReader messageReader =
                    compression.newBufferedReader(new org.capnproto.ArrayInputStream(responseBytes));
                if (!this.checkResponse(messageReader.getRoot(responseFactory), expected)) {
                    throw new Error("incorrect response");
                }
            }
        }
    }

    public void syncServer(RequestFactory requestFactory, ResponseFactory responseFactory,
                           Compression compression, long iters) throws IOException {
        org.capnproto.BufferedOutputStreamWrapper outBuffered =
            new org.capnproto.BufferedOutputStreamWrapper((new FileOutputStream(FileDescriptor.out)).getChannel());
        org.capnproto.BufferedInputStreamWrapper inBuffered =
            new org.capnproto.BufferedInputStreamWrapper((new FileInputStream(FileDescriptor.in)).getChannel());

        for (int ii = 0; ii < iters; ++ii) {
            MessageBuilder responseMessage = new MessageBuilder();
            {
                ResponseBuilder response = responseMessage.initRoot(responseFactory);
                MessageReader messageReader = compression.newBufferedReader(inBuffered);
                RequestReader request = messageReader.getRoot(requestFactory);
                this.handleRequest(request, response);
            }
            compression.writeBuffered(outBuffered, responseMessage);
        }
    }

    public void syncClient(RequestFactory requestFactory, ResponseFactory responseFactory,
                           Compression compression, long iters) throws IOException {
        Common.FastRand rng = new Common.FastRand();
        org.capnproto.BufferedOutputStreamWrapper outBuffered =
            new org.capnproto.BufferedOutputStreamWrapper((new FileOutputStream(FileDescriptor.out)).getChannel());
        org.capnproto.BufferedInputStreamWrapper inBuffered =
            new org.capnproto.BufferedInputStreamWrapper((new FileInputStream(FileDescriptor.in)).getChannel());

        for (int ii = 0; ii < iters; ++ii) {
            MessageBuilder requestMessage = new MessageBuilder();
            RequestBuilder request = requestMessage.initRoot(requestFactory);
            Expectation expected = this.setupRequest(rng, request);

            compression.writeBuffered(outBuffered, requestMessage);
            MessageReader messageReader = compression.newBufferedReader(inBuffered);
            ResponseReader response = messageReader.getRoot(responseFactory);
            if (!this.checkResponse(response, expected)) {
                throw new Error("incorrect response");
            }
        }
    }

    public void execute(String[] args, RequestFactory requestFactory, ResponseFactory responseFactory) {

        if (args.length != 4) {
            System.out.println("USAGE: TestCase MODE REUSE COMPRESSION ITERATION_COUNT");
            return;
        }

        String mode = args[0];
        String reuse = args[1];
        Compression compression = null;
        if (args[2].equals("packed")) {
            compression = Compression.PACKED;
        } else if (args[2].equals("none")) {
            compression = Compression.UNCOMPRESSED;
        } else {
            throw new Error("unrecognized compression: " + args[2]);
        }
        long iters = Long.parseLong(args[3]);

        try {
            if (mode.equals("object")) {
                passByObject(requestFactory, responseFactory, compression, iters);
            } else if (mode.equals("bytes")) {
                passByBytes(requestFactory, responseFactory, compression, iters);
            } else if (mode.equals("client")) {
                syncClient(requestFactory, responseFactory, compression, iters);
            } else if (mode.equals("server")) {
                syncServer(requestFactory, responseFactory, compression, iters);
            } else {
                System.out.println("unrecognized mode: " + mode);
            }
        } catch (IOException e) {
            System.err.println("IOException: " + e);
        }
    }
}
