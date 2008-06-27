/***********************************************************************
 * Copyright (c) 2000-2004 The Apache Software Foundation.             *
 * All rights reserved.                                                *
 * ------------------------------------------------------------------- *
 * Licensed under the Apache License, Version 2.0 (the "License"); you *
 * may not use this file except in compliance with the License. You    *
 * may obtain a copy of the License at:                                *
 *                                                                     *
 *     http://www.apache.org/licenses/LICENSE-2.0                      *
 *                                                                     *
 * Unless required by applicable law or agreed to in writing, software *
 * distributed under the License is distributed on an "AS IS" BASIS,   *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or     *
 * implied.  See the License for the specific language governing       *
 * permissions and limitations under the License.                      *
 ***********************************************************************/

package org.apache.james.testing;


import java.io.IOException;
import org.apache.commons.net.smtp.SMTPClient;


/**
 * SMTPDeliveryWorker.java
 *
 * A worker thread that is part of the SMTP delivery end to end testing.
 *
 * $Id: SMTPDeliveryWorker.java,v 1.7 2004/01/30 02:22:20 noel Exp $
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
            for (int run = 0; run < 100; run++) {
                // move the connect and disconnect out of the loop to reuse
                // the connection for all mail
                client.connect("127.0.0.1", 25);
                client.sendSimpleMessage("postmaster@localhost", "test@localhost", mail);
                boss.delivered();
                client.disconnect();
            }
            String[] outs = client.getReplyStrings();

            for (int i = 0; i < outs.length; i++) {
                System.out.println(outs[i]);
            }
        } catch (IOException e) {
            System.err.println("argh! " + workerid + " " + e.getMessage());
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
