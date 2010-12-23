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

package org.apache.james.mailetcontainer.lib.mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.mail.MessagingException;

import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;
import org.apache.mailet.Matcher;
import org.apache.mailet.MatcherConfig;

public class MockMatcher implements Matcher{

    private List<String> matches = new ArrayList<String>();
    private MatcherConfig config;
    
    public void destroy() {
        
    }

    public MatcherConfig getMatcherConfig() {
        return config;
    }

    public String getMatcherInfo() {
        return getClass().getName();
    }

    public void init(MatcherConfig config) throws MessagingException {
        this.config = config;
        matches.addAll(Arrays.asList(config.getCondition().split(",")));
    }

    public Collection match(Mail mail) throws MessagingException {
        List<MailAddress> match = new ArrayList<MailAddress>();
        
        Iterator<MailAddress> rcpts = mail.getRecipients().iterator();
        while (rcpts.hasNext()) {
            MailAddress addr = rcpts.next();
            if (matches.contains(addr.toString())) {
                match.add(addr);
            }
        }
        if (match.isEmpty()) {
            return null;
        }
        return match;
    }

}
