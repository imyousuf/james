/**
 * SMTPDeliveryWorker.java
 * 
 * Copyright (C) 11-Oct-2002 The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file. 
 *
 * Danny Angus
 */
package org.apache.james.testing;
import java.io.IOException;
import org.apache.commons.net.smtp.SMTPClient;
/**
 * @author <A href="mailto:danny@apache.org">Danny Angus</a>
 * 
 * $Id: SMTPDeliveryWorker.java,v 1.1 2002/10/13 08:17:04 danny Exp $
 */
public class SMTPDeliveryWorker implements Runnable {
    /**
     * Constructor for SMTPDeliveryWorker.
     */
    private SMTPClient client;
    private String mail;
    private int workerid;
    EndToEnd boss;
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
    public void setWorkerid(int workerid) {
        this.workerid = workerid;
    }
    public int getWorkerid() {
        return workerid;
    }
}
