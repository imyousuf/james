/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.remotemanager;

import org.apache.avalon.blocks.*;
import org.apache.james.*;
import org.apache.arch.*;
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
public class RemoteManager implements SocketHandler, Block, TimeServer.Bell {

    private ComponentManager comp;
    private Configuration conf;
    private Logger logger;
    private Store store;
    private Store.ObjectRepository userRepository;
    private TimeServer timeServer;

    private BufferedReader in;
    private InputStream socketIn;
    private PrintWriter out;
    private OutputStream r_out;
    private Hashtable admaccount;
    private Socket socket;

    public RemoteManager() {}

    public void setConfiguration(Configuration conf) {
        this.conf = conf;
    }
    
    public void setComponentManager(ComponentManager comp) {
        this.comp = comp;
    }

	public void init() throws Exception {

        this.logger = (Logger) comp.getComponent(Interfaces.LOGGER);
        logger.log("RemoteManager init...", "RemoteManager", logger.INFO);
        this.store = (Store) comp.getComponent(Interfaces.STORE);
        this.timeServer = (TimeServer) comp.getComponent(Interfaces.TIME_SERVER);
        admaccount = new Hashtable();
        for (Enumeration e = conf.getConfigurations("AdministratorAccounts.Account"); e.hasMoreElements();) {
            Configuration c = (Configuration) e.nextElement();
            admaccount.put(c.getAttribute("login"), c.getAttribute("password"));
        }
        if (admaccount.isEmpty()) {
            logger.log("No Administrative account defined", "RemoteManager", logger.WARNING);
        }
        try {
            this.userRepository = (Store.ObjectRepository) store.getPublicRepository("MailUsers");
        } catch (RuntimeException e) {
            logger.log("Cannot open public Repository MailUsers", "RemoteManager", logger.ERROR);
            throw e;
        }
        logger.log("RemoteManager ...init end", "RemoteManager", logger.INFO);
    }

    public void parseRequest(Socket s) {

        timeServer.setAlarm("RemoteManager", this, conf.getConfiguration("connectiontimeout", "120000").getValueAsLong());
        socket = s;
        String remoteHost = s.getInetAddress().getHostName();
        String remoteIP = s.getInetAddress().getHostAddress();
        try {
            socketIn = s.getInputStream();
            in = new BufferedReader(new InputStreamReader(socketIn));
            r_out = s.getOutputStream();
            out = new PrintWriter(r_out, true);
            logger.log("Access from " + remoteHost + "(" + remoteIP + ")", "RemoteManager", logger.INFO);
            out.println("James Remote mailbox administration tool");
            out.println("Please enter your login and password");
            String login = in.readLine();
            String password = in.readLine();
            while (!password.equals(admaccount.get(login))) {
                out.println("Login failed for " + login);
                logger.log("Login for " + login + " failed", "RemoteManager", logger.INFO);
                login = in.readLine();
                password = in.readLine();
            }
            timeServer.resetAlarm("RemoteManager");
            out.println("Welcome " + login + ". HELP for a list of commands");
            logger.log("Login for " + login + " succesful", "RemoteManager", logger.INFO);
            while (parseCommand(in.readLine())) {
                timeServer.resetAlarm("RemoteManager");
            }
            logger.log("Logout for " + login + ".", "RemoteManager", logger.INFO);
            s.close();
        } catch (IOException e) {
            out.println("Error. Closing connection");
            out.flush();
            logger.log("Exception during connection from " + remoteHost + " (" + remoteIP + ")", "RemoteManager", logger.ERROR);
        }
        timeServer.removeAlarm("RemoteManager");
    }
    
    public void wake(String name, String memo) {
        logger.log("Connection timeout on socket", "RemoteManager", logger.ERROR);
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
            userRepository.store(user, passwd);
            out.println("User " + user + " added");
            out.flush();
            logger.log("User " + user + " added", "RemoteManager", logger.INFO);
        } else if (command.equalsIgnoreCase("DELUSER")) {
            String user = argument;
            if (user.equals("")) {
                out.println("usage: deluser [username]");
                return true;
            }
            try {
                userRepository.remove(user);
            } catch (Exception e) {
                out.println("Error deleting user " + user + " : " + e.getMessage());
                return true;
            }
            out.println("User " + user + " deleted");
            logger.log("User " + user + " deleted", "RemoteManager", logger.INFO);
        } else if (command.equalsIgnoreCase("LISTUSERS")) {
            out.println("Existing accounts:");
            for (Enumeration e = userRepository.list(); e.hasMoreElements();) {
                out.println("user: " + (String) e.nextElement());
            }
        } else if (command.equalsIgnoreCase("HELP")) {
            out.println("Currently implemented commans:");
            out.println("help                            display this help");
            out.println("adduser [username] [password]   add a new user");
            out.println("deluser [username]              delete existing user");
            out.println("listusers                       display existing accounts");
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
    
