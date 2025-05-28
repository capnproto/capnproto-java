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

package org.capnproto;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.ref.Cleaner;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;


public class MemoryMappedAllocator implements Allocator {
    // cleaner for file cleanup when this is GCed
    private static final Cleaner cleaner = Cleaner.create();
    private final Cleaner.Cleanable cleanable;

    // the length of the random prefix part of the random filename string
    private final int PREFIX_LENGTH = 5;
    // the used charset for creating random filenames
    private static final String CHARSET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    
    // (minimum) number of bytes in the next allocation
    private int nextSize = BuilderArena.SUGGESTED_FIRST_SEGMENT_WORDS;
    // the maximum allocateable size
    public int maxSegmentBytes = Integer.MAX_VALUE - 2;

    // the memory mapped file buffer name prefix
    private final String rPrefix;

    // the allocation strategy with which the allocation size grows
    public AllocationStrategy allocationStrategy =
        AllocationStrategy.GROW_HEURISTICALLY;

    // hashmaps used for keeping track of files
    private final Map<Integer, RandomAccessFile> randomAccFiles = Collections.synchronizedMap(new HashMap<>());
    private final Map<Integer, FileChannel> channelMap = Collections.synchronizedMap(new HashMap<>());

    public MemoryMappedAllocator(String baseFileName) {
        // create random file name with baseFileNamePrefix:
        // ${baseFileName}_XXXXX_000001
        Random random = new Random();
        StringBuilder sb = new StringBuilder(PREFIX_LENGTH);
        for (int i = 0; i < PREFIX_LENGTH; i++) {
            int index = random.nextInt(CHARSET.length());
            sb.append(CHARSET.charAt(index));
        }
        rPrefix = baseFileName + "_" + sb.toString();
        this.cleanable = cleaner.register(this, new State(this.randomAccFiles, rPrefix));
    }


    public MemoryMappedAllocator(String baseFileName, AllocationStrategy allocationStrategy) {
        // create random file name with baseFileNamePrefix:
        // ${baseFileName}_XXXXX_000001
        Random random = new Random();
        StringBuilder sb = new StringBuilder(PREFIX_LENGTH);
        for (int i = 0; i < PREFIX_LENGTH; i++) {
            int index = random.nextInt(CHARSET.length());
            sb.append(CHARSET.charAt(index));
        }
        rPrefix = baseFileName + "_" + sb.toString();
        this.cleanable = cleaner.register(this, new State(this.randomAccFiles, rPrefix));
        this.allocationStrategy = allocationStrategy;
    }


    private static String nameForInt(String prefix, int key) {
        String ret = prefix + "_" + String.format("%05d", key);
        return ret;
    }


    private Integer generateFile() throws IOException {
        int fCount;
        synchronized (randomAccFiles) {
            fCount = randomAccFiles.size();
            String newFileName = nameForInt(rPrefix, fCount);
            RandomAccessFile newFile = new RandomAccessFile(newFileName, "rw");
            randomAccFiles.put(fCount, newFile);
            File test = new File(newFileName);
            test.deleteOnExit();
        }
        return fCount;
    }


    /**
     * set the grow size of the memory mapped file
     */
    @Override
    public void setNextAllocationSizeBytes(int nextSize) {
        this.nextSize = nextSize;
    }


    private FileChannel createSegment(int segmentSize) throws IOException {
        int fileKey = generateFile();
        FileChannel channel = null;
        synchronized (channelMap) {
            if (!channelMap.containsKey(fileKey)) {
                synchronized (randomAccFiles) {
                    RandomAccessFile file = randomAccFiles.get(fileKey);
                    file.setLength(segmentSize);
                    channel = file.getChannel();
                    channelMap.put(fileKey, channel);
                }
            }
        }
        return channel;
    }


    @Override
    public java.nio.ByteBuffer allocateSegment(int minimumSize) {
        int size = Math.max(minimumSize, this.nextSize);
        MappedByteBuffer result = null;
        try {
            FileChannel channel = createSegment(size);
            result = channel.map(FileChannel.MapMode.READ_WRITE, 0, size);
        }
        catch (IOException e)
        {
            System.err.println("IOException: allocateSegment failed with:" + e);
        }


        switch (this.allocationStrategy) {
            case GROW_HEURISTICALLY:
                if (size < this.maxSegmentBytes - this.nextSize) {
                    this.nextSize += size;
                } else {
                    this.nextSize = maxSegmentBytes;
                }
                break;
            case FIXED_SIZE:
                break;
        }



        // if (size < this.maxSegmentBytes - this.nextSize) {
        //     this.nextSize += size;
        // } else {
        //     this.nextSize = maxSegmentBytes;
        // }
        return result;
    }


    private static class State implements Runnable {
        private final Map<Integer, RandomAccessFile> randomAccFiles;
        private final String rPrefix;

        State(Map<Integer, RandomAccessFile> files, String rPrefix) {
            this.randomAccFiles = files;
            this.rPrefix = rPrefix;
        }

        @Override
        public void run() {
            // Cleanup logic: delete all files
            for (Map.Entry<Integer,RandomAccessFile> entry : randomAccFiles.entrySet()) {
                try {
                    entry.getValue().close();
                }
                catch (IOException e)
                {
                }
                String name = nameForInt(rPrefix, entry.getKey());
                File file = new File(name);
                file.delete();
            }
        }
    }

    /**
     * Explicit cleanup: WARNING: this invalidates all alloceted buffers,
     * use close() after using the ByteBuffers.
     */
    public void close() {
        cleanable.clean();
    }
}
