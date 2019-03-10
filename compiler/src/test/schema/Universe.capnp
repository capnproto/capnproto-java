using Java = import "/capnp/java.capnp";
$Java.package("org.capnproto.amazing");
$Java.outerClassname("Everything");

@0xf29153d00f1006e9;

struct Universe {
    location        @0: Location;
}

struct SolarSystem {
  sun               @0 : Sun;
  planets           @1 : List(Planet);
}

struct Sun {
  name              @0 : Text;
  meta              @1 : MetaData;
}

struct Planet {
  name              @0 : Text;
  meta              @1 : MetaData;
  moons             @2 : List(Moon);
}

struct Moon {
  name              @0 : Text;
  meta              @1 : MetaData;
}


struct MetaData {
  radius            @0 : UInt64;
  weight            @1 : UInt64;
  temperature       @2 : Int16;
}

struct Location {
  x                 @0 : UInt64;
  y                 @1 : UInt64;
  z                 @2 : UInt64;
  union {
       vacuum       @3 : Void;
       solarSystem  @4 : SolarSystem;

  }
}