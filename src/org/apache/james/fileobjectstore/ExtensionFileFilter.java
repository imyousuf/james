package org.apache.james.fileobjectstore;

import java.io.*;

/**
 * This filters files based on the extension (what the filename ends with).  This is used in retrieving
 * all the files of a particular type (such as .state, .message, or .checked).
 * @author Serge Knystautas <sergek@lokitech.com>
 * @version 0.9
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
