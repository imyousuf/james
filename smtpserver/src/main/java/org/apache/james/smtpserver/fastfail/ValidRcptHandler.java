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

package org.apache.james.smtpserver.fastfail;

import java.util.Collection;

import javax.annotation.Resource;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.james.domainlist.api.DomainList;
import org.apache.james.lifecycle.Configurable;
import org.apache.james.protocols.smtp.SMTPSession;
import org.apache.james.protocols.smtp.core.fastfail.AbstractValidRcptHandler;
import org.apache.james.user.api.UsersRepository;
import org.apache.james.vut.api.VirtualUserTable;
import org.apache.james.vut.api.VirtualUserTable.ErrorMappingException;
import org.apache.mailet.MailAddress;

/**
 * Handler which reject invalid recipients
 */
public class ValidRcptHandler extends AbstractValidRcptHandler implements
		Configurable {

	private UsersRepository users;

	private VirtualUserTable vut;

    private boolean useVut = true;

    private DomainList domains;

	/**
	 * Gets the users repository.
	 * 
	 * @return the users
	 */
	public final UsersRepository getUsers() {
		return users;
	}

	/**
	 * Sets the users repository.
	 * 
	 * @param users
	 *            the users to set
	 */
	@Resource(name = "localusersrepository")
	public final void setUsers(UsersRepository users) {
		this.users = users;
	}

	/**
	 * Sets the virtual user table store.
	 * 
	 * @param tableStore
	 *            the tableStore to set
	 */
	@Resource(name = "virtualusertable")
	public final void setVirtualUserTable(VirtualUserTable vut) {
		this.vut = vut;
	}


    @Resource(name="domainlist")
    public void setDomainList(DomainList domains) {
        this.domains = domains;
    }

	/**
	 * @see org.apache.james.lifecycle.Configurable#configure(org.apache.commons.configuration.Configuration)
	 */
	public void configure(HierarchicalConfiguration config) throws ConfigurationException {
		setVirtualUserTableSupport(config.getBoolean("enableVirtualUserTable",
				true));
	}

	public void setVirtualUserTableSupport(boolean useVut) {
		this.useVut = useVut;
	}


	@Override
	protected boolean isValidRecipient(SMTPSession session,
			MailAddress recipient) {

	    if (domains.containsDomain(recipient.getDomain()) == false) {
            session.getLogger().debug("Unknown domain " + recipient.getDomain() + " so reject it");

	        return false;
	    }
	    
	    String username = recipient.toString();
	    
	    // check if the server use virtualhosting, if not use only the localpart as username
	    if (users.supportVirtualHosting() == false) {
	        username = recipient.getLocalPart();
	    }
	    
		if (users.contains(username) == true) {
			return true;
		} else {

			if (useVut == true) {
	            session.getLogger().debug("Unknown user " + username + " check if its an alias");

				try {
					Collection<String> targetString = vut.getMappings(
							recipient.getLocalPart(), recipient.getDomain());

					if (targetString != null && targetString.isEmpty() == false) {
						return true;
					}
				} catch (ErrorMappingException e) {
					return false;
				}
			}

			return false;
		}
	}
}
