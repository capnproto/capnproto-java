package org.capnproto.benchmark;

public class Common {
    public static class FastRand {
        public static final int a = 1664525;
        public static final int c = 1013904223;
        public int state;

        public FastRand() {
            this.state = 1013904223;
        }

        public int nextInt() {
            this.state = this.a * this.state + c;
            return this.state;
        }

        public int nextLessThan(int range) {
            // just chop off the sign bit.
            return (0x7fffffff & this.nextInt()) % range;
        }

        public double nextDouble(double range) {
            // just chop off the sign bit.
            return (double) (0x7fffffff & this.nextInt()) * range / (double)(0x7fffffff);
        }
    }

}
