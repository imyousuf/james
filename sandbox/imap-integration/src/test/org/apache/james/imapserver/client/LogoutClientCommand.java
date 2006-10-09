package org.apache.james.imapserver.client;

public class LogoutClientCommand extends AbstractCommand {

	public LogoutClientCommand() {
		command="LOGOUT\n";
		statusResponse="OK LOGOUT completed.";
		responseList.add("* BYE IMAP4rev1 Server logging out");
	}
	
}
