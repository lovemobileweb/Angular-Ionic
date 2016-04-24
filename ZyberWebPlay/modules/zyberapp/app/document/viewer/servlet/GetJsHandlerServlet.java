package document.viewer.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import play.Logger;

/**
 * @author Alex Bobkov, Aleksey Permyakov
 */
public class GetJsHandlerServlet extends ViewerServlet {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	Logger.debug("GetJsHandlerServlet: " + request.getParameter("script"));
    	try {
        	Logger.debug("GetJsHandlerServlet");
        	
            writeOutput(viewerHandler.getJsHandler(request.getParameter("script"), request, response), response);
        } catch (Exception ex) {
        	Logger.error("", ex);
        }
    }

}
