/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.james;

import java.io.*;
import java.util.*;
import java.net.*;
import org.apache.arch.*;
import org.apache.james.*;
import org.apache.avalon.blocks.*;
import javax.mail.*;
import javax.mail.internet.*;

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
    private Vector localHost;
    private String deliverserver;

    public void setConfiguration(Configuration conf) {
        this.conf = conf;
        deliverserver = conf.getConfiguration("deliverserver").getValue();
    }
    
    public void setComponentManager(ComponentManager comp) {
        this.comp = comp;
        this.logger = (Logger) comp.getComponent(Interfaces.LOGGER);
        this.localHost = new Vector();
        for (Enumeration e = conf.getConfigurations("localhost"); e.hasMoreElements(); ) {
            String host = ((Configuration) e.nextElement()).getValue();
            localHost.addElement(host);
        }
    }
    
    public void delivery(MessageContainer mc) {
        
// FIXME: need implementation of DNS.
        Vector recipients = mc.getRecipients();
        Vector rRecipients = new Vector();
        for (int i = 0  ; i < recipients.size(); i++) {
            String recipient = (String) recipients.elementAt(i);
            if (isRemote(recipient)) {
                rRecipients.addElement(recipient);
                recipients.removeElementAt(i);
            }
        }
        if (!rRecipients.isEmpty()) {
            logger.log("Remote delivery of " + mc.getMessageId(), "JAMES", logger.INFO);
            try {
                InternetAddress addr[] = new InternetAddress[rRecipients.size()];
                int i = 0;
                for (Enumeration e = rRecipients.elements(); e.hasMoreElements(); i++) {
                    addr[i] = new InternetAddress((String) e.nextElement());
                }
                URLName urlname = new URLName("smtp://" + deliverserver);
                Transport t = Session.getDefaultInstance(System.getProperties(), null).getTransport(urlname);
                t.connect();
                t.sendMessage(mc.getMessage(), addr);
                t.close();
            } catch (Exception ex) {
                logger.log("Exception delivering mail: " + ex.getMessage(), "JAMES", logger.ERROR);
            }
        }
        mc.setRecipients(recipients);
    }

    public boolean isRemote(String recipient) {
        String host = getHost(recipient);
        return (!localHost.contains(host));
    }

    private String getUser(String recipient) {
        int sep = recipient.indexOf('@');
        if (sep == -1) return "";
        return recipient.substring(0, sep);
    }
    
    private String getHost(String recipient) {
        int sep = recipient.indexOf('@');
        if (sep == -1) return "";
        return recipient.substring(sep + 1);
    }
}
    
