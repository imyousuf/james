/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.transport.matchers;

import org.apache.mail.*;
import org.apache.james.transport.*;
import org.apache.james.usermanager.*;
import org.apache.java.util.*;
import java.util.*;

/**
 * @version 1.0.0, 24/04/1999
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 */
public class RecipientIsLocal extends AbstractMatcher {
    
    private Vector localhosts;
    private UsersRepository users;
    
    public void init(String condition) {
        localhosts = (Vector) getContext().get(Resources.SERVER_NAMES);
        UserManager usersManager = (UserManager) getContext().getComponentManager().getComponent(Resources.USERS_MANAGER);
        users = usersManager.getUserRepository("LocalUsers");
    }

    public Mail[] match(Mail mail) {
        Vector matching = new Vector();
        Vector notMatching = new Vector();
        for (Enumeration e = mail.getRecipients().elements(); e.hasMoreElements(); ) {
            String rec = (String) e.nextElement();
            if (localhosts.contains(Mail.getHost(rec)) && users.contains(Mail.getUser(rec))) {
                matching.addElement(rec);
            }
        }
        notMatching = VectorUtils.subtract(mail.getRecipients(), matching);
        return split(mail, matching, notMatching);
    }
}