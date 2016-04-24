scalacOptions ++= Seq("-Yresolve-term-conflict:object")

libraryDependencies ++= Seq("commons-httpclient" %"commons-httpclient" % "3.1",

  //Web dav
  "org.mortbay.jetty" % "jetty" % "6.1.22" withSources(),
  "com.github.lookfirst" % "sardine" % "5.6" withSources() withJavadoc(),
  "javax.servlet" % "servlet-api" % "2.5" withSources(),


  //File preview using viewerJS
  //  "org.webjars" % "viewerjs" % "0.5.5",

  //File preview using groupDocs
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.6.5",
  "com.fasterxml.jackson.core" % "jackson-core" % "2.6.5",
  "com.thetransactioncompany" % "cors-filter" % "1.9.2",
  "com.thetransactioncompany" % "java-property-utils" % "1.9.1",
  "com.groupdocs" % "groupdocs-viewer" % "2.14.0",
  "org.json" % "json" % "20090211"
)

libraryDependencies += filters

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator
routesImport += "binders.Binders._"
routesImport += "java.util.UUID"