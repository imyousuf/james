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
 * 4. Redistributions of any form whatsoever must retain the following
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

/**
 * @version 1.0.0, 24/04/1999
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 */

package org.apache.james;

import org.apache.java.util.*;
import java.util.*;
import java.io.*;

/**
 * This is a simple implementation of the org.apache.avalon.blocks.Store interface. 
 */
public class StreamStore {

    private Hashtable store;
    private String path;
    private static final char[] hexDigits = {
        '0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'
    };
    private String ext = ".ml";
    private Vector Outputs, Inputs;    

    public StreamStore(String path) {
        this.path = path;
        new File(path + "pathtry").delete();
        Outputs = new Vector();
        Inputs = new Vector();
    }
    
    public OutputStream store(String key) {
        try {
            BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(path + encode(key)));
            Outputs.addElement(stream);
            return stream;
        } catch (Exception e) {
            System.out.println("Exception caught while storing an object: " + e.getMessage());
            throw new RuntimeException("Exception caught while storing an object: " + e);
        }
    }
    
    public InputStream retrive(String key) {
        try {
            BufferedInputStream stream = new BufferedInputStream(new FileInputStream(path + encode(key)));
            Inputs.addElement(stream);
            return stream ;
        } catch (Exception e) {
            System.out.println("Exception caught while retrieving an object: " + e.getMessage());
            throw new RuntimeException("Exception caught while retrieving an object: " + e);
        }
    }
    
    public void remove(String key) {

        for (Enumeration e = Inputs.elements() ; e.hasMoreElements() ;) {
            try {
                ((InputStream) e.nextElement()).close();
            } catch (IOException ioe) {}
        }

        for (Enumeration e = Outputs.elements() ; e.hasMoreElements() ;) {
            try {
                OutputStream os = (OutputStream) e.nextElement();
                os.flush();
                os.close();
            } catch (IOException ioe) {}
        }

        try {
            String fileName = path + encode(key);
            System.out.println("deleing:" + fileName);
            File file = new File(fileName);
            file.delete();
        } catch (Exception e) {
            System.out.println("Exception caught while removing an object: " + e.getMessage());
            throw new RuntimeException("Exception caught while removing an object: " + e);
        }
    }

    private String encode(String key) {
        byte[] b = key.getBytes();
        char[] buf = new char[b.length * 2];

        for (int i = 0, j = 0, k; i < b.length; i++) {
            k = b[i];
            buf[j++] = hexDigits[(k >>> 4) & 0x0F];
            buf[j++] = hexDigits[k & 0x0F];
        }

        return new String(new String(buf) + ext);
    }
    
    /**
     * Inverse of encode exept it do not use path.
     * So decode(encode(s) - path) = s.
     * In other words it returns a String that can be used as key to retive 
     * the record contained in the 'filename' file.
     */
    private String decode(String filename) {
// fix it with prefix and ext...
        filename = filename.substring(0, filename.length());        
        byte[] b = new byte[filename.length() / 2];
        for (int i = 0, j = 0; i < filename.length(); j++) {
            b[j] = Byte.parseByte(filename.substring(i, i + 2), 16);
            i +=2;
        }
        return new String(b);
    }
}

    
