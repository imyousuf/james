package org.apache.james.server.command.smtp;

import org.apache.james.server.*;
import org.apache.james.*;
import java.util.*;
import java.util.Vector;
import javax.mail.internet.*;
import javax.activation.*; // Required for javax.mail
import org.apache.james.server.protocol.ProtocolHandler;
import org.apache.james.server.command.*;

/**
 * Command handler for the SMTP RCPT Command
 *
 * @author Serge Knystautas <sergek@lokitech.com>
 * @author Matthew Petteys <matt@arcticmail.com>
 */

public class rcpt
	extends GenericCommand
{

	public static String RCPT_VECTOR = "RCPT";

	public CommandHandlerResponse service(String command, ProtocolHandler p)
	{

		if (!command.toUpperCase().startsWith("RCPT TO:")) {
			return new CommandHandlerResponse(501, "Usage: RCPT TO:<recipient>");
    }

		Hashtable state = p.getState();

		if ( ! state. containsKey(mail.FROM) ) {
			return new CommandHandlerResponse(503, "Need MAIL before RCPT");
		}
		
    String rcpt = command.substring(command.indexOf(":")+1).trim();
    InternetAddress recipient = null;

		try {
			recipient = new InternetAddress( utils.convertAddress(rcpt) );
		} catch (ClassCastException cce) {
			return new CommandHandlerResponse(551, "Invalid Address format " + rcpt);			
    } catch (AddressException ae) {
			return new CommandHandlerResponse(551, ae.getMessage());
    }

		Vector recipients = null;
		
		if ( ! state. containsKey(RCPT_VECTOR) ) {
			recipients = new Vector(1);
		} else {
			recipients = (Vector) state.get(RCPT_VECTOR);
		}
		
		recipients.addElement(recipient);
		state.put(RCPT_VECTOR, recipients);
		
		p.setState(state);
		
		return new CommandHandlerResponse(250, "Recipient <" + recipient + "> OK", CommandHandlerResponse.OK);

  }
	
	public String[] getHelp(String command) {	
		String[] s = {	"RCPT TO: <recipient>", // [ <parameters> ]",
										"Specifies the recipient.  Can be used any number of times." }; //,
										//"Parameters are ESMTP extensions.  See \"HELP DSN\" for details." };	
		return s; 
	}
	
	public String getExtended(String command) {
		return null; 
	}

}

