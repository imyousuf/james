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

import java.util.ArrayList;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.james.imapserver.util.UnsolicitedResponseGenerator;

public class SelectCommand extends AbstractCommand {
    
    int recentCount =0;

    public SelectCommand(String folder,MimeMessage[] msgs,long uidv) throws MessagingException {
        
        command="SELECT \""+folder+"\"";

        UnsolicitedResponseGenerator rg=new UnsolicitedResponseGenerator();
        rg.addByMessages(msgs);
        recentCount = rg.getRecent();
        rg.addUidValidity(uidv);
        responseList=new ArrayList(rg.getResponseSet());
        statusResponse="OK [READ-WRITE] SELECT completed.";
        
    }

    public int getRecentCount() {
        return recentCount;
    }

}
