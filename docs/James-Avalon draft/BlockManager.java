/*
 * Copyright (c) 1999 The Java Apache Project.  All rights reserved.
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
 * 3. All advertising materials mentioning features or use of this
 *    software must display the following acknowledgment:
 *    "This product includes software and design ideas developed by the Java
 *    Apache Project (http://java.apache.org/)."
 *
 * 4. Redistributions of any form whatsoever must retain the following
 *    acknowledgment:
 *    "This product includes software and design ideas developed by the Java
 *    Apache Project (http://java.apache.org/)."
 *
 * THIS SOFTWARE IS PROVIDED BY THE JAVA APACHE PROJECT "AS IS" AND ANY
 * EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE JAVA APACHE PROJECT OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Java Apache Project. For more information
 * on the Java Apache Project please see <http://java.apache.org/>.
 */
package org.apache.avalon;

import org.apache.avalon.configuration.*;

/**
 * The <code>Framework</code> interface.
 * <br>
 * This interface is implemented by the framework and defines APIs Avalon
 * provides to <code>Block</code>s. Each <code>Block</code> will be given a
 * private implementation of this interface as a parameter in its
 * <code>init()</code> method.
 *
 * @see Block#init(Framework)
 * @version 1.0.0 (CVS $Revision: 1.1 $ $Date: 1999/09/03 00:29:28 $)
 * @author <a href="mailto:scoobie@pop.systemy.it">Federico Barbieri</a>
 * @author <a href="mailto:pier@apache.org">Pierpaolo Fumagalli</a>
 * @author <a href="mailto:stefano@apache.org">Stefano Mazzocchi</a>
 * @author <a href="http://java.apache.org/">Java Apache Project</a>
 */

public interface BlockManager {
    /**
     * This interface version (1.0.0).
     */
    public static final Version INTERFACE_VERSION=new Version(1,0,0,false);

    /**
     * Return the instance of a <code>Block</code> implementing the given
     * interface name.
     * <br>
     * The framework will check in its configurations if an implementation
     * of the interface specified as a parameter has been defined, and will
     * return an instance of it to the caller.
     *
     * @parameter name The name of the interface required as it appears
     *                 in package <b>org.apache.avalon.blocks</b>.
     * @return A <code>Block</code> instance implementing the given interface.
     * @exception SecurityException (Reserved for when Avalon will manage
     *                              security levels)
     * @exception IllegalArgumentException If the specified name cannot be
     *                                     found thru the interfaces defined
     *                                     in <b>org.apache.avalon.blocks</b>
     *                                     package.
     */
    public Block getBlock(String interfaceName)
    throws SecurityException, IllegalArgumentException, FrameworkException;

    /**
     * Executes the specified <code>Work</code> object in a separate thread.
     * <br>
     * This method is equivalent to:
     * <br>
     * <code>execute(work, 0);</code>
     *
     * <p><blockquote><i>
     * NOTE: more <code>execute(...)</code> methods must be defined to
     * properly handle thread priorities, security management and timeouts.
     * (Pier)
     * </i></blockquote></p>
     *
     * @param work The <code>Work</code> that must be executed.
     * @exception SecurityException (Reserved for when Avalon will manage
     *                              security levels)
     * @exception NullPointerException If the specified <code>Work</code>
     *                                 object is <b>null</b>.
     */
    public void execute(Work work)
    throws SecurityException, NullPointerException;

    /**
     * Executes the specified <code>Work</code> object in a separate thread
     * with a specified timeout.
     * <br>
     * If the timeout is 0 (zero) then the <code>stop()</code> method of the
     * specified <code>Work</code> object is never called.
     *
     * <p><blockquote><i>
     * NOTE: more <code>execute(...)</code> methods must be defined to
     * properly handle thread priorities, security management and timeouts.
     * (Pier)
     * </i></blockquote></p>
     *
     * @param work The <code>Work</code> that must be executed.
     * @param timeout The time to wait before the framework will call the
     *                <code>stop()</code> method.
     * @exception SecurityException (Reserved for when Avalon will manage
     *                              security levels)
     * @exception NullPointerException If the specified <code>Work</code>
     *                                 object is <b>null</b>.
     * @exception IllegalArgumentException If the specified timeout is less
     *                                     than zero.
     */
    public void execute(Work work, long timeout)
    throws SecurityException, NullPointerException, IllegalArgumentException;

    /**
     * Return a <code>Configuration</code> object containing caller
     * <code>Block</code> confgurations.
     *
     * <p><blockquote><i>
     * NOTE: Configuration will be defined in <b>org.apache.java.config</b>
     * package. (Pier)
     * </i></blockquote></p>
     *
     * @return A <b>non null</b> <code>Configuration</code> object containing
     *         calling <code>Block</code> configurations.
     */
    public Configuration getConfigurations();
    
    
    /**
     * To be added to current Avalon implementation. 
     */
    public RunnablePool getRunnablePool(String className, Configuration conf) {
    
        // Return an instance of a recycling pool filled with the specified
        // (Configurable) class. Each instance will be initializated calling 
        // init(conf).
        
    }    
}

