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

package org.apache.james.nntpserver;

import java.io.File;
import java.io.FilenameFilter;

/**
 * Filters files according to their last modified date
 *
 */
public class DateSinceFileFilter implements FilenameFilter {

    /**
     * The date that serves as the lower bound of the region of 
     * interest
     */
    private final long m_date;

    /**
     * Creates a new FileFilter that returns all Files that
     * have been modified since the date specified.
     *
     * @param date the date that serves as the lower bound of the region of 
     * interest
     */
    public DateSinceFileFilter( long date ) {
        m_date = date;
    }

    /**
     * Tests if a specified file has been modified since a
     * specified date.
     *
     * @param dir the directory in which the file was found
     * @param name the name of the file
     *
     * @return true if the file meets the criteria, false otherwise
     */
    public boolean accept( final File dir, final String name ) {
        return (new File(dir,name).lastModified() >= m_date);
    }
}
