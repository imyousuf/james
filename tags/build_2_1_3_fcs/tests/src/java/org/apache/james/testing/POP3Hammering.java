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

package org.apache.james.testing;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

/**
 * Program that can be run multiple times to recreate the
 * "stuck file" issue in Windows.
 *
 * @author Prasanna Uppaladadium <prasanna@vayusphere.com>
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
            System.out.println("Sent message : " + msg.getContent() +
                    " from: " + msg.getFrom()[0] + " To: " +
                    msg.getAllRecipients()[0]);
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

            if(folder == null || !folder.exists()) {
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

            if(delete) {
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
