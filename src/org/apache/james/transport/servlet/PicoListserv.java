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
import org.apache.arch.*;
import org.apache.james.*;
import org.apache.james.usermanager.*;
import org.apache.avalon.blocks.*;
import org.apache.mail.*;
import org.apache.james.transport.*;

/**
 * <listName>
 * <subject>
 * <membersonly>
 * <attachmentsallowed>
 * <replytolist>
 */
public class PicoListserv extends GenericListserv {
    protected boolean membersOnly = false;
    protected boolean attachmentsAllowed = true;
    protected boolean replyToList = true;
    protected String subject = null;
    protected String listName = null;
    private UsersRepository members;

    public void init () {
        listName = getConfiguration ("listName").getValue();
        subject = getConfiguration ("subject").getValue();
        membersOnly = getConfiguration("membersonly", "false").getValueAsBoolean();
        attachmentsAllowed = getConfiguration("attachmentsallowed", "true").getValueAsBoolean ();
        replyToList = getConfiguration("replytolist", "true").getValueAsBoolean ();
        UserManager manager = (UserManager) getComponentManager().getComponent(Resources.USERS_MANAGER);
        members = (UsersRepository) manager.getUserRepository("list-" + listName);
    }

    protected Vector getMembers() {
        Vector reply = new Vector();
        for (Enumeration e = members.list(); e.hasMoreElements(); ) {
            reply.addElement(e.nextElement());
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

    protected String getSubject() {
        return subject;
    }

    protected String getListservAddress() {
        Vector serverNames = (Vector) getContext().get(Resources.SERVER_NAMES);
        return listName + "@" + serverNames.elementAt(0);
    }

    public String getServletInfo() {
        return "PicoListserv Servlet";
    }
}

