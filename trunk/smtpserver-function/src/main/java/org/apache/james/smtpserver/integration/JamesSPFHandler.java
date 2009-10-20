package org.apache.james.smtpserver.integration;

import org.apache.james.smtpserver.protocol.SMTPSession;
import org.apache.james.smtpserver.protocol.core.fastfail.SPFHandler;
import org.apache.james.smtpserver.protocol.hook.HookResult;
import org.apache.mailet.Mail;

public class JamesSPFHandler extends SPFHandler implements JamesMessageHook {

	/**
	 * @see org.apache.james.smtpserver.integration.JamesMessageHook#onMessage(org.apache.james.smtpserver.protocol.SMTPSession,
	 *      org.apache.mailet.Mail)
	 */
	public HookResult onMessage(SMTPSession session, Mail mail) {
		// Store the spf header as attribute for later using
		mail.setAttribute(SPF_HEADER_MAIL_ATTRIBUTE_NAME, (String) session
				.getState().get(SPF_HEADER));

		return null;
	}

}
