package org.apache.james.experimental.imapserver.client;

import java.util.ArrayList;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.james.experimental.imapserver.util.UnsolicitedResponseGenerator;

public class SelectCommand extends AbstractCommand {
    
    int recentCount =0;

    public SelectCommand(String folder,MimeMessage[] msgs,long uidv) throws MessagingException {
        
        command="SELECT \""+folder+"\"";

        UnsolicitedResponseGenerator rg=new UnsolicitedResponseGenerator();
        rg.addByMessages(msgs);
        recentCount = rg.getRecent();
        rg.addUidValidity(uidv);
        responseList=new ArrayList(rg.getResponseSet());
        statusResponse="OK [READ-WRITE] SELECT completed.";
        
    }

    public int getRecentCount() {
        return recentCount;
    }

}
