/*--- formatted by Jindent 2.0b, (www.c-lab.de/~jindent) ---*/

package org.apache.james.server;

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
import java.lang.reflect.*;
import javax.mail.*;
import javax.mail.internet.*;
import javax.activation.*;
import org.apache.james.*;
import org.apache.james.util.*;

/**
 * This handles an individual incoming message.  It handles regular SMTP
 * commands, and when it receives a message, adds it to the spool.
 * @author Serge Knystautas <sergek@lokitech.com>
 * @author Federico Barbieri <scoobie@systemy.it>
 * @version 0.9
 */
public class SMTPHandler implements ProtocolHandler {
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
    private int mid = 0;
    private String socketID;
    private JamesServ server;
    private LoggerInterface logger;
    private static String messageEnd = "\r\n.\r\n";
    private static int scount = 0;

    /**
     * Constructor has no parameters to allow Class.forName() stuff.
     */
    public SMTPHandler() {}

    /**
     * This method fills needed parameters in handler to make it work.
     */
    public void fill(JamesServ server, Socket socket) throws IOException {
        this.logger = server.getLogger();
        this.socket = socket;
        this.server = server;
        this.sender = null;
        this.recipients = new Vector();

        try {

            // Someone has opened a connection to us.  We initialize the streams

            socketIn = socket.getInputStream();
            in = new BufferedReader(new InputStreamReader(socketIn));
            r_out = socket.getOutputStream();
            out = new PrintWriter(r_out, true);

            remoteHost = socket.getInetAddress ().getHostName ();
            remoteIP = socket.getInetAddress ().getHostAddress ();
            
            System.out.println("remote host: " + remoteHost);
            System.out.println("remote ip: " + remoteIP);
            
            socketID = remoteHost + "." + ++scount;

            logger.log("Connection from " + remoteHost + " (" + remoteIP + ") on socket " + socketID, logger.INFO_LEVEL);
        } catch (Exception e) {
            logger.log("Exception opening socket: " + e.getMessage(), logger.ERROR_LEVEL);
        }
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

        logger.log("Command " + commandLine + " received on socket " + socketID, logger.INFO_LEVEL);

        if (commandLine.startsWith("HELO")) {
            out.println("250 " + server.getProperty("server.name"));
            out.flush();

            try {
                remoteHostGiven = command.trim().substring(5);
            } catch (Exception e) {
                remoteHostGiven = "";

                logger.log("Unknown given host: " + e.getMessage());
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
            logger.log("Sender received on " + socketID + ": " + sender, logger.DEBUG_LEVEL);

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

            recipients.addElement(recipient);

            // We need to add this to the list of recipients

            out.println("250 Recipient <" + recipient + "> OK");
            out.flush();
            logger.log("Recipient received on " + socketID + ": " + recipient, logger.DEBUG_LEVEL);

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
            logger.log("Receiving data on " + socketID, logger.DEBUG_LEVEL);

            try {
                MimeMessage msg = processMessage();

                if (msg != null) {

                    // System.out.println ("\n This is the message:");
                    // msg.writeTo (System.out);

                    DeliveryState state = new DeliveryState();

                    state.setRecipients(AddressVectorToArray(recipients));
                    state.put("remote.host", remoteHost);

                    if (remoteHostGiven != null) {
                        state.put("remote.host.given", remoteHostGiven);
                    }

                    state.put("remote.ip", remoteIP);
                    server.getSpool().addMessage(msg, state);
                }
            } catch (MessagingException me) {
                me.printStackTrace();
            }

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
            out.println("221 " + server.getProperty("server.name") + " ESMTP server closing connection");
            out.flush();

            return true;
        }

        out.println("501 Command unknown: '" + command + "'");
        out.flush();

        return false;
    }

    /**
     * This method was created in VisualAge.
     */
    private MimeMessage processMessage() throws MessagingException {
        Session session = Session.getDefaultInstance(new Properties(), null);

        session.setDebug(false);

        MimeMessage msg = null;

        messageID = server.getMessageID();

        try {

            // Now we need to read in all the data into a byte array, then we
            // send that into the MimeMessage constructor

            ByteArrayOutputStream byteOut = new ByteArrayOutputStream(4096);
            int match = 0;

            // Read in 1 character at a time

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

                byteOut.write(read);
            }

            // We now have the whole message in a byte array - 3

            byte mba[] = byteOut.toByteArray();
            ByteArrayInputStream byteIn = new ByteArrayInputStream(mba, 0, mba.length - messageEnd.length());

            // We use our custom child of MimeMessage, to better handle some server side aspects of messages

            msg = new ServerMimeMessage(session, byteIn);

            // Set the Return-Path: if it is not set

            if (msg.getHeader("Return-Path") == null) {
                msg.addHeader("Return-Path", "<" + sender.getAddress() + ">");
            }

            // Add the Received: header

            String received = "from " + remoteHost + " ([" + remoteIP + "])\r\n          by " + server.getProperty("server.name") + " (" + server.getProperty("software.name") + " v" + server.getProperty("software.version") + ") with SMTP ID " + mid;

            if (recipients.size() == 1) {
                received += "\r\n          for <" + recipients.elementAt(0).toString() + ">";
            }

            received += ";\r\n          " + RFC822DateFormat.toString(new Date());

            msg.addHeader("Received", received);

            // Set the Message-ID: if it is not set

            if (msg.getHeader("Message-ID") == null) {
                msg.addHeader("Message-ID", messageID);
            } else {
                messageID = msg.getMessageID();
            }

            // Set the Date: if it is not set

            if (msg.getHeader("Date") == null) {
                msg.addHeader("Date", RFC822DateFormat.toString(new Date()));
            }

            // Set the From: if it is not set

            if (msg.getHeader("From") == null) {
                msg.addHeader("From", sender.getAddress());
            }

            // Set the To: if it is not set
            // ***** We will remove this later once we properly handle forwarding email messages

            if (msg.getHeader("To") == null) {
                msg.setRecipients(Message.RecipientType.TO, AddressVectorToArray(recipients));
            }
        } catch (IOException ioe) {
            out.println("550 Some error...");
            out.flush();

            return null;
        } catch (MessagingException me) {
            out.println("550 Messaging exception");
            out.flush();
            me.printStackTrace();

            return null;
        }

        return msg;
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

    /**
     * This method was created by a SmartGuide.
     */
    public void run() {
        try {

            // Initially greet the connector
            // Format is:
            // Sat,  24 Jan 1998 13:16:09 -0500

            out.println("220 " + server.getProperty("server.name") + " ESMTP server (" + server.getProperty("software.name") + " v" + server.getProperty("software.version") + ") ready " + RFC822DateFormat.toString(new Date()));
            out.flush();

            // Now we sit and wait for responses.

            String line;

            while ((line = in.readLine()) != null && !handleCommand(line)) {}

            socket.close();
        } catch (SocketException e) {
            logger.log("Socket " + socketID + " closed remotely.", logger.INFO_LEVEL);
        } catch (InterruptedIOException e) {
            logger.log("Socket " + socketID + " timeout.", logger.INFO_LEVEL);
        } catch (IOException e) {
            logger.log("Exception handling socket " + socketID + ": " + e.getMessage(), logger.ERROR_LEVEL);
            System.exit(1);
        }
        finally {
            try {
                socket.close();
            } catch (IOException e) {}
        }
    }

}



/*--- formatting done in "Sun Java Convention" style on 07-10-1999 ---*/

