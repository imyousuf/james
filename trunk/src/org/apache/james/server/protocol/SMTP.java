/*--- formatted by Jindent 2.0b, (www.c-lab.de/~jindent) ---*/

package org.apache.james.server.protocol;

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
import java.lang.reflect.*;
import javax.mail.*;
import javax.mail.internet.*;
import javax.activation.*; // Required for javax.mail
import org.apache.james.*;
import org.apache.james.server.*;
import org.apache.james.util.*;
import org.apache.james.server.command.*;
import org.apache.james.server.command.smtp.SMTPCommandManager;

/**
 * This handles an individual incoming message.  It handles regular SMTP
 * commands, and when it receives a message, adds it to the spool.
 * @author Serge Knystautas <sergek@lokitech.com>
 * @author Federico Barbieri <scoobie@systemy.it>
 * @author Matthew Petteys <matt@arcticmail.com>
 * @version 0.9
 */
public class SMTP
	implements ProtocolHandler
{

    private Socket socket;
    private BufferedReader in;
    private InputStream socketIn;
    private PrintWriter out;
    private OutputStream r_out;
    private InternetAddress sender;
    private Vector recipients;
    private String socketID;
    private JamesServ server;
    private LoggerInterface logger;
    private static int scount = 0;

		// Hashtable to store the command handlers
		private java.util.Hashtable CmdManagers;
		// Hashtable to stor state information
		private java.util.Hashtable State;

		// Properties of this connection
    private Properties props;

		private static String PROT_PREFIX = "protocol.";

    /**
     * Constructor has no parameters to allow Class.forName() stuff.
     */
    public SMTP() {}

    /**
     * This method fills needed parameters in handler to make it work.
		 * @param org.apache.james.server.JamesServ s
		 * @param java.util.Properties inProps
		 * @return void
     */
    public void init(JamesServ s, Properties inProps)
			throws Exception
		{

			this.State = new Hashtable(20);
      this.server = s;			
			this.props = s.getBranchProperties( inProps, PROT_PREFIX );
			this.logger = server.getLogger();
			
			logger.log( "Initializing SMTP");
			
			//props.list(System.out);
			
			// Get the commands for this protocol handler
			String commands = props.getProperty("commands");
			if ( commands == null ) {
				throw new Exception("No commands defined for protocol handler");
			}
			
			// Hashtable to store command managers
			CmdManagers = new Hashtable(8);
			
			Properties ManagerProps = null;
			CommandManager cm = null;
			
			for (Enumeration e = new StringTokenizer(commands, ","); e.hasMoreElements();) {

				// Lets get the name after the command prefix
				String CmdName = (String) e.nextElement();
				logger.log("Initializing SMTP command processor for " + CmdName);

				ManagerProps = s.getBranchProperties( props, CmdName + "." );
				
				try {
					cm = new SMTPCommandManager( ManagerProps, s);
				} catch (IllegalAccessException exc) {
					logger.log("Illegal Access Exception in CommandManager :" + cm);
				} catch (InstantiationException exc) {
					logger.log("Instantiation Exception in CommandManager :" + cm);
				} catch (ClassNotFoundException exc) {
					logger.log("Couldn't find class for in CommandManager :" + cm);
				}

				CmdManagers.put( CmdName, cm );
			}				
		}
		
    /**
     * This method processes a single socket connection.
		 * @param Objext the object to be processed that is passed from the pool
		 * @return void
     */
    public void service(Object workObject)
		{
				// Reset state and make sure we are ready for new connection
				resetState();

				// we are working with an incoming socket
        this.socket = (Socket) workObject;
				
        try {

       			// Someone has opened a connection to us.  We initialize the streams
            socketIn = socket.getInputStream();
            in = new BufferedReader(new InputStreamReader(socketIn));
            r_out = socket.getOutputStream();
            out = new PrintWriter(r_out, true);

            socketID = State.get(REMOTE_NAME) + "." + ++scount;

            logger.log("Connection from " + State.get(REMOTE_NAME) + " (" + State.get(REMOTE_IP) + ") on socket " + socketID, logger.INFO_LEVEL);

						this.State.put(REMOTE_NAME, socket.getInetAddress().getHostName());
						this.State.put(REMOTE_IP, socket.getInetAddress().getHostAddress());

            // Initially greet the connector
            // Format is:  Sat,  24 Jan 1998 13:16:09 -0500

            out.println("220 " + server.getProperty("server.name") + " ESMTP server (" + server.getProperty("software.name") + " v" + server.getProperty("software.version") + ") ready " + RFC822DateFormat.toString(new Date()));
            out.flush();

            // Now we sit and wait for responses.

            String commandLine;

            SOCKET: while ((commandLine = in.readLine()) != null) {
						
							if ( JamesServ.DEBUG ) {
								logger.log("Command [" + commandLine + "] received on socket " + socketID, logger.INFO_LEVEL);
							}
							
							CommandHandlerResponse chr = null;
								
							try {
								String command = null;
								
								commandLine = commandLine.trim().toUpperCase();
								int index = commandLine.indexOf(" ");
				
								if ( index > 1 ) { // Make sure there is something in the command
									command = commandLine.substring(0, index ).toUpperCase();
								} else {
									command = commandLine;
								}

								CommandHandler ch = getCommandProcessors( command );
								if ( ch != null ) {
									logger.log("Executing command handler for:" + commandLine);
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
            logger.log("Socket " + socketID + " closed remotely.", logger.INFO_LEVEL);
        	} catch (InterruptedIOException e) {
            logger.log("Socket " + socketID + " timeout.", logger.INFO_LEVEL);
        	} catch (IOException e) {
            logger.log("Exception handling socket " + socketID + ": " + e.getMessage(), logger.ERROR_LEVEL);
        	} catch (Exception e) {
            logger.log("Exception opening socket: " + e.getMessage(), logger.ERROR_LEVEL);
  				} 
					finally {
            try {
                socket.close();
            } catch (IOException e) {}
        	}
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
		 * @return org.apache.james.server.command.CommandHandler
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
				this.sender = null;
        this.recipients = new Vector(1);
        //this.recipients.clear(); // Darn 1.2 functions
		}

/*--- formatting done in "Sun Java Convention" style on 07-10-1999 ---*/

}

