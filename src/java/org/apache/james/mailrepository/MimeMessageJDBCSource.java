/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2003 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache", "Jakarta", "JAMES" and "Apache Software Foundation"
 *    must not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 * Portions of this software are based upon public domain software
 * originally written at the National Center for Supercomputing Applications,
 * University of Illinois, Urbana-Champaign.
 */

package org.apache.james.mailrepository;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.avalon.cornerstone.services.store.StreamRepository;
import org.apache.james.core.MimeMessageSource;
import org.apache.james.util.JDBCUtil;

/**
 * This class points to a specific message in a repository.  This will return an
 * InputStream to the JDBC field/record, possibly sequenced with the file stream.
 */
public class MimeMessageJDBCSource extends MimeMessageSource {

    /**
     * Whether 'deep debugging' is turned on.
     */
    private static final boolean DEEP_DEBUG = false;

    //Define how to get to the data
    JDBCMailRepository repository = null;
    String key = null;
    StreamRepository sr = null;

    private long size = -1;

    /**
     * SQL used to retrieve the message body
     */
    String retrieveMessageBodySQL = null;

    /**
     * SQL used to retrieve the size of the message body
     */
    String retrieveMessageBodySizeSQL = null;

    /**
     * The JDBCUtil helper class
     */
    private static final JDBCUtil theJDBCUtil =
            new JDBCUtil() {
                protected void delegatedLog(String logString) {
                    // No logging available at this point in the code.
                    // Therefore this is a noop method.
                }
            };

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

        retrieveMessageBodySQL =
            repository.sqlQueries.getSqlString("retrieveMessageBodySQL", true);
        // this is optional
        retrieveMessageBodySizeSQL =
            repository.sqlQueries.getSqlString("retrieveMessageBodySizeSQL");
    }

    /**
     * Returns a unique String ID that represents the location from where
     * this source is loaded.  This will be used to identify where the data
     * is, primarily to avoid situations where this data would get overwritten.
     *
     * @return the String ID
     */
    public String getSourceId() {
        StringBuffer sourceIdBuffer =
            new StringBuffer(128)
                    .append(repository.repositoryName)
                    .append("/")
                    .append(key);
        return sourceIdBuffer.toString();
    }

    /**
     * Return the input stream to the database field and then the file stream.  This should
     * be smart enough to work even if the file does not exist.  This is to support
     * a repository with the entire message in the database, which is how James 1.2 worked.
     */
    public synchronized InputStream getInputStream() throws IOException {
        Connection conn = null;
        PreparedStatement retrieveMessageStream = null;
        ResultSet rsRetrieveMessageStream = null;
        try {
            conn = repository.getConnection();

            byte[] headers = null;

            long start = 0;
            if (DEEP_DEBUG) {
                start = System.currentTimeMillis();
                System.out.println("starting");
            }
            retrieveMessageStream = conn.prepareStatement(retrieveMessageBodySQL);
            retrieveMessageStream.setString(1, key);
            retrieveMessageStream.setString(2, repository.repositoryName);
            rsRetrieveMessageStream = retrieveMessageStream.executeQuery();

            if (!rsRetrieveMessageStream.next()) {
                throw new IOException("Could not find message");
            }

            headers = rsRetrieveMessageStream.getBytes(1);
            if (DEEP_DEBUG) {
                System.err.println("stopping");
                System.err.println(System.currentTimeMillis() - start);
            }

            InputStream in = new ByteArrayInputStream(headers);
            try {
                if (sr != null) {
                    in = new SequenceInputStream(in, sr.get(key));
                }
            } catch (Exception e) {
                //ignore this... either sr is null, or the file does not exist
                // or something else
            }
            return in;
        } catch (SQLException sqle) {
            throw new IOException(sqle.toString());
        } finally {
            theJDBCUtil.closeJDBCResultSet(rsRetrieveMessageStream);
            theJDBCUtil.closeJDBCStatement(retrieveMessageStream);
            theJDBCUtil.closeJDBCConnection(conn);
        }
    }

    /**
     * Runs a custom SQL statement to check the size of the message body
     */
    public synchronized long getMessageSize() throws IOException {
        if (size != -1) return size;
        if (retrieveMessageBodySizeSQL == null) {
            //There was no SQL statement for this repository... figure it out the hard way
            System.err.println("no SQL statement to find size");
            return size = super.getMessageSize();
        }
        Connection conn = null;
        PreparedStatement retrieveMessageSize = null;
        ResultSet rsRetrieveMessageSize = null;
        try {
            conn = repository.getConnection();

            retrieveMessageSize = conn.prepareStatement(retrieveMessageBodySizeSQL);
            retrieveMessageSize.setString(1, key);
            retrieveMessageSize.setString(2, repository.repositoryName);
            rsRetrieveMessageSize = retrieveMessageSize.executeQuery();

            if (!rsRetrieveMessageSize.next()) {
                throw new IOException("Could not find message");
            }

            size = rsRetrieveMessageSize.getLong(1);

            InputStream in = null;
            try {
                if (sr != null) {
                    if (sr instanceof org.apache.james.mailrepository.filepair.File_Persistent_Stream_Repository) {
                        size += ((org.apache.james.mailrepository.filepair.File_Persistent_Stream_Repository) sr).getSize(key);
                    } else {
                        in = sr.get(key);
                        int len = 0;
                        byte[] block = new byte[1024];
                        while ((len = in.read(block)) > -1) {
                            size += len;
                        }
                    }
                }
            } catch (Exception e) {
                //ignore this... either sr is null, or the file does not exist
                // or something else
            } finally {
                try {
                    if (in != null) {
                        in.close();
                    }
                } catch (IOException ioe) {
                    // Ignored - no access to logger at this point in the code
                }
            }

            return size;
        } catch (SQLException sqle) {
            throw new IOException(sqle.toString());
        } finally {
            theJDBCUtil.closeJDBCResultSet(rsRetrieveMessageSize);
            theJDBCUtil.closeJDBCStatement(retrieveMessageSize);
            theJDBCUtil.closeJDBCConnection(conn);
        }
    }

    /**
     * Check to see whether this is the same repository and the same key
     */
    public boolean equals(Object obj) {
        if (obj instanceof MimeMessageJDBCSource) {
            // TODO: Figure out whether other instance variables should be part of
            // the equals equation
            MimeMessageJDBCSource source = (MimeMessageJDBCSource)obj;
            return ((source.key == key) || ((source.key != null) && source.key.equals(key))) &&
                   ((source.repository == repository) || ((source.repository != null) && source.repository.equals(repository)));
        }
        return false;
    }

    /**
     * Provide a hash code that is consistent with equals for this class
     *
     * @return the hash code
     */
     public int hashCode() {
        int result = 17;
        if (key != null) {
            result = 37 * key.hashCode();
        }
        if (repository != null) {
            result = 37 * repository.hashCode();
        }
        return result;
     }
}
