/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE file.
 */
package org.apache.james.filestorage;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.ArrayList;
import org.apache.avalon.ComponentManager;
import org.apache.avalon.ComponentManagerException;
import org.apache.avalon.Initializable;
import org.apache.avalon.blocks.AbstractBlock;
import org.apache.avalon.services.Store;
import org.apache.avalon.util.io.ResettableFileInputStream;
import org.apache.avalon.util.io.ExtensionFileFilter;
import org.apache.log.LogKit;
import org.apache.log.Logger;

/**
 * DO NOT USE - only here to deal with classloader problems, use avalon version
 * This an abstract class implementing functionality for creating a file-store.
 * @author <a href="mailto:stefano@apache.org">Stefano Mazzocchi</a>
 * @author <a href="mailto:fede@apache.org">Federico Barbieri</a>
 */
public abstract class AbstractFileRepository 
    extends AbstractBlock  
    implements Store.Repository, Initializable
{
    protected static final boolean      DEBUG          = true;

    protected static final String       HANDLED_URL    = "file://";
    protected static final char         HEX_DIGITS[]   = 
    {
        '0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'
    };
    
    protected String                    m_path;
    protected String                    m_extension;
    protected String                    m_name;
    protected FilenameFilter            m_filter;
    protected String                    m_destination;

    protected abstract String getExtensionDecorator();

    public void init() 
        throws Exception
    {
        m_logger.info( "Init " + getClass().getName() + " Store" );

        if( null == m_destination ) 
	{
	    m_destination = 
              m_configuration.getAttribute( "destinationURL" );
        }
        
        m_name = RepositoryManager.getName();
        m_extension = "." + m_name + getExtensionDecorator();
        m_filter = new ExtensionFileFilter( m_extension );
 
        if( !m_destination.startsWith( HANDLED_URL ) )
	{
	    throw new Exception( "cannot handle destination" );
	}

        m_path = m_destination.substring( HANDLED_URL.length() );
        new File( m_path ).mkdirs();

	m_logger.info( getClass().getName() + " opened in " + m_destination );
    }

    protected void setDestination( final String destination )
    {
        m_destination = destination;
    }

    protected AbstractFileRepository createChildRepository()
        throws Exception
    {
        return (AbstractFileRepository)getClass().newInstance();
    }

    public Store.Repository getChildRepository( final String childName )
    {
        AbstractFileRepository child = null; 
        
        try { child = createChildRepository(); }
        catch( final Exception e )
	{
	  throw new RuntimeException( "Cannot initialize child repository " + 
				      childName + " : " + e );
	}
	try { child.compose( m_componentManager ); }
	catch (final ComponentManagerException cme) {
	    m_logger.warn("Unable to compose child repository: " + cme);
	    return null;
	}
        child.setDestination( m_destination + File.pathSeparatorChar + 
			      childName + File.pathSeparator );
        
        try { child.init(); } 
        catch( final Exception e )
	{
	    throw new RuntimeException( "Cannot initialize child " +
					"repository " + childName + 
					" : " + e );
	}

        if( DEBUG ) 
	{
	    m_logger.debug( "Child repository of " + m_name + " created in " + 
			  m_destination + File.pathSeparatorChar + 
			  childName + File.pathSeparator );
	}

        return child;
    }

    protected File getFile( final String key )
        throws IOException
    {
        return new File( encode( key ) );
    }

    protected InputStream getInputStream( final String key )
        throws IOException
    {
        return new FileInputStream( getFile( key ) );
    }

    protected OutputStream getOutputStream( final String key )
        throws IOException
    {
        return new FileOutputStream( getFile( key ) );
    }

    /**
     * Remove the object associated to the given key.
     */
    public synchronized void remove( final String key )
    {
        try
	{
	    final File file = getFile( key );
	    file.delete();
	    if( DEBUG ) m_logger.debug( "removed key " + key );
	} 
        catch( final Exception e )
	{
	    throw new RuntimeException( "Exception caught while removing" +
					" an object: " + e );
	}
    }
    
    /**
     * Indicates if the given key is associated to a contained object.
     */
    public synchronized boolean containsKey( final String key )
    {
        try
	{
	    final File file = getFile( key );
	    if( DEBUG ) m_logger.debug( "checking key " + key );
	    return file.exists();
	} 
        catch( final Exception e )
	{
	    throw new RuntimeException( "Exception caught while searching " +
					"an object: " + e );
	}
    }

    /**
     * Returns the list of used keys.
     */
    public Iterator list() 
    {
        final File storeDir = new File( m_path );
        final String names[] = storeDir.list( m_filter );
        final ArrayList list = new ArrayList();

        for( int i = 0; i < names.length; i++ )
	{
	    list.add( decode( names[i] ) );
	}

        return list.iterator();
    }

    /**
     * Returns a String that uniquely identifies the object.
     * <b>Note:</b> since this method uses the Object.toString()
     * method, it's up to the caller to make sure that this method
     * doesn't change between different JVM executions (like
     * it may normally happen). For this reason, it's highly recommended
     * (even if not mandated) that Strings be used as keys.
     */
    protected String encode( final String key ) 
    {
        final byte[] bytes = key.getBytes();
        final char[] buffer = new char[ bytes.length * 2 ];

        for( int i = 0, j = 0; i < bytes.length; i++ )
	{
	    final int k = bytes[ i ];
	    buffer[ j++ ] = HEX_DIGITS[ ( k >>> 4 ) & 0x0F ];
	    buffer[ j++ ] = HEX_DIGITS[ k & 0x0F ];
        }

        StringBuffer result = new StringBuffer();
        result.append( m_path );
        result.append( buffer );
        result.append( m_extension );
        return result.toString();
    }
    
    /**
     * Inverse of encode exept it do not use path.
     * So decode(encode(s) - m_path) = s.
     * In other words it returns a String that can be used as key to retive 
     * the record contained in the 'filename' file.
     */
    protected String decode( String filename )
    {
        filename = 
	    filename.substring( 0, filename.length() - 
				m_extension.length() );
        final int size = filename.length();
        final byte[] bytes = new byte[ size >>> 1 ];

        for( int i = 0, j = 0; i < size; j++ )
	{
	    bytes[j] = Byte.parseByte( filename.substring(i, i + 2), 16 );
	    i +=2;
	}

        return new String( bytes );
    }
}
