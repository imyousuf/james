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

package org.apache.james.imapserver.sieve;

import javax.mail.MessagingException;

import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.james.Constants;
import org.apache.jsieve.mailet.Poster;
import org.apache.jsieve.mailet.SieveToMultiMailbox;
import org.apache.mailet.MailetConfig;

/**
 * Contains avalon bindings.
 */
public class SieveMailet extends SieveToMultiMailbox {

    @Override
    public void init(MailetConfig config) throws MessagingException {
        ServiceManager compMgr = (ServiceManager)getMailetContext().getAttribute(Constants.AVALON_COMPONENT_MANAGER);
        try {
            Poster poster = (Poster) compMgr.lookup("");
            setPoster(poster);
        } catch (ServiceException e) {
            throw new MessagingException("IMAP not installed", e);
        }
        super.init(config);
    }    
}
