package org.apache.james.imapserver.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.mail.Flags;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

public class ExpungeClientCommand extends AbstractCommand {

    List expungedMsns = new ArrayList();

    public ExpungeClientCommand(MimeMessage[] msgs) throws MessagingException {
        int msnOffset = 0;

        command = "EXPUNGE";
        statusResponse = "OK EXPUNGE completed.";
        for (int i = 0; i < msgs.length; i++) {
            if (msgs[i].getFlags().contains(Flags.Flag.DELETED)) {
                expungedMsns.add(new Integer(i + msnOffset + 1));
                msnOffset--;
            }
        }
    }

    public List getExpectedResponseList() throws MessagingException,
            IOException {
        List responseList = new LinkedList();

        for (Iterator it = expungedMsns.iterator(); it.hasNext();) {
            final int no = ((Integer) it.next()).intValue();
            String line = "* " + no + " EXPUNGE";
            responseList.add(line);
        }
        return responseList;
    }

}
