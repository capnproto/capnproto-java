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

import java.util.Collection;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class PrimitiveList {

    public static class Void {

        public static final class Factory extends ListFactory<Builder, Reader> {

            Factory() {
                super(ElementSize.VOID);
            }

            @Override
            public final Reader constructReader(SegmentDataContainer segment,
                    int ptr,
                    int elementCount, int step,
                    int structDataSize, short structPointerCount,
                    int nestingLimit) {
                return new Reader(segment, ptr, elementCount, step, structDataSize, structPointerCount, nestingLimit);
            }

            @Override
            public final Builder constructBuilder(GenericSegmentBuilder segment,
                    int ptr,
                    int elementCount, int step,
                    int structDataSize, short structPointerCount) {
                return new Builder(segment, ptr, elementCount, step, structDataSize, structPointerCount);
            }

        }
        public static final Factory factory = new Factory();

        public static final class Reader extends ListReader implements Collection<org.capnproto.Void> {

            public Reader(SegmentDataContainer segment,
                    int ptr,
                    int elementCount, int step,
                    int structDataSize, short structPointerCount,
                    int nestingLimit) {
                super(segment, ptr, elementCount, step, structDataSize, structPointerCount, nestingLimit);
            }

            public org.capnproto.Void get(int index) {
                return org.capnproto.Void.VOID;
            }

            @Override
            public boolean isEmpty() {
                return elementCount == 0;
            }

            @Override
            public boolean contains(Object o) {
                return stream().anyMatch(o::equals);
            }

            @Override
            public Object[] toArray() {
                return stream().collect(Collectors.toList()).toArray();
            }

            @Override
            public <T> T[] toArray(T[] a) {
                return stream().collect(Collectors.toList()).toArray(a);
            }

            @Override
            public boolean add(org.capnproto.Void e) {
                throw new UnsupportedOperationException("This collection is immutable");
            }

            @Override
            public boolean remove(Object o) {
                throw new UnsupportedOperationException("This collection is immutable");
            }

            @Override
            public boolean containsAll(Collection<?> c) {
                return stream().collect(Collectors.toList()).containsAll(c);
            }

            @Override
            public boolean addAll(Collection<? extends org.capnproto.Void> c) {
                throw new UnsupportedOperationException("This collection is immutable");
            }

            @Override
            public boolean removeAll(Collection<?> c) {
                throw new UnsupportedOperationException("This collection is immutable");
            }

            @Override
            public boolean retainAll(Collection<?> c) {
                throw new UnsupportedOperationException("This collection is immutable");
            }

            @Override
            public void clear() {
                throw new UnsupportedOperationException("This collection is immutable");
            }

            public Stream<org.capnproto.Void> stream() {
                return StreamSupport.stream(Spliterators.spliterator(this.iterator(), elementCount,
                        Spliterator.SIZED & Spliterator.IMMUTABLE
                ), false);
            }

            public final class Iterator implements java.util.Iterator<org.capnproto.Void> {

                public Void.Reader list;
                public int idx = 0;

                public Iterator(Void.Reader list) {
                    this.list = list;
                }

                @Override
                public org.capnproto.Void next() {
                    return get(idx++);
                }

                @Override
                public boolean hasNext() {
                    return idx < list.size();
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            }

            @Override
            public java.util.Iterator<org.capnproto.Void> iterator() {
                return new Iterator(this);
            }

        }

        public static final class Builder extends ListBuilder {

            public Builder(GenericSegmentBuilder segment, int ptr,
                    int elementCount, int step,
                    int structDataSize, short structPointerCount) {
                super(segment, ptr, elementCount, step, structDataSize, structPointerCount);
            }

            public final Reader asReader() {
                return new Reader(this.segment, this.ptr, this.elementCount, this.step,
                        this.structDataSize, this.structPointerCount,
                        java.lang.Integer.MAX_VALUE);
            }
        }
    }

    public static class Boolean {

        public static final class Factory extends ListFactory<Builder, Reader> {

            Factory() {
                super(ElementSize.BIT);
            }

            @Override
            public final Reader constructReader(SegmentDataContainer segment,
                    int ptr,
                    int elementCount, int step,
                    int structDataSize, short structPointerCount,
                    int nestingLimit) {
                return new Reader(segment, ptr, elementCount, step, structDataSize, structPointerCount, nestingLimit);
            }

            @Override
            public final Builder constructBuilder(GenericSegmentBuilder segment,
                    int ptr,
                    int elementCount, int step,
                    int structDataSize, short structPointerCount) {
                return new Builder(segment, ptr, elementCount, step, structDataSize, structPointerCount);
            }
        }
        public static final Factory factory = new Factory();

        public static final class Reader extends ListReader implements Collection<java.lang.Boolean> {

            public Reader(SegmentDataContainer segment,
                    int ptr,
                    int elementCount, int step,
                    int structDataSize, short structPointerCount,
                    int nestingLimit) {
                super(segment, ptr, elementCount, step, structDataSize, structPointerCount, nestingLimit);
            }

            public final boolean get(int index) {
                return _getBooleanElement(index);
            }

            @Override
            public boolean isEmpty() {
                return elementCount == 0;
            }

            @Override
            public boolean contains(Object o) {
                return stream().anyMatch(o::equals);
            }

            @Override
            public Object[] toArray() {
                return stream().collect(Collectors.toList()).toArray();
            }

            @Override
            public <T> T[] toArray(T[] a) {
                return stream().collect(Collectors.toList()).toArray(a);
            }

            @Override
            public boolean add(java.lang.Boolean e) {
                throw new UnsupportedOperationException("This collection is immutable");
            }

            @Override
            public boolean remove(Object o) {
                throw new UnsupportedOperationException("This collection is immutable");
            }

            @Override
            public boolean containsAll(Collection<?> c) {
                return stream().collect(Collectors.toList()).containsAll(c);
            }

            @Override
            public boolean addAll(Collection<? extends java.lang.Boolean> c) {
                throw new UnsupportedOperationException("This collection is immutable");
            }

            @Override
            public boolean removeAll(Collection<?> c) {
                throw new UnsupportedOperationException("This collection is immutable");
            }

            @Override
            public boolean retainAll(Collection<?> c) {
                throw new UnsupportedOperationException("This collection is immutable");
            }

            @Override
            public void clear() {
                throw new UnsupportedOperationException("This collection is immutable");
            }

            public Stream<java.lang.Boolean> stream() {
                return StreamSupport.stream(Spliterators.spliterator(this.iterator(), elementCount,
                        Spliterator.SIZED & Spliterator.IMMUTABLE
                ), false);
            }

            public final class Iterator implements java.util.Iterator<java.lang.Boolean> {

                public Reader list;
                public int idx = 0;

                public Iterator(Reader list) {
                    this.list = list;
                }

                @Override
                public java.lang.Boolean next() {
                    return get(idx++);
                }

                @Override
                public boolean hasNext() {
                    return idx < list.size();
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            }

            @Override
            public java.util.Iterator<java.lang.Boolean> iterator() {
                return new Iterator(this);
            }

            @Override
            public String toString() {
                return stream().map(String::valueOf).collect(Collectors.joining(","));
            }

        }

        public static final class Builder extends ListBuilder {

            public Builder(GenericSegmentBuilder segment, int ptr,
                    int elementCount, int step,
                    int structDataSize, short structPointerCount) {
                super(segment, ptr, elementCount, step, structDataSize, structPointerCount);
            }

            public boolean get(int index) {
                return _getBooleanElement(index);
            }

            public void set(int index, boolean value) {
                _setBooleanElement(index, value);
            }

            public final Reader asReader() {
                return new Reader(this.segment, this.ptr, this.elementCount, this.step,
                        this.structDataSize, this.structPointerCount,
                        java.lang.Integer.MAX_VALUE);
            }
        }
    }

    public static class Byte {

        public static final class Factory extends ListFactory<Builder, Reader> {

            Factory() {
                super(ElementSize.BYTE);
            }

            @Override
            public final Reader constructReader(SegmentDataContainer segment,
                    int ptr,
                    int elementCount, int step,
                    int structDataSize, short structPointerCount,
                    int nestingLimit) {
                return new Reader(segment, ptr, elementCount, step, structDataSize, structPointerCount, nestingLimit);
            }

            @Override
            public final Builder constructBuilder(GenericSegmentBuilder segment,
                    int ptr,
                    int elementCount, int step,
                    int structDataSize, short structPointerCount) {
                return new Builder(segment, ptr, elementCount, step, structDataSize, structPointerCount);
            }
        }
        public static final Factory factory = new Factory();

        public static final class Reader extends ListReader implements Collection<java.lang.Byte> {

            public Reader(SegmentDataContainer segment,
                    int ptr,
                    int elementCount, int step,
                    int structDataSize, short structPointerCount,
                    int nestingLimit) {
                super(segment, ptr, elementCount, step, structDataSize, structPointerCount, nestingLimit);
            }

            public byte get(int index) {
                return _getByteElement(index);
            }

            @Override
            public boolean isEmpty() {
                return elementCount == 0;
            }

            @Override
            public boolean contains(Object o) {
                return stream().anyMatch(o::equals);
            }

            @Override
            public Object[] toArray() {
                return stream().collect(Collectors.toList()).toArray();
            }

            @Override
            public <T> T[] toArray(T[] a) {
                return stream().collect(Collectors.toList()).toArray(a);
            }

            @Override
            public boolean add(java.lang.Byte e) {
                throw new UnsupportedOperationException("This collection is immutable");
            }

            @Override
            public boolean remove(Object o) {
                throw new UnsupportedOperationException("This collection is immutable");
            }

            @Override
            public boolean containsAll(Collection<?> c) {
                return stream().collect(Collectors.toList()).containsAll(c);
            }

            @Override
            public boolean addAll(Collection<? extends java.lang.Byte> c) {
                throw new UnsupportedOperationException("This collection is immutable");
            }

            @Override
            public boolean removeAll(Collection<?> c) {
                throw new UnsupportedOperationException("This collection is immutable");
            }

            @Override
            public boolean retainAll(Collection<?> c) {
                throw new UnsupportedOperationException("This collection is immutable");
            }

            @Override
            public void clear() {
                throw new UnsupportedOperationException("This collection is immutable");
            }

            public Stream<java.lang.Byte> stream() {
                return StreamSupport.stream(Spliterators.spliterator(this.iterator(), elementCount,
                        Spliterator.SIZED & Spliterator.IMMUTABLE
                ), false);
            }

            public final class Iterator implements java.util.Iterator<java.lang.Byte> {

                public Reader list;
                public int idx = 0;

                public Iterator(Reader list) {
                    this.list = list;
                }

                @Override
                public java.lang.Byte next() {
                    return get(idx++);
                }

                @Override
                public boolean hasNext() {
                    return idx < list.size();
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            }

            @Override
            public java.util.Iterator<java.lang.Byte> iterator() {
                return new Iterator(this);
            }

            @Override
            public String toString() {
                return stream().map(String::valueOf).collect(Collectors.joining(","));
            }

        }

        public static final class Builder extends ListBuilder {

            public Builder(GenericSegmentBuilder segment, int ptr,
                    int elementCount, int step,
                    int structDataSize, short structPointerCount) {
                super(segment, ptr, elementCount, step, structDataSize, structPointerCount);
            }

            public byte get(int index) {
                return _getByteElement(index);
            }

            public void set(int index, byte value) {
                _setByteElement(index, value);
            }

            public final Reader asReader() {
                return new Reader(this.segment, this.ptr, this.elementCount, this.step,
                        this.structDataSize, this.structPointerCount,
                        java.lang.Integer.MAX_VALUE);
            }
        }
    }

    public static class Short {

        public static final class Factory extends ListFactory<Builder, Reader> {

            Factory() {
                super(ElementSize.TWO_BYTES);
            }

            @Override
            public final Reader constructReader(SegmentDataContainer segment,
                    int ptr,
                    int elementCount, int step,
                    int structDataSize, short structPointerCount,
                    int nestingLimit) {
                return new Reader(segment, ptr, elementCount, step, structDataSize, structPointerCount, nestingLimit);
            }

            @Override
            public final Builder constructBuilder(GenericSegmentBuilder segment,
                    int ptr,
                    int elementCount, int step,
                    int structDataSize, short structPointerCount) {
                return new Builder(segment, ptr, elementCount, step, structDataSize, structPointerCount);
            }

        }
        public static final Factory factory = new Factory();

        public static final class Reader extends ListReader implements Collection<java.lang.Short> {

            public Reader(SegmentDataContainer segment,
                    int ptr,
                    int elementCount, int step,
                    int structDataSize, short structPointerCount,
                    int nestingLimit) {
                super(segment, ptr, elementCount, step, structDataSize, structPointerCount, nestingLimit);
            }

            public short get(int index) {
                return _getShortElement(index);
            }

            @Override
            public boolean isEmpty() {
                return elementCount == 0;
            }

            @Override
            public boolean contains(Object o) {
                return stream().anyMatch(o::equals);
            }

            @Override
            public Object[] toArray() {
                return stream().collect(Collectors.toList()).toArray();
            }

            @Override
            public <T> T[] toArray(T[] a) {
                return stream().collect(Collectors.toList()).toArray(a);
            }

            @Override
            public boolean add(java.lang.Short e) {
                throw new UnsupportedOperationException("This collection is immutable");
            }

            @Override
            public boolean remove(Object o) {
                throw new UnsupportedOperationException("This collection is immutable");
            }

            @Override
            public boolean containsAll(Collection<?> c) {
                return stream().collect(Collectors.toList()).containsAll(c);
            }

            @Override
            public boolean addAll(Collection<? extends java.lang.Short> c) {
                throw new UnsupportedOperationException("This collection is immutable");
            }

            @Override
            public boolean removeAll(Collection<?> c) {
                throw new UnsupportedOperationException("This collection is immutable");
            }

            @Override
            public boolean retainAll(Collection<?> c) {
                throw new UnsupportedOperationException("This collection is immutable");
            }

            @Override
            public void clear() {
                throw new UnsupportedOperationException("This collection is immutable");
            }

            public Stream<java.lang.Short> stream() {
                return StreamSupport.stream(Spliterators.spliterator(this.iterator(), elementCount,
                        Spliterator.SIZED & Spliterator.IMMUTABLE
                ), false);
            }

            public final class Iterator implements java.util.Iterator<java.lang.Short> {

                public Short.Reader list;
                public int idx = 0;

                public Iterator(Short.Reader list) {
                    this.list = list;
                }

                @Override
                public java.lang.Short next() {
                    return get(idx++);
                }

                @Override
                public boolean hasNext() {
                    return idx < list.size();
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            }

            @Override
            public java.util.Iterator<java.lang.Short> iterator() {
                return new Iterator(this);
            }

            @Override
            public String toString() {
                return stream().map(String::valueOf).collect(Collectors.joining(","));
            }

        }

        public static final class Builder extends ListBuilder {

            public Builder(GenericSegmentBuilder segment, int ptr,
                    int elementCount, int step,
                    int structDataSize, short structPointerCount) {
                super(segment, ptr, elementCount, step, structDataSize, structPointerCount);
            }

            public short get(int index) {
                return _getShortElement(index);
            }

            public void set(int index, short value) {
                _setShortElement(index, value);
            }

            public final Reader asReader() {
                return new Reader(this.segment, this.ptr, this.elementCount, this.step,
                        this.structDataSize, this.structPointerCount,
                        java.lang.Integer.MAX_VALUE);
            }
        }
    }

    public static class Int {

        public static final class Factory extends ListFactory<Builder, Reader> {

            Factory() {
                super(ElementSize.FOUR_BYTES);
            }

            @Override
            public final Reader constructReader(SegmentDataContainer segment,
                    int ptr,
                    int elementCount, int step,
                    int structDataSize, short structPointerCount,
                    int nestingLimit) {
                return new Reader(segment, ptr, elementCount, step, structDataSize, structPointerCount, nestingLimit);
            }

            @Override
            public final Builder constructBuilder(GenericSegmentBuilder segment,
                    int ptr,
                    int elementCount, int step,
                    int structDataSize, short structPointerCount) {
                return new Builder(segment, ptr, elementCount, step, structDataSize, structPointerCount);
            }

        }
        public static final Factory factory = new Factory();

        public static final class Reader extends ListReader implements Collection<Integer> {

            public Reader(SegmentDataContainer segment,
                    int ptr,
                    int elementCount, int step,
                    int structDataSize, short structPointerCount,
                    int nestingLimit) {
                super(segment, ptr, elementCount, step, structDataSize, structPointerCount, nestingLimit);
            }

            public int get(int index) {
                return _getIntElement(index);
            }

            @Override
            public boolean isEmpty() {
                return elementCount == 0;
            }

            @Override
            public boolean contains(Object o) {
                return stream().anyMatch(o::equals);
            }

            @Override
            public Object[] toArray() {
                return stream().collect(Collectors.toList()).toArray();
            }

            @Override
            public <T> T[] toArray(T[] a) {
                return stream().collect(Collectors.toList()).toArray(a);
            }

            @Override
            public boolean add(Integer e) {
                throw new UnsupportedOperationException("This collection is immutable");
            }

            @Override
            public boolean remove(Object o) {
                throw new UnsupportedOperationException("This collection is immutable");
            }

            @Override
            public boolean containsAll(Collection<?> c) {
                return stream().collect(Collectors.toList()).containsAll(c);
            }

            @Override
            public boolean addAll(Collection<? extends Integer> c) {
                throw new UnsupportedOperationException("This collection is immutable");
            }

            @Override
            public boolean removeAll(Collection<?> c) {
                throw new UnsupportedOperationException("This collection is immutable");
            }

            @Override
            public boolean retainAll(Collection<?> c) {
                throw new UnsupportedOperationException("This collection is immutable");
            }

            @Override
            public void clear() {
                throw new UnsupportedOperationException("This collection is immutable");
            }

            public Stream<Integer> stream() {
                return StreamSupport.stream(Spliterators.spliterator(this.iterator(), elementCount,
                        Spliterator.SIZED & Spliterator.IMMUTABLE
                ), false);
            }

            public final class Iterator implements java.util.Iterator<Integer> {

                public Reader list;
                public int idx = 0;

                public Iterator(Reader list) {
                    this.list = list;
                }

                @Override
                public Integer next() {
                    return get(idx++);
                }

                @Override
                public boolean hasNext() {
                    return idx < list.size();
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            }

            @Override
            public java.util.Iterator<Integer> iterator() {
                return new Iterator(this);
            }

            @Override
            public String toString() {
                return stream().map(String::valueOf).collect(Collectors.joining(","));
            }

        }

        public static final class Builder extends ListBuilder {

            public Builder(GenericSegmentBuilder segment, int ptr,
                    int elementCount, int step,
                    int structDataSize, short structPointerCount) {
                super(segment, ptr, elementCount, step, structDataSize, structPointerCount);
            }

            public int get(int index) {
                return _getIntElement(index);
            }

            public void set(int index, int value) {
                _setIntElement(index, value);
            }

            public final Reader asReader() {
                return new Reader(this.segment, this.ptr, this.elementCount, this.step,
                        this.structDataSize, this.structPointerCount,
                        java.lang.Integer.MAX_VALUE);
            }
        }
    }

    public static class Float {

        public static final class Factory extends ListFactory<Builder, Reader> {

            Factory() {
                super(ElementSize.FOUR_BYTES);
            }

            @Override
            public final Reader constructReader(SegmentDataContainer segment,
                    int ptr,
                    int elementCount, int step,
                    int structDataSize, short structPointerCount,
                    int nestingLimit) {
                return new Reader(segment, ptr, elementCount, step, structDataSize, structPointerCount, nestingLimit);
            }

            @Override
            public final Builder constructBuilder(GenericSegmentBuilder segment,
                    int ptr,
                    int elementCount, int step,
                    int structDataSize, short structPointerCount) {
                return new Builder(segment, ptr, elementCount, step, structDataSize, structPointerCount);
            }
        }
        public static final Factory factory = new Factory();

        public static final class Reader extends ListReader implements Collection<java.lang.Float> {

            public Reader(SegmentDataContainer segment,
                    int ptr,
                    int elementCount, int step,
                    int structDataSize, short structPointerCount,
                    int nestingLimit) {
                super(segment, ptr, elementCount, step, structDataSize, structPointerCount, nestingLimit);
            }

            public float get(int index) {
                return _getFloatElement(index);
            }

            @Override
            public boolean isEmpty() {
                return elementCount == 0;
            }

            @Override
            public boolean contains(Object o) {
                return stream().anyMatch(o::equals);
            }

            @Override
            public Object[] toArray() {
                return stream().collect(Collectors.toList()).toArray();
            }

            @Override
            public <T> T[] toArray(T[] a) {
                return stream().collect(Collectors.toList()).toArray(a);
            }

            @Override
            public boolean add(java.lang.Float e) {
                throw new UnsupportedOperationException("This collection is immutable");
            }

            @Override
            public boolean remove(Object o) {
                throw new UnsupportedOperationException("This collection is immutable");
            }

            @Override
            public boolean containsAll(Collection<?> c) {
                return stream().collect(Collectors.toList()).containsAll(c);
            }

            @Override
            public boolean addAll(Collection<? extends java.lang.Float> c) {
                throw new UnsupportedOperationException("This collection is immutable");
            }

            @Override
            public boolean removeAll(Collection<?> c) {
                throw new UnsupportedOperationException("This collection is immutable");
            }

            @Override
            public boolean retainAll(Collection<?> c) {
                throw new UnsupportedOperationException("This collection is immutable");
            }

            @Override
            public void clear() {
                throw new UnsupportedOperationException("This collection is immutable");
            }

            public Stream<java.lang.Float> stream() {
                return StreamSupport.stream(Spliterators.spliterator(this.iterator(), elementCount,
                        Spliterator.SIZED & Spliterator.IMMUTABLE
                ), false);
            }

            public final class Iterator implements java.util.Iterator<java.lang.Float> {

                public Reader list;
                public int idx = 0;

                public Iterator(Reader list) {
                    this.list = list;
                }

                @Override
                public java.lang.Float next() {
                    return get(idx++);
                }

                @Override
                public boolean hasNext() {
                    return idx < list.size();
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            }

            @Override
            public java.util.Iterator<java.lang.Float> iterator() {
                return new Iterator(this);
            }

            @Override
            public String toString() {
                return stream().map(String::valueOf).collect(Collectors.joining(","));
            }

        }

        public static final class Builder extends ListBuilder {

            public Builder(GenericSegmentBuilder segment, int ptr,
                    int elementCount, int step,
                    int structDataSize, short structPointerCount) {
                super(segment, ptr, elementCount, step, structDataSize, structPointerCount);
            }

            public float get(int index) {
                return _getFloatElement(index);
            }

            public void set(int index, float value) {
                _setFloatElement(index, value);
            }

            public final Reader asReader() {
                return new Reader(this.segment, this.ptr, this.elementCount, this.step,
                        this.structDataSize, this.structPointerCount,
                        java.lang.Integer.MAX_VALUE);
            }
        }
    }

    public static class Long {

        public static final class Factory extends ListFactory<Builder, Reader> {

            Factory() {
                super(ElementSize.EIGHT_BYTES);
            }

            @Override
            public final Reader constructReader(SegmentDataContainer segment,
                    int ptr,
                    int elementCount, int step,
                    int structDataSize, short structPointerCount,
                    int nestingLimit) {
                return new Reader(segment, ptr, elementCount, step, structDataSize, structPointerCount, nestingLimit);
            }

            @Override
            public final Builder constructBuilder(GenericSegmentBuilder segment,
                    int ptr,
                    int elementCount, int step,
                    int structDataSize, short structPointerCount) {
                return new Builder(segment, ptr, elementCount, step, structDataSize, structPointerCount);
            }
        }
        public static final Factory factory = new Factory();

        public static final class Reader extends ListReader implements Collection<java.lang.Long> {

            public Reader(SegmentDataContainer segment,
                    int ptr,
                    int elementCount, int step,
                    int structDataSize, short structPointerCount,
                    int nestingLimit) {
                super(segment, ptr, elementCount, step, structDataSize, structPointerCount, nestingLimit);
            }

            public long get(int index) {
                return _getLongElement(index);
            }

            @Override
            public boolean isEmpty() {
                return elementCount == 0;
            }

            @Override
            public boolean contains(Object o) {
                return stream().anyMatch(o::equals);
            }

            @Override
            public Object[] toArray() {
                return stream().collect(Collectors.toList()).toArray();
            }

            @Override
            public <T> T[] toArray(T[] a) {
                return stream().collect(Collectors.toList()).toArray(a);
            }

            @Override
            public boolean add(java.lang.Long e) {
                throw new UnsupportedOperationException("This collection is immutable");
            }

            @Override
            public boolean remove(Object o) {
                throw new UnsupportedOperationException("This collection is immutable");
            }

            @Override
            public boolean containsAll(Collection<?> c) {
                return stream().collect(Collectors.toList()).containsAll(c);
            }

            @Override
            public boolean addAll(Collection<? extends java.lang.Long> c) {
                throw new UnsupportedOperationException("This collection is immutable");
            }

            @Override
            public boolean removeAll(Collection<?> c) {
                throw new UnsupportedOperationException("This collection is immutable");
            }

            @Override
            public boolean retainAll(Collection<?> c) {
                throw new UnsupportedOperationException("This collection is immutable");
            }

            @Override
            public void clear() {
                throw new UnsupportedOperationException("This collection is immutable");
            }

            public Stream<java.lang.Long> stream() {
                return StreamSupport.stream(Spliterators.spliterator(this.iterator(), elementCount,
                        Spliterator.SIZED & Spliterator.IMMUTABLE
                ), false);
            }

            public final class Iterator implements java.util.Iterator<java.lang.Long> {

                public Reader list;
                public int idx = 0;

                public Iterator(Reader list) {
                    this.list = list;
                }

                @Override
                public java.lang.Long next() {
                    return get(idx++);
                }

                @Override
                public boolean hasNext() {
                    return idx < list.size();
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            }

            @Override
            public java.util.Iterator<java.lang.Long> iterator() {
                return new Iterator(this);
            }

            @Override
            public String toString() {
                return stream().map(String::valueOf).collect(Collectors.joining(","));
            }

        }

        public static final class Builder extends ListBuilder {

            public Builder(GenericSegmentBuilder segment, int ptr,
                    int elementCount, int step,
                    int structDataSize, short structPointerCount) {
                super(segment, ptr, elementCount, step, structDataSize, structPointerCount);
            }

            public long get(int index) {
                return _getLongElement(index);
            }

            public void set(int index, long value) {
                _setLongElement(index, value);
            }

            public final Reader asReader() {
                return new Reader(this.segment, this.ptr, this.elementCount, this.step,
                        this.structDataSize, this.structPointerCount,
                        java.lang.Integer.MAX_VALUE);
            }
        }
    }

    public static class Double {

        public static final class Factory extends ListFactory<Builder, Reader> {

            Factory() {
                super(ElementSize.EIGHT_BYTES);
            }

            @Override
            public final Reader constructReader(SegmentDataContainer segment,
                    int ptr,
                    int elementCount, int step,
                    int structDataSize, short structPointerCount,
                    int nestingLimit) {
                return new Reader(segment, ptr, elementCount, step, structDataSize, structPointerCount, nestingLimit);
            }

            @Override
            public final Builder constructBuilder(GenericSegmentBuilder segment,
                    int ptr,
                    int elementCount, int step,
                    int structDataSize, short structPointerCount) {
                return new Builder(segment, ptr, elementCount, step, structDataSize, structPointerCount);
            }
        }
        public static final Factory factory = new Factory();

        public static final class Reader extends ListReader implements Collection<java.lang.Double> {

            public Reader(SegmentDataContainer segment,
                    int ptr,
                    int elementCount, int step,
                    int structDataSize, short structPointerCount,
                    int nestingLimit) {
                super(segment, ptr, elementCount, step, structDataSize, structPointerCount, nestingLimit);
            }

            public double get(int index) {
                return _getDoubleElement(index);
            }

            @Override
            public boolean isEmpty() {
                return elementCount == 0;
            }

            @Override
            public boolean contains(Object o) {
                return stream().anyMatch(o::equals);
            }

            @Override
            public Object[] toArray() {
                return stream().collect(Collectors.toList()).toArray();
            }

            @Override
            public <T> T[] toArray(T[] a) {
                return stream().collect(Collectors.toList()).toArray(a);
            }

            @Override
            public boolean add(java.lang.Double e) {
                throw new UnsupportedOperationException("This collection is immutable");
            }

            @Override
            public boolean remove(Object o) {
                throw new UnsupportedOperationException("This collection is immutable");
            }

            @Override
            public boolean containsAll(Collection<?> c) {
                return stream().collect(Collectors.toList()).containsAll(c);
            }

            @Override
            public boolean addAll(Collection<? extends java.lang.Double> c) {
                throw new UnsupportedOperationException("This collection is immutable");
            }

            @Override
            public boolean removeAll(Collection<?> c) {
                throw new UnsupportedOperationException("This collection is immutable");
            }

            @Override
            public boolean retainAll(Collection<?> c) {
                throw new UnsupportedOperationException("This collection is immutable");
            }

            @Override
            public void clear() {
                throw new UnsupportedOperationException("This collection is immutable");
            }

            public Stream<java.lang.Double> stream() {
                return StreamSupport.stream(Spliterators.spliterator(this.iterator(), elementCount,
                        Spliterator.SIZED & Spliterator.IMMUTABLE
                ), false);
            }

            public final class Iterator implements java.util.Iterator<java.lang.Double> {

                public Reader list;
                public int idx = 0;

                public Iterator(Reader list) {
                    this.list = list;
                }

                @Override
                public java.lang.Double next() {
                    return get(idx++);
                }

                @Override
                public boolean hasNext() {
                    return idx < list.size();
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            }

            @Override
            public java.util.Iterator<java.lang.Double> iterator() {
                return new Iterator(this);
            }

            @Override
            public String toString() {
                return stream().map(String::valueOf).collect(Collectors.joining(","));
            }

        }

        public static final class Builder extends ListBuilder {

            public Builder(GenericSegmentBuilder segment, int ptr,
                    int elementCount, int step,
                    int structDataSize, short structPointerCount) {
                super(segment, ptr, elementCount, step, structDataSize, structPointerCount);
            }

            public double get(int index) {
                return _getDoubleElement(index);
            }

            public void set(int index, double value) {
                _setDoubleElement(index, value);
            }

            public final Reader asReader() {
                return new Reader(this.segment, this.ptr, this.elementCount, this.step,
                        this.structDataSize, this.structPointerCount,
                        java.lang.Integer.MAX_VALUE);
            }
        }
    }
}
