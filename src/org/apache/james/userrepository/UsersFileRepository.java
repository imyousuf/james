/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.userrepository;

import java.io.File;
import java.util.Iterator;
import org.apache.avalon.AbstractLoggable;
import org.apache.avalon.Component;
import org.apache.avalon.ComponentManager;
import org.apache.avalon.ComponentManagerException;
import org.apache.avalon.Composer;
import org.apache.avalon.configuration.Configurable;
import org.apache.avalon.configuration.Configuration;
import org.apache.avalon.configuration.ConfigurationException;
import org.apache.avalon.configuration.DefaultConfiguration;
import org.apache.avalon.util.Lock;
import org.apache.cornerstone.services.Store;
import org.apache.james.services.UsersRepository;

/**
 * Implementation of a Repository to store users on the File System.
 *
 * Requires a configuration element in the .conf.xml file of the form:
 *  <repository destinationURL="file://path-to-root-dir-for-repository"
 *              type="USERS"
 *              model="SYNCHRONOUS"/>
 * Requires a logger called UsersRepository.
 * 
 * @version 1.0.0, 24/04/1999
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 * @author Charles Benett <charles@benett1.demon.co.uk>
 */
public class UsersFileRepository 
    extends AbstractLoggable
    implements UsersRepository, Component, Configurable, Composer {

    private static final String TYPE = "USERS";

    private Store store;
    private Store.ObjectRepository or;
    private String destination;
    private Lock lock;

    public UsersFileRepository() {
    }

    public void configure(Configuration conf) throws ConfigurationException {

        destination = conf.getChild("destination").getAttribute("URL");
        if (!destination.endsWith(File.separator)) {
            destination += File.separator;
        }
    }

    public void compose(ComponentManager compMgr) {
        try {
            store = (Store) compMgr.lookup("org.apache.cornerstone.services.Store");
            //prepare Configurations for object and stream repositories
            DefaultConfiguration objConf
                = new DefaultConfiguration("repository", "generated:UsersFileRepository.compose()");
            objConf.addAttribute("destinationURL", destination);
            objConf.addAttribute("type", "OBJECT");
            objConf.addAttribute("model", "SYNCHRONOUS");
        
            or = (Store.ObjectRepository) store.select(objConf);
            lock = new Lock();
        } catch (ComponentManagerException cme) {
            getLogger().error("Failed to retrieve Store component:" + cme.getMessage(), cme );
        } catch (Exception e) {
            getLogger().error("Failed to retrieve Store component:" + e.getMessage(), e );
        }
    }

    public Iterator list() {
        return or.list();
    }
 
    public synchronized void addUser(String name, Object attributes) {
        try {
            or.put(name, attributes);
        } catch (Exception e) {
            throw new RuntimeException("Exception caught while storing user: " + e );
        }
    }

    public synchronized Object getAttributes(String name) {
        try {
            return or.get(name);
        } catch (Exception e) {
            throw new RuntimeException("Exception while retrieving user: " + e.getMessage());
        }
    }

    public synchronized void removeUser(String name) {
        or.remove(name);
    }

    public boolean contains(String name) {
        return or.containsKey(name);
    }

    public boolean test(String name, Object attributes) {
        try {
            return attributes.equals(or.get(name));
        } catch (Exception e) {
            return false;
        }
    }

    public int countUsers() {
        int count = 0;
        for (Iterator it = list(); it.hasNext(); it.next()) {
            count++;
        }
        return count;
    }

}
