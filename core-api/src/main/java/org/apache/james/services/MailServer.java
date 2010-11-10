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



package org.apache.james.services;


import org.apache.mailet.MailAddress;

/**
 * The interface for Phoenix blocks to the James MailServer
 *
 *
 * @version This is $Revision$
 */
public interface MailServer{
  
    /**
     * Generate a new identifier/name for a mail being processed by this server.
     *
     * @return the new identifier
     */
    String getId();
    
    /**
     * Return true if virtualHosting support is enabled, otherwise false
     * 
     * @return true or false
     */
    boolean supportVirtualHosting();
    
    /**
     * Return the default domain which will get used to deliver mail to if only the localpart
     * was given on rcpt to.
     * 
     * @return the defaultdomain
     */
    String getDefaultDomain();
    
    /**
     * Return the helloName which should use for all services by default
     * 
     * @return the helloName
     */
    String getHelloName();
    
    /**
     * Return the {@link MailAddress} of the postmaster
     * 
     * @return postmaster
     */
    public MailAddress getPostmaster();
}
