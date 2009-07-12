/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/
package org.apache.james.core;

import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.mailet.RFC2822Headers;

import javax.mail.BodyPart;
import javax.mail.Session;
import javax.mail.internet.InternetHeaders;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Properties;

import junit.framework.TestCase;

/**
 * Test the subject folding issue.
 */
public class MimeMessageTest extends TestCase {

    protected MimeMessage getSimpleMessage() throws Exception {
        MimeMessage mmCreated = new MimeMessage(Session.getDefaultInstance(new Properties()));
        mmCreated.setSubject("test");
        mmCreated.setText("test body");
        mmCreated.saveChanges();
        return mmCreated;
    }
    
    protected String getSimpleMessageCleanedSource() throws Exception {
        return "Subject: test\r\n"
            +"MIME-Version: 1.0\r\n"
            +"Content-Type: text/plain; charset=us-ascii\r\n"
            +"Content-Transfer-Encoding: 7bit\r\n"
            +"\r\n"
            +"test body";
    }
    

    protected MimeMessage getMessageWithBadReturnPath() throws Exception {
        MimeMessage mmCreated = new MimeMessage(Session.getDefaultInstance(new Properties()));
        mmCreated.setSubject("test");
        mmCreated.setHeader(RFC2822Headers.RETURN_PATH, "<mybadreturn@example.com>");
        mmCreated.setText("test body");
        mmCreated.saveChanges();
        return mmCreated;
    }
    
    protected String getMessageWithBadReturnPathSource() throws Exception {
        return "Subject: test\r\n"
            +"Return-Path: <mybadreturn@example.com>\r\n"
            +"MIME-Version: 1.0\r\n"
            +"Content-Type: text/plain; charset=us-ascii\r\n"
            +"Content-Transfer-Encoding: 7bit\r\n"
            +"\r\n"
            +"test body";
    }
    
    protected String getSimpleMessageCleanedSourceHeaderExpected() throws Exception {
        return "X-Test: foo\r\n"+getSimpleMessageCleanedSource();
    }
    
    /*
     * Class under test for String getSubject()
     */
    public void testSimpleMessage() throws Exception {
        assertEquals(getSimpleMessageCleanedSource(), getCleanedMessageSource(getSimpleMessage()));
    }
    
    
    protected MimeMessage getMultipartMessage() throws Exception {
        MimeMessage mmCreated = new MimeMessage(Session.getDefaultInstance(new Properties()));
        mmCreated.setSubject("test");
        MimeMultipart mm = new MimeMultipart("alternative");
        mm.addBodyPart(new MimeBodyPart(new InternetHeaders(new ByteArrayInputStream("X-header: test1\r\nContent-Type: text/plain; charset=Cp1252\r\n".getBytes())),"first part ???".getBytes()));
        mm.addBodyPart(new MimeBodyPart(new InternetHeaders(new ByteArrayInputStream("X-header: test2\r\nContent-Type: text/plain; charset=Cp1252\r\nContent-Transfer-Encoding: quoted-printable\r\n".getBytes())),"second part =E8=E8".getBytes()));
        mmCreated.setContent(mm);
        mmCreated.saveChanges();
        return mmCreated;
    }
    
    protected String getMultipartMessageSource() {
        return "Subject: test\r\n"
            +"MIME-Version: 1.0\r\n"
            +"Content-Type: multipart/alternative; \r\n" 
            +"\tboundary=\"----=_Part_0_XXXXXXXXXXX.XXXXXXXXXXX\"\r\n"
            +"\r\n"
            +"------=_Part_0_XXXXXXXXXXX.XXXXXXXXXXX\r\n"
            +"X-header: test1\r\n"
            +"Content-Type: text/plain; charset=Cp1252\r\n"
            +"Content-Transfer-Encoding: quoted-printable\r\n"
            +"\r\n"
            +"first part =E8\r\n"
            +"------=_Part_0_XXXXXXXXXXX.XXXXXXXXXXX\r\n"
            +"X-header: test2\r\n"
            +"Content-Type: text/plain; charset=Cp1252\r\n"
            +"Content-Transfer-Encoding: quoted-printable\r\n"
            +"\r\n"
            +"second part =E8=E8\r\n"
            +"------=_Part_0_XXXXXXXXXXX.XXXXXXXXXXX--\r\n";
    }
    
    protected String getMultipartMessageExpected1() {
        return "Subject: test\r\n"
            +"MIME-Version: 1.0\r\n"
            +"Content-Type: multipart/alternative; \r\n" 
            +"\tboundary=\"----=_Part_0_XXXXXXXXXXX.XXXXXXXXXXX\"\r\n"
            +"\r\n"
            +"------=_Part_0_XXXXXXXXXXX.XXXXXXXXXXX\r\n"
            +"X-header: test1\r\n"
            +"Content-Type: text/plain; charset=Cp1252\r\n"
            +"Content-Transfer-Encoding: quoted-printable\r\n"
            +"\r\n"
            +"test=80\r\n"
            +"------=_Part_0_XXXXXXXXXXX.XXXXXXXXXXX\r\n"
            +"X-header: test2\r\n"
            +"Content-Type: text/plain; charset=Cp1252\r\n"
            +"Content-Transfer-Encoding: quoted-printable\r\n"
            +"\r\n"
            +"second part =E8=E8\r\n"
            +"------=_Part_0_XXXXXXXXXXX.XXXXXXXXXXX--\r\n";
    }
    
    protected String getMultipartMessageExpected2() {
        return "Subject: test\r\n"
            +"MIME-Version: 1.0\r\n"
            +"Content-Type: multipart/alternative; \r\n" 
            +"\tboundary=\"----=_Part_0_XXXXXXXXXXX.XXXXXXXXXXX\"\r\n"
            +"\r\n"
            +"------=_Part_0_XXXXXXXXXXX.XXXXXXXXXXX\r\n"
            +"X-header: test1\r\n"
            +"Content-Type: text/plain; charset=Cp1252\r\n"
            +"Content-Transfer-Encoding: quoted-printable\r\n"
            +"\r\n"
            +"test=80\r\n"
            +"------=_Part_0_XXXXXXXXXXX.XXXXXXXXXXX\r\n"
            +"X-header: test2\r\n"
            +"Content-Type: text/plain; charset=Cp1252\r\n"
            +"Content-Transfer-Encoding: quoted-printable\r\n"
            +"\r\n"
            +"second part =E8=E8\r\n"
            +"------=_Part_0_XXXXXXXXXXX.XXXXXXXXXXX\r\n"
            +"Subject: test3\r\n"
            +"Content-Transfer-Encoding: 7bit\r\n"
            +"Content-Type: text/plain; charset=us-ascii\r\n"
            +"\r\n"
            +"second part\r\n"
            +"------=_Part_0_XXXXXXXXXXX.XXXXXXXXXXX--\r\n";
    }
    
    protected String getMultipartMessageExpected3() {
        return "Subject: test\r\n"
            +"MIME-Version: 1.0\r\n"
            +"Content-Type: binary/octet-stream\r\n"
            +"Content-Transfer-Encoding: quoted-printable\r\n"
            +"\r\n"
            +"mynewco=F2=E0=F9ntent=80=E0!";
    }
    
    /*
     * Class under test for String getSubject()
     */
    public void testMultipartMessageChanges() throws Exception {

        MimeMessage mm = getMultipartMessage();
        
//        ByteArrayOutputStream out = new ByteArrayOutputStream();
//        mmCreated.writeTo(out,new String[] {"Message-ID"});
//        String messageSource = out.toString();
//        System.out.println(messageSource);
        
        
        MimeMultipart content1 = (MimeMultipart) mm.getContent();
        BodyPart b1 = content1.getBodyPart(0);
        b1.setContent("test\u20AC","text/plain; charset=Cp1252");
        mm.setContent(content1,mm.getContentType());
        //.setHeader(RFC2822Headers.CONTENT_TYPE,contentType);
        mm.saveChanges();

        assertEquals(getMultipartMessageExpected1(),getCleanedMessageSource(mm));

        MimeMultipart content2 = (MimeMultipart) mm.getContent();
        content2.addBodyPart(new MimeBodyPart(new InternetHeaders(new ByteArrayInputStream("Subject: test3\r\n".getBytes())),"second part".getBytes()));
        mm.setContent(content2,mm.getContentType());
        mm.saveChanges();

        assertEquals(getMultipartMessageExpected2(),getCleanedMessageSource(mm));

        mm.setContent("mynewco\u00F2\u00E0\u00F9ntent\u20AC\u00E0!","text/plain; charset=cp1252");
        mm.setHeader(RFC2822Headers.CONTENT_TYPE,"binary/octet-stream");
        //mm.setHeader("Content-Transfer-Encoding","8bit");
        mm.saveChanges();
        
        assertEquals(getMultipartMessageExpected3(),getCleanedMessageSource(mm));
        
        ContainerUtil.dispose(mm);
        
    }

    protected MimeMessage getMissingEncodingAddHeaderMessage() throws Exception {
        MimeMessage m = new MimeMessage(Session.getDefaultInstance(new Properties()));
        m.setText("Test\u00E0\r\n");
        m.setSubject("test");
        m.saveChanges();
        return m;
    }
    

    protected String getMissingEncodingAddHeaderSource() {
        return "Subject: test\r\n"+
                "\r\n"+
                "Test\u00E0\r\n";
    }
    
    protected String getMissingEncodingAddHeaderExpected() {
        return "Subject: test\r\n"
            +"MIME-Version: 1.0\r\n"
            +"Content-Type: text/plain; charset=Cp1252\r\n"
            +"Content-Transfer-Encoding: quoted-printable\r\n"
            +"\r\n"
            +"Test=E0\r\n";
    }
    

    /**
     * This test is not usable in different locale environment.
     */
    /*
    public void testMissingEncodingAddHeader() throws Exception {
        
        
        MimeMessage mm = getMissingEncodingAddHeaderMessage();
        mm.setHeader("Content-Transfer-Encoding", "quoted-printable");
        mm.saveChanges();

        assertEquals(getMissingEncodingAddHeaderExpected(),getCleanedMessageSource(mm));
    }
    */
    

    protected String getCleanedMessageSource(MimeMessage mm) throws Exception {
        ByteArrayOutputStream out2;
        out2 = new ByteArrayOutputStream();
        mm.writeTo(out2,new String[] {"Message-ID"});

        String res = out2.toString();

        int p = res.indexOf("\r\n\r\n");
        if (p > 0) {
            String head = res.substring(0,p);
            String[] str = head.split("\r\n");
            Arrays.sort(str);
            StringBuffer outputHead = new StringBuffer();
            for (int i = str.length-1; i >= 0; i--) {
                outputHead.append(str[i]);
                outputHead.append("\r\n");
            }
            outputHead.append(res.substring(p+2));
            res = outputHead.toString();
        }
        
        res = res.replaceAll("----=_Part_\\d+_\\d+\\.\\d+","----=_Part_\\0_XXXXXXXXXXX.XXXXXXXXXXX");
        return res;
    }
    
    protected void debugMessage(MimeMessage mm) throws Exception {
        System.out.println("-------------------");
        System.out.println(getCleanedMessageSource(mm));
        System.out.println("-------------------");
    }
    

    protected MimeMessage getMissingEncodingMessage() throws Exception {
        MimeMessage mmCreated = new MimeMessage(Session.getDefaultInstance(new Properties()));
        mmCreated.setSubject("test");
        MimeMultipart mm = new MimeMultipart("alternative");
        mm.addBodyPart(new MimeBodyPart(new InternetHeaders(new ByteArrayInputStream("X-header: test2\r\nContent-Type: text/plain; charset=Cp1252\r\nContent-Transfer-Encoding: quoted-printable\r\n".getBytes())),"second part =E8=E8".getBytes()));
        mmCreated.setContent(mm);
        mmCreated.saveChanges();
        return mmCreated;
    }
    

    protected String getMissingEncodingMessageSource() {
        return "Subject: test\r\n"
        +"MIME-Version: 1.0\r\n"
        +"Content-Type: multipart/alternative; \r\n" 
        +"\tboundary=\"----=_Part_0_XXXXXXXXXXX.XXXXXXXXXXX\"\r\n"
        +"\r\n"
        +"------=_Part_0_XXXXXXXXXXX.XXXXXXXXXXX\r\n"
        +"X-header: test2\r\n"
        +"Content-Type: text/plain; charset=Cp1252\r\n"
        +"Content-Transfer-Encoding: quoted-printable\r\n"
        +"\r\n"
        +"second part =E8=E8\r\n"
        +"------=_Part_0_XXXXXXXXXXX.XXXXXXXXXXX--\r\n";
    }
    

    public void testGetLineCount() throws Exception {
        MimeMessage mm = getMissingEncodingMessage();
        try {
            int count = mm.getLineCount();
            assertTrue(count == -1 || count == 7);
        } catch (Exception e) {
            fail("Unexpected exception in getLineCount");
        }
        ContainerUtil.dispose(mm);
    }
    
    /**
     * This test throw a NullPointerException when the original message was created by
     * a MimeMessageInputStreamSource.
     */
    public void testMessageCloningViaCoW() throws Exception {
        MimeMessage mmorig = getSimpleMessage();
        
        MimeMessage mm = new MimeMessageCopyOnWriteProxy(mmorig);

        MimeMessage mm2 = new MimeMessageCopyOnWriteProxy(mm);

        mm2.setHeader("Subject", "Modified");
        
        ContainerUtil.dispose(mm2);
        System.gc();
        Thread.sleep(200);
        //((Disposable)mail_dup.getMessage()).dispose();
        
        mm.setHeader("Subject", "Modified");
        
        ContainerUtil.dispose(mm);
        ContainerUtil.dispose(mmorig);
    }
    
    /**
     * This test throw a NullPointerException when the original message was created by
     * a MimeMessageInputStreamSource.
     */
    public void testMessageCloningViaCoW2() throws Exception {
        MimeMessage mmorig = getSimpleMessage();
        
        MimeMessage mm = new MimeMessageCopyOnWriteProxy(mmorig);
        
        MimeMessage mm2 = new MimeMessageCopyOnWriteProxy(mm);
        
        ContainerUtil.dispose(mm);
        mm = null;
        System.gc();
        Thread.sleep(200);

        try {
            mm2.writeTo(System.out);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception while writing the message to output");
        }
        
        ContainerUtil.dispose(mm2);
        ContainerUtil.dispose(mmorig);
    }

    
    /**
     * This test throw a NullPointerException when the original message was created by
     * a MimeMessageInputStreamSource.
     */
    public void testMessageCloningViaCoWSubjectLost() throws Exception {
        MimeMessage mmorig = getSimpleMessage();
        
        MimeMessage mm = new MimeMessageCopyOnWriteProxy(mmorig);

        mm.setHeader("X-Test", "foo");
        mm.saveChanges();
        
        assertEquals(getSimpleMessageCleanedSourceHeaderExpected(),getCleanedMessageSource(mm));

        ContainerUtil.dispose(mm);
        ContainerUtil.dispose(mmorig);
    }
    
    public void testReturnPath() throws Exception {
        MimeMessage message = getSimpleMessage();
        assertNull(message.getHeader(RFC2822Headers.RETURN_PATH));
    }
    
    public void testHeaderOrder() throws Exception {
        MimeMessage message = getSimpleMessage();
        message.setHeader(RFC2822Headers.RETURN_PATH, "<test@test.de>");
        Enumeration h =  message.getAllHeaderLines();
        
        assertEquals(h.nextElement(),"Return-Path: <test@test.de>");
    }

}
