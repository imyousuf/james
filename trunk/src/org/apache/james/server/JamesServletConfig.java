/*--- formatted by Jindent 2.0b, (www.c-lab.de/~jindent) ---*/

package org.apache.james.server;

import java.io.*;
import java.util.*;
import org.apache.james.*;

/**
 * Cheapo implementation of the MailServletConfig.  It doesn't take much to build this.  Eventually
 * the MailServletConfig class will become more useful and necessitate a more complex implementation.
 * @author Serge Knystautas <sergek@lokitech.com>
 * @version 0.9
 */
public class JamesServletConfig implements MailServletConfig {
    Properties props;
    File confFile = null;
    MailServletContext context;

    /**
     * JamesServletConfig constructor comment.
     */
    protected JamesServletConfig(Properties props, String filename, MailServletContext context) {
        this.props = props;
        this.context = context;

        // Create the conf file if it is there

        if (filename != null) {
            confFile = new File(filename);
        } 
    }

    /**
     * getConfFile method comment.
     */
    public File getConfFile() {
        return confFile;
    }

    /**
     * This method was created in VisualAge.
     * @return org.apache.james.MailServletContext
     */
    public MailServletContext getContext() {
        return context;
    }

    /**
     * getInitParameter method comment.
     */
    public String getInitParameter(String name) {
        return props.getProperty(name);
    }

    /**
     * getInitParameterNames method comment.
     */
    public Enumeration getInitParameterNames() {
        return props.keys();
    }

}



/*--- formatting done in "Sun Java Convention" style on 07-11-1999 ---*/

