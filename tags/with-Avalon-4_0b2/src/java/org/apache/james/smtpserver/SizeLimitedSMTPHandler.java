/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.smtpserver;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;
import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.component.Composable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.ContextException;
import org.apache.avalon.framework.context.Contextualizable;
import org.apache.avalon.framework.logger.AbstractLoggable;
import org.apache.avalon.cornerstone.services.connection.ConnectionHandler;
import org.apache.avalon.cornerstone.services.scheduler.PeriodicTimeTrigger;
import org.apache.avalon.cornerstone.services.scheduler.Target;
import org.apache.avalon.cornerstone.services.scheduler.TimeScheduler;
import org.apache.james.*;
import org.apache.james.core.*;
import org.apache.james.services.MailServer;
import org.apache.james.util.*;
import org.apache.mailet.*;

/**
 * This handles an individual incoming message.  It handles regular SMTP
 * commands, and when it receives a message, adds it to the spool.
 * @author Serge Knystautas <sergek@lokitech.com>
 * @author Federico Barbieri <scoobie@systemy.it>
  * @author Matthew Pangaro <mattp@lokitech.com>
 * @version 0.9.1
 */
public class SizeLimitedSMTPHandler
    extends BaseConnectionHandler
    implements ConnectionHandler, Composable, Configurable, Target {

    public final static String SERVER_NAME = "SERVER_NAME";
    public final static String SERVER_TYPE = "SERVER_TYPE";
    public final static String REMOTE_NAME = "REMOTE_NAME";
    public final static String REMOTE_IP = "REMOTE_IP";
    public final static String NAME_GIVEN = "NAME_GIVEN";
    public final static String CURRENT_HELO_MODE = "CURRENT_HELO_MODE";
    public final static String SENDER = "SENDER_ADDRESS";
    public final static String MESG_FAILED = "MESG_FAILED";
    public final static String RCPT_VECTOR = "RCPT_VECTOR";
    public final static String SMTP_ID = "SMTP_ID";
    public final static char[] SMTPTerminator = {'\r','\n','.','\r','\n'};

    private Socket socket;
    private DataInputStream in;
    private PrintWriter out;

    private String remoteHost;
    private String remoteHostGiven;
    private String remoteIP;
    private String messageID;
    private String smtpID;

    private Configuration conf;
    private Context context;
    private TimeScheduler scheduler;
    private MailServer mailServer;

    private String softwaretype = "JAMES SMTP Server " + Constants.SOFTWARE_VERSION;
    private static long count;

    private Hashtable state     = new Hashtable();
    private Random random       = new Random();
    private long maxmessagesize;

    public void compose( final ComponentManager componentManager )
        throws ComponentException {
        mailServer = (MailServer)componentManager.
            lookup("org.apache.james.services.MailServer");
        scheduler = (TimeScheduler)componentManager.
            lookup("org.apache.avalon.cornerstone.services.scheduler.TimeScheduler");
    }

    public void configure(Configuration conf) throws ConfigurationException {
        this.conf = conf;
        timeout = conf.getChild( "connectiontimeout" ).getValueAsInteger( 120000 );
        // get the message size limit from the conf file and multiply
        //by 1024, to put it in bytes
        maxmessagesize =
            conf.getChild( "maxmessagesize" ).getValueAsLong( 0 ) * 1024;
    }

    /**
     * Handle a connection.
     * This handler is responsible for processing connections as they occur.
     *
     * @param connection the connection
     * @exception IOException if an error reading from socket occurs
     * @exception ProtocolException if an error handling connection occurs
     */
    public void handleConnection( Socket connection )
        throws IOException {

        try {
            this.socket = socket;
            final InputStream bufferedInput =
                new BufferedInputStream( socket.getInputStream(), 1024 );
            in = new DataInputStream( bufferedInput );
            out = new InternetPrintWriter(socket.getOutputStream(), true);

            remoteHost = socket.getInetAddress ().getHostName ();
            remoteIP = socket.getInetAddress ().getHostAddress ();
            smtpID = Math.abs(random.nextInt() % 1024) + "";
            state.clear();
            state.put(SERVER_NAME, this.helloName );
            state.put(SERVER_TYPE, this.softwaretype );
            state.put(REMOTE_NAME, remoteHost);
            state.put(REMOTE_IP, remoteIP);
            state.put(SMTP_ID, smtpID);
        } catch (Exception e) {
            final String message =
                "Cannot open connection from " + remoteHost + " (" + remoteIP + "): " +
                e.getMessage();
            getLogger().error( message, e );
            throw new RuntimeException( message );
        }

        getLogger().info( "Connection from " + remoteHost + " (" + remoteIP + ")" );

        try {
            // Initially greet the connector
            // Format is:  Sat,  24 Jan 1998 13:16:09 -0500

            final PeriodicTimeTrigger trigger = new PeriodicTimeTrigger( timeout, -1 );
            scheduler.addTrigger( this.toString(), trigger, this );
            out.println("220 " + this.helloName + " SMTP Server (" + softwaretype + ") ready " + RFC822DateFormat.toString(new Date()));

            while  (parseCommand(in.readLine())) {
                scheduler.resetTrigger(this.toString());
            }
            socket.close();
            scheduler.removeTrigger(this.toString());
        } catch ( final SocketException se ) {
            getLogger().debug( "Socket to " + remoteHost + " closed remotely.", se );
        } catch ( final InterruptedIOException iioe ) {
            getLogger().debug( "Socket to " + remoteHost + " timeout.", iioe );
        } catch ( final IOException ioe ) {
            getLogger().debug("Exception handling socket to " + remoteHost + ":"
                              + ioe.getMessage(), ioe );
        } catch ( final Exception e ) {
            getLogger().debug( "Exception opening socket: " + e.getMessage(), e );
        } finally {
            try {
                socket.close();
            } catch ( final IOException ioe ) {
                getLogger().error( "Exception closing socket: " + ioe.getMessage(), ioe );
            }
        }
    }

    public void targetTriggered( final String triggerName ) {
        getLogger().error("Connection timeout on socket");
        try {
            out.println("Connection timeout. Closing connection");
            socket.close();
        } catch (IOException e) {
        }
    }

    private boolean parseCommand(String command)
        throws Exception {

        if (command == null) return false;
        if (state.get(MESG_FAILED) == null) {
            getLogger().info("Command received: " + command);
        }
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
                out.println("250 SIZE " + maxmessagesize);
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
                String sender = argument1.trim();
                if (!sender.startsWith("<") || !sender.endsWith(">")) {
                    out.println("501 Syntax error in parameters or arguments");
                    getLogger().error("Error parsing sender address: " + sender
                                      + ": did not start and end with < >");
                    return true;
                }
                MailAddress senderAddress = null;
                //Remove < and >
                sender = sender.substring(1, sender.length() - 1);
                try {
                    senderAddress = new MailAddress(sender);
                } catch (Exception pe) {
                    out.println("501 Syntax error in parameters or arguments");
                    getLogger().error("Error parsing sender address: " + sender
                                      + ": " + pe.getMessage());
                    return true;
                }
                state.put(SENDER, senderAddress);
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
                Collection rcptColl = (Collection) state.get(RCPT_VECTOR);
                if (rcptColl == null) {
                    rcptColl = new Vector();
                }
                String recipient = argument1.trim();
                if (!recipient.startsWith("<") || !recipient.endsWith(">")) {
                    out.println("Syntax error in parameters or arguments");
                    getLogger().error("Error parsing recipient address: "
                                      + recipient
                                      + ": did not start and end with < >");
                    return true;
                }
                MailAddress recipientAddress = null;
                //Remove < and >
                recipient = recipient.substring(1, recipient.length() - 1);
                try {
                    recipientAddress = new MailAddress(recipient);
                } catch (Exception pe) {
                    out.println("501 Syntax error in parameters or arguments");
                    getLogger().error("Error parsing recipient address: "
                                      + recipient + ": " + pe.getMessage());
                    return true;
                }
                rcptColl.add(recipientAddress);
                state.put(RCPT_VECTOR, rcptColl);
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
                    // parse headers
                    InputStream msgIn = new CharTerminatedInputStream(in, SMTPTerminator);
                    // if the message size limit has been set, we'll
                    //wrap msgIn with a SizeLimitedInputStream
                    if (maxmessagesize > 0) {
                        msgIn =
                            new SizeLimitedInputStream(msgIn, maxmessagesize);
                    }
                    MailHeaders headers = new MailHeaders(msgIn);
                    // if headers do not contains minimum REQUIRED headers fields add them
                    if (!headers.isSet("Date")) {
                        headers.setHeader("Date", RFC822DateFormat.toString (new Date ()));
                    }
                    /*
                      We no longer add To as this in practice is not set (from what I've seen)
                      if (!headers.isSet("To")) {
                      headers.setHeader("To", );
                      }
                    */
                    if (!headers.isSet("From")) {
                        headers.setHeader("From", state.get(SENDER).toString());
                    }

                    String received = "from " + state.get(REMOTE_NAME) + " ([" + state.get(REMOTE_IP)
                        + "])\r\n          by " + this.helloName + " ("
                        + softwaretype + ") with SMTP ID " + state.get(SMTP_ID);
                    if (((Collection)state.get(RCPT_VECTOR)).size () == 1) {
                        //Only indicate a recipient if they're the only recipient
                        //(prevents email address harvesting and large headers in bulk email)
                        received += "\r\n          for <"
                            + ((Vector)state.get(RCPT_VECTOR)).elementAt(0).toString() + ">";
                    }
                    received += ";\r\n          " + RFC822DateFormat.toString (new Date ());
                    headers.addHeader ("Received", received);

                    // headers.setReceivedStamp("Unknown", (String) helloName.elementAt(0));
                    ByteArrayInputStream headersIn = new ByteArrayInputStream(headers.toByteArray());
                    MailImpl mail = new MailImpl(mailServer.getId(), (MailAddress)state.get(SENDER), (Vector)state.get(RCPT_VECTOR), new SequenceInputStream(headersIn, msgIn));
                    //call mail.getSize() to force the message to be
                    //loaded. Need to do this to limit the size
                    mail.getSize();
                    mail.setRemoteHost((String)state.get(REMOTE_NAME));
                    mail.setRemoteAddr((String)state.get(REMOTE_IP));
                    mailServer.sendMail(mail);
                } catch (MessagingException me) {
                    //Grab any exception attached to this one.
                    Exception e = me.getNextException();

                    //If there was an attached exception, and it's a
                    //MessageSizeException
                    if (e != null && e instanceof MessageSizeException) {
                        getLogger().error("552 Error processing message: "
                                          + e.getMessage());
                        //Add an item to the state to suppress
                        //logging of extra lines of data
                        //  that are sent after the size limit has
                        //been hit.
                        state.put(MESG_FAILED, Boolean.TRUE);

                        //then let the client know that the size
                        //limit has been hit.
                        out.println("552 Error processing message: "
                                    + e.getMessage());
                    } else {
                        out.println("451 Error processing message: "
                                    + me.getMessage());
                        getLogger().error("Error processing message: " +
                                          me.getMessage());
                    }
                    return true;
                }
                getLogger().info("Mail sent to Mail Server");
                resetState();
                out.println("250 Message received");
                return true;
            }
        } else if (command.equalsIgnoreCase("QUIT")) {
            out.println("221 " + state.get(SERVER_NAME) + " Service closing transmission channel");
            return false;
        } else {
            if (state.get(MESG_FAILED) == null) {
                out.println("500 " + state.get(SERVER_NAME) + " Syntax error, command unrecognized: " + command);
            }
            return true;
        }
    }

    private void resetState() {
        state.clear();
        state.put(SERVER_NAME, this.helloName );
        state.put(SERVER_TYPE, this.softwaretype );
        state.put(REMOTE_NAME, remoteHost);
        state.put(REMOTE_IP, remoteIP);
        state.put(SMTP_ID, smtpID);
    }
}
