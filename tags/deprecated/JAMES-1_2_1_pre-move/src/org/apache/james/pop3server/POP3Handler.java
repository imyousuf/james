/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.pop3server;

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;

import org.apache.avalon.*;
import org.apache.mailet.*;
import org.apache.avalon.blocks.*;
import org.apache.avalon.utils.*;
import org.apache.james.*;
import org.apache.james.core.*;
import org.apache.james.mailrepository.*;
import org.apache.james.transport.*;
import org.apache.james.userrepository.*;
import org.apache.james.util.*;

import javax.mail.MessagingException;
import javax.mail.internet.*;

/**
 * @author Federico Barbieri <scoobie@systemy.it>
 * @version 0.9
 */
public class POP3Handler implements Composer, Stoppable, Configurable, Service, TimeServer.Bell, Contextualizable {

    private ComponentManager comp;
    private Configuration conf;
    private Context context;
    private Logger logger;
    private MailServer mailServer;
    private MailRepository userInbox;
    private UsersRepository users;
    private TimeServer timeServer;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private OutputStream outs;
    private String remoteHost;
    private String remoteIP;
    private String servername;
    private String softwaretype;
    private int state;
    private String user;
    private Vector userMailbox;
    private Vector backupUserMailbox;
    private static final Mail DELETED = new MailImpl();

    private static int AUTHENTICATION_READY = 0;
    private static int AUTHENTICATION_USERSET = 1;
    private static int TRANSACTION = 2;

    private final static String OK_RESPONSE = "+OK";
    private final static String ERR_RESPONSE = "-ERR";

    /**
     * Constructor has no parameters to allow Class.forName() stuff.
     */
    public void setConfiguration(Configuration conf) {
        this.conf = conf;
    }

    public void setComponentManager(ComponentManager comp) {
        this.comp = comp;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public void init()
    throws Exception {

        logger = (Logger) comp.getComponent(Interfaces.LOGGER);
        this.mailServer = (MailServer) comp.getComponent(Interfaces.MAIL_SERVER);
        users = (UsersRepository) comp.getComponent(Constants.LOCAL_USERS);
        this.timeServer = (TimeServer) comp.getComponent(Interfaces.TIME_SERVER);
        this.softwaretype = "JAMES POP3 Server " + Constants.SOFTWARE_VERSION;
        this.servername = (String) context.get(Constants.HELO_NAME);
        this.userMailbox = new Vector();
    }

    public void parseRequest(Socket socket) {

        try {
            this.socket = socket;
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            outs = socket.getOutputStream();
            out = new InternetPrintWriter(outs, true);
            remoteHost = socket.getInetAddress ().getHostName ();
            remoteIP = socket.getInetAddress ().getHostAddress ();
        } catch (Exception e) {
            logger.log("Cannot open connection from " + remoteHost + " (" + remoteIP + "): " + e.getMessage(), "POP3", logger.ERROR);
        }

        logger.log("Connection from " + remoteHost + " (" + remoteIP + ")", "POP3", logger.INFO);
    }

    public void run() {

        try {
            timeServer.setAlarm(this.toString(), this, conf.getConfiguration("connectiontimeout").getValueAsLong(120000));
            state = AUTHENTICATION_READY;
            user = "unknown";
            out.println(OK_RESPONSE + " " + this.servername + " POP3 server (" + this.softwaretype + ") ready ");
            while (parseCommand(in.readLine())) {
                timeServer.resetAlarm(this.toString());
            }
            socket.close();
            timeServer.removeAlarm(this.toString());
            logger.log("Connection closed", "POP3", logger.INFO);

        } catch (Exception e) {
            out.println(ERR_RESPONSE + " Error closing connection.");
            out.flush();
            logger.log("Exception during connection from " + remoteHost + " (" + remoteIP + ") : " + e.getMessage(), "POP3", logger.ERROR);
            try {
                socket.close();
            } catch (IOException ioe) {
            }
        }
    }

    public void wake(String name, String memo) {
        logger.log("Connection timeout on socket", "POP3", logger.ERROR);
        try {
            out.println("Connection timeout. Closing connection");
            socket.close();
        } catch (IOException e) {
        }
    }

    private boolean parseCommand(String commandRaw) {
        if (commandRaw == null) return false;
        logger.log("Command received: " + commandRaw, "POP3", logger.INFO);
        String command = commandRaw.trim();
        StringTokenizer commandLine = new StringTokenizer(command, " ");
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
        if (command.equalsIgnoreCase("USER")) {
            if (state == AUTHENTICATION_READY && argument != null) {
                user = argument;
                state = AUTHENTICATION_USERSET;
                out.println(OK_RESPONSE);
            } else {
                out.println(ERR_RESPONSE);
            }
            return true;
        } else if (command.equalsIgnoreCase("PASS")) {
            if (state == AUTHENTICATION_USERSET && argument != null) {
                String passArg = commandRaw.substring(5);
                if (users.test(user, passArg)) {
                    state = TRANSACTION;
                    out.println(OK_RESPONSE + " Welcome " + user);
                    userInbox = mailServer.getUserInbox(user);
                    stat();
                } else {
                    state = AUTHENTICATION_READY;
                    out.println(ERR_RESPONSE + " Authentication failed.");
                }
            } else {
                out.println(ERR_RESPONSE);
            }
            return true;
        } else if (command.equalsIgnoreCase("STAT")) {
            if (state == TRANSACTION) {
                long size = 0;
                int count = 0;
                try {
                    for (Enumeration e = userMailbox.elements(); e.hasMoreElements(); ) {
                        MailImpl mc = (MailImpl) e.nextElement();
                        if (mc != DELETED) {
                            size += mc.getSize();
                            count++;
                        }
                    }
                    out.println(OK_RESPONSE + " " + count + " " + size);
                } catch (MessagingException me) {
                    out.println(ERR_RESPONSE);
                }
            } else {
                out.println(ERR_RESPONSE);
            }
            return true;
        } else if (command.equalsIgnoreCase("LIST")) {
            if (state == TRANSACTION) {
                if (argument == null) {
                    long size = 0;
                    int count = 0;
                    try {
                        for (Enumeration e = userMailbox.elements(); e.hasMoreElements(); ) {
                            MailImpl mc = (MailImpl) e.nextElement();
                            if (mc != DELETED) {
                                size += mc.getSize();
                                count++;
                            }
                        }
                        out.println(OK_RESPONSE + " " + count + " " + size);
                        count = 0;
                        for (Enumeration e = userMailbox.elements(); e.hasMoreElements(); count++) {
                            MailImpl mc = (MailImpl) e.nextElement();
                            if (mc != DELETED) {
                                out.println(count + " " + mc.getSize());
                            }
                        }
                        out.println(".");
                    } catch (MessagingException me) {
                        out.println(ERR_RESPONSE);
                    }
                } else {
                    int num = 0;
                    try {
                        num = Integer.parseInt(argument);
                        MailImpl mc = (MailImpl) userMailbox.elementAt(num);
                        if (mc != DELETED) {
                            out.println(OK_RESPONSE + " " + num + " " + mc.getSize());
                        } else {
                            out.println(ERR_RESPONSE + " Message (" + num + ") does not exist.");
                        }
                    } catch (ArrayIndexOutOfBoundsException npe) {
                        out.println(ERR_RESPONSE + " Message (" + num + ") does not exist.");
                    } catch (NumberFormatException nfe) {
                        out.println(ERR_RESPONSE + " " + argument + " is not a valid number");
                    } catch (MessagingException me) {
                        out.println(ERR_RESPONSE);
                    }
                }
            } else {
                out.println(ERR_RESPONSE);
            }
            return true;
        } else if (command.equalsIgnoreCase("UIDL")) {
            if (state == TRANSACTION) {
                if (argument == null) {
                    out.println(OK_RESPONSE + " unique-id listing follows");
                    int count = 0;
                    for (Enumeration e = userMailbox.elements(); e.hasMoreElements(); count++) {
                        MailImpl mc = (MailImpl) e.nextElement();
                        if (mc != DELETED) {
                            out.println(count + " " + mc.getName());
                        }
                    }
                    out.println(".");
                } else {
                    int num = 0;
                    try {
                        num = Integer.parseInt(argument);
                        MailImpl mc = (MailImpl) userMailbox.elementAt(num);
                        if (mc != DELETED) {
                            out.println(OK_RESPONSE + " " + num + " " + mc.getName());
                        } else {
                            out.println(ERR_RESPONSE + " Message (" + num + ") does not exist.");
                        }
                    } catch (ArrayIndexOutOfBoundsException npe) {
                        out.println(ERR_RESPONSE + " Message (" + num + ") does not exist.");
                    } catch (NumberFormatException nfe) {
                        out.println(ERR_RESPONSE + " " + argument + " is not a valid number");
                    }
                }
            } else {
                out.println(ERR_RESPONSE);
            }
            return true;
        } else if (command.equalsIgnoreCase("RSET")) {
            if (state == TRANSACTION) {
                stat();
                out.println(OK_RESPONSE);
            } else {
                out.println(ERR_RESPONSE);
            }
            return true;
        } else if (command.equalsIgnoreCase("DELE")) {
            if (state == TRANSACTION) {
                int num = 0;
                try {
                    num = Integer.parseInt(argument);
                } catch (Exception e) {
                    out.println(ERR_RESPONSE + " Usage: DELE [mail number]");
                    return true;
                }
                try {
                    userMailbox.setElementAt(DELETED, num);
                    out.println(OK_RESPONSE + " Message removed");
                } catch (ArrayIndexOutOfBoundsException iob) {
                    out.println(ERR_RESPONSE + " Message (" + num + ") does not exist.");
                }
            } else {
                out.println(ERR_RESPONSE);
            }
            return true;
        } else if (command.equalsIgnoreCase("NOOP")) {
            if (state == TRANSACTION) {
                out.println(OK_RESPONSE);
            } else {
                out.println(ERR_RESPONSE);
            }
            return true;
        } else if (command.equalsIgnoreCase("RETR")) {
            if (state == TRANSACTION) {
                int num = 0;
                try {
                    num = Integer.parseInt(argument.trim());
                } catch (Exception e) {
                    out.println(ERR_RESPONSE + " Usage: RETR [mail number]");
                    return true;
                }
//?May be written as return parseCommand("TOP " + num + " " + Integer.MAX_VALUE);?
                try {
                    MailImpl mc = (MailImpl) userMailbox.elementAt(num);
                    if (mc != DELETED) {
                        out.println(OK_RESPONSE + " Message follows");
                        mc.writeMessageTo(outs);
                        out.println();
                        out.println(".");
                    } else {
                        out.println(ERR_RESPONSE + " Message (" + num + ") deleted.");
                    }
                } catch (IOException ioe) {
                    out.println(ERR_RESPONSE + " Error while retrieving message.");
                } catch (MessagingException me) {
                    out.println(ERR_RESPONSE + " Error while retrieving message.");
                } catch (ArrayIndexOutOfBoundsException iob) {
                    out.println(ERR_RESPONSE + " Message (" + num + ") does not exist.");
                }
// -------------------------------------------?
            } else {
                out.println(ERR_RESPONSE);
            }
            return true;
        } else if (command.equalsIgnoreCase("TOP")) {
            if (state == TRANSACTION) {
                int num = 0;
                int lines = 0;
                try {
                    num = Integer.parseInt(argument);
                    lines = Integer.parseInt(argument1);
                } catch (NumberFormatException nfe) {
                    out.println(ERR_RESPONSE + " Usage: TOP [mail number] [Line number]");
                    return true;
                }
                try {
                    MailImpl mc = (MailImpl) userMailbox.elementAt(num);
                    if (mc != DELETED) {
                        out.println(OK_RESPONSE + " Message follows");
                        for (Enumeration e = mc.getMessage().getAllHeaderLines(); e.hasMoreElements(); ) {
                            out.println(e.nextElement());
                        }
                        out.println("");
                        mc.writeContentTo(outs, lines);
                        out.println(".");
                    } else {
                        out.println(ERR_RESPONSE + " Message (" + num + ") already deleted.");
                    }
                } catch (IOException ioe) {
                    out.println(ERR_RESPONSE + " Error while retrieving message.");
                } catch (MessagingException me) {
                    out.println(ERR_RESPONSE + " Error while retrieving message.");
                } catch (ArrayIndexOutOfBoundsException iob) {
                    out.println(ERR_RESPONSE + " Message (" + num + ") does not exist.");
                }
            } else {
                out.println(ERR_RESPONSE);
            }
            return true;
        } else if (command.equalsIgnoreCase("QUIT")) {
            if (state == AUTHENTICATION_READY ||  state == AUTHENTICATION_USERSET) {
                return false;
            }
            Vector toBeRemoved = VectorUtils.subtract(backupUserMailbox, userMailbox);
            try {
                for (Enumeration e = toBeRemoved.elements(); e.hasMoreElements(); ) {
                    MailImpl mc = (MailImpl) e.nextElement();
                    userInbox.remove(mc.getName());
                }
                out.println(OK_RESPONSE + " Apache James POP3 Server signing off.");
            } catch (Exception ex) {
                out.println(ERR_RESPONSE + " Some deleted messages were not removed");
                logger.log("Some deleted messages were not removed: " + ex.getMessage(), "POP3", logger.ERROR);
            }
            return false;
        } else {
            out.println(ERR_RESPONSE);
            return true;
        }
    }

    public void stop() {
            // todo
        logger.log("Stop SMTPHandler", "POP3", logger.ERROR);
    }

    public void destroy() {

        logger.log("Destroy SMTPHandler", "POP3", logger.ERROR);
    }

    private void stat() {
        userMailbox = new Vector();
        userMailbox.addElement(DELETED);
        for (Enumeration e = userInbox.list(); e.hasMoreElements(); ) {
            String key = (String) e.nextElement();
            MailImpl mc = userInbox.retrieve(key);
            userMailbox.addElement(mc);
        }
        backupUserMailbox = (Vector) userMailbox.clone();
    }
}
