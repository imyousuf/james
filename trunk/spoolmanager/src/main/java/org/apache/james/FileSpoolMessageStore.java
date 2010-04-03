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
package org.apache.james;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.james.core.MimeMessageSource;
import org.apache.james.lifecycle.Disposable;

/**
 * Use the filesystem as storage for the Email message while spooling
 * 
 *
 */
public class FileSpoolMessageStore implements SpoolMessageStore{

    private final String PROCESSING_SUFFIX =".processing";

    private File spooldir;
    
    public FileSpoolMessageStore(String storageDir) {
        spooldir = new File(storageDir);
        if (spooldir.exists()) {
            if (spooldir.isFile()) {
                throw new RuntimeException("Spooldirectory " + storageDir + " already exists and is a file!");
            }
        } else {
            if (spooldir.mkdirs() == false) {
                throw new RuntimeException("Unable to create Spooldirectory " + spooldir);
            }
        }
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.SpoolMessageStore#getMessage(java.lang.String)
     */
    public synchronized MimeMessageSource getMessage(String key) throws IOException{
        File f = new File(spooldir, key);
        File processingF = new File(f.getCanonicalPath() + PROCESSING_SUFFIX);

        if (f.exists()) {
            
            // delete processing file
            processingF.delete();
            
            // rename the old file .processing, so we don't get trouble when saving it back the the fs
            if (f.renameTo(processingF) == false) {
                throw new IOException("Unable to rename file " + f + " to " + processingF);
            }
            return new FileMimeMessageSource(processingF);
        } else {
            // delete processing file
            processingF.delete();

            if (processingF.createNewFile()) {
                return new FileMimeMessageSource(processingF);
            }
            throw new IOException("Unable to create file " + processingF);

        }
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.SpoolMessageStore#saveMessage(java.lang.String)
     */
    public synchronized OutputStream saveMessage(String key) throws IOException{
        File f = new File(spooldir,key);
        if (f.exists()) {
            throw new IOException("Unable to create file because a file with the name " + f + " already exists");
        } else {
            // just create the file
            f.createNewFile();
            return new FileOutputStream(f);   
        }

        
    }

    /**
     * {@link MimeMessageSource} which use the filesystem 
     *
     */
    private final class FileMimeMessageSource extends MimeMessageSource implements Disposable {

        private File file;
        private String id;
        
        private FileMimeMessageSource(File file) throws IOException {
            this.file = file;
            this.id = file.getCanonicalPath();
        }
        
        @Override
        public InputStream getInputStream() throws IOException {
            return new FileInputStream(file);
        }

        @Override
        public String getSourceId() {
            return id;
        }

        public void dispose() {
            file.delete();
        }
        
    }
}
