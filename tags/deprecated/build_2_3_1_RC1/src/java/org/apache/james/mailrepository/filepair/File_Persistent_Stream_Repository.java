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

package org.apache.james.mailrepository.filepair;

import org.apache.avalon.cornerstone.services.store.StreamRepository;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Implementation of a StreamRepository to a File.
 * TODO: -retieve(String key) should return a FilterInputStream to allow
 * mark and reset methods. (working not like BufferedInputStream!!!)
 *
 */
public class File_Persistent_Stream_Repository
    extends AbstractFileRepository
    implements StreamRepository
{
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
            return getInputStream( key );
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
            return new BufferedOutputStream( outputStream );
        }
        catch( final IOException ioe )
        {
            final String message = "Exception caught while storing a stream ";
            getLogger().warn( message, ioe );
            throw new RuntimeException( message + ": " + ioe );
        }
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
