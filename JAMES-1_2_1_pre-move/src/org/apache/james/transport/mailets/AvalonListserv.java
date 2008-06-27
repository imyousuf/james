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
import org.apache.james.transport.*;
import org.apache.james.userrepository.*;
import org.apache.mailet.*;

/**
 * <membersPath>
 * <membersonly>
 * <attachmentsallowed>
 * <replytolist>
 * <subjectprefix>
 */
public class AvalonListserv extends GenericListserv {

    protected boolean membersOnly = false;
    protected boolean attachmentsAllowed = true;
    protected boolean replyToList = true;
    protected String subjectPrefix = null;
    private UsersRepository members;

    public void init() {
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
        subjectPrefix = getInitParameter("subjectprefix");

        ComponentManager comp = (ComponentManager)getMailetContext().getAttribute(Constants.AVALON_COMPONENT_MANAGER);
        org.apache.avalon.blocks.Store store = (org.apache.avalon.blocks.Store) comp.getComponent(org.apache.avalon.blocks.Interfaces.STORE);
        String membersPath = getInitParameter("membersPath");
        members = (UsersRepository) store.getPrivateRepository(membersPath, UsersRepository.USER, org.apache.avalon.blocks.Store.ASYNCHRONOUS);
    }

    public Collection getMembers() throws ParseException {
        Collection reply = new Vector();
        for (Enumeration e = members.list(); e.hasMoreElements(); ) {
            reply.add(new MailAddress(e.nextElement().toString()));
        }
        return reply;
    }

    public boolean isMembersOnly() {
        return membersOnly;
    }

    public boolean isAttachmentsAllowed() {
        return attachmentsAllowed;
    }

    public boolean isReplyToList() {
        return replyToList;
    }

    public String getSubjectPrefix() {
        return subjectPrefix;
    }

    public String getMailetInfo() {
        return "AvalonListserv Mailet";
    }
}
