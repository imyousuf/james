/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache", "Jakarta", "JAMES" and "Apache Software Foundation"
 *    must not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 * Portions of this software are based upon public domain software
 * originally written at the National Center for Supercomputing Applications,
 * University of Illinois, Urbana-Champaign.
 */

package org.apache.james.test;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.types.CommandlineJava;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.avalon.framework.parameters.Parameterizable;
import org.apache.avalon.framework.ExceptionUtil;
import org.apache.avalon.framework.CascadingRuntimeException;
import org.apache.avalon.phoenix.components.embeddor.SingleAppEmbeddor;

/**
 * An attempt at a task which can launch James from Ant, and shut it down again.
 * Doesn't really work, yet.
 */
public final class JamesTask
       extends org.apache.tools.ant.Task
{
    private CommandlineJava cmdl = new CommandlineJava();
    private Parameters m_parameters;
    private static SingleAppEmbeddor m_embeddor;
    private String m_action;

    public void setAction( String action )
    {
        m_action = action;
    }

    /**
     * Set the classpath to be used for this compilation.
     */
    public void setClasspath(Path s) {
        createClasspath().append(s);
    }

    /**
     * Creates a nested classpath element
     */
    public Path createClasspath() {
        return cmdl.createClasspath(project).createPath();
    }

    /**
     * Adds a reference to a CLASSPATH defined elsewhere.
     */
    public void setClasspathRef(Reference r) {
        createClasspath().setRefid(r);
    }


    public void execute() throws BuildException
    {
        if ( m_action.equalsIgnoreCase( "start" ) ) {
            startup();
        }
        else if ( m_action.equalsIgnoreCase( "stop" ) ) {
            shutdown();
        }
        else if ( m_action.equalsIgnoreCase( "start-stop" ) ) {
            startup();
            shutdown();
        }
        else if ( m_action.equalsIgnoreCase( "restart" ) ) {
            optionalShutdown();
            startup();
        }
        else {
            throw new BuildException( "Invalid action: '" + m_action + "'" );
        }
    }

    private void startup() throws BuildException
    {
        if ( m_embeddor != null ) {
           throw new BuildException( "Already started" );
        }

        m_parameters = new Parameters();
//        m_parameters.setParameter( "log-destination", "." );
//        m_parameters.setParameter( "log-priority", "DEBUG" );
//        m_parameters.setParameter( "application-name", "james" );
        m_parameters.setParameter( "application-location", "dist/apps/james.sar");

        try
        {
            m_embeddor = new SingleAppEmbeddor();
            if( m_embeddor instanceof Parameterizable )
            {
                ( (Parameterizable)m_embeddor ).parameterize( m_parameters );
            }
            m_embeddor.initialize();

//            final Thread thread = new Thread( this, "Phoenix" );
//            thread.start();
        }
        catch( final Throwable throwable )
        {
            System.out.println( "Exception in initiliaze()" );
            throw new BuildException( throwable );
        }

        try
        {
            ClassLoader ctxLoader = Thread.currentThread().getContextClassLoader();
            AntClassLoader loader = new AntClassLoader(ctxLoader, project, cmdl.getClasspath(), true);
            loader.setIsolated(false);
            loader.setThreadContextLoader();
            // nothing
            // m_embeddor.execute();
            // Launch the startup thread.
            Thread startThread = new StartupThread();
            startThread.start();

            // Hack to make sure that the embeddor has actually started.
            // Need to make a change to Phoenix, so that we can wait til it's running.
            // Yeild processor.
            Thread.sleep( 1000 );
            // m_embeddor will now be in use until applications are deployed.
            synchronized ( m_embeddor ) {
                System.out.println( "got synch at: " + System.currentTimeMillis() );
            }
        }
        catch( final Throwable throwable )
        {
            System.out.println( "Exception in execute()" );
            throw new BuildException( throwable );
        }

    }

    private class StartupThread extends Thread
    {
        StartupThread()
        {
            super( "JamesStartup" );
            this.setDaemon( true );
        }

        public void run()
        {
            try {
                m_embeddor.execute();
            }
            catch ( Exception exc ) {
                exc.printStackTrace();
                throw new CascadingRuntimeException( "Exception in execute()", exc );
            }
        }

    }

    private void optionalShutdown() throws BuildException
    {
        if ( m_embeddor != null ) {
            shutdown();
        }
    }

    private void shutdown() throws BuildException
    {
        System.out.println( "In shutdown()" );
        if ( m_embeddor == null ) {
            throw new BuildException( "Not running." );
        }

        try
        {
            m_embeddor.dispose();
            System.out.println( "Called dispose()" );
            m_embeddor = null;
            m_parameters = null;
        }
        catch( final Throwable throwable )
        {
            System.out.println( "Exception in dispose()" );
            throw new BuildException( throwable );
        }
    }
}
