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
		}
		public void println (boolean x) {
			  synchronized (lock) {
					  print (x);
					  println ();
				}
		}
		public void println (char x) {
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
