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

package org.apache.james.transport.mailets;

import org.apache.mailet.GenericMailet;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;

import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import java.util.Collection;
import java.util.HashSet;
import java.util.StringTokenizer;

/**
 * <P>Replaces incoming recipients with those specified, and resends the message unaltered.</P>
 * <P>Can be totally replaced by an equivalent usage of {@link Resend} (see below),
 * simply replacing <I>&lt;forwardto&gt;</I> with <I>&lt;recipients&gt</I>.
 *
 * <P>Sample configuration:</P>
 * <PRE><CODE>
 * &lt;mailet match="All" class="Forward">
 *   &lt;forwardto&gt;<I>comma delimited list of email addresses</I>&lt;/forwardto&gt;
 *   &lt;passThrough&gt;<I>true or false, default=false</I>&lt;/passThrough&gt;
 *   &lt;fakeDomainCheck&gt;<I>true or false, default=true</I>&lt;/fakeDomainCheck&gt;
 *   &lt;debug&gt;<I>true or false, default=false</I>&lt;/debug&gt;
 * &lt;/mailet&gt;
 * </CODE></PRE>
 *
 * <P>The behaviour of this mailet is equivalent to using Resend with the following
 * configuration:</P>
 * <PRE><CODE>
 * &lt;mailet match="All" class="Resend">
 *   &lt;recipients&gt;comma delimited list of email addresses&lt;/recipients&gt;
 *   &lt;passThrough&gt;true or false&lt;/passThrough&gt;
 *   &lt;fakeDomainCheck&gt;<I>true or false</I>&lt;/fakeDomainCheck&gt;
 *   &lt;debug&gt;<I>true or false</I>&lt;/debug&gt;
 * &lt;/mailet&gt;
 * </CODE></PRE>
 *
 * @version CVS $Revision: 1.15 $ $Date: 2003/06/27 14:25:47 $
 */
public class Forward extends AbstractRedirect {

    /**
     * Return a string describing this mailet.
     *
     * @return a string describing this mailet
     */
    public String getMailetInfo() {
        return "Forward Mailet";
    }

    /** Gets the expected init parameters. */
    protected  String[] getAllowedInitParameters() {
        String[] allowedArray = {
//            "static",
            "debug",
            "passThrough",
            "fakeDomainCheck",
            "forwardto",
        };
        return allowedArray;
    }

    /* ******************************************************************** */
    /* ****************** Begin of getX and setX methods ****************** */
    /* ******************************************************************** */

    /**
     * @return UNALTERED
     */
    protected int getInLineType() throws MessagingException {
        return UNALTERED;
    }

    /**
     * @return NONE
     */
    protected int getAttachmentType() throws MessagingException {
        return NONE;
    }

    /**
     * @return ""
     */
    protected String getMessage() throws MessagingException {
        return "";
    }

    /**
     * @return the <CODE>recipients</CODE> init parameter or null if missing
     */
    protected Collection getRecipients() throws MessagingException {
        Collection newRecipients = new HashSet();
        String addressList = getInitParameter("forwardto");
        // if nothing was specified, return null meaning no change
        if (addressList == null) {
            return null;
        }
        StringTokenizer st = new StringTokenizer(addressList, ",", false);
        while(st.hasMoreTokens()) {
            try {
                newRecipients.add(new MailAddress(st.nextToken()));
            } catch(Exception e) {
                log("add recipient failed in getRecipients");
            }
        }
        return newRecipients;
    }

    /**
     * @return null
     */
    protected InternetAddress[] getTo() throws MessagingException {
        return null;
    }

    /**
     * @return null
     */
    protected MailAddress getReplyTo() throws MessagingException {
        return null;
    }

    /**
     * @return null
     */
    protected MailAddress getReturnPath() throws MessagingException {
        return null;
    }

    /**
     * @return null
     */
    protected MailAddress getSender() throws MessagingException {
        return null;
    }

    /**
     * @return null
     */
    protected String getSubject() throws MessagingException {
        return null;
    }

    /**
     * @return ""
     */
    protected String getSubjectPrefix() throws MessagingException {
        return "";
    }

    /**
     * @return false
     */
    protected boolean attachError() throws MessagingException {
        return false;
    }

    /**
     * @return false
     */
    protected boolean isReply() throws MessagingException {
        return false;
    }

    /* ******************************************************************** */
    /* ******************* End of getX and setX methods ******************* */
    /* ******************************************************************** */

}

