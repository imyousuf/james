/*--- formatted by Jindent 2.0b, (www.c-lab.de/~jindent) ---*/

package org.apache.james.smtpserver;

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
import java.lang.reflect.*;
import javax.mail.*;
import javax.mail.internet.*;
import javax.activation.*;
import org.apache.avalon.blocks.*;
import java.net.*;
import org.apache.james.*;
import org.apache.java.util.*;
import org.apache.java.lang.*;
import org.apache.java.recycle.*;
import org.apache.avalon.util.*;
import org.apache.james.smtpserver.command.*;
import org.apache.avalon.SimpleContext;

/**
 * This handles an individual incoming message.  It handles regular SMTP
 * commands, and when it receives a message, adds it to the spool.
 * @author Serge Knystautas <sergek@lokitech.com>
 * @author Federico Barbieri <scoobie@systemy.it>
 * @version 0.9
 */
public class SMTPHandler implements ProtocolHandler, Contextualizable, Stoppable, Recyclable {

    private Socket socket;
    private BufferedReader in;
    private InputStream socketIn;
    private PrintWriter out;
    private OutputStream r_out;
    private String remoteHost;
    private String remoteHostGiven;
    private String remoteIP;
    private String messageID;

    private Configuration conf;
    private MessageSpool spool;    
    private Logger logger;
    private ContextualizableContainer pool;

    // Hashtable to store the command handlers
    private Hashtable CmdManagers;
    // State information
    private Hashtable State;

    private String servername;
    private String postmaster;
    private String softwaretype;
        
    /**
     * Constructor has no parameters to allow Class.forName() stuff.
     */
    public SMTPHandler() {}

    /**
     * This method fills needed parameters in handler to make it work.
     */
    public void init(Context context) throws Exception {

        this.conf = context.getConfiguration();
        this.logger = (Logger) context.getImplementation(Interfaces.LOGGER);
        this.pool = (ContextualizableContainer) context.getImplementation("pool");

        // Hashtable to store the state
        State = new Hashtable();

        // Hashtable to store command managers
        CmdManagers = new Hashtable(8);

        try {
            this.servername = conf.getChild("servername").getValueAsString();
        } catch (Exception ce) {
            this.servername = java.net.InetAddress.getLocalHost().getHostName();
        }
                
        try {
            this.postmaster = conf.getChild("postmaster").getValueAsString();
        } catch (Exception ce) {
            this.postmaster = "postmaster@" + this.servername;
        }
                
        String softwarename = null;
        try {
            softwarename = conf.getChild("softwarename").getValueAsString();
        } catch (Exception ce) {
            softwarename = "Apache James";
        }
                
        String softwareversion = null;
        try {
            softwareversion = conf.getChild("softwareversion").getValueAsString();
        } catch (Exception ce) {
            softwareversion = "0.1";
        }

        this.softwaretype = softwarename + " v" + softwareversion;

        Enumeration commandE = null;

        for (commandE = conf.getChildren("command"); commandE.hasMoreElements();) {

            Configuration node = (Configuration) commandE.nextElement();
            String CmdName = node.getAttribute("name");					
            logger.log("Initializing SMTP command processor for " + CmdName);

            SimpleContext sc = new SimpleContext(node);
            sc.put(Interfaces.LOGGER, logger);
            sc.put("spool", (MessageSpool) context.getImplementation("spool") );
            sc.put("messageid", (SMTPServer.MessageId) context.getImplementation("messageid") );

            CmdManagers.put( CmdName, new SMTPCommandManager( sc ) );
                    
        }
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

        } catch (Exception e) {
        }

        logger.log("Connection from " + remoteHost + " (" + remoteIP + ")", "SMTPServer", logger.INFO);
    }
    
    /**
     * This method was created by a SmartGuide.
     */
    public void run() {
        
        // Reset state and make sure we are ready for new connection
        resetState();
                
        try {

            this.State.put(REMOTE_NAME, remoteHost);
            this.State.put(REMOTE_IP, remoteIP);

            // Initially greet the connector
            // Format is:  Sat,  24 Jan 1998 13:16:09 -0500

            out.println("220 " + this.servername + " ESMTP server (" + this.softwaretype + ") ready " + RFC822DateFormat.toString(new Date()));
            out.flush();
                
            // Now we sit and wait for responses.

            String command;
        String commandLine;
        SOCKET: while ((commandLine = in.readLine()) != null) {
                        
                    logger.log("Command [" + commandLine + "]", logger.DEBUG);
                            
                    CommandHandlerResponse chr = null;
                                
                    try {
                                
                        commandLine = commandLine.trim().toUpperCase();
                        int index = commandLine.indexOf(" ");
                
                        if ( index > 1 ) { // Make sure there is something in the command
                            command = commandLine.substring(0, index).toUpperCase();
                        } else {
                            command = commandLine;
                        }

                        CommandHandler ch = getCommandProcessors( command );
                        if ( ch != null ) {
                            System.out.println("Executing command handler for:" + commandLine);
                            chr = ch.service(commandLine, this);									
                        }
                                
                        if ( chr == null ) {
                            // couldn't find a command processor to process this
                            chr = new CommandHandlerResponse(500, "Command unrecognized: \"" + command + "\"");
                        }
                                
                    } catch (	NullPointerException e ) {

                        // null pointer thrown
                        chr = new CommandHandlerResponse(500, "Command exception");
                        e.printStackTrace( System.out );

                    }	finally {
                                
                        // Send response to output
                        chr.printResponse(out);

                        if (chr.getExitStatus() == CommandHandlerResponse.EXIT ) {
                            break SOCKET;									
                        }		
                            
                    }
                }
                        
        socket.close();
                        
       } catch (SocketException e) {
        logger.log("Socket to " + remoteHost + " closed remotely.", logger.DEBUG);
       } catch (InterruptedIOException e) {
         logger.log("Socket to " + remoteHost + " timeout.", logger.DEBUG);
       } catch (IOException e) {
         logger.log("Exception handling socket to " + remoteHost + ":" + e.getMessage(), logger.DEBUG);
       } catch (Exception e) {
         logger.log("Exception opening socket: " + e.getMessage(), logger.DEBUG);
             } finally {
            try {
            socket.close();
       } catch (IOException e) {
        logger.log("Exception closing socket: " + e.getMessage(), logger.DEBUG);						
             }
      }
                        System.out.println("done??");

        pool.recycle(this);
    }
    
    public void stop() {
            // todo
    }

        /**
         * Cleans up the protocol handler
         * @return void
         */
        public void destroy() {
            for (Enumeration e = CmdManagers.elements() ; e.hasMoreElements();) {
                ((CommandHandler) e.nextElement()).destroy();
            }
        }
        
        /**
         * Returns a Vector of the CommandProcessors for this command
         * @param java.lang.String command - the command that needs processing
         * @return org.apache.james.smtpserver.CommandHandler
         */
        public CommandHandler getCommandProcessors(String command) {
            if ( CmdManagers.containsKey(command) ) {
                return (CommandHandler) CmdManagers.get( command );									
            } else { return null; }
        }

        /**
         * Gets the output stream for this socket
         * @return java.io.PrintWriter
         */
        public PrintWriter getOut() {
            return out;
        }
        
        /**
         * Gets the input stream for this socket
         * @return java.io.BufferedReader
         */
        public BufferedReader getIn() {
            return in;
        }
        
        /**
         * Returns the hashtable of the CommandProcessors
         @ return java.util.Hashtable
         */
        public Hashtable getAllCommandProcessors() {
            return CmdManagers;
        }

    /**
         * Returns a hashtable used to store the state of the message delivery
     * @return java.util.Hashtable
     */
    public Hashtable getState() {
        return State;
    }

    /**
         * Returns a hashtable used to store the state of the message delivery
     * @param s java.util.Hashtable
         * @return void
     */
    public void setState( Hashtable s ) {
        State = s;
    }

    /**
     * Resets the session to default state
         * @return void
     */
        public void resetState()
        {
                this.State.clear(); // Erases all state entries
                
                this.State.put(SERVER_NAME, this.servername );
                this.State.put(POSTMASTER, this.postmaster );
                this.State.put(SERVER_TYPE, this.softwaretype );
        }
        
    /**
     * Cleans up this handler
         * @return void
     */
        public void clean() {
                this.socket = null;
                resetState();				
    }
}



/*--- formatting done in "Sun Java Convention" style on 07-10-1999 ---*/

