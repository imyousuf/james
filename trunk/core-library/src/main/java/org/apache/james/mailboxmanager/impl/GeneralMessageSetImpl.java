/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.mailboxmanager.impl;

import org.apache.james.mailboxmanager.GeneralMessageSet;

public class GeneralMessageSetImpl implements GeneralMessageSet {

    private int type;

    private long uidFrom;

    private long uidTo;

    private String key;

    private GeneralMessageSetImpl() {
    }

    public int getType() {
        return type;
    }

    public long getUidFrom() throws IllegalStateException {
        if (type != TYPE_UID)
            throw new IllegalStateException("not in UID mode");
        return uidFrom;
    }

    public long getUidTo() throws IllegalStateException {
        if (type != TYPE_UID)
            throw new IllegalStateException("not in UID mode");
        return uidTo;
    }

    public String getKey() throws IllegalStateException {
        return key;
    }

    public static GeneralMessageSet oneUid(long uid) {
        GeneralMessageSetImpl gms = new GeneralMessageSetImpl();
        gms.type = TYPE_UID;
        gms.uidFrom = uid;
        gms.uidTo = uid;
        return gms;
    }

    public static GeneralMessageSet all() {
        GeneralMessageSetImpl gms = new GeneralMessageSetImpl();
        gms.type = TYPE_ALL;
        return gms;
    }

    public static GeneralMessageSet uidRange(long from, long to) {
        GeneralMessageSetImpl gms = new GeneralMessageSetImpl();
        if (to == Long.MAX_VALUE) {
            to = -1;
        }
        gms.type = TYPE_UID;
        gms.uidFrom = from;
        gms.uidTo = to;
        return gms;
    }

    public String toString() {
        return "TYPE: " + type + " UID: " + uidFrom + ":" + uidTo;
    }

    public boolean isValid() {
        if (type == TYPE_ALL) {
            return true;
        } else if (type == TYPE_UID) {
            if (uidTo < 0) {
                return true;
            } else {
                return (uidFrom <= uidTo);
            }
        } else {
            return false;
        }

    }

    public static GeneralMessageSet oneKey(String key) {
        GeneralMessageSetImpl gms = new GeneralMessageSetImpl();
        gms.type = TYPE_KEY;
        gms.key=key;
        return gms; 
    }

    public static GeneralMessageSet nothing() {
        GeneralMessageSetImpl gms = new GeneralMessageSetImpl();
        gms.type = TYPE_NOTHING;
        return gms;
    }
}
