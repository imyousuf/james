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
import org.apache.avalon.*;
import org.apache.avalon.blocks.*;
import org.apache.avalon.configuration.*;

/**
 * Wrap a mail (viewed as Stream).
 * @author Federico Barbieri <scoobie@systemy.it>
 * @version 0.9
 */
public class MessageContainer implements Serializable {

    private InputStream in;
    private DeliveryState state;
    private String messageId;

    public MessageContainer() {
        state = new DeliveryState();
    }

    public MessageContainer(String sender, Vector recipients) {
        state = new DeliveryState();
        state.setSender(sender);
        state.setRecipients(recipients);
        this.in = null;
    }

    public void setBodyInputStream(InputStream i) {
        in = i;
    }

    public InputStream getBodyInputStream() {
        return in;
    }	

    public void setState(DeliveryState ds) {
        state = ds;
    }

    public DeliveryState getState() {
        return state;
    }
    
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
    
    public String getMessageId() {
        return messageId;
    }
}
