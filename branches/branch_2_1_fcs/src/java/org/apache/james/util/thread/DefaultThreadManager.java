/***********************************************************************
 * Copyright (c) 1999-2004 The Apache Software Foundation.             *
 * All rights reserved.                                                *
 * ------------------------------------------------------------------- *
 * Licensed under the Apache License, Version 2.0 (the "License"); you *
 * may not use this file except in compliance with the License. You    *
 * may obtain a copy of the License at:                                *
 *                                                                     *
 *     http://www.apache.org/licenses/LICENSE-2.0                      *
 *                                                                     *
 * Unless required by applicable law or agreed to in writing, software *
 * distributed under the License is distributed on an "AS IS" BASIS,   *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or     *
 * implied.  See the License for the specific language governing       *
 * permissions and limitations under the License.                      *
 ***********************************************************************/

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
