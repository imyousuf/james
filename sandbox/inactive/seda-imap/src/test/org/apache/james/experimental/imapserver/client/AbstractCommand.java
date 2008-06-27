package org.apache.james.experimental.imapserver.client;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.mail.MessagingException;

public abstract class AbstractCommand implements Command {

    protected List responseList = new LinkedList();
    protected String statusResponse = null;
    protected String command = null; 

    public List getExpectedResponseList() throws MessagingException, IOException {
        return responseList ;
    }

    public String getExpectedStatusResponse() {
        return statusResponse;
    }

    public String getCommand() {
        return command;
    }

}
