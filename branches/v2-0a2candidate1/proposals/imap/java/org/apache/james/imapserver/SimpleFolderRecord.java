/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.imapserver;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import org.apache.avalon.framework.activity.Initializable;

/**
 * Object representing the record of a folder in an IMAP on an IMAP Host.
 * 
 * @author <a href="mailto:charles@benett1.demon.co.uk">Charles Benett</a>
 * @version 0.1 on 14 Dec 2000
 */

public class SimpleFolderRecord 
    implements FolderRecord, Serializable, Initializable {

    private final String fullName; 
    private final String owner;
    private final String absoluteName;

    private boolean nameInUse;
    private boolean deleted;
    private int uidValidity;
    private int highestUID;
    private Set usersWithLookupRights;
    private Set usersWithReadRights;
    private boolean marked;
    private boolean notSelectableByAnyone;
    private int exists;
    private int recent;
    private Map unseenByUser;

    /**
     * Constructor Records the full name, including namespace, of this mailbox
     * relative, to a specified user, and the absolute name.. 
     *
     * @param mailboxName String mailbox hierarchical name including namespace
     * @param user String a user. An empty user parameter indicates that the 
     * mailbox name is absolute.
     */
    public SimpleFolderRecord(String mailboxName, String user,
                              String absName) {
        fullName = mailboxName;
        owner = user;
        absoluteName = absName;
    }

    public void initialize() {
        nameInUse = true;
        deleted = false;
        uidValidity = 1; 
        highestUID = 1; 
    }

    public String getFullName() {
        return fullName;
    }

    public String getUser() {
        return owner;
    }

    public String getAbsoluteName() {
        return absoluteName;
    }

    public void setNameInUse(boolean state) {
        nameInUse = state;
    }

    public boolean isNameInUse() {
        return nameInUse;
    }

    public void setDeleted(boolean state) {
        deleted = state;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setUidValidity(int uidV) {
        if (uidV > uidValidity) {
            uidValidity = uidV;
        }
    }

    public int getUidValidity() {
        return uidValidity;
    }

    public void setHighestUid(int uid) {
        highestUID = uid;
    }

    public int getHighestUid() {
        return highestUID;
    }

    public void setLookupRights(Set users) {
        usersWithLookupRights = users;
    }

    public boolean hasLookupRights(String user) {
        return usersWithLookupRights.contains(user) ;
    }
 
    public void setReadRights(Set users) {
        usersWithReadRights = users;
    }

    public boolean hasReadRights(String user) {
        return usersWithReadRights.contains(user) ;
    }

    public void setMarked(boolean mark) {
        marked = mark;
    }

    public boolean isMarked() {
        return marked;
    }

    public void setNotSelectableByAnyone(boolean state) {
        notSelectableByAnyone = state;
    }

    public boolean isNotSelectableByAnyone() {
        return notSelectableByAnyone;
    }

    public boolean isSelectable(String user) {
        return (!notSelectableByAnyone && hasReadRights(user));
    }

    public void setExists(int num) {
        exists = num;
    }

    public int getExists() {
        return exists;
    }

    public void setRecent(int num) {
        recent = num;
    }

    public int getRecent() {
        return recent;
    }

    public void setUnseenbyUser(Map unseen) {
        unseenByUser = unseen;
    }

    public int getUnseen(String user) {
        if (unseenByUser.containsKey(user)) {
            Integer unseen = ((Integer)unseenByUser.get(user));
            return unseen.intValue();
        } else {
            return exists;
        }
    }
}
