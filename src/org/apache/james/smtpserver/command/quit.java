package org.apache.james.smtpserver.command;

import org.apache.james.smtpserver.ProtocolHandler;
import org.apache.java.util.*;
import java.util.*;
import org.apache.avalon.blocks.*;

/**
 * Command handler for the SMTP QUIT Command
 *
 * @author Serge Knystautas <sergek@lokitech.com>
 * @author Matthew Petteys <matt@arcticmail.com>
 */

public class quit
    extends GenericCommand
{

    private Logger logger;

    public void init(Context context)
        throws Exception
    {
        this.logger = (Logger) context.getImplementation(Interfaces.LOGGER);
    }

    public CommandHandlerResponse service(String command, ProtocolHandler p)
    {
        Hashtable state = p.getState();
        if (command.trim().toUpperCase().startsWith("QUIT")) {
            CommandHandlerResponse chr = new CommandHandlerResponse(221, (String)state.get(p.SERVER_NAME) + " Service closing transmission channel");
            chr.setExitStatus(CommandHandlerResponse.EXIT);
            return chr;
        }
        return null;
    }
    
    public String[] getHelp(String command) {	
        String[] s = {	"QUIT",
                                        "    Exit." };
        return s; 
    }
    
    public String getExtended(String command) {
        return null; 
    }

}
