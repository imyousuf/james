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

import javax.annotation.Resource;
import javax.mail.MessagingException;

import org.apache.james.services.MailServer;
import org.apache.jsieve.mailet.Poster;
import org.apache.jsieve.mailet.SieveMailboxMailet;
import org.apache.mailet.MailAddress;
import org.apache.mailet.MailetConfig;

/**
 * Contains resource bindings.
 */
public class SieveMailet extends SieveMailboxMailet {

    private MailServer mailServer;

    @Resource(name="org.apache.james.services.MailServer")
    public void setMailSerer(MailServer mailServer) {
        this.mailServer = mailServer;
    }
    



    @Override
    public void init(MailetConfig config) throws MessagingException {
        // ATM Fixed implementation
        setLocator(new ResourceLocatorImpl(mailServer.supportVirtualHosting()));
        
        super.init(config);
    }




    public SieveMailet() {
        super();

    }

    @Resource(name="org.apache.jsieve.mailet.Poster")
    @Override
    public void setPoster(Poster poster) {
        super.setPoster(poster);
    }
   
    /**
     * Return the username to use for sieve processing for the given MailAddress. If virtualhosting
     * is supported use the full emailaddrees as username
     * 
     * @param m
     * @return username
     */
    protected String getUsername(MailAddress m) {
        if (mailServer.supportVirtualHosting()) {
            return m.toString();
        } else {
            return super.getUsername(m);
        }
    }
    
}
