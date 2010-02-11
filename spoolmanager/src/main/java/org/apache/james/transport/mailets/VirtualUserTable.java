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

package org.apache.james.transport.mailets;

import java.util.ArrayList;
import java.util.Collection;

import javax.annotation.Resource;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.james.api.vut.ErrorMappingException;
import org.apache.james.api.vut.VirtualUserTableStore;
import org.apache.mailet.MailAddress;
import org.apache.mailet.MailetException;

/**
 * Mailet which should get used when using VirtualUserTable-Store to implementations
 * for mappings of forwards and aliases. 
 * 
 * If no VirtualUsertable-Store name is given the default of DefaultVirtualUserTable
 * will get used.
 * 
 * eg. <virtualusertable>DefaultVirtualUserTable</virtualusertable>
 *
 */
public class VirtualUserTable extends AbstractVirtualUserTableMailet {
    private VirtualUserTableStore vutStore;
    private org.apache.james.api.vut.VirtualUserTable vut;
   

    /**
     * Gets the virtual user table store.
     * @return the vutStore, possibly null
     */
    public final VirtualUserTableStore getVutStore() {
        return vutStore;
    }

    /**
     * Sets the virtual table store.
     * @param vutStore the vutStore to set, possibly null
     */
    @Resource(name="virtualusertable-store")
    public final void setVutStore(VirtualUserTableStore vutStore) {
        this.vutStore = vutStore;
    }

    /**
     * @see org.apache.mailet.base.GenericMailet#init()
     */
    public void init() throws MessagingException {
        super.init();
        
        if (vutStore == null) {
            throw new MailetException("Not initialised. Please ensure that the mailet container supports either" +
            " setter or constructor injection. ");
        }
        
        String vutName = getInitParameter("virtualusertable");
        vut = vutStore.getTable(vutName);
        if (vut == null) {
            throw new MailetException("Could not find VirtualUserTable with name " + vutName);
        }
    }

    /**
     * @see org.apache.james.transport.mailets.AbstractVirtualUserTable#processMail(org.apache.mailet.MailAddress, org.apache.mailet.MailAddress, javax.mail.internet.MimeMessage)
     */
    public Collection<MailAddress> processMail(MailAddress sender, MailAddress recipient, MimeMessage message) throws MessagingException {
        try {
            Collection<String> mappings = vut.getMappings(recipient.getLocalPart(), recipient.getDomain());
            
            if (mappings != null) {
                return handleMappings(mappings, sender, recipient, message);
            }
        } catch (ErrorMappingException e) {
            StringBuilder errorBuffer = new StringBuilder(128)
                .append("A problem as occoured trying to alias and forward user ")
                .append(recipient)
                .append(": ")
                .append(e.getMessage());
                throw new MessagingException(errorBuffer.toString());
        }
        
        Collection<MailAddress> rcpts = new ArrayList<MailAddress>();
        rcpts.add(recipient);
        return rcpts;
    }

    /**
     * @see org.apache.mailet.base.GenericMailet#getMailetInfo()
     */
    public String getMailetInfo() {
        return "VirtualUserTable Mailet";
    }
    
    
}
