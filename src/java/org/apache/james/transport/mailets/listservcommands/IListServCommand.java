/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2003 The Apache Software Foundation.  All rights
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
package org.apache.james.transport.mailets.listservcommands;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.james.transport.mailets.ICommandListservManager;
import org.apache.mailet.Mail;

import javax.mail.MessagingException;

/**
 * IListServCommand is the interface that all pluggable list serv commands must implement.
 * The lifecycle of a IListServCommand will be controlled by the {@link ICommandListservManager}
 *
 * <br />
 * <br />
 * Requests sent to the CommandListservManager take the form of:
 * <pre>
 * &lt;listName&gt;-&lt;commandName&gt;@domain
 * </pre>
 * and if the commandName matches the command's name, then the {@link #onCommand} will be invoked.
 *
 * <br />
 * <br />
 * A typical command is configured:
 * <pre>
 * &lt;command name="subscribe" class="Subscribe"/&gt;
 * </pre>
 *
 * <br />
 * <br />
 * Typically, IListServCommands will format some text to reply with based off of resource files
 * and calls to {@link org.apache.james.util.XMLResources#getString}
 *
 * This allows you to customize the messages sent by these commands by editing text files and not editing the javacode.
 *
 * @see ICommandListservManager
 */
public interface IListServCommand {

    /**
     * The name of this command
     * specified by the 'name' parameter.
     * eg:
     * <pre>
     * &lt;command name="subscribe" class="Subscribe"/&gt;
     * </pre>
     * @return the name of this command
     */
    public String getCommandName();

    /**
     * Perform any required initialization
     * @param configuration
     * @throws ConfigurationException
     */
    public void init(ICommandListservManager commandListservManager, Configuration configuration) throws ConfigurationException;

    /**
     * Process this command to your hearts content
     * @param mail
     * @throws MessagingException
     */
    public void onCommand(Mail mail) throws MessagingException;
}
