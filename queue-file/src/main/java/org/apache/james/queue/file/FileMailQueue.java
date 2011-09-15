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
package org.apache.james.queue.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.mail.MessagingException;
import javax.mail.util.SharedFileInputStream;

import org.apache.james.core.MimeMessageCopyOnWriteProxy;
import org.apache.james.core.MimeMessageSource;
import org.apache.james.lifecycle.api.Disposable;
import org.apache.james.lifecycle.api.LifecycleUtil;
import org.apache.james.queue.api.ManageableMailQueue;
import org.apache.mailet.Mail;
import org.slf4j.Logger;

/**
 * {@link ManageableMailQueue} implementation which use the fs to store {@link Mail}'s
 * 
 * On create of the {@link FileMailQueue} the {@link #init()} will get called. This takes care of load the needed meta-data into memory for fast access.
 * 
 * 
 * TODO: Split emails in sub-directories to make it more efficient with huge queues
 * 
 *
 */
public class FileMailQueue implements ManageableMailQueue {

    private ConcurrentHashMap<String, FileItem> keyMappings = new ConcurrentHashMap<String, FileMailQueue.FileItem>();
    private BlockingQueue<String> inmemoryQueue = new LinkedBlockingQueue<String>();
    private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    
    private final String queuename;
    private final File parentDir;
    private String queueDirName;
    private final Logger log;
    private boolean sync;
    private final static String MSG_EXTENSION = ".msg";
    private final static String OBJECT_EXTENSION = ".obj";
    
    public FileMailQueue(File parentDir, String queuename, boolean sync, Logger log) throws IOException {
        this.queuename = queuename;
        this.parentDir = parentDir;
        this.log = log;
        this.sync = sync;
        init();
    }
    
    private void init() throws IOException {
        File queueDir = new File(parentDir, queuename);
        queueDirName = queueDir.getAbsolutePath();
        
        if (!queueDir.exists()) {
            if (!queueDir.mkdirs()) {
                throw new IOException("Unable to create queue directory " + queueDir);
            }
        } else {
            String[] files = queueDir.list(new FilenameFilter() {
                
                @Override
                public boolean accept(File dir, String name) {
                    if (name.endsWith(MSG_EXTENSION)) {
                        if (new File(dir, name.substring(0, name.length() -MSG_EXTENSION.length()) + OBJECT_EXTENSION).exists()) {
                            return true;
                        }
                    }
                    return false;
                }
            });
            for (int a = 0; a < files.length; a++) {
                final String name = files[a];
                
                int i = name.indexOf("-");
                if ( i > -1) {
                    final String objectFilename = name.substring(0, name.length() - MSG_EXTENSION.length()) + OBJECT_EXTENSION;
                    long next = Long.parseLong(name.substring(0,i));
                    final String key = name.substring(i +1, name.length() - MSG_EXTENSION.length());
                    FileItem item = new FileItem(queueDirName + "/" + objectFilename, queueDirName + "/" + name);
                    keyMappings.put(key, item);
                    if (next <= System.currentTimeMillis()) {
                        
                        try {
                            inmemoryQueue.put(key);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException("Unable to init", e);
                        }
                    } else {
                        
                        // Schedule a task which will put the mail in the queue for processing after a given delay
                        scheduler.schedule(new Runnable() {
                            
                            @Override
                            public void run() {
                                try {
                                    inmemoryQueue.put(key);
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                    throw new RuntimeException("Unable to init", e);
                                }                                
                            }
                        }, next - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
                    }
                }
            }
        }
    }
    
    
    
    
    @Override
    public void enQueue(Mail mail, long delay, TimeUnit unit) throws MailQueueException {
        final String key = mail.getName();
        FileOutputStream out = null;
        FileOutputStream foout = null;
        ObjectOutputStream oout = null;
        try {
            String name = getFileNameWithoutExtension(queueDirName, key, unit.toMillis(delay));
            
            final FileItem item = new FileItem(name + OBJECT_EXTENSION, name + MSG_EXTENSION);

            foout = new FileOutputStream(item.getObjectFile());
            oout = new ObjectOutputStream(foout);
            oout.writeObject(mail);
            oout.flush();
            if (sync) foout.getFD().sync();
            out = new FileOutputStream(item.getMessageFile());
            mail.getMessage().writeTo(out);
            out.flush();
            if (sync) out.getFD().sync();
            
            keyMappings.put(key, item);
            
            if (delay > 0) {
                // The message should get delayed so schedule it for later 
                scheduler.schedule(new Runnable() {
                    
                    @Override
                    public void run() {
                        try {
                            inmemoryQueue.put(key);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException("Unable to init", e);
                        }                                
                    }
                }, delay, unit);
            
            } else {
                inmemoryQueue.put(key);
            }
            
            //TODO: Think about exception handling in detail
        } catch (FileNotFoundException e) {
            throw new MailQueueException("Unable to enqueue mail", e);
        } catch (IOException e) {
            throw new MailQueueException("Unable to enqueue mail", e);

        } catch (MessagingException e) {
            throw new MailQueueException("Unable to enqueue mail", e);
        } catch (InterruptedException e) {
            throw new MailQueueException("Unable to enqueue mail", e);

        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    // ignore on close
                }
            }
            if (oout != null) {
                try {
                    oout.close();
                } catch (IOException e) {
                    // ignore on close
                }
            }
            if (foout != null) {
                try {
                    foout.close();
                } catch (IOException e) {
                    // ignore on close
                }
            }
        }

    }

    private String getFileNameWithoutExtension(String parentdir, String name, long delay) {
        return parentdir + "/" + System.currentTimeMillis() + delay + "-" + name;
    }
    
    
    @Override
    public void enQueue(Mail mail) throws MailQueueException {
        enQueue(mail, 0, TimeUnit.MILLISECONDS);
    }

    @Override
    public MailQueueItem deQueue() throws MailQueueException {
        try {
            FileItem item = null;
            String k = null;
            while (item == null) {
                k = inmemoryQueue.take();
                item = keyMappings.get(k);
            }
            final String key = k;
            ObjectInputStream oin = null;
            try {
                final File objectFile = new File(item.getObjectFile());
                final File msgFile = new File(item.getMessageFile());
                oin = new ObjectInputStream(new FileInputStream(objectFile));
                final Mail mail = (Mail) oin.readObject();
                mail.setMessage(new MimeMessageCopyOnWriteProxy(new FileMimeMessageSource(msgFile)));
                return new MailQueueItem() {

                    @Override
                    public Mail getMail() {
                        return mail;
                    }

                    @Override
                    public void done(boolean success) throws MailQueueException {
                        if (!success) {
                            try {
                                inmemoryQueue.put(key);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                throw new MailQueueException("Unable to rollback", e);
                            }
                        } else {
                            keyMappings.remove(key);
                            if (!objectFile.delete()) {
                                if (log.isInfoEnabled()) {
                                    log.info("Unable to delete file " + objectFile);
                                }
                            }
                            if (!msgFile.delete()) {
                                if (log.isInfoEnabled()) {
                                    log.info("Unable to delete file " + msgFile);
                                }                            }
                        }

                        LifecycleUtil.dispose(mail);
                    }
                };
                
                // TODO: Think about exception handling in detail
            } catch (FileNotFoundException e) {
                throw new MailQueueException("Unable to dequeue", e);
            } catch (IOException e) {
                throw new MailQueueException("Unable to dequeue", e);
            } catch (ClassNotFoundException e) {
                throw new MailQueueException("Unable to dequeue", e);
            } catch (MessagingException e) {
                throw new MailQueueException("Unable to dequeue", e);
                
            } finally {
                if (oin != null) {
                    try {
                        oin.close();
                    } catch (IOException e) {
                        // ignore on close
                    }
                }
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MailQueueException("Unable to dequeue", e);
        }
    }

    private final class FileMimeMessageSource extends MimeMessageSource implements Disposable{

        private File file;
        private SharedFileInputStream in;

        public FileMimeMessageSource(File file) throws IOException {
            this.file = file;
            this.in = new SharedFileInputStream(file);
        }
        
        @Override
        public String getSourceId() {
            return file.getAbsolutePath();
        }

        /**
         * Get an input stream to retrieve the data stored in the temporary file
         * 
         * @return a <code>BufferedInputStream</code> containing the data
         */
        public InputStream getInputStream() throws IOException {
            return in.newStream(0, -1);
        }


        @Override
        public long getMessageSize() throws IOException {
            return file.length();
        }
        

        /*
         * (non-Javadoc)
         * 
         * @see org.apache.james.core.MimeMessageSource#disposeSource()
         */
        public void dispose() {
            try {
                in.close();
            } catch (IOException e) {
            }
            
            file = null;
        }
        
    }
    
    /**
     * Helper class which is used to reference the path to the object and msg file
     * 
     *
     */
    private final static class FileItem {
        private String objectfile;
        private String messagefile;

        public FileItem(String objectfile, String messagefile) {
            this.objectfile = objectfile;
            this.messagefile = messagefile;
        }
        
        
        public String getObjectFile() {
            return objectfile;
        }
        
        public String getMessageFile() {
            return messagefile;
        }
        
    }
    @Override
    public long getSize() throws MailQueueException {
        return keyMappings.size();
    }

    @Override
    public long flush() throws MailQueueException {
        Iterator<String> keys = keyMappings.keySet().iterator();
        long i = 0;
        while(keys.hasNext()) {
            String key = keys.next();
            if (inmemoryQueue.contains(key) == false) {
                inmemoryQueue.add(key);
                i++;
            }
        }
        return i;
    }

    /**
     * TODO: implement me
     * 
     * @return
     * @throws MailQueueException
     */
    @Override
    public long clear() throws MailQueueException {
        throw new MailQueueException("Not supported yet");
    }

    /**
     * TODO: implement me
     * 
     * @param type
     * @param value
     * @return
     * @throws MailQueueException
     */
    @Override
    public long remove(Type type, String value) throws MailQueueException {
        throw new MailQueueException("Not supported yet");

    }

    /**
     * TODO: implement me
     * 
     * @return
     * @throws MailQueueException
     */
    @Override
    public MailQueueIterator browse() throws MailQueueException {
        throw new MailQueueException("Not supported yet");
    }
    
}
