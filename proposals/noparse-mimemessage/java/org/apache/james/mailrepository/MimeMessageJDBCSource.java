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
import java.io.SequenceInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.apache.james.core.MimeMessageSource;
import org.apache.avalon.cornerstone.services.store.StreamRepository;

/**
 * This class points to a specific message in a repository.  This will return an
 * InputStream to the JDBC field/record, possibly sequenced with the file stream.
 */
public class MimeMessageJDBCSource extends MimeMessageSource {

    //Define how to get to the data
    JDBCMailRepository repository = null;
    String key = null;
    StreamRepository sr = null;

    String retrieveMessageBodySQL = null;

    /**
     * Construct a MimeMessageSource based on a JDBC repository, a key, and a
     * stream repository (where we might store the message body)
     */
    public MimeMessageJDBCSource(JDBCMailRepository repository,
            String key, StreamRepository sr) throws IOException {
        if (repository == null) {
            throw new IOException("Repository is null");
        }
        if (key == null) {
            throw new IOException("Message name (key) was not defined");
        }
        this.repository = repository;
        this.key = key;
        this.sr = sr;

        retrieveMessageBodySQL = repository.sqlQueries.getProperty("retrieveMessageBodySQL");
    }

    /**
     * Return the input stream to the database field and then the file stream.  This should
     * be smart enough to work even if the file does not exist.  This is to support
     * a repository with the entire message in the database, which is how James 1.2 worked.
     */
    public synchronized InputStream getInputStream() throws IOException {
        //System.err.println("loading data for " + key + "/" + repository);

        try {
            Connection conn = repository.getConnection();

            PreparedStatement retrieveMessageStream = conn.prepareStatement(retrieveMessageBodySQL);
            retrieveMessageStream.setString(1, key);
            retrieveMessageStream.setString(2, repository.repositoryName);
            ResultSet rsRetrieveMessageStream = retrieveMessageStream.executeQuery();

            if (!rsRetrieveMessageStream.next()) {
                throw new IOException("Could not find message");
            }

            byte[] headers = rsRetrieveMessageStream.getBytes(1);
            InputStream in = new ByteArrayInputStream(headers);
            if (sr != null) {
                in = new SequenceInputStream(in, sr.get(key));
            }
            return in;
        } catch (SQLException sqle) {
            throw new IOException(sqle.toString());
        }
    }

    /**
     * Check to see whether this is the same repository and the same key
     */
    public boolean equals(Object obj) {
        if (obj instanceof MimeMessageJDBCSource) {
            MimeMessageJDBCSource source = (MimeMessageJDBCSource)obj;
            return source.key.equals(key) && source.repository.equals(repository);
        }
        return false;
    }
}
