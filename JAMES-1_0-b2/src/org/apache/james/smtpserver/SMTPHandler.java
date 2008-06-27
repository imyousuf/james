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
import org.apache.java.io.CharTerminatedInputStream;
import javax.mail.*;
import javax.mail.internet.*;

/**
 * This handles an individual incoming message.  It handles regular SMTP
 * commands, and when it receives a message, adds it to the spool.
 * @author Serge Knystautas <sergek@lokitech.com>
 * @author Federico Barbieri <scoobie@systemy.it>
 * @version 0.9
 */
public class SMTPHandler implements Composer, Configurable, Stoppable, TimeServer.Bell, Contextualizable {

	public final static String SERVER_NAME = "SERVER_NAME";
	public final static String SERVER_TYPE = "SERVER_TYPE";
	public final static String REMOTE_NAME = "REMOTE_NAME";
	public final static String REMOTE_IP = "REMOTE_IP";
	public final static String NAME_GIVEN = "NAME_GIVEN";
	public final static String CURRENT_HELO_MODE = "CURRENT_HELO_MODE";
    public final static String SENDER = "SENDER_ADDRESS";
    public final static String RCPT_VECTOR = "RCPT_VECTOR";
    private static final char[] SMTPTerminator = {'\r','\n','.','\r','\n'};
    
    private Socket socket;
    private BufferedReader in;
    private InputStream socketIn;
    private PrintWriter out;

    private String remoteHost;
    private String remoteHostGiven;
    private String remoteIP;
    private String messageID;

    private ComponentManager comp;
    private Configuration conf;
    private Context context;
    private Logger logger;
    private TimeServer timeServer;
    private MailServer mailServer;

    private String servername;
    private String softwaretype = "JAMES SMTP Server " + Constants.SOFTWARE_VERSION;
    private static long count;
    private Hashtable state;

    public SMTPHandler() {
    }

    public void setConfiguration(Configuration conf) {
        this.conf = conf;
    }
    
    public void setContext(Context context) {
        this.context = context;
    }
    
    public void setComponentManager(ComponentManager comp) {
        this.comp = (SimpleComponentManager) comp;
    }
    
    public void init()
    throws Exception {
        logger = (Logger) comp.getComponent(Interfaces.LOGGER);
        mailServer = (MailServer) comp.getComponent(Interfaces.MAIL_SERVER);
        timeServer = (TimeServer) comp.getComponent(Interfaces.TIME_SERVER);
        servername = (String) context.get(Constants.HELO_NAME);
        state = new Hashtable();
    }

    public void parseRequest(Socket socket) {

        try {
            this.socket = socket;
            socketIn = new BufferedInputStream(socket.getInputStream(), 1024);
            in = new BufferedReader(new InputStreamReader(socketIn));
            out = new PrintWriter(socket.getOutputStream(), true);
    
            remoteHost = socket.getInetAddress ().getHostName ();
            remoteIP = socket.getInetAddress ().getHostAddress ();
            state.clear();
            state.put(SERVER_NAME, this.servername );
            state.put(SERVER_TYPE, this.softwaretype );
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

            timeServer.setAlarm(this.toString(), this, conf.getConfiguration("connectiontimeout", "120000").getValueAsLong());
            out.println("220 " + this.servername + " SMTP Server (" + softwaretype + ") ready " + RFC822DateFormat.toString(new Date()));

            while  (parseCommand(in.readLine())) {
                timeServer.resetAlarm(this.toString());
            }
            socket.close();
            timeServer.removeAlarm("RemoteManager");
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

    public void wake(String name, String memo) {
        logger.log("Connection timeout on socket", "SMTPServer", logger.ERROR);
        try {
            out.println("Connection timeout. Closing connection");
            socket.close();
        } catch (IOException e) {
        }
    }

    private boolean parseCommand(String command)
    throws Exception {
            
        if (command == null) return false;
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
            // NOOP Command
        } else if (command.equalsIgnoreCase("NOOP")) {
                out.println("250 OK");
                return true;
            // DATA Command
        } else if (command.equalsIgnoreCase("RSET")) {
                resetState();
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
                try {
                    mailServer.sendMail((String) state.get(SENDER), (Vector) state.get(RCPT_VECTOR), new CharTerminatedInputStream(socketIn, SMTPTerminator));
                } catch (MessagingException me) {
                    out.println("451 Error processing message: " + me.getMessage());
                    logger.log("Error processing message: " + me.getMessage(), "SMTPServer", logger.ERROR);
                    return true;
                }
                logger.log("Mail sent to Mail Server", "SMTPServer", logger.INFO);
                resetState();
                out.println("250 Message received");
                return true;
            }
        } else if (command.equalsIgnoreCase("QUIT")) {
            out.println("221 " + state.get(SERVER_NAME) + " Service closing transmission channel");
            return false;
        } else {
            out.println("500 " + state.get(SERVER_NAME) + " Syntax error, command unrecognized: " + command);
            return true;
        }
    }

    private void resetState() {
        state.clear();
        state.put(SERVER_NAME, this.servername );
        state.put(SERVER_TYPE, this.softwaretype );
    }
    
    public void stop() {
    }
}
