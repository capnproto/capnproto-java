How to release
==============

* Increment the version number in `project/build.scala`.
* `sbt publishSigned`
* Go to https://oss.sonatype.org/#stagingRepositories and follow
  [these instructions](http://central.sonatype.org/pages/releasing-the-deployment.html).
* Wait for a bit... https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.capnproto%22