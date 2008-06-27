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


import java.util.Properties;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;


/**
 * Program that can be run multiple times to recreate the
 * "stuck file" issue in Windows.
 *
 */
public class POP3Hammering {

    /**
     * The POP3 host to be tested.
     */
    private String mailHost;

    /**
     * The name of the user account being used to send and receive
     * the test emails.
     */
    private String user;

    /**
     * The password of the user account being used to send the test emails.
     */
    private String password;

    /**
     * The prefix for the test message body.  The body will correspond
     * to this string with a number appended.
     */
    private static final String body = "Test message number: ";

    /**
     * The number of the current test mail.
     */
    private int mailNumber;

    /**
     * The sole constructor for this class.
     *
     * @param host the name of the mail host
     * @param user the user name that will be both the sender and recipient of the mails.
     * @param password the user password
     */
    public POP3Hammering(String host, String user, String password) {
        this.mailHost = host;
        this.user = user;
        this.password = password;
        mailNumber = 0;
    }

    /**
     * Sends a test mail to the user account.
     */
    void sendMail() {
        try {
            Properties prop = new Properties();

            prop.put("java.smtp.host", mailHost);
            Session session = Session.getDefaultInstance(prop, null);
            // Transport transport = session.getTransport("smtp");
            MimeMessage msg = new MimeMessage(session);

            msg.setFrom(new InternetAddress(user + "@localhost"));
            msg.addRecipient(Message.RecipientType.TO, new InternetAddress(user + "@localhost"));
            msg.setContent(body + ++mailNumber, "text/plain");
            Transport.send(msg);
            // transport.close();
            System.out.println("Sent message : " + msg.getContent() + " from: " + msg.getFrom()[0] + " To: " + msg.getAllRecipients()[0]);
        } catch (Throwable e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    /**
     * Checks to see if an email has been received.
     *
     * @param delete whether the email is to be deleted.
     */
    void receiveMail(boolean delete) {
        Store store = null;
        Folder folder = null;

        try {
            Properties prop = new Properties();

            prop.put("java.smtp.host", mailHost);
            Session session = Session.getDefaultInstance(prop, null);

            store = session.getStore("pop3");
            store.connect(mailHost, user, password);

            folder = store.getFolder("INBOX");

            if (folder == null || !folder.exists()) {
                System.out.println("This folder does not exist.");
                return;
            }

            folder.open(Folder.READ_WRITE);

            Message[] msgs = folder.getMessages();

            System.out.println("Received " + msgs.length + " messages for " + user);
            Message msg = msgs[0];

            System.out.println("From: " + msg.getFrom()[0].toString());
            System.out.println("To: " + msg.getRecipients(Message.RecipientType.TO)[0]);
            System.out.println("-------------------");
            System.out.println(msg.getContent().toString());

            if (delete) {
                msg.setFlag(Flags.Flag.DELETED, true);
                System.out.println("Deleted.");
            }
            folder.close(true);
            store.close();
        } catch (MessagingException e) {
            e.printStackTrace();
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            try {
                if (folder != null) {
                    folder.close(true);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                if (store != null) {
                    store.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Executes the body of the test, sending two mails and checking
     * the destination inbox to confirm that the mails were received.
     *
     * @param args the host name, user id, and password
     */
    public static void main(String[] args) throws Throwable {
        POP3Hammering tester = new POP3Hammering(args[0], args[1], args[2]);

        tester.sendMail();
        tester.sendMail();

        tester.receiveMail(true);
        tester.receiveMail(true);
    }
}
