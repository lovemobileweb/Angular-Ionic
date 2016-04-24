package com.zyber.webdavserver3;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import services.DBTests;
import zyber.server.ZyberSession;
import zyber.server.ZyberTestSession;
import zyber.server.ZyberUserSession;
import zyber.server.dao.Path;

import com.github.sardine.DavResource;
import com.github.sardine.Sardine;
import com.github.sardine.SardineFactory;

public class WD123ServletTest {
	int port=8024;
	public final Logger log	= LoggerFactory.getLogger(MethodHandles.lookup().lookupClass().getName());

	static Path createFile(Path p, String name, String body) throws IOException {
		Path ret = p.createFile(name);
		try (java.io.OutputStream os = ret.getOutputStream()) {
			os.write(body.getBytes());			
		}
		return ret;
	}
	public String printTree(Path p, String prefixSoFar) {
		if (p.isFile())
			return prefixSoFar + " / '" + p.getName() + "'\n";
		if (p.isDirectory()) {
			String r = "";
			r += (prefixSoFar == null ? "" : prefixSoFar + " / ") + "'" + p.getName() + "' /\n";
			for (Path ch : p.getChildren()) {
				r += printTree(ch, (prefixSoFar == null ? "" : prefixSoFar + " / ") + "'" + p.getName() + "'");
			}
			return r;
		}
		throw new IllegalStateException();

	}
	
//	@Test
	public void test() throws Exception {
		ZyberSession z = ZyberTestSession.getSessionForTesting();
		ZyberUserSession zus = new ZyberUserSession(z, DBTests.testingTenantId(),"test_webdav");
        ZyberUserSession zus2 = new ZyberUserSession(z, DBTests.testingTenantId(),"guest");
        
        
        if (zus2.getRootPath().getChild("test1.txt").all().size() == 0) {
        	log.info("Created sample files.");
        	createFile(zus2.getRootPath(),"test1.txt", "Contents of test1");
        	createFile(zus2.getRootPath(),"test2.txt", "Contents of test2");
        	createFile(zus2.getRootPath(),"test3.txt", "Contents of test3");
        	zus2.getRootPath().createDirectory("dirtest");
        	createFile(zus2.getRootPath().getFirstChild("dirtest"),"subtest.txt", "Contents of subtest.");
        	log.debug("Created sample files: \n"+printTree(zus2.getRootPath(), null));
        }
		
        Server webServer = new Server(port);
        Context root = new Context(webServer, "/", Context.SESSIONS);
        root.addServlet(new ServletHolder(new WD123Servlet()), "/*");

        log.warn("Starting webdav server at http://localhost:"+port+"/");
        webServer.start();
        
        Sardine sardine = SardineFactory.begin();
        sardine.setCredentials(zus.user.getName(), "Zyber12");
        //List<DavResource> resources = sardine.list("http://localhost:"+port+"/");

        listFolder(sardine, "/", 5);
        listFolder(sardine, "/dirtest/",2);

        checkFileContents(sardine, "/test1.txt", "Contents of test1");
        writeFile(sardine, "/webdav_upload1.txt", "Webdav Contents");
        
        
        //Thread.sleep(60*1000);
        webServer.stop();
        webServer.destroy();
	}

	private void writeFile(Sardine sardine, String path, String fileContents) throws IOException {
		sardine.put("http://localhost:"+port+path, fileContents.getBytes());
	}
	private void checkFileContents(Sardine sardine, String file, String contents) throws IOException {
		log.info("Checking file contents: "+file);
		String readFromServer = IOUtils.toString(sardine.get("http://localhost:"+port+file));
		
		assertEquals(contents, readFromServer);
		
		
	}

	private void listFolder(Sardine sardine, String path, int expected) throws IOException {
		log.info("Checking folder contents: "+path);
        List<DavResource> resources2 = sardine.list("http://localhost:"+port+path);
        for (DavResource res : resources2)
        {
        	log.info("  "+res);
        }       
        assertEquals(resources2.size(),expected);
	}

}
