/*--- formatted by Jindent 2.0b, (www.c-lab.de/~jindent) ---*/

package org.apache.james.util;

import java.io.*;
import java.util.*;
import org.apache.james.server.JamesServ;

/**
 * This is the main interface defining the configuration pattern. The rule is:
 * Constructors has no parameters.
 * Immediatly after construction the parent must call init(..) to  provide to
 * its child its private configuration (Properties) branch.
 * You can use the server insance to get shared classes like logger etc.
 * @author Federico Barbieri <scoobie@systemy.it>
 * @version 0.9
 */
public interface Configurable {

    /**
     * Method Declaration.
     * 
     * 
     * @param server
     * @param props
     *
     * @see
     */
    public void init(JamesServ server, Properties props)
    throws Exception;
}



/*--- formatting done in "Sun Java Convention" style on 07-12-1999 ---*/

