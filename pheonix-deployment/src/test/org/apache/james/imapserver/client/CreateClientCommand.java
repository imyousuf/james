package org.apache.james.imapserver.client;

public class CreateClientCommand extends AbstractCommand {

    public CreateClientCommand(String folder) {
        command = "CREATE \""+folder+"\"";
        statusResponse="OK CREATE completed.";
    }
}