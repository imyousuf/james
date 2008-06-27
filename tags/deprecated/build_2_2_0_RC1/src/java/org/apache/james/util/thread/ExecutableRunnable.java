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

import org.apache.avalon.framework.activity.Executable;

/**
 * Class to adapt a {@link Runnable} object in
 * an {@link Executable} object.
 *
 */
final class ExecutableRunnable
   implements Executable
{
    ///The runnable instance being wrapped
       private Runnable m_runnable;

    /**
     * Create adapter using specified runnable.
     *
     * @param runnable the runnable to adapt to
     */
       protected ExecutableRunnable( final Runnable runnable )
       {
           if( null == runnable )
           {
               throw new NullPointerException( "runnable" );
           }
           m_runnable = runnable;
       }

    /**
     * Execute the underlying {@link Runnable} object.
     *
     * @throws Exception if an error occurs
     */
       public void execute()
               throws Exception
       {
           m_runnable.run();
       }
}
