package org.capnproto;

public class PrimitiveList {
    public static class Void {
        public static final class Factory extends ListFactory<Builder, Reader> {
            Factory() {super (FieldSize.VOID); }

            public final Reader constructReader(SegmentReader segment,
                                                  int ptr,
                                                  int elementCount, int step,
                                                  int structDataSize, short structPointerCount,
                                                  int nestingLimit) {
                return new Reader(segment, ptr, elementCount, step, structDataSize, structPointerCount, nestingLimit);
            }

            public final Builder constructBuilder(SegmentBuilder segment,
                                                    int ptr,
                                                    int elementCount, int step,
                                                    int structDataSize, short structPointerCount) {
                return new Builder(segment, ptr, elementCount, step, structDataSize, structPointerCount);
            }

        }
        public static final Factory factory = new Factory();

        public static final class Reader extends ListReader {
            public Reader(SegmentReader segment,
                          int ptr,
                          int elementCount, int step,
                          int structDataSize, short structPointerCount,
                          int nestingLimit) {
                super(segment, ptr, elementCount, step, structDataSize, structPointerCount, nestingLimit);
            }

            public org.capnproto.Void get(int index) {
                return org.capnproto.Void.VOID;
            }
        }

        public static final class Builder extends ListBuilder {
            public Builder(SegmentBuilder segment, int ptr,
                           int elementCount, int step,
                           int structDataSize, short structPointerCount){
                super(segment, ptr, elementCount, step, structDataSize, structPointerCount);
            }
        }
    }

    public static class Boolean {
        public static final class Factory extends ListFactory<Builder, Reader> {
            Factory() {super (FieldSize.BIT); }
            public final Reader constructReader(SegmentReader segment,
                                                  int ptr,
                                                  int elementCount, int step,
                                                  int structDataSize, short structPointerCount,
                                                  int nestingLimit) {
                return new Reader(segment, ptr, elementCount, step, structDataSize, structPointerCount, nestingLimit);
            }

            public final Builder constructBuilder(SegmentBuilder segment,
                                                    int ptr,
                                                    int elementCount, int step,
                                                    int structDataSize, short structPointerCount) {
                return new Builder(segment, ptr, elementCount, step, structDataSize, structPointerCount);
            }
        }
        public static final Factory factory = new Factory();

        public static final class Reader extends ListReader {
            public Reader(SegmentReader segment,
                          int ptr,
                          int elementCount, int step,
                          int structDataSize, short structPointerCount,
                          int nestingLimit) {
                super(segment, ptr, elementCount, step, structDataSize, structPointerCount, nestingLimit);
            }

            public boolean get(int index) {
                return _getBooleanElement(index);
            }
        }

        public static final class Builder extends ListBuilder {
            public Builder(SegmentBuilder segment, int ptr,
                           int elementCount, int step,
                           int structDataSize, short structPointerCount){
                super(segment, ptr, elementCount, step, structDataSize, structPointerCount);
            }

            public boolean get(int index) {
                return _getBooleanElement(index);
            }

            public void set(int index, boolean value) {
                _setBooleanElement(index, value);
            }
        }
    }

    public static class Byte {
        public static final class Factory extends ListFactory<Builder, Reader> {
            Factory() {super (FieldSize.BYTE); }
            public final Reader constructReader(SegmentReader segment,
                                                  int ptr,
                                                  int elementCount, int step,
                                                  int structDataSize, short structPointerCount,
                                                  int nestingLimit) {
                return new Reader(segment, ptr, elementCount, step, structDataSize, structPointerCount, nestingLimit);
            }

            public final Builder constructBuilder(SegmentBuilder segment,
                                                    int ptr,
                                                    int elementCount, int step,
                                                    int structDataSize, short structPointerCount) {
                return new Builder(segment, ptr, elementCount, step, structDataSize, structPointerCount);
            }
        }
        public static final Factory factory = new Factory();

        public static final class Reader extends ListReader {
            public Reader(SegmentReader segment,
                          int ptr,
                          int elementCount, int step,
                          int structDataSize, short structPointerCount,
                          int nestingLimit) {
                super(segment, ptr, elementCount, step, structDataSize, structPointerCount, nestingLimit);
            }

            public byte get(int index) {
                return _getByteElement(index);
            }
        }

        public static final class Builder extends ListBuilder {
            public Builder(SegmentBuilder segment, int ptr,
                           int elementCount, int step,
                           int structDataSize, short structPointerCount){
                super(segment, ptr, elementCount, step, structDataSize, structPointerCount);
            }

            public byte get(int index) {
                return _getByteElement(index);
            }

            public void set(int index, byte value) {
                _setByteElement(index, value);
            }
        }

    }

    public static class Short {
        public static final class Factory extends ListFactory<Builder, Reader> {
            Factory() {super (FieldSize.TWO_BYTES); }
            public final Reader constructReader(SegmentReader segment,
                                                  int ptr,
                                                  int elementCount, int step,
                                                  int structDataSize, short structPointerCount,
                                                  int nestingLimit) {
                return new Reader(segment, ptr, elementCount, step, structDataSize, structPointerCount, nestingLimit);
            }

            public final Builder constructBuilder(SegmentBuilder segment,
                                                    int ptr,
                                                    int elementCount, int step,
                                                    int structDataSize, short structPointerCount) {
                return new Builder(segment, ptr, elementCount, step, structDataSize, structPointerCount);
            }

        }
        public static final Factory factory = new Factory();

        public static final class Reader extends ListReader {
            public Reader(SegmentReader segment,
                          int ptr,
                          int elementCount, int step,
                          int structDataSize, short structPointerCount,
                          int nestingLimit) {
                super(segment, ptr, elementCount, step, structDataSize, structPointerCount, nestingLimit);
            }

            public short get(int index) {
                return _getShortElement(index);
            }
        }

        public static final class Builder extends ListBuilder {
            public Builder(SegmentBuilder segment, int ptr,
                           int elementCount, int step,
                           int structDataSize, short structPointerCount){
                super(segment, ptr, elementCount, step, structDataSize, structPointerCount);
            }

            public short get(int index) {
                return _getShortElement(index);
            }

            public void set(int index, short value) {
                _setShortElement(index, value);
            }
        }

    }

    public static class Int {
        public static final class Factory extends ListFactory<Builder, Reader> {
            Factory() {super (FieldSize.FOUR_BYTES); }
            public final Reader constructReader(SegmentReader segment,
                                                  int ptr,
                                                  int elementCount, int step,
                                                  int structDataSize, short structPointerCount,
                                                  int nestingLimit) {
                return new Reader(segment, ptr, elementCount, step, structDataSize, structPointerCount, nestingLimit);
            }

            public final Builder constructBuilder(SegmentBuilder segment,
                                                    int ptr,
                                                    int elementCount, int step,
                                                    int structDataSize, short structPointerCount) {
                return new Builder(segment, ptr, elementCount, step, structDataSize, structPointerCount);
            }

        }
        public static final Factory factory = new Factory();

        public static final class Reader extends ListReader {
            public Reader(SegmentReader segment,
                          int ptr,
                          int elementCount, int step,
                          int structDataSize, short structPointerCount,
                          int nestingLimit) {
                super(segment, ptr, elementCount, step, structDataSize, structPointerCount, nestingLimit);
            }

            public int get(int index) {
                return _getIntElement(index);
            }
        }

        public static final class Builder extends ListBuilder {
            public Builder(SegmentBuilder segment, int ptr,
                           int elementCount, int step,
                           int structDataSize, short structPointerCount){
                super(segment, ptr, elementCount, step, structDataSize, structPointerCount);
            }

            public int get(int index) {
                return _getIntElement(index);
            }

            public void set(int index, int value) {
                _setIntElement(index, value);
            }
        }
    }

    public static class Float {
        public static final class Factory extends ListFactory<Builder, Reader> {
            Factory() {super (FieldSize.FOUR_BYTES); }
            public final Reader constructReader(SegmentReader segment,
                                                  int ptr,
                                                  int elementCount, int step,
                                                  int structDataSize, short structPointerCount,
                                                  int nestingLimit) {
                return new Reader(segment, ptr, elementCount, step, structDataSize, structPointerCount, nestingLimit);
            }

            public final Builder constructBuilder(SegmentBuilder segment,
                                                    int ptr,
                                                    int elementCount, int step,
                                                    int structDataSize, short structPointerCount) {
                return new Builder(segment, ptr, elementCount, step, structDataSize, structPointerCount);
            }
        }
        public static final Factory factory = new Factory();

        public static final class Reader extends ListReader {
            public Reader(SegmentReader segment,
                          int ptr,
                          int elementCount, int step,
                          int structDataSize, short structPointerCount,
                          int nestingLimit) {
                super(segment, ptr, elementCount, step, structDataSize, structPointerCount, nestingLimit);
            }

            public float get(int index) {
                return _getFloatElement(index);
            }
        }

        public static final class Builder extends ListBuilder {
            public Builder(SegmentBuilder segment, int ptr,
                           int elementCount, int step,
                           int structDataSize, short structPointerCount){
                super(segment, ptr, elementCount, step, structDataSize, structPointerCount);
            }

            public float get(int index) {
                return _getFloatElement(index);
            }

            public void set(int index, float value) {
                _setFloatElement(index, value);
            }
        }
    }


    public static class Long {
        public static final class Factory extends ListFactory<Builder, Reader> {
            Factory() {super (FieldSize.EIGHT_BYTES); }
            public final Reader constructReader(SegmentReader segment,
                                                  int ptr,
                                                  int elementCount, int step,
                                                  int structDataSize, short structPointerCount,
                                                  int nestingLimit) {
                return new Reader(segment, ptr, elementCount, step, structDataSize, structPointerCount, nestingLimit);
            }

            public final Builder constructBuilder(SegmentBuilder segment,
                                                    int ptr,
                                                    int elementCount, int step,
                                                    int structDataSize, short structPointerCount) {
                return new Builder(segment, ptr, elementCount, step, structDataSize, structPointerCount);
            }
        }
        public static final Factory factory = new Factory();

        public static final class Reader extends ListReader {
            public Reader(SegmentReader segment,
                          int ptr,
                          int elementCount, int step,
                          int structDataSize, short structPointerCount,
                          int nestingLimit) {
                super(segment, ptr, elementCount, step, structDataSize, structPointerCount, nestingLimit);
            }

            public long get(int index) {
                return _getLongElement(index);
            }
        }

        public static final class Builder extends ListBuilder {
            public Builder(SegmentBuilder segment, int ptr,
                           int elementCount, int step,
                           int structDataSize, short structPointerCount){
                super(segment, ptr, elementCount, step, structDataSize, structPointerCount);
            }

            public long get(int index) {
                return _getLongElement(index);
            }

            public void set(int index, long value) {
                _setLongElement(index, value);
            }
        }
    }

    public static class Double {
        public static final class Factory extends ListFactory<Builder, Reader> {
            Factory() {super (FieldSize.EIGHT_BYTES); }
            public final Reader constructReader(SegmentReader segment,
                                                  int ptr,
                                                  int elementCount, int step,
                                                  int structDataSize, short structPointerCount,
                                                  int nestingLimit) {
                return new Reader(segment, ptr, elementCount, step, structDataSize, structPointerCount, nestingLimit);
            }

            public final Builder constructBuilder(SegmentBuilder segment,
                                                    int ptr,
                                                    int elementCount, int step,
                                                    int structDataSize, short structPointerCount) {
                return new Builder(segment, ptr, elementCount, step, structDataSize, structPointerCount);
            }
        }
        public static final Factory factory = new Factory();

        public static final class Reader extends ListReader {
            public Reader(SegmentReader segment,
                          int ptr,
                          int elementCount, int step,
                          int structDataSize, short structPointerCount,
                          int nestingLimit) {
                super(segment, ptr, elementCount, step, structDataSize, structPointerCount, nestingLimit);
            }

            public double get(int index) {
                return _getDoubleElement(index);
            }
        }

        public static final class Builder extends ListBuilder {
            public Builder(SegmentBuilder segment, int ptr,
                           int elementCount, int step,
                           int structDataSize, short structPointerCount){
                super(segment, ptr, elementCount, step, structDataSize, structPointerCount);
            }

            public double get(int index) {
                return _getDoubleElement(index);
            }

            public void set(int index, double value) {
                _setDoubleElement(index, value);
            }
        }
    }
}
