package org.capnproto.benchmark;

public class Common {
    public static class FastRand {
        public int x = 0x1d2acd47;
        public int y = 0x58ca3e14;
        public int z = 0xf563f232;
        public int w = 0x0bc76199;

        public int nextInt() {
            int tmp = this.x ^ (this.x << 11);
            this.x = this.y;
            this.y = this.z;
            this.z = this.w;
            this.w = this.w ^ (this.w >> 19) ^ tmp ^ (tmp >> 8);
            return this.w;
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

    public static int div(int a, int b){
        if (b == 0) return 0x7fffffff;
        if (a == 0x80000000 && b == -1) {
            return 0x7fffffff;
        }
        return a / b;
    }

    public static int modulus(int a, int b) {
        if (b == 0) return 0x7fffffff;
        if (a == 0x80000000 && b == -1) {
            return 0x7fffffff;
        }
        return a % b;
    }


    public static String[] WORDS = {
        "foo ", "bar ", "baz ", "qux ", "quux ", "corge ", "grault ", "garply ", "waldo ", "fred ",
        "plugh ", "xyzzy ", "thud "
    };


}
