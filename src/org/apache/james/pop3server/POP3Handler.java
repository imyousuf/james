/*--- formatted by Jindent 2.0b, (www.c-lab.de/~jindent) ---*/

package org.apache.james.pop3server;

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
import org.apache.avalon.blocks.*;
import org.apache.james.*;
import org.apache.java.util.*;
import org.apache.java.lang.*;
import org.apache.java.recycle.*;
import org.apache.avalon.util.*;
import org.apache.avalon.SimpleContext;

/**
 * @author Serge Knystautas <sergek@lokitech.com>
 * @author Federico Barbieri <scoobie@systemy.it>
 * @version 0.9
 */
public class POP3Handler implements Contextualizable, Stoppable, Recyclable {

    private Socket socket;
    private BufferedReader in;
    private InputStream socketIn;
    private PrintWriter out;
    private OutputStream r_out;
    private Configuration conf;
    private String remoteHost;
    private String remoteIP;
    private Logger logger;
    private Store store;
    private Store.Repository mailRepository;
    private Store.ObjectRepository userRepository;
    private String servername;
    private String postmaster;
    private String softwaretype;
    private int state;
    private String user, passw;
    private ContextualizableContainer pool;
    
    
    private static int READY = 0;
    private static int USERSET = 1;
    private static int AUTHENTICATED = 2;
    private static int QUIT = 10;
        
    /**
     * Constructor has no parameters to allow Class.forName() stuff.
     */
    public POP3Handler() {}

    /**
     * This method fills needed parameters in handler to make it work.
     */
    public void init(Context context) throws Exception {

        this.conf = context.getConfiguration();
        this.logger = (Logger) context.getImplementation(Interfaces.LOGGER);
        this.pool = (ContextualizableContainer) context.getImplementation("pool");
        this.store = (Store) context.getImplementation(Interfaces.STORE);
        this.mailRepository = store.getPublicRepository("LocalInbox");
        this.userRepository = (Store.ObjectRepository) store.getPublicRepository("MailUsers");

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
    }

    public void parseRequest(Socket socket) {

        try {
            this.socket = socket;
            socketIn = socket.getInputStream();
            in = new BufferedReader(new InputStreamReader(socketIn));
            r_out = socket.getOutputStream();
            out = new PrintWriter(r_out, true);
    
            remoteHost = "pippo";
            remoteIP = "192.168.1.3";
//            remoteHost = socket.getInetAddress ().getHostName ();
//            remoteIP = socket.getInetAddress ().getHostAddress ();

        } catch (Exception e) {
        }

        logger.log("Connection from " + remoteHost + " (" + remoteIP + ")", "POP3Server", logger.INFO);
    }
    
    public void run() {
    	
        try {
            state = READY;
            out.println(this.servername + " POP3 server (" + this.softwaretype + ") ready ");
            out.flush();
            while (parseCommand(in.readLine()));
            out.println("by " + user);
            out.flush();
            socket.close();
        } catch (IOException e) {
            out.println("Error. Closing connection");
            out.flush();
            logger.log("Exception during connection from " + remoteHost + " (" + remoteIP + ")", "POP3Server", logger.ERROR);
        }
        pool.recycle(this);
    }
    
    private boolean parseCommand(String command) {
        command = command.trim().toUpperCase();
        if (command.startsWith("USER")) {
            if (state == READY) {
                user = command.substring(5);
                state = USERSET;
                out.println("OK.");
                out.flush();
            } else {
                out.println("ERR.");
                out.flush();
            }
            return true;
        } else if (command.startsWith("PASSW")) {
            if (state == USERSET) {
                passw = command.substring(6);
                String serverpassw = (String) userRepository.get(user);
                if (passw.equals(serverpassw)) {
                    state = AUTHENTICATED;
                    out.println("OK. Welcome " + user);
                    out.flush();
                } else {
                    state = READY;
                    out.println("ERR. Authentication failed");
                    out.flush();
                }
            } else {
                out.println("ERR.");
                out.flush();
            }
            return true;
        } else if (command.startsWith("MAIL")) {
            if (state == AUTHENTICATED) {
                out.println("OK.");
                out.flush();
            } else {
                out.println("ERR.");
                out.flush();
            }
            return true;
        } else if (command.startsWith("QUIT")) {
            return false;
        }
        out.println("ERR.");
        out.flush();
        return true;
    }
    
    public void stop() {
            // todo
    }

    public void destroy() {
    }
       
    public void clean() {
        this.socket = null;
        state = READY;
    }
}



/*--- formatting done in "Sun Java Convention" style on 07-10-1999 ---*/