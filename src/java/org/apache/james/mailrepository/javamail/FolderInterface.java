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



package org.apache.james.mailrepository.javamail;

import javax.mail.Message;
import javax.mail.MessagingException;

/**
 * Interface to javax.mail.Folder functionality to be able to replace 
 * implementation or using Mocks when testing 
 */

public interface FolderInterface {

    public Message[] getMessages() throws MessagingException;

    public Message getMessage(int no) throws MessagingException;

    public int getMessageCount() throws MessagingException;

    public void appendMessages(Message[] messages) throws MessagingException;

    public boolean isOpen();

    public void open(int status) throws MessagingException;

    public void close(boolean b) throws MessagingException;

}
