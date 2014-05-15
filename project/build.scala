import sbt.Keys._
import sbt._

object Build extends sbt.Build {

  lazy val root =
    project(
      id = "capnproto-java",
      base = file(".")
    ).aggregate(generator, examples)

  lazy val generator =
    project(
      id = "generator",
      base = file("generator")
    ).settings(Defaults.itSettings: _*)
    .settings(compile <<= compile in Compile dependsOn(compile in Test, compile in IntegrationTest))

  lazy val examples =
    project(
      id = "examples",
      base = file("examples")
    ).dependsOn(generator)
    .settings(publish := {})
    .settings(publishLocal := {})
    .settings(fork in run := true)
    .settings(outputStrategy := Some(StdoutOutput))
    .settings(javaOptions in run ++= Seq(
      "-ms2g",
      "-mx2g",
      "-XX:+AlwaysPreTouch",
      "-XX:+TieredCompilation"
    ))

  def project(id: String, base: File) =
    Project(
      id = id,
      base = base,
      settings =
        Project.defaultSettings ++
        Shared.settings ++
        Seq(libraryDependencies ++= Shared.testDeps)
    ).configs(IntegrationTest)
}

object Shared {

  val testDeps = Seq(
    "org.scalatest" %% "scalatest" % "2.1.6" % "it,test",
    "org.scalacheck" %% "scalacheck" % "1.11.4" % "it,test"
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
