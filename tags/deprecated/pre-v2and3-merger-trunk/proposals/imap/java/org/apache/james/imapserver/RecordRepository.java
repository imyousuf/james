/***********************************************************************
 * Copyright (c) 2000-2004 The Apache Software Foundation.             *
 * All rights reserved.                                                *
 * ------------------------------------------------------------------- *
 * Licensed under the Apache License, Version 2.0 (the "License"); you *
 * may not use this file except in compliance with the License. You    *
 * may obtain a copy of the License at:                                *
 *                                                                     *
 *     http://www.apache.org/licenses/LICENSE-2.0                      *
 *                                                                     *
 * Unless required by applicable law or agreed to in writing, software *
 * distributed under the License is distributed on an "AS IS" BASIS,   *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or     *
 * implied.  See the License for the specific language governing       *
 * permissions and limitations under the License.                      *
 ***********************************************************************/

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
     * @return Iterator over Strings of AbsoluteNames of Folders. Calling
     * objects cannot change contents of Iterator.
     */
    Iterator getAbsoluteNames();

    /**
     * Retrieves a folder record given the folder's full name. 
     *
     * @param folderAbsoluteName String name of a folder
     * @return FolderRecord for specified folder, null if no such FolderRecord
     */
    FolderRecord retrieve( String folderAbsoluteName );
    
    /**
     * Tests if there is a folder record for the given folder name.
     *
     * @param folderAbsoluteName String name of a folder
     * @return boolean True if there is a record for the specified folder.
     */
    boolean containsRecord( String folderAbsoluteName );

    /**
     * Returns the a unique UID validity value for this Host.
     * UID validity values are used to differentiate messages in 2 mailboxes with the same names
     * (when one is deleted).
     */
    int nextUIDValidity();

    /**
     * Deletes the FolderRecord from the repository.
     */
    void deleteRecord( FolderRecord record );
}

    
