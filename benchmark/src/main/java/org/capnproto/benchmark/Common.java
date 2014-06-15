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
            // sign?
            return this.nextInt() % range;
        }

        public double nextDouble(double range) {
            // XXX sign?
            return (double) this.nextInt() * range / (double)(0xffffffffL);
        }
    }

}
