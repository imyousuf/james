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

package org.apache.james.context;

import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.ContextException;
import org.apache.avalon.framework.context.Contextualizable;
import org.apache.james.services.FileSystem;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.io.FileInputStream;

/**
 * Avalon implementation of the FileSystem service
 * 
 * @since 2.4
 */
public class AvalonFileSystem implements FileSystem, Contextualizable {

    /**
     * Avalon context used by this implementation
     */
    private Context context;

    /**
     * delegates to method getFile() and returns file as InputStream.
     * @see org.apache.james.context.AvalonFileSystem#getFile(java.lang.String) 
     */
    public InputStream getResource(String url) throws IOException {
        return new FileInputStream(getFile(url)); 
    }

    /**
     * @see org.apache.james.services.FileSystem#getFile(java.lang.String)
     */
    public File getFile(String fileURL) throws FileNotFoundException {
        try {
            return AvalonContextUtilities.getFile(context, fileURL);
        } catch (ContextException e) {
            throw new FileNotFoundException("Context exception: "+e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new FileNotFoundException("Context exception: "+e.getMessage());
        }
    }
    
    /**
     * @see org.apache.james.services.FileSystem#getBasedir()
     */
    public File getBasedir() throws FileNotFoundException {
        try {
            return (File) context.get( "urn:avalon:home" );
        } catch (ContextException e) {
            try {
                return ((File) context.get( AvalonContextConstants.APPLICATION_HOME) );
            } catch (ContextException e2) {
                throw new FileNotFoundException(e2.getMessage());
            }
        }
    }

    /**
     * @see org.apache.avalon.framework.context.Contextualizable#contextualize(org.apache.avalon.framework.context.Context)
     */
    public void contextualize(Context context) throws ContextException {
        this.context = context;
    }

}
