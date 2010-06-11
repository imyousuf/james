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

package org.apache.james.user.impl.file;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.james.api.user.UserMetaDataRespository;
import org.apache.james.api.user.UserRepositoryException;
import org.apache.james.services.FileSystem;

/**
 * Stores user meta-data in the file system.
 */
public class FileUserMetaDataRepository implements UserMetaDataRespository {
    
    private static final String SERIALIZED_FILE_TYPE_NAME = ".ser";

    /** Characters that may safely be used to create names */
    private final static char[] SAFE_CHARS = {
        'a','b','c','d','e','f','g','h','i','j',
        'k','l','m','n','o','p','q','r','s','t',
        'u','v','w','x','y','z','0','1','2','3',
        '4','5','6','7','8','9'
    };
    
    private File baseDirectory;

    private FileSystem fs;

    private String baseDirUrl;
    
    public FileUserMetaDataRepository() {
        super();
    }

    @Resource(name="filesystem")
    public void setFileSystem(FileSystem fs) {
        this.fs = fs;
    }
    
    public void setBaseDirectory(String baseDirUrl) {
        this.baseDirUrl = baseDirUrl;
    }
    
    
    
    @PostConstruct
    public void init() throws Exception{
        baseDirectory = fs.getFile(baseDirUrl);
        if (!baseDirectory.exists()) {
            if (!baseDirectory.mkdirs()) {
                throw new Exception("Cannot create directory: " + baseDirectory);
            }
        }
    }
    
    
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.api.user.UserMetaDataRespository#clear(java.lang.String)
     */
    public void clear(String username) throws UserRepositoryException {
        final File userDir = userDirectory(username);
        try {
            FileUtils.deleteDirectory(userDir);
        } catch (IOException e) {
            throw new UserRepositoryException("Cannot delete " + userDir.getAbsolutePath(), e);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.api.user.UserMetaDataRespository#getAttribute(java.lang.String, java.lang.String)
     */
    public Serializable getAttribute(String username, String key)
            throws UserRepositoryException {
        final File valueFile = valueFile(username, key);
        final Serializable result;
        if (valueFile.exists()) {
            ObjectInputStream in = null;
            try {
                in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(valueFile)));
                result = (Serializable) in.readObject();
            } catch (IOException e) {
                throw new UserRepositoryException(e);
            } catch (ClassNotFoundException e) {
                throw new UserRepositoryException(e);
            } finally {
                IOUtils.closeQuietly(in);
            }
            
        } else {
            result = null;
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.api.user.UserMetaDataRespository#setAttribute(java.lang.String, java.io.Serializable, java.lang.String)
     */
    public void setAttribute(String username, Serializable value, String key)
            throws UserRepositoryException {

        final File valueFile = valueFile(username, key);
        ObjectOutputStream out = null;
        try {
            
            out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(valueFile)));
            out.writeObject(value);
            
        } catch (IOException e) {
            throw new UserRepositoryException(e);
        } finally {
            IOUtils.closeQuietly(out);
        }
    }

    private File valueFile(String username, String key) throws UserRepositoryException {
        final File userDir = userDirectory(username);
        
        final String valueFileName = fileSystemSafeName(key, SERIALIZED_FILE_TYPE_NAME);
        final File valueFile = new File(userDir, valueFileName);
        return valueFile;
    }

    private File userDirectory(String username) throws UserRepositoryException {        
        final String userDirectoryName = fileSystemSafeName(username);
        final File userDir = new File(baseDirectory, userDirectoryName);
        if (!userDir.exists()) {
            if (!userDir.mkdir()) {
                throw new UserRepositoryException("Cannot create directory: " + userDir.getAbsolutePath());
            }
        }
        return userDir;
    }

   

    /**
     * Maps a value to a file-system safe name.
     * @param value name, not null
     * @param suffix optional suffix to be append, possibly null
     * @return file system safe mapping of the name
     */
    private String fileSystemSafeName(String value) {
        return fileSystemSafeName(value, null);
    }
    
    /**
     * Maps a value to a file-system safe name.
     * @param value name, not null
     * @param suffix optional suffix to be append, possibly null
     * @return file system safe mapping of the name
     */
    private String fileSystemSafeName(String value, String suffix) {
        final int length = value.length();
        final StringBuffer buffer = new StringBuffer(length * 10);
        for (int i=0;i<length;i++) {
            final int next = value.charAt(i);
            for (int j=0;j<4;j++) {
                buffer.append(SAFE_CHARS[(next >> j * 4) % 32]);
            }
        }
        if (suffix != null) {
            buffer.append(suffix);
        }
        final String result = buffer.toString();
        return result;
    }
}
