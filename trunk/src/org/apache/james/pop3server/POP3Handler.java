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
import org.apache.avalon.blocks.*;
import org.apache.james.*;
import org.apache.java.util.*;
import org.apache.arch.*;

/**
 * @author Federico Barbieri <scoobie@systemy.it>
 * @version 0.9
 */
public class POP3Handler implements Composer, Stoppable, Configurable, Service {

    private SimpleComponentManager comp;
    private Configuration conf;
    private Logger logger;
    private Store store;
    private Store.ObjectRepository userRepository;
    private Store.MessageContainerRepository userInbox;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String remoteHost;
    private String remoteIP;
    private String servername;
    private String postmaster;
    private String softwaretype;
    private int state;
    private String user;
    private Vector userMailbox;
    private Vector backupUserMailbox;
    private static final MessageContainer DELETED = new MessageContainer();
    
    private static int AUTHENTICATION_READY = 0;
    private static int AUTHENTICATION_USERSET = 1;
    private static int TRANSACTION = 2;
    private static int QUIT = 10;
        
    /**
     * Constructor has no parameters to allow Class.forName() stuff.
     */
    public POP3Handler() {
    }
    
    public void setConfiguration(Configuration conf) {

        this.conf = conf;
    }
    
    public void setComponentManager(ComponentManager comp) {

        this.comp = (SimpleComponentManager) comp;
    }

    public void init() 
    throws Exception {
        
        this.logger = (Logger) comp.getComponent(Interfaces.LOGGER);
        this.store = (Store) comp.getComponent(Interfaces.STORE);
        this.userRepository = (Store.ObjectRepository) comp.getComponent("mailUsers");
        this.servername = conf.getConfiguration("servername", "localhost").getValue();
        this.postmaster = conf.getConfiguration("postmaster", "postmaster@" + this.servername).getValue();
        this.softwaretype = "Apache James POP3 v @@version@@";
        this.userMailbox = new Vector();
    }
    
    public void parseRequest(Socket socket) {

        try {
            this.socket = socket;
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            remoteHost = socket.getInetAddress ().getHostName ();
            remoteIP = socket.getInetAddress ().getHostAddress ();
        } catch (Exception e) {
            logger.log("Cannot open connection from " + remoteHost + " (" + remoteIP + "): " + e.getMessage(), "POP3Server", logger.ERROR);
        }

        logger.log("Connection from " + remoteHost + " (" + remoteIP + ")", "POP3Server", logger.INFO);
    }
    
    public void run() {
    	
        try {
            state = AUTHENTICATION_READY;
            user = "unknown";
            out.println("+OK" + this.servername + " POP3 server (" + this.softwaretype + ") ready ");
            while (parseCommand(in.readLine()));
            socket.close();
            logger.log("Connection closed", "POP3Server", logger.INFO);

        } catch (IOException e) {
            out.println("-ERR. Error closing connection.");
            out.flush();
            logger.log("Exception during connection from " + remoteHost + " (" + remoteIP + ")", "POP3Server", logger.ERROR);
        }
    }
    
    private boolean parseCommand(String command) {
        logger.log("Command recieved: " + command, "POP3Server", logger.INFO);
        command = command.trim().toUpperCase();
        String argument = (String) null;
        if (command.length() > 5) {
            argument = command.substring(5).trim();
        }
        if (command.startsWith("USER")) {
            if (state == AUTHENTICATION_READY && argument != null) {
                user = argument;
                state = AUTHENTICATION_USERSET;
                out.println("+OK.");
            } else {
                out.println("-ERR.");
            }
            return true;
        } else if (command.startsWith("PASS")) {
            if (state == AUTHENTICATION_USERSET && argument != null) {
                if (userRepository.test(user, argument)) {
                    state = TRANSACTION;
                    out.println("+OK. Welcome " + user);
                    userInbox = getUserMailbox(user);
                    stat();
                } else {
                    state = AUTHENTICATION_READY;
                    out.println("-ERR. Authentication failed.");
                }
            } else {
                out.println("-ERR.");
            }
            return true;
        } else if (command.startsWith("STAT")) {
            if (state == TRANSACTION) {
                long size = 0;
                int count = 0;
                try {
                    for (Enumeration e = userMailbox.elements(); e.hasMoreElements(); ) {
                        MessageContainer mc = (MessageContainer) e.nextElement();
                        if (mc != DELETED) {
                            size += mc.getBodyInputStream().available();
                            count++;
                        }
                    }
                    out.println("+OK " + count + " " + size);
                } catch (IOException ioe) {
                    out.println("-ERR.");
                }
            } else {
                out.println("-ERR.");
            }
            return true;
        } else if (command.startsWith("LIST")) {
            if (state == TRANSACTION) {
                if (argument == null) {
                    long size = 0;
                    int count = 0;
                    try {
                        for (Enumeration e = userMailbox.elements(); e.hasMoreElements(); ) {
                            MessageContainer mc = (MessageContainer) e.nextElement();
                            if (mc != DELETED) {
                                size += mc.getBodyInputStream().available();
                                count++;
                            }
                        }
                        out.println("+OK " + count + " " + size);
                        count = 0;
                        for (Enumeration e = userMailbox.elements(); e.hasMoreElements(); count++) {
                            MessageContainer mc = (MessageContainer) e.nextElement();
                            if (mc != DELETED) {
                                out.println(count + " " + mc.getBodyInputStream().available());
                            }
                        }
                        out.println(".");                    
                    } catch (IOException ioe) {
                        out.println("-ERR.");
                    }
                } else {
                    int num = 0;
                    try {
                        num = Integer.parseInt(argument);
                        MessageContainer mc = (MessageContainer) userMailbox.elementAt(num);
                        if (mc != DELETED) {
                            out.println("+OK " + num + " " + mc.getBodyInputStream().available());
                        } else {
                            out.println("-ERR. Message (" + num + ") does not exist.");
                        }
                    } catch (ArrayIndexOutOfBoundsException npe) {
                        out.println("-ERR. Message (" + num + ") does not exist.");
                    } catch (NumberFormatException nfe) {
                        out.println("-ERR. " + argument + " is not a valid number");
                    } catch (IOException ioe) {
                        out.println("-ERR.");
                    }
                }
            } else {
                out.println("-ERR.");                
            }
            return true;
        } else if (command.startsWith("UIDL")) {
            if (state == TRANSACTION) {
                if (argument == null) {
                    out.println("+OK unique-id listing follows");
                    int count = 0;
                    for (Enumeration e = userMailbox.elements(); e.hasMoreElements(); count++) {
                        MessageContainer mc = (MessageContainer) e.nextElement();
                        if (mc != DELETED) {
                            out.println(count + " " + mc.getMessageId());
                        }
                    }
                    out.println(".");                    
                } else {
                    int num = 0;
                    try {
                        num = Integer.parseInt(argument);
                        MessageContainer mc = (MessageContainer) userMailbox.elementAt(num);
                        if (mc != DELETED) {
                            out.println("+OK " + num + " " + mc.getMessageId());
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
        } else if (command.startsWith("RSET")) {
            if (state == TRANSACTION) {
                stat();
                out.println("+OK.");
            } else {
                out.println("-ERR.");
            }
            return true;
        } else if (command.startsWith("DELE")) {
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
        } else if (command.startsWith("NOOP")) {
            out.println("+OK");
            return true;
        } else if (command.startsWith("RETR")) {
            if (state == TRANSACTION) {
                int num = 0;
                try {
                    num = Integer.parseInt(command.substring(4).trim());
                } catch (Exception e) {
                    out.println("-ERR. Usage: RETR [mail number]");
                    return true;
                }
                try {
//?May be written as return parseCommand("TOP " + num + " " + Integer.MAX_VALUE);?
                    MessageContainer mc = (MessageContainer) userMailbox.elementAt(num);
                    if (mc != DELETED) {
                        FilterInputStream fis = mc.getBodyInputStream();
                        fis.mark(0);
                        BufferedReader mailIn = new BufferedReader(new InputStreamReader(fis));
                        out.println("+OK Message follows");
                        for (String nextLine = mailIn.readLine(); nextLine != null; nextLine = mailIn.readLine()) {
                            out.println(nextLine);
                        }
                        fis.reset();
                    } else {
                        out.println("-ERR. Message (" + num + ") deleted.");
                    }
                } catch (IOException ioe) {
                    out.println("-ERR. Error while retrieving message.");
                } catch (ArrayIndexOutOfBoundsException iob) {
                    out.println("-ERR. Message (" + num + ") does not exist.");
                }
            } else {
                out.println("-ERR.");
            }
            return true;
        } else if (command.startsWith("TOP")) {
            if (state == TRANSACTION) {
                int num = 0;
                int lines = 0;
                try {
                    argument = command.substring(4);
                    int sep = argument.indexOf(' ');
                    num = Integer.parseInt(argument.substring(0, sep));
                    lines = Integer.parseInt(argument.substring(sep + 1));
                } catch (ArrayIndexOutOfBoundsException e) {
                    out.println("-ERR. Usage: TOP [mail number] [Line number]");
                    return true;
                } catch (NumberFormatException nfe) {
                    out.println("-ERR. Usage: TOP [mail number] [Line number]");
                    return true;
                }
                try {
                    MessageContainer mc = (MessageContainer) userMailbox.elementAt(num);
                    if (mc != DELETED) {
                        FilterInputStream fis = mc.getBodyInputStream();
                        fis.mark(0);
                        BufferedReader mailIn = new BufferedReader(new InputStreamReader(fis));
                        out.println("+OK Message follows");
                        for (String nextLine = mailIn.readLine(); nextLine != null && !nextLine.equals(""); nextLine = mailIn.readLine()) {
                            out.println(nextLine);
                        }
                        out.println("");
                        for (String nextLine = mailIn.readLine(); nextLine != null && lines-- > 0; nextLine = mailIn.readLine()) {
                            out.println(nextLine);
                        }
                        fis.reset();
                    } else {
                        out.println("-ERR. Message (" + num + ") already deleted.");
                    }
                } catch (IOException ioe) {
                    out.println("-ERR. Error while retrieving message.");
                } catch (ArrayIndexOutOfBoundsException iob) {
                    out.println("-ERR. Message (" + num + ") does not exist.");
                }
            } else {
                out.println("-ERR.");
            }
            return true;
        } else if (command.startsWith("QUIT")) {
logger.log("1", "POP3Server", logger.ERROR);
            Vector toBeRemoved = new Vector();
            for (Enumeration e = backupUserMailbox.elements(); e.hasMoreElements(); ) {
                Object mail = e.nextElement();
                if (!userMailbox.contains(mail)) {
                    toBeRemoved.addElement(mail);
                }
            }
logger.log("2", "POP3Server", logger.ERROR);
            try {
                for (Enumeration e = toBeRemoved.elements(); e.hasMoreElements(); ) {
                    MessageContainer mc = (MessageContainer) e.nextElement();
                    mc.getBodyInputStream().close();
                    userInbox.remove(mc.getMessageId());
                }
logger.log("3", "POP3Server", logger.ERROR);
                out.println("+OK. Apache James POP3 Server signinig off.");
logger.log("4", "POP3Server", logger.ERROR);
            } catch (Exception ex) {
                ex.printStackTrace();
logger.log("5", "POP3Server", logger.ERROR);
                out.println("-ERR. Some deleted messages were not removed.");
logger.log("6", "POP3Server", logger.ERROR);
            }
logger.log("7", "POP3Server", logger.ERROR);
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
       
    private Store.MessageContainerRepository getUserMailbox(String userName) {

        Store.MessageContainerRepository userInbox = (Store.MessageContainerRepository) null;
        String repositoryName = "localInbox." + userName;
        try {
            userInbox = (Store.MessageContainerRepository) comp.getComponent(repositoryName);
        } catch (ComponentNotFoundException ex) {
            userInbox = (Store.MessageContainerRepository) store.getPublicRepository(repositoryName);
            comp.put(repositoryName, userInbox);
        }
        return userInbox;
    }
    
    private void stat() {
        userMailbox = new Vector();
        userMailbox.addElement(DELETED);
        for (Enumeration e = userInbox.list(); e.hasMoreElements(); ) {
            String key = (String) e.nextElement();
            MessageContainer mc = userInbox.retrieve(key);
            userMailbox.addElement(mc);
        }
        backupUserMailbox = (Vector) userMailbox.clone();
    }
}
