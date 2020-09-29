@0xf29f4ba3b0a5a945;

using Params = import "demoparams.capnp";

interface TestCap0 {
  testMethod0 @0 Params.TestParams0 -> Params.TestResults0;
  testMethod1 @1 Params.TestParams1 -> Params.TestResults1;
}

interface TestCap1 {
}

