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

import org.apache.avalon.excalibur.thread.ThreadControl;

/**
 * Default implementation of ThreadControl interface.
 *
 * @author <a href="mailto:peter at apache.org">Peter Donald</a>
 */
final class DefaultThreadControl
   implements ThreadControl
{
    ///Thread that this control is associated with
       private Thread m_thread;

    ///Throwable that caused thread to terminate
       private Throwable m_throwable;

    /**
     * Construct thread control for a specific thread.
     *
     * @param thread the thread to control
     */
       protected DefaultThreadControl( final Thread thread )
       {
           m_thread = thread;
       }

    /**
     * Wait for specified time for thread to complete it's work.
     *
     * @param milliSeconds the duration in milliseconds to wait until the thread has finished work
     * @throws IllegalStateException if isValid() == false
     * @throws InterruptedException if another thread has interrupted the current thread.
     *            The interrupted status of the current thread is cleared when this exception
     *            is thrown.
     */
       public synchronized void join( final long milliSeconds )
               throws IllegalStateException, InterruptedException
       {
        //final long start = System.currentTimeMillis();
           wait( milliSeconds );
        /*
          if( !isFinished() )
          {
          final long now = System.currentTimeMillis();
          if( start + milliSeconds > now )
          {
          final long remaining = milliSeconds - (now - start);
          join( remaining );
          }
          }
        */
       }

    /**
     * Call Thread.interrupt() on thread being controlled.
     *
     * @throws IllegalStateException if isValid() == false
     * @throws SecurityException if caller does not have permission to call interupt()
     */
       public synchronized void interupt()
               throws IllegalStateException, SecurityException
       {
           if( !isFinished() )
           {
               m_thread.interrupt();
           }
       }

    /**
     * Determine if thread has finished execution
     *
     * @return true if thread is finished, false otherwise
     */
       public synchronized boolean isFinished()
       {
           return ( null == m_thread );
       }

    /**
     * Retrieve throwable that caused thread to cease execution.
     * Only valid when true == isFinished()
     *
     * @return the throwable that caused thread to finish execution
     */
       public Throwable getThrowable()
       {
           return m_throwable;
       }

    /**
     * Method called by thread to release control.
     *
     * @param throwable Throwable that caused thread to complete (may be null)
     */
       protected synchronized void finish( final Throwable throwable )
       {
           m_thread = null;
           m_throwable = throwable;
           notifyAll();
       }
}
