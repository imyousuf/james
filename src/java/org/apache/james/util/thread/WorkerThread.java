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
 * @author <a href="mailto:stefano@apache.org">Stefano Mazzocchi</a>
 * @author <a href="mailto:peter at apache.org">Peter Donald</a>
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
