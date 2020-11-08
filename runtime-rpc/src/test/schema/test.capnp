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

struct TestSturdyRef {
  hostId @0 :TestSturdyRefHostId;
  objectId @1 :AnyPointer;
}

struct TestSturdyRefHostId {
  host @0 :Text;
}

struct TestSturdyRefObjectId {
  tag @0 :Tag;
  enum Tag {
    testInterface @0;
    testExtends @1;
    testPipeline @2;
    testTailCallee @3;
    testTailCaller @4;
    testMoreStuff @5;
  }
}

struct TestProvisionId {}
struct TestRecipientId {}
struct TestThirdPartyCapId {}
struct TestJoinResult {}

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

interface TestCallOrder {
  getCallSequence @0 (expected: UInt32) -> (n: UInt32);
  # First call returns 0, next returns 1, ...
  #
  # The input `expected` is ignored but useful for disambiguating debug logs.
}

interface TestTailCallee {
  struct TailResult {
    i @0 :UInt32;
    t @1 :Text;
    c @2 :TestCallOrder;
  }

  foo @0 (i :Int32, t :Text) -> TailResult;
}

interface TestTailCaller {
  foo @0 (i :Int32, callee :TestTailCallee) -> TestTailCallee.TailResult;
}

interface TestHandle {}

interface TestMoreStuff extends(TestCallOrder) {
  # Catch-all type that contains lots of testing methods.

  callFoo @0 (cap :TestInterface) -> (s: Text);
  # Call `cap.foo()`, check the result, and return "bar".

  callFooWhenResolved @1 (cap :TestInterface) -> (s: Text);
  # Like callFoo but waits for `cap` to resolve first.

  neverReturn @2 (cap :TestInterface) -> (capCopy :TestInterface);
  # Doesn't return.  You should cancel it.

  hold @3 (cap :TestInterface) -> ();
  # Returns immediately but holds on to the capability.

  callHeld @4 () -> (s: Text);
  # Calls the capability previously held using `hold` (and keeps holding it).

  getHeld @5 () -> (cap :TestInterface);
  # Returns the capability previously held using `hold` (and keeps holding it).

  echo @6 (cap :TestCallOrder) -> (cap :TestCallOrder);
  # Just returns the input cap.

  expectCancel @7 (cap :TestInterface) -> ();
  # evalLater()-loops forever, holding `cap`.  Must be canceled.

  methodWithDefaults @8 (a :Text, b :UInt32 = 123, c :Text = "foo") -> (d :Text, e :Text = "bar");

  methodWithNullDefault @12 (a :Text, b :TestInterface = null);

  getHandle @9 () -> (handle :TestHandle);
  # Get a new handle. Tests have an out-of-band way to check the current number of live handles, so
  # this can be used to test garbage collection.

  getNull @10 () -> (nullCap :TestMoreStuff);
  # Always returns a null capability.

  getEnormousString @11 () -> (str :Text);
  # Attempts to return an 100MB string. Should always fail.

  writeToFd @13 (fdCap1 :TestInterface, fdCap2 :TestInterface)
             -> (fdCap3 :TestInterface, secondFdPresent :Bool);
  # Expects fdCap1 and fdCap2 wrap socket file descriptors. Writes "foo" to the first and "bar" to
  # the second. Also creates a socketpair, writes "baz" to one end, and returns the other end.
}

struct TestGenerics(Foo, Bar) {
  foo @0 :Foo;
  rev @1 :TestGenerics(Bar, Foo);

  interface Interface(Qux) {
  }
}
