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

package org.apache.james.api.imap.message.response;

public interface AddressVisitor {
    /**
     * <p>
     * An RFC2060 <code>address</code>.
     * </p>
     * @param addressName <code>addr_name</code> phrase from RFC822 <code>mailbox</code,
     * or null if the header is not present in the message
     * @param addressAdl <code>addr_adl</code> phrase from RFC822 <code>route-addr</code>,
     * or null if the header is not present in the message
     * @param addressMailbox null to indicate the end of the RFC822 group.
     * When not null: if <code>addressHost</> is null, this holds
     * the RFC822 group name otherwise, the RFC822 <code>local-part</code>
     * @param addressHost null indicates RFC822 group syntax.
     * Not null holds RFC822 domain name
     */
    public void address(CharSequence addressName, CharSequence addressAdl, 
            CharSequence addressMailbox, CharSequence addressHost);
}