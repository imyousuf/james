/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE file.
 */
package org.apache.james.filestorage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Iterator;
import java.util.ArrayList;
import org.apache.avalon.services.Store;

/**
 * DO NOT USE - only here to deal with classloader problems, use avalon version
 * This is a simple implementation of persistent object store using
 * object serialization on the file system.
 *
 * @author <a href="mailto:stefano@apache.org">Stefano Mazzocchi</a>
 * @author <a href="mailto:fede@apache.org">Federico Barbieri</a>
 */
public class File_Persistent_Object_Repository 
    extends AbstractFileRepository  
    implements Store.ObjectRepository
{
    protected String getExtensionDecorator()
    {
        return ".FileObjectStore";
    }

    /**
     * Get the object associated to the given unique key.
     */
    public synchronized Object get( final String key )
    {
        try
        {
            final InputStream inputStream = getInputStream( key );

            try
            {
                final ObjectInputStream stream = new ObjectInputStream( inputStream );
                final Object object = stream.readObject();
                if( DEBUG ) m_logger.debug( "returning object " + 
                                          object + " for key " + key );
                return object;
            }
	    catch( final Exception e1 )
		{
		    m_logger.warn("Exception caught while retrieving an object - position 1: " + e1);
		    throw new RuntimeException( "Exception caught while retrieving an object pos1: " + e1 );
		}
            finally
            {
                inputStream.close();
            }
        } 
        catch( final Exception e )
        {
	    m_logger.warn("Exception caught while retrieving an object: " + e);
            throw new RuntimeException( "Exception caught while retrieving an object: " + e );
        }
    }
   
    /**
     * Store the given object and associates it to the given key
     */ 
    public synchronized void put( final String key, final Object value )
    {
        try
        {
            final OutputStream outputStream = getOutputStream( key );

            try
            {
                final ObjectOutputStream stream = new ObjectOutputStream( outputStream );
                stream.writeObject( value );
                if( DEBUG ) m_logger.debug( "storing object " + value + " for key " + key );
            }
	    catch( final Exception e1 )
		{
		    m_logger.warn("Exception caught while storing an object - position 1: " + e1);
		    throw new RuntimeException( "Exception caught while storing an object pos1: " + e1 );
		}
            finally
            {
                outputStream.close();
            }
        } 
        catch( final Exception e )
        {
	    m_logger.warn("Exception caught while storing an object: " + e);
            throw new RuntimeException( "Exception caught while storing an object: " + e );
        }
    }
}
