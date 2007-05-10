package org.apache.james.experimental.imapserver.client;

public class DeleteClientCommand extends AbstractCommand {

    public DeleteClientCommand(String folder) {
        command = "DELETE \""+folder+"\"";
        statusResponse="OK DELETE completed.";
    }
}
