For clearing word websav cache:

HKEY_CURRENT_USER\Software\Microsoft\Office\\Common\Internet\Server Cache\ 

Map webdav to folder in windows: http://barracudadrive.com/tutorials/mapping_windows_drive.lsp

Libroffice web dav: http://help.libreoffice.org/Common/Opening_a_Document_Using_WebDAV_over_HTTPS

http://www.codeodor.com/index.cfm/2013/3/13/Editing-Docs-in-MS-Word-and-Saving-Them-Back-To-Your-Rails-Site-Using-WebDAV/3650

<script>
  function officelink(link) {
    try {
      new ActiveXObject("SharePoint.OpenDocuments.4").EditDocument(link.href);
      return false;
    }
    catch(e) {
      try {
        document.getElementById("winFirefoxPlugin").EditDocument(link.href);
        return false;
      }
      catch(e2) {
        return true;
      }
    }
    
  }
</script>
  
<a href="/webdav_docs/inetfilter.doc" onclick="return officelink(this)">inetfilter.doc

<object id="winFirefoxPlugin" type="application/x-sharepoint


or: http://stackoverflow.com/questions/653442/how-to-create-a-html-link-which-forces-ms-word-to-edit-document-on-webdav-server


http://blogs.msdn.com/b/russmax/archive/2012/03/10/behind-the-scenes-opening-a-document-from-a-sharepoint-2010-document-library.aspx