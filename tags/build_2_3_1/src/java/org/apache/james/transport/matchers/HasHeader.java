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

package org.apache.james.transport.matchers;

import org.apache.mailet.GenericMatcher;
import org.apache.mailet.Mail;

import java.util.Collection;

/**
 * use: <mailet match="HasHeader=<header>" class="..." />
 *
 * This matcher simply checks to see if the header named is present.
 * If complements the AddHeader mailet.
 *
 * TODO: support lists of headers and values, e.g, match="{<header>[=value]}+"
 *       [will require a complete rewrite from the current trivial one-liner]
 *
 */
public class HasHeader extends GenericMatcher {

    public Collection match(Mail mail) throws javax.mail.MessagingException {
        return (mail.getMessage().getHeader(getCondition(), null) != null) ? mail.getRecipients() : null;
    }
}

