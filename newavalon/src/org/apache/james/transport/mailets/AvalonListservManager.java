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
//import org.apache.avalon.services.Store;

import org.apache.james.*;
import org.apache.james.services.MailStore;
import org.apache.james.services.UsersRepository;
import org.apache.mailet.*;
import org.apache.james.transport.*;

/**
 * Adds or removes an email address to a listserv.
 *
 * Sample configuration:
 * <pre>
 * <mailet match="CommandForListserv=james@list.working-dogs.com" class="AvalonListservManager">
 *     <membersPath>file://../var/list-james</membersPath>
 * </mailet>
 * </pre>
 * @author  Serge Knystautas <sergek@lokitech.com>
 */
public class AvalonListservManager extends GenericListservManager {

    private UsersRepository members;

    public void init() {
        ComponentManager compMgr = (ComponentManager)getMailetContext().getAttribute(Constants.AVALON_COMPONENT_MANAGER);
	try {
	    MailStore mailstore = (MailStore) compMgr.lookup("org.apache.james.services.MailStore");
	    String membersPath = getInitParameter("membersPath");
	    DefaultConfiguration usersConf
		= new DefaultConfiguration("repository", "generated:AvalonListServManager");
	    usersConf.addAttribute("destinationURL", membersPath);
	    usersConf.addAttribute("type", "USERS");
	    usersConf.addAttribute("model", "SYNCHRONOUS");
	    
	    members = (UsersRepository) mailstore.select(usersConf);
     	} catch (ComponentNotFoundException cnfe) {
	    log("Failed to retrieve Store component:" + cnfe.getMessage());
	} catch (ComponentNotAccessibleException cnae) {
	    log("Failed to retrieve Store component:" + cnae.getMessage());
	} catch (Exception e) {
	    log("Failed to retrieve Store component:" + e.getMessage());
	}
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

