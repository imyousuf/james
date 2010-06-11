package org.apache.james.imapserver.client;

public class CopyClientCommand extends AbstractCommand {
    
    public CopyClientCommand(MessageSet set,String destination) {
        command = "";
        if (set.isUid())  { 
            command ="UID ";
        }
        command += "COPY "+set + " \""+destination+"\"";
        statusResponse="OK COPY completed.";
    }

}
