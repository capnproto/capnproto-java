@0xb365fb00cc89383b;

using Java = import "/capnp/java.capnp";
$Java.package("org.capnproto.rpctest");
$Java.outerClassname("Test");

enum TestEnum {
  foo @0;
  bar @1;
  baz @2;
  qux @3;
  quux @4;
  corge @5;
  grault @6;
  garply @7;
}

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
  structField    @14 : TestAllTypes;
  enumField      @15 : TestEnum;
  interfaceField @16 : Void;  # TODO

  voidList      @17 : List(Void);
  boolList      @18 : List(Bool);
  int8List      @19 : List(Int8);
  int16List     @20 : List(Int16);
  int32List     @21 : List(Int32);
  int64List     @22 : List(Int64);
  uInt8List     @23 : List(UInt8);
  uInt16List    @24 : List(UInt16);
  uInt32List    @25 : List(UInt32);
  uInt64List    @26 : List(UInt64);
  float32List   @27 : List(Float32);
  float64List   @28 : List(Float64);
  textList      @29 : List(Text);
  dataList      @30 : List(Data);
  structList    @31 : List(TestAllTypes);
  enumList      @32 : List(TestEnum);
  interfaceList @33 : List(Void);  # TODO
}

struct TestAnyPointer {
  anyPointerField @0 :AnyPointer;

  # Do not add any other fields here!  Some tests rely on anyPointerField being the last pointer
  # in the struct.
}

#struct TestAnyOthers {
#  anyStructField @0 :AnyStruct;
#  anyListField @1 :AnyList;
#  capabilityField @2 :Capability;
#}

struct TestOutOfOrder {
  foo @3 :Text;
  bar @2 :Text;
  baz @8 :Text;
  qux @0 :Text;
  quux @6 :Text;
  corge @4 :Text;
  grault @1 :Text;
  garply @7 :Text;
  waldo @5 :Text;
}

struct TestUnnamedUnion {
  before @0 :Text;

  union {
    foo @1 :UInt16;
    bar @3 :UInt32;
  }

  middle @2 :UInt16;

  after @4 :Text;
}

struct TestUnionInUnion {
  # There is no reason to ever do this.
  outer :union {
    inner :union {
      foo @0 :Int32;
      bar @1 :Int32;
    }
    baz @2 :Int32;
  }
}

struct TestGroups {
  groups :union {
    foo :group {
      corge @0 :Int32;
      grault @2 :Int64;
      garply @8 :Text;
    }
    bar :group {
      corge @3 :Int32;
      grault @4 :Text;
      garply @5 :Int64;
    }
    baz :group {
      corge @1 :Int32;
      grault @6 :Text;
      garply @7 :Text;
    }
  }
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

interface TestStreaming {
    doStreamI @0 (i: UInt32) -> stream;
    doStreamJ @1 (j: UInt32) -> stream;
    finishStream @2 () -> (totalI :UInt32, totalJ: UInt32);
    # Test streaming. finishStream() returns the totals of the values streamed to the other calls.
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
