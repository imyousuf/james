package org.apache.james.smtpserver.command;

import org.apache.james.smtpserver.*;
import org.apache.james.*;
import org.apache.avalon.blocks.*;
import org.apache.java.util.*;
import java.util.*;
import javax.mail.internet.*;
import javax.mail.*;
import javax.activation.*; // Required for javax.mail
import java.io.*;
import org.apache.java.util.*;

/**
 * Command handler for the SMTP DATA Command
 *
 * @author Serge Knystautas <sergek@lokitech.com>
 * @author Matthew Petteys <matt@arcticmail.com>
 */

public class data
    extends GenericCommand
{
    private final static int TempMessageBuffer = 10000;

    private final static String messageEnd = "\r\n.\r\n";
    private MessageSpool spool;    
    private Logger logger;
    private Hashtable state;
    private SMTPServer.MessageId MessageId;
    
    public void init(Context context)
        throws Exception
    {
        this.logger = (Logger) context.getImplementation(Interfaces.LOGGER);
        this.spool = (MessageSpool) context.getImplementation("spool");
        this.MessageId = (SMTPServer.MessageId) context.getImplementation("messageid");
    }

    public CommandHandlerResponse service(String command, ProtocolHandler p)
    {
        
        logger.log("Processing data command..");

        state = p.getState();

        if ( ! state.containsKey(mail.FROM) ) {
            return new CommandHandlerResponse(503, "No sender specified");
        } else { logger.log(state.get(mail.FROM)); }
            
        
        if ( ! state.containsKey(rcpt.RCPT_VECTOR) ) {
            return new CommandHandlerResponse(503, "No recipients specified");
        }

        // mid is nothing
        String messageID = MessageId.getNew();
        OutputStream body = null;

        try {

            PrintWriter out = p.getOut();
            BufferedReader in = p.getIn();

            (new CommandHandlerResponse(354, "Ok Send data ending with <CRLF>.<CRLF>")).printResponse(out);

            // Buffering to check for lines that start w/ .
            int match = 1; // Mark first byte matched for initial buffer read

            // We are going to buffer the message
            int TempMessagePos = 0;
            byte[] TempMessage = new byte[TempMessageBuffer];

            byte buffer = (byte) in.read(); // initialize buffer var			            

            while (match < messageEnd.length()) {
                byte read = (byte) in.read();
                
                if (read == messageEnd.charAt(match)) {
                    logger.log("Match:" + new Integer(match) +":" + new Byte(read));
                    match++;
                } else
                if (match > 0) {
                        
                    if (match == 3)	{
                        // should match /r/n. or line starting with a period
                        //  Continue w/o updating the buffer
                        match = 0;
                        continue;
                    }
                        
                    // Lets reset the match count
                    match = 0;

                    // See if we match the first character now??
                    if (read == messageEnd.charAt(0)) {
                        match++;
                    }
                }

                if ( TempMessagePos >= TempMessageBuffer ) {
                    TempMessagePos++;
                    //logger.log("WRT:" + new Integer(TempMessagePos) +":" + new Byte(buffer));
                    body.write(buffer);
                } else {
                    //logger.log("ARR:" + new Integer(TempMessagePos) +":" + new Byte(buffer));
                    TempMessage[TempMessagePos++] = buffer;
                    if ( TempMessagePos == TempMessageBuffer ) {
                        try {
                            body = this.ProcessMessage(messageID, state, TempMessage);
                        } catch (MessagingException me) {
                            logger.log("Error processing incoming mail : " + me.getMessage());
                        }
                    }
                }
                buffer = read;
            }

            if (TempMessagePos < TempMessageBuffer ) {
                try {
                    body = this.ProcessMessage(messageID, state, TempMessage);
                } catch (MessagingException me) {
                    logger.log("Error processing incoming mail (2) : " + me.getMessage());
                }
            }    

            logger.log("Done with data - flushing");
            body.flush();
            body.close();

            spool.free(messageID);

        } catch (Exception e) {
            logger.log("Exception caught : " + e.getMessage());
            e.printStackTrace();
        }

        p.resetState();
        
        return new CommandHandlerResponse(250, "Message received:" + messageID, CommandHandlerResponse.OK);	
    }
    
    public String[] getHelp(String command) {
        String[] s = {	"DATA",
                                        "    Following text is collected as the message.",
                                        "    End with a single dot." };
        return s; 
    }
    
    public String getExtended(String command) {
        return null; 
    }

    private OutputStream ProcessMessage(String messageID, Hashtable state, byte[] body)
        throws MessagingException {

        MimeMessage msg = null;
        javax.mail.Session session = javax.mail.Session.getDefaultInstance(new Properties(), null);
        session.setDebug(false);
        try {
            msg = new ServerMimeMessage(session, new ByteArrayInputStream(body, 0, body.length));
        } catch ( MessagingException me ) {
            System.out.println("Error reading in mime message from InputStream");
        }
        
        try {
            msg.writeTo(System.out);
        } catch (IOException ioe) {
            logger.log("Error writing MimeMessage to OutputStream", "DATA-COMMAND", Logger.ERROR);
        }


logger.log("t1");

        Vector v = (Vector)state.get(rcpt.RCPT_VECTOR); 
        InternetAddress addresses[] = new InternetAddress[v.size()];
        for (int i = 0; i < v.size(); i++) {
            addresses[i] = new InternetAddress((String) v.elementAt(i));
        }

logger.log("t2");

        String sender = (String) state.get(mail.FROM);

        // Set the Return-Path: if it is not set
        if (msg.getHeader("Return-Path") == null) {
            msg.addHeader("Return-Path", "<" + sender + ">");
        } 

logger.log("t3");

        // Add the Received: header
        String received = "from " + state.get(ProtocolHandler.REMOTE_NAME) + " ([" + state.get(ProtocolHandler.REMOTE_IP) + "])\r\n          by " + state.get(ProtocolHandler.SERVER_NAME) + " (" + state.get(ProtocolHandler.SERVER_TYPE) + ")";
        if (java.lang.reflect.Array.getLength(addresses) == 1) {
            received += "\r\n          for <" + addresses[0].toString() + ">";
        } 
        received += ";\r\n          " + RFC822DateFormat.toString(new Date());

        msg.addHeader("Received", received);

        // Set the Message-ID: if it is not set
        if (msg.getHeader("Message-ID") == null) {
            msg.addHeader("Message-ID", messageID);
        }

        // Set the Date: if it is not set
        if (msg.getHeader("Date") == null) {
            msg.addHeader("Date", RFC822DateFormat.toString(new Date()));
        } 

        // Set the From: if it is not set
        if (msg.getHeader("From") == null) {
            msg.addHeader("From", sender);
        } 

        // Set the To: if it is not set
        // ***** We will remove this later once we properly handle forwarding email message
        if (msg.getHeader("To") == null) {
            msg.setRecipients(Message.RecipientType.TO, addresses);
        } 
        
logger.log("t4");
if (spool==null) {logger.log("t4-");}

        // Get a container for this mail object
        MessageContainer mc = spool.addMessage(messageID, (String)state.get(mail.FROM), (Vector)state.get(rcpt.RCPT_VECTOR) );

logger.log("t41");
        // Get the delivery state for modification
        DeliveryState MsgState = mc.getState();

        // Set delivery state information
        MsgState.setRecipients(addresses);
        MsgState.put("remote.host", (String)state.get(ProtocolHandler.REMOTE_NAME));
        MsgState.put("remote.ip", (String)state.get(ProtocolHandler.REMOTE_IP));

        if ( state.containsKey(ProtocolHandler.NAME_GIVEN) ) {
            MsgState.put("remote.host.given", (String)state.get(ProtocolHandler.NAME_GIVEN));
        } 

logger.log("t42");
        // Set the state back in message container
        mc.setState(MsgState);

        // Save the message container back into spool
        OutputStream stream = spool.store(messageID, mc);

logger.log("t43");
        // Write our modified mail message to the output stream
        try {
            msg.writeTo(stream);
            msg.writeTo(System.out);
        } catch (IOException ioe) {
            logger.log("Error writing MimeMessage to OutputStream", "DATA-COMMAND", Logger.ERROR);
        }

        // Return the output stream so we can complete the mail message
        return stream;

    }
}

