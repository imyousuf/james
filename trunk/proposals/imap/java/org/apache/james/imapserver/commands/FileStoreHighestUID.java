/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
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
