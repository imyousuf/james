/*--- formatted by Jindent 2.0b, (www.c-lab.de/~jindent) ---*/

package org.apache.james.server;

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;
import javax.activation.*;
import org.apache.james.*;
import org.apache.james.util.*;

/**
 * Just wait on the specified port and call the specified handler wheen a socket
 * is opened.
 * @author Federico Barbieri <scoobie@systemy.it>
 * @version 0.9
 */
public class SocketListener implements Runnable, Configurable {
    private ServerSocket ss;
    private int port;
    private String handlerClassName;
    private JamesServ serv;
    private LoggerInterface logger;
    private int timeout;

    /**
     * Class Constructor.
     * 
     * 
     * @see
     */
    public SocketListener() {}

    /**
     * Method Declaration.
     * 
     * 
     * @param serv
     * @param port
     * @param handlerClassName
     * 
     * @exception Exception
     * 
     * @see
     */
    public void init(JamesServ serv, Properties props) 
    throws Exception {
        this.serv = serv;
        this.handlerClassName = props.getProperty("handler");
        this.port = Integer.parseInt(props.getProperty("port"));
        this.timeout = Integer.parseInt(props.getProperty("timeout", "12000"));
        if (handlerClassName == null || port == 0) {
            throw new Exception("Cannot start Listener on port " + port + " with handler " + handlerClassName);
        }
        this.ss = new ServerSocket(port);
        this.logger = serv.getLogger();
    }

    /**
     * Method Declaration.
     * 
     * 
     * @see
     */
    public void run() {

        logger.log("Protocol Handler is " + handlerClassName + " on port " + port + ".", logger.INFO_LEVEL);
        Socket s;
        while (true) {
            try {
                ProtocolHandler handler = (ProtocolHandler) Class.forName(handlerClassName).newInstance();
                s = ss.accept();
                s.setSoTimeout(timeout);
                handler.fill(serv, s);
                new Thread(handler).start();
            } catch (SocketException e) {
                logger.log("Socket closed" + e.getMessage(), logger.ERROR_LEVEL);
            } catch (IOException e) {
                logger.log("Socket closed" + e.getMessage(), logger.ERROR_LEVEL);
            } catch (ClassNotFoundException e) {
                logger.log("Cannot load handler" + e.getMessage(), logger.ERROR_LEVEL);
                System.exit(1);
            } catch (IllegalAccessException e) {
                logger.log(e.getMessage(), logger.ERROR_LEVEL);
                System.exit(1);
            } catch (InstantiationException e) {
                logger.log(e.getMessage(), logger.ERROR_LEVEL);
                System.exit(1);
            }
        }
    }

}



/*--- formatting done in "Sun Java Convention" style on 07-10-1999 ---*/

