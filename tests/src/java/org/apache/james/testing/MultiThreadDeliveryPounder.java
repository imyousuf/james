/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
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

package org.apache.james.testing;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;

/**
 * Creates numerous threads
 *
 * @author Serge Knystautas <sergek@lokitech.com>
 */
public class MultiThreadDeliveryPounder extends Thread {

    /**
     * The number of loops to be executed by the thread.
     */
    int loops = 0;

    /**
     * The user to whom emails will be sent.
     */
    String user = null;

    /**
     * The prefix for the test message body.  The body will correspond
     * to this string with a number appended.
     */
    private static final String body = "Test message number: ";

    /**
     * Sole constructor for this class.
     *
     * @param loops the number of loops the thread should execute.
     * @param user the user to whom the emails will be sent.
     */
    public MultiThreadDeliveryPounder(int loops, String user) {
        this.loops = loops;
        this.user = user;

        start();
    }

    /**
     * Executes a fixed number of loops, emailing the user each loop.
     */
    public void run() {
        try {
            Properties prop = new Properties();
            Session session = Session.getDefaultInstance(prop, null);
            // Transport transport = session.getTransport("smtp");

            for (int i = 0; i < loops; i++) {
                MimeMessage msg = new MimeMessage(session);
                msg.setFrom(new InternetAddress(user + "@localhost"));
                msg.addRecipient(Message.RecipientType.TO, new InternetAddress(user + "@localhost"));
                msg.setContent(body + i, "text/plain");
                Transport.send(msg);
                StringBuffer outputBuffer =
                    new StringBuffer(256)
                        .append("Sent message : ")
                        .append(msg.getContent())
                        .append(" from: ")
                        .append(msg.getFrom()[0])
                        .append(" To: ")
                        .append(msg.getAllRecipients()[0]);
                System.out.println(outputBuffer.toString());
            }
        } catch (Throwable e) {
            e.printStackTrace();
            //System.exit(1);
        }
    }

    /**
     * Executes the test, creating a number of worker threads, each of whom 
     * will loop a fixed number of times, sending emails to the specified user
     * on each loop.
     *
     * @param args the number of threads, the number of loops to execute,
     *             the user to whom to send.
     */
    public static void main(String[] args) throws Throwable {
        if (args.length != 3) {
            System.err.println("Usage: ");
            System.err.println(" java org.apache.james.testing.MultiThreadDeliveryPounder <threadcount> <loops> <user>");
            System.exit(1);
        }
        int threadCount = Integer.parseInt(args[0]);
        int loops = Integer.parseInt(args[1]);
        String user = args[2];

        Collection threads = new Vector();
        long start = System.currentTimeMillis();
        for (int i = 0; i < threadCount; i++) {
            threads.add(new MultiThreadDeliveryPounder(loops, user));
        }

        for (Iterator i = threads.iterator(); i.hasNext(); ) {
            Thread t = (Thread)i.next();
            t.join();
        }
        long end = System.currentTimeMillis();
        System.out.println((end - start) + " milliseconds");
    }
}
