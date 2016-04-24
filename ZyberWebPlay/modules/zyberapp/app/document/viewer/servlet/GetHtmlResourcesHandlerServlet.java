package document.viewer.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import play.Logger;

/**
 *
 * @author Alex Bobkov
 */
public class GetHtmlResourcesHandlerServlet extends ViewerServlet{
    
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{
        try {
        	Logger.debug("GetHtmlResourcesHandlerServlet");
        	
        	
            String guid = request.getParameter("guid");
            String page = request.getParameter("page");
            String resourceName = request.getParameter("resourceName");
            writeOutput(
                    viewerHandler.getHtmlResourcesHandler(
                            request.getParameter("filePath"),
                            guid,
                            page == null ? null : Integer.valueOf(page),
                            resourceName,
                            response),
                    response);
        } catch (Exception ex) {
        	Logger.error("", ex);
        }
    }

}
