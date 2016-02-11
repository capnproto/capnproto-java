How to release
==============

* Increment the version number in `project/build.scala`.
* `sbt publishSigned`
* Go to https://oss.sonatype.org/#stagingRepositories and follow
  [these instructions](http://central.sonatype.org/pages/releasing-the-deployment.html).
* The package should be available within ten minutes,
  but [it might take up to two hours](https://issues.sonatype.org/browse/OSSRH-13527?focusedCommentId=338745&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-338745) for it
  to become available at https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.capnproto%22
* Make a git tag.
