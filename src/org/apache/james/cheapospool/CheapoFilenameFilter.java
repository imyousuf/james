/*--- formatted by Jindent 2.0b, (www.c-lab.de/~jindent) ---*/

package org.apache.james.cheapospool;

import java.io.*;

/**
 * This filters files passed on what their filename begins with.  Used in finding particular message-IDs
 * since all messages and state are stored using their message-ID.
 * @author Serge Knystautas <sergek@lokitech.com>
 * @version 0.9
 */
public class CheapoFilenameFilter implements FilenameFilter {
    String filename;

    /**
     * CheapoFilenameFilter constructor comment.
     */
    public CheapoFilenameFilter(String filename) {
        this.filename = filename + ".";
    }

    /**
     * accept method comment.
     */
    public boolean accept(File dir, String name) {
        return name.startsWith(filename);
    }

}



/*--- formatting done in "Sun Java Convention" style on 07-11-1999 ---*/

