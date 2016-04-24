package services

import play.api.i18n.DefaultMessagesApi
import play.api.i18n.DefaultLangs
import play.api.Environment
import play.api.Application
import java.util.UUID
import com.datastax.driver.mapping.annotations.PartitionKey
import scala.collection.JavaConverters._
import com.datastax.driver.mapping.annotations.Column
import com.datastax.driver.core.querybuilder.QueryBuilder
import com.datastax.driver.core.Session
import com.datastax.driver.mapping.annotations.Table
import org.reflections.util.ConfigurationBuilder
import org.reflections.util.ClasspathHelper
import org.reflections.scanners.TypeAnnotationsScanner
import org.reflections.scanners.ResourcesScanner
import org.reflections.scanners.SubTypesScanner
import play.api.Logger
import zyber.server.dao.admin.Tenant
import zyber.server.ZyberUserSession
import zyber.server.ZyberTestSession
import play.api.inject.guice.GuiceApplicationBuilder
import zyber.server.ZyberSession
import play.api.inject.bind
import core.ZyberCacheManagerProvider
import net.sf.ehcache.CacheManager
import core.Jetty
import core.JettyProviderTests

trait DBTest {
  implicit val testingTenantId = UUID.fromString("01111111-1111-1111-1111-111111111111")

  val tenantIdColumn = "tenant_id"

  val testingTenant = new Tenant(testingTenantId, "localhost")

  val adminTenantsUsername = "admin@z.com"
  val adminTenantsPass = "secret"

  def messagesApi(implicit app: Application) =
    new DefaultMessagesApi(Environment.simple(), app.configuration, new DefaultLangs(app.configuration))

  def deleteForTestingTenant(s: Session, klazz: Class[_]): Unit = {
    deleteForTestingTenant(s, klazz, tenantIdColumn, testingTenantId)
  }

  def deleteForTestingTenant(s: Session, klazz: Class[_], tenantIdColumn: String, testingTenantId: UUID): Unit = {
    if (klazz.isAnnotationPresent(classOf[Table])) {
      //      Logger.debug("For class: " + klazz.getName)
      val fields = getAnnotatedFields(klazz, classOf[PartitionKey])
      //      Logger.debug(s"Fields: ${fields.size} " + fields.map { x => x.getName }.mkString(","))
      val names = fields.filter { f =>
        f.getAnnotation(classOf[PartitionKey]).value() > 0
      } map { f =>
        if (f.isAnnotationPresent(classOf[Column])) {
          val ca = f.getAnnotation(classOf[Column])
          ca.name()
        } else
          f.getName
      }
      //      Logger.debug(s"names: ${names.size} " + names.mkString(","))
      val table = klazz.getAnnotation(classOf[Table])

      val statement = QueryBuilder.select().all().from(table.keyspace(), table.name())

      val all = s.execute(statement).all()

      val partitionKeys = all.asScala.toList
        .filter { row => testingTenantId.equals(row.getUUID(tenantIdColumn)) }
        .map { row => names.toList.map(name => (name, row.getUUID(name))) }

      //      Logger.debug(partitionKeys.toString())

      val groupedPartitionKeys = partitionKeys.foldLeft(Map[String, List[UUID]]()) { (m, l) =>
        l.foldLeft(m) { (mp, p) =>
          val v = mp.getOrElse(p._1, Nil)
          mp.updated(p._1, v :+ p._2)
        }
      }
      if (groupedPartitionKeys.size == names.size) {
        val whereStatement = QueryBuilder.delete().from(table.keyspace(), table.name())
          .where(QueryBuilder.eq(tenantIdColumn, testingTenantId))

        //        Logger.debug(groupedPartitionKeys.toString())
        val delStatement = groupedPartitionKeys.foldRight(whereStatement) { (keys, statement) =>
          statement.and(QueryBuilder.in(keys._1, keys._2.asJava))
        }
        //        Logger.debug(delStatement.getQueryString)        
        s.execute(delStatement)
      }
    }
  }

  def getAnnotatedMethods(klazz: Class[_], annotation: Class[_ <: java.lang.annotation.Annotation]) = {
    val methods = klazz.getMethods.toList
    methods.filter { m =>
      m.isAnnotationPresent(annotation)
    }
  }

  def getAnnotatedFields(klazz: Class[_], annotation: Class[_ <: java.lang.annotation.Annotation]) = {
    val methods = klazz.getDeclaredFields
    Logger.debug(s"Fields: ${methods.size} ${methods.map(_.getName).mkString(",")}")
    methods.filter { f =>
      f.isAnnotationPresent(annotation)
    }
  }

  lazy val zus =
    ZyberTestSession.getTestUserSession(testingTenantId, adminTenantsUsername, adminTenantsPass)
  lazy val zyberSession = zus.session

  def zyberFakeApp = new GuiceApplicationBuilder().
    overrides(bind[ZyberSession].to(zyberSession)).
    overrides(bind[CacheManager].toProvider[ZyberCacheManagerProvider]).
    overrides(bind[Jetty].toProvider[JettyProviderTests]).
    build()
}

object DBTests extends DBTest