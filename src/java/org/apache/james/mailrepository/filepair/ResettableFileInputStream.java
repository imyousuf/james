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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 */
public class ResettableFileInputStream
    extends InputStream
{
    protected static final int DEFAULT_BUFFER_SIZE = 1024;

    protected final String m_filename;
    protected int m_bufferSize;
    protected InputStream m_inputStream;
    protected long m_position;
    protected long m_mark;
    protected boolean m_isMarkSet;

    public ResettableFileInputStream( final File file )
        throws IOException
    {
        this( file.getCanonicalPath() );
    }

    public ResettableFileInputStream( final String filename )
        throws IOException
    {
        this( filename, DEFAULT_BUFFER_SIZE );
    }

    public ResettableFileInputStream( final String filename, final int bufferSize )
        throws IOException
    {
        m_bufferSize = bufferSize;
        m_filename = filename;
        m_position = 0;

        m_inputStream = newStream();
    }

    public void mark( final int readLimit )
    {
        m_isMarkSet = true;
        m_mark = m_position;
        m_inputStream.mark( readLimit );
    }

    public boolean markSupported()
    {
        return true;
    }

    public void reset()
        throws IOException
    {
        if( !m_isMarkSet )
        {
            throw new IOException( "Unmarked Stream" );
        }
        try
        {
            m_inputStream.reset();
        }
        catch( final IOException ioe )
        {
            try
            {
                m_inputStream.close();
                m_inputStream = newStream();
                m_inputStream.skip( m_mark );
                m_position = m_mark;
            }
            catch( final Exception e )
            {
                throw new IOException( "Cannot reset current Stream: " + e.getMessage() );
            }
        }
    }

    protected InputStream newStream()
        throws IOException
    {
        return new BufferedInputStream( new FileInputStream( m_filename ), m_bufferSize );
    }

    public int available()
        throws IOException
    {
        return m_inputStream.available();
    }

    public void close() throws IOException
    {
        m_inputStream.close();
    }

    public int read() throws IOException
    {
        m_position++;
        return m_inputStream.read();
    }

    public int read( final byte[] bytes, final int offset, final int length )
        throws IOException
    {
        final int count = m_inputStream.read( bytes, offset, length );
        m_position += count;
        return count;
    }

    public long skip( final long count )
        throws IOException
    {
        m_position += count;
        return m_inputStream.skip( count );
    }
}
