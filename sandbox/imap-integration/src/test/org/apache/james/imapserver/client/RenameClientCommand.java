package org.apache.james.imapserver.client;

public class RenameClientCommand extends AbstractCommand {
	
	public RenameClientCommand(String folder,String destination) {
		command = "RENAME \""+folder+"\" \""+destination+"\"";
		statusResponse="OK RENAME completed.";
	}
}
