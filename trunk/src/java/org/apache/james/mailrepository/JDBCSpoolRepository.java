/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.mailrepository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import javax.mail.internet.MimeMessage;
import org.apache.avalon.framework.component.Component;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.AbstractLoggable;
import org.apache.james.core.MimeMessageWrapper;
import org.apache.james.core.MailImpl;
import org.apache.james.services.SpoolRepository;
import org.apache.james.util.Lock;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;

/**
 * Implementation of a SpoolRepository on a database.
 *
 * <p>Requires a configuration element in the .conf.xml file of the form:
 *  <br><repository destinationURL="town://path"
 *  <br>            type="MAIL"
 *  <br>            model="SYNCHRONOUS"/>
 *  <br>            <driver>sun.jdbc.odbc.JdbcOdbcDriver</conn>
 *  <br>            <conn>jdbc:odbc:LocalDB</conn>
 *  <br>            <table>Message</table>
 *  <br></repository>
 * <p>destinationURL specifies..(Serge??)
 * <br>Type can be SPOOL or MAIL
 * <br>Model is currently not used and may be dropped
 * <br>conn is the location of the ...(Serge)
 * <br>table is the name of the table in the Database to be used
 *
 * <p>Requires a logger called MailRepository.
 *
 * @version 1.0.0, 24/04/1999
 * @author  Serge Knystautas <sergek@lokitech.com>
 */
public class JDBCSpoolRepository
    extends JDBCMailRepository
    implements SpoolRepository {

    public synchronized String accept() {
        while (true) {
            Connection conn = null;
            PreparedStatement listMessages = null;
            ResultSet rsListMessages = null;
            try {
                conn = getConnection();
                listMessages =
                    conn.prepareStatement(sqlQueries.getSqlString("listMessagesSQL", true));
                listMessages.setString(1, repositoryName);
                rsListMessages = listMessages.executeQuery();

                while (rsListMessages.next()) {
                    String message = rsListMessages.getString(1);

                    if (lock(message)) {
                        rsListMessages.close();
                        listMessages.close();
                        return message;
                    }
                }
                rsListMessages.close();
                listMessages.close();
            } catch (Exception me) {
                me.printStackTrace();
                throw new RuntimeException("Exception while listing mail: " + me.getMessage());
            } finally {
                try {
                    rsListMessages.close();
                } catch (Exception e) {
                }
                try {
                    listMessages.close();
                } catch (Exception e) {
                }
                try {
                    conn.close();
                } catch (Exception e) {
                }
            }
            try {
                wait();
            } catch (InterruptedException ignored) {
            }
        }
    }

    public synchronized String accept(long delay) {
        while (true) {
            long next = 0;
            Connection conn = null;
            PreparedStatement listMessages = null;
            ResultSet rsListMessages = null;
            try {
                conn = getConnection();
                listMessages =
                    conn.prepareStatement(sqlQueries.getSqlString("listMessagesSQL", true));
                listMessages.setString(1, repositoryName);
                rsListMessages = listMessages.executeQuery();

                while (rsListMessages.next()) {
                    String message = rsListMessages.getString(1);
                    String state = rsListMessages.getString(2);
                    boolean process = false;
                    if (state.equals(Mail.ERROR)) {
                        //Test the time
                        long timeToProcess = delay + rsListMessages.getTimestamp(3).getTime();
                        if (System.currentTimeMillis() > timeToProcess) {
                            process = true;
                        } else {
                            if (next == 0 || next > timeToProcess) {
                                //Mark this as the next most likely possible mail to process
                                next = timeToProcess;
                            }
                        }
                    } else {
                        process = true;
                    }

                    if (process && lock(message)) {
                        return message;
                    }
                }
            } catch (Exception me) {
                me.printStackTrace();
                throw new RuntimeException("Exception while listing mail: " + me.getMessage());
            } finally {
                try {
                    rsListMessages.close();
                } catch (Exception e) {
                }
                try {
                    listMessages.close();
                } catch (Exception e) {
                }
                try {
                    conn.close();
                } catch (Exception e) {
                }
            }

            //We did not find any... let's wait for a certain amount of time
            try {
                if (next == 0) {
                    wait();
                } else {
                    wait(next - System.currentTimeMillis());
                }
            } catch (InterruptedException ignored) {
            }
        }
    }
}
