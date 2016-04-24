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
public class GetDocumentPageImageHandlerServlet extends ViewerServlet {

	private static final long serialVersionUID = 1L;

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			System.out.println("GetDocumentPageImageHandlerServlet");
    		Logger.debug("GetDocumentPageImageHandlerServlet");

			int width = Integer.valueOf(request.getParameter("width"));
			int quality = Integer.valueOf(request.getParameter("quality"));
			boolean usePdf = Boolean.valueOf(request.getParameter("usePdf"));
			int pageIndex = Integer.valueOf(request.getParameter("pageIndex"));
			boolean isPrint = Boolean.valueOf(request.getParameter("isPrint"));
			final String watermarkPosition = request
					.getParameter("watermarkPosition");
			final Integer watermarkFontSize = Integer.valueOf(request
					.getParameter("watermarkFontSize"));
			final Boolean useHtmlBasedEngine = Boolean.valueOf(request
					.getParameter("useHtmlBasedEngine"));
			final Boolean rotate = Boolean.valueOf(request
					.getParameter("rotate"));

			String path = request.getParameter("path");
			
			writeOutput(viewerHandler.getDocumentPageImageHandler(path, width,
					quality, usePdf, pageIndex, isPrint, watermarkPosition,
					watermarkFontSize, useHtmlBasedEngine, rotate, response),
					response);

//			System.out.println("Query string: " + request.getQueryString());
//			InputStream is = viewerHandler.getDocumentPageImageHandler(request
//					.getQueryString());
//			writeOutput(is, response);
		} catch (Exception ex) {
    		Logger.error("", ex);

		}
	}

}
