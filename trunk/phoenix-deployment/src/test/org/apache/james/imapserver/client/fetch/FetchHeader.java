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

package org.apache.james.imapserver.client.fetch;

import java.util.Enumeration;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

public class FetchHeader {

    private String[] fields;

    public String getCommand() {
        if (fields == null) {
            return "HEADER";
        } else {
            return "HEADER.FIELDS ("+getFormattedFieldList()+")";
        }
    }

    private String getFormattedFieldList() {
        String result ="";
        for (int i = 0; i < fields.length; i++) {
            result +=" "+fields[i];
            
        }
        if (result.length()>0) {
            result=result.substring(1);
        }
        return result;
    }

    public String getData(MimeMessage m) throws MessagingException {
        String result = "";
        final Enumeration e;
        if (fields==null) {
        e= m.getAllHeaderLines();
        } else {
            e = m.getMatchingHeaderLines(fields);
        }
        while (e.hasMoreElements()) {
            String line = (String) e.nextElement();
            result += line + "\r\n";
        }
        result += "\r\n"; // TODO Should this be counted for size?
        return result;
    }

    public void setFields(String[] fields) {
        this.fields = fields;

    }

}
