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
package org.apache.james.util.io;

import java.io.File;
import java.io.FilenameFilter;

/**
 * This filter accepts <code>File</code>s that are directories.
 * <p>Eg., here is how to print out a list of the current directory's subdirectories:</p>
 *
 * <pre>
 * File dir = new File(".");
 * String[] files = dir.list( new DirectoryFileFilter() );
 * for ( int i=0; i&lt;files.length; i++ )
 * {
 *     System.out.println(files[i]);
 * }
 * </pre>
 *
 * @author <a href="mailto:dev@avalon.apache.org">Avalon Development Team</a>
 * @version $Revision$ $Date$
 * @since 4.0
 */
public class DirectoryFileFilter
    implements FilenameFilter
{
    public boolean accept( final File file, final String name )
    {
        return new File( file, name ).isDirectory();
    }
}


