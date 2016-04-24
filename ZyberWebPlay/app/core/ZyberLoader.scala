package core

import play.api.ApplicationLoader
import play.api.inject._
import play.api.inject.guice._
import net.sf.ehcache.CacheManager
import java.io.PrintStream
import play.api.Logger

class CustomApplicationLoader extends GuiceApplicationLoader() {
  override def builder(context: ApplicationLoader.Context): GuiceApplicationBuilder = {
//    System.setOut(new PrintStream(System.out) {
//      override def print(s: String): Unit = {
//        Logger.info(s);
//      }
//    });
//    
//    System.setErr(new PrintStream(System.out) {
//      override def print(s: String): Unit = {
//        Logger.error(s);
//      }
//    });
    initialBuilder
      .in(context.environment)
      .loadConfig(context.initialConfiguration)
      .overrides(overrides(context): _*)
      //We just override default cache provider with custom one
      .overrides(bind[CacheManager].toProvider[ZyberCacheManagerProvider])
  }
}