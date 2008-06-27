/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.imapserver;

import java.util.Iterator;

/**
 * Interface for objects representing a Repository of FolderRecords.
 * There should be a RecordRepository for every Host.
 * <p>Note that there is no method for removing Records: an IMAP host is
 * meant to retain information about deleted folders.
 *
 * @author <a href="mailto:charles@benett1.demon.co.uk">Charles Benett</a>
 * @version 0.1 on 14 Dec 2000
 * @see FolderRecord
 */
public interface RecordRepository {

    String RECORD = "RECORD";

    /**
     * Sets the location of this repository.
     *
     * @param rootPath String location of this repository
     */
    void setPath( String rootPath );

    /**
     * Stores a folder record in this repository.
     *
     * @param fr FolderRecord to be stored
     */
    void store( FolderRecord fr );
      
    /**
     * Returns Iterator over names of folders in repository
     *
     * @returns Iterator over Strings of AbsoluteNames of Folders. Calling
     * objects cannot change contents of Iterator.
     */
    Iterator getAbsoluteNames();

    /**
     * Retrieves a folder record given the folder's full name. 
     *
     * @param folderAbsoluteName String name of a folder
     * @returns FolderRecord for specified folder, null if no such FolderRecord
     */
    FolderRecord retrieve( String folderAbsoluteName );
    
    /**
     * Tests if there is a folder record for the given folder name.
     *
     * @param folderAbsoluteName String name of a folder
     * @returns boolean True if there is a record for the specified folder.
     */
    boolean containsRecord( String folderAbsoluteName );
}

    
