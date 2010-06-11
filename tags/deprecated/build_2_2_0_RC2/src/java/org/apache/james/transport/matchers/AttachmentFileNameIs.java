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
import java.util.Locale;


/**
 * <P>Checks if at least one attachment has a file name which matches any
 * element of a comma-separated list of file name masks.
 * The match is case insensitive.</P>
 * <P>File name masks may start with a wildcard '*'.</P>
 * <P>Multiple file name masks can be specified, e.g.: '*.scr,*.bat'.</P>
 *
 * @version CVS $Revision: 1.1.2.4 $ $Date: 2004/03/15 03:54:21 $
 * @since 2.2.0
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
            mask.matchString = mask.matchString.toLowerCase(Locale.US);
            mask.matchString = mask.matchString.trim();
            theMasks.add(mask);
        }
        masks = (Mask[])theMasks.toArray(new Mask[0]);
    }

    /** 
     * Either every recipient is matching or neither of them.
     * @throws MessagingException if no matching attachment is found and at least one exception was thrown
     */
    public Collection match(Mail mail) throws MessagingException {
        
        Exception anException = null;
        
        try {
            MimeMessage message = mail.getMessage();
            Object content;
            
            /**
             * if there is an attachment and no inline text,
             * the content type can be anything
             */
            if (message.getContentType() == null) {
                return null;
            }
            
            content = message.getContent();
            if (content instanceof Multipart) {
                Multipart multipart = (Multipart) content;
                for (int i = 0; i < multipart.getCount(); i++) {
                    try {
                        Part part = multipart.getBodyPart(i);
                        String fileName = part.getFileName();
                        if (fileName != null && matchFound(fileName)) {
                            return mail.getRecipients(); // matching file found
                        }
                    } catch (MessagingException e) {
                        anException = e;
                    } // ignore any messaging exception and process next bodypart
                }
            } else {
                String fileName = message.getFileName();
                if (fileName != null && matchFound(fileName)) {
                    return mail.getRecipients(); // matching file found
                }
            }
        } catch (Exception e) {
            anException = e;
        }
        
        // if no matching attachment was found and at least one exception was catched rethrow it up
        if (anException != null) {
            throw new MessagingException("Malformed message", anException);
        }
        
        return null; // no matching attachment found
    }
    
    /*
     * Checks if <I>fileName</I> matches with at least one of the <CODE>masks</CODE>.
     */
    private boolean matchFound(String fileName) {
        fileName = fileName.toLowerCase(Locale.US);
        fileName = fileName.trim();
            
        for (int j = 0; j < masks.length; j++) {
            boolean fMatch;
            Mask mask = masks[j];
            
            //XXX: file names in mail may contain directory - theoretically
            if (mask.suffixMatch) {
                fMatch = fileName.endsWith(mask.matchString);
            } else {
                fMatch = fileName.equals(mask.matchString);
            }
            if (fMatch) return true; // matching file found
        }
        return false;
    }
}

