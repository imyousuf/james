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

package org.apache.james.api.imap.display;

import org.apache.james.api.imap.ImapConstants;

/**
 * Keys human response text that may be displayed to the user. 
 */
public class HumanReadableTextKey {

    public static final HumanReadableTextKey GENERIC_LSUB_FAILURE 
    = new HumanReadableTextKey("org.apache.james.imap.GENERIC_SUBSCRIPTION_FAILURE", 
    "Cannot list subscriptions."); 
    
    public static final HumanReadableTextKey GENERIC_UNSUBSCRIPTION_FAILURE 
    = new HumanReadableTextKey("org.apache.james.imap.GENERIC_SUBSCRIPTION_FAILURE", 
    "Cannot unsubscribe."); 
    
    public static final HumanReadableTextKey GENERIC_SUBSCRIPTION_FAILURE 
    = new HumanReadableTextKey("org.apache.james.imap.GENERIC_SUBSCRIPTION_FAILURE", 
    "Cannot subscribe."); 
    
    public static final HumanReadableTextKey FAILURE_NO_SUCH_MAILBOX 
        = new HumanReadableTextKey("org.apache.james.imap.FAILURE_NO_SUCH_MAILBOX", 
                "failed. No such mailbox.");

    public static final HumanReadableTextKey COMPLETED 
        = new HumanReadableTextKey("org.apache.james.imap.COMPLETED", 
                "completed.");
    
    public static final HumanReadableTextKey INVALID_LOGIN 
        = new HumanReadableTextKey("org.apache.james.imap.INVALID_LOGIN",
                "failed. Invalid login/password.");
    
    public static final HumanReadableTextKey UNSUPPORTED_SEARCH_CRITERIA 
        = new HumanReadableTextKey("org.apache.james.imap.UNSUPPORTED_CRITERIA",
                "failed. One or more search criteria is unsupported.");
    
    public static final HumanReadableTextKey BAD_CHARSET 
        = new HumanReadableTextKey("org.apache.james.imap.BAD_CHARSET",
                "failed. Charset is unsupported.");
    
    public static final HumanReadableTextKey MAILBOX_IS_READ_ONLY 
        = new HumanReadableTextKey("org.apache.james.imap.MAILBOX_IS_READ_ONLY",
                "failed. Mailbox is read only.");
    
    public static final HumanReadableTextKey BYE 
        = new HumanReadableTextKey("org.apache.james.imap.BYE",
         ImapConstants.VERSION + " Server logging out");
    
    private final String defaultValue;
    private final String key;
    public HumanReadableTextKey(final String key, final String defaultValue) {
        super();
        this.defaultValue = defaultValue;
        this.key = key;
    }
   
    /**
     * Gets the default value for this text.
     * @return default human readable text, not null
     */
    public final String getDefaultValue() {
        return defaultValue;
    }
    
    /**
     * Gets a unique key that can be used to loopup the text.
     * How this is performed is implementation independent.
     * @return key value, not null
     */
    public final String getKey() {
        return key;
    }

    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + ((key == null) ? 0 : key.hashCode());
        return result;
    }

    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final HumanReadableTextKey other = (HumanReadableTextKey) obj;
        if (key == null) {
            if (other.key != null)
                return false;
        } else if (!key.equals(other.key))
            return false;
        return true;
    }
    
    public String toString() {
        return defaultValue;
    }
}
