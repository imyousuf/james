/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.remotemanager;

import org.apache.avalon.*;
import org.apache.avalon.services.*;
import org.apache.james.*;
import org.apache.james.services.*;
import org.apache.log.LogKit;
import org.apache.log.Logger;
import java.net.*;
import java.io.*;
import java.util.*;

/**
 * Provides a really rude network interface to administer James.
 * Allow to add accounts.
 * TODO: -improve protocol
 *       -add remove user
 *       -much more...
 * @version 1.0.0, 24/04/1999
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 */
public class RemoteManager implements SocketServer.SocketHandler, Scheduler.Target, Composer, Configurable {

    private ComponentManager compMgr;
    private Configuration conf;
    private Logger logger =  LogKit.getLoggerFor("james.RemoteManager");
    private UsersRepository users;
    private Scheduler scheduler;
    private MailServer mailServer;

    private BufferedReader in;
    private InputStream socketIn;
    private PrintWriter out;
    private OutputStream r_out;
    private HashMap admaccount;
    private Socket socket;
    private long timeout;

    public RemoteManager() {
    }

    public void configure(Configuration conf)  throws ConfigurationException {
        this.conf = conf;
	timeout = conf.getChild("connectiontimeout").getValueAsLong(120000);
    }

    public void compose(ComponentManager compMgr) {
        this.compMgr = compMgr;
    }

    public void init() throws Exception {

        logger.info("RemoteManager init...");
        scheduler = (Scheduler) compMgr.lookup("org.apache.avalon.services.Scheduler");
	mailServer = (MailServer) compMgr.lookup("org.apache.james.services.MailServer");
        SocketServer socketServer = (SocketServer) compMgr.lookup("org.apache.avalon.services.SocketServer");
        int port = conf.getChild("port").getValueAsInt(4554);
        InetAddress bind = null;
        try {
            String bindTo = conf.getChild("bind").getValue();
            if (bindTo.length() > 0) {
                bind = InetAddress.getByName(bindTo);
            }
        } catch (ConfigurationException e) {
        }

	String type = SocketServer.DEFAULT;
	try {
	    if (conf.getChild("useTLS").getValue().equals("TRUE")) type = SocketServer.TLS;
	} catch (ConfigurationException e) {
	}
	
        logger.info("RemoteManager using " + type + " on port " + port);
	
	Configuration adm = conf.getChild("administrator_accounts");
        admaccount = new HashMap();
        for (Iterator it = adm.getChildren("account"); it.hasNext();) {
            Configuration c = (Configuration) it.next();
            admaccount.put(c.getAttribute("login"), c.getAttribute("password"));
        }
        if (admaccount.isEmpty()) {
            logger.warn("No Administrative account defined");
	    logger.warn("RemoteManager failed to init");
	    return;
        } else {
	    socketServer.openListener("JAMESRemoteControlListener",type, port, bind, this);
	    users = (UsersRepository) compMgr.lookup("org.apache.james.services.UsersRepository");
	    logger.info("RemoteManager ...init end");
	}
    }


    public void parseRequest(Socket s) {

        scheduler.setAlarm("RemoteManager", new Scheduler.Alarm(timeout), this);
        socket = s;
        String remoteHost = s.getInetAddress().getHostName();
        String remoteIP = s.getInetAddress().getHostAddress();
        try {
            socketIn = s.getInputStream();
            in = new BufferedReader(new InputStreamReader(socketIn));
            r_out = s.getOutputStream();
            out = new PrintWriter(r_out, true);
            logger.info("Access from " + remoteHost + "(" + remoteIP + ")");
            out.println("JAMES RemoteAdministration Tool " + Constants.SOFTWARE_VERSION);
            out.println("Please enter your login and password");
            String login = in.readLine();
            String password = in.readLine();
            while (!password.equals(admaccount.get(login)) || password.length() == 0) {
                out.println("Login failed for " + login);
                logger.info("Login for " + login + " failed");
                login = in.readLine();
                password = in.readLine();
            }
            scheduler.resetAlarm("RemoteManager");
            out.println("Welcome " + login + ". HELP for a list of commands");
            logger.info("Login for " + login + " succesful");
            while (parseCommand(in.readLine())) {
                scheduler.resetAlarm("RemoteManager");
            }
            logger.info("Logout for " + login + ".");
            s.close();
        } catch (IOException e) {
            out.println("Error. Closing connection");
            out.flush();
            logger.error("Exception during connection from " + remoteHost + " (" + remoteIP + ")");
        }
        scheduler.removeAlarm("RemoteManager");
    }

    public void wake(String name, Scheduler.Event event) {
        logger.error("Connection timeout on socket");
        try {
            out.println("Connection timeout. Closing connection");
            socket.close();
        } catch (IOException e) {
        }
    }

    private boolean parseCommand(String command) {
        if (command == null) return false;
        StringTokenizer commandLine = new StringTokenizer(command.trim(), " ");
        int arguments = commandLine.countTokens();
        if (arguments == 0) {
            return true;
        } else if(arguments > 0) {
            command = commandLine.nextToken();
        }
        String argument = (String) null;
        if(arguments > 1) {
            argument = commandLine.nextToken();
        }
        String argument1 = (String) null;
        if(arguments > 2) {
            argument1 = commandLine.nextToken();
        }
        if (command.equalsIgnoreCase("ADDUSER")) {
            String user = argument;
            String passwd = argument1;
            try {
                if (user.equals("") || passwd.equals("")) {
                    out.println("usage: adduser [username] [password]");
                    return true;
                }
            } catch (NullPointerException e) {
                out.println("usage: adduser [username] [password]");
                return true;
            }
            if (users.contains(user)) {
                out.println("user " + user + " already exist");
            } else {
                if(mailServer.addUser(user, passwd)) {
            out.println("User " + user + " added");
            logger.info("User " + user + " added");
        } else {
            out.println("Error adding user " + user);
            logger.info("Error adding user " + user);
        }
            }
            out.flush();
        } else if (command.equalsIgnoreCase("DELUSER")) {
            String user = argument;
            if (user.equals("")) {
                out.println("usage: deluser [username]");
                return true;
            }
            try {
                users.removeUser(user);
            } catch (Exception e) {
                out.println("Error deleting user " + user + " : " + e.getMessage());
                return true;
            }
            out.println("User " + user + " deleted");
            logger.info("User " + user + " deleted");
        } else if (command.equalsIgnoreCase("LISTUSERS")) {
            out.println("Existing accounts " + users.countUsers());
            for (Iterator it = users.list(); it.hasNext();) {
                out.println("user: " + (String) it.next());
            }
        } else if (command.equalsIgnoreCase("COUNTUSERS")) {
            out.println("Existing accounts " + users.countUsers());
        } else if (command.equalsIgnoreCase("VERIFY")) {
            String user = argument;
            if (user.equals("")) {
                out.println("usage: verify [username]");
                return true;
            }
            if (users.contains(user)) {
                out.println("User " + user + " exist");
            } else {
                out.println("User " + user + " does not exist");
            }
        } else if (command.equalsIgnoreCase("HELP")) {
            out.println("Currently implemented commans:");
            out.println("help                            display this help");
            out.println("adduser [username] [password]   add a new user");
            out.println("deluser [username]              delete existing user");
            out.println("listusers                       display existing accounts");
            out.println("countusers                      display the number of existing accounts");
            out.println("verify [username]               verify if specified user exist");
            out.println("quit                            close connection");
            out.flush();
        } else if (command.equalsIgnoreCase("QUIT")) {
            out.println("bye");
            return false;
        } else {
            out.println("unknown command " + command);
        }
        return true;
    }

    public void destroy() {
    }
}

