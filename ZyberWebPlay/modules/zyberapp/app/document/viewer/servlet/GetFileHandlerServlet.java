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
public class GetFileHandlerServlet extends ViewerServlet {

	private static final long serialVersionUID = 1L;

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			Logger.debug("GetFileHandlerServlet");

			String path = request.getParameter("path");
			boolean getPdf = Boolean.valueOf(request.getParameter("getPdf"));
			writeOutput(viewerHandler.getFileHandler(path, getPdf, response),
					response);
		} catch (Exception ex) {
			Logger.error("", ex);
		}
	}

}
