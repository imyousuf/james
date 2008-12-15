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

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.james.Constants;
import org.apache.james.api.vut.ErrorMappingException;
import org.apache.james.api.vut.VirtualUserTableStore;
import org.apache.mailet.MailAddress;

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
    private org.apache.james.api.vut.VirtualUserTable vut;

    /*
     * (non-Javadoc)
     * @see org.apache.mailet.base.GenericMailet#init()
     */
    public void init() throws MessagingException {
        super.init();
        ServiceManager compMgr = (ServiceManager) getMailetContext().getAttribute(Constants.AVALON_COMPONENT_MANAGER);

        try {
            String vutName = getInitParameter("virtualusertable");
            if (vutName == null || vutName.length() == 0) {
                try {
                    vut = ((VirtualUserTableStore) compMgr.lookup(VirtualUserTableStore.ROLE)).getTable(vutName);
                } catch (ServiceException e) {
                    log("Failed to retrieve VirtualUserTable component:" + e.getMessage());
                }
            } else {
                vut = ((VirtualUserTableStore) compMgr.lookup(VirtualUserTableStore.ROLE)).getTable("DefaultVirtualUserTable");
            }

        } catch (ServiceException cnfe) {
            log("Failed to retrieve UsersStore component:" + cnfe.getMessage());
        }
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.transport.mailets.AbstractVirtualUserTable#processMail(org.apache.mailet.MailAddress, org.apache.mailet.MailAddress, javax.mail.internet.MimeMessage)
     */
    public Collection processMail(MailAddress sender, MailAddress recipient, MimeMessage message) throws MessagingException {
        try {
            Collection mappings = vut.getMappings(recipient.getUser(), recipient.getHost());
            
            if (mappings != null) {
                return handleMappings(mappings, sender, recipient, message);
            }
        } catch (ErrorMappingException e) {
            StringBuffer errorBuffer = new StringBuffer(128)
                .append("A problem as occoured trying to alias and forward user ")
                .append(recipient)
                .append(": ")
                .append(e.getMessage());
                throw new MessagingException(errorBuffer.toString());
        }
        
        Collection rcpts = new ArrayList();
        rcpts.add(recipient);
        return rcpts;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.mailet.base.GenericMailet#getMailetInfo()
     */
    public String getMailetInfo() {
        return "VirtualUserTable Mailet";
    }
    
    
}
