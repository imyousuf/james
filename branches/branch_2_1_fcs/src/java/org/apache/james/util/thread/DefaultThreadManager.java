/*

 ============================================================================
                   The Apache Software License, Version 1.1
 ============================================================================

 Copyright (C) 1999-2003 The Apache Software Foundation. All rights reserved.

 Redistribution and use in source and binary forms, with or without modifica-
 tion, are permitted provided that the following conditions are met:

 1. Redistributions of  source code must  retain the above copyright  notice,
    this list of conditions and the following disclaimer.

 2. Redistributions in binary form must reproduce the above copyright notice,
    this list of conditions and the following disclaimer in the documentation
    and/or other materials provided with the distribution.

 3. The end-user documentation included with the redistribution, if any, must
    include  the following  acknowledgment:  "This product includes  software
    developed  by the  Apache Software Foundation  (http://www.apache.org/)."
    Alternately, this  acknowledgment may  appear in the software itself,  if
    and wherever such third-party acknowledgments normally appear.

 4. The names "Jakarta", "Apache Avalon", "Avalon Cornerstone", "Avalon
    Framework" and "Apache Software Foundation"  must not be used to endorse
    or promote products derived  from this  software without  prior written
    permission. For written permission, please contact apache@apache.org.

 5. Products  derived from this software may not  be called "Apache", nor may
    "Apache" appear  in their name,  without prior written permission  of the
    Apache Software Foundation.

 THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 FITNESS  FOR A PARTICULAR  PURPOSE ARE  DISCLAIMED.  IN NO  EVENT SHALL  THE
 APACHE SOFTWARE  FOUNDATION  OR ITS CONTRIBUTORS  BE LIABLE FOR  ANY DIRECT,
 INDIRECT, INCIDENTAL, SPECIAL,  EXEMPLARY, OR CONSEQUENTIAL  DAMAGES (INCLU-
 DING, BUT NOT LIMITED TO, PROCUREMENT  OF SUBSTITUTE GOODS OR SERVICES; LOSS
 OF USE, DATA, OR  PROFITS; OR BUSINESS  INTERRUPTION)  HOWEVER CAUSED AND ON
 ANY  THEORY OF LIABILITY,  WHETHER  IN CONTRACT,  STRICT LIABILITY,  OR TORT
 (INCLUDING  NEGLIGENCE OR  OTHERWISE) ARISING IN  ANY WAY OUT OF THE  USE OF
 THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 This software  consists of voluntary contributions made  by many individuals
 on  behalf of the Apache Software  Foundation. For more  information on the
 Apache Software Foundation, please see <http://www.apache.org/>.

*/

package org.apache.james.util.thread;

import java.util.HashMap;
import org.apache.avalon.cornerstone.services.threads.ThreadManager;
import org.apache.excalibur.threadcontext.ThreadContext;
import org.apache.avalon.excalibur.thread.ThreadPool;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.component.Component;
import org.apache.avalon.framework.logger.AbstractLogEnabled;

/**
 * Default implementation of ThreadManager.
 *
 * @phoenix:block
 * @phoenix:service name="org.apache.avalon.cornerstone.services.threads.ThreadManager"
 *
 * @author <a href="mailto:peter at apache.org">Peter Donald</a>
 */
public class DefaultThreadManager
   extends AbstractLogEnabled
   implements ThreadManager, Configurable, Component
{
    ///Map of thread pools for application
       private HashMap m_threadPools = new HashMap();

    /**
     * Setup thread pools based on configuration data.
     *
     * @param configuration the configuration data
     * @exception ConfigurationException if an error occurs
     * @phoenix:configuration-schema type="relax-ng"
     */
       public void configure( final Configuration configuration )
               throws ConfigurationException
       {
           ThreadContext threadContext = ThreadContext.getThreadContext();
           if( null != threadContext )
           {
               threadContext = threadContext.duplicate();
           }

           final Configuration[] groups = configuration.getChildren( "thread-group" );
           for( int i = 0; i < groups.length; i++ )
           {
               configureThreadPool( groups[ i ], threadContext );
           }
       }

       private void configureThreadPool( final Configuration configuration,
                                         final ThreadContext threadContext )
               throws ConfigurationException
       {
           final String name = configuration.getChild( "name" ).getValue();
           final int priority = configuration.getChild( "priority" ).getValueAsInteger( 5 );
           final boolean isDaemon = configuration.getChild( "is-daemon" ).getValueAsBoolean( false );

           final int minThreads = configuration.getChild( "min-threads" ).getValueAsInteger( 5 );
           final int maxThreads = configuration.getChild( "max-threads" ).getValueAsInteger( 10 );
           final int minSpareThreads = configuration.getChild( "min-spare-threads" ).
                                       getValueAsInteger( maxThreads - minThreads );

           try
           {
               final DefaultThreadPool threadPool =
                   new DefaultThreadPool( name, minThreads, maxThreads, threadContext );
               threadPool.setDaemon( isDaemon );
               threadPool.enableLogging( getLogger() );
               m_threadPools.put( name, threadPool );
           }
           catch( final Exception e )
           {
               final String message = "Error creating ThreadPool named " + name;
               throw new ConfigurationException( message, e );
           }
       }

    /**
     * Retrieve a thread pool by name.
     *
     * @param name the name of thread pool
     * @return the threadpool
     * @exception IllegalArgumentException if the name of thread pool is
     *            invalid or named pool does not exist
     */
       public ThreadPool getThreadPool( final String name )
               throws IllegalArgumentException
       {
           final ThreadPool threadPool = (ThreadPool)m_threadPools.get( name );

           if( null == threadPool )
           {
               final String message = "Unable to locate ThreadPool named " + name;
               throw new IllegalArgumentException( message );
           }

           return threadPool;
       }

    /**
     * Retrieve the default thread pool.
     *
     * @return the thread pool
     */
       public ThreadPool getDefaultThreadPool()
       {
           return getThreadPool( "default" );
       }
}
