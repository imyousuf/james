package org.apache.james.core;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import javax.mail.*;
import javax.mail.internet.*;

/**
 * EnhancedMimeMessage is a direct subclass of MimeMessage that overrides the
 * getLineCount method of javax.mail.internet.MimeMessage with one that works
 * (kind of) and adds additional methods writeContentTo and getMessageSize.
 *
 * @author <a href="mailto:charles@benett1.demon.co.uk">Charles Benett</a>
 * @version 0.1  on 14 Dec 2000
 */
public class EnhancedMimeMessage extends MimeMessage {
  

    public EnhancedMimeMessage(Session session, InputStream in)
	throws MessagingException {
        super(session, in);
    }

    public  EnhancedMimeMessage(MimeMessage message)
	throws MessagingException {
        super(message);
    }


    /**
     * Corrects JavaMail 1.1 version which always returns -1.
     * Only corrected for content less than 5000 bytes,
     * to avoid memory hogging.
     */
    public int getLineCount() throws MessagingException {
	if (content == null) {
	    return -1;
	}
	int size = content.length; // size of byte array
	int lineCount = 0;
	if (size < 5000) {
	    for (int i=0; i < size -2; i++) {
		if (content[i] == '\r' && content[i+1] == '\n') {
		    lineCount++;
		}
	    }
	    if (content[size -2] != '\r' || content[size-1] != '\n') {
		//message has a non-empty final line.
		lineCount++;
	    }
	} else {
	    lineCount = -1;
	}
        return lineCount;
    }


    /**
     * Writes content only, ie not headers, to the specified outputstream.
     */
    public void writeContentTo(OutputStream outs)
	throws java.io.IOException, MessagingException {
	int size = content.length; // size of byte array
	int chunk = 1000; //arbitrary choice - ideas welcome
	int pointer = 0;
	while (pointer < size) {
	    if ((size - pointer) > chunk) {
		outs.write(content, pointer, chunk);
	    } else {
		outs.write(content, pointer, size-pointer);
	    }
	    pointer += chunk;
	}
    }

    /**
     * Returns size of message, ie headers and content. Current implementation
     * actually returns number of characters in headers plus number of bytes
     * in the internal content byte array.
     */
    public int getMessageSize() throws MessagingException {
	int contentSize = content.length;
	int headerSize = 0;
	Enumeration e = getAllHeaderLines();
	while (e.hasMoreElements()) {
	    headerSize += ((String)e.nextElement()).length();
	}
	return headerSize + contentSize;
    }


}


