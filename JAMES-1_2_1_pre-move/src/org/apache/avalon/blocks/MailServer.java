/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.avalon.blocks;

import javax.mail.internet.*;
import javax.mail.MessagingException;
import java.util.*;
import org.apache.mailet.*;
import org.apache.avalon.*;
import org.apache.james.*;
import org.apache.james.mailrepository.*;
import java.io.*;

/**
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 */

public interface MailServer {

    public void sendMail(MailAddress sender, Collection recipients, MimeMessage msg)
    throws MessagingException;

    public void sendMail(MailAddress sender, Collection recipients, InputStream msg)
    throws MessagingException;

    public void sendMail(Mail mail)
    throws MessagingException;

    public MailRepository getUserInbox(String userName);

    public String getId();

    public boolean addUser(String userName, String password);
}
