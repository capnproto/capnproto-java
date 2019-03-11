using Java = import "/capnp/java.capnp";
$Java.package("org.capnproto.amazing");
$Java.outerClassname("MessageJava");

@0xf29153d00f1006e9;

struct Message {
    time            @0 :UInt64;
    # the message sent time

    union {
        data        @1 :Data;
        metaData       :group {
            name    @2 :Text;
            size    @3 :Int64;
        }
        messages    @4 :List(Message);
        flag        @5 :Flag;
    }
}

enum Flag {
    start @0;
    end @1;
    fileNotFound @2;
}
