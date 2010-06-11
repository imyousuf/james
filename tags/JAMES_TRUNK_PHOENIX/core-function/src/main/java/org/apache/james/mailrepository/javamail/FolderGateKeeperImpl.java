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
 * @see FolderGateKeeper
 *
 */
public class FolderGateKeeperImpl implements FolderGateKeeper {

    private final FolderInterface folder;

    private int inUse = 0;

    boolean open = false;

    private final StoreGateKeeper storeGateKeeper;

    public FolderGateKeeperImpl(FolderInterface folder,StoreGateKeeper storeGateKeeper) {
        if (folder.isOpen()) {
            throw new IllegalStateException(
                    "FolderGateKeeper: initial state must be closed");
        }
        this.folder = folder;
        this.storeGateKeeper=storeGateKeeper;
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
        if ((folder.getType() & Folder.HOLDS_MESSAGES)==0) {
            throw new RuntimeException("cannot open a Folder that does not hold messages");
        }
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

    /**
     * @see org.apache.james.mailrepository.javamail.FolderGateKeeper#getFullName()
     */
    public String getFullName() throws MessagingException {
        use();
        String fn=getFolder().getFullName();
        free();
        return fn;
    }

    /**
     * @see org.apache.james.mailrepository.javamail.FolderGateKeeper#create(int)
     */
    public void create(int holds_folders) throws MessagingException {
        use();
        getFolder().create(holds_folders);
        free();
    }

    /**
     * @see org.apache.james.mailrepository.javamail.FolderGateKeeper#exists()
     */
    public boolean exists() throws MessagingException {
        use();
        boolean e=getFolder().exists();
        free();
        return e;
    }


    /**
     * @see org.apache.james.mailrepository.javamail.FolderGateKeeper#renameTo(javax.mail.Folder)
     */
    public synchronized void renameTo(Folder destination) throws MessagingException {
        if (inUse!=0) {
            throw new IllegalStateException("cannot operate of folder that is in use");
        } else {
            folder.renameTo(destination);
        }
        
    }

    /**
     * @see org.apache.james.mailrepository.javamail.FolderGateKeeper#renameTo(java.lang.String)
     */
    public synchronized void renameTo(String newName) throws MessagingException {
        storeGateKeeper.renameTo(this,newName);
    }

}
