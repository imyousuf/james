/*
 * Copyright (c) 1999 The Java Apache Project.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. All advertising materials mentioning features or use of this
 *    software must display the following acknowledgment:
 *    "This product includes software and design ideas developed by the Java
 *    Apache Project (http://java.apache.org/)."
 *
 * 4. The names "Cocoon", "Cocoon Servlet" and "Java Apache Project" must
 *    not be used to endorse or promote products derived from this software
 *    without prior written permission.
 *
 * 5. Products derived from this software may not be called "Cocoon"
 *    nor may "Cocoon" and "Java Apache Project" appear in their names without
 *    prior written permission of the Java Apache Project.
 *
 * 6. Redistributions of any form whatsoever must retain the following
 *    acknowledgment:
 *    "This product includes software and design ideas developed by the Java
 *    Apache Project (http://java.apache.org/)."
 *
 * THIS SOFTWARE IS PROVIDED BY THE JAVA APACHE PROJECT "AS IS" AND ANY
 * EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE JAVA APACHE PROJECT OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Java Apache Project. For more information
 * on the Java Apache Project please see <http://java.apache.org/>.
 */

package org.apache.james;

import org.apache.java.util.*;
import java.util.*;
import java.io.*;


/**
 * This is a simple implementation of persistent object store using
 * object serialization on the file system.
 *
 * @author <a href="mailto:stefano@apache.org">Stefano Mazzocchi</a>
 * @author <a href="mailto:scoobie@systemy.it">Federico Barbieri</a>
 * @version $Revision: 1.2 $ $Date: 1999/11/12 20:38:34 $
 */

public class PersistentStore {
    
    private String path;
    private final static String EXTENTION = ".store";
    private FilenameFilter filter = new ExtensionFileFilter(EXTENTION);
    private static final char[] hexDigits = {
        '0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'
    };

    public PersistentStore(String path) {
        
        this.path = path;
        new File(path + "pathtry").delete();
    }
    
    /**
     * Get the object associated to the given unique key.
     */
    public synchronized Object get(Object key) {
        try {
            String filename = this.encode(key);
            ObjectInputStream stream = new ObjectInputStream(new FileInputStream(filename));
            Object object = stream.readObject();
            stream.close();
            return object;
        } catch (Exception e) {
            throw new RuntimeException("Exception caught while retrieving an object: " + e);
        }
    }
   
    /**
     * Store the given object and associates it to the given key
     */ 
    public synchronized void store(Object key, Object value) {
        try {
            String filename = this.encode(key);
            ObjectOutputStream stream = new ObjectOutputStream(new FileOutputStream(filename));
            stream.writeObject(value);
            stream.flush();
            stream.close();
        } catch (Exception e) {
            throw new RuntimeException("Exception caught while storing an object: " + e);
        }
    }
    
    /**
     * Remove the object associated to the given key.
     */
    public synchronized void remove(Object key) {
        try {
            new File(this.encode(key)).delete();
        } catch (Exception e) {
            throw new RuntimeException("Exception caught while removing an object: " + e);
        }
    }
    
    /**
     * Indicates if the given object is contained in the store.
     */
    public boolean contains(Object key) {
        throw new RuntimeException("Method PersistentStore.contains(Object) is not implemented.");
    }

    /**
     * Indicates if the given key is associated to a contained object.
     */
    public synchronized boolean containsKey(Object key) {
        try {
            File file = new File(this.encode(key));
            return file.exists();
        } catch (Exception e) {
            throw new RuntimeException("Exception caught while searching an object: " + e);
        }
    }
    
    /**
     * Returns the list of used keys.
     */
    public Enumeration list() {
        
        File storeDir = new File(path);
        String[] names = storeDir.list(filter);
        Vector v = new Vector();
        for (int i = 0; i < names.length; i++) {
            v.addElement(decode(names[i]));
        }
        return v.elements();
    }

    /**
     * Returns a String that uniquely identifies the object.
     * <b>Note:</b> since this method uses the Object.toString()
     * method, it's up to the caller to make sure that this method
     * doesn't change between different JVM executions (like
     * it may normally happen). For this reason, it's highly recommended
     * (even if not mandated) that Strings be used as keys.
     */
    private String encode(Object key) {
        byte[] b = key.toString().getBytes();
        char[] buf = new char[b.length * 2];

        for (int i = 0, j = 0, k; i < b.length; i++) {
            k = b[i];
            buf[j++] = hexDigits[(k >>> 4) & 0x0F];
            buf[j++] = hexDigits[k & 0x0F];
        }

        StringBuffer buffer = new StringBuffer();
        buffer.append(path);
        buffer.append(buf);
        buffer.append(EXTENTION);
        return buffer.toString();
    }
    
    /**
     * Inverse of encode exept it do not use path.
     * So decode(encode(s) - path) = s.
     * In other words it returns a String that can be used as key to retive 
     * the record contained in the 'filename' file.
     */
    private String decode(String filename) {

        filename = filename.substring(0, filename.length() - EXTENTION.length());        
        byte[] b = new byte[filename.length() >>> 1];
        for (int i = 0, j = 0; i < filename.length(); j++) {
            b[j] = Byte.parseByte(filename.substring(i, i + 2), 16);
            i +=2;
        }
        return new String(b);
    }

    /**
     * Returns the signature of this store implementation
     */
    public String getStatus() {
        return "Persistent Object Storage";
    }
    
    /**
     * This filters files based on the extension (what the filename 
     * ends with).  This is used in retrieving all the files of a 
     * particular type (such as .state, .message, or .checked).
     * @author Serge Knystautas <sergek@lokitech.com>
     */
    public class ExtensionFileFilter implements FilenameFilter {
    
        private String ext;
    
        public ExtensionFileFilter(String ext) {
            this.ext = ext;
        }
    
        public boolean accept(File file, String name) {
            return name.endsWith(ext);
        }
    }
}
