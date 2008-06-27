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
                        new StringBuffer(256).append("Sent message : ").append(msg.getContent()).append(" from: ").append(msg.getFrom()[0]).append(" To: ").append(msg.getAllRecipients()[0]);

                System.out.println(outputBuffer.toString());
            }
        } catch (Throwable e) {
            e.printStackTrace();
            // System.exit(1);
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

        for (Iterator i = threads.iterator(); i.hasNext();) {
            Thread t = (Thread) i.next();

            t.join();
        }
        long end = System.currentTimeMillis();

        System.out.println((end - start) + " milliseconds");
    }
}
