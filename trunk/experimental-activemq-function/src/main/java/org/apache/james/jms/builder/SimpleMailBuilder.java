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

package org.apache.james.jms.builder;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

import javax.mail.MessagingException;
import javax.mail.internet.ParseException;

import org.apache.commons.io.IOUtils;
import org.apache.james.api.jms.MailBuilder;
import org.apache.james.core.MailImpl;
import org.apache.james.mime4j.field.AddressListField;
import org.apache.james.mime4j.field.Field;
import org.apache.james.mime4j.field.MailboxListField;
import org.apache.james.mime4j.field.address.Mailbox;
import org.apache.james.mime4j.field.address.MailboxList;
import org.apache.james.services.MailServer;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;

/**
 * <p>Basic implementation that builds {@link Mail} 
 * from basic non-mime <code>ASCII</code> mail.
 * Naively uses the <code>to</code> header
 * to determine the recipent and <code>from</code>
 * to determine the sender.
 * </p><p>
 * <strong>Note</strong> this implementation is too
 * simple to cope well with wild emails. It may be useful
 * when dealing with generated ASCII emails.
 * </p>
 */
public class SimpleMailBuilder implements MailBuilder {
	
	private static final String US_ASCII = "US-ASCII";

	private final IdGenerator generator;
	
	public SimpleMailBuilder() {
		this(new IdGenerator() {
			private final Random random = new Random();
			public String getId() {
				return "SimpleMailBuilder#" + random.nextLong();
			}
		});
	}
	
	public SimpleMailBuilder(final IdGenerator generator) {
		this.generator = generator;
	}
	
	public Mail build(String text) throws MessagingException {
		final Collection recipients = new ArrayList();
		MailAddress sender = null;
		final int length = text.length();
		int position = 0;
		while (position < length) {
			position = text.indexOf('\n', position);
			if (position == -1) {
				position = length;
			}
			final char nextChar = text.charAt(++position);
			
			switch (nextChar) {
				case '\r':
					// end of headers
					position = length;
					break;
				case 'F':
				case 'f':
					// from header?
					sender = parseFrom(text, sender, length, position);
					break; 
				case 'T':
				case 't':
					parseTo(text, recipients, length, position);
					break; 
				default:
					break;
			}
		}
		final String key = generator.getId();
		final InputStream input = toInputStream(text);
		final Mail result = new MailImpl(key, sender, recipients, input);
		return result;
	}

	private MailAddress parseFrom(final String text, final MailAddress defaultAddress, final int length, int position) throws ParseException {
		MailAddress result = defaultAddress;
		int eol = text.indexOf('\r', position);
		if (eol == -1) {
			eol = length;
		}
		final String line = text.substring(position, eol);
		final Field parsedField = Field.parse(line);
		if (parsedField.isFrom()) {
			if (parsedField instanceof AddressListField) {
				final AddressListField field = (AddressListField) parsedField;	
				final MailboxList mailboxes = field.getAddressList().flatten();
				result = firstMailAddress(mailboxes);
			} else if (parsedField instanceof MailboxListField) {
				final MailboxListField field = (MailboxListField) parsedField;
				final MailboxList mailboxes = field.getMailboxList();
				result = firstMailAddress(mailboxes);
			}
		}
		return result;
	}
	
	private void parseTo(final String text, Collection addresses, final int length, int position) throws ParseException {
		int eol = text.indexOf('\r', position);
		if (eol == -1) {
			eol = length;
		}
		final String line = text.substring(position, eol);
		final Field parsedField = Field.parse(line);
		if (parsedField.isTo()) {
			if (parsedField instanceof AddressListField) {
				final AddressListField field = (AddressListField) parsedField;	
				final MailboxList mailboxes = field.getAddressList().flatten();
				addMailAddresses(addresses, mailboxes);
			} else if (parsedField instanceof MailboxListField) {
				final MailboxListField field = (MailboxListField) parsedField;
				final MailboxList mailboxes = field.getMailboxList();
				addMailAddresses(addresses, mailboxes);
			}
		}
	}

	private void addMailAddresses(Collection addresses, final MailboxList mailboxes) throws ParseException {
		int size = mailboxes.size();
		for (int i=0;i< size;i++) {
			final MailAddress address = toMailAddress(mailboxes.get(i));
			addresses.add(address);
		}
	}

	private MailAddress firstMailAddress(final MailboxList mailboxes) throws ParseException {
		MailAddress result = null;
		if (mailboxes.size() > 0) {
			final Mailbox first = mailboxes.get(0);
			result = toMailAddress(first);
		}
		return result;
	}

	private MailAddress toMailAddress(final Mailbox mailbox) throws ParseException {
		MailAddress result;
		final String domain = mailbox.getDomain();
		final String local = mailbox.getLocalPart();
		result = new MailAddress(local, domain);
		return result;
	}

	private InputStream toInputStream(String text) throws MessagingException {
		try {
			final InputStream result = IOUtils.toInputStream(text, US_ASCII);
			return result;
		} catch (IOException e) {
			throw new MessagingException("Conversion to ASCII stream failed.", e);
		}
	}

	
	
	public interface IdGenerator {
		
	    /**
	     * Generate a new identifier/name for a mail being processed by this server.
	     *
	     * @return the new identifier
	     */
	    String getId();
	}
	
	public final static class JamesIdGenerator implements IdGenerator {
		private final MailServer mailServer;
		
		public JamesIdGenerator(final MailServer mailServer) {
			super();
			this.mailServer = mailServer;
		}

		public String getId() {
			return mailServer.getId();
		}

		/**
		 * Renders suitably for logging.
		 * @return a <code>String</code> representation 
		 * of this object.
		 */
		public String toString()
		{
		    final String TAB = " ";
		    final String retValue = "JamesIdGenerator ( "
		        + "mailServer = " + this.mailServer + TAB
		        + " )";		
		    return retValue;
		}
		
		
	}

	/**
	 * Renders suitably for logging.
	 * @return a <code>String</code> representation 
	 * of this object.
	 */
	public String toString()
	{
	    final String TAB = " ";
	    
	    final String retValue = "SimpleMailBuilder ( "
	        + "generator = " + this.generator + TAB
	        + " )";
	
	    return retValue;
	}
	
	
}
