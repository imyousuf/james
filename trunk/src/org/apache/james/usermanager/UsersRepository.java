/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.usermanager;

import org.apache.avalon.interfaces.*;
import org.apache.java.lang.*;
import org.apache.java.util.*;
import java.util.*;
import java.io.*;

/**
 * Implementation of a Repository to store users.
 * @version 1.0.0, 24/04/1999
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 */
public class UsersRepository implements Store.Repository {

    public final static String USER = "USER";

    private Store.ObjectRepository or;
    private String path;
    private String name;
    private String destination;
    private String type;
    private String model;

    public void setAttributes(String name, String destination, String type, String model) {

        this.name = name;
        this.destination = destination;
        this.model = model;
        this.type = type;
    }
        
    public void setComponentManager(ComponentManager comp) {

        Store store = (Store) comp.getComponent(Interfaces.STORE);
        this.or = (Store.ObjectRepository) store.getPrivateRepository(destination, Store.OBJECT, model);
    }
    
    public String getName() {
        return name;
    }
    
    public String getType() {
        return type;
    }
    
    public String getModel() {
        return model;
    }
    
    public String getChildDestination(String childName) {
        return destination + childName.replace ('.', File.separatorChar) + File.separator;
    }

    public synchronized void addUser(String name, Object attributes) {
        try {
            or.store(name, attributes);
        } catch (Exception e) {
            throw new RuntimeException("Exception caught while storing user: " + e);
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

    public Enumeration list() {
        return or.list();
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
}

    
