package document.viewer.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import play.Logger;
import document.viewer.domain.MediaType;

/**
 *
 * @author Alex Bobkov
 */
public class GetDocumentPageHtmlHandlerServlet extends ViewerServlet{
    
	private static final long serialVersionUID = 1L;

	@Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{
        try {
    		Logger.debug("GetDocumentPageHtmlHandlerServlet");

            writeOutput(MediaType.APPLICATION_JSON, response, viewerHandler.getDocumentPageHtmlHandler(request, response));
        } catch (Exception ex) {
    		Logger.debug("", ex);
        }
    }
}
