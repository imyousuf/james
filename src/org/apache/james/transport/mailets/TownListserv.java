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
import org.apache.mailet.*;
import com.workingdogs.town.*;

/**
 * Database driven listserv.  Queries the database everytime so you do not need
 * to restart mail server for changes to take effect.
 *
 * Parameters:
 * - conn : the connection definition URL
 * - listserv_id : the unique identifier for this listserv in the database
 * - listserv_table : the table that contains the listserv definitions
 * - members_table : the table that contains the list of members for each listserv
 */
public class TownListserv extends GenericListserv {

    String conn = null;
    String listservID = null;
    String listservTable = null;
    String membersTable = null;

    public void init() {
        conn = getInitParameter("conn");
        listservID = getInitParameter("listserv_id");
        listservTable = getInitParameter("listserv_table");
        membersTable = getInitParameter("members_table");
    }

    public Collection getMembers() throws ParseException {
        Collection members = new Vector();
        try {
            TableDataSet tds = new TableDataSet(ConnDefinition.getInstance(conn), membersTable);
            tds.setWhere("listserv_id = '" + listservID + "'");
            for (int i = 0; i < tds.size(); i++) {
                MailAddress addr = new MailAddress(tds.getRecord(i).getAsString("list_address"));
                members.add(addr);
            }
        } catch (TownException te) {
            log("error retrieving members list", te);
        }
        return members;
    }

    public boolean isMembersOnly() {
        try {
            TableDataSet tds = new TableDataSet(ConnDefinition.getInstance(conn), listservTable);
            tds.setWhere("listserv_id = '" + listservID + "'");
            return tds.getRecord(0).getAsBoolean("members_only");
        } catch (TownException te) {
            log("error retrieving members only flag", te);
            return true;
        }
    }

    public boolean isAttachmentsAllowed() {
        try {
            TableDataSet tds = new TableDataSet(ConnDefinition.getInstance(conn), listservTable);
            tds.setWhere("listserv_id = '" + listservID + "'");
            return tds.getRecord(0).getAsBoolean("attachments_allowed");
        } catch (TownException te) {
            log("error retrieving attachments allowed flag", te);
            return true;
        }
    }

    public boolean isReplyToList() {
        try {
            TableDataSet tds = new TableDataSet(ConnDefinition.getInstance(conn), listservTable);
            tds.setWhere("listserv_id = '" + listservID + "'");
            return tds.getRecord(0).getAsBoolean("reply_to_list");
        } catch (TownException te) {
            log("error retrieving reply to list flag", te);
            return true;
        }
    }

    public String getSubjectPrefix() {
        try {
            TableDataSet tds = new TableDataSet(ConnDefinition.getInstance(conn), listservTable);
            tds.setWhere("listserv_id = '" + listservID + "'");
            return tds.getRecord(0).getAsString("subject_prefix");
        } catch (TownException te) {
            log("error retrieving subject prefix", te);
            return null;
        }
    }

    public MailAddress getListservAddress() throws ParseException {
        try {
            TableDataSet tds = new TableDataSet(ConnDefinition.getInstance(conn), listservTable);
            tds.setWhere("listserv_id = '" + listservID + "'");
            return new MailAddress(tds.getRecord(0).getAsString("list_address"));
        } catch (TownException te) {
            log("error retrieving listserv address", te);
            return null;
        }
    }

    public String getMailetInfo() {
        return "TownListserv Mailet";
    }
}
