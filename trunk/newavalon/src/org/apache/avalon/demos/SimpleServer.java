/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE file.
 */
package org.apache.avalon.demos;


import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.Iterator;
import java.util.StringTokenizer;

import org.apache.log.LogKit;
import org.apache.log.Logger;
import org.apache.avalon.Configurable;
import org.apache.avalon.Configuration;
import org.apache.avalon.ConfigurationException;
import org.apache.avalon.Component;
import org.apache.avalon.blocks.AbstractBlock;
import org.apache.avalon.services.*;

/**
 * This is a demo block used to demonstrate a simple server using Avalon. The
 * server listens on a port specified in .confs. All commands are one line
 * commands. It understands three commands: PUT, COUNT, LIST.
 * <br>PUT <string> stores the given string on the file system
 * <br>COUNT counts the number of strings stored
 * <br>LIST responds with all the strings, one per line.
 *
 * @author Charles Benett <charles@benett1.demon.co.uk>
 */
public class SimpleServer extends AbstractBlock implements SimpleService {
    protected final static boolean        LOG        = true;
    protected final static boolean        DEBUG      = LOG && false;
    protected Logger LOGGER = LOG ? LogKit.getLoggerFor("Demo") : null;
    private BufferedReader in;
    private PrintWriter out;
    //private TimeServer timeServer;
    Store.ObjectRepository repository;
    private String remoteHost;
    private String remoteIP;
    private int count = 0;

    public void init() throws Exception {
        super.init();
        if( LOG ) LOGGER.info("init Demo ...");
  
	Configuration repConf = m_configuration.getChild("repository");
	if( LOG ) LOGGER.info("Want to use repository in:" + repConf.getAttribute("destinationURL"));
	Store testStore = (Store) m_componentManager.lookup("org.apache.avalon.services.Store");
	if (testStore != null) {
	    Component c = testStore.select(repConf);
	    repository = (Store.ObjectRepository) c;
	    if( LOG ) LOGGER.info("Got repository");
	} else {
	    if( LOG ) LOGGER.info("Whoops! store component is null");
	}

	Configuration portConf = m_configuration.getChild("port");
	int port = portConf.getValueAsInt();
	if( LOG ) LOGGER.info("Want to open port on:" + port);
	SocketServer socketServer = (SocketServer) m_componentManager.lookup("org.apache.avalon.services.SocketServer");
	if (socketServer != null) {
	    socketServer.openListener("DemoListener", SocketServer.DEFAULT, port, this);
	    if( LOG ) LOGGER.info("Got socket");
	} else {
	    if( LOG ) LOGGER.info("Whoops! socketServer component is null");
	}


	//timeServer = (TimeServer) m_componentManager.lookup("

        if( LOG ) LOGGER.info("...Demo init");
    }
    
  
   public void parseRequest(Socket socket) {

       try {
	   in = new BufferedReader(new InputStreamReader(socket.getInputStream()), 1024);
	   out = new PrintWriter(new BufferedOutputStream(socket.getOutputStream()), true);

	   remoteHost = socket.getInetAddress ().getHostName ();
	   remoteIP = socket.getInetAddress ().getHostAddress ();
	   if( LOG ) LOGGER.info("Connection from " + remoteHost + " (" + remoteIP + ")");
	   
	   //Greet connection
	   out.println("Welcome to the Avalon Demo Server!");
	   
	   // Handle connection
	   while  (parseCommand(in.readLine())) {
	       // timeServer.resetAlarm(this.toString());
	   }
	   
	   //Finish
	   out.flush();
	   socket.close();
       } catch (SocketException e) {
	   if( LOG ) LOGGER.info("Socket to " + remoteHost + " closed remotely.");
       } catch (InterruptedIOException e) {
	   if( LOG ) LOGGER.info("Socket to " + remoteHost + " timeout.");
       } catch (IOException e) {
	   if( LOG ) LOGGER.info("Exception handling socket to " + remoteHost + ":" + e.getMessage());
       } catch (Exception e) {
	   if( LOG ) LOGGER.info("Exception on socket: " + e.getMessage());
       } finally {
	   try {
	       socket.close();
	   } catch (IOException e) {
	       if( LOG ) LOGGER.error("Exception closing socket: " + e.getMessage());
	   }
       }
   }

    private boolean parseCommand(String command)
	throws Exception {

        if (command == null) return false;
        if( LOG ) LOGGER.info("Command received: " + command);

	StringTokenizer commandLine = new StringTokenizer(command.trim());
        int arguments = commandLine.countTokens();
	String argument = null;
        if (arguments == 0) {
            return true;
        }
	String fullcommand = command;
	command = commandLine.nextToken();
	if(arguments > 1) {
	    argument = fullcommand.substring(command.length() + 1);
        }
       
	if (command.equalsIgnoreCase("TEST")) {
	    out.println("You said 'TEST'");
	    DummyClass write = new DummyClass();
	    write.setName(argument);
	    try {
		repository.put(argument, write);
	    } catch (Exception e1) {
		LOGGER.warn("Exception putting into repository: " + e1);
		e1.printStackTrace();
	    }
	    out.println("Dummy written, trying for read");
	    try {
		Iterator it = repository.list();
	    } catch (Exception e2) {
		LOGGER.warn("Exception putting into repository: " + e2);
		e2.printStackTrace();
	    }
	    DummyClass read = null;
	    try {
		read = (DummyClass) repository.get(argument);
	    } catch (Exception e3) {
		LOGGER.warn("Exception reading from repository: " + e3);
		e3.printStackTrace();
	    }
	    out.println("Recovered: " + read.getName());
	    return true;
	} else if (command.equalsIgnoreCase("PUT")) {
	    out.println("You said 'PUT'");
	    String key = "AMsg" + ++count;
	    repository.put(key, argument);
	    return true;
	} else if (command.equalsIgnoreCase("LIST")) {
	    out.println("You said 'LIST'");
	    Iterator it = repository.list();
	    while (it.hasNext()) {
		String k = (String)it.next();
		String txt = (String) repository.get(k);
		out.println("Msg " + k + " was " + txt);
	    }
	    out.println("That's All folks!");
	    return true;
	} else if (command.equalsIgnoreCase("COUNT")) {
	    out.println("You said 'COUNT'");
	    Iterator it = repository.list();
	    int c = 0;
	    while (it.hasNext()) {
		Object ignore = it.next();
		c=c+1;
	    }
	    out.println("Number of messages in repository is: " + c);
	    return true;
	} else {
	    out.println("Only valid commands are: PUT, LIST or COUNT.");
	    return true;
	}
    }
}


