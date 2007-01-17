/**
 * 
 */
package org.apache.james.smtpserver.core;

import org.apache.james.smtpserver.LineHandler;
import org.apache.james.smtpserver.SMTPSession;

/**
 * DataLineFilter are used to check the Data stream while the message is
 * being received.
 */
public interface DataLineFilter {
    void onLine(SMTPSession session, byte[] line, LineHandler next);
}