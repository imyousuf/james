/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2003 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache", "Jakarta", "JAMES" and "Apache Software Foundation"
 *    must not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 * Portions of this software are based upon public domain software
 * originally written at the National Center for Supercomputing Applications,
 * University of Illinois, Urbana-Champaign.
 */

package org.apache.james.imapserver;

import org.apache.avalon.framework.activity.Initializable;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

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
    public SimpleFolderRecord( String user,
                               String absName) {
        //fullName = mailboxName;
        owner = user;
        absoluteName = absName;
        fullName = absName;
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
