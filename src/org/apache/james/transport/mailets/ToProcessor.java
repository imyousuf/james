/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.transport.mailets;

import org.apache.java.lang.*;
import org.apache.james.*;
import org.apache.mail.*;
import org.apache.avalon.interfaces.*;
import org.apache.james.transport.*;

/**
 * Receive  a MessageContainer from JamesSpoolManager and takes care of delivery
 * the message to remote hosts. If for some reason mail can't be delivered
 * store it in the "delayed" Repository and set an Alarm. After "delayTime" the
 * Alarm will wake the mailet that will try to send it again. After "maxRetries"
 * the mail will be considered underiverable and will be returned to sender.
 *
 * Note: Many FIXME on the air.
 *
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 */
public class ToProcessor extends AbstractMailet {

    private Logger logger;
    private Mailet processor;

    public void init () throws Exception {
        MailetContext context = getContext();
        ComponentManager comp = context.getComponentManager();
        logger = (Logger) comp.getComponent(Interfaces.LOGGER);
        Configuration conf = context.getConfiguration();
        String proc = conf.getConfiguration("processor").getValue();
        processor = (Mailet) getContext().get(proc);
    }

    public void service(Mail mail) throws Exception {
        logger.log("Sending mail " + mail.getName() + " to " + processor, "Mailets", Logger.INFO);
        processor.service(mail);
    }


    public String getMailetInfo() {
        return "ToProcessor Mailet";
    }
}
