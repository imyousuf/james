/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james;

import org.apache.avalon.blocks.*;
import org.apache.arch.*;
import org.apache.java.util.*;
import java.util.*;
import java.io.*;
import org.apache.james.*;

/**
 * Implementation of a Repository to store MessageContainer.
 * @version 1.0.0, 24/04/1999
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 */
public class MessageContainerRepository implements Store.Repository {

    /**
     * Define a STREAM repository. Streams are stored in the specified
     * destination.
     */
    public final static String MESSAGE_CONTAINER = "MESSAGE_CONTAINER";

    private Store.StreamRepository sr;
    private String path;
    private String name;
    private String destination;
    private String type;
    private String model;

    public MessageContainerRepository() {
    }

    public void setAttributes(String name, String destination, String type, String model) {

        this.name = name;
        this.destination = destination;
        this.model = model;
        this.type = type;
    }
        
    public void setComponentManager(ComponentManager comp) {

        Store store = (Store) comp.getComponent(Interfaces.STORE);
        this.sr = (Store.StreamRepository) store.getPrivateRepository(destination, Store.STREAM, model);
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
        return destination + childName.replace('.', '\\') + "\\";
    }
    
    public void store(String key, MessageContainer mc) {
        PrintWriter out = new PrintWriter(sr.store(key));
        try {
            InputStream is = mc.getBodyInputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(is));
            is.mark(Integer.MAX_VALUE);
            for (String nextLine = in.readLine(); nextLine != null; nextLine = in.readLine()) {
                out.println(nextLine);
            }
            out.flush();
            out.close();
            is.reset();
        } catch (Exception e) {
            throw new RuntimeException("Exception caught while storing Message Container: " + e);
        }
    }

    public MessageContainer retrieve(String key) {
        MessageContainer mc = new MessageContainer();
        mc.setBodyInputStream(sr.retrieve(key));
        mc.setMessageId(key);
        return mc;
    }
    
    public void remove(String key) {
        sr.remove(key);
    }

    public Enumeration list() {
        return sr.list();
    }
}

    
