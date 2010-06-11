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

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Date;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.commons.net.SocketClient;
import org.apache.commons.net.smtp.SMTP;
import org.apache.commons.net.smtp.SMTPClient;
import org.apache.james.util.RFC822DateFormat;

/**
 * Send email. Can be configured and extended to test specific SMTP
 * operations.
 */
public class SMTPTest extends BaseTest {

    /**
     * The SMTP host to be tested.
     */
    private String host;

    /**
     * The name of the user account being used to send the test emails.
     */
    private String username;

    /**
     * The password of the user account being used to send the test emails.
     */
    private String password;

    /** mail from */
    private String from;
    /** mail to */
    private String[] to;
    /** send mail msg copy to */
    private String[] cc;
    /** send mail msg blind copy to */
    private String[] bcc;
    /** mail message */
    private String mailMsg;

    /**
     * Sole constructor for the test case.
     *
     * @param name the name of the test case
     */
    public SMTPTest(String name) {
        super(name);
    }

    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(Configuration)
     */
    public void configure(Configuration configuration) 
        throws ConfigurationException {
        this.host = configuration.getChild("host").getValue();
        this.username = configuration.getChild("username").getValue(null);
        this.password = configuration.getChild("password").getValue(null);
        this.from = configuration.getChild("from").getValue();
        this.to = getChildrenValues(configuration,"to");
        this.cc = getChildrenValues(configuration,"cc");
        this.bcc = getChildrenValues(configuration,"bcc");
        int msgSize = configuration.getChild("msgsize").getValueAsInteger(1000);
        String subject = configuration.getChild("subject").getValue("");
        this.mailMsg = createMailMsg(subject,msgSize);
        super.configure(configuration);
    }

    /**
     * SMTP client that provides a simple interface to the SMTP interaction
     * from the client side.
     */
    private SMTPClient client;

    /**
     * Sets up the test case by creating the SMTP client and connecting
     * to the host.
     */
    protected void setUp() throws Exception {
        client = new SMTPClient();
    }

    /**
     * The number of messages sent
     */
    private static int msgSentCounter;

    /**
     * The number of failed msgs
     */
    private static int failureCounter;

    /**
     * Send Mail message
     */
    public void sendMsg() throws Exception {
        try {
            client.connect(host);
            // HBNOTE: extend this to do to, cc, bcc.
            client.sendSimpleMessage("postmaster@localhost", to[0], mailMsg);
            System.out.println("msgs sent="+(++msgSentCounter));
        } catch( Throwable t) {
            System.out.println("msg send failures="+(++failureCounter));
        } finally {
            try {
                client.disconnect();
            } catch(Throwable t) { }
        }
    }

    // ------ helper methods ------

    /** 
     * @param arr of strings
     * @param sep separator character.
     * @return concatenates a an array of strings. 
     */
    private String toString(String[] arr,char sep) {
        StringBuffer buf = new StringBuffer();
        for ( int i = 0 ; i < arr.length ; i++ ) {
            if ( i > 0 )
                buf.append(sep);
            buf.append(arr[i]);
        }
        return buf.toString();
    }
    private String createMailMsg(String subject,int msgSize) {
        StringWriter str = new StringWriter();
        final char[] CRLF = new char[] { '\r', '\n' };
        PrintWriter prt = new PrintWriter(str) {
                public void println() {
                    write(CRLF,0,CRLF.length);
                    flush();
                }
            };
        prt.println("From: "+from);
        String to = toString(this.to,';');
        if ( to.length() > 0 )
            prt.println("To: "+to);
        String cc = toString(this.cc,';');
        if ( cc.length() > 0 )
            prt.println("CC: "+cc);
        prt.println("Subject: "+subject);
        prt.println("Date: "+RFC822DateFormat.toString(new Date()));
        prt.println("MIME-Version: 1.0");
        prt.println("Content-Type: text/plain; charset=\"iso-8859-1\"");
        prt.println();
        char[] ca = new char[msgSize];
        for (int i = 0; i < msgSize; i++)
            ca[i] = 'm';
        prt.print(new String(ca));
        prt.flush();
        return str.toString();
    }
}
