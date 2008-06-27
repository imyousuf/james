/***********************************************************************
 * Copyright (c) 2000-2004 The Apache Software Foundation.             *
 * All rights reserved.                                                *
 * ------------------------------------------------------------------- *
 * Licensed under the Apache License, Version 2.0 (the "License"); you *
 * may not use this file except in compliance with the License. You    *
 * may obtain a copy of the License at:                                *
 *                                                                     *
 *     http://www.apache.org/licenses/LICENSE-2.0                      *
 *                                                                     *
 * Unless required by applicable law or agreed to in writing, software *
 * distributed under the License is distributed on an "AS IS" BASIS,   *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or     *
 * implied.  See the License for the specific language governing       *
 * permissions and limitations under the License.                      *
 ***********************************************************************/

package org.apache.james.smtpserver;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.component.Composable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.ContextException;
import org.apache.avalon.framework.context.Contextualizable;
import org.apache.avalon.cornerstone.services.connection.ConnectionHandler;
import org.apache.avalon.cornerstone.services.scheduler.PeriodicTimeTrigger;
import org.apache.avalon.cornerstone.services.scheduler.Target;
import org.apache.avalon.cornerstone.services.scheduler.TimeScheduler;
import org.apache.james.*;
import org.apache.james.core.*;
import org.apache.james.services.MailServer;
import org.apache.james.services.UsersRepository;
import org.apache.james.services.UsersStore;
import org.apache.james.util.*;
import org.apache.mailet.*;
import javax.security.sasl.*;

/**
 * This handles an individual incoming message.  It handles regular SMTP
 * commands, and when it receives a message, adds it to the spool.
 *
 * This is $Revision: 1.1.2.3 $
 * Committed on $Date: 2004/03/15 03:54:14 $ by: $Author: noel $
 */
public class SMTPHandler
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
    public final static String AUTH = "AUTHENTICATED";
    public final static char[] SMTPTerminator = {'\r','\n','.','\r','\n'};
    private final static boolean DEEP_DEBUG = true;

    private Socket socket;
    private DataInputStream in;
    private PrintWriter out;

    private String remoteHost;
    private String remoteHostGiven;
    private String remoteIP;
    private String messageID;
    private String smtpID;

    private boolean authRequired = false;
    private boolean verifyIdentity = false;

    private TimeScheduler scheduler;
    private UsersRepository users;
    private MailServer mailServer;

    private String softwaretype = "JAMES SMTP Server "
                                   + Constants.SOFTWARE_VERSION;
    private static long count;
    private HashMap state       = new HashMap();
    private Random random       = new Random();
    private long maxmessagesize = 0;

    private static SaslServerFactory serverFactory = new cryptix.sasl.ServerFactory();

    public void configure ( Configuration configuration )
           throws ConfigurationException {
        super.configure(configuration);
        authRequired
           = configuration.getChild("authRequired").getValueAsBoolean(true);
        verifyIdentity
           = configuration.getChild("verifyIdentity").getValueAsBoolean(false);
        // get the message size limit from the conf file and multiply
        // by 1024, to put it in bytes
        maxmessagesize =
            configuration.getChild( "maxmessagesize" ).getValueAsLong( 0 ) * 1024;
        if (DEEP_DEBUG) {
            getLogger().debug("Max message size is: " + maxmessagesize);
        }

        Sasl.setSaslServerFactory(serverFactory);
    }

    public void compose( final ComponentManager componentManager )
        throws ComponentException {
        mailServer = (MailServer)componentManager.lookup(
                                 "org.apache.james.services.MailServer");
        scheduler = (TimeScheduler)componentManager.lookup(
            "org.apache.avalon.cornerstone.services.scheduler.TimeScheduler");
        UsersStore usersStore = (UsersStore)componentManager.lookup(
            "org.apache.james.services.UsersStore" );
        users = usersStore.getRepository("LocalUsers");
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
            this.socket = connection;
            final InputStream bufferedInput =
                new BufferedInputStream( socket.getInputStream(), 1024 );
            in = new DataInputStream( bufferedInput );
            out = new InternetPrintWriter(socket.getOutputStream(), true);

            remoteHost = socket.getInetAddress ().getHostName ();
            remoteIP = socket.getInetAddress ().getHostAddress ();
            smtpID = Math.abs(random.nextInt() % 1024) + "";
            resetState();
        } catch (Exception e) {
            getLogger().error("Cannot open connection from " + remoteHost
                              + " (" + remoteIP + "): " + e.getMessage(), e );
            throw new RuntimeException("Cannot open connection from "
                      + remoteHost + " (" + remoteIP + "): " + e.getMessage());
        }

        getLogger().info("Connection from " + remoteHost + " ("
                         + remoteIP + ")");

        try {
            // Initially greet the connector
            // Format is:  Sat,  24 Jan 1998 13:16:09 -0500

            final PeriodicTimeTrigger trigger
                  = new PeriodicTimeTrigger( timeout, -1 );
            scheduler.addTrigger( this.toString(), trigger, this );
            out.println("220 " + this.helloName + " SMTP Server ("
                        + softwaretype + ") ready "
                        + RFC822DateFormat.toString(new Date()));

            while  (parseCommand(in.readLine())) {
                scheduler.resetTrigger(this.toString());
            }
            socket.close();
            scheduler.removeTrigger(this.toString());
        } catch (SocketException se) {
            getLogger().debug("Socket to " + remoteHost
                              + " closed remotely.", se );
        } catch ( InterruptedIOException iioe ) {
            getLogger().debug( "Socket to " + remoteHost + " timeout.", iioe );
        } catch ( IOException ioe ) {
            getLogger().debug( "Exception handling socket to " + remoteHost
                               + ":" + ioe.getMessage(), ioe );
        } catch (Exception e) {
            getLogger().debug( "Exception opening socket: "
                               + e.getMessage(), e );
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                getLogger().error("Exception closing socket: "
                                  + e.getMessage());
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

    private void resetState() {
        state.clear();
        state.put(SERVER_NAME, this.helloName );
        state.put(SERVER_TYPE, this.softwaretype );
        state.put(REMOTE_NAME, remoteHost);
        state.put(REMOTE_IP, remoteIP);
        state.put(SMTP_ID, smtpID);
    }

    private boolean parseCommand(String command)
        throws Exception {

        if (command == null) return false;
        if (state.get(MESG_FAILED) == null) {
            getLogger().info("Command received: " + command);
        }
        StringTokenizer commandLine
            = new StringTokenizer(command.trim(), " :");
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

        if (command.equalsIgnoreCase("HELO"))
            doHELO(command,argument,argument1);
        else if (command.equalsIgnoreCase("EHLO"))
            doEHLO(command,argument,argument1);
        else if (command.equalsIgnoreCase("AUTH"))
            doAUTH(command,argument,argument1);
        else if (command.equalsIgnoreCase("MAIL"))
            doMAIL(command,argument,argument1);
        else if (command.equalsIgnoreCase("RCPT"))
            doRCPT(command,argument,argument1);
        else if (command.equalsIgnoreCase("NOOP"))
            doNOOP(command,argument,argument1);
        else if (command.equalsIgnoreCase("RSET"))
            doRSET(command,argument,argument1);
        else if (command.equalsIgnoreCase("DATA"))
            doDATA(command,argument,argument1);
        else if (command.equalsIgnoreCase("QUIT"))
            doQUIT(command,argument,argument1);
        else
            doUnknownCmd(command,argument,argument1);
        return (command.equalsIgnoreCase("QUIT") == false);
    }

    private void doHELO(String command,String argument,String argument1) {
        if (state.containsKey(CURRENT_HELO_MODE)) {
            out.println("250 " + state.get(SERVER_NAME)
                        + " Duplicate HELO");
        } else if (argument == null) {
            out.println("501 domain address required: " + command);
        } else {
            state.put(CURRENT_HELO_MODE, command);
            state.put(NAME_GIVEN, argument);
            out.println( "250 " + state.get(SERVER_NAME) + " Hello "
                        + argument + " (" + state.get(REMOTE_NAME)
                        + " [" + state.get(REMOTE_IP) + "])");
        }
    }
    private void doEHLO(String command,String argument,String argument1) {
        if (state.containsKey(CURRENT_HELO_MODE)) {
            out.println("250 " + state.get(SERVER_NAME)
                        + " Duplicate EHLO");
        } else if (argument == null) {
            out.println("501 domain address required: " + command);
        } else {
            state.put(CURRENT_HELO_MODE, command);
            state.put(NAME_GIVEN, argument);
	    if (authRequired) {
	        out.println("250-AUTH "+getAuthString());
            }
	    if (maxmessagesize > 0) {
	        out.println("250-SIZE " + maxmessagesize);
            }
            out.println( "250 " + state.get(SERVER_NAME) + " Hello "
                        + argument + " (" + state.get(REMOTE_NAME)
                        + " [" + state.get(REMOTE_IP) + "])");


        }


    }

    private String getAuthString() {
        StringBuffer authString = new StringBuffer("LOGIN ");
        String[] mechanisms = serverFactory.getMechanismNames(null);
        if (mechanisms.length > 0) {
            authString.append(mechanisms[0]);
            for (int i=1;i<mechanisms.length;i++) {
                authString.append(" "+mechanisms[i]);
            }
        }
        return authString.toString();
    }

    private void doAUTH(String command,String argument,String argument1)
            throws Exception {
        String[] mechanisms = serverFactory.getMechanismNames(null);
        if (state.containsKey(AUTH)) {
            out.println("503 User has previously authenticated."
                        + " Further authentication is not required!");
            return;
        } else if (argument == null) {
            out.println("501 Usage: AUTH <"+getAuthString()+
                        "> [<initial response>]");
            return;
//      } else if (argument.equalsIgnoreCase("PLAIN")) {
//          String userpass, user, pass;
//          StringTokenizer authTokenizer;
//          if (argument1 == null) {
//              out.println("334 OK. Continue authentication");
//              userpass = in.readLine().trim();
//          } else
//              userpass = argument1.trim();
//          authTokenizer = new StringTokenizer(
//                            Base64.decodeAsString(userpass), "\0");
//          user = authTokenizer.nextToken();
//          pass = authTokenizer.nextToken();
//          // Authenticate user
//          if (users.test(user, pass)) {
//              state.put(AUTH, user);
//              out.println("235 Authentication Successful");
//              getLogger().info("AUTH method PLAIN succeeded");
//          } else {
//              out.println("535 Authentication Failed");
//              getLogger().error("AUTH method PLAIN failed");
//          }
//          return;
        } else if (argument.equalsIgnoreCase("LOGIN")) {
            String user, pass;

            if (argument1 == null) {
                out.println("334 VXNlcm5hbWU6"); // base64 encoded "Username:"
                user = in.readLine().trim();
            } else
                user = argument1.trim();
            user = Base64.decodeAsString(user);
            out.println("334 UGFzc3dvcmQ6"); // base64 encoded "Password:"
            pass = Base64.decodeAsString(in.readLine().trim());
            //Authenticate user
            if (users.test(user, pass)) {
                state.put(AUTH, user);
                out.println("235 Authentication Successful");
                getLogger().info("AUTH method LOGIN succeeded");
            } else {
                out.println("535 Authentication Failed");
                getLogger().error("AUTH method LOGIN failed");
            }
            return;
        }

        for (int i=0;i<mechanisms.length;i++) {
            if (argument.equalsIgnoreCase(mechanisms[i])) {
                java.util.Hashtable properties = new java.util.Hashtable();
                properties.put("cryptix.sasl.srp.password.file","/tmp/cryptix-sasl/etc/tpasswd");
                properties.put("cryptix.sasl.plain.password.file","/tmp/cryptix-sasl/etc/passwd");
                SaslServer server =
                    Sasl.createSaslServer(mechanisms[i],
                                          "SMTP",
                                          (String)state.get(SERVER_NAME),
                                          properties,
                                          null);
                
                SaslProfile profile = new SaslProfile(server, in, out);
                if (profile.doAUTH(argument1)) { 
                    state.put(AUTH, server.getAuthorizationID());
                    out.println("235 Authentication Successful");
                    getLogger().info("AUTH method "+mechanisms[i]+" succeeded");
                } else {
                    out.println("535 Authentication Failed");
                    getLogger().error("AUTH method "+mechanisms[i]+" failed");
                }
                return;
            }
        }

        out.println("504 Unrecognized Authentication Type");
        getLogger().error("AUTH method " + argument
                          + " is an unrecognized authentication type");
        return;
    }

    private void doMAIL(String command,String argument,String argument1) {
        if (state.containsKey(SENDER)) {
            out.println("503 Sender already specified");
        } else if (argument == null || !argument.equalsIgnoreCase("FROM")
                   || argument1 == null) {
            out.println("501 Usage: MAIL FROM:<sender>");
        } else {
            String sender = argument1.trim();
            if (!sender.startsWith("<") || !sender.endsWith(">")) {
                out.println("501 Syntax error in parameters or arguments");
                getLogger().error("Error parsing sender address: " + sender
                                  + ": did not start and end with < >");
                return;
            }
            MailAddress senderAddress = null;
            //Remove < and >
            sender = sender.substring(1, sender.length() - 1);
            if (sender.length() == 0) {
                //This is the <> case.  Let senderAddress == null
            } else {
                try {
                    senderAddress = new MailAddress(sender);
                } catch (Exception pe) {
                    out.println("501 Syntax error in parameters or arguments");
                    getLogger().error("Error parsing sender address: " + sender
                                      + ": " + pe.getMessage());
                    return;
                }
            }
            state.put(SENDER, senderAddress);
            out.println("250 Sender <" + sender + "> OK");
        }
    }

    private void doRCPT(String command,String argument,String argument1) {
        if (!state.containsKey(SENDER)) {
            out.println("503 Need MAIL before RCPT");
        } else if (argument == null || !argument.equalsIgnoreCase("TO")
                   || argument1 == null) {
            out.println("501 Usage: RCPT TO:<recipient>");
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
                return;
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
                return;
            }
            // If this is a delivery failure notification (MAIL FROM: <>)
            //   we don't enforce authentication
            if (authRequired && state.get(SENDER) != null) {
                // Make sure the mail is being sent locally if not
                // authenticated else reject.

                if (!state.containsKey(AUTH)) {
                    String toDomain = recipientAddress.getHost();

                    if (!mailServer.isLocalServer(toDomain)) {
                        out.println("530 Authentication Required");
                        getLogger().error(
                            "Authentication is required for mail request");
                        return;
                    }
                } else {
                    // Identity verification checking
                    if (verifyIdentity) {
                        String authUser = (String)state.get(AUTH);
                        MailAddress senderAddress
                                = (MailAddress)state.get(SENDER);
                        boolean domainExists = false;

                        if (!authUser.equalsIgnoreCase(
                                senderAddress.getUser())) {
                        out.println("503 Incorrect Authentication for Specified Email Address");
                        getLogger().error("User " + authUser
                                + " authenticated, however tried sending email as "
                                + senderAddress);
                        return;
                    }
                    if (!mailServer.isLocalServer(
                                       senderAddress.getHost())) {
                        out.println("503 Incorrect Authentication for Specified Email Address");
                        getLogger().error("User " + authUser
                            + " authenticated, however tried sending email as "
                            + senderAddress);
                            return;
                        }
                    }
                }
            }
            rcptColl.add(recipientAddress);
            state.put(RCPT_VECTOR, rcptColl);
            out.println("250 Recipient <" + recipient + "> OK");
        }
    }
    private void doNOOP(String command,String argument,String argument1) {
        out.println("250 OK");
    }
    private void doRSET(String command,String argument,String argument1) {
        resetState();
        out.println("250 OK");
    }

    private void doDATA(String command,String argument,String argument1) {
        if (!state.containsKey(SENDER)) {
            out.println("503 No sender specified");
        } else if (!state.containsKey(RCPT_VECTOR)) {
            out.println("503 No recipients specified");
        } else {
            out.println("354 Ok Send data ending with <CRLF>.<CRLF>");
            try {
                // parse headers
                InputStream msgIn
                    = new CharTerminatedInputStream(in, SMTPTerminator);
                // if the message size limit has been set, we'll
                // wrap msgIn with a SizeLimitedInputStream
                if (maxmessagesize > 0) {
                    if (DEEP_DEBUG) {
                        getLogger().debug("Using SizeLimitedInputStream "
                                  + " with max message size: "
                                  + maxmessagesize);
                    }
                    msgIn = new SizeLimitedInputStream(msgIn, maxmessagesize);
                }
                //Removes the dot stuffing
                msgIn = new SMTPInputStream(msgIn);

                //Parse out the message headers
                MailHeaders headers = new MailHeaders(msgIn);

                // if headers do not contains minimum REQUIRED headers fields,
		        // add them
                if (!headers.isSet("Date")) {
                    headers.setHeader("Date",
                            RFC822DateFormat.toString(new Date()));
                }

                if (!headers.isSet("From") && state.get(SENDER) != null) {
                    headers.setHeader("From", state.get(SENDER).toString());
                }

                //Determine the Return-Path
                String returnPath = headers.getHeader("Return-Path", "\r\n");
                headers.removeHeader("Return-Path");
                if (returnPath == null) {
                    if (state.get(SENDER) == null) {
                        returnPath = "<>";
                    } else {
                        returnPath = "<" + state.get(SENDER) + ">";
                    }
                }

                //We will rebuild the header object to put Return-Path and our
                //  Received message at the top
                Enumeration headerLines = headers.getAllHeaderLines();

                headers = new MailHeaders();
                //Put the Return-Path first
                headers.addHeaderLine("Return-Path: " + returnPath);
                //Put our Received header next
                headers.addHeaderLine("Received: from " + state.get(REMOTE_NAME)
                        + " ([" + state.get(REMOTE_IP) + "])");
                String temp = "          by " + this.helloName + " ("
                        + softwaretype + ") with SMTP ID " + state.get(SMTP_ID);
                if (((Collection)state.get(RCPT_VECTOR)).size() == 1) {
                    //Only indicate a recipient if they're the only recipient
                    //(prevents email address harvesting and large headers in
                    // bulk email)
                    headers.addHeaderLine(temp);
                    headers.addHeaderLine("          for <"
                            + ((Vector)state.get(RCPT_VECTOR)).get(0).toString()
                            + ">;");
                } else {
                    //Put the ; on the end of the 'by' line
                    headers.addHeaderLine(temp + ";");
                }
                headers.addHeaderLine("          "
                        + RFC822DateFormat.toString(new Date()));

                //Add all the original message headers back in next
                while (headerLines.hasMoreElements()) {
                    headers.addHeaderLine((String)headerLines.nextElement());
                }

                ByteArrayInputStream headersIn
                    = new ByteArrayInputStream(headers.toByteArray());
                MailImpl mail = new MailImpl(mailServer.getId(),
                                    (MailAddress)state.get(SENDER),
                                    (Vector)state.get(RCPT_VECTOR),
                                    new SequenceInputStream(headersIn, msgIn));
                // if the message size limit has been set, we'll
                // call mail.getSize() to force the message to be
                // loaded. Need to do this to limit the size
                if (maxmessagesize > 0) {
                    mail.getSize();
                }
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
                    // Add an item to the state to suppress
                    // logging of extra lines of data
                    // that are sent after the size limit has
                    // been hit.
                    state.put(MESG_FAILED, Boolean.TRUE);

                    //then let the client know that the size
                    //limit has been hit.
                    out.println("552 Error processing message: "
                                + e.getMessage());
                } else {
                    out.println("451 Error processing message: "
                                + me.getMessage());
                    getLogger().error("Error processing message: "
                                + me.getMessage());
		            me.printStackTrace();
                }
                return;
            }
            getLogger().info("Mail sent to Mail Server");
            resetState();
            out.println("250 Message received");
        }
    }

    private void doQUIT(String command,String argument,String argument1) {
        out.println("221 " + state.get(SERVER_NAME)
                    + " Service closing transmission channel");
    }

    private void doUnknownCmd(String command,String argument,
                              String argument1) {
        if (state.get(MESG_FAILED) == null) {
            out.println("500 " + state.get(SERVER_NAME)
                        + " Syntax error, command unrecognized: " + command);
        }
    }

}
