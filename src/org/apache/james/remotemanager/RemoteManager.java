/*
 * Copyright (c) 1999 The Java Apache Project.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. All advertising materials mentioning features or use of this
 *    software must display the following acknowledgment:
 *    "This product includes software and design ideas developed by the Java
 *    Apache Project (http://java.apache.org/)."
 *
 * 4. Redistributions of any form whatsoever must retain the following
 *    acknowledgment:
 *    "This product includes software and design ideas developed by the Java
 *    Apache Project (http://java.apache.org/)."
 *
 * THIS SOFTWARE IS PROVIDED BY THE JAVA APACHE PROJECT "AS IS" AND ANY
 * EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE JAVA APACHE PROJECT OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Java Apache Project. For more information
 * on the Java Apache Project please see <http://java.apache.org/>.
 */

/**
 * @version 1.0.0, 24/04/1999
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 */

package org.apache.james.remotemanager;

import org.apache.avalon.blocks.*;
import org.apache.avalon.*;
import org.apache.avalon.util.*;
import org.apache.java.util.*;
import org.apache.java.recycle.*;
import org.apache.java.lang.*;
import org.apache.james.*;
import java.net.*;
import java.io.*;
import java.util.*;

public class RemoteManager implements SocketHandler, Block {

    private Configuration conf;
    private Logger logger;
    private Store store;
    private Store.ObjectRepository userRepository;

    private Socket socket;
    private BufferedReader in;
    private InputStream socketIn;
    private PrintWriter out;
    private OutputStream r_out;
    private Hashtable admaccount;

    public RemoteManager() {}

    public void init(Context context) throws Exception {

        this.conf = context.getConfiguration();
        this.logger = (Logger) context.getImplementation(Interfaces.LOGGER);
        logger.log("RemoteManager init...", "RemoteManager", logger.INFO);
        this.store = (Store) context.getImplementation(Interfaces.STORE);
        admaccount = new Hashtable();
        for (Enumeration e = conf.getChild("AdministratorAccounts").getChildren("Account"); e.hasMoreElements();) {
            Configuration c = (Configuration) e.nextElement();
            admaccount.put(c.getChild("Login").getValueAsString(), c.getChild("Password").getValueAsString());
        }
        if (admaccount.isEmpty()) {
            logger.log("No Administrative account defined", "RemoteManager", logger.WARNING);
        }
        try {
            this.userRepository = (Store.ObjectRepository) store.getPublicRepository("MailUsers");
        } catch (RuntimeException e) {
            this.userRepository = (Store.ObjectRepository) store.getNewPublicRepository(Store.OBJECT, Store.ASYNCHRONOUS, "MailUsers", "file://localhost/../james/spool/passwd/");
        }
        logger.log("RemoteManager ...init end", "RemoteManager", logger.INFO);
    }

    public void parseRequest(Socket s) {

        try {
            socketIn = s.getInputStream();
            in = new BufferedReader(new InputStreamReader(socketIn));
            r_out = s.getOutputStream();
            out = new PrintWriter(r_out, true);
//            String remoteHost = socket.getInetAddress ().getHostName ();
//            String remoteIP = socket.getInetAddress ().getHostAddress ();
            String remoteHost = "maggie";
            String remoteIP = "192.168.1.3";
            logger.log("Access from " + remoteHost + "(" + remoteIP + ")", "RemoteManager", logger.INFO);
            out.println("James Remote mailbox administration tool");
            String login = in.readLine();
            String password = in.readLine();
            while (!password.equals(admaccount.get(login))) {
                out.println("Login failed for " + login);
                logger.log("Login for " + login + " failed", "RemoteManager", logger.INFO);
                login = in.readLine();
                password = in.readLine();
            }
            out.println("Welcome " + login + ". HELP for a list of commands");
            logger.log("Login for " + login + " succesful", "RemoteManager", logger.INFO);
            while (parseCommand(in.readLine()));
            s.close();
        } catch (IOException e) {
            out.println("Error. Closing connection");
            out.flush();
//            logger.log("Exception during connection from " + remoteHost + " (" + remoteIP + ")", "RemoteManager", logger.ERROR);
        }
    }

    private boolean parseCommand(String command) {
        command = command.trim().toUpperCase();
        if (command.startsWith("ADDUSER")) {
            int sep = command.indexOf(",");
            String user = command.substring(8, sep);
            String passwd = command.substring(sep + 1);
            if (user.equals("") || passwd.equals("")) {
                out.println("Cannot add user with empty login or password");
                out.flush();
                return true;
            }
            userRepository.store(user, passwd);
            out.println("User " + user + " added");
            out.flush();
            logger.log("User " + user + " added", "RemoteManager", logger.INFO);
        } else if (command.startsWith("DELUSER")) {
            String user = command.substring(8);
            if (user.equals("")) {
                out.println("usage: deluser [username]");
                out.flush();
                return true;
            }
            try {
                userRepository.remove(user);
            } catch (Exception e) {
                out.println("Error deleting user " + user + " : " + e.getMessage());
                return true;
            }
            out.println("User " + user + " deleted");
            out.flush();
            logger.log("User " + user + " deleted", "RemoteManager", logger.INFO);
        } else if (command.startsWith("LISTUSERS")) {
            out.println("Existing accounts:");
            for (Enumeration e = userRepository.list(); e.hasMoreElements();) {
                out.println("user: " + (String) e.nextElement());
            }
            out.flush();
        } else if (command.startsWith("HELP")) {
            out.println("Currently implemented commans:");
            out.println("help                          display this help");
            out.println("adduser [login],[password]    add a new user");
            out.println("listusers                     display existing accounts");
            out.println("quit                          close connection");
            out.flush();
        } else if (command.startsWith("QUIT")) {
            out.println("bye");
            out.flush();
            return false;
        } else {
            out.println("unknown command " + command);
            out.flush();
        }
        return true;
    }

    public void destroy() {
    }

    public BlockInfo getBlockInfo() {
        // fill me
        return (BlockInfo) null;
    }
}
    
