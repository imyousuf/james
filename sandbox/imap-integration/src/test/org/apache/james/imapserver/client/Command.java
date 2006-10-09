package org.apache.james.imapserver.client;

import java.io.IOException;
import java.util.List;

import javax.mail.MessagingException;

public interface Command {

	public List getExpectedResponseList() throws MessagingException, IOException;

	/**
	 * Untagged, without leading space or trailing newline.<br>
	 * e.g. : "OK LOGIN completed."
	 */
	public String getExpectedStatusResponse();

	/**
	 * Untagged, without leading space with trailing newline. <br>
	 *  e.g. : "LOGIN user password\n"
	 */
	public String getCommand();

}
