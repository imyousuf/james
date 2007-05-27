package org.apache.james.experimental.imapserver.commands;

import org.apache.james.imap.command.CommandTemplate;


public class MockCommand extends CommandTemplate {

    public static final String NAME = "MOCK";

    public String getArgSyntax() {
        return null;
    }

    public String getName() {
        return NAME;
    }
}
