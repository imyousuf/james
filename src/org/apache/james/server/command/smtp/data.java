package org.apache.james.server.command.smtp;

import org.apache.james.server.*;
import org.apache.james.*;
import org.apache.james.util.*;
import java.util.*;
import java.util.Vector;
import javax.mail.internet.*;
import javax.mail.*;
import javax.activation.*; // Required for javax.mail
import java.io.*;
import org.apache.james.server.protocol.ProtocolHandler;
import org.apache.james.server.command.*;

/**
 * Command handler for the SMTP DATA Command
 *
 * @author Serge Knystautas <sergek@lokitech.com>
 * @author Matthew Petteys <matt@arcticmail.com>
 */

public class data
	extends GenericCommand
{
	private final static String messageEnd = "\r\n.\r\n";

	private PrintWriter out;
	private BufferedReader in;
	private MailServletContext context;
	private Hashtable state;
	
	public void init(JamesServletConfig config)
	{
		context = config.getContext();
	}

	public CommandHandlerResponse service(String command, ProtocolHandler p)
	{
		
		context.log("Going in for the kill...-1");

		state = p.getState();

    if ( ! state.containsKey(mail.FROM) ) {
			return new CommandHandlerResponse(503, "No sender specified");
		}
		
		if ( ! state.containsKey(rcpt.RCPT_VECTOR) ) {
			return new CommandHandlerResponse(503, "No recipients specified");
		}

		String messageID;
    try {

			PrintWriter out = p.getOut();
			BufferedReader in = p.getIn();

    	(new CommandHandlerResponse(354, "Ok Send data ending with <CRLF>.<CRLF>")).printResponse(out);
    
			context.log("Going in for the kill...");

    	MimeMessage msg = null;
   		javax.mail.Session session = javax.mail.Session.getDefaultInstance(new Properties(), null);
			session.setDebug(true);
			messageID = context.getMessageID();

			// Now we need to read in all the data into a byte array, then we
			// send that into the MimeMessage constructor

      // Read in 1 character at a time
      ByteArrayOutputStream byteOut = new ByteArrayOutputStream(4096);

			/* Supporting RFC 821
			
			4.5.2.  TRANSPARENCY
			
						2. When a line of mail text is received by the receiver-SMTP
            it checks the line.  If the line is composed of a single
            period it is the end of mail.  If the first character is a
            period and there are other characters on the line, the first
            character is deleted.		
			*/
			
			// Buffering to check for lines that start w/ .
			int match = 1; // Mark first byte matched for initial buffer read
			byte buffer = (byte) in.read(); // initialize buffer var
			
      while (match < messageEnd.length()) {
				byte read = (byte) in.read();
				
				if (read == messageEnd.charAt(match)) {
					//context.log("Match:" + new Integer(match) +":" + new Byte(read));
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
				
        byteOut.write(buffer);				
				buffer = read;
     	}
      // We now have the whole message in a byte array - 3

      byte mba[] = byteOut.toByteArray();
			ByteArrayInputStream byteIn = new ByteArrayInputStream(mba, 0, mba.length - (messageEnd.length()-1));
			
			//context.log(byteIn.toString());
			//InternetAddress addresses[] = (InternetAddress[])((Vector)state.get(rcpt.RCPT_VECTOR)).toArray();
			Vector v = (Vector)state.get(rcpt.RCPT_VECTOR); 
			context.log(v.toString());
			int size = v.size();
			InternetAddress addresses[] = new InternetAddress[size];
			for (int i = 0; i < size; i++) {
      	InternetAddress ia = (InternetAddress) v.elementAt(i);
				addresses[i] = ia;
			}
			
			InternetAddress sender = (InternetAddress) state.get(mail.FROM);
			
     	// We use our custom child of MimeMessage, to better handle some server side aspects of messages
			msg = new ServerMimeMessage(session, byteIn);

			// Set the Return-Path: if it is not set
      if (msg.getHeader("Return-Path") == null) {
       	msg.addHeader("Return-Path", "<" + sender.getAddress() + ">");
      } 

      // Add the Received: header
      String received = "from " + state.get(p.REMOTE_NAME) + " ([" + state.get(p.REMOTE_IP) + "])\r\n          by " + context.getProperty("server.name") + " (" + context.getProperty("software.name") + " v" + context.getProperty("software.version") + ") with SMTP ID 0"; //TODO+ mid;
      if (java.lang.reflect.Array.getLength(addresses) == 1) {
      	received += "\r\n          for <" + addresses[0].toString() + ">";
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
      	msg.setRecipients(Message.RecipientType.TO, addresses);
      } 

			if (msg != null) {
				// System.out.println ("\n This is the message:");
        // msg.writeTo (System.out);
				
				DeliveryState MsgState = new DeliveryState();

				MsgState.setRecipients(addresses);
        MsgState.put("remote.host", (String)state.get(p.REMOTE_NAME));
        MsgState.put("remote.ip", (String)state.get(p.REMOTE_IP));

        if ( state.containsKey(p.NAME_GIVEN) ) {
        	MsgState.put("remote.host.given", (String)state.get(p.NAME_GIVEN));
        } 

        context.sendMessage(msg, MsgState);
      }
			
			} catch (NullPointerException npe) {
				return new CommandHandlerResponse(554, "Error processing message (npe)");
			} catch (ClassCastException cce) {
				cce.printStackTrace(System.out);
				return new CommandHandlerResponse(554, "Error processing message (cce)");
     	} catch (IOException ioe) {
       	//context.log("Exception @ " + new Date());
				ioe.printStackTrace(System.out);
				return new CommandHandlerResponse(554, "Error processing message (io)");
     	} catch (MessagingException me) {
       	//context.log("Exception @ " + new Date());
				return new CommandHandlerResponse(554, "Error processing message (me)");
      	//context.log("Exception @ " + new Date());
      	//me.printStackTrace(System.out);
    	}

		// NEED TO RESET
    //sender = null;
    //recipients = new Vector();

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
}

