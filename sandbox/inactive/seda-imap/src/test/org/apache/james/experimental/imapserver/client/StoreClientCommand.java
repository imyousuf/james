package org.apache.james.experimental.imapserver.client;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.mail.Flags;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

public class StoreClientCommand extends AbstractCommand {

    public static final int MODE_SET = 0;

    public static final int MODE_ADD = 1;

    public static final int MODE_REMOVE = 2;

    private final int mode;

    private final boolean silent;

    private final Flags flags;

    private MessageSet set;

    public StoreClientCommand(int mode, boolean silent, Flags flags,
            MessageSet set) {
        this.mode = mode;
        this.silent = silent;
        this.flags = flags;
        this.set = set;
        this.statusResponse = "OK STORE completed.";
    }

    public String getCommand() {
        command = "";
        if (set.isUid()) {
            command = "UID ";
        }
        command += "STORE ";
        command += set + " ";

        if (mode == MODE_ADD) {
            command += "+";
        } else if (mode == MODE_REMOVE) {
            command += "-";
        }
        command += "FLAGS";
        if (silent) {
            command += ".SILENT";
        }
        command += " (";
        command += FetchCommand.flagsToString(flags);
        command += ")";

        return command;

    }

    public List getExpectedResponseList() throws MessagingException {
        List responseList = new LinkedList();
        if (!silent) {
            List selectedNumbers = set.getSelectedMessageNumbers();
            for (Iterator it = selectedNumbers.iterator(); it.hasNext();) {
                final int no = ((Integer) it.next()).intValue();
                final MimeMessage mm = set.getMessage(no);
                String line = "* " + no + " FETCH (";
                line += "FLAGS (" + FetchCommand.flagsToString(mm.getFlags())
                        + ")";
                if (set.isUid()) {
                    line += " UID " + set.getUid(no);
                }
                line += ")";
                responseList.add(line);
            }
        }
        return responseList;
    }
}
