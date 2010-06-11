/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.services;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import org.apache.avalon.Component;
import org.apache.avalon.ComponentNotAccessibleException;
import org.apache.avalon.ComponentNotFoundException;
import org.apache.avalon.ComponentSelector;
import org.apache.avalon.services.Service;

/**
 * @author Federico Barbieri <fede@apache.org>
 */
public interface Store
    extends Service, ComponentSelector
{
    /**
     * This method accept a Configuration object as hint and returns the 
     * corresponding Repository. 
     * The Configuration must be in the form of:
     * <repository destinationURL="[URL of this repository]"
     *             type="[repository type ex. OBJECT or STREAM or MAIL etc.]"
     *             model="[repository model ex. PERSISTENT or CACHE etc.]">
     *   [addition configuration]
     * </repository>
     */
    Component select(Object hint) throws ComponentNotFoundException,
        ComponentNotAccessibleException;

    /**
     * Indicate to Store that the given Repository is no longer needed by
     * caller. Objects calling select() must call release() when finished with
     * a Repository. Caller must assume that references to this object are
     * invalid after this method has been called.
     */
    void release(Repository repository);


    /**
     * Generic Repository interface
     */
    interface Repository extends Component
    {
        Repository getChildRepository( String childName );
    }
    
    /**
     * Repository for Serializable Objects.
     */

    interface ObjectRepository 
        extends Repository
    {
        Object get( String key );

        void put( String key, Object value );

        void remove( String key );

        boolean containsKey( String key );

        Iterator list();
    }

    /**
     * Repository for Streams 
     */
    interface StreamRepository 
        extends Repository
    {
        OutputStream put( String key );

        InputStream get( String key );

        void remove( String key );

        Iterator list();
    }
}
