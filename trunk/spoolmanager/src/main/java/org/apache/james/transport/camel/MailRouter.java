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
package org.apache.james.transport.camel;

import org.apache.camel.Body;
import org.apache.mailet.Mail;

/**
 * Route the mail to the right JMS queue depending on the state of the Mail. 
 * 
 * 
 *
 */
public class MailRouter {
    
    /**
     * Route Mail to the right JMS queue based on the state of the mail
     * 
     * @param mail
     * @return camel endpoint uri
     */
    public String to(@Body Mail mail) {
        String queueName = "activemq:queue:processor."+ mail.getState();
        return queueName;
    }
    

}
