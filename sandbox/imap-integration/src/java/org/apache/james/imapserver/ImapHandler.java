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

package org.apache.james.imapserver;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

import org.apache.avalon.cornerstone.services.connection.ConnectionHandler;
import org.apache.avalon.excalibur.pool.Poolable;
import org.apache.avalon.framework.activity.Disposable;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.james.Constants;
import org.apache.james.imapserver.debug.CopyInputStream;
import org.apache.james.imapserver.debug.SplitOutputStream;
import org.apache.james.util.InternetPrintWriter;
import org.apache.james.util.watchdog.Watchdog;
import org.apache.james.util.watchdog.WatchdogTarget;

/**
 * The handler class for IMAP connections.
 * TODO: This is a quick cut-and-paste hack from POP3Handler. This, and the ImapServer
 * should probably be rewritten from scratch.
 */
public class ImapHandler
        extends AbstractLogEnabled
        implements ImapHandlerInterface, ConnectionHandler, Poolable, ImapConstants
{

    private String softwaretype = "JAMES IMAP4rev1 Server " + Constants.SOFTWARE_VERSION;
    private ImapRequestHandler requestHandler = new ImapRequestHandler();
    private ImapSession session;

    /**
     * The per-service configuration data that applies to all handlers
     */
    private ImapHandlerConfigurationData theConfigData;

    /**
     * The thread executing this handler
     */
    private Thread handlerThread;

    /**
     * The TCP/IP socket over which the IMAP interaction
     * is occurring
     */
    private Socket socket;

    /**
     * The reader associated with incoming characters.
     */
    private BufferedReader in;

    /**
     * The socket's input stream.
     */
    private InputStream ins;

    /**
     * The writer to which outgoing messages are written.
     */
    private PrintWriter out;

    /**
     * The socket's output stream
     */
    private OutputStream outs;

    /**
     * The watchdog being used by this handler to deal with idle timeouts.
     */
    private Watchdog theWatchdog;

    /**
     * The watchdog target that idles out this handler.
     */
    private WatchdogTarget theWatchdogTarget = new IMAPWatchdogTarget();
    
    private boolean handlerIsUp=false;

    /**
     * Set the configuration data for the handler.
     *
     * @param theData the configuration data
     */
    public void setConfigurationData( Object theData )
    {
        if (theData instanceof ImapHandlerConfigurationData) {
            theConfigData = (ImapHandlerConfigurationData) theData;
        } else {
            throw new IllegalArgumentException("Configuration object does not implement POP3HandlerConfigurationData");
        }
    }

    /**
     * Set the Watchdog for use by this handler.
     *
     * @param theWatchdog the watchdog
     */
    public void setWatchdog( Watchdog theWatchdog )
    {
        this.theWatchdog = theWatchdog;
    }

    /**
     * Gets the Watchdog Target that should be used by Watchdogs managing
     * this connection.
     *
     * @return the WatchdogTarget
     */
    WatchdogTarget getWatchdogTarget()
    {
        return theWatchdogTarget;
    }

    public void forceConnectionClose(final String message) {
        getLogger().debug("forceConnectionClose: "+message);
        ImapResponse response = new ImapResponse(outs);
        response.byeResponse(message);        
        resetHandler();
    }

    /**
     * @see ConnectionHandler#handleConnection(Socket)
     */
    public void handleConnection( Socket connection )
            throws IOException
    {
        handlerIsUp=true;
        getLogger().debug("Accepting connection for "+connection.toString());
        // DEBUG
        
        String tcplogprefix= null;
        if (theConfigData.doStreamdump()) {
            String streamdumpDir=theConfigData.getStreamdumpDir();
            tcplogprefix= streamdumpDir+"/TCP-IMAP."+System.currentTimeMillis()+".";
            File logdir = new File(streamdumpDir);
            if (!logdir.exists()) {
                logdir.mkdir();
            }
        }
        String remoteHost = "";
        String remoteIP = "";

        try {
            this.socket = connection;
            synchronized ( this ) {
                handlerThread = Thread.currentThread();
            }
            ins = socket.getInputStream();
            if (theConfigData.doStreamdump()) {
                ins = new CopyInputStream(ins, new FileOutputStream(
                        tcplogprefix + "in"));
            }
            in = new BufferedReader( new InputStreamReader( socket.getInputStream(), "ASCII" ), 512 );
            remoteIP = socket.getInetAddress().getHostAddress();
            remoteHost = socket.getInetAddress().getHostName();
        }
        catch ( IOException e ) {
            if ( getLogger().isErrorEnabled() ) {
                StringBuffer exceptionBuffer =
                        new StringBuffer( 256 )
                        .append( "Cannot open connection from " )
                        .append( remoteHost )
                        .append( " (" )
                        .append( remoteIP )
                        .append( "): " )
                        .append( e.getMessage() );
                getLogger().error( exceptionBuffer.toString(), e );
            }
            throw e;
        }

        if ( getLogger().isInfoEnabled() ) {
            StringBuffer logBuffer =
                    new StringBuffer( 128 )
                    .append( "Connection from " )
                    .append( remoteHost )
                    .append( " (" )
                    .append( remoteIP )
                    .append( ") " );
            getLogger().info( logBuffer.toString() );
        }

        try {
            outs = new BufferedOutputStream( socket.getOutputStream(), 1024 );
            if (theConfigData.doStreamdump()) {
               outs = new SplitOutputStream(outs, new FileOutputStream(tcplogprefix+"out"));
            }
            out = new InternetPrintWriter( outs, true );
            ImapResponse response = new ImapResponse( outs );

            // Write welcome message
            StringBuffer responseBuffer =
                    new StringBuffer( 256 )
                    .append( VERSION )
                    .append( " Server " )
                    .append( theConfigData.getHelloName() )
                    .append( " ready" );
            response.okResponse( null, responseBuffer.toString() );

            session = new ImapSessionImpl( theConfigData.getMailboxManagerProvider(),
                                           theConfigData.getUsersRepository(),
                                           this,
                                           socket.getInetAddress().getHostName(),
                                           socket.getInetAddress().getHostAddress());

            theWatchdog.start();
            while ( requestHandler.handleRequest( ins, outs, session ) ) {
                if (!handlerIsUp) {
                    getLogger().debug("Handler has been resetted");
                    return;
                }
                theWatchdog.reset();
            }
            theWatchdog.stop();
            

            //Write BYE message.
            if ( getLogger().isInfoEnabled() ) {
                StringBuffer logBuffer =
                        new StringBuffer( 128 )
                        .append( "Connection for " )
                        .append( session.getUser().getUserName() )
                        .append( " from " )
                        .append( remoteHost )
                        .append( " (" )
                        .append( remoteIP )
                        .append( ") closed." );
                getLogger().info( logBuffer.toString() );
            }

        }
        catch (Exception e) {
            out.println("Error closing connection.");
            out.flush();
            StringBuffer exceptionBuffer =
                    new StringBuffer( 128 )
                    .append( "Exception on connection from " )
                    .append( remoteHost )
                    .append( " (" )
                    .append( remoteIP )
                    .append( ") : " )
                    .append( e.getMessage() );
            getLogger().error( exceptionBuffer.toString(), e );
        }
        finally {
            resetHandler();
        }
    }

    /**
     * Resets the handler data to a basic state.
     */
    public void resetHandler()
    {
        if (handlerIsUp == false) {
            return;
        }
        handlerIsUp = false;
        if (theWatchdog != null) {
            if (theWatchdog instanceof Disposable) {
                ((Disposable) theWatchdog).dispose();
            }
            theWatchdog = null;
        }

        // Close and clear streams, sockets

        try {
            if ( socket != null ) {
                socket.close();
                socket = null;
            }
        }
        catch ( IOException ioe ) {
            // Ignoring exception on close
        }
        finally {
            socket = null;
        }

        try {
            if ( in != null ) {
                in.close();
            }
        }
        catch ( Exception e ) {
            // Ignored
        }
        finally {
            in = null;
        }

        try {
            if ( out != null ) {
                out.close();
            }
        }
        catch ( Exception e ) {
            // Ignored
        }
        finally {
            out = null;
        }

        try {
            if ( outs != null ) {
                outs.close();
            }
        }
        catch ( Exception e ) {
            // Ignored
        }
        finally {
            outs = null;
        }

        synchronized ( this ) {
            // Interrupt the thread to recover from internal hangs
            if ( handlerThread != null ) {
                handlerThread.interrupt();
                handlerThread = null;
            }
        }

        // Clear user data
        
        try {
               if (session != null) session.closeMailbox();
        } catch (Exception e) {
            getLogger().error("session.cleanUp", e);
        }
        session = null;

        // Clear config data
        theConfigData = null;
    }

    /**
     * A private inner class which serves as an adaptor
     * between the WatchdogTarget interface and this
     * handler class.
     */
    private class IMAPWatchdogTarget
            implements WatchdogTarget
    {

        /**
         * @see WatchdogTarget#execute()
         */
        public void execute()
        {
            forceConnectionClose("IMAP Connection has idled out.");
        }
    }

}

