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
 * An abstract implementation of a listserv.  The underlying implementation must define
 * various settings, and can vary in their individual configuration.  Supports restricting
 * to members only, allowing attachments or not, and sending replies back to the list.
 *
 * This listserv will assume that when it processes a message, only this listserv is the
 * recipient.
 */
public abstract class GenericListserv extends GenericMailet {

    /**
     * Returns a Collection of String objects that are recipient's email addresses
     */
    public abstract Collection getMembers();

    /**
     * Returns whether this list should restrict to senders only
     */
    public abstract boolean isMembersOnly();

    /**
     * Returns whether this listserv allow attachments
     */
    public abstract boolean isAttachmentsAllowed();

    /**
     * Returns whether listserv should add reply-to header
     */
    public abstract boolean isReplyToList();

    /**
     * The email address that this listserv processes on
     */
    public abstract String getListservAddress();

    /**
     * An optional subject prefix which will be surrounded by [].
     */
    public abstract String getSubjectPrefix();

    /**
     * Processes the message.  Assumes it is the only recipient of this forked message.
     */
    public final void service(Mail mail) {
        //Do nothing
    }
    /*
    public final void service(Mail mail) throws Exception {
        Collection members = getMembers();

        //Check for recipients only....
        if (isRecipientsOnly()) {
            //later...
        }

        //Check for no attachments
        if (!isAttachmentsAllowed()) {
            //later...
        }

        //Do stuff
        Vector newRcpts = new Vector ();

        //Put all the other recipients into a new Vector of recipients
        for (int j = 0; j < addr.length; j++)
            if (j != i)
                newRcpts.addElement (addr[j]);

        //Add everyone on the listserv's recipient list
        for (int j = 0; j < recipients.size (); j++)
            if (!newRcpts.contains (recipients.elementAt (j)))
                newRcpts.addElement (recipients.elementAt (j));

        //Convert the vector into an array
        addr = new InternetAddress[newRcpts.size ()];
        for (int j = 0; j < newRcpts.size (); j++)
            addr[j] = (InternetAddress)newRcpts.elementAt (j);

        //Set the new recipient list
        state.setRecipients (addr);

        //Set the subject if so
        if (subject != null)
        {
            String subj = message.getSubject ();
            //If the "subject" is in the subject line, remove it and everything before it
            int index = subj.indexOf (subject);
            if (index > -1)
            {
                if (index == 0)
                    subj = subject + ' ' + subj.substring (index + subject.length () + 1);
                else
                    subj = subject + ' ' + subj.substring (0, index) + subj.substring (index + subject.length () + 1);
            } else
                subj = subject + ' ' + subj;

            message.setSubject (subj);
        }
        message.setHeader ("Reply-To", listservAddress.toString ());

        state.setState (DeliveryState.PROCESSED);
        return;
    }
    */
}

