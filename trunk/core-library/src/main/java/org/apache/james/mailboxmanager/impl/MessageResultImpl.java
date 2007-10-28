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

import java.util.Date;

import javax.mail.Flags;
import javax.mail.Flags.Flag;
import javax.mail.internet.MimeMessage;

import org.apache.james.mailboxmanager.MessageResult;
import org.apache.mailet.Mail;

public class MessageResultImpl implements MessageResult {
    
    private MimeMessage mimeMessage;
    private long uid;
    private int msn;
    private Flags flags;
    private int size;
    private Date internalDate;
    private String key;
    private Headers headers;
    

    public MessageResultImpl(long uid) {
        this.uid=uid;
    }

    public MessageResultImpl() {
    }

    public MessageResultImpl(long uid, Flags flags) {
        this.uid=uid;
        this.flags=flags;
    }

    public MessageResultImpl(MessageResult origMr) {
        this.uid=origMr.getUid();
        this.flags=origMr.getFlags();
        this.mimeMessage=origMr.getMimeMessage();
    }

    public int getIncludedResults() {
        // TODO Auto-generated method stub
        return 0;
    }

    public boolean contains(int result) {
        // TODO Auto-generated method stub
        return false;
    }

    public MimeMessage getMimeMessage() {
        return mimeMessage;
    }

    public void setMimeMessage(MimeMessage mimeMessage) {
        this.mimeMessage=mimeMessage;
    }

    
    public long getUid() {
        return uid;
    }

    public long getUidValidity() {
        // TODO Auto-generated method stub
        return 0;
    }

    public int getMsn() {
        return msn;
    }

    public Date getInternalDate() {
        return internalDate;
    }

    public Flags getFlags() {
        return flags;
    }

    public Mail getMail() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getKey() {
        return key;
    }

    public void setUid(long uid) {
        this.uid=uid;
    }
    public void setMsn(int msn) {
        this.msn=msn;
    }
    public int getSize() {
        return size;
    }

    public void setFlags(Flags flags) {
        this.flags=flags;
    }

    public int compareTo(Object o) {
        MessageResult that=(MessageResult)o;
        if (this.uid>0 && that.getUid()>0) {
            return new Long(uid).compareTo(new Long(that.getUid()));    
        } else {
            throw new RuntimeException("can't compare");
        }
        
    }

    public void setSize(int size) {
        this.size=size;
    }

    public void setInternalDate(Date internalDate) {
        this.internalDate = internalDate;
    }
    
    public String toString() {
        return "UID: "+uid+" FLAGS: "+flagsToString(flags);
    }

    public static  String flagsToString(Flags flags) {
        if (flags==null) {
            return "null";
        }
        String result="";
        Flag[] f=flags.getSystemFlags();
        for (int i = 0; i < f.length; i++) {
            result +=" "+flagToString(f[i]);
        }
        if (result.length()>0) {
             // without leading space
            result=result.substring(1);
        }
        return result;
    }
    public static String flagToString(Flag flag) {
        if (flag.equals(Flag.ANSWERED)) {
            return "\\Answered";
        }
        if (flag.equals(Flag.DELETED)) {
            return "\\Deleted";
        }
        if (flag.equals(Flag.DRAFT)) {
            return "\\Draft";
        }
        if (flag.equals(Flag.FLAGGED)) {
            return "\\Flagged";
        }
        if (flag.equals(Flag.RECENT)) {
            return "\\Recent";
        }
        if (flag.equals(Flag.SEEN)) {
            return "\\Seen";
        }
        throw new IllegalArgumentException("unknown Flag: "+flag);

    }

    public void setKey(String key) {
        this.key=key;
    }

    public Headers getHeaders() {
        return headers;
    }

    public void setHeaders(Headers headers) {
        this.headers = headers;
    }
}
