name := """ZyberWebPlay"""

scalaVersion := "2.11.7"

EclipseKeys.withSource := true

EclipseKeys.skipParents in ThisBuild := false

organization in ThisBuild := "com.zyber"
version := "0.1.0"
scalaVersion in ThisBuild := "2.11.7"

javacOptions ++= Seq("-source", "1.8", "-target", "1.8")

//Fix for GroupDocs library issue in scala: with this groupDocs doesn't work properly, getImage for thumbnails fails
scalacOptions ++= Seq("-Yresolve-term-conflict:object")



lazy val common = (project in file("modules/common")).enablePlugins(PlayScala)

lazy val admin = (project in file("modules/admin")).enablePlugins(PlayScala).dependsOn(common)

lazy val zyberapp = (project in file("modules/zyberapp")).enablePlugins(PlayScala).dependsOn(common)

lazy val root = (project in file(".")).enablePlugins(PlayScala).aggregate(common, zyberapp,admin,codegen).dependsOn(common, zyberapp,admin,codegen )

aggregate in update := false
updateOptions := updateOptions.value.withCachedResolution(true)

lazy val codegen = (project in file("modules/codegen")).settings(
	scalaVersion := "2.11.7",
	javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
	libraryDependencies ++= List(
		  "commons-lang" % "commons-lang" % "2.6" withSources() withJavadoc(),
		  "commons-io" % "commons-io" % "2.4" withSources() withJavadoc(),
		  "com.novocode" % "junit-interface" % "0.11" % "test",
		  "com.github.javaparser" % "javaparser-core" % "2.1.0" withSources() withJavadoc() // Java parser, so we can parse the files to and AST.
      ))


lazy val generator = TaskKey[Unit]("gen-accessors")
lazy val accessorsCodeGenTask = (baseDirectory in Compile, dependencyClasspath in Compile, runner in Compile, streams) map { (dir, cp, r, s) =>
  val outputDir =     (dir / "modules" / "common" / "app" / "zyber" / "server" / "dao").getPath
  val accessorsDir =  (dir / "modules" / "common" / "app" / "zyber" / "server" / "dao" / "rawaccessors").getPath
  val generatorFile = (dir / "modules" / "codegen" / "src" / "main" / "java" / "zyber" / "GenerateTenantedAccesssors.java").getPath
  try {
    toError(r.run("zyber.GenerateTenantedAccesssors", cp.files, Array(accessorsDir, outputDir, generatorFile), s.log))
  } catch {
    case e: Exception =>
      println("Error running accessors generator task: " + e.getMessage)
      e.printStackTrace()
  }
}

lazy val generateCQL = TaskKey[Unit]("gen-cql")
lazy val cqlCodeGenTask = (baseDirectory in Compile, dependencyClasspath in Compile, runner in Compile, streams) map { (dir, cp, r, s) =>
  val outputDir = (dir / "conf" / "zyber").getPath
  val accessorsDir = (dir / "modules" / "common" /  "app" / "zyber" / "server" / "dao").getPath
  val generatorFile = (dir / "modules" / "codegen" / "src" / "main" / "java" / "zyber" / "GenerateCQL.java").getPath
  try {
    toError(r.run("zyber.GenerateCQL", cp.files, Array(accessorsDir, outputDir, generatorFile), s.log))
  } catch {
    case e: Exception =>
      println("Error running accessors generator task: " + e.getMessage)
      e.printStackTrace()
  }
}

libraryDependencies ++= Seq(
	specs2 % Test,
	"org.reflections" % "reflections" % "0.9.10" % "test" withSources() withJavadoc()
	)


generator <<= accessorsCodeGenTask

generateCQL <<= cqlCodeGenTask
compile in Compile <<= (compile in Compile).dependsOn(generator, generateCQL)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

//buildInfoOptions += BuildInfoOption.BuildTime

//JsEngineKeys.engineType := JsEngineKeys.EngineType.Node

