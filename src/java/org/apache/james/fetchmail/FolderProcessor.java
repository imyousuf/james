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

import javax.mail.FetchProfile;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

/**
 * <p>Class <code>FolderProcessor</code> opens a Folder, fetches the Envelopes for
 * each Message and then iterates over all of the Messages, delegating their
 * processing to <code>MessageProcessor</code>.</p>
 * 
 * <p>If isRecurse(), all subfolders are fetched recursively.</p>
 * 
 * <p>Creation Date: 25-May-03</p>
 *
 */
public class FolderProcessor extends ProcessorAbstract
{
    /**
     * The fetched folder
     */ 
    private Folder fieldFolder;
    
    private Boolean fieldMarkSeenPermanent;

    /**
     * Constructor for FolderProcessor.
     * @param folder The folder to be fetched
     * @param account The account being processed
     */
    protected FolderProcessor(Folder folder, Account account)
    {
        super(account);
        setFolder(folder);
    }
    
    /**
     * Method process opens a Folder, fetches the Envelopes for all of its 
     * Messages, creates a <code>MessageProcessor</code> and runs it to process
     * each message.
     * 
     * @see org.apache.james.fetchmail.ProcessorAbstract#process()
     */
    public void process() throws MessagingException
    {
        int messagesProcessed = 0;
        try
        {
            // open the folder            
            try
            {
                open();
            }
            catch (MessagingException ex)
            {
                getLogger().error(
                    getFetchTaskName() + " Failed to open folder!");
                throw ex;
            }

            // Fetch the Envelopes from the folder
            // Note that the fetch profile causes the envelope contents
            // for all of the messages to be bulk fetched. This is an
            // efficiency measure as we know we are going to read each envelope.
            Message[] messagesIn = getFolder().getMessages();
            FetchProfile fp = new FetchProfile();
            fp.add(FetchProfile.Item.ENVELOPE);
            getFolder().fetch(messagesIn, fp);

            // Process each message
            for (int i = 0; i < messagesIn.length; i++)
            {
                MimeMessage message = (MimeMessage) messagesIn[i];
                if (isFetchAll() || !isSeen(message))
                {
                    try
                    {
                        new MessageProcessor(message, getAccount()).process();
                        messagesProcessed++;
                    }
                    // Catch and report an exception but don't rethrow it, 
                    // allowing subsequent messages to be processed.                    
                    catch (MessagingException mex)
                    {
                        StringBuffer logMessageBuffer =
                            new StringBuffer("Exception processing message ID: ");
                        logMessageBuffer.append(message.getMessageID());
                        logMessageBuffer.append(" - ");
                        logMessageBuffer.append(mex.toString());
                        getLogger().error(logMessageBuffer.toString());
                    }
                }
            }
        }
        catch (MessagingException mex)
        {
            getLogger().error(mex.toString());
        }
        finally
        {
            // Close the folder
            try
            {
                close();
            }
            catch (MessagingException ex)
            {
                // No-op
            }
            StringBuffer logMessageBuffer = new StringBuffer("Processed ");
            logMessageBuffer.append(messagesProcessed);
            logMessageBuffer.append(" messages in folder '");
            logMessageBuffer.append(getFolder().getName());
            logMessageBuffer.append("'");
            getLogger().info(logMessageBuffer.toString());
        }

        // Recurse through sub-folders if required
        try
        {
            if (isRecurse())
                recurse();
        }
        catch (MessagingException mex)
        {
            getLogger().error(mex.toString());
        }

        return;
    }
    
    /**
     * Method close.
     * @throws MessagingException
     */
    protected void close() throws MessagingException
    {
        if (null != getFolder() && getFolder().isOpen())
            getFolder().close(true);
    }   
    
    /**
     * Method recurse.
     * @throws MessagingException
     */
    protected void recurse() throws MessagingException
    {
        if ((getFolder().getType() & Folder.HOLDS_FOLDERS)
            == Folder.HOLDS_FOLDERS)
        {
            // folder contains subfolders...
            Folder folders[] = getFolder().list();

            for (int i = 0; i < folders.length; i++)
            {
                new FolderProcessor(folders[i], getAccount()).process();
            }

        }
    }   
    
    /**
     * Method open.
     * @throws MessagingException
     */
    protected void open() throws MessagingException
    {
        int openFlag = Folder.READ_WRITE;
        
        if (isOpenReadOnly())
            openFlag = Folder.READ_ONLY;

        getFolder().open(openFlag);                 
    }           

    /**
     * Returns the folder.
     * @return Folder
     */
    protected Folder getFolder()
    {
        return fieldFolder;
    }
    
    /**
     * Answer if <code>aMessage</code> has been SEEN.
     * @param aMessage
     * @return boolean
     * @throws MessagingException
     */
    protected boolean isSeen(MimeMessage aMessage) throws MessagingException
    {
        boolean isSeen = false;
        if (isMarkSeenPermanent().booleanValue())
            isSeen = aMessage.isSet(Flags.Flag.SEEN);
        else
            isSeen = handleMarkSeenNotPermanent(aMessage);
        return isSeen;
    }

    /**
     * Answer the result of computing markSeenPermanent.
     * @return Boolean
     */
    protected Boolean computeMarkSeenPermanent()
    {
        return new Boolean(
            getFolder().getPermanentFlags().contains(Flags.Flag.SEEN));
    }

    /**
     * <p>Handler for when the folder does not support the SEEN flag.
     * The default behaviour implemented here is to answer the value of the
     * SEEN flag anyway.</p>
     * 
     * <p>Subclasses may choose to override this method and implement their own
     *  solutions.</p>
     *
     * @param aMessage
     * @return boolean 
     * @throws MessagingException
     */
    protected boolean handleMarkSeenNotPermanent(MimeMessage aMessage)
        throws MessagingException
    {
        return aMessage.isSet(Flags.Flag.SEEN);
    }    

    /**
     * Sets the folder.
     * @param folder The folder to set
     */
    protected void setFolder(Folder folder)
    {
        fieldFolder = folder;
    }
    
    /**
     * Returns the isMarkSeenPermanent.
     * @return Boolean
     */
    protected Boolean isMarkSeenPermanent()
    {
        Boolean markSeenPermanent = null;
        if (null == (markSeenPermanent = isMarkSeenPermanentBasic()))
        {
            updateMarkSeenPermanent();
            return isMarkSeenPermanent();
        }    
        return markSeenPermanent;
    }
    
    /**
     * Returns the markSeenPermanent.
     * @return Boolean
     */
    private Boolean isMarkSeenPermanentBasic()
    {
        return fieldMarkSeenPermanent;
    }    

    /**
     * Sets the markSeenPermanent.
     * @param markSeenPermanent The isMarkSeenPermanent to set
     */
    protected void setMarkSeenPermanent(Boolean markSeenPermanent)
    {
        fieldMarkSeenPermanent = markSeenPermanent;
    }
    
    /**
     * Updates the markSeenPermanent.
     */
    protected void updateMarkSeenPermanent()
    {
        setMarkSeenPermanent(computeMarkSeenPermanent());
    }    

}
