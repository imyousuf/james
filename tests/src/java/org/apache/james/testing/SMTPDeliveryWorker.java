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
 * $Id: SMTPDeliveryWorker.java,v 1.3.2.2 2003/03/08 21:53:49 noel Exp $
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
                //move the connect and disconnect out of the loop to reuse 
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
