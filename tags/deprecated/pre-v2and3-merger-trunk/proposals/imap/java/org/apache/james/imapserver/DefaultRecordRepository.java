/***********************************************************************
 * Copyright (c) 2000-2004 The Apache Software Foundation.             *
 * All rights reserved.                                                *
 * ------------------------------------------------------------------- *
 * Licensed under the Apache License, Version 2.0 (the "License"); you *
 * may not use this file except in compliance with the License. You    *
 * may obtain a copy of the License at:                                *
 *                                                                     *
 *     http://www.apache.org/licenses/LICENSE-2.0                      *
 *                                                                     *
 * Unless required by applicable law or agreed to in writing, software *
 * distributed under the License is distributed on an "AS IS" BASIS,   *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or     *
 * implied.  See the License for the specific language governing       *
 * permissions and limitations under the License.                      *
 ***********************************************************************/

package org.apache.james.imapserver;

import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.james.util.Assert;

import java.io.*;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Iterator;


/**
 * Implementation of a RecordRepository on a FileSystem.
 *
 * @author <a href="mailto:sascha@kulawik.de">Sascha Kulawik</a>
 * @author <a href="mailto:charles@benett1.demon.co.uk">Charles Benett</a>
 * @version 0.2 on 04 Aug 2002
 * @see RecordRepository
 */
public class DefaultRecordRepository
    extends AbstractLogEnabled
    implements RecordRepository   {
 
    private String path;
    private File repository;

    /**
     * Returns the a unique UID validity value for this Host.
     * UID validity values are used to differentiate messages in 2 mailboxes with the same names
     * (when one is deleted).
     */
    public int nextUIDValidity()
    {
        // TODO - make this a better unique value
        // ( although this will probably never break in practice,
        //  should be incrementing a persisted value.
        return Math.abs( Calendar.getInstance().hashCode() );
    }

    /**
     * Deletes the FolderRecord from the repository.
     */
    public synchronized void deleteRecord( FolderRecord fr )
    {
        try {
            String key = path + File.separator + fr.getAbsoluteName();
            File record = new File( key );
            Assert.isTrue( Assert.ON &&
                           record.exists() );
            record.delete();
            getLogger().info("Record deleted for: " + fr.getAbsoluteName());
            notifyAll();
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new
                RuntimeException("Exception caught while storing Folder Record: " + e);
        }
    }

    public void setPath(final String rootPath) {
        if (path != null) {
            throw new RuntimeException("Error: Attempt to reset AvalonRecordRepository");
        }
        path = rootPath;
        
        repository = new File(rootPath);

        if (!repository.isDirectory()) {
            if (! repository.mkdirs()){
                throw new RuntimeException("Error: Cannot create directory for AvalonRecordRepository at: " + rootPath);
            }
        } else if (!repository.canWrite()) {
            throw new RuntimeException("Error: Cannot write to directory for AvalonRecordRepository at: " + rootPath);
        }

                
    }

    public synchronized void store( final FolderRecord fr) {
        ObjectOutputStream out = null;
        try {
            String key = path + File.separator + fr.getAbsoluteName();
            out = new ObjectOutputStream( new FileOutputStream(key) );
            out.writeObject(fr);
            out.close();
            getLogger().info("Record stored for: " + fr.getAbsoluteName());
            notifyAll();
        } catch (Exception e) {
            if (out != null) {
                try {
                    out.close();
                } catch (Exception ignored) {
                }
            }
            e.printStackTrace();
            throw new
                RuntimeException("Exception caught while storing Folder Record: " + e);
        }
    }

    public synchronized Iterator getAbsoluteNames() {
        String[] names = repository.list();
        return Collections.unmodifiableList(Arrays.asList(names)).iterator();
    }

    public synchronized FolderRecord retrieve(final String folderAbsoluteName) {
        FolderRecord fr = null;
        ObjectInputStream in = null;
        try {
            String key = path + File.separator + folderAbsoluteName;
            in        = new ObjectInputStream( new FileInputStream(key) );
            fr = (FolderRecord) in.readObject();
            in.close();
  
        } catch (Exception e) {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception ignored) {
                }
            }
            e.printStackTrace();
            throw new
                RuntimeException("Exception caught while reading Folder Record: " + e);
        } finally {
            notifyAll();
        }
        return fr;
    }
       
    public boolean containsRecord(String folderAbsoluteName) {
        File testFile = new File(repository, folderAbsoluteName);
        return testFile.exists();
    }
}

    
