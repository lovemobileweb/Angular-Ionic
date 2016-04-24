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
public class GetFontHandlerServlet extends ViewerServlet{

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
        	Logger.debug("GetFontHandlerServlet");

            String pathInfo = request.getPathInfo();
            String[] path = pathInfo.split("/");
            writeOutput(viewerHandler.getFontHandler(path[path.length - 1], request, response), response);
        } catch (Exception ex) {
        	Logger.error("", ex);	
        }
    }
    
}
