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

package org.apache.james.transport.matchers;

import org.apache.mailet.Mail;
import org.apache.mailet.MailetContext;
import org.apache.mailet.MailAddress;
import javax.mail.MessagingException;
import java.util.Locale;

/**
 * <P>Experimental: Checks whether a recipient has exceeded a maximum allowed quota for messages
 * standing in his inbox. Such quota is <I>the same</I> for all users.</P>
 * <P>Will check if the total size of all his messages in the inbox are greater
 * than a certain number of bytes.  You can use 'k' and 'm' as optional postfixes.
 * In other words, "1m" is the same as writing "1024k", which is the same as
 * "1048576".</P>
 * <P>Here follows an example of a config.xml definition:</P>
 * <PRE><CODE>
 * &lt;processor name="transport"&gt;
 * .
 * .
 * .
 *    &lt;mailet match=match="RecipientIsOverFixedQuota=40M" class="ToProcessor"&gt;
 *       &lt;processor&gt; error &lt;/processor&gt;
 *       &lt;notice&gt;The recipient has exceeded maximum allowed size quota&lt;/notice&gt;
 *    &lt;/mailet&gt;
 * .
 * .
 * .
 * &lt;/processor&gt;
 * </CODE></PRE>
 * 
 * <P>This matcher need to calculate the mailbox size everytime it is called. This can slow down things if there are many mails in
 * the mailbox. Some users also report big problems with the matcher if a JDBC based mailrepository is used. </P>
 * @version 1.0.0, 2003-05-11
 */

public class RecipientIsOverFixedQuota extends AbstractStorageQuota {
    private long quota = 0;

    /**
     * Standard matcher initialization.
     * Does a <CODE>super.init()</CODE> and parses the common storage quota amount from
     * <I>config.xml</I> for later use.
     */
    public void init() throws MessagingException {
        super.init();
        quota = parseQuota(getCondition().trim().toLowerCase(Locale.US));
    }

    protected long getQuota(MailAddress recipient, Mail _) throws MessagingException {
        return quota;
    }
}
