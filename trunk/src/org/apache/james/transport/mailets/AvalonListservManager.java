/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.transport.mailets;

import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;
import org.apache.avalon.*;
import org.apache.james.*;
import org.apache.james.usermanager.*;
import org.apache.avalon.blocks.*;
import org.apache.mailet.*;
import org.apache.james.transport.*;

/**
 * Adds or removes an email address to a listserv.
 *
 * Sample configuration:
 * <pre>
 * <mailet match="CommandForListserv=james@list.working-dogs.com" class="AvalonListservManager">
 *     <user_repository>james-listserv</user_repository>
 * </mailet>
 * </pre>
 * @author  Serge Knystautas <sergek@lokitech.com>
 */
public class AvalonListservManager extends GenericListservManager {

    private UsersRepository members;

    public void init() {
        ComponentManager comp = (ComponentManager)getMailetContext().getAttribute(Constants.AVALON_COMPONENT_MANAGER);
        UserManager manager = (UserManager) comp.getComponent(Resources.USERS_MANAGER);
        members = (UsersRepository) manager.getUserRepository("list-" + getInitParameter("listName"));
    }

    public boolean addAddress(MailAddress address) {
        members.addUser(address.toString(), "");
        return true;
    }

    public boolean removeAddress(MailAddress address) {
        members.removeUser(address.toString());
        return true;
    }

    public String getMailetInfo() {
        return "AvalonListservManager Mailet";
    }
}

