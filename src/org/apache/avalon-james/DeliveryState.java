/*--- formatted by Jindent 2.0b, (www.c-lab.de/~jindent) ---*/

package org.apache.james;

import java.io.*;
import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;
import org.apache.avalon.*;
import org.apache.avalon.blocks.*;
import org.apache.avalon.configuration.*;

/**
 * This object accompanies the MimeMessage as a message is processed by the James mail server.
 * It stores state specific information, the recipient list for this message, and other
 * properties.
 * @author Serge Knystautas <sergek@lokitech.com>
 * @version 0.9
 */
public class DeliveryState implements Serializable {
    protected InternetAddress[] recipients;
    protected int state = NOT_PROCESSED;
    protected Properties hash;

    // The message has just been added to the queue and not preprocessed

    public static final int NOT_PROCESSED = 1;

    // The message is being pre-processed
    // public static final int PRE_PROCESSING = 2;
    // The message has been pre-processed, but needs to be processed

    public static final int PRE_PROCESSED = 3;

    // The message is being processed
    // public static final int PROCESSING = 4;
    // The message has been processed and needs to be post-processed

    public static final int PROCESSED = 5;

    // The message is being post-processed
    // public static final int POST_PROCESSING = 6;
    // The message has been post-processed and needs to be delivered to the appropriate server

    public static final int POST_PROCESSED = 7;

    // The message tried to be sent to a server, but was not deliverable and needs to be post-failure processed

    public static final int FAILED_DELIVERY = 8;

    // The message has been post-failure-processed

    public static final int FAILURE_PROCESSED = 9;

    // The message should no longer be processed (we want to drop it out of the loop and let it die)

    public static final int ABORT = 10;

    // The message was successfully delivered

    public static final int DELIVERED = 11;

    // The message couldn't be delivered because the host could not be found

    public static final int HOST_NOT_FOUND = 1;

    // The recipient is invalid

    public static final int INVALID_RECIPIENT = 2;

    // While we found a DNS entry to an A name... when we tried to connect we got java.net.UnknownHostException

    public static final int UNKNOWN_HOST = 3;

    // The message was trying to connect to a local address... this means the account given was invalid

    public static final int ACCOUNT_NOT_FOUND = 4;

    // Custom failure message... we'll eventually determine what more of these are and convert to int values

    public static final int UNKNOWN_FAILURE = -1;

    /**
     * DeliveryState constructor comment.
     */
    public DeliveryState() {
        hash = new Properties();
    }

    /**
     * Recreates a delivery state using an input stream
     * @param in InputStream
     */
    public DeliveryState(InputStream in) throws IOException {
        Properties props = new Properties();

        props.load(in);

        state = Integer.parseInt(props.getProperty("state"));

        int count = Integer.parseInt(props.getProperty("recipient.count"));

        recipients = new InternetAddress[count];

        for (int i = 0; i < recipients.length; i++) {
            try {
                recipients[i] = new InternetAddress(props.getProperty("recipient." + i));
            } catch (AddressException ae) {
                ae.printStackTrace();
            }
        }

        hash = props;
    }

    /**
     * This method was created in VisualAge.
     * @return java.util.Enumeration
     */
    public Enumeration getKeys() {
        return hash.keys();
    }

    /**
     * This method was created in VisualAge.
     * @return java.lang.String
     * @param key java.lang.String
     */
    public String getProperty(String key) {
        return hash.getProperty(key);
    }

    /**
     * Get the recipients this message is targeted for
     * @return Address[]
     */
    public InternetAddress[] getRecipients() {
        return recipients;
    }

    /**
     * What is the state of this message
     * @return int
     */
    public int getState() {
        return state;
    }

    /**
     * This method was created in VisualAge.
     * @return java.lang.String
     */
    public String getStateText() {
        switch (state) {

        case NOT_PROCESSED: 
            return "Not processed";

        case PRE_PROCESSED: 
            return "Pre-processed";

        case PROCESSED: 
            return "Processed";

        case POST_PROCESSED: 
            return "Post-processed";

        case FAILED_DELIVERY: 
            return "Failed delivery";

        case FAILURE_PROCESSED: 
            return "Failed processed";

        case ABORT: 
            return "Aborted";

        case DELIVERED: 
            return "Delivered";
        }

        return "unknown";
    }

    /**
     * This method was created in VisualAge.
     * @param name java.lang.String
     * @param value java.lang.String
     */
    public void put(String name, String value) {
        hash.put(name, value);
    }

    /**
     * Set the recipients for this message.  Allows you to redirect the message to other
     * recipients.  Be sure to modify the To: header so that the receiving mail servers will know
     * what to do with them.
     * @param recipients javax.mail.Address[]
     */
    public void setRecipients(InternetAddress[] recipients) {
        this.recipients = recipients;
    }

    /**
     * Update the state of the message
     * @param state int
     */
    public void setState(int state) {
        this.state = state;
    }

    /**
     * This method was created in VisualAge.
     * @param out java.io.OutputStream
     */
    public void writeTo(OutputStream outstream) throws IOException {
        PrintWriter out = new PrintWriter(outstream);

        out.println("state=" + state);
        out.println("recipient.count=" + recipients.length);

        for (int i = 0; i < recipients.length; i++) {
            out.println("recipient." + i + "=" + recipients[i]);
        }
        for (Enumeration e = hash.keys(); e.hasMoreElements(); ) {
            String key = (String) e.nextElement();

            if (key.equals("state") || key.startsWith("recipient")) {
                continue;
            } 

            out.println(key + "=" + hash.get(key));
        }

        out.flush();
    }

}



/*--- formatting done in "Sun Java Convention" style on 07-11-1999 ---*/

