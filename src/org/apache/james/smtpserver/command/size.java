package org.apache.james.smtpserver.command;

import org.apache.james.smtpserver.ProtocolHandler;
import org.apache.java.util.*;
import org.apache.avalon.blocks.*;
import org.apache.java.util.*;

/**
 * Command handler for the SMTP SIZE Command
 *
 * @author Matthew Petteys <matt@arcticmail.com>
 */

public class size
    extends GenericCommand
{

    public String MaxSizeName = "maxsize";
    private Integer MaxSize;
    private Configuration conf;
    private Logger logger;

    public void init(Context context)
        throws Exception
    {
        this.conf = context.getConfiguration();
        this.logger = (Logger) context.getImplementation(Interfaces.LOGGER);

        // Check to see if another response code is specified
        try {
            MaxSize = new Integer(conf.getChild(MaxSizeName).getValueAsInt());
        } catch (NumberFormatException nfe) {
            logger.log("Invalid maximum message specified");
        } catch (Exception ce) {
            logger.log("No maximum message size specified.");
        }

        // If the size wasn't specified - RFC says that size of 0 is unlimited
        if ( MaxSize == null ) {
            MaxSize = new Integer(0);
        }
        
    }

    public CommandHandlerResponse service(String command, ProtocolHandler p)
    {
        return null; // No command specified with SIZE
    }
    
    public String[] getHelp(String command) {	
        return null; 
    }
    
    public String getExtended(String command) {
        return "SIZE " + MaxSize;
    }
}
