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
import org.apache.mailet.MailetException;
import java.io.Serializable;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.mail.MessagingException;

/**
 * This mailet sets attributes on the Mail.
 * 
 * Sample configuration:
 * &lt;mailet match="All" class="SetMailAttribute"&gt;
 *   &lt;name1&gt;value1&lt;/name1&gt;
 *   &lt;name2&gt;value2&lt;/name2&gt;
 * &lt;/mailet&gt;
 *
 * @version CVS $Revision: 1.1.2.1 $ $Date: 2003/07/15 10:50:24 $
 * @since 2.2.0
 */
public class SetMailAttribute extends GenericMailet {

    private HashMap attributesToSet = new HashMap(2);
    
    private Set entries;
    
    /**
     * Return a string describing this mailet.
     *
     * @return a string describing this mailet
     */
    public String getMailetInfo() {
        return "Set Mail Attribute Mailet";
    }

    /**
     * Initialize the mailet
     *
     * @throws MailetException if the processor parameter is missing
     */
    public void init() throws MailetException
    {
        Iterator iter = getInitParameterNames();
        while (iter.hasNext()) {
            String name = iter.next().toString();
            String value = getInitParameter (name);
            attributesToSet.put (name,value);
        }
        entries = attributesToSet.entrySet();
    }

    /**
     * Sets the configured attributes
     *
     * @param mail the mail to process
     *
     * @throws MessagingException in all cases
     */
    public void service(Mail mail) throws MessagingException {
        if (entries != null) {
            Iterator iter = entries.iterator();
            while (iter.hasNext()) {
                Map.Entry entry = (Map.Entry)iter.next();
                mail.setAttribute ((String)entry.getKey(),(Serializable)entry.getValue());
            }
        }
    }
    

}
