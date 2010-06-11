/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2003 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache", "Jakarta", "JAMES" and "Apache Software Foundation"
 *    must not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 * Portions of this software are based upon public domain software
 * originally written at the National Center for Supercomputing Applications,
 * University of Illinois, Urbana-Champaign.
 */

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

    
