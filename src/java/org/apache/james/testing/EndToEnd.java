/**
 * RemoteManagerEndToEnd.java
 * 
 * Copyright (C) 27-Sep-2002 The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file. 
 *
 * Danny Angus
 */
package org.apache.james.testing;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.SocketException;
import java.util.Date;
import junit.framework.TestCase;
import org.apache.commons.net.pop3.POP3Client;
import org.apache.commons.net.smtp.SMTPClient;
import org.apache.commons.net.telnet.TelnetClient;
import examples.IOUtil;
/**
 * @author <A href="mailto:danny@apache.org">Danny Angus</a>
 * 
 * $Id: EndToEnd.java,v 1.1 2002/10/13 08:17:03 danny Exp $
 */
public class EndToEnd extends TestCase {
    /**
     * Constructor for RemoteManagerEndToEnd.
     * @param arg0
     */
    private int numworkers = 10;
    private int messagesize =  1024*10;
    private int workingworkers;
    private Date start;
    private String[] script1 =
        { "root", "root", "help", "adduser test test", "listusers", "quit" };
    private String[] script2 =
        { "root", "root", "listusers", "deluser test", "listusers", "quit" };
    private boolean finished = false;
    private int delivered = 0;
    public EndToEnd(String arg0) {
        super(arg0);
    }
    public static void main(String[] args) {
        junit.textui.TestRunner.run(EndToEnd.class);
    }
    public void testEndToEnd() {
        TelnetClient client = new TelnetClient();
        BufferedReader in;
        OutputStreamWriter out;
        try {
            client.setDefaultTimeout(500);
            client.connect("127.0.0.1", 4555);
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            OutputStream oo = client.getOutputStream();
            out = new OutputStreamWriter(oo);
            print(in, System.out);
            for (int i = 0; i < script1.length; i++) {
                out.write(script1[i] + "\n");
                System.out.println(" " + script1[i] + " \n");
                out.flush();
                print(in, System.out);
            }
            mailTest();
            client.disconnect();
        } catch (SocketException e) {
            e.printStackTrace();
            assertTrue(false);
        } catch (IOException e) {
            e.printStackTrace();
            assertTrue(false);
        }
    }
    private void echo(OutputStreamWriter out) {
    }
    private String print(BufferedReader in, OutputStream output) {
        String outString = "";
        try {
            String readString = in.readLine();
            while (readString != null) {
                outString += readString + "\n";
                readString = in.readLine();
            }
        } catch (IOException e) {
            //e.printStackTrace();
            //assertTrue(false);
        }
        System.out.println(outString);
        return outString + "==";
    }
    private void mailTest() {
        start = new Date();
        StringBuffer mail1 = new StringBuffer();
        mail1.append(
            "Subject: test\nFrom: postmaster@localhost\nTo: test@localhost\n\nTHIS IS A TEST");
        for (int kb = 0; kb < messagesize; kb++) {
            mail1.append("m");
        }
        String mail = mail1.toString();
        SMTPDeliveryWorker[] workers = new SMTPDeliveryWorker[numworkers];
        Thread[] threads = new Thread[workers.length];
        for (int i = 0; i < workers.length; i++) {
            workers[i] = new SMTPDeliveryWorker(new SMTPClient(), mail, this);
            workers[i].setWorkerid(i);
            threads[i] = new Thread((SMTPDeliveryWorker) workers[i]);
        }
        for (int i = 0; i < workers.length; i++) {
            System.out.println("starting worker:" + i);
            ((Thread) threads[i]).start();
            workingworkers++;
        }
        while (!finished) {
        }
        try {
            POP3Client pclient = new POP3Client();
            pclient.connect("127.0.0.1", 110);
            System.out.println(pclient.getReplyString());
            pclient.login("test", "test");
            System.out.println(pclient.getReplyString());
            pclient.setState(pclient.TRANSACTION_STATE);
            pclient.listMessages();
            System.out.println(pclient.getReplyString());
            pclient.disconnect();
            System.out.println(pclient.getReplyString());
        } catch (SocketException e) {
        } catch (IOException e) {
        }
        long time = (new Date()).getTime() - start.getTime();
        System.err.println("time total " + (int) time);
    }
    public void finished(int workerid) {
        workingworkers--;
        System.out.println("workers still working.." + workingworkers);
        if (workingworkers == 0) {
            long time = (new Date()).getTime() - start.getTime();
            System.err.println("time to deliver set " + (int) (time/1000));
            //System.err.println("messages per second " + (int)(1000/(time/1000)));
            //System.err.println("data rate="+((messagesize*1000)/(time/1000)));
            finished = true;
        }
    }
    public void delivered() {
        System.out.println("-" + (++delivered));
    }
}
