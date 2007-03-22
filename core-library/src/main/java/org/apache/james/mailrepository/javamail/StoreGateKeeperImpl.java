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

import java.util.HashMap;
import java.util.Map;

import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.Store;

public class StoreGateKeeperImpl implements StoreGateKeeper {
    
    private Store store;
    
    Map folderGateKeeperMap = new HashMap();
    FolderGateKeeper defaultFolder = null;

    private FolderAdapterFactory folderFactory;
    
    public StoreGateKeeperImpl(Store store) {
        this.store=store;
    }

    /**
     * @see org.apache.james.mailrepository.javamail.StoreGateKeeper#getFolder(java.lang.String)
     */
    public synchronized FolderGateKeeper getFolder(String name) throws MessagingException {
        if (name.length()==0) {
            return null;
        }
        if (name.equalsIgnoreCase("INBOX")) {
            name="INBOX";
        }
        FolderGateKeeper fgk=(FolderGateKeeper) folderGateKeeperMap.get(name);
        if (fgk==null) {
            Folder f = store.getFolder(name);
            fgk=new FolderGateKeeperImpl(folderFactory.createAdapter(f),this);
            folderGateKeeperMap.put(name,fgk);
        }
        
        return fgk;
    }
    
    /**
     * @see org.apache.james.mailrepository.javamail.StoreGateKeeper#getDefaultFolder()
     */
    public synchronized FolderGateKeeper getDefaultFolder() throws MessagingException {
        if (defaultFolder==null) {
            Folder f = store.getDefaultFolder();
            defaultFolder=new FolderGateKeeperImpl(folderFactory.createAdapter(f),this);
        }
        return defaultFolder;
    }

    /**
     * @see org.apache.james.mailrepository.javamail.StoreGateKeeper#setFolderAdapterFactory(org.apache.james.mailrepository.javamail.FolderAdapterFactory)
     */
    public void setFolderAdapterFactory(FolderAdapterFactory folderFactory) {
        this.folderFactory=folderFactory;
        
    }

    /**
     * @see org.apache.james.mailrepository.javamail.StoreGateKeeper#list(java.lang.String)
     */
    public FolderGateKeeper[] list(String string) throws MessagingException {
        getDefaultFolder().use();
        FolderInterface[] folders = getDefaultFolder().getFolder().list(string);
        FolderGateKeeper[] keepers =new FolderGateKeeper[folders.length];
        for (int i = 0; i < keepers.length; i++) {
            keepers[i]=getFolder(folders[i].getFullName());
        }
        getDefaultFolder().free();
        
        return keepers;
    }
    
    /**
     * @see org.apache.james.mailrepository.javamail.StoreGateKeeper#renameTo(org.apache.james.mailrepository.javamail.FolderGateKeeper, java.lang.String)
     */
    public void renameTo(FolderGateKeeper from,String to) throws MessagingException {
        String fromName=from.getFullName();
        FolderGateKeeper[] subFolders = list(from.getFullName()+".*");
        for (int i = 0; i < subFolders.length; i++) {
            FolderGateKeeper subFolder=subFolders[i];
            String subFolderName=subFolder.getFullName();
            String newSubFolderName=to+subFolderName.substring(fromName.length());
            Folder subDestination=store.getFolder(newSubFolderName);
            subFolder.renameTo(subDestination);
        }
        Folder destination=store.getFolder(to);
        from.renameTo(destination);
    
    }

}
