@0xd693321951fee8f3;

using Java = import "/capnp/java.capnp";
$Java.package("org.capnproto.testimport");
$Java.outerClassname("TestImport");


using import "test.capnp".TestAllTypes;

struct Foo {
  importedStruct @0 :TestAllTypes;
}
