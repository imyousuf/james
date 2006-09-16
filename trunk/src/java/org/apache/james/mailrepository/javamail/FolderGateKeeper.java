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

import javax.mail.MessagingException;

/**
 * offers access to an underlaying Folder and manages open/close operations.<br>
 * The FolderGateKeeper can be handed over to different threads.
 * <br>
 * Clients have to call use() one time before and free() one time after they are
 * operating on the folder. When use() has been called free() has to be called
 * afterwards in any circumstance usally in a finally block.<br>
 * 
 * <pre>
 * try {
 *     use();
 *     getFolder().doSomething();
 *     getFolder().doSomething();
 * } finally {
 *     free();
 * }
 * </pre>
 * 
 * It is not allowed to open/close Folder from outside.
 */

public interface FolderGateKeeper {

    /**
     * increments count of users
     */
    public void use();

    /**
     * decrements count of users and closes folder if 0 and folder is open.
     * 
     * @throws MessagingException
     *             if something went wrong closing the Folder
     * @throws IllegalStateException
     *             if there are already 0 users
     * @throws IllegalStateException
     *             if the state of the folder differs from the last known
     */
    public void free() throws MessagingException;

    /**
     * Gets the Folder and opens it, if necessary
     * 
     * @return an open Folder
     * @throws MessagingException
     *             if something went wron opening the Folder
     * @throws IllegalStateException
     *             if the state of the folder differs from the last known
     * @throws IllegalStateException
     *             if there are only 0 users
     */
    public FolderInterface getOpenFolder() throws MessagingException;

    /**
     * used to test whether everyone has freed it
     * 
     * @return number of users
     */
    public int getUseCount();
    
    /**
     * Gets the Folder and don't care whether it is open or closed.
     * 
     * @return a open or closed Folder
     * @throws IllegalStateException
     *             if the state of the folder differs from the last known
     * @throws IllegalStateException
     *             if there are only 0 users
     */
    public FolderInterface getFolder();

}
