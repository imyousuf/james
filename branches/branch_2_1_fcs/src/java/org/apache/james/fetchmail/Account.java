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

import java.util.ArrayList;
import java.util.List;
import javax.mail.internet.ParseException;

import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.mailet.MailAddress;

/**
 * <p>Class <code>Account</code> encapsulates the account details required to
 * fetch mail from a message store.</p>
 * 
 * <p>Instances are <code>Comparable</code> based on their sequence number.</p>
 * 
 * <p>Creation Date: 05-Jun-03</p>
 */
class Account implements Comparable
{
    /**
     * The user password for this account
     */
    private String fieldPassword;     

    /**
     * The user to send the fetched mail to
     */
    private MailAddress fieldRecipient;  

    /**
     * The user name for this account
     */
    private String fieldUser;

    /**
     * The ParsedConfiguration
     */    
    private ParsedConfiguration fieldParsedConfiguration;
    
    /**
     * List of MessageIDs for which processing has been deferred
     * because the intended recipient could not be found.
     */
    private List fieldDeferredRecipientNotFoundMessageIDs;    
    
    /**
     * The sequence number for this account
     */
    private int fieldSequenceNumber;
    
    /**
     * Ignore the recipient deduced from the header and use 'fieldRecipient'
     */
    private boolean fieldIgnoreRecipientHeader;     

    /**
     * Constructor for Account.
     */
    private Account()
    {
        super();
    }
    
    /**
     * Constructor for Account.
     * 
     * @param sequenceNumber
     * @param parsedConfiguration
     * @param user
     * @param password
     * @param recipient
     * @param ignoreRecipientHeader
     * @throws ConfigurationException
     */
    
    public Account(
        int sequenceNumber,
        ParsedConfiguration parsedConfiguration,
        String user,
        String password,
        String recipient,
        boolean ignoreRecipientHeader)
        throws ConfigurationException
    {
        this();
        setSequenceNumber(sequenceNumber);
        setParsedConfiguration(parsedConfiguration);
        setUser(user);
        setPassword(password);
        setRecipient(recipient);
        setIgnoreRecipientHeader(ignoreRecipientHeader);
    }   

    /**
     * Returns the password.
     * @return String
     */
    public String getPassword()
    {
        return fieldPassword;
    }

    /**
     * Returns the recipient.
     * @return MailAddress
     */
    public MailAddress getRecipient()
    {
        return fieldRecipient;
    }

    /**
     * Returns the user.
     * @return String
     */
    public String getUser()
    {
        return fieldUser;
    }

    /**
     * Sets the password.
     * @param password The password to set
     */
    protected void setPassword(String password)
    {
        fieldPassword = password;
    }

    /**
     * Sets the recipient.
     * @param recipient The recipient to set
     */
    protected void setRecipient(MailAddress recipient)
    {
        fieldRecipient = recipient;
    }
    
    /**
     * Sets the recipient.
     * @param recipient The recipient to set
     */
    protected void setRecipient(String recipient) throws ConfigurationException
    {
        if (null == recipient)
        {
            fieldRecipient = null;
            return;
        }
            
        try
        {
            setRecipient(new MailAddress(recipient));
        }
        catch (ParseException pe)
        {
            throw new ConfigurationException(
                "Invalid recipient address specified: " + recipient);
        }
    }   


    

    /**
     * Sets the user.
     * @param user The user to set
     */
    protected void setUser(String user)
    {
        fieldUser = user;
    }

    /**
     * Sets the ignoreRecipientHeader.
     * @param ignoreRecipientHeader The ignoreRecipientHeader to set
     */
    protected void setIgnoreRecipientHeader(boolean ignoreRecipientHeader)
    {
        fieldIgnoreRecipientHeader = ignoreRecipientHeader;
    }

    /**
     * Returns the ignoreRecipientHeader.
     * @return boolean
     */
    public boolean isIgnoreRecipientHeader()
    {
        return fieldIgnoreRecipientHeader;
    }

    /**
     * Returns the sequenceNumber.
     * @return int
     */
    public int getSequenceNumber()
    {
        return fieldSequenceNumber;
    }

    /**
     * Sets the sequenceNumber.
     * @param sequenceNumber The sequenceNumber to set
     */
    protected void setSequenceNumber(int sequenceNumber)
    {
        fieldSequenceNumber = sequenceNumber;
    }

    /**
     * Compares this object with the specified object for order.  Returns a
     * negative integer, zero, or a positive integer if this object is less
     * than, equal to, or greater than the specified object.
     * 
     * @see java.lang.Comparable#compareTo(Object)
     */
    public int compareTo(Object o)
    {
        return getSequenceNumber() - ((Account) o).getSequenceNumber();
    }

    /**
     * Returns the deferredRecipientNotFoundMessageIDs. lazily initialised.
     * @return List
     */
    public List getDeferredRecipientNotFoundMessageIDs()
    {
        List messageIDs = null;
        if (null
            == (messageIDs = getDeferredRecipientNotFoundMessageIDsBasic()))
        {
            updateDeferredRecipientNotFoundMessageIDs();
            return getDeferredRecipientNotFoundMessageIDs();
        }
        return messageIDs;
    }

    /**
     * Returns the deferredRecipientNotFoundMessageIDs.
     * @return List
     */
    private List getDeferredRecipientNotFoundMessageIDsBasic()
    {
        return fieldDeferredRecipientNotFoundMessageIDs;
    }
    
    /**
     * Returns a new List of deferredRecipientNotFoundMessageIDs.
     * @return List
     */
    protected List computeDeferredRecipientNotFoundMessageIDs()
    {
        return new ArrayList(16);
    }    
    
    /**
     * Updates the deferredRecipientNotFoundMessageIDs.
     */
    protected void updateDeferredRecipientNotFoundMessageIDs()
    {
        setDeferredRecipientNotFoundMessageIDs(computeDeferredRecipientNotFoundMessageIDs());
    }    

    /**
     * Sets the defferedRecipientNotFoundMessageIDs.
     * @param defferedRecipientNotFoundMessageIDs The defferedRecipientNotFoundMessageIDs to set
     */
    protected void setDeferredRecipientNotFoundMessageIDs(List defferedRecipientNotFoundMessageIDs)
    {
        fieldDeferredRecipientNotFoundMessageIDs =
            defferedRecipientNotFoundMessageIDs;
    }

    /**
     * Returns the parsedConfiguration.
     * @return ParsedConfiguration
     */
    public ParsedConfiguration getParsedConfiguration()
    {
        return fieldParsedConfiguration;
    }

    /**
     * Sets the parsedConfiguration.
     * @param parsedConfiguration The parsedConfiguration to set
     */
    protected void setParsedConfiguration(ParsedConfiguration parsedConfiguration)
    {
        fieldParsedConfiguration = parsedConfiguration;
    }

}
