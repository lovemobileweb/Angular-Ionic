package document.viewer.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import play.Logger;

/**
 * @author Alex Bobkov, Aleksey Permyakov
 */
public class GetCssHandlerServlet extends ViewerServlet {

	private static final long serialVersionUID = 1L;

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		Logger.debug("GetCssHandlerServlet: " + request.getParameter("script"));

		try {

			response.setHeader("Content-type", "text/css");
			writeOutput(viewerHandler.getCssHandler(
					request.getParameter("script"), request, response),
					response);
		} catch (Exception ex) {
			Logger.error("", ex);
		}
	}

}
