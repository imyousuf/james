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
//import org.apache.avalon.blocks.*;
import org.apache.mailet.*;
import org.apache.james.transport.*;

/**
 * <listName>
 * <membersonly>
 * <attachmentsallowed>
 * <replytolist>
 */
public class PicoListserv extends GenericMailet {

    protected boolean membersOnly = false;
    protected boolean attachmentsAllowed = true;
    protected boolean replyToList = true;
    protected String listName = null;
    private UsersRepository members;

    public void init() {
        listName = getInitParameter("listName");
        try {
            membersOnly = new Boolean(getInitParameter("membersonly")).booleanValue();
        } catch (Exception e) {
        }
        try {
            attachmentsAllowed = new Boolean(getInitParameter("attachmentsallowed")).booleanValue();
        } catch (Exception e) {
        }
        try {
            replyToList = new Boolean(getInitParameter("replytolist")).booleanValue();
        } catch (Exception e) {
        }

        ComponentManager comp = (ComponentManager)getMailetContext().getAttribute(Constants.AVALON_COMPONENT_MANAGER);
        UserManager manager = (UserManager) comp.getComponent(Resources.USERS_MANAGER);
        members = (UsersRepository) manager.getUserRepository("list-" + listName);
    }

    protected Collection getMembers() throws MessagingException {
        Collection reply = new Vector();
        for (Enumeration e = members.list(); e.hasMoreElements(); ) {
            reply.add(new MailAddress(e.nextElement().toString()));
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
        Collection serverNames = (Collection) getMailetContext().getServerNames();
        return listName + "@" + serverNames.iterator().next();
    }

    public void service(Mail mail) throws MailetException, MessagingException {
        getMailetContext().sendMail(mail.getSender(), getMembers(), mail.getMessage());
        mail.setState(Mail.GHOST);
    }

    public String getMailetInfo() {
        return "PicoListserv Mailet";
    }
}
