package controllers

import java.util.List
import com.github.sardine.{DavResource, Sardine, SardineFactory}
import org.apache.commons.io.IOUtils
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import org.specs2.specification.BeforeEach
import play.api.Logger
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.RequestHeader
import play.api.test.WithServer
import services.DBTest
import util.{DefaultMultitenancyHelperImp, MultitenancyHelper}
import zyber.server.{ZyberSession, ZyberUserSession}
import zyber.server.dao.{Path, PathType, User, UserKeys}
import zyber.server.dao.admin.Tenant
import scala.collection.JavaConverters.asScalaBufferConverter
import core.ZyberCacheManagerProvider
import net.sf.ehcache.CacheManager

@RunWith(classOf[JUnitRunner])
class WebDavControllerSpec extends Specification with DBTest with BeforeEach{
  
  val ms = new DefaultMultitenancyHelperImp(zyberSession){
    override def getTenant(request: RequestHeader): Option[String] = Some("")
    override def validTenant(subdomain: String): Option[Tenant] = Some(testingTenant)
  }
  def before = {
    deleteForTestingTenant(zyberSession.getSession, classOf[User])
    deleteForTestingTenant(zyberSession.getSession, classOf[UserKeys])
    deleteForTestingTenant(zyberSession.getSession, classOf[Path])
  }
  
  def application =
    new GuiceApplicationBuilder()
      .overrides(bind[ZyberSession].to(zyberSession))
      .overrides(bind[MultitenancyHelper].to(ms))
      .overrides(bind[CacheManager].toProvider[ZyberCacheManagerProvider])
      .build();

  val port = 9002

  "WebDavController" should {
    "Read File Contents" in new WithServer(app = application, port = port) {
      val zus = new ZyberUserSession(zyberSession, testingTenantId, "xx3@wd.com", "Zyber12")

      val u = zus.user
      val rp = u.getRootPath(zus)

      createSampleFile(rp, "cat1.txt");
      createSampleFile(rp, "cat2.txt");
      createSampleFile(rp, "cat3.txt");
      createSampleFile(rp, "test1.txt");
      var dirtest: Path = rp.getFirstChild("dirtest");
      if (dirtest == null) dirtest = rp.createChild("dirtest", PathType.Directory);
      createSampleFile(dirtest, "fish1.txt");
      createSampleFile(dirtest, "fish2.txt");

//      import scala.io.Source
//      val html = Source.fromURL("http://localhost:" + port + "/WebDAV/")
//      val s = html.mkString
//      println(s)

      var sardine: Sardine = SardineFactory.begin(zus.user.getEmail, "Zyber12");

//      sardine.setCredentials(zus.user.getEmail, "Zyber12");
      
      Logger.debug("Connecting to: " + "http://localhost:" + port + "/WebDAV/");

      val resources = sardine.list("http://localhost:" + port + "/WebDAV/");

      //listFolder(sardine, "/", 5);
      listFolder(sardine, "/dirtest/", 2);

      checkFileContents(sardine, "/test1.txt", "Contents of test1.txt");
      checkFileContents(sardine, "/dirtest/fish1.txt", "Contents of fish1.txt");
      writeFile(sardine, "/webdav_upload1.txt", "Webdav Contents");
      sardine.shutdown();

    }

  }
  def createSampleFile(rp: Path, name: String) = {
    var cat: Path = rp.getFirstChild(name)
    if (cat == null) {
      var child = rp.createChild(name, PathType.File);
      var os = child.getOutputStream
      os.write(("Contents of " + name).getBytes)
      os.close
    }
  }
  def writeFile(sardine: Sardine, path: String, fileContents: String) = {
    //sardine.put("http://localhost:"+port+"/WebDAV"+path, fileContents.getBytes())
    sardine.put("http://localhost:" + port + "/WebDAV" + path, fileContents.getBytes())
  }

  def checkFileContents(sardine: Sardine, file: String, contents: String) = {
    Logger.info("Checking file contents: " + file);
    val readFromServer = IOUtils.toString(sardine.get("http://localhost:" + port + "/WebDAV" + file));

    contents mustEqual readFromServer
  }

  def listFolder(sardine: Sardine, path: String, expected: Int) = {
    Logger.info("Checking folder contents: " + path);
    val resources2: List[DavResource] = sardine.list("http://localhost:" + port + "/WebDAV" + path)

    resources2.asScala.foreach { res =>
      Logger.info("  " + res);
    }
    resources2.size() mustEqual expected;
  }

}