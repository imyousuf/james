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
 * @version CVS $Revision: 1.2 $ $Date: 2004/01/30 02:22:12 $
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
