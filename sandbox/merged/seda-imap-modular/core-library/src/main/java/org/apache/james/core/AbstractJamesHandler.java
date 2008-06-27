/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/



package org.apache.james.core;

import org.apache.avalon.cornerstone.services.connection.ConnectionHandler;
import org.apache.avalon.excalibur.pool.Poolable;
import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.james.services.DNSServer;
import org.apache.james.util.CRLFTerminatedReader;
import org.apache.james.util.InternetPrintWriter;
import org.apache.james.util.watchdog.Watchdog;
import org.apache.james.util.watchdog.WatchdogTarget;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;

/**
 * Common Handler code
 */
public abstract class AbstractJamesHandler extends AbstractLogEnabled implements ConnectionHandler, Poolable,Serviceable {


    /**
     * The thread executing this handler
     */
    protected Thread handlerThread;

    /**
     * The TCP/IP socket over which the service interaction
     * is occurring
     */
    protected Socket socket;

    /**
     * The writer to which outgoing messages are written.
     */
    protected PrintWriter out;
    
    /**
     * The incoming stream of bytes coming from the socket.
     */
    protected InputStream in;

    /**
     * The reader associated with incoming characters.
     */
    protected CRLFTerminatedReader inReader;

    /**
     * The socket's output stream
     */
    protected OutputStream outs;
    
    /**
     * The watchdog being used by this handler to deal with idle timeouts.
     */
    protected Watchdog theWatchdog;

    /**
     * The watchdog target that idles out this handler.
     */
    private WatchdogTarget theWatchdogTarget = new JamesWatchdogTarget();

    /**
     * This method will be implemented checking for the correct class
     * type.
     * 
     * @param theData Configuration Bean.
     */
    public abstract void setConfigurationData(Object theData);
    

    /**
     * The remote host name obtained by lookup on the socket.
     */
    protected String remoteHost = null;

    /**
     * The remote IP address of the socket.
     */
    protected String remoteIP = null;

    /**
     * The DNSServer
     */
    protected DNSServer dnsServer = null;

    /**
     * Used for debug: if not null enable tcp stream dump.
     */
    private String tcplogprefix = null;
    

    /**
     * @see org.apache.avalon.framework.service.Serviceable#service(org.apache.avalon.framework.service.ServiceManager)
     */
    public void service(ServiceManager arg0) throws ServiceException {
        setDnsServer((DNSServer) arg0.lookup(DNSServer.ROLE));
    }

    /**
     * Helper method for accepting connections.
     * This MUST be called in the specializations.
     *
     * @param connection The Socket which belongs to the connection 
     * @throws IOException get thrown if an IO error is detected
     */
    protected void initHandler( Socket connection ) throws IOException {
        this.socket = connection;
        remoteIP = socket.getInetAddress().getHostAddress();
        remoteHost = dnsServer.getHostName(socket.getInetAddress());

        try {
            synchronized (this) {
                handlerThread = Thread.currentThread();
            }
            in = new BufferedInputStream(socket.getInputStream(), 1024);
            // An ASCII encoding can be used because all transmissions other
            // that those in the message body command are guaranteed
            // to be ASCII
            
            outs = new BufferedOutputStream(socket.getOutputStream(), 1024);
            // enable tcp dump for debug
            if (tcplogprefix != null) {
                outs = new SplitOutputStream(outs, new FileOutputStream(tcplogprefix+"out"));
                in = new CopyInputStream(in, new FileOutputStream(tcplogprefix+"in"));
            }
            inReader = new CRLFTerminatedReader(in, "ASCII");
            
            out = new InternetPrintWriter(outs, true);
        } catch (RuntimeException e) {
            StringBuffer exceptionBuffer = 
                new StringBuffer(256)
                    .append("Unexpected exception opening from ")
                    .append(remoteHost)
                    .append(" (")
                    .append(remoteIP)
                    .append("): ")
                    .append(e.getMessage());
            String exceptionString = exceptionBuffer.toString();
            getLogger().error(exceptionString, e);
            throw e;
        } catch (IOException e) {
            StringBuffer exceptionBuffer = 
                new StringBuffer(256)
                    .append("Cannot open connection from ")
                    .append(remoteHost)
                    .append(" (")
                    .append(remoteIP)
                    .append("): ")
                    .append(e.getMessage());
            String exceptionString = exceptionBuffer.toString();
            getLogger().error(exceptionString, e);
            throw e;
        }
        
        if (getLogger().isInfoEnabled()) {
            StringBuffer infoBuffer =
                new StringBuffer(128)
                        .append("Connection from ")
                        .append(remoteHost)
                        .append(" (")
                        .append(remoteIP)
                        .append(")");
            getLogger().info(infoBuffer.toString());
        }
    }

    /**
     * The method clean up and close the allocated resources
     */
    private void cleanHandler() {
        // Clear the Watchdog
        if (theWatchdog != null) {
            ContainerUtil.dispose(theWatchdog);
            theWatchdog = null;
        }

        // Clear the streams
        try {
            if (inReader != null) {
                inReader.close();
            }
        } catch (IOException ioe) {
            getLogger().warn("Handler: Unexpected exception occurred while closing reader: " + ioe);
        } finally {
            inReader = null;
        }

        in = null;

        if (out != null) {
            out.close();
            out = null;
        }
        outs = null;

        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ioe) {
            getLogger().warn("Handler: Unexpected exception occurred while closing socket: " + ioe);
        } finally {
            socket = null;
        }
        
        remoteIP = null;
        remoteHost = null;

        synchronized (this) {
            handlerThread = null;
        }
    }

    /**
     * @see org.apache.avalon.cornerstone.services.connection.ConnectionHandler#handleConnection(java.net.Socket)
     */
    public void handleConnection(Socket connection) throws IOException {
        initHandler(connection);

        try {
            
            // Do something:
            handleProtocol();
            
            getLogger().debug("Closing socket.");
        } catch (SocketException se) {
            if (getLogger().isErrorEnabled()) {
                StringBuffer errorBuffer =
                    new StringBuffer(64)
                        .append("Socket to ")
                        .append(remoteHost)
                        .append(" (")
                        .append(remoteIP)
                        .append(") closed remotely.");
                getLogger().error(errorBuffer.toString(), se );
            }
        } catch ( InterruptedIOException iioe ) {
            if (getLogger().isErrorEnabled()) {
                StringBuffer errorBuffer =
                    new StringBuffer(64)
                        .append("Socket to ")
                        .append(remoteHost)
                        .append(" (")
                        .append(remoteIP)
                        .append(") timeout.");
                getLogger().error( errorBuffer.toString(), iioe );
            }
        } catch ( IOException ioe ) {
            if (getLogger().isErrorEnabled()) {
                StringBuffer errorBuffer =
                    new StringBuffer(256)
                            .append("Exception handling socket to ")
                            .append(remoteHost)
                            .append(" (")
                            .append(remoteIP)
                            .append(") : ")
                            .append(ioe.getMessage());
                getLogger().error( errorBuffer.toString(), ioe );
            }
        } catch (RuntimeException e) {
            errorHandler(e);
        } finally {
            //Clear all the session state variables
            cleanHandler();
            resetHandler();
        }
    }

    /**
     * Method which will be colled on error
     *  
     * @param e the RuntimeException
     */
    protected void errorHandler(RuntimeException e) {
        if (getLogger().isErrorEnabled()) {
            getLogger().error( "Unexpected runtime exception: "
                               + e.getMessage(), e );
        }
    }


    /**
     * Handle the protocol
     * 
     * @throws IOException get thrown if an IO error is detected
     */
    protected abstract void handleProtocol() throws IOException;

   /**
    * Resets the handler data to a basic state.
    */
    protected abstract void resetHandler();


    /**
     * Set the Watchdog for use by this handler.
     *
     * @param theWatchdog the watchdog
     */
    public void setWatchdog(Watchdog theWatchdog) {
        this.theWatchdog = theWatchdog;
    }

    /**
     * Gets the Watchdog Target that should be used by Watchdogs managing
     * this connection.
     *
     * @return the WatchdogTarget
     */
    WatchdogTarget getWatchdogTarget() {
        return theWatchdogTarget;
    }

    /**
     * Idle out this connection
     */
    void idleClose() {
        if (getLogger() != null) {
            getLogger().error("Service Connection has idled out.");
        }
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (Exception e) {
            // ignored
        } finally {
            socket = null;
        }

        synchronized (this) {
            // Interrupt the thread to recover from internal hangs
            if (handlerThread != null) {
                handlerThread.interrupt();
                handlerThread = null;
            }
        }

    }

    /**
     * This method logs at a "DEBUG" level the response string that
     * was sent to the service client.  The method is provided largely
     * as syntactic sugar to neaten up the code base.  It is declared
     * private and final to encourage compiler inlining.
     *
     * @param responseString the response string sent to the client
     */
    private final void logResponseString(String responseString) {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("Sent: " + responseString);
        }
    }

    /**
     * Write and flush a response string.  The response is also logged.
     * Should be used for the last line of a multi-line response or
     * for a single line response.
     *
     * @param responseString the response string sent to the client
     */
    protected final void writeLoggedFlushedResponse(String responseString) {
        out.println(responseString);
        out.flush();
        logResponseString(responseString);
    }

    /**
     * Write a response string.  The response is also logged.
     * Used for multi-line responses.
     *
     * @param responseString the response string sent to the client
     */
    protected final void writeLoggedResponse(String responseString) {
        out.println(responseString);
        logResponseString(responseString);
    }

    /**
     * A private inner class which serves as an adaptor
     * between the WatchdogTarget interface and this
     * handler class.
     */
    private class JamesWatchdogTarget
        implements WatchdogTarget {

        /**
         * @see org.apache.james.util.watchdog.WatchdogTarget#execute()
         */
        public void execute() {
            AbstractJamesHandler.this.idleClose();
        }

    }

    /**
     * If not null, this will enable dump to file for tcp connections
     * 
     * @param streamDumpDir the dir
     */
    public void setStreamDumpDir(String streamDumpDir) {
        if (streamDumpDir != null) {
            String streamdumpDir=streamDumpDir;
            this.tcplogprefix = streamdumpDir+"/TCP-DUMP."+System.currentTimeMillis()+".";
            File logdir = new File(streamdumpDir);
            if (!logdir.exists()) {
                logdir.mkdir();
            }
        } else {
            this.tcplogprefix = null;
        }
    }

    public void setDnsServer(DNSServer dnsServer) {
        this.dnsServer = dnsServer;
    }

}
