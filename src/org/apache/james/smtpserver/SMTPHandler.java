/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/
package org.apache.james.smtpserver;

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
import org.apache.avalon.blocks.*;
import org.apache.james.*;
import org.apache.arch.*;

/**
 * This handles an individual incoming message.  It handles regular SMTP
 * commands, and when it receives a message, adds it to the spool.
 * @author Serge Knystautas <sergek@lokitech.com>
 * @author Federico Barbieri <scoobie@systemy.it>
 * @version 0.9
 */
public class SMTPHandler implements Composer, Configurable, Stoppable {

	public static String SERVER_NAME = "SERVER_NAME";
	public static String POSTMASTER = "POSTMASTER";
	public static String SERVER_TYPE = "SERVER_TYPE";
	public static String REMOTE_NAME = "REMOTE_NAME";
	public static String REMOTE_IP = "REMOTE_IP";
	public static String NAME_GIVEN = "NAME_GIVEN";
	public static String CURRENT_HELO_MODE = "CURRENT_HELO_MODE";
    public static String SENDER = "SENDER_ADDRESS";
    public static String RCPT_VECTOR = "RCPT_VECTOR";
    
    private Socket socket;
    private BufferedReader in;
    private InputStream socketIn;
    private PrintWriter out;
    private OutputStream r_out;

    private String remoteHost;
    private String remoteHostGiven;
    private String remoteIP;
    private String messageID;

    private ComponentManager comp;
    private Configuration conf;
    private MessageSpool spool;    
    private Logger logger;
    private IdProvider idp;

    private String servername;
    private String postmaster;
    private String softwaretype;
    
    private Hashtable state;

    public SMTPHandler() {
    }

    public void setConfiguration(Configuration conf) {
        this.conf = conf;
    }
    
    public void setComponentManager(ComponentManager comp) {
        this.comp = comp;
        logger = (Logger) comp.getComponent(Interfaces.LOGGER);
        spool = (MessageSpool) comp.getComponent("spool");
        idp = (IdProvider) comp.getComponent("idprovider");
        state = new Hashtable();

        servername = conf.getConfiguration("servername", "localhost").getValue();
        postmaster = conf.getConfiguration("postmaster", "postmaster@" + servername).getValue();
        softwaretype = "Apache James SMTP v @@version@@";
    }

    public void parseRequest(Socket socket) {

        try {
            this.socket = socket;
            socketIn = socket.getInputStream();
            in = new BufferedReader(new InputStreamReader(socketIn));
            r_out = socket.getOutputStream();
            out = new PrintWriter(r_out, true);
    
            remoteHost = socket.getInetAddress ().getHostName ();
            remoteIP = socket.getInetAddress ().getHostAddress ();
            state.put(REMOTE_NAME, remoteHost);
            state.put(REMOTE_IP, remoteIP);
        } catch (Exception e) {
            logger.log("Cannot open connection from " + remoteHost + " (" + remoteIP + "): " + e.getMessage(), "SMTPServer", logger.ERROR);
            throw new RuntimeException("Cannot open connection from " + remoteHost + " (" + remoteIP + "): " + e.getMessage());
        }

        logger.log("Connection from " + remoteHost + " (" + remoteIP + ")", "SMTPServer", logger.INFO);
    }
    
    public void run() {
        
        try {
            // Initially greet the connector
            // Format is:  Sat,  24 Jan 1998 13:16:09 -0500

            out.println("220 " + this.servername + " SMTP server (" + this.softwaretype + ") ready " + RFC822DateFormat.toString(new Date()));
            out.flush();

            for  (String commandLine = in.readLine();
                commandLine != null && parseCommand(commandLine);
                commandLine = in.readLine()) out.flush();
            socket.close();

        } catch (SocketException e) {
            logger.log("Socket to " + remoteHost + " closed remotely.", "SMTPServer", logger.DEBUG);
        } catch (InterruptedIOException e) {
            logger.log("Socket to " + remoteHost + " timeout.", "SMTPServer", logger.DEBUG);
        } catch (IOException e) {
            logger.log("Exception handling socket to " + remoteHost + ":" + e.getMessage(), "SMTPServer", logger.DEBUG);
        } catch (Exception e) {
            logger.log("Exception opening socket: " + e.getMessage(), "SMTPServer", logger.DEBUG);
        } finally {
            try {
            socket.close();
            } catch (IOException e) {
                logger.log("Exception closing socket: " + e.getMessage(), "SMTPServer", logger.ERROR);
            }
        }
    }

    private boolean parseCommand(String command)
    throws Exception {
            
        logger.log("Command recieved: " + command, "SMTPServer", logger.INFO);
        StringTokenizer commandLine = new StringTokenizer(command.trim(), " :");
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

        logger.log("Command=" + command + " argument=" + argument + " argument1=" + argument1, "SMTPServer", logger.DEBUG);
            // HELO Command
        if (command.equalsIgnoreCase("HELO")) {
            if (state.containsKey(CURRENT_HELO_MODE)) {
                out.println("250 " + state.get(SERVER_NAME) + " Duplicate HELO/EHLO");
                return true;
            } else if (argument == null) {
                out.println("501 domain address required: " + commandLine);
                return true;
            } else {
                state.put(CURRENT_HELO_MODE, command);
                state.put(NAME_GIVEN, argument);
                out.println("250 " + state.get(SERVER_NAME) + " Hello " + argument + " (" + state.get(REMOTE_NAME) + " [" + state.get(REMOTE_IP) + "])");
                return true;
            }
            // EHLO Command
        } else if (command.equalsIgnoreCase("EHLO")) {
            if (state.containsKey(CURRENT_HELO_MODE)) {
                out.println("250 " + state.get(SERVER_NAME) + " Duplicate HELO/EHLO");
                return true;
            } else if (argument == null) {
                out.println("501 domain address required: " + commandLine);
                return true;
            } else {
                state.put(CURRENT_HELO_MODE, command);
                state.put(NAME_GIVEN, argument);
                out.println("250 " + state.get(SERVER_NAME) + " Hello " + argument + " (" + state.get(REMOTE_NAME) + " [" + state.get(REMOTE_IP) + "])");
                return true;
            }
            // MAIL Command
        } else if (command.equalsIgnoreCase("MAIL")) {
            if (state.containsKey(SENDER)) {
                out.println("503 Sender already specified");
                return true;
            } else if (argument == null || !argument.equalsIgnoreCase("FROM") || argument1 == null) {
                out.println("501 Usage: MAIL FROM:<sender>");
                return true;
            } else {
                String sender = argument1.replace('<', ' ').replace('>', ' ').trim();
                state.put(SENDER, sender);
                out.println("250 Sender <" + sender + "> OK");
                return true;
            }
            // RCPT Command
        } else if (command.equalsIgnoreCase("RCPT")) {
            if (!state.containsKey(SENDER)) {
                out.println("503 Need MAIL before RCPT");
                return true;
            } else if (argument == null || !argument.equalsIgnoreCase("TO") || argument1 == null) {
                out.println("501 Usage: RCPT TO:<recipient>");
                return true;
            } else {
                Vector rcptVector = (Vector) state.get(RCPT_VECTOR);
                if (rcptVector == null) rcptVector = new Vector();
                String recipient = argument1.replace('<', ' ').replace('>', ' ').trim();
                rcptVector.addElement(recipient);
                state.put(RCPT_VECTOR, rcptVector);
                out.println("250 Recipient <" + recipient + "> OK");
                return true;
            }
            // NOP Command
        } else if (command.equalsIgnoreCase("NOP")) {
                out.println("250 OK");
                return true;
            // DATA Command
        } else if (command.equalsIgnoreCase("DATA")) {
            if (!state.containsKey(SENDER)) {
                out.println("503 No sender specified");
                return true;
            } else if (!state.containsKey(RCPT_VECTOR)) {
                out.println("503 No recipients specified");
                return true;
            } else {
                out.println("354 Ok Send data ending with <CRLF>.<CRLF>");
                String messageId = idp.getMessageId();
                OutputStream mout = spool.addMessage(messageId, (String) state.get(SENDER), (Vector) state.get(RCPT_VECTOR));
                for (SMTPInputStream min = new SMTPInputStream(socketIn); !min.hasReachedEnd(); mout.write(min.read()));
                mout.flush();
                mout.close();
                spool.free(messageId);
                resetState();
                out.println("250 Message received: " + messageId);
                return true;
            }
        } else if (command.equalsIgnoreCase("QUIT")) {
            out.println("221 " + state.get(SERVER_NAME) + " Service closing transmission channel");
            return false;
        } else {
            out.println("500 " + state.get(SERVER_NAME) + " Syntax error, command unrecognized: " + commandLine);
            return true;
        }
    }

    private void resetState() {
        state.clear();
        state.put(SERVER_NAME, this.servername );
        state.put(POSTMASTER, this.postmaster );
        state.put(SERVER_TYPE, this.softwaretype );
    }
    
    public void stop() {
    }
}
