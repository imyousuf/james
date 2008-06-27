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
 * filters files according to their date of last modification
 *
 * @author  Harmeet Bedi <harmeet@kodemuse.com>
 */
public class DateSinceFileFilter implements FilenameFilter
{
    private final long m_date;

    public DateSinceFileFilter( long date ) 
    {
        m_date = date;
    }

    public boolean accept( final File file, final String name ) 
    {
        return (new File(file,name).lastModified() >= m_date);
    }
}
