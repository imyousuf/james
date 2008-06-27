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
package org.apache.james.imapserver.client;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.mail.Flags;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

public class StatusClientCommand implements Command {

    private String folder;

    private LinkedHashMap statusMap = new LinkedHashMap();

    private long uidNext;

    private long uidValidity;

    private MimeMessage[] msgs;

    public StatusClientCommand(String folder, MimeMessage[] msgs, long uidNext,
            long uidValidity) throws MessagingException {
        this.msgs=msgs;
        this.folder = folder;
        this.uidNext = uidNext ;
        this.uidValidity = uidValidity;
    }

    public String getCommand() {
        String command = "STATUS \"" + folder + "\" ";
        command += getStatusList(false);
        return command;
    }
    
    public List getExpectedResponseList() throws MessagingException, IOException {
        String response="* STATUS "+folder+" ";
        response += getStatusList(true);
        return Arrays.asList(new String[] {response});
    }

    protected String getStatusList(boolean withValues) {
        String list="(";
        for (Iterator iter = statusMap.entrySet().iterator(); iter.hasNext();) {
            Map.Entry entry= (Map.Entry) iter.next();
            list += entry.getKey().toString();
            if (withValues) {
                list += " " + entry.getValue().toString();    
            }
            if (iter.hasNext()) {
                list += " ";
            }
        }
        list += ")";
        return list;
    }
    
    public String getExpectedStatusResponse() {
        return "OK STATUS completed.";
    }

    protected void selectStatus(String status, boolean add,long value) {
        if (add) {
            statusMap.put(status,new Long(value));
        } else {
            statusMap.remove(status);
        }
    }

    public void setStatusMessages(boolean statusMessages) {
        selectStatus("MESSAGES", statusMessages,msgs.length);
    }

    public void setStatusRecent(boolean statusRecent) throws MessagingException {

        selectStatus("RECENT", statusRecent,countFlags(Flags.Flag.RECENT,true));
    }
    
    protected int countFlags(Flags.Flag flag, boolean value) throws MessagingException {
        int count=0;
        for (int i = 0; i < msgs.length; i++) {
            if (msgs[i].getFlags().contains(flag) == value) {
                count++;
            }
        }
        return count;
    }

    public void setStatusUidNext(boolean statusUidNext) {
        selectStatus("UIDNEXT", statusUidNext,uidNext);
    }

    public void setStatusUidValidity(boolean statusUidValidity) {
        selectStatus("UIDVALIDITY", statusUidValidity,uidValidity);
    }

    public void setStatusUnseen(boolean statusUnseen) throws MessagingException {
        selectStatus("UNSEEN", statusUnseen,countFlags(Flags.Flag.SEEN,false));
    }



}
