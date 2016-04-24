libraryDependencies ++= Seq(
  cache,
  ws,
  "org.mindrot" % "jbcrypt" % "0.3m",
  "org.passay" % "passay" % "1.1.0",
  "org.apache.tika" % "tika-core" % "1.11",

  //WebDAV
  "javax.servlet" % "javax.servlet-api" % "3.1.0" withSources() withJavadoc(),
  "javax.jcr" % "jcr" % "2.0" withSources() withJavadoc(),
  "org.apache.jackrabbit" % "jackrabbit-jcr-commons" % "2.7.3" withSources() withJavadoc(),
  "org.apache.jackrabbit" % "jackrabbit-jcr-server" % "2.7.3" withSources() withJavadoc(),
//  "commons-cli" % "commons-cli" % "1.1" withSources() withJavadoc(),


  //Core dependencies
  "com.datastax.cassandra" % "cassandra-driver-mapping" % "3.0.0" withSources() withJavadoc(),
  "commons-lang" % "commons-lang" % "2.6" withSources() withJavadoc(),
  "commons-io" % "commons-io" % "2.4" withSources() withJavadoc(),
//  "com.thedeanda" % "lorem" % "2.0" withSources() withJavadoc(),
  "com.novocode" % "junit-interface" % "0.11" % "test",
  "org.reflections" % "reflections" % "0.9.10" % "test" withSources() withJavadoc()

  // "com.chrisomeara" % "pillar_2.10" % "2.0.1" withSources() withJavadoc(), // Will be handy for database change sets... But is for a different version of scala ??!??
)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

libraryDependencies += filters

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator
routesImport += "binders.Binders._"
routesImport += "java.util.UUID"
