/*--- formatted by Jindent 2.0b, (www.c-lab.de/~jindent) ---*/

package org.apache.james.smtpserver;

import java.io.*;
import java.net.*;
import java.util.*;
import org.apache.james.smtpserver.command.CommandHandler;

/**
 * General Socket Handler. Gets a socket and menage protocol over it.
 * @author Federico Barbieri <scoobie@systemy.it>
 * @author matt@arcticmail.com
 * @version 0.91
 */
public interface ProtocolHandler {


		public final static String SERVER_NAME = "server.name";
		public final static String POSTMASTER = "postmaster";
		public final static String SERVER_TYPE = "server.type";

		/**
		 * Constant for the remote machine host name of machine establishing the connection
		 */
		public static String REMOTE_NAME = "REMOTE_NAME";
		
		/**
		 * Constant for the remote machine IP of machine establishing the connection
		 */
		public static String REMOTE_IP = "REMOTE_IP";
		
		/**
		 * Constant for the name given of machine establishing the connection
		 */
		public static String NAME_GIVEN = "NAME_GIVEN";

		/**
		 * Gets a hashtable containing state information for this connection
		 *
		 * @return java.util.Hashtable
		 */
		public java.util.Hashtable getState();

		/**
		 * Sets the hashtable containing state information for this connection
		 *
		 * @param h java.util.Hashtable
		 */
		public void setState( java.util.Hashtable h );
		
		/**
		 * Resets the hashtable containing state information for this connection to an initial state
		 */		
		public void resetState();
		
		/**
		 * Gets all the Command Processors objects for this connection.
		 *
		 * @return jave.util.Vector
		 */		
		public CommandHandler getCommandProcessors(String command);
		
		/**
		 * Gets all the Command Processors for this connection in a Hashtable.  Should be keyed by the command
		 *  that the CommandProcessor object processes.
		 *
		 * @return jave.util.Hashtable
		 */		
		public Hashtable getAllCommandProcessors();

		/**
		 * Returns a Buffered Reader for the socket
		 *
		 * @returns java.io.BufferedReader
		 */
		public BufferedReader getIn();

		/**
		 * Returns a PrintWriter for the socket
		 *
		 * @returns java.io.PrintWriter
		 */
		public PrintWriter getOut();

}



/*--- formatting done in "Sun Java Convention" style on 07-10-1999 ---*/

