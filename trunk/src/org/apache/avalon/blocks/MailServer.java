/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/
 
package org.apache.avalon.blocks;

import javax.mail.internet.*;
import java.util.*;
import org.apache.arch.*;
import org.apache.james.*;

/**
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 */

public interface MailServer {

    public void sendMail(String sender, Vector recipients, MimeMessage msg);
    
    public void sendMail(String sender, Vector recipients, String body);

    public MessageContainerRepository getInbox();

    public MessageContainerRepository getUserInbox(String userName);

    /* to be extended with methods like

    public OutputStream sendMail(String sender, Vector recipients);
    
    and something to retrive mails from mailbox like javax.mail.Store etc.*/
}