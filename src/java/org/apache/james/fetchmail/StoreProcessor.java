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
 
package org.apache.james.fetchmail;

import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;

/**
 * <p>Class <code>StoreProcessor</code> connects to a message store, gets the
 * target Folder and delegates its processing to <code>FolderProcessor</code>.</p>
 * 
 * <p>Creation Date: 27-May-03</p>
 */
public class StoreProcessor extends ProcessorAbstract
{
    /**
     * The Session to use.
     */ 
    private Session fieldSession;

    /**
     * Constructor for StoreProcessor.
     * @param account 
     */
    private StoreProcessor(Account account)
    {
        super(account);        
    }
    
    /**
     * Constructor for StoreProcessor.
     * @param account
     * @param session
     */
    protected StoreProcessor(Account account, Session session)
    {
        this(account);
        setSession(session);
    }

    /**
     * Method process connects to a Folder in a Message Store, creates a
     * <code>FolderProcessor</code> and runs it to process the messages in
     * the Folder.
     * 
     * @see org.apache.james.fetchmail.ProcessorAbstract#process()
     */
    public void process() throws MessagingException
    {
        Store store = null;
        Session session = null;
        Folder folder = null;

        StringBuffer logMessageBuffer =
            new StringBuffer("Starting fetching mail from server '");
        logMessageBuffer.append(getHost());
        logMessageBuffer.append("' for user '");
        logMessageBuffer.append(getUser());
        logMessageBuffer.append("' in folder '");
        logMessageBuffer.append(getJavaMailFolderName());
        logMessageBuffer.append("'");
        getLogger().info(logMessageBuffer.toString());

        try
        {
            // Get a Store object
            store = getSession().getStore(getJavaMailProviderName());

            // Connect
            if (getHost() != null
                || getUser() != null
                || getPassword() != null)
                store.connect(getHost(), getUser(), getPassword());
            else
                store.connect();

            // Get the Folder
            folder = store.getFolder(getJavaMailFolderName());
            if (folder == null)
                getLogger().error(getFetchTaskName() + " No default folder");

            // Process the Folder
            new FolderProcessor(folder, getAccount()).process();

        }
        catch (MessagingException ex)
        {
            getLogger().error(ex.getMessage());
        }
        finally
        {
            try
            {
                if (null != store && store.isConnected())
                    store.close();
            }
            catch (MessagingException ex)
            {
                getLogger().error(ex.getMessage());
            }
            logMessageBuffer =
                new StringBuffer("Finished fetching mail from server '");
            logMessageBuffer.append(getHost());
            logMessageBuffer.append("' for user '");
            logMessageBuffer.append(getUser());
            logMessageBuffer.append("' in folder '");
            logMessageBuffer.append(getJavaMailFolderName());
            logMessageBuffer.append("'");
            getLogger().info(logMessageBuffer.toString());
        }
    }

    /**
     * Returns the session.
     * @return Session
     */
    protected Session getSession()
    {
        return fieldSession;
    }

    /**
     * Sets the session.
     * @param session The session to set
     */
    protected void setSession(Session session)
    {
        fieldSession = session;
    }

}
