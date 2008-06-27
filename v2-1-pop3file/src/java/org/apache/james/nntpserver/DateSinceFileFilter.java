/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.nntpserver;

import java.io.File;
import java.io.FilenameFilter;

/**
 * Filters files according to their last modified date
 *
 * @author  Harmeet Bedi <harmeet@kodemuse.com>
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
