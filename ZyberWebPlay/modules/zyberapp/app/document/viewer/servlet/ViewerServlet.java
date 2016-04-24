package document.viewer.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.groupdocs.viewer.config.ServiceConfiguration;
import com.groupdocs.viewer.handlers.ViewerHandler;
import com.groupdocs.viewer.handlers.ViewerHandlerBasic;
import com.groupdocs.viewer.utils.ByteArrayStreamUtils;
import com.groupdocs.viewer.utils.Utils;

import document.viewer.config.Configuration;
import document.viewer.domain.MediaType;
import document.viewer.handler.CassandraInputDataHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

import play.Logger;

/**
 *
 * @author Alex Bobkov
 */
public abstract class ViewerServlet extends HttpServlet {
    protected final String DEFAULT_ENCODING = "UTF-8";
    protected static ViewerHandler viewerHandler;
    
    protected static ViewerHandlerBasic vhb;
    


    @Override
    public void init() throws ServletException {
    	
        try {
            if(viewerHandler == null){
                final ServiceConfiguration config = new ServiceConfiguration(new Configuration());
                vhb = new ViewerHandlerBasic(config);

//                viewerHandler = new ViewerHandler(config /*, new CustomHtmlCacheHandler(config.getCachePath())*/);
                viewerHandler = new ViewerHandler(config, new CassandraInputDataHandler());

            }
        } catch (Exception ex) {
            Logger.error("Error in Init:", ex);
        }
    }

    protected void writeOutput(MediaType contentType, HttpServletResponse response, Object object) throws Exception {
        String json;
        if (object instanceof String) {
            json = (String) object;
        } else {
            ByteArrayStreamUtils streamUtils = new ByteArrayStreamUtils();
            try {
                new ObjectMapper().writeValue(streamUtils, object);
                json = IOUtils.toString(streamUtils.getInputStream());
            } finally {
                Utils.closeStreams(streamUtils);
            }
        }
        response.getOutputStream().write(json.getBytes(DEFAULT_ENCODING));
        if (contentType != null && !contentType.toString().isEmpty()) {
            response.setContentType(contentType.toString());
        }
    }
    
    protected void writeOutput(Object inputStream, HttpServletResponse response) throws IOException{
        if(inputStream != null){
            IOUtils.copy((InputStream) inputStream, response.getOutputStream());
        }else{
            response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
        }
    }
}