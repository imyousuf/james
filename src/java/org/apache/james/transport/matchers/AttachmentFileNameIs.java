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
 
package org.apache.james.transport.matchers;

import org.apache.mailet.GenericMatcher;
import org.apache.mailet.Mail;

import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.MimeMessage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.StringTokenizer;


/**
 * Checks if at least one attachment has a file name which matches any
 * element of a comma-separated list of file name masks.
 * <P>File name masks may start with a wildcard * 
 * <p>Multiple file name masks can be specified, e.g.: '*.scr,*.bat'
 *
 * Possible enhancement: it should be case insensitive
 */
public class AttachmentFileNameIs extends GenericMatcher {
    /**
     * represents a single parsed file name mask
     */
    private static class Mask {
        /** true if the mask starts with a wildcard asterisk */
        public boolean suffixMatch;
        
        /** file name mask not including the wildcard asterisk */
        public String matchString;
    }
    
    /** contains ParsedMask instances, setup by init */
    private Mask[] masks = null;
    
    /** parses the condition */
    public void init() throws MessagingException {
        /** sets up fileNameMasks variable by parsing the condition */
        
        StringTokenizer st = new StringTokenizer(getCondition(), ", ", false);
        ArrayList theMasks = new ArrayList(20);
        while (st.hasMoreTokens()) {
            Mask mask = new Mask(); 
            String fileName = st.nextToken();
            if (fileName.startsWith("*")) {
                mask.suffixMatch = true;
                mask.matchString = fileName.substring(1);
            } else {
                mask.suffixMatch = false;
                mask.matchString = fileName;
            }
            theMasks.add(mask);
        }
        masks = (Mask[])theMasks.toArray(new Mask[0]);
    }

    /**
     * either every recipient is matching or neither of them
     */
    public Collection match(Mail mail) throws MessagingException {
        Multipart content;
        MimeMessage message = mail.getMessage();
        
        /**
         * if the content-type is not multipart/mixed, then there is no 
         * attachment 
         */ 
        if (message.getContentType() == null ||
                !message.getContentType().startsWith("multipart/mixed")) {
            return null;
        }
        try {
            content = (Multipart)message.getContent(); 
        } catch (java.io.IOException e) {
            throw new MessagingException(
                    "Attachment file names cannot be determined", e);
        }
        for (int i = 0; i < content.getCount(); i++) {
            Part part = content.getBodyPart(i);
            String fileName = part.getFileName();
            if (fileName == null) continue;
            for (int j = 0; j < masks.length; j++) {
                boolean fMatch;
                Mask mask = masks[j];
                
                //XXX: file names in mail may contain directory - theoretically
                if (mask.suffixMatch) {
                    fMatch = fileName.endsWith(mask.matchString);
                } else {
                    fMatch = fileName.equals(mask.matchString);
                }
                if (fMatch) return mail.getRecipients(); // matching file found
            }
        }
        
        return null; // no matching attachment found
    }
}

