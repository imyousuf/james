package org.apache.james.smtpserver.protocol.core;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.james.smtpserver.protocol.LineHandler;
import org.apache.james.smtpserver.protocol.SMTPSession;
import org.apache.mailet.base.RFC2822Headers;
import org.apache.mailet.base.RFC822DateFormat;

public class ReceivedDataLineFilter implements DataLineFilter {

    private final static String SOFTWARE_TYPE = "JAMES SMTP Server ";

    // Replace this with something usefull
    // + Constants.SOFTWARE_VERSION;

    /**
     * Static RFC822DateFormat used to generate date headers
     */
    private final static RFC822DateFormat rfc822DateFormat = new RFC822DateFormat();
    private final static String HEADERS_WRITTEN = "HEADERS_WRITTEN";

    /**
     * @see org.apache.james.smtpserver.protocol.core.DataLineFilter#onLine(org.apache.james.smtpserver.protocol.SMTPSession,
     *      byte[], org.apache.james.smtpserver.protocol.LineHandler)
     */
    public void onLine(SMTPSession session, byte[] line, LineHandler next) {
        if (session.getState().containsKey(HEADERS_WRITTEN) == false) {
            addNewReceivedMailHeaders(session, next);
            session.getState().put(HEADERS_WRITTEN, true);
        }
        next.onLine(session, line);
    }

    private void addNewReceivedMailHeaders(SMTPSession session, LineHandler next) {
        StringBuilder headerLineBuffer = new StringBuilder();

        String heloMode = (String) session.getConnectionState().get(
                SMTPSession.CURRENT_HELO_MODE);
        String heloName = (String) session.getConnectionState().get(
                SMTPSession.CURRENT_HELO_NAME);

        // Put our Received header first
        headerLineBuffer.append(RFC2822Headers.RECEIVED + ": from ").append(
                session.getRemoteHost());

        if (heloName != null) {
            headerLineBuffer.append(" (").append(heloMode).append(" ").append(
                    heloName).append(") ");
        }

        headerLineBuffer.append(" ([").append(session.getRemoteIPAddress())
                .append("])").append("\r\n");
        next.onLine(session, headerLineBuffer.toString().getBytes());
        headerLineBuffer.delete(0, headerLineBuffer.length());

        headerLineBuffer.append("          by ").append(session.getHelloName())
                .append(" (").append(SOFTWARE_TYPE).append(") with ");

        // Check if EHLO was used
        if ("EHLO".equals(heloMode)) {
            // Not successful auth
            if (session.getUser() == null) {
                headerLineBuffer.append("ESMTP");
            } else {
                // See RFC3848
                // The new keyword "ESMTPA" indicates the use of ESMTP when the
                // SMTP
                // AUTH [3] extension is also used and authentication is
                // successfully
                // achieved.
                headerLineBuffer.append("ESMTPA");
            }
        } else {
            headerLineBuffer.append("SMTP");
        }

        headerLineBuffer.append(" ID ").append(session.getSessionID());

        if (((Collection) session.getState().get(SMTPSession.RCPT_LIST)).size() == 1) {
            // Only indicate a recipient if they're the only recipient
            // (prevents email address harvesting and large headers in
            // bulk email)
            headerLineBuffer.append("\r\n");
            next.onLine(session, headerLineBuffer.toString().getBytes());
            headerLineBuffer.delete(0, headerLineBuffer.length());

            headerLineBuffer.delete(0, headerLineBuffer.length());
            headerLineBuffer.append("          for <").append(
                    ((List) session.getState().get(SMTPSession.RCPT_LIST)).get(
                            0).toString()).append(">;").append("\r\n");

            next.onLine(session, headerLineBuffer.toString().getBytes());
            headerLineBuffer.delete(0, headerLineBuffer.length());

            headerLineBuffer.delete(0, headerLineBuffer.length());
        } else {
            // Put the ; on the end of the 'by' line
            headerLineBuffer.append(";");
            headerLineBuffer.append("\r\n");
            next.onLine(session, headerLineBuffer.toString().getBytes());
            headerLineBuffer.delete(0, headerLineBuffer.length());
        }
        headerLineBuffer = null;
        next.onLine(session, ("          "
                + rfc822DateFormat.format(new Date()) + "\r\n").getBytes());
    }
}
