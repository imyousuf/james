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



package org.apache.james.mailrepository;

import org.apache.avalon.cornerstone.services.store.ObjectRepository;
import org.apache.avalon.cornerstone.services.store.Store;
import org.apache.avalon.cornerstone.services.store.StreamRepository;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.james.core.MimeMessageCopyOnWriteProxy;
import org.apache.james.core.MimeMessageWrapper;
import org.apache.mailet.Mail;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Implementation of a MailRepository on a FileSystem.
 *
 * Requires a configuration element in the .conf.xml file of the form:
 *  &lt;repository destinationURL="file://path-to-root-dir-for-repository"
 *              type="MAIL"
 *              model="SYNCHRONOUS"/&gt;
 * Requires a logger called MailRepository.
 *
 * @version 1.0.0, 24/04/1999
 */
public class AvalonMailRepository
    extends AbstractMailRepository {

    private StreamRepository streamRepository;
    private ObjectRepository objectRepository;
    private String destination;
    private Set keys;
    private boolean fifo;
    private boolean cacheKeys; // experimental: for use with write mostly repositories such as spam and error

    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(Configuration)
     */
    public void configure(Configuration conf) throws ConfigurationException {
        destination = conf.getAttribute("destinationURL");
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("AvalonMailRepository.destinationURL: " + destination);
        }
        String checkType = conf.getAttribute("type");
        if (! (checkType.equals("MAIL") || checkType.equals("SPOOL")) ) {
            String exceptionString = "Attempt to configure AvalonMailRepository as " +
                                     checkType;
            if (getLogger().isWarnEnabled()) {
                getLogger().warn(exceptionString);
            }
            throw new ConfigurationException(exceptionString);
        }
        fifo = conf.getAttributeAsBoolean("FIFO", false);
        cacheKeys = conf.getAttributeAsBoolean("CACHEKEYS", true);
        // ignore model
    }

    /**
     * @see org.apache.avalon.framework.activity.Initializable#initialize()
     */
    public void initialize()
            throws Exception {
        super.initialize();
        try {
            objectRepository = (ObjectRepository) selectRepository(store, "OBJECT");
            streamRepository = (StreamRepository) selectRepository(store, "STREAM");

            if (cacheKeys) keys = Collections.synchronizedSet(new HashSet());

            //Finds non-matching pairs and deletes the extra files
            HashSet streamKeys = new HashSet();
            for (Iterator i = streamRepository.list(); i.hasNext(); ) {
                streamKeys.add(i.next());
            }
            HashSet objectKeys = new HashSet();
            for (Iterator i = objectRepository.list(); i.hasNext(); ) {
                objectKeys.add(i.next());
            }

            Collection strandedStreams = (Collection)streamKeys.clone();
            strandedStreams.removeAll(objectKeys);
            for (Iterator i = strandedStreams.iterator(); i.hasNext(); ) {
                String key = (String)i.next();
                remove(key);
            }

            Collection strandedObjects = (Collection)objectKeys.clone();
            strandedObjects.removeAll(streamKeys);
            for (Iterator i = strandedObjects.iterator(); i.hasNext(); ) {
                String key = (String)i.next();
                remove(key);
            }

            if (keys != null) {
                // Next get a list from the object repository
                // and use that for the list of keys
                keys.clear();
                for (Iterator i = objectRepository.list(); i.hasNext(); ) {
                    keys.add(i.next());
                }
            }
            if (getLogger().isDebugEnabled()) {
                StringBuffer logBuffer =
                    new StringBuffer(128)
                            .append(getClass().getName())
                            .append(" created in ")
                            .append(destination);
                getLogger().debug(logBuffer.toString());
            }
        } catch (Exception e) {
            final String message = "Failed to retrieve Store component:" + e.getMessage();
            getLogger().error( message, e );
            throw e;
        }
    }

    private Object selectRepository(Store store, String type) throws ServiceException {
        DefaultConfiguration objectConfiguration
            = new DefaultConfiguration( "repository",
                                        "generated:AvalonFileRepository.compose()" );

        objectConfiguration.setAttribute("destinationURL", destination);
        objectConfiguration.setAttribute("type", type);
        objectConfiguration.setAttribute("model", "SYNCHRONOUS");
        return store.select(objectConfiguration);
    }

    /**
     * @see org.apache.james.mailrepository.AbstractMailRepository#internalStore(Mail)
     */
    protected void internalStore(Mail mc) throws MessagingException, IOException {
        String key = mc.getName();
        if (keys != null && !keys.contains(key)) {
            keys.add(key);
        }
        boolean saveStream = true;

        MimeMessage message = mc.getMessage();
        // if the message is a Copy on Write proxy we check the wrapped message
        // to optimize the behaviour in case of MimeMessageWrapper
        if (message instanceof MimeMessageCopyOnWriteProxy) {
            MimeMessageCopyOnWriteProxy messageCow = (MimeMessageCopyOnWriteProxy) message;
            message = messageCow.getWrappedMessage();
        }
        if (message instanceof MimeMessageWrapper) {
            MimeMessageWrapper wrapper = (MimeMessageWrapper) message;
            if (DEEP_DEBUG) {
                System.out.println("Retrieving from: " + wrapper.getSourceId());
                StringBuffer debugBuffer =
                    new StringBuffer(64)
                            .append("Saving to:       ")
                            .append(destination)
                            .append("/")
                            .append(mc.getName());
                System.out.println(debugBuffer.toString());
                System.out.println("Modified: " + wrapper.isModified());
            }
            StringBuffer destinationBuffer =
                new StringBuffer(128)
                    .append(destination)
                    .append("/")
                    .append(mc.getName());
            if (destinationBuffer.toString().equals(wrapper.getSourceId()) && !wrapper.isModified()) {
                //We're trying to save to the same place, and it's not modified... we shouldn't save.
                //More importantly, if we try to save, we will create a 0-byte file since we're
                //retrying to retrieve from a file we'll be overwriting.
                saveStream = false;
            }
        }
        if (saveStream) {
            OutputStream out = null;
            try {
                out = streamRepository.put(key);
                mc.getMessage().writeTo(out);
            } finally {
                if (out != null) out.close();
            }
        }
        //Always save the header information
        objectRepository.put(key, mc);
    }

    /**
     * @see org.apache.james.services.MailRepository#retrieve(String)
     */
    public Mail retrieve(String key) throws MessagingException {
        if ((DEEP_DEBUG) && (getLogger().isDebugEnabled())) {
            getLogger().debug("Retrieving mail: " + key);
        }
        try {
            Mail mc = null;
            try {
                mc = (Mail) objectRepository.get(key);
            } 
            catch (RuntimeException re){
                StringBuffer exceptionBuffer = new StringBuffer(128);
                if(re.getCause() instanceof Error){
                    exceptionBuffer.append("Error when retrieving mail, not deleting: ")
                            .append(re.toString());
                }else{
                    exceptionBuffer.append("Exception retrieving mail: ")
                            .append(re.toString())
                            .append(", so we're deleting it.");
                    remove(key);
                }
                getLogger().warn(exceptionBuffer.toString());
                return null;
            }
            MimeMessageAvalonSource source = new MimeMessageAvalonSource(streamRepository, destination, key);
            mc.setMessage(new MimeMessageCopyOnWriteProxy(source));

            return mc;
        } catch (Exception me) {
            getLogger().error("Exception retrieving mail: " + me);
            throw new MessagingException("Exception while retrieving mail: " + me.getMessage());
        }
    }


    /**
     * @see org.apache.james.mailrepository.AbstractMailRepository#internalRemove(String)
     */
    protected void internalRemove(String key) throws MessagingException {
        if (keys != null) keys.remove(key);
        streamRepository.remove(key);
        objectRepository.remove(key);
    }


    /**
     * @see org.apache.james.services.MailRepository#list()
     */
    public Iterator list() {
        // Fix ConcurrentModificationException by cloning 
        // the keyset before getting an iterator
        final ArrayList clone;
        if (keys != null) synchronized(keys) {
            clone = new ArrayList(keys);
        } else {
            clone = new ArrayList();
            for (Iterator i = objectRepository.list(); i.hasNext(); ) {
                clone.add(i.next());
            }
        }
        if (fifo) Collections.sort(clone); // Keys is a HashSet; impose FIFO for apps that need it
        return clone.iterator();
    }
}
