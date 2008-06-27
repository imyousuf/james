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

import org.apache.avalon.excalibur.pool.Pool;
import org.apache.avalon.excalibur.pool.Poolable;
import org.apache.avalon.excalibur.thread.ThreadControl;
import org.apache.avalon.framework.activity.Executable;
import org.apache.avalon.framework.logger.LogEnabled;
import org.apache.avalon.framework.logger.Logger;
import org.apache.excalibur.threadcontext.ThreadContext;

/**
 * This class extends the Thread class to add recyclable functionalities.
 *
 */
class WorkerThread
   extends Thread
   implements Poolable, LogEnabled
{
       private Logger m_logger;
       private Pool m_pool;

       private Executable m_work;
       private DefaultThreadControl m_threadControl;
       private ThreadContext m_context;
       private boolean m_alive;

       private String m_name;

    /**
     * Allocates a new <code>Worker</code> object.
     */
       protected WorkerThread( final ThreadGroup group,
                               final String name,
                               final Pool pool,
                               final ThreadContext context )
       {
           super( group, "" );
           m_name = name;
           m_pool = pool;
           m_context = context;
           m_work = null;
           m_alive = true;

           setDaemon( false );
       }

       public void enableLogging( final Logger logger )
       {
           m_logger = logger;
       }

    /**
     * The main execution loop.
     */
       public final synchronized void run()
       {
           debug( "starting." );

        // Notify the pool this worker started running.
        //notifyAll();

           while( m_alive )
           {
               waitUntilCondition( true );

               debug( "running." );

               try
               {
                //TODO: Thread name setting should reuse the ThreadContext code.
                   Thread.currentThread().setName( m_name );
                   if( null != m_context ) ThreadContext.setThreadContext( m_context );
                   m_work.execute();
                   m_threadControl.finish( null );
               }
               catch( final ThreadDeath threadDeath )
               {
                   debug( "thread has died." );
                   m_threadControl.finish( threadDeath );
                // This is to let the thread death propagate to the runtime
                // enviroment to let it know it must kill this worker
                   throw threadDeath;
               }
               catch( final Throwable throwable )
               {
                // Error thrown while working.
                   debug( "error caught: " + throwable );
                   m_threadControl.finish( throwable );
               }
               finally
               {
                   debug( "done." );
                   m_work = null;
                   m_threadControl = null;
                   if( null != m_context ) ThreadContext.setThreadContext( null );
               }

            //should this be just notify or notifyAll ???
            //It seems to resource intensive option to use notify()
            //notifyAll();
               notify();

            // recycle ourselves
               if( null != m_pool )
               {
                   m_pool.put( this );
               }
               else
               {
                   m_alive = false;
               }
           }
       }

    /**
     * Set the <code>alive</code> variable to false causing the worker to die.
     * If the worker is stalled and a timeout generated this call, this method
     * does not change the state of the worker (that must be destroyed in other
     * ways).
     */
       public void dispose()
       {
           debug( "destroying." );
           m_alive = false;
           waitUntilCondition( false );
       }

       protected synchronized ThreadControl execute( final Executable work )
       {
           m_work = work;
           m_threadControl = new DefaultThreadControl( this );

           debug( "notifying this worker." );
           notify();

           return m_threadControl;
       }

    /**
     * Set the <code>Work</code> code this <code>Worker</code> must
     * execute and <i>notifies</i> its thread to do it.
     */
       protected synchronized void executeAndWait( final Executable work )
       {
           execute( work );
           waitUntilCondition( false );
       }

       private synchronized void waitUntilCondition( final boolean hasWork )
       {
           while( hasWork == ( null == m_work ) )
           {
               try
               {
                   debug( "waiting." );
                   wait();
                   debug( "notified." );
               }
               catch( final InterruptedException ie )
               {
               }
           }
       }

       private void debug( final String message )
       {
           if( false )
           {
               final String output = getName() + ": " + message;
               m_logger.debug( output );
            //System.out.println( output );
           }
       }
}
