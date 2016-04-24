package controllers

object ZyberResponsesConstants {
  
  //HEAD request for /remote.php/webdav/
  val unauthorizedHeaders = Map(
    "Server" -> "Apache",
   "X-Powered-By" -> "PHP/5.5.30",
    "Expires" -> "Thu, 19 Nov 1981 08:52:00 GMT",
    "Cache-Control" -> "no-store, no-cache, must-revalidate, post-check=0, pre-check=0",
    "Pragma" -> "no-cache",
    "Content-Security-Policy" -> "default-src 'self'; script-src 'self' 'unsafe-eval'; style-src 'self' 'unsafe-inline'; frame-src *; img-src *; font-src 'self' data->; media-src *; connect-src *",
    "X-XSS-Protection" -> "1; mode=block",
    "X-Content-Type-Options" -> "nosniff",
    "X-Frame-Options" -> "SAMEORIGIN",
    "X-Robots-Tag" -> "none",
    "WWW-Authenticate" -> "Basic realm=\"Zyber\"",
    "Strict-Transport-Security" -> "max-age=63072000; includeSubDomains",
    "Keep-Alive" -> "timeout=5, max=98", 
    "Connection" -> "Keep-Alive",
    "Content-Type" -> "application/xml; charset=utf-8")

  // GET /ocs/v1.php/apps/files_sharing/api/v1/shares no params url
  val sharesHeadersGet = Map("Server" -> "Apache",
  "X-Powered-By" -> "PHP/5.5.30",
    "Vary" -> "Origin",
    "Expires" -> "Thu, 19 Nov 1981 08:52:00 GMT",
    "Cache-Control" -> "no-store, no-cache, must-revalidate, post-check=0, pre-check=0",
    "Pragma" -> "no-cache",
    "Content-Security-Policy" -> "default-src 'self'; script-src 'self' 'unsafe-eval'; style-src 'self' 'unsafe-inline'; frame-src *; img-src *; font-src 'self' data->; media-src *; connect-src *",
    "X-XSS-Protection" -> "1; mode=block",
    "X-Content-Type-Options" -> "nosniff",
    "X-Frame-Options" -> "SAMEORIGIN",
    "X-Robots-Tag" -> "none",
    "Set-Cookie" -> "oc_username=deleted; expires=Thu, 01-Jan-1970 00:00:01 GMT; Max-Age=0; httponly",
    "Set-Cookie" -> "oc_token=deleted; expires=Thu, 01-Jan-1970 00:00:01 GMT; Max-Age=0; httponly",
    "Set-Cookie" -> "oc_remember_login=deleted; expires=Thu, 01-Jan-1970 00:00:01 GMT; Max-Age=0; httponly",
    "Set-Cookie" -> "oc_username=deleted; expires=Thu, 01-Jan-1970 00:00:01 GMT; Max-Age=0; path=/; httponly",
    "Set-Cookie" -> "oc_token=deleted; expires=Thu, 01-Jan-1970 00:00:01 GMT; Max-Age=0; path=/; httponly",
    "Set-Cookie" -> "oc_remember_login=deleted; expires=Thu, 01-Jan-1970 00:00:01 GMT; Max-Age=0; path=/; httponly",
    "Set-Cookie" -> "ocwlpgg1j54t=6391f9b207bd5768cc42a711e3516a44; path=/; HttpOnly",
    "Strict-Transport-Security" -> "max-age=63072000; includeSubDomains",
    "Content-Length" -> "127",
    "Keep-Alive" -> "timeout=5, max=98",
    "Connection" -> "Keep-Alive",
    "Content-Type" -> "text/xml; charset=UTF-8")

    
    // GET status.php url
  val statusPhpHeadersGet = Map("Set-Cookie" -> "ocwlpgg1j54t=648bcf40ad260a8c2c0ca671237c3269; path=/; HttpOnly", "Content-Type" -> "application/json",
"Expires" -> "Thu, 19 Nov 1981 08:52:00 GMT",
"X-Powered-By" -> "PHP/5.5.30",
    "Cache-Control" -> "no-store, no-cache, must-revalidate, post-check=0, pre-check=0",
    "Pragma" -> "no-cache",
    "Content-Security-Policy" -> "default-src 'self'; script-src 'self' 'unsafe-eval'; style-src 'self' 'unsafe-inline'; frame-src *; img-src *; font-src 'self' data->; media-src *; connect-src *",
    "X-XSS-Protection" -> "1; mode=block",
    "X-Content-Type-Options" -> "nosniff",
    "X-Frame-Options" -> "SAMEORIGIN",
    "X-Robots-Tag" -> "none",
    "Strict-Transport-Security" -> "max-age=63072000; includeSubDomains",
    "Content-Length" -> "127",
    "Keep-Alive" -> "timeout=5, max=98",
    "Connection" -> "Keep-Alive",
    "Server" -> "Apache")
  // PROPFIND status.php url
  val foldersHeadersProfind = Map(  
  "Date" -> "Thu, 14 Jan 2016 03:30:18 GMT",
"Server" -> "Apache",
"X-Powered-By" -> "PHP/5.5.30",
"Expires" -> "Thu, 19 Nov 1981 08:52:00 GMT",
"Cache-Control" -> "no-store, no-cache, must-revalidate, post-check=0, pre-check=0",
"Pragma" -> "no-cache",
"Content-Security-Policy" -> "default-src 'self'; script-src 'self' 'unsafe-eval'; style-src 'self' 'unsafe-inline'; frame-src *; img-src *; font-src 'self' data:; media-src *; connect-src *",
"X-XSS-Protection" -> "1; mode=block",
"X-Content-Type-Options" -> "nosniff",
"X-Frame-Options" -> "SAMEORIGIN",
"X-Robots-Tag" -> "none",
"Vary" -> "Brief,Prefer",
"DAV" -> "1, 3, extended-mkcol",
"Set-Cookie" -> "ocwlpgg1j54t=80fd6fd2ef8cd0a61bfeedc376e41650; path=/; HttpOnly",
"Strict-Transport-Security" -> "max-age=63072000; includeSubDomains",
"Keep-Alive" -> "timeout=5, max=99",
"Connection" -> "Keep-Alive",
"Content-Type" -> "application/xml; charset=utf-8"
//"Content-Length" -> " 610"
)




//shareType param 
val OWNCLOUD_SHARE_USER =  0 
val OWNCLOUD_SHARE_GROUP = 1 
val OWNCLOUD_SHARE_PUBLIC_LINK = 3
val OWNCLOUD_SHARE_FEDERATED_CLOUD = 6
val OWNCLOUD_UNSHARE = 101

val OWNCLOUD_SHARE_WITH_USER = "user"
val OWNCLOUD_SHARE_WITH_GROUP = "group"

val ZYBER_SHARE_PUBLIC = "public"
val ZYBER_SHARE_PASSWORD = "password"
val ZYBER_SHARE_WITH_USER = "users"
val ZYBER_SHARE_WITH_GROUP = "group"
val ZYBER_REVOKE_SHARE = "revoke"

def convertShareTypeToZyber(owncloudType : Int)  : String = owncloudType match {
    case OWNCLOUD_SHARE_USER => ZYBER_SHARE_WITH_USER
    case OWNCLOUD_SHARE_GROUP => "share_type_not_found" //ZYBER_SHARE_WITH_GROUP
    case OWNCLOUD_SHARE_PUBLIC_LINK => ZYBER_SHARE_PUBLIC
    case OWNCLOUD_SHARE_FEDERATED_CLOUD => "share_type_not_found"
    case OWNCLOUD_UNSHARE =>  ZYBER_REVOKE_SHARE
    case _ => null
  }

def convertShareTypeFromZyber(zyberType : String) : Int = zyberType match {
   case ZYBER_SHARE_WITH_USER =>  OWNCLOUD_SHARE_USER 
    case ZYBER_SHARE_PUBLIC => OWNCLOUD_SHARE_PUBLIC_LINK
   // case OWNCLOUD_SHARE_FEDERATED_CLOUD => return "share_type_not_found"
    case ZYBER_REVOKE_SHARE =>  OWNCLOUD_UNSHARE
    case _ => -1
}


//OS WEbDAV clients

val MSminired = "Microsoft-WebDAV-MiniRed"

// should move here the logics of agent check
def checkOSWebDAVClients(client: String) : Boolean= {
  client match{
    case MSminired => true
    case _ => false
  }
}
}





//Future implementations
/* 
 share api specs https://doc.owncloud.com/server/9.0/developer_manual/core/ocs-share-api.html
 Syntax: /shares
Method: POST
POST Arguments: path - (string) path to the file/folder which should be shared
POST Arguments: shareType - (int) 0 = user; 1 = group; 3 = public link; 6 = federated cloud share
POST Arguments: shareWith - (string) user / group id with which the file should be shared
POST Arguments: publicUpload - (boolean) allow public upload to a public shared folder (true/false)
POST Arguments: password - (string) password to protect public link Share with
POST Arguments: permissions - (int) 1 = read; 2 = update; 4 = create; 8 = delete; 16 = share; 31 = all (default: 31, for public shares: 1)
Mandatory fields: shareType, path and shareWith for shareType 0 or 1.
Result: XML containing the share ID (int) of the newly created share*/
