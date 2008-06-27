/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.mailrepository;

import com.workingdogs.town.ConnDefinition;
import com.workingdogs.town.QueryDataSet;
import com.workingdogs.town.Record;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.james.core.JamesMimeMessageInputStream;

public class TownMimeMessageInputStream 
    extends JamesMimeMessageInputStream {

    //Define how to get to the data
    String connDefinition = null;
    String table = null;
    String key = null;
    String repository = null;

    public TownMimeMessageInputStream(String connDefinition, 
                                      String table, 
                                      String key, 
                                      String repository) throws IOException {
        if (connDefinition == null) {
            throw new IOException("Conn definition is null");
        }
        if (table == null) {
            throw new IOException("Table is null");
        }
        if (key == null) {
            throw new IOException("Message name (key) was not defined");
        }
        if (repository == null) {
            throw new IOException("Repository is null");
        }
        this.connDefinition = connDefinition;
        this.table = table;
        this.key = key;
        this.repository = repository;
    }

    public String getConnDefinition() {
        return connDefinition;
    }

    public String getTable() {
        return table;
    }

    public String getMessageName() {
        return key;
    }

    public String getRepository() {
        return repository;
    }

    protected synchronized InputStream openStream() throws IOException {
        //System.err.println("loading data for " + key + "/" + repository);

        //TableDataSet messages = new TableDataSet(ConnDefinition.getInstance(connDefinition), table);
        //messages.setWhere("message_name='" + key + "' and repository_name='" + repository + "'");
        //messages.setColumns("message_body");
        QueryDataSet messages = new QueryDataSet(ConnDefinition.getInstance(connDefinition),
                                                 "SELECT message_body "
                                                 + " FROM " + table
                                                 + " WHERE message_name='" + key + "' AND repository_name='" + repository + "'");
        if (messages.size() == 0) {
            throw new IOException("Could not find message");
        }
        Record message = messages.getRecord(0);
        return new ByteArrayInputStream(message.getAsBytes("message_body"));
    }

    public boolean equals(Object obj) {
        if (obj instanceof TownMimeMessageInputStream) {
            TownMimeMessageInputStream in = (TownMimeMessageInputStream)obj;
            return in.getConnDefinition().equals(connDefinition)
                && in.getTable().equals(table)
                && in.getMessageName().equals(key)
                && in.getRepository().equals(repository);
        }
        return false;
    }
}
