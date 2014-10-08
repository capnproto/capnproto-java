// Copyright (c) 2013-2014 Sandstorm Development Group, Inc. and contributors
// Licensed under the MIT License:
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

package org.capnproto.benchmark;

import java.io.IOException;
import java.nio.ByteBuffer;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileDescriptor;

import org.capnproto.StructFactory;
import org.capnproto.MessageBuilder;
import org.capnproto.MessageReader;

public abstract class TestCase<RequestFactory extends
                          StructFactory<RequestBuilder, RequestReader>,
                          RequestBuilder extends org.capnproto.StructBuilder,
                          RequestReader extends org.capnproto.StructReader,
                          ResponseFactory extends StructFactory<ResponseBuilder, ResponseReader>,
                          ResponseBuilder extends org.capnproto.StructBuilder,
                          ResponseReader extends org.capnproto.StructReader, Expectation> {
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
