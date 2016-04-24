package controllers.zyberapp

import java.io.OutputStream
import java.util.UUID
import javax.inject.Inject
import core.RawStreamingBodyParser
import play.api.Logger
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.iteratee.Enumerator
import play.api.mvc.Action
import play.api.mvc.Controller
import zyber.server.ZyberSession
import zyber.server.ZyberUserSession
import zyber.server.dao.Path
import zyber.server.dao.PathType
import play.api.i18n.MessagesApi
import play.api.i18n.I18nSupport
import play.api.i18n.Messages
import play.api.Logger
import play.api.mvc.RequestHeader
import play.api.mvc.{ Result, Results }
import play.api.mvc.AnyContent
import core.{ RawStreamingBodyParser, StreamingBodyParser }
import java.io.OutputStream
import java.util.UUID
import services.{ LoginService, FileService }
import bridge.SharingBridge
import models.extra.SharingSubmission
import models.JPath
import zyber.server.dao.User
import core.ZyberResponse
import org.apache.commons.httpclient.util.URIUtil
import scala.collection.JavaConverters.asScalaBufferConverter
import scala.xml.{Elem, Node}
import util.MultitenancyHelper
import util.Secured
import controllers.ZyberResponsesConstants

trait Resource {
  def property(prop: Node): Option[Node] = prop match {
    case t @ <resourcetype/> => Some(t)
    case _                   => None
  }
  def url: String
  def children: Seq[Resource]
}

class PathResource(file: Path, request: RequestHeader, requestPath: String, parentName: String) extends Resource {
  val formatter = {
    val df = new java.text.SimpleDateFormat("EEE, dd MMM yyyy hh:mm:ss z")
    df.setTimeZone(java.util.TimeZone.getTimeZone("GMT"))
    df
  }
  def httpDate(time: Long): String = formatter.format(new java.util.Date(time))

  override def property(prop: Node): Option[Node] = {
    def easyNode(value: Node): Option[Node] =
      prop match { case Elem(p, l, at, sc) => Some(Elem("d", l, at.remove("xmlns"), sc, true, value)) }
    def easy(value: String): Option[Node] =
      easyNode(scala.xml.Text(value))

    prop match {
      case <getlastmodified/>  => easy(httpDate(file.getModifiedDate.getTime))
      case <getcontentlength/> => easy(file.getSize.toString)
      case <displayname/>      => easy(file.getName); // was displayname (no angle brackets. Did I break it YXJ)
      case <resourcetype/> => {
        if (file.isDirectory) easyNode(<D:collection/>) else Some(prop)
      }
      case _ => super.property(prop)
    }
  }
  //def url = "http://localhost:7070"+file.getPath.substring(1)+(if (file.isDirectory) "/" else "")

  //Ceraful verification on path and parent path for correct files path response

  var parentPath =
    if (requestPath.substring(1).dropRight(1).equals(parentName)) {
      requestPath.substring(1)
    } else if (requestPath.isEmpty() || requestPath.equals("/") || requestPath.substring(1).dropRight(1).equals(file.getName)) { "" }
    else { requestPath.substring(1) }

  def url = URIUtil.encodeQuery((if (request.secure) "https://" else "http://") + request.host + "/WebDAV/" + "remote.php/webdav/" + parentPath +
    (if (!file.isDirectory && file.getName.startsWith("/")) { file.getName.substring(1) } else { file.getName }) +
    (if (!file.getName.endsWith("/") && file.isDirectory) "/" else ""))
  def children = ((file.getChildren.all().asScala.toList).:::(List[Path](file))).map(new PathResource(_, request, requestPath, file.getName))
}

class WebDavController @Inject() (
    val session: ZyberSession,
    val messagesApi: MessagesApi,

    val loginService: LoginService, val fileService: FileService, val multitenancyHelper: MultitenancyHelper) extends Controller with I18nSupport with Secured {
  import scala.xml._

  val sharingBridge = new SharingBridge(fileService, loginService)

  def propfind(props: NodeSeq, res: Resource, depth: String): Elem = {
    val resources: Seq[Resource] = depth match {
      case "0" => res :: Nil
      case "1" => res.children /*++ (res::Nil) */
    }

    <D:multistatus xmlns:D="DAV:">
      {
        resources.map(res => {
          val mapped: Seq[(Node, Option[Node])] = props.map(p => (p, res.property(p)))
          <D:response>
            <D:href>{ res.url }</D:href>
            <D:propstat>
              <D:prop>
                { mapped.flatMap(_ match { case (_, Some(p)) => p :: Nil; case (_, None) => Nil }) }
              </D:prop>
              <D:status>HTTP/1.1 200 OK</D:status>
            </D:propstat>
          </D:response>
        })
      }
    </D:multistatus>
  }

  def propfindForMobile(props: NodeSeq, res: Resource, depth: String, zus: ZyberUserSession, requestPath: String): Elem = {
    val resources: Seq[Resource] = depth match {
      case "0" => res :: Nil
      case "1" => res.children /*++ (res::Nil) */
    }

    <d:multistatus xmlns:D="DAV:" xmlns:s="http://sabredav.org/ns" xmlns:oc="http://owncloud.org/ns">
      {
        resources.map(res => {

          val mapped: Seq[(Node, Option[Node])] = props.map(p => (p, res.property(p)))
          <d:response>
            <d:href>{ res.url }</d:href>
            <d:propstat>
              <d:prop>
                { mapped.flatMap(_ match { case (_, Some(p)) => p :: Nil; case (_, None) => Nil }) }
                { if (res.url.endsWith("/")) { <d:resourcetype><d:collection/></d:resourcetype> } else { <d:resourcetype/> } }
                <oc:permissions>RDNVW</oc:permissions>
              </d:prop>
              <d:status>HTTP/1.1 200 OK</d:status>
            </d:propstat>
            {
              if (res.url.endsWith("/")) {
                <d:propstat>
                  <d:prop>
                    <d:getcontenttype/>
                    <d:getcontentlength/>
                  </d:prop>
                  <d:status>HTTP/1.1 404 Not Found</d:status>
                </d:propstat>
              }
            }
          </d:response>
        })

      }
    </d:multistatus>
  }

  def helper(path: String, zus: ZyberUserSession): Path = {
    val rp = zus.getRootPath
  
    //    val maybeChild=Option(rp.getChild("cat.txt"));
    //   
    //    maybeChild.map(child=>
    var cat: Path = rp.getFirstChild("cat2.txt")
    if (cat == null) {
      var child = rp.createChild("cat2.txt", PathType.File);
      var os = child.getOutputStream
      os.write("Test text".getBytes)
      os.close
    }
    Logger.debug("ROOT PATH " + rp);
    Logger.debug("WebDAV - Helper Searching for (" + path + ")");
    if (path.isEmpty() || path.equals("/")) {
      Logger.debug("WebDAV - Helper Returns(/ (Root Path))");
      rp;
    } else {

      Ok("HOME PATH " + path)

      val ret = rp.findChild(path);
      Logger.debug("WebDAV - Helper Returns(" + (if (ret == null) "null" else ret.getName) + ")");
      ret
    }
  }

  def doHeadForWeb(zus: ZyberUserSession, requestPath: String) = Action { rs =>
    Unauthorized("").withHeaders(ZyberResponsesConstants.unauthorizedHeaders.toSeq: _*)

  }

  def doGetForWeb(zus: ZyberUserSession, requestPath: String, tid: UUID, request: RequestHeader) = Action { rs =>
    val path = helper(requestPath, zus);
    Logger.debug("requestPath " + requestPath);
    if (path == null || path.equals("null")) {
      if (requestPath.endsWith("status.php")) {
        Ok("{\"installed\":true,\"maintenance\":false,\"version\":\"8.1.0.7\",\"versionstring\":\"8.1 RC1\",\"edition\":\"\"}"). //.withHeaders("Content-Type" -> "application/json");
          withHeaders("Set-Cookie" -> "ocwlpgg1j54t=648bcf40ad260a8c2c0ca671237c3269; path=/; HttpOnly", "Content-Type" -> "application/json") //, "WWW-Authenticate" -> "Basic", "location"->"http://192.168.88.174:9000/WebDAV"

      } else if (requestPath.equals("ocs/v1.php/apps/files_sharing/api/v1/shares")) {
        Logger.debug("In shares");

        val nf = java.text.NumberFormat.getIntegerInstance;
        Ok(<html>
             <head><title>Directory listing for: { requestPath }</title></head>
             <body>
               <h3>Directory Listing for: { if (requestPath == "") "/" else requestPath }</h3>
               <table>
                 <tr><th>Name</th><th aligh='right'>Size</th><th align='right'>Last Modified</th></tr>
                 {
                   path.getChildren.all().asScala.map(p => {
                     <tr>
                       <td><a href={ rs.path + "/" + p.getName }>{ p.getName }</a></td>
                       <td align='right'>{ nf.format(p.getSize) }</td>
                       <td align='right'>{ p.getModifiedDate }</td>
                     </tr>
                   })
                 }
               </table>
             </body>
           </html>).withHeaders("Content-Type" -> "text/html")

      } else {
        NotFound(Messages("WebDav.FileNotFound", requestPath));
      }
    } else {
      if (path.isDirectory()) {
        if (rs.accepts("text/html")) {
          val nf = java.text.NumberFormat.getIntegerInstance;
          Ok(<html>
               <head><title>Directory listing for: { requestPath }</title></head>
               <body>
                 <h3>Directory Listing for: { if (requestPath == "") "/" else requestPath }</h3>
                 <table>
                   <tr><th>Name</th><th aligh='right'>Size</th><th align='right'>Last Modified</th></tr>
                   {
                     path.getChildren.all().asScala.map(p => {
                       <tr>
                         <td><a href={ rs.path + "/" + p.getName }>{ p.getName }</a></td>
                         <td align='right'>{ nf.format(p.getSize) }</td>
                         <td align='right'>{ p.getModifiedDate }</td>
                       </tr>
                     })
                   }
                 </table>
               </body>
             </html>).withHeaders("Content-Type" -> "text/html")
        } else {
          MethodNotAllowed("Maybe you are seeking the children here, and should be doing a PROPFIND?")
        }
      } else {

        // res.setContentLength(path.length.intValue)
        Ok.chunked(Enumerator.fromStream(path.getInputStream)).withHeaders("Content-Disposition" -> s"attachment; filename=${path.getName}; Content-Length=${path.getSize};")
      }
    }
  }

  def doOptionsForWeb(zus: ZyberUserSession, path_ignored: String) = Action { rs =>
   
    Ok.withHeaders(
      "DAV" -> "1",
      "Allow" -> "GET,OPTIONS,POST,PROPFIND",
      "WWW-Authenticate" -> """Digest realm="testrealm@host.com",
                        qop="auth,auth-int",
                        nonce="dcd98b7102dd2f0e8b11d0f600bfb0c093",
                        opaque="5ccc069c403ebaf9f0171e9517f40e41""""")
  }
  
  //WWW-Authenticate:

  // Test with: curl -i -X PROPFIND http://localhost:9000/WebDAV/ --upload-file - -H "Depth: 1" -d '<?xml version="1.0"?><a:propfind xmlns:a="DAV:"><a:prop><a:resourcetype/></a:prop></a:propfind>'
  // Test with: curl -i -X PROPFIND http://localhost:9000/WebDAV/ --upload-file - -H "Depth: 1" -d '^<?xml version="1.0"?^>^<a:propfind xmlns:a="DAV:"^>^<a:prop^>^<a:resourcetype/^>^</a:prop^>^</a:propfind^>'
  // Test with: curl -i -X PROPFIND http://localhost:9000/WebDAV/ -iv --raw --upload-file - -H "Depth: 1" -d "^<?xml version='1.0'?^>^<a:propfind xmlns:a='DAV:'^>^<a:prop^>^<a:resourcetype/^>^</a:prop^>^</a:propfind^>"
  // Test with: curl -i -X PROPFIND -iv --raw --upload-file - -H "Depth: 1" -d "SO MANY CATS" http://localhost:9000/WebDAV/ 
  // Test with: 
  //--user arnaudq:secret 
  // curl -iv --raw --request OPTIONS --header "Content-Type: text/xml" --header "Brief:t" --header "Depth: 1"  --data "" http://localhost:9000/  
  // curl -iv --raw --request OPTIONS --header "Content-Type: text/xml" --header "Brief:t" --header "Depth: 1"  --data "" http://localhost:9000  
  // curl -iv --raw --request PROPFIND --header "Content-Type: text/xml" --header "Brief:t" --header "Depth: 1"  --data "<D:propfind xmlns:D='DAV:'><D:prop><D:displayname/></D:prop></D:propfind>" http://localhost:9000/WebDAV/ 

  // OwnCloud iOS client.
  // Request #1
  // curl -iv --raw --request GET http://localhost:9000/WebDAV/status.php 
  // curl -iv --raw --request GET http://demo.zyber.com/status.php 
  //   Expect content-type json: {"installed":true,"maintenance":false,"version":"8.1.0.7","versionstring":"8.1 RC1","edition":""}
  // Request #2
  // curl -iv --raw --request HEAD http://localhost:9000/WebDAV/remote.php/webdav/ 
  // curl -iv --raw --request HEAD http://demo.zyber.com/remote.php/webdav 
  // Expect?
  //< HTTP/1.1 200 OK
  //< Date: Thu, 24 Dec 2015 01:38:30 GMT
  //< Server: Apache
  //< X-Powered-By: PHP/5.4.42
  //< Expires: Thu, 19 Nov 1981 08:52:00 GMT
  //< Cache-Control: no-store, no-cache, must-revalidate, post-check=0, pre-check=0
  //< Pragma: no-cache
  //< Content-Security-Policy: default-src 'self'; script-src 'self' 'unsafe-eval'; style-src 'self' 'unsafe-inline'; frame-src *; img-src *; font-src 'self' data:; media-src *; connect-src *
  //< X-XSS-Protection: 1; mode=block
  //< X-Content-Type-Options: nosniff
  //< X-Frame-Options: SAMEORIGIN
  //< X-Robots-Tag: none
  def doPropFindForWeb(zus: ZyberUserSession, requestPath: String, request: RequestHeader, tid: UUID) = Action(parse.tolerantXml) { rs =>
    val path = helper(requestPath, zus);
    if (path == null) {
      NotFound(Messages("WebDav.FileNotFound", requestPath));
    } else {
      //      Logger.debug("Body: "+rs.body);
      //      val foo = rs.body.asText;
      //      val fo2 = rs.body.asXml;

      //      val input = rs.body.asXml.getOrElse(<?xml version="1.0" encoding="UTF-8" standalone="yes"?><propfind xmlns="DAV:"><allprop/></propfind>)
      val input = rs.body;
      val depth = rs.headers.get("Depth").getOrElse("1")

      val res: PathResource = new PathResource(path, request, requestPath, fileService.getParentName(path, zus))

      val isAllProps: Boolean = !(input \ "allprop").isEmpty;
      val findResult =
        if (isAllProps) {
          val appPropsQ = <propfind><prop><getlastmodified/></prop><prop><getcontentlength/></prop><prop><resourcetype/></prop></propfind>;

          propfind(appPropsQ \ "prop" \ "_", res, depth)
        } else {
          propfind(input \ "prop" \ "_", res, depth)
        }

      val prettyPrinter = new scala.xml.PrettyPrinter(80, 2)

      Logger.debug(prettyPrinter.format(findResult))

      //      res.setContentType("application/xml")
      //Commented for test, seems web works without it
      MultiStatus(""); //findResult
    }
  }
  // def streamUpload(path: String) = Authenticated(StreamingBodyParser.streamingBodyParser(streamConstructor(path))) { request =>
  // def streamConstructor(path: String): RequestHeader => String => Option[OutputStream] = {
  // rh =>
  //   fn =>
  //     val spath = decode(path)
  //     username(rh).flatMap { user =>
  //       fileService.getOSPath(fn, user, spath)
  //     }

  //Reference: https://github.com/heiflo/play21-file-upload-streaming
  def doPutStreamConstructor(requestPath: String, zus: ZyberUserSession): RequestHeader => Option[OutputStream] = {
    rh =>
      {
        var path = helper(requestPath, zus)
        if (path == null) {
          val rp = zus.getRootPath;
          val tn = requestPath.substring(requestPath.lastIndexOf("/"), requestPath.length())
          path = rp.getFirstChild(tn)
          if (path == null) {
            path = rp.createChild(tn, PathType.File);
          }
          //        username(rh).flatMap { user =>
          //          fileService.getOSPath(fn, user, spath)
        }
        Option(path.getOutputStream)
      }
  }

  /* def doPut2ForWeb(zus: ZyberUserSession, requestPath: String, tenantId: UUID) = Action(parse.raw) { rs =>
    var path = helper(requestPath, zus)
    if (path == null) {
      val rp = zus.getRootPath
      val tn = requestPath.substring(requestPath.lastIndexOf("/"), requestPath.length())
      path = rp.getFirstChild(tn)
      if (path == null) {
        path = rp.createChild(tn, PathType.File);
      }
    }
    val os = path.getOutputStream
    os.write(rs.body.asBytes(99999999).get)
    os.close();
    Logger.debug("Wrote file : " + path.getName + " with " + path.getSize + " bytes.");
    Ok
  }*/

  def doPutForWeb(zus: ZyberUserSession, requestPath: String, tid: UUID) =
    Action(new RawStreamingBodyParser(doPutStreamConstructor(requestPath, zus)).bodyParser) { rs: RequestHeader =>
      Ok("File uploaded")
    }

  def handleRequest(request: RequestHeader): Action[_] = {
  Logger.debug("HANDLE REQUEST")
    userWebdav(request).fold(result => Action {result},
      user => {
        val tid = user.getTenantId
        val zus = new ZyberUserSession(session, user)
        
        var filePath = request.path;
        if (request.path.startsWith("/WebDAV/remote.php/webdav"))filePath = request.path.substring("/WebDAV/remote.php/webdav".length());
        else if (request.path.startsWith("/WebDAV")) filePath = request.path.substring("/WebDAV".length());
if(filePath=="") filePath = "/"
        Logger.debug("WebDAV: " + request.method + " at " + filePath);

        val userAgent: Int = getUserAgent(request)
        performRequest(zus, userAgent, URIUtil.decode(filePath), tid, request, user)

      })
  }

  def performRequest(zus: ZyberUserSession, userAgent: Int, filePath: String, tid: UUID, request: RequestHeader, user: User): Action[_] = {
    userAgent match {
      case MICROSOFT_OFFICE =>
        Logger.debug("Not supported User Agent: MICROSOFT_OFFICE")
        Action { Unauthorized.withHeaders("WWW-Authenticate" -> """Basic realm="ZyberWebDAV"""") }
      case OWNCLOUD_DESKTOP =>
        Logger.debug("Not supported User Agent: OWNCLOUD_DESKTOP")
        Action { Unauthorized.withHeaders("WWW-Authenticate" -> """Basic realm="ZyberWebDAV"""") }
      case MOBILE_OWNCLOUD => performMobileResponse(filePath, tid, request, zus, user)
       case MSminired | DavClnt => performWebResponse(filePath, tid, request, zus)
        case _               => performWebResponse(filePath, tid, request, zus)
    }
  }

  def performMobileResponse(filePath: String, tid: UUID, request: RequestHeader, zus: ZyberUserSession, user: User): Action[_] = {
    Logger.debug("Mobile ownCLoud Cleint found");

        request.method match {
          case "HEAD" => {
        doHeadForMobile(zus, filePath);
          }
          case "GET" => {
        doGetForMobile(zus, filePath, tid);
          }
          case "OPTIONS" => {
        doOptionsForMobile(zus, filePath);
          }
          case "PUT" => {
        doPut2ForMobile(zus, filePath, tid);
          }
      case "MKCOL" => {
        doMKCOLForMobile(zus, filePath, tid);
      }
      case "DELETE" => {
        doDeleteForMobile(zus, filePath, tid); //if(filePath.endsWith("/")){filePath.dropRight(1)}else{filePath}
      }
          case "PROPFIND" => {
        doPropFindForMobile(zus, filePath, tid);
          }

      case "POST" => {
        doPostForMobile(zus, filePath, tid, user);
      }
          case _ => {
            Action { InternalServerError("Unknown request method: " + request.method) }
          }
        }
  }

  def performWebResponse(filePath: String, tid: UUID, request: RequestHeader, zus: ZyberUserSession): Action[_] = {
    request.method match {
      case "HEAD" => {
        doHeadForWeb(zus, filePath);
  }
      case "GET" => {
        doGetForWeb(zus, filePath, tid, request);
      }
      case "OPTIONS" => {
        doOptionsForWeb(zus, filePath);
      }
      case "PUT" => {
        doPut2ForMobile(zus, filePath, tid);
      }
      case "PROPFIND" => {

        doPropFindForWeb(zus, filePath, request, tid);
      }
      case _ => {
        Action { InternalServerError("Unknown request method: " + request.method) }
      }
    }
  }
    
  def getUserAgent(request: RequestHeader): Int = {
    val uagent = request.headers.get("User-Agent").getOrElse("").toLowerCase()
    if (uagent.contains("owncloud") && ((uagent.contains("ios") || uagent.contains("android")))) {
      return MOBILE_OWNCLOUD
    } else if (uagent.contains("microsoft") && uagent.contains("office")) {
      return MICROSOFT_OFFICE
    } else if (uagent.contains("owncloud") && uagent.contains("smth")) {
      return OWNCLOUD_DESKTOP
      } else if (uagent.contains("Microsoft-WebDAV-MiniRedir")) {
      return MSminired
      } else if (uagent.contains("DavCln")) {
      return DavClnt
    } else {
      return WEB
    }
  }
    
  val MOBILE_OWNCLOUD = 11
  val MICROSOFT_OFFICE = 22
  val OWNCLOUD_DESKTOP = 33
  val WEB = 44
  val MSminired = 55
 val DavClnt = 66

  def doHeadForMobile(zus: ZyberUserSession, requestPath: String) = Action { rs =>
    Unauthorized("").withHeaders(ZyberResponsesConstants.unauthorizedHeaders.toSeq: _*)
    }
    
  def doGetForMobile(zus: ZyberUserSession, requestPath: String, tid: UUID) = Action { request =>
    val path = helper(requestPath, zus);
    
    if (path == null || path.equals("null")) {
      if (requestPath.endsWith("status.php")) {
        // Support for ownCloud ios.
        Ok("{\"installed\":true,\"maintenance\":false,\"version\":\"8.1.0.7\",\"versionstring\":\"8.1 RC1\",\"edition\":\"\"}"). //.withHeaders("Content-Type" -> "application/json");
          withHeaders(ZyberResponsesConstants.statusPhpHeadersGet.toSeq: _*) //, "WWW-Authenticate" -> "Basic", "location"->"http://192.168.88.174:9000/WebDAV"

      } else if (requestPath.equals("ocs/v1.php/apps/files_sharing/api/v1/shares") || requestPath.equals("/ocs/v1.php/apps/files_sharing/api/v1/shares")) {
        Logger.debug("In shares");
        val uagent = request.headers.get("User-Agent").getOrElse("").toLowerCase()
        if (uagent.contains("owncloud") && (uagent.contains("ios") || uagent.contains("android"))) {

          Ok(" <?xml version=\"1.0\"?><ocs><meta><status>ok</status><statuscode>100</statuscode><message/></meta><data/</ocs>").withHeaders(
            ZyberResponsesConstants.sharesHeadersGet.toSeq: _*)
        } else {

          val nf = java.text.NumberFormat.getIntegerInstance;
          NotFound(<html>
                     <head><title>Mobile Response is not processed correctly</title></head>
                     <body> </body>
                   </html>).withHeaders("Content-Type" -> "text/html")

    }
      
        /* OSC is for shares no need now. Should be replaced to some OSC control def
        Logger.debug("In shares");
        Logger.debug("Shares authorized");
        val t = getTenant(request).flatMap(loginService.validTenant).getOrElse(null)
        if (t == null) {
          Unauthorized.withHeaders("WWW-Authenticate" -> """Basic realm="ZyberWebDAV"""")
        } else {
          if (authenticateByAuthorizationHeader(request, t).isRight) {
            //authorized .../shares... should respond with files list , probably only for OCS-APIREQUEST: true
            //ownCloud
            //defineOSCHeader
            request.headers.get("OCS-APIREQUEST") match{
              case Some("true") => //getOSCResult(zus, requestPath, request)
                BadRequest("OCS API IN DEVELOPMENT")
                case _ => //perform web dav request - now using propfind method
               BadRequest("OCS API IN DEVELOPMENT")
            }
      
      
          } else {
            
            //ownCloud by design 1st time uses ../shares... url unauthorized
            Logger.debug("Shares unauthorized");
            val uagent = request.headers.get("User-Agent").getOrElse("").toLowerCase();
            if (uagent.contains("owncloud") && (uagent.contains("ios") || uagent.contains("android"))) {

              val sxml: scala.xml.Elem = <ocs><meta><status>ok</status><statuscode>100</statuscode><message/></meta></ocs>
              Ok("""<?xml version="1.0" encoding="UTF-8">""" + sxml).withHeaders( //standalone="yes"
                ZyberResponsesConstants.sharesHeadersGet.toSeq: _*)

            } else {

              val nf = java.text.NumberFormat.getIntegerInstance;
              NotFound(<html>
                         <head><title>Mobile Response is not processed correctly</title></head>
                         <body> </body>
                       </html>).withHeaders("Content-Type" -> "text/html")

    }
    }
    }
      */ } else if (requestPath.endsWith("0")) {
        //Mock respond mobile app requires. Response ok if not OSC share data sent in OSC api format
        Ok("<?xml version=\"1.\"?><ocs><meta><status>failure</status><statuscode>404</statuscode><message>share doesn't exist</message></meta><data/></ocs>").
          withHeaders("Content-Type" -> "text/xml; charset=UTF-8")
        //checkDirectoryShareSettings
      } else if (requestPath.endsWith("capabilities")) {
        //Server capabilities request, omit for now
        //http://localcom:9000/WebDAV/ocs/v1.php/cloud/capabilities
        NotFound(Messages("WebDAV/ocs/v1.php/cloud/capabilities", requestPath));
      } else {
        NotFound(Messages("WebDav.FileNotFound", requestPath));
    }
    } else {
    
      Logger.debug("requestPath " + requestPath);
      Logger.debug("found path: " + path.getName);

      if (path.isDirectory()) {

        Ok(" <?xml version=\"1.0\"?><ocs><meta><status>ok</status><statuscode>100</statuscode><message/></meta><data/</ocs>").withHeaders(
          ZyberResponsesConstants.sharesHeadersGet.toSeq: _*)

      } else {

        Ok.chunked(Enumerator.fromStream(path.getInputStream)).withHeaders("Content-Disposition" -> s"attachment; filename=${path.getName}; Content-Length=${path.getSize};")
      }
    }
  }

  def doOptionsForMobile(zus: ZyberUserSession, path_ignored: String) = Action { rs =>
    Ok.withHeaders(
      "DAV" -> "1",
      "Allow" -> "GET,OPTIONS,POST,PROPFIND")
  }

  def doPropFindForMobile(zus: ZyberUserSession, requestPath: String, tid: UUID) = Action(parse.tolerantXml) { request =>
    val path = helper(requestPath, zus);
    if (path == null) {
      NotFound(Messages("WebDav.FileNotFound", requestPath));
    } else {

      val input = request.body;
      val depth = request.headers.get("Depth").getOrElse("1")

      val res: PathResource = new PathResource(path, request, requestPath, fileService.getParentName(path, zus))

      val isAllProps: Boolean = !(input \ "allprop").isEmpty;
      //Folder response about files must include info about folder itself only on top of response elements list

      def findResults = {
        if (isAllProps) {
          val appPropsQ = <propfind><prop><getlastmodified/></prop><prop><getcontentlength/></prop><prop><resourcetype/></prop></propfind>;

          propfindForMobile(appPropsQ \ "prop" \ "_", res, depth, zus, requestPath)
        } else {
          propfindForMobile(input \ "prop" \ "_", res, depth, zus, requestPath)
        }
      }
      val findResult = findResults

      val prettyPrinter = new scala.xml.PrettyPrinter(80, 2)

      Logger.debug(prettyPrinter.format(findResults))

      MultiStatus("<?xml version=\"1.0\"?>" + findResult.toString()).withHeaders(ZyberResponsesConstants.foldersHeadersProfind.toSeq: _*)
    }
  }

  def doDeleteForMobile(zus: ZyberUserSession, requestPath1: String, tid: UUID) = Action { request =>

    val requestPath = if (requestPath1.endsWith("/")) { requestPath1.dropRight(1) }
    else { requestPath1 }

    val homePath = requestPath.dropRight(requestPath.size - requestPath.lastIndexOf("/") - 1)

    val fileName = requestPath.substring(requestPath.lastIndexOf("/") + 1)
    Logger.debug("File name " + fileName)
    var path = helper(requestPath, zus)
    if (path != null) {
      val rp = helper(homePath, zus)
      val tn = fileName
      path = rp.getFirstChild(tn)
      if (path != null) {
        fileService.delete(path.getPathId)(zus.user) match {
          case Some(bol) => MultiStatus(Messages("File has been deleted"))
          case None      => InternalServerError(Messages("Could not delete file"))

        }
      } else {
        InternalServerError(Messages("Could not delete file"))
      }
    } else {
      InternalServerError(Messages("Could not delete file"))
    }
  }

  def doPostForMobile(zus: ZyberUserSession, requestPath: String, tid: UUID, user: User) = Action { rs =>

    val params: Map[String, String] = rs.body.toString().split("&").map(fullParam => fullParam.split("=")(0) -> fullParam.split("=")(1)).toMap //.//mapBy(_._1)
    params.foreach(x => Logger.debug("key " + x._1 + "value " + x._2))

    var path = helper(params.get("path").getOrElse(null), zus)
    if (path == null) {
      val rp = zus.getRootPath
      val tn = requestPath.substring(requestPath.lastIndexOf("/"), requestPath.length())
      path = rp.getFirstChild(tn)
      if (path == null) {
        path = rp.createChild(tn, PathType.File);
      }
    }

    var shareType: String = params.get("shareType").map { value => ZyberResponsesConstants.convertShareTypeToZyber(value.toInt) }.getOrElse("")
    val shareWith = params.get("shareWith")
    val sharePassword = params.get("password")
    sharePassword match {
      case Some(password) => shareType = "password"
      case None           =>
    }
    val sharingSubmission = SharingSubmission(shareType.toString(), sharePassword, shareWith)
    Option(sharingSubmission).map(s => {
      fileService.getPathByUUID(UUID.fromString(params.get("path").getOrElse("")))(user).map(p => {
        val jPath: JPath = if (p.getType.equals(PathType.File)) sharingBridge.setSharesFor(p, sharingSubmission, user)(user) else sharingBridge.setSharesForFolder(p, sharingSubmission, user)(user)

        Ok("<?xml version=\"1.0\"?>" +
          "<ocs>" +
          "<meta>" +
          "<status>ok</status>" +
          "<statuscode>100</statuscode>" +
          "<message/>" +
          "</meta>" +
          "<data>" +
          "<id>" + jPath.shareId + "</id>" +
          "<url>" + jPath.fullPath + "</url>" +
          "<token>7tYR9uURshNNLop</token>" +
          "</data>" +
          "</ocs>").withHeaders("Content-Type" -> "text/xml; charset=UTF-8")
      }).getOrElse(NotFound)
    }).getOrElse(BadRequest)
  }

  def doPut2ForMobile(zus: ZyberUserSession, requestPath: String, tenantId: UUID) = Action(parse.raw) { rs =>
    val homePath = requestPath.dropRight(requestPath.size - requestPath.lastIndexOf("/") - 1)
    val fileName = requestPath.substring(requestPath.lastIndexOf("/") + 1)
    Logger.debug("File name " + fileName)
    var path = helper(requestPath, zus)
    if (path == null) {
      val rp = helper(homePath, zus)
      val tn = fileName
      path = rp.getFirstChild(tn)
      if (path == null) {
        path = rp.createChild(tn, PathType.File);
      }
    }
    val os = path.getOutputStream
    os.write(rs.body.asBytes(99999999).get)
    os.close();
    Logger.debug("Wrote file : " + path.getName + " with " + path.getSize + " bytes.");
    Created("File has been stored")
  }

  def doMKCOLForMobile(zus: ZyberUserSession, requestPath: String, tenantId: UUID) = Action(parse.raw) { rs =>
    val homePath = requestPath.dropRight(requestPath.size - requestPath.lastIndexOf("/") - 1)
    Logger.debug("HOME PATH " + homePath)
    val folderName = requestPath.substring(requestPath.lastIndexOf("/") + 1)
    Logger.debug("Folder name " + folderName)

    var path = helper(requestPath, zus)
    if (path == null) {
      val rp = helper(homePath, zus)
      val tn = folderName
      path = rp.getFirstChild(tn)
      if (path == null) {
        if (!folderName.matches("[^\\/:?*\"|]+"))
          Conflict(Messages("invalid.name"))
        else
          // createFolder method was changed to String (homePath) -> Path, try to use helper(homePath, zus), need test because the method might return only root path
          fileService.createFolder(rp, folderName)(zus.user) match {
          //  case None                 => Conflict(Messages("api.invalid.path", homePath))
            case  Left(msg)      => ZyberResponse.errorResult(msg)
            case  Right(retPath) => Created(Messages("Created", retPath.getName))
          }
      } else { Conflict(Messages("Folder exists")) }

    } else { Conflict(Messages("Folder exists")) }
  }

  def doPutForMobile(zus: ZyberUserSession, requestPath: String, tid: UUID) =
    Action(new RawStreamingBodyParser(doPutStreamConstructor(requestPath, zus)).bodyParser) { rs: RequestHeader =>
      Ok("File uploaded")
    }

  /* def getOSCResult(zus: ZyberUserSession, requestPath: String, request: RequestHeader) = {
    val path = helper(requestPath, zus);
    if (path == null) {
      NotFound(Messages("WebDav.FileNotFound", requestPath));
    } else {

      val sxml: scala.xml.Elem = <ocs><meta><status>ok</status><statuscode>100</statuscode><message/></meta></ocs>
      Ok("""<?xml version="1.0" encoding="UTF-8">""" + sxml).withHeaders(ZyberResponsesConstants.sharesHeadersGet.toSeq: _*)

     // val input = request.g
      val depth = request.headers.get("Depth").getOrElse("1")

      val res: PathResource = new PathResource(path, request)
      //"""<?xml version="1.0" encoding="UTF-8">""" + sxml).withHeaders(ZyberResponsesConstants.sharesHeadersGet.toSeq: _*
      val isAllProps: Boolean = !(input \ "allprop").isEmpty;
      val start = "cce \n<?xml version=\"1.0\" encoding=\"utf-8\"?>  \n" +
        "<d:multistatus xmlns:d=\"DAV:\" xmlns:s=\"http://sabredav.org/ns\" xmlns:oc=\"http://owncloud.org/ns\">"
      val end = "\n 0" +
        "</d:multistatus> "
      def findResults = {
        if (isAllProps) {
          val appPropsQ = <propfind><prop><getlastmodified/></prop><prop><getcontentlength/></prop><prop><resourcetype/></prop></propfind>;

          propfindForMobile(appPropsQ \ "prop" \ "_", res, depth)
        } else {
          propfindForMobile(input \ "prop" \ "_", res, depth)
        }
      }
      val findResult = findResults

      val prettyPrinter = new scala.xml.PrettyPrinter(80, 2)

      Logger.debug(prettyPrinter.format(findResults))

      Ok(findResult.toString())

    }
  } */

}
