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
 * the message to local inboxs. Once the message has been delivered the 
 * recipient MUST be removed from the MessageContainer.
 * @version 1.0.0, 24/04/1999
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 */
public class LocalAgent implements Configurable, Composer {

    private SimpleComponentManager comp;
    private Configuration conf;
    private Logger logger;
    private Store.ObjectRepository mailUsers;
    private Store store;
    private Vector localUsers;
    private Vector localHost;

    public void setConfiguration(Configuration conf) {
        this.conf = conf;
    }
    
    public void setComponentManager(ComponentManager comp) {
        this.comp = (SimpleComponentManager) comp;
        this.logger = (Logger) comp.getComponent(Interfaces.LOGGER);
        this.mailUsers = (Store.ObjectRepository) comp.getComponent("mailUsers");
        this.store = (Store) comp.getComponent(Interfaces.STORE);
        this.localUsers = new Vector();
        this.localHost = new Vector();
        for (Enumeration e = mailUsers.list(); e.hasMoreElements(); ) {
            String user = (String) e.nextElement(); 
            localUsers.addElement(user.toUpperCase());
        }
        for (Enumeration e = conf.getConfigurations("localhost"); e.hasMoreElements(); ) {
            String host = ((Configuration) e.nextElement()).getValue();
            localHost.addElement(host.toUpperCase());
        }
    }
    
    public boolean isLocal(String recipient) {
        int sep = recipient.indexOf('@');
        if (sep == -1) return false;
        String host = recipient.substring(sep + 1);
        String user = recipient.substring(0, sep);
        if (localHost.contains(host) && localUsers.contains(user)) return true;
        else return false;
    }
    
    private String getUser(String recipient) {
        int sep = recipient.indexOf('@');
        if (sep == -1) return "";
        return recipient.substring(0, sep);
    }
    
    public void delivery(MessageContainer mc) {
        DeliveryState state = mc.getState();
        Vector recipients = state.getRecipients();
        for (Enumeration e = recipients.elements(); e.hasMoreElements(); ) {
            String recipient = (String) e.nextElement();
            if (isLocal(recipient)) {
                logger.log("Local delivery to " + recipient, "SMTPServer", logger.INFO);
                getUserMailbox(getUser(recipient)).store(mc.getMessageId(), mc);
                recipients.removeElementAt(recipients.indexOf(recipient));
            }
        }
        state.setRecipients(recipients);
    }
    
    private Store.MessageContainerRepository getUserMailbox(String userName) {

        Store.MessageContainerRepository userInbox = (Store.MessageContainerRepository) null;
        String repositoryName = "localInbox." + userName;
        try {
            userInbox = (Store.MessageContainerRepository) comp.getComponent(repositoryName);
        } catch (ComponentNotFoundException ex) {
            userInbox = (Store.MessageContainerRepository) store.getPublicRepository(repositoryName);
            comp.put(repositoryName, userInbox);
        }
        return userInbox;
    }
}
    
