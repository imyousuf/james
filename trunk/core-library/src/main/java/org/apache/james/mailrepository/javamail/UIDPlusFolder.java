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
import javax.mail.UIDFolder;

/**
 * Interim interface to provide access to UID PLUS methods reflecting RFC 2359,
 * until official Javamail API offers this.
 */

public interface UIDPlusFolder extends UIDFolder, FolderInterface {
    /**
     * Appends the given messages to the folder and returns corresponding uids.<br>
     * Implementations may require the folder to be open.
     * 
     * @see javax.mail.Folder#appendMessages(javax.mail.Message[])
     * 
     * @param msgs
     *            messages to append
     * @return array of same size and sequenze of msgs containing corresponding
     *         uids or -1, if something went wrong
     * @throws MessagingException
     * @throws IllegalStateException when folder has to be open
     */
    public long[] addUIDMessages(Message[] msgs) throws MessagingException;

    /**
     * Appends the given messages to the folder and returns corresponding
     * instances of the appended messages.<br>
     * Implementations may require the folder to be open.
     * 
     * @see javax.mail.Folder#appendMessages(javax.mail.Message[])
     * 
     * @param msgs
     *            messages to append
     * @return array of same size and sequenze of msgs containing corresponding
     *         added messages or null, if something went wrong
     * @throws MessagingException
     * @throws IllegalStateException when folder has to be open
     */
    public Message[] addMessages(Message[] msgs) throws MessagingException;

}
