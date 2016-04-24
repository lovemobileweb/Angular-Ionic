libraryDependencies ++= Seq(
  "com.github.javaparser" % "javaparser-core" % "2.1.0" withSources() withJavadoc() // Java parser, so we can parse the files to and AST.
)