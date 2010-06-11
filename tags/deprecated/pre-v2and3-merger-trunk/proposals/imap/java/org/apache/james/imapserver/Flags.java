/***********************************************************************
 * Copyright (c) 2000-2004 The Apache Software Foundation.             *
 * All rights reserved.                                                *
 * ------------------------------------------------------------------- *
 * Licensed under the Apache License, Version 2.0 (the "License"); you *
 * may not use this file except in compliance with the License. You    *
 * may obtain a copy of the License at:                                *
 *                                                                     *
 *     http://www.apache.org/licenses/LICENSE-2.0                      *
 *                                                                     *
 * Unless required by applicable law or agreed to in writing, software *
 * distributed under the License is distributed on an "AS IS" BASIS,   *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or     *
 * implied.  See the License for the specific language governing       *
 * permissions and limitations under the License.                      *
 ***********************************************************************/

package org.apache.james.imapserver;

import org.apache.avalon.framework.activity.Initializable;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * The set of flags associated with a message. The \Seen flag is maintained
 * on a per-user basis.
 *
 * <p>Reference: RFC 2060 - para 2.3
 * @author <a href="mailto:charles@benett1.demon.co.uk">Charles Benett</a>
 * @version 0.1 on 14 Dec 2000
 */
public class Flags 
    implements Serializable, Initializable {

    public static final int ANSWERED  = 0;
    public static final int DELETED   = 1;
    public static final int DRAFT     = 2;
    public static final int FLAGGED   = 3;
    public static final int RECENT    = 4;
    public static final int SEEN      = 5;

    // Array does not include seen flag
    private boolean[] flags = {false, false, false, false, true};

    //users who have seen this message
    private Set users; 

    public Flags() {
    }

    /**
     * Initialisation - only for object creation not on deserialisation.
     */
    public void initialize() {
        users = new HashSet();
    }

    /**
     * Returns IMAP formatted String of Flags for named user
     */
    public String getFlags(String user) {
        StringBuffer buf = new StringBuffer();
        buf.append("(");
        if (flags[ANSWERED]) { buf.append("\\ANSWERED ");}
        if (flags[DELETED]) { buf.append("\\DELETED ");}
        if (flags[DRAFT]) { buf.append("\\DRAFT ");}
        if (flags[FLAGGED]) { buf.append("\\FLAGGED ");}
        if (flags[RECENT]) { buf.append("\\RECENT ");}
        if (users.contains(user)) { buf.append("\\SEEN ");}
        buf.append(")");
        return buf.toString();
    }

    /**
     * Sets Flags for message from IMAP-forammted string parameter.
     * <BR> The FLAGS<list> form overwrites existing flags, ie sets all other
     * flags to false.
     * <BR> The +FLAGS<list> form adds the flags in list to the existing flags
     * <BR> The -FLAGS<list> form removes the flags in list from the existing
     * flags
     * <BR> Note that the Recent flag cannot be set by user and is ignored by
     * this method.
     *
     * @param flagString a string formatted according to
     * RFC2060 store_att_flags
     * @param user the String email address of the user
     * @return true if successful, false if not (including uninterpretable
     * argument)
     */
    public boolean setFlags(String flagString, String user) {
        flagString = flagString.toUpperCase();
        if (flagString.startsWith("FLAGS")) {
            boolean [] newflags = new boolean[5];
            newflags[ANSWERED]
                = (flagString.indexOf("\\ANSWERED") != -1) ? true : false;
            newflags[DELETED]
                = (flagString.indexOf("\\DELETED") != -1) ? true : false;
            newflags[DRAFT]
                = (flagString.indexOf("\\DRAFT") != -1) ? true : false;
            newflags[FLAGGED]
                = (flagString.indexOf("\\FLAGGED") != -1) ? true : false;
            newflags[RECENT] =  false;
            if (flagString.indexOf("\\SEEN") != -1) {
                users.add(user);
            }
            System.arraycopy(newflags, 0, flags, 0, newflags.length);
            return true;
        } else if (flagString.startsWith("+FLAGS") ||flagString.startsWith("-FLAGS") ) {
            boolean mod = (flagString.startsWith("+") ? true : false);
            if (flagString.indexOf("\\ANSWERED") != -1) {
                flags[ANSWERED] = mod;
            }
            if (flagString.indexOf("\\DELETED") != -1) {
                flags[DELETED] = mod;
            }
            if (flagString.indexOf("\\DRAFT") != -1) {
                flags[DRAFT] = mod;
            }
            if (flagString.indexOf("\\FLAGGED") != -1) {
                flags[FLAGGED] = mod;
            }
            if (flagString.indexOf("\\SEEN") != -1) {
                if( mod) {
                    users.add(user);
                } else {
                    users.remove(user);
                }
            }
            return true;
        } else {
            return false;
        }
    }

    public void setAnswered(boolean newState) {
        flags[ANSWERED] = newState;
    }

    public boolean isAnswered() {
        return flags[ANSWERED];
    }

    public void setDeleted(boolean newState) {
        flags[DELETED] = newState;
    }

    public boolean isDeleted() {
        return flags[DELETED];
    }

    public void setDraft(boolean newState) {
        flags[DRAFT] = newState;
    }

    public boolean isDraft() {
        return flags[DRAFT];
    }

    public void setFlagged(boolean newState) {
        flags[FLAGGED] = newState;
    }

    public boolean isFlagged() {
        return flags[FLAGGED];
    }

    public void setRecent(boolean newState) {
        flags[RECENT] = newState;
    }

    public boolean isRecent() {
        return flags[RECENT];
    }

    public void setSeen(boolean newState, String user) {
        if( newState) {
            users.add(user);
        } else {
            users.remove(user);
        }
    }

    public boolean isSeen(String user) {
        return users.contains(user);
    }
}

