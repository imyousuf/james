/*--- formatted by Jindent 2.0b, (www.c-lab.de/~jindent) ---*/

package org.apache.james.server;

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;
import javax.activation.*;
import org.apache.james.*;
import org.apache.james.util.*;
import org.apache.james.server.socket.SocketHandler;

/**
 * <b>Run this class!!</b>
 * Pass it the name of a conf file.  This will be used to configure the server daemon, the spool, and
 * the mail servlets themselves.  Hope it works for you!
 * @author Serge Knystautas <sergek@lokitech.com>
 * @author Federico Barbieri <scoobie@systemy.it>
 * @version 0.9
 */
public class JamesServ {

    private Properties props;
    private LoggerInterface logger;
    private JamesSpoolManager spoolMgr = null;
    protected static int mid = 0;
    private MessageSpool spool;

		public static boolean DEBUG = true;

    /**
     * This method was created in VisualAge.
     * @param props java.util.Properties
     */
    public JamesServ(String confFile) {

        // Create a new logger.
        logger = new Logger();
        logger.log("Logger instantiated", logger.INFO_LEVEL);

        // Loadig properties
        props = new Properties();

        try {
            FileInputStream in = new FileInputStream(confFile);
            props.load(in);
            in.close();
        } catch (IOException e) {
            logger.log("Cannot open configuration file: " + e.getMessage(), logger.ERROR_LEVEL);
            System.exit(1);
        }
        logger.log("Properties loaded", logger.INFO_LEVEL);

        if (props.getProperty("server.name") == null) {
            InetAddress serverHost = null;
            try {
                serverHost = InetAddress.getLocalHost();
                props.put("server.name", serverHost.getHostName());
            } catch (UnknownHostException e) {
                logger.log("Cannot identify LocalHost: " + e.getMessage(), logger.ERROR_LEVEL);
                System.exit(1);
            }
        }

				// Create the message spooler
        try {
            spool = new org.apache.james.server.MessageSpool();
            spool.init(this, getBranchProperties(props, "spool."));
        } catch (Exception e) {
            logger.log("Exception in Message spool init: " + e.getMessage(), logger.ERROR_LEVEL);
						e.printStackTrace();
            System.exit(1);
        }
        logger.log("Message spool instantiated.", logger.INFO_LEVEL);

        // Create a new spool manager
        try {
            spoolMgr = new JamesSpoolManager();
            spoolMgr.init(this, getBranchProperties(props, "spoolManager."));
        } catch (Exception e) {
            logger.log("Exception in SpoolManager init: " + e.getMessage(), logger.ERROR_LEVEL);
            System.exit(1);
        }
        logger.log("SpoolManager instantiated", logger.INFO_LEVEL);

        int threads = 1;
        try {
            threads = Integer.parseInt(props.getProperty("spoolManagerthreads"));
        } catch (Exception e) {
            logger.log("Unknown spoolManagerthreads. Using default(" + threads + "): " + e.getMessage(), logger.WARNING_LEVEL);
        }

        while (threads-- > 0) {
            new Thread(spoolMgr, "James spoolmgr " + (threads + 1)).start();
            logger.log("Spool tread " + (threads + 1) + " started", logger.INFO_LEVEL);
        }
    }

    /**
     * This method was created in VisualAge.
     * @return java.lang.String
     */
    public String getMessageID() {

        Date msgTime = new Date();
        return msgTime.getTime() + "." + mid++ + "@" + getProperty("server.name");
    }

    /**
     * Return logger.
     */
    public LoggerInterface getLogger() {
        return logger;
    }

    /**
     * This method was created in VisualAge.
     * @return java.lang.String
     * @param name java.lang.String
     */
    public String getProperty(String name) {
        return props.getProperty(name);
    }

    /**
     * This method was created in VisualAge.
     * @return org.apache.james.MessageSpool
     */
    public MessageSpool getSpool() {
        return spool;
    }

    /**
     * This is the main entry point of the James server. It open porperties, 
     * initializes spool and logger and starts listeners.
     * @param arg java.lang.String[]
     */
    public static void main(String arg[]) {
        if (arg.length != 1) {
            System.out.println("usage: java org.apache.james.JamesServ configFile");
        } else {
            new JamesServ(arg[0]).run();
        }
    }

    /**
     * This method was created in VisualAge.
     */
    public void run() {
        System.out.println("MXServ is running");
        logger.log("MXServ started", logger.INFO_LEVEL);

        String portS;
        String handlerS;


        for (int i = 1; i < Integer.parseInt(props.getProperty("listener.number", "1")) + 1; i++) {
            try {
							String slPrefix = "listener." + i + ".";
							String slClass = slPrefix + "class";
							
							if ( props.getProperty(slClass) == null ) {
								logger.log("No listeners class specified in configuration file for listener " + i);
								System.exit(-1);	
							} else {
								SocketHandler sl = (SocketHandler) Class.forName( props.getProperty(slClass) ).newInstance();
              	sl.init(this, this.getBranchProperties(props, slPrefix));
                new Thread(sl).start();
              }
            } catch (Exception e) {
								e.printStackTrace();
                logger.log("Exception starting listener(" + i + "): " + e.getMessage(), logger.ERROR_LEVEL);
								System.exit(-1);	
            }
        }
    }

    public Properties getBranchProperties(Properties root, String prefix) {
        Properties branch = new Properties();
        String rootName;
        int prefixLength = prefix.length();
        for (Enumeration e = root.propertyNames(); e.hasMoreElements();) {
            rootName = (String) e.nextElement();
            if (rootName.startsWith(prefix)) {
                branch.put(rootName.substring(prefixLength), root.getProperty(rootName));
            }
        }
        return branch;
    }
}



/*--- formatting done in "Sun Java Convention" style on 07-11-1999 ---*/

