/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.transport.mailets;

import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;
import org.apache.avalon.*;
import org.apache.james.*;
import org.apache.james.usermanager.*;
import org.apache.avalon.blocks.*;
import org.apache.mailet.*;
import org.apache.james.transport.*;

/**
 * Routes a given email address to a list of members, based on simple
 * listserv settings.
 *
 * Sample configuration:
 * <pre>
 * <mailet match="RecipientIs=test@glissando.lokitech.com" class="MiniListserv">
 *     <membersonly>true</membersonly>
 *     <attachmentsallowed>false</attachmentsallowed>
 *     <replytolist>true</replytolist>
 *     <member>sergek@lokitech.com</member>
 *     <member>sknystautas@yahoo.com</member>
 * </mailet>
 * </pre>
 * @author  Serge Knystautas <sergek@lokitech.com>
 */
public class PicoListManager extends GenericMailet {

    private String listName;
    private UsersRepository members;
    private String subscribe;
    private String unsubscribe;

    public void init () {
        listName = getInitParameter("listName");
        subscribe = getInitParameter("subscribe");
        if (subscribe == null) {
            subscribe = listName + "-on";
        }
        unsubscribe = getInitParameter("unsubscribe");
        if (unsubscribe == null) {
            unsubscribe = listName + "-off";
        }

        ComponentManager comp = (ComponentManager)getMailetContext().getAttribute(Constants.AVALON_COMPONENT_MANAGER);
        UserManager manager = (UserManager) comp.getComponent(Resources.USERS_MANAGER);
        members = (UsersRepository) manager.getUserRepository("list-" + listName);
        //transport = (Mailet) context.get("transport");
        //log("PicoListserv: " + listName + " sub " + subscribe + " unsub " + unsubscribe + "-" + transport);
    }

    public void service(Mail mail) throws MailetException, MessagingException {
        Collection recipients = mail.getRecipients();
        MailAddress recipient = (MailAddress) recipients.iterator().next();
        if (recipients.size() != 1) {
            log("bouncing back: Cannot handle more than one recipient");
            getMailetContext().bounce(mail, "Cannot handle more than one recipient");
            mail.setState(Mail.GHOST);
            return;
        }

        if (recipient.getUser().equalsIgnoreCase(subscribe)) {
            MailAddress sender = mail.getSender();
            members.addUser(sender.toString(), "");
            log("User " + sender + " subscribed to list " + listName);
            getMailetContext().bounce(mail, "You have been subscribed to this list");
            mail.setState(Mail.GHOST);
            return;
        }
        if (recipient.getUser().equalsIgnoreCase(unsubscribe)) {
            MailAddress sender = mail.getSender();
            members.removeUser(sender.toString());
            log("User " + sender + " unsubscribed to list " + listName);
            getMailetContext().bounce(mail, "You have been unsubscribed to this list");
            mail.setState(Mail.GHOST);
            return;
        }
        log("Return untouched mail. The address " + recipient + " cannot be handled.");
    }

    public String getMailetInfo() {
        return "PicoListManager Mailet";
    }
}

