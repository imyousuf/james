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

import java.io.*;
import java.util.*;

/**
 * Reference: RFC 2060
 * @author <a href="mailto:s@rapi.com">Stephan Schiessling</a>
 * @version 0.1 on 15 Aug 2002
 */
public class FileStoreHighestUID implements HighestUID {

    private int highestUID;
    private int whenToWrite;
    private static final int WRITE_STEP = 3;
    private File file;
  
    public FileStoreHighestUID(File f) {
        file = f;
        highestUID = 0;
    
        if (file.exists()) {
            ObjectInputStream is = null;
            try {
                is = new ObjectInputStream(new FileInputStream(file));
                Integer i = (Integer) is.readObject();
                highestUID = i.intValue();
                is.close();
                is = null;
            } catch (Exception ex) {
                // log here
                ex.printStackTrace();
                if (is != null) {
                    try {
                        is.close();
                    } catch (Exception ignored) {}
                }
                throw new RuntimeException("Could not load highestUID!");
            }
            // maybe james was stopped without writing correct highestUID, therefore add
            // STEP_HIGHEST_UID, to ensure uniqeness of highestUID.
            highestUID += WRITE_STEP;
        }
        write();
        whenToWrite = highestUID+WRITE_STEP;
        System.out.println("Initialized highestUID="+highestUID);
    }
  
    public synchronized int get() {
        return highestUID;
    }
  
    public synchronized void increase() {
        highestUID++;
        if (highestUID >= whenToWrite) {
            // save this highestUID
            whenToWrite = highestUID+WRITE_STEP;
            // make it persistent
            write();
        }
    }
  
    private void write() {
        ObjectOutputStream os = null;
        try {
            os = new ObjectOutputStream( new FileOutputStream(file));
            os.writeObject(new Integer(highestUID));
            os.close();
            os = null;
        } catch (Exception ex) {
            // log here
            ex.printStackTrace();
            if (os != null) {
                try {
                    os.close();
                } catch (Exception ignored) {}
            }
            throw new RuntimeException("Failed to save highestUID!");
        }
    }
}
