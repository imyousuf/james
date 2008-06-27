/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.pop3server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;
import javax.mail.MessagingException;
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
import org.apache.avalon.framework.logger.AbstractLoggable;
import org.apache.avalon.cornerstone.services.connection.ConnectionHandler;
import org.apache.avalon.cornerstone.services.scheduler.PeriodicTimeTrigger;
import org.apache.avalon.cornerstone.services.scheduler.Target;
import org.apache.avalon.cornerstone.services.scheduler.TimeScheduler;
import org.apache.avalon.excalibur.collections.ListUtils;
import org.apache.james.Constants;
import org.apache.james.core.MailImpl;
import org.apache.james.services.MailRepository;
import org.apache.james.services.MailServer;
import org.apache.james.services.UsersRepository;
import org.apache.james.services.UsersStore;
import org.apache.james.BaseConnectionHandler;
import org.apache.james.util.InternetPrintWriter;
import org.apache.james.util.SchedulerNotifyOutputStream;
import org.apache.mailet.Mail;

/**
 * @author Federico Barbieri <scoobie@systemy.it>
 * @version 0.9
 */
public class POP3Handler
    extends BaseConnectionHandler
    implements ConnectionHandler, Composable, Configurable, Target {

    private String softwaretype        = "JAMES POP3 Server " + Constants.SOFTWARE_VERSION;

    private ComponentManager compMgr;
    private MailServer mailServer;
    private MailRepository userInbox;
    private UsersRepository users;
    private TimeScheduler scheduler;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private OutputStream outs;
    private String remoteHost;
    private String remoteIP;
    private int state;
    private String user;
    private Vector userMailbox = new Vector();
    private Vector backupUserMailbox;
    private static final Mail DELETED = new MailImpl();

    private int lengthReset = 20000;

    private static int AUTHENTICATION_READY = 0;
    private static int AUTHENTICATION_USERSET = 1;
    private static int TRANSACTION = 2;

    private final static String OK_RESPONSE = "+OK";
    private final static String ERR_RESPONSE = "-ERR";

    public void configure(Configuration configuration)
            throws ConfigurationException {
        super.configure(configuration);

        lengthReset = configuration.getChild("lengthReset").getValueAsInteger(20000);
    }

    public void compose( final ComponentManager componentManager )
        throws ComponentException {
        mailServer = (MailServer)componentManager.
            lookup( "org.apache.james.services.MailServer" );
        UsersStore usersStore = (UsersStore)componentManager.
            lookup( "org.apache.james.services.UsersStore" );
        users = usersStore.getRepository("LocalUsers");
        scheduler = (TimeScheduler)componentManager.
            lookup( "org.apache.avalon.cornerstone.services.scheduler.TimeScheduler" );
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
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            outs = socket.getOutputStream();
            out = new InternetPrintWriter(outs, true);
            remoteHost = socket.getInetAddress ().getHostName ();
            remoteIP = socket.getInetAddress ().getHostAddress ();
        } catch (Exception e) {
            getLogger().error( "Cannot open connection from " + remoteHost +
                               " (" + remoteIP + "): " + e.getMessage(), e );
        }

        getLogger().info( "Connection from " + remoteHost + " (" + remoteIP + ")" );

        try {
            final PeriodicTimeTrigger trigger = new PeriodicTimeTrigger( timeout, -1 );
            scheduler.addTrigger( this.toString(), trigger, this );
            state = AUTHENTICATION_READY;
            user = "unknown";
            out.println( OK_RESPONSE + " " + this.helloName +
                         " POP3 server (" + this.softwaretype + ") ready " );
            while (parseCommand(in.readLine())) {
                scheduler.resetTrigger(this.toString());
            }
            socket.close();
            scheduler.removeTrigger(this.toString());
            getLogger().info("Connection closed");

        } catch (Exception e) {
            out.println(ERR_RESPONSE + " Error closing connection.");
            out.flush();
            getLogger().error( "Exception during connection from " + remoteHost +
                               " (" + remoteIP + ") : " + e.getMessage(), e );
            try {
                socket.close();
            } catch (IOException ioe) {
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

    private boolean parseCommand(String commandRaw) {
        if (commandRaw == null) return false;
        getLogger().info("Command received: " + commandRaw);
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

        if (command.equalsIgnoreCase("USER"))
            doUSER(command,argument,argument1);
        else if (command.equalsIgnoreCase("PASS"))
            doPASS(command,argument,argument1);
        else if (command.equalsIgnoreCase("STAT"))
            doSTAT(command,argument,argument1);
        else if (command.equalsIgnoreCase("LIST"))
            doLIST(command,argument,argument1);
        else if (command.equalsIgnoreCase("UIDL"))
            doUIDL(command,argument,argument1);
        else if (command.equalsIgnoreCase("RSET"))
            doRSET(command,argument,argument1);
        else if (command.equalsIgnoreCase("DELE"))
            doDELE(command,argument,argument1);
        else if (command.equalsIgnoreCase("NOOP"))
            doNOOP(command,argument,argument1);
        else if (command.equalsIgnoreCase("RETR"))
            doRETR(command,argument,argument1);
        else if (command.equalsIgnoreCase("TOP"))
            doTOP(command,argument,argument1);
        else if (command.equalsIgnoreCase("QUIT"))
            doQUIT(command,argument,argument1);
        else
            doUnknownCmd(command,argument,argument1);
        return (command.equalsIgnoreCase("QUIT") == false);
    }

    private void doUSER(String command,String argument,String argument1) {
        if (state == AUTHENTICATION_READY && argument != null) {
            user = argument;
            state = AUTHENTICATION_USERSET;
            out.println(OK_RESPONSE);
        } else {
            out.println(ERR_RESPONSE);
        }
    }

    private void doPASS(String command,String argument,String argument1) {
        if (state == AUTHENTICATION_USERSET && argument != null) {
            String passArg = argument;
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
    }

    private void doSTAT(String command,String argument,String argument1) {
        if (state == TRANSACTION) {
            long size = 0;
            int count = 0;
            try {
                for (Enumeration e = userMailbox.elements(); e.hasMoreElements(); ) {
                    MailImpl mc = (MailImpl) e.nextElement();
                    if (mc != DELETED) {
                        size += mc.getMessageSize();
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
    }
    private void doLIST(String command,String argument,String argument1) {
        if (state == TRANSACTION) {
            if (argument == null) {
                long size = 0;
                int count = 0;
                try {
                    for (Enumeration e = userMailbox.elements(); e.hasMoreElements(); ) {
                        MailImpl mc = (MailImpl) e.nextElement();
                        if (mc != DELETED) {
                            size += mc.getMessageSize();
                            count++;
                        }
                    }
                    out.println(OK_RESPONSE + " " + count + " " + size);
                    count = 0;
                    for (Enumeration e = userMailbox.elements(); e.hasMoreElements(); count++) {
                        MailImpl mc = (MailImpl) e.nextElement();
                        if (mc != DELETED) {
                            out.println(count + " " + mc.getMessageSize());
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
                        out.println(OK_RESPONSE + " " + num + " " + mc.getMessageSize());
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
    }

    private void doUIDL(String command,String argument,String argument1) {
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
    }
    private void doRSET(String command,String argument,String argument1) {
        if (state == TRANSACTION) {
            stat();
            out.println(OK_RESPONSE);
        } else {
            out.println(ERR_RESPONSE);
        }
    }

    private void doDELE(String command,String argument,String argument1) {
        if (state == TRANSACTION) {
            int num = 0;
            try {
                num = Integer.parseInt(argument);
            } catch (Exception e) {
                out.println(ERR_RESPONSE + " Usage: DELE [mail number]");
                return;
            }
            try {
                MailImpl mc = (MailImpl) userMailbox.elementAt(num);
                if (mc == DELETED) {
                    out.println(ERR_RESPONSE + " Message (" + num + ") does not exist.");
                } else {
                    userMailbox.setElementAt(DELETED, num);
                    out.println(OK_RESPONSE + " Message removed");
                }
            } catch (ArrayIndexOutOfBoundsException iob) {
                out.println(ERR_RESPONSE + " Message (" + num + ") does not exist.");
            }
        } else {
            out.println(ERR_RESPONSE);
        }
    }
    private void doNOOP(String command,String argument,String argument1) {
        if (state == TRANSACTION) {
            out.println(OK_RESPONSE);
        } else {
            out.println(ERR_RESPONSE);
        }
    }
    private void doRETR(String command,String argument,String argument1) {
        if (state == TRANSACTION) {
            int num = 0;
            try {
                num = Integer.parseInt(argument.trim());
            } catch (Exception e) {
                out.println(ERR_RESPONSE + " Usage: RETR [mail number]");
                return;
            }
            //?May be written as
            //return parseCommand("TOP " + num + " " + Integer.MAX_VALUE);?
            try {
                MailImpl mc = (MailImpl) userMailbox.elementAt(num);
                if (mc != DELETED) {
                    out.println(OK_RESPONSE + " Message follows");
                    SchedulerNotifyOutputStream nouts =
                            new SchedulerNotifyOutputStream(outs, scheduler,
                            this.toString(), lengthReset);
                    mc.writeMessageTo(nouts);
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
    }
    private void doTOP(String command,String argument,String argument1) {
        if (state == TRANSACTION) {
            int num = 0;
            int lines = 0;
            try {
                num = Integer.parseInt(argument);
                lines = Integer.parseInt(argument1);
            } catch (NumberFormatException nfe) {
                out.println(ERR_RESPONSE + " Usage: TOP [mail number] [Line number]");
                return;
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
    }
    private void doQUIT(String command,String argument,String argument1) {
        if (state == AUTHENTICATION_READY ||  state == AUTHENTICATION_USERSET) {
            return;
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
            getLogger().error("Some deleted messages were not removed: " + ex.getMessage());
        }
    }
    private void doUnknownCmd(String command,String argument,String argument1) {
        out.println(ERR_RESPONSE);
    }
}

