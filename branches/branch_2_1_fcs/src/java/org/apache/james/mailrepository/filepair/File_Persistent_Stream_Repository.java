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

/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.james.mailrepository.filepair;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import org.apache.avalon.cornerstone.services.store.StreamRepository;
import org.apache.avalon.excalibur.io.IOUtil;

/**
 * Implementation of a StreamRepository to a File.
 * TODO: -retieve(String key) should return a FilterInputStream to allow
 * mark and reset methods. (working not like BufferedInputStream!!!)
 *
 * @author  Federico Barbieri <fede@apache.org>
 */
public class File_Persistent_Stream_Repository
    extends AbstractFileRepository
    implements StreamRepository
{
    protected final HashMap m_inputs = new HashMap();
    protected final HashMap m_outputs = new HashMap();

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
                ( (ArrayList)o ).add( stream );
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
            getLogger().warn( message, ioe );
            throw new RuntimeException( message + ": " + ioe );
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
                ( (ArrayList)o ).add( stream );
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
            getLogger().warn( message, ioe );
            throw new RuntimeException( message + ": " + ioe );
        }
    }

    public synchronized void remove( final String key )
    {
        Object o = m_inputs.remove( key );
        if( null != o )
        {
            if( o instanceof InputStream )
            {
                IOUtil.shutdownStream( (InputStream)o );
            }
            else
            {
                final ArrayList list = (ArrayList)o;
                final int size = list.size();

                for( int i = 0; i < size; i++ )
                {
                    IOUtil.shutdownStream( (InputStream)list.get( i ) );
                }
            }
        }

        o = m_outputs.remove( key );
        if( null != o )
        {
            if( o instanceof OutputStream )
            {
                IOUtil.shutdownStream( (OutputStream)o );
            }
            else
            {
                final ArrayList list = (ArrayList)o;
                final int size = list.size();

                for( int i = 0; i < size; i++ )
                {
                    IOUtil.shutdownStream( (OutputStream)list.get( 0 ) );
                }
            }
        }

        super.remove( key );
    }

    public long getSize(final String key) {
        try {
            return getFile(key).length();
        }
        catch(IOException e) {
            return 0;
        }
    }
}
