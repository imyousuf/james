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
package org.apache.james.mailetcontainer.api;

import java.util.Collection;

import javax.mail.MessagingException;

import org.apache.mailet.MailAddress;
import org.apache.mailet.Mailet;
import org.apache.mailet.Matcher;

/**
 * A Listener which will get notified after {@link Mailet#service(org.apache.mailet.Mail)} and {@link Matcher#match(org.apache.mailet.Mail)} methods are called from
 * the container
 *  
 *
 */
public interface MailetContainerListener {

    /**
     * Get called after each {@link Mailet} call was complete 
     * 
     * @param m
     * @param mailName
     * @param state
     * @param processTime in ms
     * @param e or null if no {@link MessagingException} was thrown
     */
    public void afterMailet( Mailet m, String mailName, String state, long processTime, MessagingException e);
    
    /**
     * Get called after each {@link Matcher} call was complete 

     * @param m
     * @param mailName
     * @param recipients
     * @param matches 
     * @param processTime in ms
     * @param e or null if no {@link MessagingException} was thrown
     * 
     */
    public void afterMatcher( Matcher m,  String mailName, Collection<MailAddress> recipients, Collection<MailAddress> matches, long processTime, MessagingException e);
    
}
