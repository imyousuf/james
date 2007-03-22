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

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;

/**
 * Interface to javax.mail.Folder functionality to be able to replace 
 * implementation or using Mocks when testing 
 */

public interface FolderInterface {

    /**
     * @see javax.mail.Folder#getMessages()
     */
    public Message[] getMessages() throws MessagingException;

    /**
     * @see javax.mail.Folder#getMessage(int)
     */
    public Message getMessage(int no) throws MessagingException;

    /**
     * @see javax.mail.Folder#getMessageCount()
     */
    public int getMessageCount() throws MessagingException;

    /**
     * @see javax.mail.Folder#appendMessages(Message[])
     */
    public void appendMessages(Message[] messages) throws MessagingException;

    /**
     * @see javax.mail.Folder#isOpen()
     */
    public boolean isOpen();

    /**
     * @see javax.mail.Folder#open(int)
     */
    public void open(int status) throws MessagingException;

    /**
     * @see javax.mail.Folder#close(boolean)
     */
    public void close(boolean b) throws MessagingException;

    /**
     * @see javax.mail.Folder#exists()
     */
    public boolean exists() throws MessagingException;

    /**
     * @see javax.mail.Folder#create(int)
     */
    public boolean create(int holds_messages) throws MessagingException;

    /**
     * @see javax.mail.Folder#getType()
     */
    public int getType() throws MessagingException;

    /**
     * @see javax.mail.Folder#getFullName()
     */
    public String getFullName();

    /**
     * @see javax.mail.Folder#expunge()
     */
    public Message[] expunge() throws MessagingException;

    /**
     * @see javax.mail.Folder#getUnreadMessageCount()
     */
    public int getUnreadMessageCount() throws MessagingException;

    /**
     * @see javax.mail.Folder#getNewMessageCount()
     */
    public int getNewMessageCount() throws MessagingException;

    /**
     * @see javax.mail.Folder#getPermanentFlags()
     */
    public Flags getPermanentFlags();

    /**
     * @see javax.mail.Folder#getName()
     */
    public String getName();

    /**
     * @see javax.mail.Folder#list(String)
     */
    public FolderInterface[] list(String string) throws MessagingException;
    
    /**
     * @see javax.mail.Folder#delete(boolean)
     */
    public boolean delete(boolean recurse) throws MessagingException;

    /**
     * @see javax.mail.Folder#renameTo(Folder)
     */
    public boolean renameTo(Folder destination) throws MessagingException;;

}
