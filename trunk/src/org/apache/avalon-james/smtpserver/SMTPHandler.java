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

/**
 * This handles an individual incoming message.  It handles regular SMTP
 * commands, and when it receives a message, adds it to the spool.
 * @author Serge Knystautas <sergek@lokitech.com>
 * @author Federico Barbieri <scoobie@systemy.it>
 * @version 0.9
 */
public class SMTPHandler implements Contextualizable, Stoppable, Recyclable {
    private Socket socket;
    private BufferedReader in;
    private InputStream socketIn;
    private PrintWriter out;
    private OutputStream r_out;
    private InternetAddress sender;
    private Vector recipients;
    private String remoteHost;
    private String remoteHostGiven;
    private String remoteIP;
    private String messageID;
    private static int mid = 0;
    private String socketID;
    private static String messageEnd = "\r\n.\r\n";
    private static int scount = 0;


    private Configuration conf;
    private String serverName;
    private String softwareName;
    private String softwareVersion;
    private MessageSpool spool;    
    private Logger logger;
    private ContextualizableContainer pool;
    
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
        this.spool = (MessageSpool) context.getImplementation("spool");
        this.pool = (ContextualizableContainer) context.getImplementation("pool");
        
        this.serverName = conf.getChild("servername").getValueAsString();
        this.softwareName = conf.getChild("softwarename").getValueAsString();
        this.softwareVersion = conf.getChild("softwareversion").getValueAsString();
        this.recipients = new Vector();
    }

    public void clean() {

        this.sender = null;
        this.recipients.clear();
        this.socket = null;
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
            socketID = remoteHost + "." + ++scount;
        } catch (Exception e) {
        }

        logger.log("Connection from " + remoteHost + " (" + remoteIP + ") on socket " + socketID, "SMTPServer", logger.INFO);
    }
    
    /**
     * This method was created by a SmartGuide.
     */
    public void run() {
        try {

            // Initially greet the connector
            // Format is:
            // Sat,  24 Jan 1998 13:16:09 -0500

            out.println("220 " + serverName + " ESMTP server (" + softwareName + " v" + softwareVersion + ") ready " + RFC822DateFormat.toString(new Date()));
            out.flush();

            // Now we sit and wait for responses.

            String line;

            while ((line = in.readLine()) != null && !handleCommand(line)) {}

            socket.close();
        } catch (SocketException e) {
            logger.log("Socket " + socketID + " closed remotely.", "SMTPServer", logger.ERROR);
        } catch (InterruptedIOException e) {
            logger.log("Socket " + socketID + " timeout.", "SMTPServer", logger.ERROR);
        } catch (IOException e) {
            logger.log("Exception handling socket " + socketID + ": " + e.getMessage(), "SMTPServer", logger.ERROR);
        } finally {
            try {
                socket.close();
            } catch (IOException e) {}
        }
        pool.recycle(this);
    }
    
    public void stop() {
    }

    /**
     * Method Declaration.
     * 
     * 
     * @param command
     * 
     * @return
     * 
     * @exception IOException
     * 
     * @see
     */
    private boolean handleCommand(String command) throws IOException {
        String commandLine = command.trim().toUpperCase();

        if (commandLine.startsWith("HELO")) {
            out.println("250 " + serverName);
            out.flush();

            try {
                remoteHostGiven = command.trim().substring(5);
            } catch (Exception e) {
                remoteHostGiven = "";

                logger.log("Unknown given host: " + e.getMessage(), "SMTPServer", logger.ERROR);
            }
            return false;
        }
        if (commandLine.startsWith("HELP")) {

            // Need to expand this to support individual command help

            out.println("214-This SMTP server is a part of the Java Mail Servlet Engine.  For");
            out.println("214-information about the Loki Technologies Mail Server, please see");
            out.println("214-http://www.lokitech.com.");
            out.println("214-");
            out.println("214-      Supported commands:");
            out.println("214-");
            out.println("214-           EHLO     HELO     MAIL     RCPT     DATA");
            out.println("214-           VRFY     RSET     NOOP     QUIT");
            out.println("214-");
            out.println("214-      SMTP Extensions supported through EHLO:");
            out.println("214-");
            out.println("214-           EXPN     HELP     SIZE");
            out.println("214-");
            out.println("214-For more information about a listed topic, use \"HELP <topic>\"");
            out.println("214 Please report mail-related problems to Postmaster at this site.");
            out.flush();

            return false;
        }
        if (commandLine.startsWith("MAIL")) {

            // We need to set the address provided as the default address

            if (sender != null) {
                out.println("503 Sender already specified");
                out.flush();

                return false;
            }
            if (!commandLine.startsWith("MAIL FROM:")) {
                out.println("501 Usage: MAIL FROM:<sender>");
                out.flush();

                return false;
            }

            String send = command.substring(10).trim();

            sender = null;

            try {
                sender = new InternetAddress(send);
            } catch (AddressException e) {
                out.println("553 Invalid address syntax");
                out.flush();

                sender = null;

                return false;
            }

            out.println("250 Sender <" + sender + "> OK");
            out.flush();
            logger.log("Sender received on " + socketID + ": " + sender, "SMTPServer", logger.DEBUG);

            return false;
        }
        if (commandLine.startsWith("RCPT")) {
            if (!commandLine.startsWith("RCPT TO:")) {
                out.println("501 Usage: RCPT TO:<recipient>");
                out.flush();

                return false;
            }

            String rcpt = commandLine.substring(8).trim();
            InternetAddress recipient = null;

            try {
                recipient = new InternetAddress(rcpt);
            } catch (AddressException e) {
                out.println("553 Invalid address syntax");
                out.flush();

                return false;
            }
//      I've changed this to a Vector of Strings since InternetAddress seems not
//      to implements Seralizable.

// [was]           recipients.addElement(recipient);
            recipients.addElement(rcpt);
            // We need to add this to the list of recipients

            out.println("250 Recipient <" + recipient + "> OK");
            out.flush();
            logger.log("Recipient received on " + socketID + ": " + recipient, "SMTPServer", logger.DEBUG);

            return false;
        }
        if (commandLine.startsWith("EHLO")) {
            out.println("220 Command not implemented");
            out.flush();

            return false;
        }
        if (commandLine.startsWith("DATA")) {
            if (sender == null) {
                out.println("503 No sender specified");
                out.flush();

                return false;
            }
            if (recipients.size() == 0) {
                out.println("503 No recipients specified");
                out.flush();

                return false;
            }

            out.println("354 Ok Send data ending with <CRLF>.<CRLF>");
            out.flush();
            messageID = getMessageID();
            logger.log("Receiving data (" + messageID + ") on " + socketID, "SMTPServer", logger.DEBUG);

//      We store the sender as a String since InternetAddress seems not to 
//      implements Seralizable.
            OutputStream body = spool.addMessage(messageID, sender.getAddress(), recipients);
            int match = 0;
            while (match < messageEnd.length()) {
                byte read = (byte) in.read();
                if (read == messageEnd.charAt(match)) {
                    match++;
                } else {
                    if (match > 0) {
                        match = 0;
                        if (read == messageEnd.charAt(0)) {
                            match++;
                        }
                    }
                }

                body.write(read);
            }
            body.flush();
            body.close();
            spool.free(messageID);

            // Now we're done

            out.println("250 Message received: " + messageID);
            out.flush();

            sender = null;
            recipients.clear();

            return false;
        }
        if (commandLine.startsWith("VRFY")) {

            // Verifies an email address

            out.println("250 Ok");
            out.flush();

            return false;
        }
        if (commandLine.startsWith("RSET")) {

            // Resets the state

            out.println("250 Reset state");
            out.flush();

            sender = null;

            recipients.clear();

            return false;
        }
        if (commandLine.startsWith("NOOP")) {

            // No operation

            out.println("250 Ok");
            out.flush();

            return false;
        }
        if (commandLine.startsWith("EXPN")) {
            out.println("250 Ok");
            out.flush();

            return false;
        }
        if (commandLine.startsWith("QUIT")) {
            out.println("221 " + serverName + " ESMTP server closing connection");
            out.flush();

            return true;
        }

        out.println("501 Command unknown: '" + command + "'");
        out.flush();

        return false;
    }

    /**
     * Method Declaration.
     * 
     * 
     * @param v
     * 
     * @return
     * 
     * @exception ClassCastException
     * 
     * @see
     */
    private InternetAddress[] AddressVectorToArray(Vector v) throws ClassCastException {
        InternetAddress[] array = new InternetAddress[v.size()];
        int i = 0;

        for (Enumeration e = v.elements(); e.hasMoreElements(); i++) {
            array[i] = (InternetAddress) e.nextElement();
        }

        return array;
    }

    public void destroy() throws Exception {
    }

    private String getMessageID() {

        return new String(new Date().getTime() + "III" + mid++ + "III@" + serverName);
    }

}



/*--- formatting done in "Sun Java Convention" style on 07-10-1999 ---*/

