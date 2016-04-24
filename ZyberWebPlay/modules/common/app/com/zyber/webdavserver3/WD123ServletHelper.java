/*
package com.zyber.webdavserver3;

import org.apache.commons.cli.*;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;
import zyber.server.ZyberSession;
import zyber.server.ZyberTestSession;

import java.lang.invoke.MethodHandles;

*/
/**
 * Created by alynch on 3/3/2016 AD.
 *//*

public class WD123ServletHelper {
    public static void main(String[] args) throws Exception {
        final Options options = new Options();
        options.addOption("p", "port", true, "port to bind to");
        options.addOption("m", "create-model", false, "creates model in cassandra");
        options.addOption("h", "help", false, "print usage information");
        final CommandLineParser parser = new GnuParser();
        final CommandLine cmd = parser.parse(options, args);
        if (cmd.hasOption("h")) {
            new HelpFormatter().printHelp(MethodHandles.lookup().lookupClass().getSimpleName(), "Easy way to debug/run it this way.", options, "");

            return;
        }
        ZyberSession s = ZyberTestSession.getSessionForTesting();

        int port = Integer.parseInt(cmd.getOptionValue("port", "8080"));
        Server webServer = new Server(port);
        Context root = new Context(webServer, "/", Context.SESSIONS);
        root.addServlet(new ServletHolder(new WD123Servlet(s)), "*//*
");

        webServer.start();


	}
}
*/
