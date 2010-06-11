package org.apache.james.experimental.imapserver.client.fetch;

import java.io.IOException;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.james.experimental.imapserver.util.MessageGenerator;

public class FetchBody {

    final boolean peek;

    private FetchHeader fetchHeader;

    public FetchBody(boolean peek) {
        this.peek = peek;
    }

    public String getCommand() {
        String result= "";
        if (peek) {
            result += "BODY.PEEK[";
        } else {
            result += "BODY[";
        }
        if (fetchHeader!=null) {
            result += fetchHeader.getCommand();
        }
        result += "]";
        return result;
    }

    public String getResult(MimeMessage m) throws IOException,
            MessagingException {
        // TODO decide whether it should be BODY.PEEK when peek!
        String result = "BODY[";
        final String data;
        if (fetchHeader != null) {
            result += fetchHeader.getCommand();
            data = fetchHeader.getData(m);
        } else {
            data = getData(m);
        }
        result += "] {" + data.length() + "}\r\n" + data;
        // TODO Shouldn't we append another CRLF?
        return result;
    }

    private String getData(MimeMessage m) throws IOException,
            MessagingException {
        String data = MessageGenerator.messageContentToString(m);
        return data;
    }

    public void setFetchHeader(FetchHeader fetchHeader) {
        this.fetchHeader = fetchHeader;

    }

}
