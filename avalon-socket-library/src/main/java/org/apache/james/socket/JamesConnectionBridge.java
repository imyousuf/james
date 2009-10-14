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

package org.apache.james.socket;

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

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.apache.avalon.cornerstone.services.connection.ConnectionHandler;
import org.apache.avalon.excalibur.pool.Poolable;
import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.avalon.framework.logger.Logger;
import org.apache.commons.logging.Log;
import org.apache.james.api.dnsservice.DNSService;
import org.apache.james.socket.api.CRLFTerminatedReader;
import org.apache.james.socket.api.ProtocolContext;
import org.apache.james.socket.api.ProtocolHandler;
import org.apache.james.socket.api.Watchdog;
import org.apache.james.socket.shared.SwitchableInputStream;
import org.apache.james.socket.shared.SwitchableOutputStream;
import org.apache.james.util.InternetPrintWriter;

/**
 * Common Handler code
 */
public class JamesConnectionBridge implements ProtocolContext, ConnectionHandler, Poolable, WatchdogTarget {
 
    private static final int DEFAULT_OUTPUT_BUFFER_SIZE = 1024;

    private static final int DEFAULT_INPUT_BUFFER_SIZE = 1024;

    /** Name used by default */
    private static final String DEFAULT_NAME = "Handler-ANON";

    /**
     * The thread executing this handler
     */
    private Thread handlerThread;

    /**
     * The TCP/IP socket over which the service interaction
     * is occurring
     */
    private Socket socket;

    /**
     * The writer to which outgoing messages are written.
     */
    private PrintWriter out;
    
    /**
     * The incoming stream of bytes coming from the socket.
     */
    private InputStream in;

    /**
     * The reader associated with incoming characters.
     */
    private CRLFTerminatedReader inReader;

    /**
     * The socket's output stream
     */
    private OutputStream outs;
    
    /**
     * The watchdog being used by this handler to deal with idle timeouts.
     */
    private Watchdog theWatchdog;
    
    /**
     * The remote host name obtained by lookup on the socket.
     */
    private String remoteHost = null;

    /**
     * The remote IP address of the socket.
     */
    private String remoteIP = null;

    /**
     * Used for debug: if not null enable tcp stream dump.
     */
    private String tcplogprefix = null;

    /**
     * Names the handler.
     * This name is used for contextual logging.
     */
    private final String name;
    

    /**
     * The DNSService
     */
    private final DNSService dnsServer;
    
    private final ProtocolHandler protocolHandler;
    
    private final Log log;
    
    private SSLSocketFactory factory;
    private boolean secureEnabled = false;
    private SwitchableInputStream switchableIn;
    private SwitchableOutputStream switchableOut;
    
    public JamesConnectionBridge(final ProtocolHandler delegated, final DNSService dnsServer, final String name, 
            final Logger logger, final SSLSocketFactory factory) {
    	this(delegated, dnsServer, name, logger);
        this.factory = factory;
    }
    
    public JamesConnectionBridge(final ProtocolHandler delegated, final DNSService dnsServer, final String name, 
            final Logger logger) {
        this.protocolHandler = delegated;
        this.dnsServer = dnsServer;
        if (name == null) {
            this.name = DEFAULT_NAME;
        } else {
            this.name = name;
        }
        this.log = new HandlerLog(logger, "[" + name + "] ");
    }

    /**
     * Helper method for accepting connections.
     * This MUST be called in the specializations.
     *
     * @param connection The Socket which belongs to the connection 
     * @throws IOException get thrown if an IO error is detected
     */
    private void initHandler( Socket connection ) throws IOException {
        this.socket = connection;
        remoteIP = socket.getInetAddress().getHostAddress();
        remoteHost = dnsServer.getHostName(socket.getInetAddress());

        try {
            synchronized (this) {
                handlerThread = Thread.currentThread();
            }

            connectStreams(socket);
        } catch (RuntimeException e) {
            StringBuilder exceptionBuffer = 
                new StringBuilder(256)
                    .append("Unexpected exception opening from ")
                    .append(remoteHost)
                    .append(" (")
                    .append(remoteIP)
                    .append("): ")
                    .append(e.getMessage());
            String exceptionString = exceptionBuffer.toString();
            log.error(exceptionString, e);
            throw e;
        } catch (IOException e) {
            StringBuilder exceptionBuffer = 
                new StringBuilder(256)
                    .append("Cannot open connection from ")
                    .append(remoteHost)
                    .append(" (")
                    .append(remoteIP)
                    .append("): ")
                    .append(e.getMessage());
            String exceptionString = exceptionBuffer.toString();
            log.error(exceptionString, e);
            throw e;
        }
        
        if (log.isInfoEnabled()) {
            StringBuilder infoBuffer =
                new StringBuilder(128)
                        .append("Connection from ")
                        .append(remoteHost)
                        .append(" (")
                        .append(remoteIP)
                        .append(")");
            log.info(infoBuffer.toString());
        }
    }


    /**
     * Create a SSLSocket which wraps the given one
     * 
     * @param socket
     * @return sslsocket
     * @throws IOException
     */
    private SSLSocket secureSocket(Socket socket) throws IOException {
        SSLSocket sslsock = (SSLSocket) factory.createSocket(socket, socket
                .getInetAddress().getHostName(), socket.getPort(), true);
        sslsock.setUseClientMode(false);
        getLogger().debug("Finished negotiating SSL - algorithm is "
                        + sslsock.getSession().getCipherSuite());
        return sslsock;
    }
	
    /**
     * Connect all streams of the given socket
     * 
     * @param socket
     * @throws IOException
     */
    private void connectStreams(Socket socket) throws IOException {

        InputStream is = socket.getInputStream();
        OutputStream out2 = socket.getOutputStream();
        switchableIn = new SwitchableInputStream(is);
        switchableOut = new SwitchableOutputStream(out2);
        in = new BufferedInputStream(switchableIn, DEFAULT_INPUT_BUFFER_SIZE);
        outs = new BufferedOutputStream(switchableOut,
                DEFAULT_OUTPUT_BUFFER_SIZE);

        // enable tcp dump for debug
        if (tcplogprefix != null) {
            outs = new SplitOutputStream(outs, new FileOutputStream(
                    tcplogprefix + "out"));
            in = new CopyInputStream(in, new FileOutputStream(tcplogprefix
                    + "in"));
        }

        // An ASCII encoding can be used because all transmissions other
        // that those in the message body command are guaranteed
        // to be ASCII
        inReader = new CRLFTerminatedReader(in, "ASCII");

        out = new InternetPrintWriter(outs, true);
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
            log.warn("Unexpected exception occurred while closing reader: " + ioe);
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
            log.warn("[Unexpected exception occurred while closing socket: " + ioe);
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
            protocolHandler.handleProtocol(this);
            
            log.debug("Closing socket");
        } catch (SocketException se) {
            // Indicates a problem at the underlying protocol level
            if (log.isWarnEnabled()) {
                String message =
                    new StringBuilder(64)
                        .append("Socket to ")
                        .append(remoteHost)
                        .append(" (")
                        .append(remoteIP)
                        .append("): ")
                        .append(se.getMessage()).toString();
                log.warn(message);
                log.debug(se.getMessage(), se);
            }
        } catch ( InterruptedIOException iioe ) {
            if (log.isErrorEnabled()) {
                StringBuilder errorBuffer =
                    new StringBuilder(64)
                        .append("Socket to ")
                        .append(remoteHost)
                        .append(" (")
                        .append(remoteIP)
                        .append(") timeout.");
                log.error( errorBuffer.toString(), iioe );
            }
        } catch ( IOException ioe ) {
            if (log.isWarnEnabled()) {
                String message =
                    new StringBuilder(256)
                            .append("Exception handling socket to ")
                            .append(remoteHost)
                            .append(" (")
                            .append(remoteIP)
                            .append(") : ")
                            .append(ioe.getMessage()).toString();
                log.warn(message);
                log.debug( ioe.getMessage(), ioe );
            }
        } catch (RuntimeException e) {
            if (log.isErrorEnabled()) {
                log.error(e.getMessage(), e );
            }
            if (log.isInfoEnabled()) {
                StringBuilder exceptionBuffer =
                    new StringBuilder(128)
                            .append("Fatal exception during connection from ")
                            .append(getRemoteHost())
                            .append(" (")
                            .append(getRemoteIP())
                            .append("): ")
                            .append(e.getMessage());
                log.info(exceptionBuffer.toString(), e);
            }
            protocolHandler.fatalFailure(e, this);
        } finally {
            //Clear all the session state variables
            cleanHandler();
            resetHandler();
        }
    }

    /**
     * Set the Watchdog for use by this handler.
     *
     * @param theWatchdog the watchdog
     */
    public void setWatchdog(Watchdog theWatchdog) {
        this.theWatchdog = theWatchdog;
    }

    /**
     * Idle out this connection
     */
    private void idleClose() {
        if (log != null) {
            log.error("Service Connection has idled out.");
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
        if (log.isDebugEnabled()) {
            log.debug("Sent: " + responseString);
        }
    }

    /**
     * Write and flush a response string.  The response is also logged.
     * Should be used for the last line of a multi-line response or
     * for a single line response.
     *
     * @param responseString the response string sent to the client
     */
    public final void writeLoggedFlushedResponse(String responseString) {
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
    public final void writeLoggedResponse(String responseString) {
        out.println(responseString);
        logResponseString(responseString);
    }

    /**
     * If not null, this will enable dump to file for tcp connections
     * 
     * @param streamDumpDir the dir
     */
    public void setStreamDumpDir(String streamDumpDir) {
        if (streamDumpDir != null) {
            String streamdumpDir=streamDumpDir;
            this.tcplogprefix = streamdumpDir+"/" + getName() + "_TCP-DUMP."+System.currentTimeMillis()+".";
            File logdir = new File(streamdumpDir);
            if (!logdir.exists()) {
                logdir.mkdir();
            }
        } else {
            this.tcplogprefix = null;
        }
    }

    /**
     * The name of this handler.
     * Used for context sensitive logging.
     * @return the name, not null
     */
    public final String getName() {
        return name;
    }

    /**
     * Use for context sensitive logging.
     * @return the name of the handler
     */
    @Override
    public String toString() {
        return name;
    }

   /**
    * Resets the handler data to a basic state.
    */
    private void resetHandler() {
        protocolHandler.resetHandler();
        remoteHost = null;
        remoteIP = null;
        secureEnabled = false;
    }

    /**
     * @see org.apache.james.socket.api.ProtocolContext#getRemoteIP()
     */
    public String getRemoteIP() {
        return remoteIP;
    }

    /**
     * @see org.apache.james.socket.api.ProtocolContext#getInputReader()
     */
    public CRLFTerminatedReader getInputReader() {
        return inReader;
    }

    /**
     * @see org.apache.james.socket.api.ProtocolContext#getInputStream()
     */
    public InputStream getInputStream() {
        return in;
    }

    /**
     * @see org.apache.james.socket.api.ProtocolContext#getOutputStream()
     */
    public OutputStream getOutputStream() {
        return outs;
    }

    /**
     * @see org.apache.james.socket.api.ProtocolContext#getOutputWriter()
     */
    public PrintWriter getOutputWriter() {
        return out;
    }

    /**
     * @see org.apache.james.socket.api.ProtocolContext#getRemoteHost()
     */
    public String getRemoteHost() {
        return remoteHost;
    }
    
    /**
     * @see org.apache.james.socket.api.ProtocolContext#getWatchdog()
     */
    public Watchdog getWatchdog() {
        return theWatchdog;
    }

    /**
     * @see org.apache.james.socket.api.ProtocolContext#getLogger()
     */
    public Log getLogger() {
        return log;
    }

    /**
     * @see org.apache.james.socket.api.ProtocolContext#isDisconnected()
     */
    public boolean isDisconnected() {
        return socket == null;
    }

    /**
     * @see org.apache.james.socket.WatchdogTarget#execute()
     */
    public void execute() {
        idleClose();
    }

    /**
     * @see org.apache.james.socket.api.ProtocolContext#isSecure()
     */
    public boolean isSecure() {
        return secureEnabled;
    }

    /**
     * @see org.apache.james.socket.api.ProtocolContext#secure()
     */
    public void secure() throws IOException {
        if (factory == null) {
            throw new UnsupportedOperationException("StartTLS not supported");
        }

        socket = secureSocket(socket);

        switchableIn.setWrappedInputStream(socket.getInputStream());
        switchableOut.setWrappedOutputStream(socket.getOutputStream());
        this.secureEnabled = true;

    }
}
