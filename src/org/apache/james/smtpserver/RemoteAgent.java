/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.smtpserver;

import java.io.*;
import java.util.*;
import org.apache.arch.*;
import org.apache.james.*;
import org.apache.avalon.blocks.*;

/**
 * Receive  a MessageContainer from JamesSpoolManager and takes care of delivery 
 * the message to remote hosts. Once the message has been delivered the 
 * recipient MUST be removed from the MessageContainer.
 * YET TO BE IMPLEMENTED!
 * @version 1.0.0, 24/04/1999
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 */
public class RemoteAgent implements Configurable, Composer {

    private ComponentManager comp;
    private Configuration conf;
    private Logger logger;

    public void setConfiguration(Configuration conf) {
        this.conf = conf;
    }
    
    public void setComponentManager(ComponentManager comp) {
        this.comp = comp;
        this.logger = (Logger) comp.getComponent(Interfaces.LOGGER);
    }
    
    public void delivery(MessageContainer mc) {
        
// FIXME: need implementation of remote delivery.
        Vector recipients = mc.getState().getRecipients();
        for (Enumeration e = recipients.elements(); e.hasMoreElements(); ) {
            String recipient = (String) e.nextElement();
            if (isRemote(recipient)) {
                logger.log("Remote delivery=" +recipient, "SMTPServer", logger.INFO);
                recipients.removeElementAt(recipients.indexOf(recipient));
            }
        }
        mc.getState().setRecipients(recipients);
    }

    public boolean isRemote(String recipient) {
        return true;
    }
}
    
