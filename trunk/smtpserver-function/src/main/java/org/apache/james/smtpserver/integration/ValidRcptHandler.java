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

package org.apache.james.smtpserver.integration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.annotation.Resource;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.james.api.protocol.Configurable;
import org.apache.james.api.user.UsersRepository;
import org.apache.james.api.vut.ErrorMappingException;
import org.apache.james.api.vut.VirtualUserTable;
import org.apache.james.api.vut.VirtualUserTableStore;
import org.apache.james.dsn.DSNStatus;
import org.apache.james.smtpserver.protocol.SMTPRetCode;
import org.apache.james.smtpserver.protocol.SMTPSession;
import org.apache.james.smtpserver.protocol.core.fastfail.AbstractValidRcptHandler;
import org.apache.james.smtpserver.protocol.hook.HookResult;
import org.apache.james.smtpserver.protocol.hook.HookReturnCode;
import org.apache.mailet.MailAddress;
import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.Perl5Compiler;
import org.apache.oro.text.regex.Perl5Matcher;

/**
 * Handler which reject invalid recipients
 */
public class ValidRcptHandler extends AbstractValidRcptHandler implements
		Configurable {

	private UsersRepository users;

	private VirtualUserTableStore tableStore;

	private Collection<String> recipients = new ArrayList<String>();
	private Collection<String> domains = new ArrayList<String>();
	private Collection<Pattern> regex = new ArrayList<Pattern>();
	private boolean vut = true;
	private VirtualUserTable table;
	private String tableName = null;

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
	@Resource(name = "org.apache.james.api.user.UsersRepository")
	public final void setUsers(UsersRepository users) {
		this.users = users;
	}

	/**
	 * Gets the virtual user table store.
	 * 
	 * @return the tableStore
	 */
	public final VirtualUserTableStore getTableStore() {
		return tableStore;
	}

	/**
	 * Sets the virtual user table store.
	 * 
	 * @param tableStore
	 *            the tableStore to set
	 */
	@Resource(name = "org.apache.james.api.vut.VirtualUserTableStore")
	public final void setTableStore(VirtualUserTableStore tableStore) {
		this.tableStore = tableStore;
		loadTable();
	}

	/**
	 * @see org.apache.james.api.protocol.Configurable#configure(org.apache.commons.configuration.Configuration)
	 */
	@SuppressWarnings("unchecked")
	public void configure(Configuration config) throws ConfigurationException {
		setValidRecipients(config.getList("validRecipients"));
		setValidDomains(config.getList("validDomains"));
		try {
			setValidRegex(config.getList("validRegexPattern"));
		} catch (MalformedPatternException mpe) {
			throw new ConfigurationException("Malformed pattern: ", mpe);
		}
		setVirtualUserTableSupport(config.getBoolean("enableVirtualUserTable",
				true));
		setTableName(config.getString("table", null));
	}

	/**
	 * Set the valid recipients.
	 * 
	 * @param recip
	 *            The valid recipients. Commaseperated list
	 */
	public void setValidRecipients(Collection<String> recips) {
		Iterator<String> recipsIt = recips.iterator();
		while (recipsIt.hasNext()) {
			String recipient = recipsIt.next();
			recipients.add(recipient);
		}
	}

	/**
	 * Set the domains for which every rcpt will be accepted.
	 * 
	 * @param dom
	 *            The valid domains. Comma seperated list
	 */
	public void setValidDomains(Collection<String> doms) {
		Iterator<String> domsIt = doms.iterator();

		while (domsIt.hasNext()) {
			String domain = domsIt.next().toLowerCase();
			domains.add(domain);
		}
	}

	/**
	 * 
	 * @param reg
	 * @throws MalformedPatternException
	 */
	public void setValidRegex(Collection<String> regs)
			throws MalformedPatternException {
		Perl5Compiler compiler = new Perl5Compiler();

		Iterator<String> regsIt = regs.iterator();
		while (regsIt.hasNext()) {
			String patternString = regsIt.next();
			Pattern pattern = compiler.compile(patternString,
					Perl5Compiler.READ_ONLY_MASK);
			regex.add(pattern);

		}
	}

	public void setVirtualUserTableSupport(boolean vut) {
		this.vut = vut;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
		loadTable();
	}

	private void loadTable() {
		if (this.tableName == null || this.tableName.equals("")) {
			this.tableName = VirtualUserTableStore.DEFAULT_TABLE;
		}
		if (tableStore != null) {
			table = tableStore.getTable(this.tableName);
		}
	}


	@Override
	protected boolean isValidRecipient(SMTPSession session,
			MailAddress recipient) {

		if (users.contains(recipient.getLocalPart()) == true
				|| recipients.contains(recipient.toString().toLowerCase())
				|| domains.contains(recipient.getDomain().toLowerCase())) {
			return true;
		} else {
			if (vut == true) {
				try {
					Collection<String> targetString = table.getMappings(
							recipient.getLocalPart(), recipient.getDomain());

					if (targetString != null && targetString.isEmpty() == false) {
						return true;
					}
				} catch (ErrorMappingException e) {
					return false;
				}
			}

			if (!regex.isEmpty()) {
				Iterator<Pattern> reg = regex.iterator();
				Perl5Matcher matcher = new Perl5Matcher();

				while (reg.hasNext()) {
					if (matcher.matches(recipient.toString(), reg.next())) {
						// regex match
						return true;
					}
				}
			}
			return false;
		}
	}
}
