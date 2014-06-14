package org.capnproto.benchmark;

public class Common {
    public static class FastRand {
        public static final int a = 1664525;
        public static final int c = 1013904223;
        public int state;

        public FastRand() {
            this.state = 1013904223;
        }

        public int next_int() {
            this.state = this.a * this.state + c;
            return this.state;
        }
    }

}
