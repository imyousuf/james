/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james;

import java.io.*;
import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;

/**
 * This object accompanies the MimeMessage as a message is processed by the James mail server.
 * It stores state specific information, the recipient list for this message, and other
 * properties.
 * @author Serge Knystautas <sergek@lokitech.com>
 * @version 0.9
 */
public class DeliveryState extends Hashtable implements Serializable {

    private int state;
    private String sender;
    private Vector recipients;

        // Default states
    public static final int DEFAULT_STATE = 1;
    public static final int NOT_PROCESSED = 1;
    public static final int PRE_PROCESSED = 3;
    public static final int PROCESSED = 5;
    public static final int POST_PROCESSED = 7;
    public static final int FAILED_DELIVERY = 8;
    public static final int FAILURE_PROCESSED = 9;
    public static final int ABORT = 10;
    public static final int DELIVERED = 11;
    public static final int HOST_NOT_FOUND = 1;
    public static final int INVALID_RECIPIENT = 2;
    public static final int UNKNOWN_HOST = 3;
    public static final int ACCOUNT_NOT_FOUND = 4;
    public static final int UNKNOWN_FAILURE = -1;

    public DeliveryState() {
        super();
        state = DEFAULT_STATE;
        recipients = new Vector();
        sender = new String();
    }

    public Vector getRecipients() {
        return recipients;
    }

    public void setRecipients(Vector recipients) {
        this.recipients = recipients;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }
    
    public String getSender() {
        return sender;
    }

    public void setState(int state) {
        this.state = state;
    }

    public int getState() {
        return state;
    }

    /**
     * This method was created in VisualAge.
     * @return java.lang.String
     */
    public String getStateText() {
        switch ( this.getState() ) {

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
}