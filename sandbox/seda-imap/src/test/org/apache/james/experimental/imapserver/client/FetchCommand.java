package org.apache.james.experimental.imapserver.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.mail.Flags;
import javax.mail.MessagingException;
import javax.mail.Flags.Flag;
import javax.mail.internet.MimeMessage;

import org.apache.james.experimental.imapserver.client.fetch.FetchBody;

public class FetchCommand extends AbstractCommand {

    final long from;

    final long to;

    boolean uid;

    private MimeMessage[] msgs;

    private long[] uids;

    private boolean fetchFlags;

    private boolean fetchRfc822Size;
    
    private FetchBody body;
    
    private boolean useParenthesis = true;
    
    private boolean oneSeqNumberOnly = false;

    public FetchCommand(MimeMessage[] msgs, long from, long to) {
        statusResponse = "OK FETCH completed.";
        this.msgs = msgs;
        this.from = from;
        this.to = to;
    }

    public FetchCommand(MimeMessage[] msgs, long no) {
        statusResponse = "OK FETCH completed.";
        this.msgs = msgs;
        this.from = no;
        this.to = no;
        this.oneSeqNumberOnly = true;
    }

    public void setUseParenthesis(boolean useParenthesis) {
        this.useParenthesis = useParenthesis;
    }
    
    public void setUids(long[] uids) {
        this.uids=uids;
        this.uid=true;
    }

    public String getCommand() {
        String command = "";
        if (uid) {
            command += "UID ";
        }
        command += "fetch " + from;
        if (!oneSeqNumberOnly) {
            if (to > 0) {
                command += ":" + to;
            } else {
                command += ":*";
            }
        }

        command += " ";
        if (useParenthesis) {
            command += "(";
        }
        
        String items = "";
        // FLAGS
        if (fetchFlags) {
            items += " FLAGS";
        }
        // RFC822.SIZE
        if (fetchRfc822Size) {
            items += " RFC822.SIZE";
        }
        // BODY
        if (body != null) {
            items += " " + body.getCommand();
        }

        if (items.length() > 0) {
            items = items.substring(1);
        }
        command += items;
        if (useParenthesis) {
            command += ")";
        }
        command += "\n";
        return command;
    }

    private List getSelectedMessageNumbers() {
        List selectedNumbers = new ArrayList();
        if (uid) {
            final long to;
            if (this.to>0) {
                to=this.to;
            } else {
                to=Long.MAX_VALUE;
            }
            for (int i=0; i< msgs.length; i++) {
                if (uids[i]>=from && uids[i]<=to)  {
                    selectedNumbers.add(new Integer((int)i+1));
                }
            }

        } else {
            final long from;
            if (this.from > 0) {
                from = this.from;
            } else {
                from = 1;
            }

            final long to;
            if (this.to > 0) {
                if (this.to > msgs.length) {
                    to = msgs.length;
                } else {
                    to = this.to;
                }
            } else {
                to = msgs.length;
            }

            for (long i = from; i <= to; i++) {
                selectedNumbers.add(new Integer((int)i));
            }
        }
        
        return selectedNumbers;
    }
    public String getResultForMessageNumber(int no) throws MessagingException, IOException {
        final MimeMessage mm = msgs[no-1];
        String result = "";
        
        // FLAGS
        if (fetchFlags) {
            result +=" FLAGS ("+flagsToString(mm.getFlags())+")";
        }
        
        // UID
        if (uid) {
            final long uid=uids[no-1];
            result += " UID "+uid;
        }
        
        // RFC822.SIZE
        if (fetchRfc822Size) {
            final int size=mm.getSize();
            result += " RFC822.SIZE "+size;
        }

        // BODY
        if (body!=null) {
            result += " "+body.getResult(mm);
        }
        
        if (result.length()>0) {
             // without leading space
            result=result.substring(1);
        }
        return result;
    }
    
    public List getExpectedResponseList() throws MessagingException, IOException {
        List responseList = new LinkedList();

        List selectedNumbers = getSelectedMessageNumbers();
        
        for (Iterator it = selectedNumbers.iterator(); it.hasNext();) {
            final int no=((Integer)it.next()).intValue();
            String line = "* " + no + " FETCH (";
            line += getResultForMessageNumber(no);
            line += ")";
            responseList.add(line);
        }
        return responseList;

    }

    public static String flagToString(Flag flag) {
        if (flag.equals(Flag.ANSWERED)) {
            return "\\Answered";
        }
        if (flag.equals(Flag.DELETED)) {
            return "\\Deleted";
        }
        if (flag.equals(Flag.DRAFT)) {
            return "\\Draft";
        }
        if (flag.equals(Flag.FLAGGED)) {
            return "\\Flagged";
        }
        if (flag.equals(Flag.RECENT)) {
            return "\\Recent";
        }
        if (flag.equals(Flag.SEEN)) {
            return "\\Seen";
        }
        throw new IllegalArgumentException("unknown Flag: "+flag);

    }
    
    public static  String flagsToString(Flags flags) {
        String result="";
        Flag[] f=flags.getSystemFlags();
        for (int i = 0; i < f.length; i++) {
            result +=" "+flagToString(f[i]);
        }
        if (result.length()>0) {
             // without leading space
            result=result.substring(1);
        }
        return result;
    }

    public void setFetchFlags(boolean fetchFlags) {
        this.fetchFlags=fetchFlags;
        
    }

    public void setFetchRfc822Size(boolean fetchRfc822Size) {
        this.fetchRfc822Size=fetchRfc822Size;
        
    }
    

    public void setFetchBody(FetchBody body) {
        this.body=body;
        
    }

}
