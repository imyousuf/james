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

import javax.mail.MessagingException;

/**
 * A Listener which will get called after {@link CompositeMailProcessor#service(org.apache.mailet.Mail)} was called
 *
 */
public interface CompositeMailProcessorListener {

    /**
     * Get called after the processing via a {@link MailProcessor} was complete
     * 
     * @param processor
     * @param mailName
     * @param processTime in ms
     * @param e or null if no exception was thrown
     */
    public void afterProcessor(MailProcessor processor, String mailName, long processTime, MessagingException e);

}
