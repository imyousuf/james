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
//import org.apache.avalon.services.Store;
import org.apache.james.*;
import org.apache.james.transport.*;
import org.apache.james.services.MailStore;
import org.apache.james.services.UsersRepository;
import org.apache.mailet.*;

/**
 * MailingListServer capability
 *
 * Requires a configuration element in the .conf.xml file of the form:
 *  <repository destinationURL="file://path-to-root-dir-for-repository"
 *              type="USERS"
 *              model="SYNCHRONOUS"/>
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

        ComponentManager compMgr = (ComponentManager)getMailetContext().getAttribute(Constants.AVALON_COMPONENT_MANAGER);
	try {
	    MailStore mailstore = (MailStore) compMgr.lookup("org.apache.james.services.MailStore");
	    String membersPath = getInitParameter("membersPath");
	    DefaultConfiguration usersConf
		= new DefaultConfiguration("repository", "generated:AvalonListServ");
	    usersConf.addAttribute("destinationURL", membersPath);
	    usersConf.addAttribute("type", "USERS");
	    usersConf.addAttribute("model", "SYNCHRONOUS");
	    
	    members = (UsersRepository) mailstore.select(usersConf);
	} catch (ComponentNotFoundException cnfe) {
	    log("Failed to retrieve Store component:" + cnfe.getMessage());
	} catch (ComponentNotAccessibleException cnae) {
	    log("Failed to retrieve Store component:" + cnae.getMessage());
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
