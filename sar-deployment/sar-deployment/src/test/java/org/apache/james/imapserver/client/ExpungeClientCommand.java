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

package org.apache.james.imapserver.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.mail.Flags;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

public class ExpungeClientCommand extends AbstractCommand {

    List expungedMsns = new ArrayList();

    public ExpungeClientCommand(MimeMessage[] msgs) throws MessagingException {
        command = "EXPUNGE";
        statusResponse = "OK EXPUNGE completed.";
        for (int i = 0; i < msgs.length; i++) {
            if (msgs[i].getFlags().contains(Flags.Flag.DELETED)) {
                expungedMsns.add(new Integer(i + 1));
            }
        }
    }

    public List getExpectedResponseList() throws MessagingException,
            IOException {
        List responseList = new LinkedList();

        for (Iterator it = expungedMsns.iterator(); it.hasNext();) {
            final int no = ((Integer) it.next()).intValue();
            String line = "* " + no + " EXPUNGE";
            responseList.add(line);
        }
        return responseList;
    }

}
