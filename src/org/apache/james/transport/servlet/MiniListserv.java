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
public class MiniListserv extends GenericListserv {
    protected boolean membersOnly = false;
    protected boolean attachmentsAllowed = true;
    protected boolean replyToList = true;
    protected String subject = null;
    protected String listservAddress = null;
    protected Vector members = null;

    public void init () {
        listservAddress = getConfiguration ("listservaddress").getValue();
        subject = getConfiguration ("subject").getValue();
        membersOnly = getConfiguration("membersonly", "false").getValueAsBoolean();
        attachmentsAllowed = getConfiguration("attachmentsallowed", "true").getValueAsBoolean ();
        replyToList = getConfiguration("replytolist", "true").getValueAsBoolean ();
        members = new Vector();
        for (Enumeration e = getConfigurations("member"); e.hasMoreElements(); ) {
            Configuration c = (Configuration) e.nextElement();
            members.addElement(c.getValue());
        }
    }

    protected Vector getMembers() {
        return members;
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
        return listservAddress;
    }

    public String getServletInfo() {
        return "MiniListserv Servlet";
    }
}

