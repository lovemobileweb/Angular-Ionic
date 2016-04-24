package core

import javax.inject.{ Inject, _ }
import com.typesafe.config.{ Config, ConfigFactory }
import play.api.{ Configuration, Environment, Logger }
import play.api.inject.{ ApplicationLifecycle, _ }
import zyber.server.ZyberSession
import scala.concurrent.Future
import net.sf.ehcache.CacheManager
import net.sf.ehcache.config.ConfigurationFactory
import java.util.UUID
import com.google.inject.AbstractModule
import play.api.libs.concurrent.AkkaGuiceSupport
import actors.CreatePathReceptionist

//class ZyberModule extends Module {
//
////  val config = new ServiceConfiguration(new com.groupdocs.viewer.samples.javaweb.config.Configuration())
////  val viewerHandler = new com.groupdocs.viewer.handlers.ViewerHandler(config)
//  def bindings(environment: Environment,
//               configuration: Configuration) = {
//    Seq(
//      bind[ZyberSession].toProvider[ZyberSessionProvider],
//      bind[Jetty].toProvider[JettyProvider].eagerly()
////      ,bind[com.groupdocs.viewer.handlers.ViewerHandler].to(viewerHandler)
//      )
//  }
//}

class ZyberModule extends AbstractModule with AkkaGuiceSupport {

  def configure() = {

    bind(classOf[ZyberSession]).toProvider(classOf[ZyberSessionProvider])
    bind(classOf[Jetty]).toProvider(classOf[JettyProvider]).asEagerSingleton

    bindActor[CreatePathReceptionist]("create-folder-actor")
  }
}

@Singleton
class ZyberSessionProvider @Inject() (
    applicationLifecycle: ApplicationLifecycle,
    configuration: Configuration) extends Provider[ZyberSession] {
  lazy val username = configuration.getString("admin_user").get
  lazy val password = configuration.getString("admin_password").get
  lazy val zyberSession = ZyberSession.getZyberSession(username, password)
  Info.init()

  applicationLifecycle.addStopHook { () =>
    Future.successful {
      val load: Config = ConfigFactory.load("local")
      if (!load.hasPath("reuseZyber") || !load.getBoolean("reuseZyber")) {
        Logger.info("Closing connection...")
        zyberSession.close()
      } else {
        Logger.info("Keeping connection")
      }

    }
  }

  lazy val get: ZyberSession = {
    zyberSession
  }
}

/*Workaround for play issue related to ehcache: https://github.com/playframework/playframework/issues/4717
 * Not sure if it solves the problem, but everything seems to work fine
 * */
@Singleton
class ZyberCacheManagerProvider @Inject() (env: Environment, config: Configuration, lifecycle: ApplicationLifecycle) extends Provider[CacheManager] {
  lazy val get: CacheManager = {
    val resourceName = config.underlying.getString("play.cache.configResource")
    val configResource = env.resource(resourceName).getOrElse(env.classLoader.getResource("ehcache-default.xml"))
    val configuration = ConfigurationFactory.parseConfiguration(configResource)
    configuration.setName(UUID.randomUUID.toString)
    val manager = CacheManager.newInstance(configuration)
    lifecycle.addStopHook(() => Future.successful(manager.shutdown()))
    manager
  }
}