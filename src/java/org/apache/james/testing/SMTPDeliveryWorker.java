/**
 * Copyright (C) 11-Oct-2002 The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file. 
 */
package org.apache.james.testing;
import java.io.IOException;
import org.apache.commons.net.smtp.SMTPClient;
/**
 * SMTPDeliveryWorker.java
 *
 * A worker thread that is part of the SMTP delivery end to end testing.
 *
 * @author <A href="mailto:danny@apache.org">Danny Angus</a>
 * 
 * $Id: SMTPDeliveryWorker.java,v 1.2 2002/10/14 06:44:22 pgoldstein Exp $
 */
public class SMTPDeliveryWorker implements Runnable {
    private SMTPClient client;

    /**
     * The test of the mail message to send.
     */
    private String mail;

    /**
     * The worker id.  Primarily used for identification by the class
     * managing the threads.
     */
    private int workerid;

    /**
     * The EndToEnd test case that spawned this thread.
     */
    EndToEnd boss;

    /**
     * Constructor for SMTPDeliveryWorker.
     *
     * @param client the SMTP client being used by this test to 
     */
    public SMTPDeliveryWorker(SMTPClient client, String mail, EndToEnd boss) {
        this.boss = boss;
        this.client = client;
        this.mail = mail;
    }
    /**
     * @see java.lang.Runnable#run()
     */
    public void run() {
        try {
            
            //move the connect and disconnect into the loop to make a new connection for every mail
            client.connect("127.0.0.1", 25);
            for (int run = 0; run < 100; run++) {
                //System.out.println(client.getReplyString());
                client.sendSimpleMessage("postmaster@localhost", "test@localhost", mail);
                boss.delivered();
            }
            client.disconnect();
            String[] outs = client.getReplyStrings();
            for (int i = 0; i < outs.length; i++) {
                System.out.println(outs[i]);
            }
        } catch (IOException e) {
            System.err.println("argh! "+workerid+" "+e.getMessage());
        }
        boss.finished(workerid);
    }

    /**
     * Set the worker id for this worker thread.
     *
     * @param workerid the worker thread id
     */
    public void setWorkerid(int workerid) {
        this.workerid = workerid;
    }

    /**
     * Get the worker id for this worker thread.
     *
     * @return the worker thread id
     */
    public int getWorkerid() {
        return workerid;
    }
}
