/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.imap41server;

import org.apache.avalon.blocks.*;
import org.apache.avalon.*;
import org.apache.avalon.utils.*;
import java.util.*;
import java.io.*;
import org.apache.mailet.Mail;
import javax.mail.internet.*;
import javax.mail.MessagingException;

/**
 * Interface for objects representing a Repository of FolderRecords.
 * There should a RecordRepository for every Host.
 *
 * @version 1.0.0, 24/04/1999
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 */
public interface RecordRepository extends Store.Repository {

  
    public final static String RECORD = "RECORD";


    /**
     * Stores a folder record in this repository.
     *
     * @param fr FolderRecord to be stored
     */
    public void store(FolderRecord fr) ;
      
    /**
     * Retrieves a folder record given the folder's full name. At the moment, names can be obtained  from list() in superinterface Store.Repository
     *
     * @param folderName String name of a folder
     * @returns FolderRecord for specified folder, null if no such FolderRecord
     */
    public FolderRecord retrieve(String folderName);
    
    /**
     * Tests if there is a folder record for the given folder full name.
     *
     * @param folderName String name of a folder
     * @returns boolean True if there is a record for the specified folder.
     */
    public boolean containsRecord(String folderName);

}

    
