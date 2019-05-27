import sbt.Keys._
import sbt._

object Build extends sbt.Build {

  lazy val root =
    project(
      id = "capnproto-java",
      base = file(".")
    ).aggregate(compiler, runtime, examples)
      .settings(cleanFiles <+= baseDirectory { base => base / "capnpc-java"})

  lazy val compiler =
    project(
      id = "compiler",
      base = file("compiler")
    ).dependsOn(runtime)
      .settings(makeCppTask)
      .settings(compile <<= compile in Compile dependsOn makeCpp)
      .settings(compileTestSchemaTask)
      .settings(test <<= test in Test dependsOn compileTestSchema)
      .settings(unmanagedSourceDirectories in Test += sourceDirectory.value / "test" / "generated")
      .settings(cleanFiles += sourceDirectory.value / "test" / "generated")

  lazy val runtime =
    project(
      id = "runtime",
      base = file("runtime")
    ).settings(publishArtifact := true)
     .settings(crossPaths := false)     // disable outputting the _<scala-version> suffix

  lazy val examples =
    project(
      id = "examples",
      base = file("examples")
    ).dependsOn(runtime, compiler)
      .settings(makeExamplesTask)
      .settings(test <<= test in Test dependsOn makeExamples)
      .settings(compile <<= compile in Compile dependsOn makeExamples)
      .settings(unmanagedSourceDirectories in Compile += sourceDirectory.value / "main" / "generated")
      .settings(cleanFiles += sourceDirectory.value / "main" / "generated")
      .settings(crossPaths := false)     // disable outputting the _<scala-version> suffix

  lazy val benchmark =
    project(
      id = "benchmark",
      base = file("benchmark")
    ).dependsOn(runtime, compiler)
     .settings(compileBenchmarkSchemaTask)
     .settings(compile <<= compile in Compile dependsOn compileBenchmarkSchema)
      .settings(unmanagedSourceDirectories in Compile += sourceDirectory.value / "main" / "generated")
      .settings(cleanFiles += sourceDirectory.value / "main" / "generated")
      .settings(crossPaths := false)     // disable outputting the _<scala-version> suffix

  def project(id: String, base: File) =
    Project(
      id = id,
      base = base,
      settings =
        Project.defaultSettings ++
        Shared.settings ++
        Seq(libraryDependencies ++= Shared.testDeps)
    ).configs(IntegrationTest)

  val compileTestSchema = taskKey[Unit]("Run capnpc-java on test schema")
  val compileTestSchemaTask = compileTestSchema := {
    val result0 = "mkdir -p compiler/src/test/generated".!!
    val result = "capnp compile -I compiler/src/main/schema/  --src-prefix=compiler/src/test/schema/ -o./capnpc-java:compiler/src/test/generated compiler/src/test/schema/test.capnp compiler/src/test/schema/test-import.capnp".!!
    println(s"**** CodeGen for test.capnp started\n$result\n**** CodeGen complete.");
  }

  val makeCpp = taskKey[Unit]("Run make against the C++ code to create the Java code generator")
  val makeCppTask = makeCpp := {
    val makeResult = "make capnpc-java".!!
    println(s"**** C++ Build Started\n$makeResult\n**** C++ Build Complete")
  }

  val makeExamples = taskKey[Unit]("Run capnpc-java compiler against the addressbook schema")
  val makeExamplesTask = makeExamples := {
    Thread.sleep(1000)
    val makeResult = "make addressbook".!!
    println(s"**** CodeGen for Addressbook Started\n$makeResult\n**** CodeGen for Addressbook Complete")
  }

  val compileBenchmarkSchema = taskKey[Unit]("Run capnpc-java on benchmark schema")
  val compileBenchmarkSchemaTask = compileBenchmarkSchema := {
    val result0 = "mkdir -p benchmark/src/main/generated".!!
    val result = "capnp compile -I compiler/src/main/schema/  --src-prefix=benchmark/src/main/schema/ -o./capnpc-java:benchmark/src/main/generated benchmark/src/main/schema/eval.capnp benchmark/src/main/schema/carsales.capnp benchmark/src/main/schema/catrank.capnp".!!
    println(s"**** CodeGen for benchmark started\n$result\n**** CodeGen complete.");
  }

}

object Shared {

  val testDeps = Seq(
    "org.scalatest" %% "scalatest" % "2.1.6" % "it,test"
//    "org.scalacheck" %% "scalacheck" % "1.11.4" % "it,test",
  )

  val settings = Seq(
    scalaVersion := "2.11.0",
    scalacOptions := Seq(
      "-deprecation",
      "-feature",
      "-optimise",
      "-Yinline-warnings",
      "-unchecked",
      "-feature"
    ),
    javacOptions in (Compile, compile) ++= Seq("-source", "1.7", "-target", "1.7"),
    resolvers += Resolver.sonatypeRepo("snapshots"),
    resolvers += Resolver.sonatypeRepo("releases"),
    shellPrompt := ShellPrompt.buildShellPrompt,
    organization := "org.capnproto",
    publishArtifact := false,
    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (isSnapshot.value)
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases"  at nexus + "service/local/staging/deploy/maven2")
    },
    credentials += Credentials(Path.userHome / ".ivy2" / ".credentials"),
    publishMavenStyle := true,
    version := "0.1.4",
    autoScalaLibrary := false,
    publishArtifact in Test := false,
    autoScalaLibrary in test := false,
    pomIncludeRepository := { x => false },
    pomExtra := (
      <url>https://capnproto.org/</url>
      <licenses>
        <license>
          <name>MIT</name>
          <url>http://opensource.org/licenses/MIT</url>
          <distribution>repo</distribution>
        </license>
      </licenses>
      <scm>
        <url>git@github.com:capnproto/capnproto-java.git</url>
        <connection>scm:git@github.com:capnproto/capnproto-java.git</connection>
      </scm>
      <developers>
        <developer>
          <id>dwrensha</id>
          <name>David Renshaw</name>
          <url>https://github.com/dwrensha</url>
        </developer>
      </developers>
    )

  )

}

object ShellPrompt {
  object devnull extends ProcessLogger {
    def info(s: => String) {}
    def error(s: => String) {}
    def buffer[T](f: => T): T = f
  }

  def currBranch = (
    ("git status -sb" lines_! devnull headOption)
    getOrElse "-" stripPrefix "## "
    )

  val buildShellPrompt = {
    (state: State) => {
      val currProject = Project.extract(state).currentProject.id
      "[%s](%s)$ ".format(
        currProject, currBranch /*, BuildSettings.buildVersion*/
      )
    }
  }
}


