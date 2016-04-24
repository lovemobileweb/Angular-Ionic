libraryDependencies += filters

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator
routesImport += "binders.Binders._"
routesImport += "java.util.UUID"