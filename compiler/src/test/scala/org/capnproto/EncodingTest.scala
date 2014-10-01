package org.capnproto;

import org.capnproto.test.Test._;
import org.scalatest.FunSuite
import org.scalatest.Matchers._;

class EncodingSuite extends FunSuite {

  test("AllTypes") {
    val message = new MessageBuilder();
    val allTypes = message.initRoot(TestAllTypes.factory);
    TestUtil.initTestMessage(allTypes);
    TestUtil.checkTestMessage(allTypes);
    TestUtil.checkTestMessage(allTypes.asReader());
  }

  test("AllTypesMultiSegment") {
    val message = new MessageBuilder(5, BuilderArena.AllocationStrategy.FIXED_SIZE);
    val allTypes = message.initRoot(TestAllTypes.factory);
    TestUtil.initTestMessage(allTypes);

    TestUtil.checkTestMessage(allTypes);
    TestUtil.checkTestMessage(allTypes.asReader());
  }

  test("Defaults") {
    val message = new MessageBuilder();
    val defaults = message.initRoot(TestDefaults.factory);
    TestUtil.checkDefaultMessage(defaults);
    TestUtil.checkDefaultMessage(defaults.asReader());
  }

  test("Groups") {
    val builder = new MessageBuilder();
    val root = builder.initRoot(TestGroups.factory);

    {
      val foo = root.getGroups().initFoo();
      foo.setCorge(12345678);
      foo.setGrault(123456789012345L);
      foo.setGarply(new Text.Reader("foobar"));

      assert(12345678 == foo.getCorge());
      assert(123456789012345L == foo.getGrault());
      assert("foobar" == foo.getGarply().toString());
    }

    {
      val bar = root.getGroups.initBar();
      bar.setCorge(23456789);
      bar.setGrault(new Text.Reader("barbaz"));
      bar.setGarply(234567890123456L);

      assert(23456789 == bar.getCorge());
      assert("barbaz" == bar.getGrault().toString());
      assert(234567890123456L == bar.getGarply());
    }

    {
      val baz = root.getGroups().initBaz();
      baz.setCorge(34567890);
      baz.setGrault(new Text.Reader("bazqux"));
      baz.setGarply(new Text.Reader("quxquux"));

      assert(34567890 == baz.getCorge());
      assert("bazqux" == baz.getGrault().toString());
      assert("quxquux" == baz.getGarply().toString());
    }
  }

  test("Constants") {
    assert(Void.VOID == TestConstants.VOID_CONST);
    assert(true == TestConstants.BOOL_CONST);
    assert(-123 == TestConstants.INT8_CONST);
    assert(-12345 == TestConstants.INT16_CONST);
    assert(-12345678 == TestConstants.INT32_CONST);
    assert(-123456789012345L == TestConstants.INT64_CONST);

    assert(-22 == TestConstants.UINT8_CONST);
    assert(-19858 == TestConstants.UINT16_CONST);
    assert(-838178284 == TestConstants.UINT32_CONST);
    assert(-6101065172474983726L == TestConstants.UINT64_CONST);

    assert(1234.5f == TestConstants.FLOAT32_CONST);
    (-123e45) should equal (TestConstants.FLOAT64_CONST);

    (TestConstants.ENUM_CONST) should equal (TestEnum.CORGE);

  }

  test("GlobalConstants") {
    assert(12345 == GLOBAL_INT);
  }

  // to debug, do this:
  //Serialize.writeMessage((new java.io.FileOutputStream("/Users/dwrensha/Desktop/test.dat")).getChannel(),
  //                       message);


}
