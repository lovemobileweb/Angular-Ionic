package document.viewer.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;

import play.Logger;

import com.groupdocs.viewer.config.IServiceConfiguration;
import com.groupdocs.viewer.config.ServiceConfiguration;
import com.groupdocs.viewer.domain.request.ViewDocumentRequest;

import document.viewer.domain.MediaType;

/**
 *
 * @author Alex Bobkov
 */
public class ViewDocumentHandlerServlet extends ViewerServlet{

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Implement this method to support IE
    	Logger.debug("ViewDocumentHandlerServlet get");

    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
        	Logger.debug("ViewDocumentHandlerServlet post");
//        	Logger.debug("Query String: " + request.getQueryString());
//        	Logger.debug("getRequestURI: " + request.getRequestURI());
//        	Logger.debug("toString: " + request.toString());
        	
//        	Enumeration<String> headerNames = request.getHeaderNames();
//        	while(headerNames.hasMoreElements()){
//        		String ne = headerNames.nextElement();
////        		Logger.debug("Header name: " + ne);
////        		Logger.debug("Header: " + request.getHeader(ne));
//        	}
//        	
//        	Enumeration<String> paramNames = request.getParameterNames();
//        	while(paramNames.hasMoreElements()){
//        		String ne = paramNames.nextElement();
//        		Logger.debug("Param name: " + ne);
//        		Logger.debug("Param : " + request.getParameter(ne));
//        	}
//        	
//        	Enumeration<String> attrNames = request.getAttributeNames();
//        	while(attrNames.hasMoreElements()){
//        		String ne = attrNames.nextElement();
//        		Logger.debug("Attribute name: " + ne);
//        		Logger.debug("Param : " + request.getAttribute(ne));
//        	}
//        	
//        	Map<String, String[]> parameters = request.getParameterMap();
//        	for(String k : parameters.keySet()){
//        		Logger.debug("Key: " + k);
//        		String[] values = parameters.get(k);
//        		for(String v : values){
//        			Logger.debug("Value: " + v);
//        		}
//        	}
//        	JSONObject js = new JSONObject(CharStreams.toString(request.getReader())); 
////        	Logger.debug("Body: " + js.toString());
//        	ViewDocumentRequest dr = getDocumentRequest(vhb.getLocale(), js);
//        	
//        	ViewDocumentResponse dresp = vhb.viewDocument(dr, "");
////        	JSONObject jsonObject = new JSONObject(dresp);
////        	String jsString = jsonObject.toString();
//        	
//        	ObjectMapper mapper = new ObjectMapper();
//        	String jsString = mapper.writeValueAsString(dresp);
//        	
////        	Logger.debug("Result string: " + jsString);
////            writeOutput(MediaType.APPLICATION_JSON, response, dresp);
//            
//            response.setContentType(MediaType.APPLICATION_JSON.toString());
//            response.setCharacterEncoding(DEFAULT_ENCODING);
//            response.getWriter().write(jsString);
        	
        	String dhResult = viewerHandler.viewDocumentHandler(request, response);
//            Logger.debug("Result dh: " + dhResult);
        	writeOutput(MediaType.APPLICATION_JSON, response, dhResult);
        } catch (Exception ex) {
        	Logger.error("", ex);
        }
    }
    
    private ViewDocumentRequest getDocumentRequest(String locale, JSONObject js) throws JSONException {
		ServiceConfiguration config = vhb.getConfiguration();
		IServiceConfiguration c = config.getConfig();

		ViewDocumentRequest dr = new ViewDocumentRequest();
		
		dr.setPath(js.getString("path"));
		dr.setConvertWordDocumentsCompletely(c
				.isConvertWordDocumentsCompletely());
//		dr.setEmbedImagesIntoHtmlForWordFiles(true);
		dr.setFileDisplayName(js.getString("fileDisplayName"));
		dr.setIgnoreDocumentAbsence(js.getBoolean("ignoreDocumentAbsence"));
		// dr.setInstanceIdToken("");//TODO
		dr.setLocale(locale);
//		 dr.setPassword(js.getString("password"));//TODO
		dr.setPreloadPagesCount(js.getInt("preloadPagesCount"));
		// dr.setPrivateKey(c.getEncryptionKey());//TODO
//		dr.setQuality(js.getInt("quality"));//TODO
		dr.setSupportListOfBookmarks(js.getBoolean("supportListOfBookmarks"));
		dr.setSupportListOfContentControls(js.getBoolean("supportListOfContentControls"));
		dr.setSupportPageRotation(js.getBoolean("supportPageRotation"));
		dr.setUseHtmlBasedEngine(c.isUseHtmlBasedEngine());
//		dr.setUsePdf(js.getBoolean("usePdf"));
		
		dr.setUsePngImagesForHtmlBasedEngine(c.isUsePngImagesForHtmlBasedEngine());
		// dr.setUserId("");//TODO
		dr.setWatermarkColor(js.getString("watermarkColor"));
		dr.setWatermarkFontSize(c.getWatermarkFontSize());
		dr.setWatermarkPosition(js.getString("watermarkPosition"));
		dr.setWatermarkText(js.getString("watermarkText"));
		//TODO do safe js reading
//		Object wm = js.get("watermarkWidth");
//		dr.setWatermarkWidth(wm != null ? Integer.parseInt(wm.toString()) : null);
		dr.setWatermarkWidth(null);

//		dr.setWidth(js.getInt("width"));
		
		return dr;
	}
    
}
