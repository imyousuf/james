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

package org.apache.james.mailboxmanager.acl;

import java.util.HashMap;


/**
 * Reprensents a set of rights that can be assigned to a mailbox in combination
 * of an ACL. It is similar to javamails com.sun.mail.imap.Rights
 */

public class MailboxRights {

    public MailboxRights() {

    }

    public void add(Right right) {
        ;
    }

    /**
     * returns a string representation like defined in RFC 2086. Rights not
     * supported by RFC 2086 will be omitted.
     * 
     */
    public String toImapString() {
        return null;

    }


    
    public void remove(Right right) {
        ;
    }

    public boolean contains(Right right) {
        return false;
    }
/**
 * 
 * draft an incomplete. Idea is to internally use String representives. The list of possible rights
 * could increase a lot. That would make it impossible to find an appropriate char.
 *
 */
    public static final class Right {

        private static HashMap allRights = new HashMap();

        public static final Right WRITE = getInstance("w");

        private Right(String representative) {

        }

        private synchronized static final Right getInstance(String s) {
            Right right = (Right) allRights.get(s);
            if (right == null) {
                right = new Right(s);
            }
            return right;
        }

    }

}
