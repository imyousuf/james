/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.james.servlet;

import java.io.*;
import java.util.*;
import java.net.*;
import org.apache.arch.*;
import org.apache.james.*;
import org.apache.mail.*;
import org.apache.avalon.blocks.*;
import javax.mail.Transport;
import javax.mail.Session;
import javax.mail.URLName;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.*;


/**
 * Receive  a MessageContainer from JamesSpoolManager and takes care of delivery 
 * the message to remote hosts. If for some reason mail can't be delivered
 * store it in the "delayed" Repository and set an Alarm. After "delayTime" the 
 * Alarm will wake the servlet that will try to send it again. After "maxRetyes"
 * the mail will be considered underiverable and will be returned to sender.
 *
 * Note: Many FIXME on the air.
 *
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 */
public class RemoteDelivery extends GenericAvalonMailServlet implements TimeServer.Bell {

    private ComponentManager comp;
    private MessageContainerRepository delayed;
    private String outgoingmailserver;
    private Transport transport;
    private Transport loopBack;
    private TimeServer timeServer;
    private long delayTime;
    private int maxRetyes;

    public void init () 
    throws Exception {
        comp = getComponentManager();
        String delayedRepository = getConfiguration("repository").getValue();
        delayTime = getConfiguration("delayTime", "21600000").getValueAsLong(); // default is 6*60*60*1000 mills
        maxRetyes = getConfiguration("maxRetyes", "5").getValueAsInt(); // default is 5 retryes
        Store store = (Store) comp.getComponent(Interfaces.STORE);
        delayed = (MessageContainerRepository) store.getPrivateRepository(delayedRepository, MessageContainerRepository.MESSAGE_CONTAINER, Store.ASYNCHRONOUS);
        timeServer = (TimeServer) comp.getComponent(Interfaces.TIME_SERVER);
        int i = 0;
        for (Enumeration e = delayed.list(); e.hasMoreElements(); ) {
            String key = (String) e.nextElement();
            timeServer.setAlarm(key, this, ++i * 10000);
            log("delayed message " + key + " set for delivery in " + (i * 10) + " seconds");
        }
        outgoingmailserver = getConfiguration("outgoingmailserver").getValue();
        URLName urlname = new URLName("smtp://" + outgoingmailserver);
        transport = Session.getDefaultInstance(System.getProperties(), null).getTransport(urlname);
        urlname = new URLName("smtp://localhost");
        loopBack = Session.getDefaultInstance(System.getProperties(), null).getTransport(urlname);
    }
    
    public MessageContainer service(MessageContainer mc) {
        
// FIXME: need implementation of DNS.
        log("Remotly delivering mail " + mc.getMessageId());
        Vector recipients = mc.getRecipients();
        try {
            InternetAddress addr[] = new InternetAddress[recipients.size()];
            int i = 0;
            for (Enumeration e = recipients.elements(); e.hasMoreElements(); i++) {
                addr[i] = new InternetAddress((String) e.nextElement());
            }
            transport.connect();
            transport.sendMessage(mc.getMessage(), addr);
            transport.close();
        } catch (Exception ex) {
// FIXME: Need to return only undeliverable recipients.
            log("Exception delivering mail: " + ex.getMessage());
            if (!mc.getState().equals(MessageContainer.ERROR)) {
                mc.setState(MessageContainer.ERROR);
                mc.setErrorMessage("1");
            }
            int retryes = Integer.parseInt(mc.getErrorMessage());
            if (retryes > maxRetyes) {
                log("Sending back message " + mc.getMessageId() + " after " + retryes + " retyes");
                try {
                    MimeMessage reply = (MimeMessage) (mc.getMessage()).reply(false);
                    reply.setText("Unable to deliver this message to recipients: " + ex.getMessage());
                    InternetAddress addr[] = {new InternetAddress(mc.getSender())};
                    reply.setRecipients(Message.RecipientType.TO, addr);
                    reply.setFrom(new InternetAddress("JAMES@maggie"));
// FIXME: Should we use "transport" or "loopBack" ?
                    loopBack.connect();
                    loopBack.sendMessage(reply, addr);
                    loopBack.close();
                } catch (Exception ignore) {
// FIXME: cannot destroy mails... what should we do here ?
                    log("Unable to reply. Destroying message");
                }
            } else {
                log("Storing message " + mc.getMessageId() + " into delayed after " + retryes + " retyes");
                mc.setErrorMessage("" + ++retryes);
                delayed.store(mc.getMessageId(), mc);
                timeServer.setAlarm(mc.getMessageId(), this, delayTime);
            }
        }
        return (MessageContainer) null;
    }

    public void wake(String name, String memo) {
        MessageContainer mc = delayed.retrieve(name);
        delayed.remove(name);
        service(mc);
    }
    
    public void destroy() {
    }

    public String getServletInfo() {
        return "RemoteDelivery Mail Servlet";
    }
}
    
