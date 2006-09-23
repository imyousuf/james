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

import javax.mail.Folder;
import javax.mail.MessagingException;


/**
 * offers access to an underlaying Folder and manages open/close operations.<br>
 * The FolderGateKeeper can be handed over to different threads.
 * 
 * @see org.apache.james.mailrepository.javamail.FolderGateKeeper
 *
 */
public class FolderGateKeeperImpl implements FolderGateKeeper {

    private FolderInterface folder;

    private int inUse = 0;

    boolean open = false;

    /**
     * Construct new FolderGateKeeperImpl
     * 
     * @param folder the FolderInterface
     */
    public FolderGateKeeperImpl(FolderInterface folder) {
        if (folder.isOpen()) {
            throw new IllegalStateException(
                    "FolderGateKeeper: initial state must be closed");
        }
        this.folder = folder;
    }

    /**
     * @see org.apache.james.mailrepository.javamail.FolderGateKeeper#use()
     */
    public synchronized void use() {
        inUse++;
    }

    /**
     * @see org.apache.james.mailrepository.javamail.FolderGateKeeper#free()
     */
    public synchronized void free() throws MessagingException {
        if (inUse < 1) {
            throw new IllegalStateException(
                    "called free() but FolderGateKeeper is not in use");
        } else {
            inUse--;
            if (inUse == 0) {
                if (open != folder.isOpen()) {
                    throw new IllegalStateException(
                            "free(): folder state not equals last known state: open="
                                    + open);
                }
                if (open) {
                    // TODO expunge should be configurable
                    folder.close(true);
                    open = false;
                }
            }
        }

    }

    /**
     * @see org.apache.james.mailrepository.javamail.FolderGateKeeper#getOpenFolder()
     */
    public synchronized FolderInterface getOpenFolder() throws MessagingException {
        if (inUse < 1) {
            throw new IllegalStateException(
                    "called getFolder() but folder is not in use");
        }
        if (open != folder.isOpen()) {
            throw new IllegalStateException(
                    "getFolder() folder state not equals last known state: open="
                            + open);
        }
        if (!open) {
            folder.open(Folder.READ_WRITE);
            open = true;
        }
        return folder;

    }

    /**
     * @see org.apache.james.mailrepository.javamail.FolderGateKeeper#getUseCount()
     */
    public synchronized int getUseCount() {
        return inUse;
    }

    /**
     * @see org.apache.james.mailrepository.javamail.FolderGateKeeper#getFolder()
     */
    public synchronized FolderInterface getFolder() {
        if (inUse < 1) {
            throw new IllegalStateException(
                    "called getFolder() but folder is not in use");
        }
        if (open != folder.isOpen()) {
            throw new IllegalStateException(
                    "getFolder() folder state not equals last known state: open="
                            + open);
        }
        return folder;
    }

}
