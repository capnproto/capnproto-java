@0xb365fb00cc89383b;

using Java = import "/capnp/java.capnp";
$Java.package("org.capnproto.test");
$Java.outerClassname("Test");

struct TestAllTypes {
  voidField      @0  : Void;
  boolField      @1  : Bool;
  int8Field      @2  : Int8;
  int16Field     @3  : Int16;
  int32Field     @4  : Int32;
  int64Field     @5  : Int64;
  uInt8Field     @6  : UInt8;
  uInt16Field    @7  : UInt16;
  uInt32Field    @8  : UInt32;
  uInt64Field    @9  : UInt64;
  float32Field   @10 : Float32;
  float64Field   @11 : Float64;
  textField      @12 : Text;
  dataField      @13 : Data;
}

interface TestInterface {
  foo @0 (i :UInt32, j :Bool) -> (x :Text);
  bar @1 () -> ();
  baz @2 (s: TestAllTypes);
}

interface TestExtends extends(TestInterface) {
  qux @0 ();
  corge @1 TestAllTypes -> ();
  grault @2 () -> TestAllTypes;
}

interface TestExtends2 extends(TestExtends) {}

interface TestPipeline {
  getCap @0 (n: UInt32, inCap :TestInterface) -> (s: Text, outBox :Box);
  testPointers @1 (cap :TestInterface, obj :AnyPointer, list :List(TestInterface)) -> ();
  getAnyCap @2 (n: UInt32, inCap :Capability) -> (s: Text, outBox :AnyBox);

  struct Box {
    cap @0 :TestInterface;
  }
  struct AnyBox {
    cap @0 :Capability;
  }
}

struct TestGenerics(Foo, Bar) {
  foo @0 :Foo;
  rev @1 :TestGenerics(Bar, Foo);

  interface Interface(Qux) {
  }
}
