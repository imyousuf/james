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
import org.apache.james.util.LockException;
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
            System.err.println("calling accept");
            for(Iterator it = list(); it.hasNext(); ) {
                Object o = it.next();
                if (lock.lock(o)) {
                    return o.toString();
                }
            }
            try {
                System.out.println("waiting for " + this);
                wait();
            } catch (InterruptedException ignored) {
            }
        }
    }

    public synchronized String accept(long delay) {
        while (true) {
            long youngest = 0;
            //Really unoptimized query here... should be much smart about this...
            for (Iterator it = list(); it.hasNext(); ) {
                String s = it.next().toString();
                if (lock.lock(s)) {
                    //We have a lock on this object... let's grab the message
                    //  and see if it's a valid time.
                    MailImpl mail = retrieve(s);
                    if (mail.getState().equals(Mail.ERROR)) {
                        //Test the time...
                        long timeToProcess = delay + mail.getLastUpdated().getTime();
                        if (System.currentTimeMillis() > timeToProcess) {
                            //We're ready to process this again
                            return s;
                        } else {
                            //We're not ready to process this.
                            if (youngest == 0 || youngest > timeToProcess) {
                                //Mark this as the next most likely possible mail to process
                                youngest = timeToProcess;
                            }
                        }
                    } else {
                        //This guy is good to go... return him
                        return s;
                    }
                }
            }
            //We did not find any... let's wait for a certain amount of time
            try {
                if (youngest == 0) {
                    wait();
                } else {
                    wait(youngest - System.currentTimeMillis());
                }
            } catch (InterruptedException ignored) {
            }
            System.err.println("done waiting in long accept");
        }
    }
}
