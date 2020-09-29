@0x91b57797d64253c4;

using Java = import "/capnp/java.capnp";
$Java.package("org.capnproto.demo");
$Java.outerClassname("Demo");

struct TestParams0 {
   param0 @0 :Int32;
}

struct TestResults0 {
   result0 @0 :Int32;
}

struct TestParams1 {
   param0 @0 :AnyPointer;
}

struct TestResults1 {
   result0 @0 :AnyPointer;
}


