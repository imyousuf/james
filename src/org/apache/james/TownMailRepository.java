/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james;

import org.apache.avalon.blocks.*;
import org.apache.avalon.*;
import org.apache.avalon.utils.*;
import java.util.*;
import java.io.*;
import org.apache.mailet.*;
import org.apache.james.core.*;
import javax.mail.internet.*;
import javax.mail.MessagingException;
import com.workingdogs.town.*;

/**
 * Implementation of a MailRepository on a database.
 * @version 1.0.0, 24/04/1999
 * @author  Serge Knystautas <sergek@lokitech.com>
 */
public class TownMailRepository implements SpoolRepository {

    /**
     * Define a STREAM repository. Streams are stored in the specified
     * destination.
     */

    private String name;
    private String type;
    private String model;
    private Lock lock;

    private String repositoryName;
    private String conndefinition;

    public TownMailRepository() {
    }

    public void setAttributes(String name, String destination, String type, String model) {
        this.name = name;
        this.model = model;
        this.type = type;

        //need to parse the destination out to find the mail repository and
        //  the database connection URL
        String temp = destination.substring("town://".length());
        int at = temp.indexOf("@");
        repositoryName = temp.substring(0, at);
        conndefinition = temp.substring(at + 1);
    }

    public void setComponentManager(ComponentManager comp) {
        lock = new Lock();
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getModel() {
        return model;
    }

    public String getChildDestination(String childName) {
        return "town://" + repositoryName + "/" + childName + "@" + conndefinition;
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


    public synchronized void store(MailImpl mc) {
        try {
            //System.err.println("storing " + mc.getName());
            String key = mc.getName();
            mc.setLastUpdated(new Date());
            TableDataSet messages = new TableDataSet(ConnDefinition.getInstance(conndefinition), "Message");
            messages.setWhere("message_name = '" + key + "'");
            Record mail = null;
            if (messages.size() == 0) {
                //insert the message
                mail = messages.addRecord();
                mail.setValue("message_name", key);
                mail.setValue("repository_name", repositoryName + "");
            } else {
                //update the message
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
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            messageBody.writeTo(bout);
            mail.setValue("message_body", bout.toByteArray());
            mail.save();
            notifyAll();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Exception caught while storing Message Container: " + e);
        }
    }

    public synchronized MailImpl retrieve(String key) {
        //MailImpl mc = (MailImpl) or.get(key);
        try {
            //System.err.println("retrieving " + key);
            TableDataSet messages = new TableDataSet(ConnDefinition.getInstance(conndefinition), "Message");
            messages.setWhere("message_name='" + key + "' and repository_name='" + repositoryName + "'");
            Record message = messages.getRecord(0);
            MailImpl mc = new MailImpl();
            mc.setName(message.getAsString("message_name"));
            mc.setState(message.getAsString("message_state"));
            mc.setErrorMessage(message.getAsString("error_message"));
            mc.setSender(new MailAddress(message.getAsString("sender")));
            BufferedReader recipientList = new BufferedReader(new StringReader(message.getAsString("recipients")));
            Vector recipients = new Vector();
            String recipient = null;
            while ((recipient = recipientList.readLine()) != null) {
                recipients.add(new MailAddress(recipient));
            }
            mc.setRecipients(recipients);
            mc.setRemoteHost(message.getAsString("remote_host"));
            mc.setRemoteAddr(message.getAsString("remote_addr"));
            mc.setLastUpdated(message.getAsUtilDate("last_updated"));
            ByteArrayInputStream bin = new ByteArrayInputStream(message.getAsBytes("message_body"));
            mc.setMessage(bin);
            return mc;
        } catch (Exception me) {
            throw new RuntimeException("Exception while retrieving mail: " + me.getMessage());
        }
    }

    public synchronized void remove(MailImpl mail) {
        remove(mail.getName());
    }

    public synchronized void remove(String key) {
        lock(key);
        try {
            TableDataSet messages = new TableDataSet(ConnDefinition.getInstance(conndefinition), "Message");
            messages.setWhere("message_name='" + key + "' and repository_name='" + repositoryName + "'");
            Record message = messages.getRecord(0);
            message.markToBeDeleted();
            message.save();
        } catch (Exception me) {
            throw new RuntimeException("Exception while removing mail: " + me.getMessage());
        }
        unlock(key);
    }

    public Enumeration list() {
        try {
            TableDataSet messages = new TableDataSet(ConnDefinition.getInstance(conndefinition), "Message");
            messages.setColumns("message_name");
            messages.setWhere("repository_name='" + repositoryName + "'");
            messages.setOrder("last_updated");
            Vector messageList = new Vector(messages.size());
            for (int i = 0; i < messages.size(); i++) {
                messageList.add(messages.getRecord(i).getAsString("message_name"));
            }
            return messageList.elements();
        } catch (Exception me) {
            throw new RuntimeException("Exception while listing mail: " + me.getMessage());
        }
    }

    public synchronized String accept() {

        while (true) {
            for(Enumeration e = list(); e.hasMoreElements(); ) {
                Object o = e.nextElement();
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
            for (Enumeration e = list(); e.hasMoreElements(); ) {
                String s = e.nextElement().toString();
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
