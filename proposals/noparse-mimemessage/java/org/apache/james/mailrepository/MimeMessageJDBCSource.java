/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.mailrepository;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.apache.james.core.MimeMessageSource;

public class MimeMessageJDBCSource
    extends MimeMessageSource {

    private String retrieveMessageStreamSQL;

    //Define how to get to the data
    JDBCMailRepository repository = null;
    String key = null;

    //The inputstream, if closed, is null, if open contains appropriate references
    InputStream in;
    Statement inStatement;
    ResultSet inResultSet;
    Connection conn;

    public MimeMessageJDBCSource(JDBCMailRepository repository,
                                      String key) throws IOException {
        if (repository == null) {
            throw new IOException("Repository is null");
        }
        if (key == null) {
            throw new IOException("Message name (key) was not defined");
        }
        this.repository = repository;
        this.key = key;

        retrieveMessageStreamSQL = "SELECT message_body FROM " + repository.tableName
                + " WHERE message_name = ? AND repository_name = ?";
    }

    public synchronized InputStream getInputStream() throws IOException {
        //System.err.println("loading data for " + key + "/" + repository);

        try {
            conn = repository.getConnection();

            PreparedStatement retrieveMessageStream = conn.prepareStatement(retrieveMessageStreamSQL);
            retrieveMessageStream.setString(1, key);
            retrieveMessageStream.setString(2, repository.repositoryName);
            //System.err.println(retrieveMessageStream);
            //System.err.println(retrieveMessageStreamSQL);
            //System.err.println("'" + key + "'");
            //System.err.println("'" + repository.repositoryName + "'");
            ResultSet rsRetrieveMessageStream = retrieveMessageStream.executeQuery();

            if (!rsRetrieveMessageStream.next()) {
                throw new IOException("Could not find message");
            }

            in = rsRetrieveMessageStream.getBinaryStream(1);
            inResultSet = rsRetrieveMessageStream;
            inStatement = retrieveMessageStream;
            return in;
        } catch (SQLException sqle) {
            throw new IOException(sqle.toString());
        } finally {
            //Do we really want to do this?  I think not
            /*
            try {
                conn.close();
            } catch (Exception e) {
            }
            */
        }
    }

    public boolean equals(Object obj) {
        if (obj instanceof MimeMessageJDBCSource) {
            MimeMessageJDBCSource source = (MimeMessageJDBCSource)obj;
            return source.key.equals(key) && source.repository.equals(repository);
        }
        return false;
    }
}
