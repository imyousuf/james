/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.util;

import java.io.*;

public class InternetPrintWriter extends PrintWriter {
      private static String lineSeparator = "\r\n";

    public InternetPrintWriter (Writer out) {
        super (out);
    }
    public InternetPrintWriter (Writer out, boolean autoFlush) {
        super (out, autoFlush);
    }
    public InternetPrintWriter (OutputStream out) {
        super (out);
    }
    public InternetPrintWriter (OutputStream out, boolean autoFlush) {
        super (out, autoFlush);
    }

    public void println () {
        print (lineSeparator);
        super.flush();
    }

    public void println(boolean x) {
        synchronized (lock) {
            print(x);
            println();
        }
    }
    public void println(char x) {
        synchronized (lock) {
            print (x);
            println ();
        }
    }
    public void println (int x) {
    synchronized (lock) {
    print (x);
    println ();
    }
    }
    public void println (long x) {
    synchronized (lock) {
    print (x);
    println ();
    }
    }
    public void println (float x) {
    synchronized (lock) {
    print (x);
    println ();
    }
    }
    public void println (double x) {
    synchronized (lock) {
    print (x);
    println ();
    }
    }
    public void println (char[] x) {
    synchronized (lock) {
    print (x);
    println ();
    }
    }
    public void println (String x) {
    synchronized (lock) {
    print (x);
    println ();
    }
    }
    public void println (Object x) {
    synchronized (lock) {
    print (x);
    println ();
    }
    }
}
