/*--- formatted by Jindent 2.0b, (www.c-lab.de/~jindent) ---*/

package org.apache.james.server.socket;

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;
import javax.activation.*; // Required for javax.mail
import org.apache.james.*;
import org.apache.james.server.*;
import org.apache.james.util.*;
import org.apache.james.server.protocol.ProtocolHandler;

/**
 * Just wait on the specified port and hands off the socket to a pool
 * is opened.
 * @author Federico Barbieri <scoobie@systemy.it>
 * @author Matthew Petteys <matt@arcticmail.com>
 * @version 0.9
 */

public class SocketListener
	implements SocketHandler
{
    private ServerSocket ss;
    private int port;
    private String poolClassName;
    private JamesServ serv;
    private LoggerInterface logger;
    private int timeout;
		private Properties props;
		private boolean run;
		private PoolInterface pool;
		
		private final static String DefaultSocketTimeout = "12000";
		private final static String DefaultStartingThreads = "2";
		private final static String DefaultMaxThreads = "10";
		
    /**
     * Empty class constructor.
     */
    public SocketListener() {}

    /**
     * Initialize the socket listener
     * @param serv org.apache.james.server.JamesServ
     * @param inProps java.util.Properties
		 * @return void
     * @exception Exception
     */
    public void init(JamesServ serv, Properties inProps) 
    	throws Exception
		{
		
				this.run = true;
				this.props = inProps;
        this.serv = serv;
        this.poolClassName = props.getProperty("pool");
				
				if ( poolClassName == null) {
					throw new Exception("No pool class defined");
				}
				
        this.port = Integer.parseInt(props.getProperty("port"));
        this.timeout = Integer.parseInt(props.getProperty("timeout", DefaultSocketTimeout));
				
        this.ss = new ServerSocket(port);
        this.logger = serv.getLogger();
				
				try {
					pool = (PoolInterface) (Class.forName( poolClassName )).newInstance();
					pool.init(serv, props);
					logger.log("SocketListenerPool started..");
				} catch (ClassNotFoundException cnfe) {
					cnfe.printStackTrace();
					throw new Exception("Error Finding Socket Listener Thread Pool");
				} catch (Exception e) {
					e.printStackTrace();
					throw new Exception("Error Initializing Thread Pool");
				}				
    }

    /**
     * Starts the thread that listens for the connections
		 * @return void
     */			
    public void run() {

        logger.log("PooledProtocolHandler is " + poolClassName + " on port " + port + ".", logger.INFO_LEVEL);
        Socket s;
				
        while (this.run) {
					try {						
            s = ss.accept();
            s.setSoTimeout(timeout);
        		logger.log("Connection received.", logger.INFO_LEVEL);
						pool.service(s);
					} catch (Exception e) {
						e.printStackTrace();
						this.logger.log("Exception from spool.performWork:" + e.getMessage());
					}
				}

				// Clean up the pool
				pool.destroy();
    }

}



/*--- formatting done in "Sun Java Convention" style on 07-10-1999 ---*/

