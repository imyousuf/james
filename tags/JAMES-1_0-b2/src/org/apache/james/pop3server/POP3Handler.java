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

import org.apache.arch.*;
import org.apache.mail.Mail;
import org.apache.avalon.blocks.*;
import org.apache.java.util.*;
import org.apache.james.*;
import org.apache.james.usermanager.*;

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
    private UserManager userManager;
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
    private static final Mail DELETED = new Mail();
    
    private static int AUTHENTICATION_READY = 0;
    private static int AUTHENTICATION_USERSET = 1;
    private static int TRANSACTION = 2;
        
    /**
     * Constructor has no parameters to allow Class.forName() stuff.
     */
    public POP3Handler() {
    }
    
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
        
        this.logger = (Logger) comp.getComponent(Interfaces.LOGGER);
        this.mailServer = (MailServer) comp.getComponent(Interfaces.MAIL_SERVER);
        this.userManager = (UserManager) comp.getComponent(Constants.USERS_MANAGER);
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
            out = new PrintWriter(outs, true);
            remoteHost = socket.getInetAddress ().getHostName ();
            remoteIP = socket.getInetAddress ().getHostAddress ();
        } catch (Exception e) {
            logger.log("Cannot open connection from " + remoteHost + " (" + remoteIP + "): " + e.getMessage(), "POP3Server", logger.ERROR);
        }

        logger.log("Connection from " + remoteHost + " (" + remoteIP + ")", "POP3Server", logger.INFO);
    }
    
    public void run() {
    	
        try {
            timeServer.setAlarm(this.toString(), this, conf.getConfiguration("connectiontimeout", "120000").getValueAsLong());
            state = AUTHENTICATION_READY;
            user = "unknown";
            out.println("+OK " + this.servername + " POP3 server (" + this.softwaretype + ") ready ");
            while (parseCommand(in.readLine())) {
                timeServer.resetAlarm(this.toString());
            }
            socket.close();
            timeServer.removeAlarm(this.toString());
            logger.log("Connection closed", "POP3Server", logger.INFO);

        } catch (Exception e) {
            out.println("-ERR. Error closing connection.");
            out.flush();
            logger.log("Exception during connection from " + remoteHost + " (" + remoteIP + ")", "POP3Server", logger.ERROR);
        }
    }
    
    public void wake(String name, String memo) {
        logger.log("Connection timeout on socket", "POP3Server", logger.ERROR);
        try {
            out.println("Connection timeout. Closing connection");
            socket.close();
        } catch (IOException e) {
        }
    }

    private boolean parseCommand(String command) {
        if (command == null) return false;
        logger.log("Command recieved: " + command, "POP3Server", logger.INFO);
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
        if (command.equalsIgnoreCase("USER")) {
            if (state == AUTHENTICATION_READY && argument != null) {
                user = argument;
                state = AUTHENTICATION_USERSET;
                out.println("+OK.");
            } else {
                out.println("-ERR.");
            }
            return true;
        } else if (command.equalsIgnoreCase("PASS")) {
            if (state == AUTHENTICATION_USERSET && argument != null) {
                if (userManager.test(user, argument)) {
                    state = TRANSACTION;
                    out.println("+OK. Welcome " + user);
                    userInbox = mailServer.getUserInbox(user);
                    stat();
                } else {
                    state = AUTHENTICATION_READY;
                    out.println("-ERR. Authentication failed.");
                }
            } else {
                out.println("-ERR.");
            }
            return true;
        } else if (command.equalsIgnoreCase("STAT")) {
            if (state == TRANSACTION) {
                long size = 0;
                int count = 0;
                try {
                    for (Enumeration e = userMailbox.elements(); e.hasMoreElements(); ) {
                        Mail mc = (Mail) e.nextElement();
                        if (mc != DELETED) {
                            size += mc.getMessage().getSize();
                            count++;
                        }
                    }
                    out.println("+OK " + count + " " + size);
                } catch (MessagingException me) {
                    out.println("-ERR.");
                }
            } else {
                out.println("-ERR.");
            }
            return true;
        } else if (command.equalsIgnoreCase("LIST")) {
            if (state == TRANSACTION) {
                if (argument == null) {
                    long size = 0;
                    int count = 0;
                    try {
                        for (Enumeration e = userMailbox.elements(); e.hasMoreElements(); ) {
                            Mail mc = (Mail) e.nextElement();
                            if (mc != DELETED) {
                                size += mc.getMessage().getSize();
                                count++;
                            }
                        }
                        out.println("+OK " + count + " " + size);
                        count = 0;
                        for (Enumeration e = userMailbox.elements(); e.hasMoreElements(); count++) {
                            Mail mc = (Mail) e.nextElement();
                            if (mc != DELETED) {
                                out.println(count + " " + mc.getMessage().getSize());
                            }
                        }
                        out.println(".");                    
                    } catch (MessagingException me) {
                        out.println("-ERR.");
                    }
                } else {
                    int num = 0;
                    try {
                        num = Integer.parseInt(argument);
                        Mail mc = (Mail) userMailbox.elementAt(num);
                        if (mc != DELETED) {
                            out.println("+OK " + num + " " + mc.getMessage().getSize());
                        } else {
                            out.println("-ERR. Message (" + num + ") does not exist.");
                        }
                    } catch (ArrayIndexOutOfBoundsException npe) {
                        out.println("-ERR. Message (" + num + ") does not exist.");
                    } catch (NumberFormatException nfe) {
                        out.println("-ERR. " + argument + " is not a valid number");
                    } catch (MessagingException me) {
                        out.println("-ERR.");
                    }
                }
            } else {
                out.println("-ERR.");                
            }
            return true;
        } else if (command.equalsIgnoreCase("UIDL")) {
            if (state == TRANSACTION) {
                if (argument == null) {
                    out.println("+OK unique-id listing follows");
                    int count = 0;
                    for (Enumeration e = userMailbox.elements(); e.hasMoreElements(); count++) {
                        Mail mc = (Mail) e.nextElement();
                        if (mc != DELETED) {
                            out.println(count + " " + mc.getName());
                        }
                    }
                    out.println(".");                    
                } else {
                    int num = 0;
                    try {
                        num = Integer.parseInt(argument);
                        Mail mc = (Mail) userMailbox.elementAt(num);
                        if (mc != DELETED) {
                            out.println("+OK " + num + " " + mc.getName());
                        } else {
                            out.println("-ERR. Message (" + num + ") does not exist.");
                        }
                    } catch (ArrayIndexOutOfBoundsException npe) {
                        out.println("-ERR. Message (" + num + ") does not exist.");
                    } catch (NumberFormatException nfe) {
                        out.println("-ERR. " + argument + " is not a valid number");
                    }
                }
            } else {
                out.println("-ERR.");                
            }
            return true;            
        } else if (command.equalsIgnoreCase("RSET")) {
            if (state == TRANSACTION) {
                stat();
                out.println("+OK.");
            } else {
                out.println("-ERR.");
            }
            return true;
        } else if (command.equalsIgnoreCase("DELE")) {
            if (state == TRANSACTION) {
                int num = 0;
                try {
                    num = Integer.parseInt(argument);
                } catch (Exception e) {
                    out.println("-ERR. Usage: DELE [mail number]");
                    return true;
                }
                try {
                    userMailbox.setElementAt(DELETED, num);
                    out.println("+OK Message removed");
                } catch (ArrayIndexOutOfBoundsException iob) {
                    out.println("-ERR. Message (" + num + ") does not exist.");
                }
            } else {
                out.println("-ERR.");
            }
            return true;
        } else if (command.equalsIgnoreCase("NOOP")) {
            out.println("+OK");
            return true;
        } else if (command.equalsIgnoreCase("RETR")) {
            if (state == TRANSACTION) {
                int num = 0;
                try {
                    num = Integer.parseInt(argument.trim());
                } catch (Exception e) {
                    out.println("-ERR. Usage: RETR [mail number]");
                    return true;
                }
//?May be written as return parseCommand("TOP " + num + " " + Integer.MAX_VALUE);?
                try {
                    Mail mc = (Mail) userMailbox.elementAt(num);
                    if (mc != DELETED) {
                        out.println("+OK Message follows");
                        mc.writeMessageTo(outs);
                    } else {
                        out.println("-ERR. Message (" + num + ") deleted.");
                    }
                } catch (IOException ioe) {
                    out.println("-ERR. Error while retrieving message.");
                } catch (MessagingException me) {
                    out.println("-ERR. Error while retrieving message.");
                } catch (ArrayIndexOutOfBoundsException iob) {
                    out.println("-ERR. Message (" + num + ") does not exist.");
                }
// -------------------------------------------?
            } else {
                out.println("-ERR.");
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
                    out.println("-ERR. Usage: TOP [mail number] [Line number]");
                    return true;
                }
                try {
                    Mail mc = (Mail) userMailbox.elementAt(num);
                    if (mc != DELETED) {
                        out.println("+OK Message follows");
                        for (Enumeration e = mc.getMessage().getAllHeaderLines(); e.hasMoreElements(); ) {
                            out.println(e.nextElement());
                        }
                        out.println("");
// FIXME!!!: need to print first "lines" of the message
                        while (lines-- > 0) {
                            out.println("!!! PARTYALLY SUPPORTED COMMAND !!!");
                        }
                    } else {
                        out.println("-ERR. Message (" + num + ") already deleted.");
                    }
                } catch (MessagingException me) {
                    out.println("-ERR. Error while retrieving message.");
                } catch (ArrayIndexOutOfBoundsException iob) {
                    out.println("-ERR. Message (" + num + ") does not exist.");
                }
            } else {
                out.println("-ERR.");
            }
            return true;
        } else if (command.equalsIgnoreCase("QUIT")) {
            if (state == AUTHENTICATION_READY ||  state == AUTHENTICATION_USERSET) {
                return false;
            }
            Vector toBeRemoved = VectorUtils.subtract(backupUserMailbox, userMailbox);
            try {
                for (Enumeration e = toBeRemoved.elements(); e.hasMoreElements(); ) {
                    Mail mc = (Mail) e.nextElement();
                    userInbox.remove(mc.getName());
                }
                out.println("+OK. Apache James POP3 Server signinig off.");
            } catch (Exception ex) {
                out.println("-ERR. Some deleted messages were not removed");
                logger.log("Some deleted messages were not removed: " + ex.getMessage(), "POP3Server", logger.ERROR);
            }
            return false;
        } else {
            out.println("-ERR.");
            return true;
        }
    }
    
    public void stop() {
            // todo
        logger.log("Stop SMTPHandler", "POP3Server", logger.ERROR);
    }

    public void destroy() {

        logger.log("Destroy SMTPHandler", "POP3Server", logger.ERROR);
    }
       
    private void stat() {
        userMailbox = new Vector();
        userMailbox.addElement(DELETED);
        for (Enumeration e = userInbox.list(); e.hasMoreElements(); ) {
            String key = (String) e.nextElement();
            Mail mc = userInbox.retrieve(key);
            userMailbox.addElement(mc);
        }
        backupUserMailbox = (Vector) userMailbox.clone();
    }
}
