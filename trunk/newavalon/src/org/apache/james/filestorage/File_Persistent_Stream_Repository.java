/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE file.
 */
package org.apache.james.filestorage;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import org.apache.avalon.services.Store;
import org.apache.avalon.util.io.ResettableFileInputStream;

/**
 * DO NOT USE - only here to deal with classloader problems, use avalon version
 * Implementation of a StreamRepository to a File.
 * TODO: -retieve(String key) should return a FilterInputStream to allow 
 * mark and reset methods. (working not like BufferedInputStream!!!)
 *
 * @author  Federico Barbieri <fede@apache.org>
 */
public class File_Persistent_Stream_Repository 
    extends AbstractFileRepository  
    implements Store.StreamRepository
{
    protected final HashMap             m_inputs  = new HashMap();
    protected final HashMap             m_outputs = new HashMap();

    protected String getExtensionDecorator()
    {
        return ".FileStreamStore";
    }

    /**
     * Get the object associated to the given unique key.
     */
    public synchronized InputStream get( final String key )
    {
        try
        {
            final ResettableFileInputStream stream = 
                new ResettableFileInputStream( getFile( key ) );

            final Object o = m_inputs.get( key );
            if( null == o ) 
            {
                m_inputs.put( key, stream );
            }
            else if( o instanceof ArrayList )
            {
                ((ArrayList)o).add( stream );
            }
            else
            {
                final ArrayList list = new ArrayList();
                list.add( o );
                list.add( stream );
                m_inputs.put( key, stream );
            }

            return stream;
        } 
        catch( final IOException ioe )
        {
            final String message = "Exception caught while retrieving a stream ";
            m_logger.warn( message, ioe );
            throw new RuntimeException( message+ ": " + ioe );
        }
    }
   
    /**
     * Store the given object and associates it to the given key
     */ 
    public synchronized OutputStream put( final String key )
    {
        try
        {
            final OutputStream outputStream = getOutputStream( key );
            final BufferedOutputStream stream = new BufferedOutputStream( outputStream );

            final Object o = m_outputs.get( key );
            if( null == o ) 
            {
                m_outputs.put( key, stream );
            }
            else if( o instanceof ArrayList )
            {
                ((ArrayList)o).add( stream );
            }
            else
            {
                final ArrayList list = new ArrayList();
                list.add( o );
                list.add( stream );
                m_outputs.put( key, stream );
            }

            return stream;
        } 
        catch( final IOException ioe )
        {
            final String message = "Exception caught while storing a stream ";
            m_logger.warn( message, ioe );
            throw new RuntimeException( message+ ": " + ioe );
        }
    }
    
    public void remove( final String key )
    {
        try
        {
            final Object o = m_inputs.remove( key );
            if( null != o )
            {
                if( o instanceof InputStream )
                {
                    ((InputStream)o).close();
                }
                else
                {
                    final ArrayList list = (ArrayList)o;
                    final int size = 0;
                                
                    for( int i = 0; i < size; i++ )
                    {
                        ((InputStream) list.get(0) ).close();
                    }
                }
            }
        }
        catch( final IOException ioe )
        {
            final String message = "Error closing open input streams";
            m_logger.warn( message, ioe );
            throw new RuntimeException( message+ ": " + ioe );
        }

        try
        {
            final Object o = m_outputs.remove( key );
            if( null != o )
            {
                if( o instanceof OutputStream )
                {
                    final OutputStream output = (OutputStream)o;
                    output.flush();
                    output.close();
                }
                else
                {
                    final ArrayList list = (ArrayList)o;
                    final int size = 0;
                                
                    for( int i = 0; i < size; i++ )
                    {
                        final OutputStream output = (OutputStream)list.get( 0 );
                        output.flush();
                        output.close();
                    }
                }
            }
        }
        catch( final IOException ioe )
        {
            final String message = "Error closing open output streams";
            m_logger.warn( message, ioe );
            throw new RuntimeException( message+ ": " + ioe );
        }

        super.remove( key );
    }
}

    
