package org.apache.james.smtpserver.command;

import org.apache.james.smtpserver.*;
import org.apache.james.*;
import java.util.*;
import java.util.Vector;
import javax.mail.internet.*;
import javax.activation.*; // Required for javax.mail
import org.apache.java.util.*;

/**
 * Command handler for the SMTP MAIL Command
 *
 * @author Serge Knystautas <sergek@lokitech.com>
 * @author Matthew Petteys <matt@arcticmail.com>
 */

public class mail
    extends GenericCommand
{

    public static String FROM = "FROM_ADDRESS";

    public CommandHandlerResponse service(String command, ProtocolHandler p)
    {

        Hashtable state = p.getState();
        
        if ( state.containsKey(FROM) ) {
            return new CommandHandlerResponse(503, "Sender already specified");
        }

    if ( ! command.toUpperCase().startsWith("MAIL FROM:") ) {
            return new CommandHandlerResponse(501, "Usage: MAIL FROM:<sender>");
    }

    String send = command.substring(command.indexOf(":")+1).trim();
        
        InternetAddress sender = null;
        try {
            sender = new InternetAddress( utils.convertAddress(send) );
    } catch (AddressException ae) {
            return new CommandHandlerResponse(551, ae.getMessage());
    }

        state.put(FROM, sender.toString());		
        p.setState(state);
        
        return new CommandHandlerResponse(250, "Sender <" + sender + "> OK", CommandHandlerResponse.OK);

  }
    
    public String[] getHelp(String command) {	
        String[] s = {	"MAIL FROM: <sender>", // [ <parameters> ]",
                                        "Specifies the sender." }; //,
                                        //"Parameters are ESMTP extensions.  See \"HELP DSN\" for details." };	
        return s; 
    }
    
    public String getExtended(String command) {
        return null; 
    }
    
}

