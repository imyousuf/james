/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.transport.servlet;

import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;
import org.apache.java.lang.*;
import org.apache.james.*;
import org.apache.james.usermanager.*;
import org.apache.avalon.interfaces.*;
import org.apache.mail.*;
import org.apache.james.transport.*;

/**
 * Routes a given email address to a list of members, based on simple
 * listserv settings.
 *
 * Sample configuration in avalon.conf.xml:
 * <pre>
 * <servlet match="RecipientIs=test@glissando.lokitech.com" class="MiniListserv">
 *     <listservaddress>test@glissando.lokitech.com</listservaddress> <!--this will likely be removed shortly-->
 *     <subject>Test</subject>
 *     <membersonly>true</membersonly>
 *     <attachmentsallowed>false</attachmentsallowed>
 *     <replytolist>true</replytolist>
 *     <member>sergek@lokitech.com</member>
 *     <member>sknystautas@yahoo.com</member>
 * </servlet>
 * </pre>
 * @author  Serge Knystautas <sergek@lokitech.com>
 */
public class PicoListManager extends GenericMailServlet {

    private String listName;
    private UsersRepository members;
    private String subscribe;
    private String unsubscribe;

    public void init () {
        listName = getConfiguration("listName").getValue();
        subscribe = getConfiguration("subscribe", listName + "-on").getValue();
        unsubscribe = getConfiguration("unsubscribe", listName + "-off").getValue();
        UserManager manager = (UserManager) getComponentManager().getComponent(Resources.USERS_MANAGER);
        members = (UsersRepository) manager.getUserRepository("list-" + listName);
        log("PicoListserv: " + listName + " sub " + subscribe + " unsub " + unsubscribe);
    }

    public Mail service(Mail mail) {
        try {
            Vector recipients = mail.getRecipients();
            String recipient = (String) recipients.elementAt(0);
            if (recipients.size() != 1) {
                return mail.bounce("Cannot handle more than one recipient");
            }
            
            if (Mail.getUser(recipient).equalsIgnoreCase(subscribe)) {
                String sender = mail.getSender();
                members.addUser(sender, "");
                log("User " + sender + " subscribed to list " + listName);
                return mail.bounce("You have been subscribed to this list");
            } 
            if (Mail.getUser(recipient).equalsIgnoreCase(unsubscribe)) {
                String sender = mail.getSender();
                members.removeUser(sender);
                log("User " + sender + " unsubscribed to list " + listName);
                return mail.bounce("You have been unsubscribed to this list");
            }
            log("Return untouched mail. The address " + recipient + " cannot be handled.");
        } catch (MessagingException me) {
            log("Exception while retrieving message " + mail.getName ()+ ": " + me.getMessage());
        }
        return mail;
    }

    public String getServletInfo() {
        return "PicoListManager Servlet";
    }
}

