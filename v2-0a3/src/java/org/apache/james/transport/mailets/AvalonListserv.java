/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.transport.mailets;

import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.james.Constants;
import org.apache.james.services.UsersRepository;
import org.apache.james.services.UsersStore;
import org.apache.mailet.MailAddress;

import javax.mail.internet.ParseException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;

/**
 * MailingListServer capability.
 * 
 * <p>Requires a configuration element in the .conf.xml file of the form:
 * <br>&lt;repositoryName&gt;name of user repository configured in UsersStore block &lt;/repositoryName&gt;
 * <br>&lt;membersonly&gt;
 * <br>&lt;attachmentsallowed&gt;
 * <br>&lt;replytolist&gt;
 * <br>&lt;subjectprefix&gt;
 *
 * @author  <a href="sergek@lokitech.com">Serge Knystautas </a>
 * @version This is $Revision: 1.3 $
 * Committed on $Date: 2002/01/18 02:48:37 $ by: $Author: darrell $ 
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

        ComponentManager compMgr = (ComponentManager)getMailetContext().getAttribute(Constants.AVALON_COMPONENT_MANAGER);
        try {
            UsersStore usersStore = (UsersStore)compMgr.lookup("org.apache.james.services.UsersStore");
            String repName = getInitParameter("repositoryName");

            members = (UsersRepository)usersStore.getRepository( repName );
        } catch (ComponentException cnfe) {
            log("Failed to retrieve Store component:" + cnfe.getMessage());
        } catch (Exception e) {
            log("Failed to retrieve Store component:" + e.getMessage());
        }
    }

    public Collection getMembers() throws ParseException {
        Collection reply = new Vector();
        for (Iterator it = members.list(); it.hasNext(); ) {
            reply.add(new MailAddress(it.next().toString()));
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
