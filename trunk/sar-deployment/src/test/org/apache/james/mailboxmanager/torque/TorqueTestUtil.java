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

package org.apache.james.mailboxmanager.torque;

import org.apache.james.mailboxmanager.TestUtil;
import org.apache.james.mailboxmanager.torque.om.MailboxRowPeer;
import org.apache.torque.TorqueException;
import org.apache.torque.util.Criteria;

public class TorqueTestUtil extends TestUtil {

    

    public static void clearTables() throws TorqueException {
//        MessageBodyPeer.doDelete(new Criteria().and(MessageBodyPeer.MAILBOX_ID,
//                new Integer(-1), Criteria.GREATER_THAN));
//        MessageHeaderPeer.doDelete(new Criteria().and(MessageHeaderPeer.MAILBOX_ID,
//                new Integer(-1), Criteria.GREATER_THAN));
//        MessageFlagsPeer.doDelete(new Criteria().and(MessageFlagsPeer.MAILBOX_ID,
//                new Integer(-1), Criteria.GREATER_THAN));
//        MessageRowPeer.doDelete(new Criteria().and(MessageRowPeer.MAILBOX_ID,
//                new Integer(-1), Criteria.GREATER_THAN));
        MailboxRowPeer.doDelete(new Criteria().and(MailboxRowPeer.MAILBOX_ID,
                new Integer(-1), Criteria.GREATER_THAN));
    }

}
