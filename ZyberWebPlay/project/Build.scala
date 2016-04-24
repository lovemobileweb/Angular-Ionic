/*
import sbt._
import Keys._
import Tests._

object myBuild extends Build {

  // code generation task
  lazy val generator = TaskKey[Unit]("gen-accessors")
  lazy val accessorsCodeGenTask = (baseDirectory in Compile, dependencyClasspath in Compile, runner in Compile, streams) map { (dir, cp, r, s) =>
    val outputDir = (dir / "app" / "zyber" / "server" / "dao").getPath
    val accessorsDir = (dir / "app" / "zyber" / "server" / "dao" / "rawaccessors").getPath
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
    val accessorsDir = (dir / "app" / "zyber" / "server" / "dao").getPath
    val generatorFile = (dir / "modules" / "codegen" / "src" / "main" / "java" / "zyber" / "GenerateCQL.java").getPath
    try {
      toError(r.run("zyber.GenerateCQL", cp.files, Array(accessorsDir, outputDir, generatorFile), s.log))
    } catch {
      case e: Exception =>
        println("Error running accessors generator task: " + e.getMessage)
        e.printStackTrace()
    }
  }
}*/
