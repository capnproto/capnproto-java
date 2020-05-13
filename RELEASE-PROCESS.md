How to release
==============

* Make sure that `~/.m2/settings.xml` has your Sonatype credentials.
* Increment the version number in the various pom.xml files.
* `mvn -pl runtime clean deploy -P release`
* The package should be available within ten minutes,
  but [it might take up to two hours](https://issues.sonatype.org/browse/OSSRH-13527?focusedCommentId=338745&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-338745) for it
  to become available at https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.capnproto%22
* Make a git tag.
