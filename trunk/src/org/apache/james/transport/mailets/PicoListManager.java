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
import org.apache.mail.*;
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
public class PicoListManager extends AbstractMailet {

    private String listName;
    private UsersRepository members;
    private String subscribe;
    private String unsubscribe;
    private Logger logger;
    private Mailet transport;

    public void init () {
        MailetContext context = getContext();
        ComponentManager comp = context.getComponentManager();
        logger = (Logger) comp.getComponent(Interfaces.LOGGER);
        Configuration conf = context.getConfiguration();
        listName = conf.getConfiguration("listName").getValue();
        subscribe = conf.getConfiguration("subscribe").getValue(listName + "-on");
        unsubscribe = conf.getConfiguration("unsubscribe").getValue(listName + "-off");
        UserManager manager = (UserManager) comp.getComponent(Resources.USERS_MANAGER);
        members = (UsersRepository) manager.getUserRepository("list-" + listName);
        transport = (Mailet) context.get("transport");
        logger.log("PicoListserv: " + listName + " sub " + subscribe + " unsub " + unsubscribe + "-" + transport);
    }

    public void service(Mail mail) throws Exception {
        Collection recipients = mail.getRecipients();
        String recipient = (String) recipients.iterator().next();
        if (recipients.size() != 1) {
            logger.log("bouncing back: Cannot handle more than one recipient");
            transport.service(mail.bounce("Cannot handle more than one recipient"));
            mail.setState(Mail.GHOST);
            return;
        }

        if (Mail.getUser(recipient).equalsIgnoreCase(subscribe)) {
            String sender = mail.getSender();
            members.addUser(sender, "");
            logger.log("User " + sender + " subscribed to list " + listName);
            transport.service(mail.bounce("You have been subscribed to this list"));
            mail.setState(Mail.GHOST);
            return;
        }
        if (Mail.getUser(recipient).equalsIgnoreCase(unsubscribe)) {
            String sender = mail.getSender();
            members.removeUser(sender);
            logger.log("User " + sender + " unsubscribed to list " + listName);
            transport.service(mail.bounce("You have been unsubscribed to this list"));
            mail.setState(Mail.GHOST);
            return;
        }
        logger.log("Return untouched mail. The address " + recipient + " cannot be handled.");
    }

    public String getMailetInfo() {
        return "PicoListManager Mailet";
    }
}

