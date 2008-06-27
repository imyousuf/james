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
import com.workingdogs.town.TableDataSet;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
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
import org.apache.james.core.JamesMimeMessage;
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
 *  <br>            <conn>file:///dev/james/dist/var/maildatabase</conn>
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
public class TownSpoolRepository
    extends AbstractLoggable
    implements SpoolRepository, Component, Configurable {

    private Lock lock;
    private String destination;
    private String repositoryName;

    private String conndefinition;
    private String tableName;

    public void configure(Configuration conf) throws ConfigurationException {
        destination = conf.getAttribute("destinationURL");
        repositoryName = destination.substring(destination.indexOf("//") + 2);
        String checkType = conf.getAttribute("type");
        if (! (checkType.equals("MAIL") || checkType.equals("SPOOL")) ) {
            final String message =
                "Attempt to configure TownSpoolRepository as " + checkType;
            getLogger().warn( message );
            throw new ConfigurationException( message );
        }
        // ignore model
        conndefinition = conf.getChild("conn").getValue();
        tableName = conf.getChild("table").getValue();
    }

    public synchronized void unlock(Object key) {
        if (lock.unlock(key)) {
            notifyAll();
        } else {
            throw new LockException("Your thread do not own the lock of record " + key);
        }
    }

    public synchronized void lock(Object key) {
        if (lock.lock(key)) {
            notifyAll();
        } else {
            throw new LockException("Record " + key + " already locked by another thread");
        }
    }

    public void store(MailImpl mc) {
        try {
            //System.err.println("storing " + mc.getName());
            String key = mc.getName();
            mc.setLastUpdated(new Date());
            TableDataSet messages = new TableDataSet(ConnDefinition.getInstance(conndefinition), tableName);
            messages.setWhere("message_name = '" + key + "' and repository_name = '" + repositoryName + "'");
            Record mail = null;
            boolean inserted = true;
            if (messages.size() == 0) {
                inserted = true;
                //insert the message
                mail = messages.addRecord();
                mail.setValue("message_name", key);
                mail.setValue("repository_name", repositoryName + "");
            } else {
                //update the message
                inserted = false;
                mail = messages.getRecord(0);
            }
            mail.setValue("message_state", mc.getState());
            mail.setValue("error_message", mc.getErrorMessage());
            mail.setValue("sender", mc.getSender().toString());
            StringBuffer recipients = new StringBuffer();
            for (Iterator i = mc.getRecipients().iterator(); i.hasNext(); ) {
                recipients.append(i.next().toString());
                if (i.hasNext()) {
                    recipients.append("\r\n");
                }
            }
            mail.setValue("recipients", recipients.toString());
            mail.setValue("remote_host", mc.getRemoteHost());
            mail.setValue("remote_addr", mc.getRemoteAddr());
            mail.setValue("last_updated", mc.getLastUpdated());
            MimeMessage messageBody = mc.getMessage();

            boolean saveInRecord = false;
            if (messageBody instanceof JamesMimeMessage) {
                JamesMimeMessage jamesmessage = (JamesMimeMessage)messageBody;
                if (jamesmessage.isModified()) {
                    //Just save it...we can't be clever here
                    saveInRecord = true;
                } else {
                    if (mail.toBeSavedWithUpdate()) {
                        //Do nothing... the message wasn't changed.
                        //System.err.println("Not saving message (" + toString() + "... wasn't changed");
                    } else {
                        //For now always save the record
                        saveInRecord = true;


                        //This message could be a transfer from another source
                        InputStream in = jamesmessage.getSourceStream();
                        if (in instanceof TownMimeMessageInputStream) {
                            //This must already be stored in the same database (hopefully)

                            //  Let's copy the record from here to there

                            //Once we do this, we'll no longer mark to saveInRecord
                        }

                    }
                }
            } else {
                //This is some other unknown MimeMessage
                saveInRecord = true;
            }
            if (saveInRecord) {
                //Update this field here
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                if (messageBody != null) {
                    messageBody.writeTo(bout);
                }
                mail.setValue("message_body", bout.toByteArray());
            }
            if (mail.toBeSavedWithUpdate() && messageBody instanceof JamesMimeMessage
                && !((JamesMimeMessage)messageBody).isModified()) {
                //Do nothing... the message wasn't changed.
            } else {
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                if (messageBody != null) {
                    messageBody.writeTo(bout);
                }
                mail.setValue("message_body", bout.toByteArray());
            }
            mail.save();
            synchronized (this) {
                notifyAll();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Exception caught while storing mail Container: " + e);
        }
    }

    public MailImpl retrieve(String key) {
        try {
            //System.err.println("retrieving " + key);
            //TableDataSet messages = new TableDataSet(ConnDefinition.getInstance(conndefinition), tableName);
            QueryDataSet messages = new QueryDataSet(ConnDefinition.getInstance(conndefinition),
                                                     "SELECT message_name, message_state, error_message, sender, recipients, remote_host, remote_addr, last_updated"
                                                     + " FROM " + tableName
                                                     + " WHERE message_name='" + key + "' and repository_name='" + repositoryName + "'");
            //messages.setWhere("message_name='" + key + "' and repository_name='" + repositoryName + "'");
            Record message = messages.getRecord(0);
            MailImpl mc = new MailImpl();
            mc.setName(message.getAsString("message_name"));
            mc.setState(message.getAsString("message_state"));
            mc.setErrorMessage(message.getAsString("error_message"));
            mc.setSender(new MailAddress(message.getAsString("sender")));
            StringTokenizer st = new StringTokenizer(message.getAsString("recipients"), "\r\n", false);
            Set recipients = new HashSet();
            while (st.hasMoreTokens()) {
                recipients.add(new MailAddress(st.nextToken()));
            }
            mc.setRecipients(recipients);
            mc.setRemoteHost(message.getAsString("remote_host"));
            mc.setRemoteAddr(message.getAsString("remote_addr"));
            mc.setLastUpdated(message.getAsUtilDate("last_updated"));
            InputStream in = new TownMimeMessageInputStream(conndefinition, tableName, key, repositoryName);
            //InputStream in = new ByteArrayInputStream(message.getAsBytes("message_body"));
            JamesMimeMessage jamesmessage = new JamesMimeMessage(javax.mail.Session.getDefaultInstance(System.getProperties(), null), in);
            mc.setMessage(jamesmessage);
            //mc.setMessage(bin);
            return mc;
        } catch (Exception me) {
            me.printStackTrace();
            throw new RuntimeException("Exception while retrieving mail: " + me.getMessage());
        }
    }

    public void remove(MailImpl mail) {
        remove(mail.getName());
    }

    public void remove(String key) {
        //System.err.println("removing " + key);
        try {
            lock(key);
            TableDataSet messages = new TableDataSet(ConnDefinition.getInstance(conndefinition), tableName);
            messages.setWhere("message_name='" + key + "' and repository_name='" + repositoryName + "'");
            Record message = messages.getRecord(0);
            message.markToBeDeleted();
            message.save();
        } catch (Exception me) {
            throw new RuntimeException("Exception while removing mail: " + me.getMessage());
        } finally {
            unlock(key);
        }
    }

    public Iterator list() {
        try {
            QueryDataSet messages = new QueryDataSet(ConnDefinition.getInstance(conndefinition),
                                                     "SELECT message_name FROM " + tableName + " WHERE repository_name = '" + repositoryName + "' "
                                                     + "ORDER BY last_updated");
            List messageList = new ArrayList(messages.size());
            for (int i = 0; i < messages.size(); i++) {
                messageList.add(messages.getRecord(i).getAsString("message_name"));
            }
            return messageList.iterator();
        } catch (Exception me) {
            me.printStackTrace();
            throw new RuntimeException("Exception while listing mail: " + me.getMessage());
        }
    }

    public synchronized String accept() {
        while (true) {
            for(Iterator it = list(); it.hasNext(); ) {
                Object o = it.next();
                if (lock.lock(o)) {
                    return o.toString();
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
        }
    }
}
