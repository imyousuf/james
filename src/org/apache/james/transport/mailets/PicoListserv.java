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
import org.apache.java.lang.*;
import org.apache.james.*;
import org.apache.james.usermanager.*;
import org.apache.avalon.interfaces.*;
import org.apache.mail.*;
import org.apache.james.transport.*;

/**
 * <listName>
 * <membersonly>
 * <attachmentsallowed>
 * <replytolist>
 */
public class PicoListserv extends AbstractMailet {

    protected boolean membersOnly = false;
    protected boolean attachmentsAllowed = true;
    protected boolean replyToList = true;
    protected String listName = null;
    private UsersRepository members;
    private Logger logger;
    private Mailet transport;

    public void init () {
        MailetContext context = getContext();
        ComponentManager comp = context.getComponentManager();
        logger = (Logger) comp.getComponent(Interfaces.LOGGER);
        Configuration conf = context.getConfiguration();
        listName = conf.getConfiguration ("listName").getValue();
        membersOnly = conf.getConfiguration("membersonly", "false").getValueAsBoolean();
        attachmentsAllowed = conf.getConfiguration("attachmentsallowed", "true").getValueAsBoolean ();
        replyToList = conf.getConfiguration("replytolist", "true").getValueAsBoolean ();
        UserManager manager = (UserManager) comp.getComponent(Resources.USERS_MANAGER);
        members = (UsersRepository) manager.getUserRepository("list-" + listName);
        transport = (Mailet) context.get("transport");
    }

    protected Collection getMembers() {
        Collection reply = new Vector();
        for (Enumeration e = members.list(); e.hasMoreElements(); ) {
            reply.add(e.nextElement());
        }
        return reply;
    }

    protected boolean isMembersOnly() {
        return membersOnly;
    }

    protected boolean isAttachmentsAllowed() {
        return attachmentsAllowed;
    }

    protected boolean isReplyToList() {
        return replyToList;
    }

    protected String getListservAddress() {
        Collection serverNames = (Collection) getContext().get(Resources.SERVER_NAMES);
        return listName + "@" + serverNames.iterator().next();
    }

    public String getMailetInfo() {
        return "PicoListserv Mailet";
    }

    public void service(Mail mail) throws Exception {
        mail.setRecipients(getMembers());
        transport.service(mail);
        mail.setState(Mail.GHOST);
    }
}

