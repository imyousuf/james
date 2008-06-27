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

import javax.mail.MessagingException;
import javax.mail.internet.*;

import org.apache.avalon.*;
import org.apache.avalon.services.*;
import org.apache.avalon.util.*;

import org.apache.james.*;
import org.apache.james.core.*;
import org.apache.james.services.*;
import org.apache.james.util.InternetPrintWriter;

import org.apache.log.LogKit;
import org.apache.log.Logger;

import org.apache.mailet.*;

/**
 * @author Federico Barbieri <scoobie@systemy.it>
 * @version 0.9
 */
public class POP3Handler implements Composer, Stoppable, Configurable, Service, Scheduler.Target, Contextualizable, Runnable {

    private ComponentManager compMgr;
    private Configuration conf;
    private Context context;
    private Logger logger =  LogKit.getLoggerFor("james.POP3Server");
    private MailServer mailServer;
    private MailRepository userInbox;
    private UsersRepository users;
    private Scheduler scheduler;
    private long timeout;

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
    public void configure(Configuration conf) throws ConfigurationException {
        this.conf = conf;
	timeout = conf.getChild("connectiontimeout").getValueAsLong(120000);
    }

    public void  contextualize(Context context) {
        this.context = context;
    }

    public void compose(ComponentManager comp) {
        compMgr = comp;
    }

    public void init() throws Exception {

	try {
	    mailServer = (MailServer) compMgr.lookup("org.apache.james.services.MailServer");
	    users = (UsersRepository) compMgr.lookup("org.apache.james.services.UsersRepository");
	    scheduler = (Scheduler) compMgr.lookup("org.apache.avalon.services.Scheduler");
	    softwaretype = "JAMES POP3 Server " + Constants.SOFTWARE_VERSION;
	    servername = (String) context.get(Constants.HELO_NAME);
	    userMailbox = new Vector();

	} catch (Exception e) {
	    logger.error("Exception initializing a PO3Handler was : " + e);
	    e.printStackTrace();
	    throw e;
	}
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
            logger.error("Cannot open connection from " + remoteHost + " (" + remoteIP + "): " + e.getMessage());
        }

        logger.info("Connection from " + remoteHost + " (" + remoteIP + ")");
    }

    public void run() {

        try {
	    scheduler.setAlarm(this.toString(), new Scheduler.Alarm(timeout), this);
            state = AUTHENTICATION_READY;
            user = "unknown";
            out.println(OK_RESPONSE + " " + this.servername + " POP3 server (" + this.softwaretype + ") ready ");
            while (parseCommand(in.readLine())) {
                scheduler.resetAlarm(this.toString());
            }
            socket.close();
            scheduler.removeAlarm(this.toString());
            logger.info("Connection closed");

        } catch (Exception e) {
            out.println(ERR_RESPONSE + " Error closing connection.");
            out.flush();
            logger.error("Exception during connection from " + remoteHost + " (" + remoteIP + ") : " + e.getMessage());
            try {
                socket.close();
            } catch (IOException ioe) {
            }
        }
    }

    public void wake(String name, Scheduler.Event event) {
        logger.error("Connection timeout on socket");
        try {
            out.println("Connection timeout. Closing connection");
            socket.close();
        } catch (IOException e) {
        }
    }

    private boolean parseCommand(String commandRaw) {
        if (commandRaw == null) return false;
        logger.info("Command received: " + commandRaw);
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
            List toBeRemoved =  ListUtils.subtract(backupUserMailbox, userMailbox);
            try {
                for (Iterator it = toBeRemoved.iterator(); it.hasNext(); ) {
                    MailImpl mc = (MailImpl) it.next();
                    userInbox.remove(mc.getName());
                }
                out.println(OK_RESPONSE + " Apache James POP3 Server signing off.");
            } catch (Exception ex) {
                out.println(ERR_RESPONSE + " Some deleted messages were not removed");
                logger.error("Some deleted messages were not removed: " + ex.getMessage());
            }
            return false;
        } else {
            out.println(ERR_RESPONSE);
            return true;
        }
    }

    public void stop() {
            // todo
        logger.error("Stop POP3Handler");
    }

    public void destroy() {

        logger.error("Destroy POP3Handler");
    }

    private void stat() {
        userMailbox = new Vector();
        userMailbox.addElement(DELETED);
        for (Iterator it = userInbox.list(); it.hasNext(); ) {
            String key = (String) it.next();
            MailImpl mc = userInbox.retrieve(key);
            userMailbox.addElement(mc);
        }
        backupUserMailbox = (Vector) userMailbox.clone();
    }
}
