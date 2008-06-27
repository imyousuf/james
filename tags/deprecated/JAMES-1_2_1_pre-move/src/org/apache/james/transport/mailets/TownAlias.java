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
 * Rewrites recipient addresses based on a database table.  The connection
 * is configured by passing the URL to a conn definition.  You need to set
 * the table name to check (or view) along with the source and target columns
 * to use.  For example,
 * <mailet match="All" class="TownAlias">
 *   <conn>file:///dev/james/dist/var/maildatabase</conn>
 *   <table>MailAlias</table>
 *   <sourceCol>email_alias</sourceCol>
 *   <targetCol>email_address</targetCol>
 * </mailet>
 *
 * @author  Serge Knystautas <sergek@lokitech.com>
 */
public class TownAlias extends GenericMailet {
    private String conndef = null;
    private String tableName = null;
    private String sourceColumn = null;
    private String targetColumn = null;

    public void init() {
        conndef = getInitParameter("conn");
        tableName = getInitParameter("table");
        sourceColumn = getInitParameter("sourceCol");
        targetColumn = getInitParameter("targetCol");
    }

    public void service(Mail mail) throws MessagingException {
        Collection recipients = mail.getRecipients();
        String inClause = null;
        for (Iterator i = recipients.iterator(); i.hasNext(); ) {
            if (inClause == null) {
                inClause = "'" + i.next().toString() + "'";
            } else {
                inClause += ",'" + i.next().toString() + "'";
            }
        }
        if (inClause == null) {
            return;
        }
        try {
            TableDataSet tds = new TableDataSet(ConnDefinition.getInstance(conndef), tableName);
            tds.setWhere(sourceColumn + " IN (" + inClause + ")");
            for (int i = 0; i < tds.size(); i++) {
                Record rec = tds.getRecord(i);
                MailAddress source = new MailAddress(rec.getAsString(sourceColumn));
                if (recipients.contains(source)) {
                    MailAddress target = new MailAddress(rec.getAsString(targetColumn));
                    recipients.remove(source);
                    recipients.add(target);
                }
            }
        } catch (TownException te) {
            throw new MailetException("Error accessing database", te);
        }
    }

    public String getMailetInfo() {
        return "Town (database) aliasing mailet";
    }
}

