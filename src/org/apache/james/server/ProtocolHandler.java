/*--- formatted by Jindent 2.0b, (www.c-lab.de/~jindent) ---*/

package org.apache.james.server;

import java.io.*;
import java.net.*;

/**
 * General Socket Handler. Gets a socket and menage protocol over it.
 * @author Federico Barbieri <scoobie@systemy.it>
 * @version 0.9
 */
public interface ProtocolHandler extends Runnable {

    /**
     * Method Declaration.
     * 
     * 
     * @param socket
     * @param server
     * 
     * @exception IOException
     * 
     * @see
     */
    public void fill(JamesServ server, Socket socket) throws IOException;
}



/*--- formatting done in "Sun Java Convention" style on 07-10-1999 ---*/

