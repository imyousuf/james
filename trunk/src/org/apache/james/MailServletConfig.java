/*--- formatted by Jindent 2.0b, (www.c-lab.de/~jindent) ---*/

package org.apache.james;

import java.io.*;
import java.util.*;

/**
 * This object is passed to mail servlets in their <code>init</code> method.
 * 
 * @author Serge Knystautas <sergek@lokitech.com>
 * @version 0.9
 * @see MailServlet#init
 */
public interface MailServletConfig {

    /**
     * Returns the servlet's conf file as a File object, or null if there are no initialization parameters.
     * @return java.io.File
     */
    public File getConfFile();

    /**
     * This method was created in VisualAge.
     * @return org.apache.james.MailServletContext
     */
    public MailServletContext getContext();

    /**
     * Returns a string containing the value of the named initialization parameter of the servlet,
     * or null if the parameter does not exist.
     * @return java.lang.String
     * @param name java.lang.String
     */
    public String getInitParameter(String name);

    /**
     * Returns the names of the servlet's initialization parameters as an enumeration of strings,
     * or an empty enumeration if there are no initialization parameters.
     * @return java.util.Enumeration
     */
    public Enumeration getInitParameterNames();
}



/*--- formatting done in "Sun Java Convention" style on 07-11-1999 ---*/

