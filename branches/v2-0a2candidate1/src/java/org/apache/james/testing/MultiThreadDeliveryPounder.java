/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.testing;

import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;

/**
 * Creates numerous threads
 *
 * @author Serge Knystautas <sergek@lokitech.com>
 */
public class MultiThreadDeliveryPounder extends Thread {
    int loops = 0;
    String user = null;
    Properties prop = new Properties();
    private static final String body = "Test message number: ";

    public MultiThreadDeliveryPounder(int loops, String user) {
        this.loops = loops;
        this.user = user;

        start();
    }

    public void run() {
        try {
            Session session = Session.getDefaultInstance(prop, null);
            // Transport transport = session.getTransport("smtp");

            for (int i = 0; i < loops; i++) {
                MimeMessage msg = new MimeMessage(session);
                msg.setFrom(new InternetAddress(user + "@localhost"));
                msg.addRecipient(Message.RecipientType.TO, new InternetAddress(user + "@localhost"));
                msg.setContent(body + i, "text/plain");
                Transport.send(msg);
                System.out.println("Sent message : " + msg.getContent() +
                        " from: " + msg.getFrom()[0] + " To: " +
                        msg.getAllRecipients()[0]);
            }
        } catch (Throwable e) {
            e.printStackTrace();
            //System.exit(1);
        }
    }

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
