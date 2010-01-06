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
 * This takes a <code>FilenameFilter<code> as input and inverts the selection.
 * This is used in retrieving files that are not accepted by a filter.
 *
 * @version CVS $Revision$ $Date$
 */
public class InvertedFileFilter
    implements FilenameFilter
{
    private final FilenameFilter m_originalFilter;

    public InvertedFileFilter( final FilenameFilter originalFilter )
    {
        m_originalFilter = originalFilter;
    }

    public boolean accept( final File file, final String name )
    {
        return !m_originalFilter.accept( file, name );
    }
}


