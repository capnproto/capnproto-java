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
    ).settings(makeCppTask).dependsOn(runtime)
      .settings(compile <<= compile in Compile dependsOn makeCpp)
      .settings(compileTestSchemaTask)

      .settings(test <<= test in Test dependsOn compileTestSchema )
      .settings(unmanagedSourceDirectories in Test += sourceDirectory.value / "test" / "generated")

  lazy val runtime =
    project(
      id = "runtime",
      base = file("runtime")
    )

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
    val result = "capnp compile -I compiler/src/main/cpp/  --src-prefix=compiler/src/test/schema/ -o./capnpc-java:compiler/src/test/generated compiler/src/test/schema/test.capnp".!!
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
    resolvers += Resolver.sonatypeRepo("snapshots"),
    resolvers += Resolver.sonatypeRepo("releases"),
    shellPrompt := ShellPrompt.buildShellPrompt
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
