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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.mail.MessagingException;

import org.apache.avalon.cornerstone.services.scheduler.Target;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.james.services.MailServer;
import org.apache.james.services.UsersStore;
import org.apache.james.services.UsersRepository;

/**
 * <p>Class <code>FetchMail</code> is an Avalon task that is periodically
 * triggered to fetch mail from a JavaMail Message Store.</p>
 * 
 * <p>The lifecycle of an instance of <code>FetchMail</code> is managed by
 * Avalon. The <code>configure(Configuration)</code> method is invoked to parse
 * and validate Configuration properties. The targetTriggered(String) method is
 * invoked to execute the task.</p>
 *  
 * <p>When triggered, a sorted list of Message Store Accounts to be processed is
 * built. Each Message Store Account is processed by delegating to 
 * <code>StoreProcessor</code>.</p>
 * 
 * <p>There are two kinds of Message Store Accounts, static and dynamic. Static 
 * accounts are expliciltly declared in the Configuration. Dynamic accounts are
 * built each time the task is executed, one per each user defined to James,
 * using the James user name with a configurable prefix and suffix to define
 * the host user identity and recipient identity for each Account. Dynamic
 * accounts allow <code>FetchMail</code> to fetch mail for all James users
 * without modifying the Configuration parameters or restarting the Avalon 
 * server.</p>
 * 
 * <p>To fully understand the operations supported by this task, read the Class
 * documention for each Class in the delegation chain starting with this 
 * class' delegate, <code>StoreProcessor</code>. </p>
 * 
 * <p>Creation Date: 24-May-03</p>
 * 
 */
public class FetchMail extends AbstractLogEnabled implements Configurable, Target
{
    /**
     * Creation Date: 06-Jun-03
     */
    private class ParsedDynamicAccountParameters
    {
        private String fieldUserPrefix;
        private String fieldUserSuffix;
        
        private String fieldPassword;
        
        private int fieldSequenceNumber;

        private boolean fieldIgnoreRecipientHeader;     
        private String fieldRecipientPrefix;
        private String fieldRecipientSuffix;

        /**
         * Constructor for ParsedDynamicAccountParameters.
         */
        private ParsedDynamicAccountParameters()
        {
            super();
        }

        /**
         * Constructor for ParsedDynamicAccountParameters.
         */
        public ParsedDynamicAccountParameters(
            int sequenceNumber,
            Configuration configuration)
            throws ConfigurationException
        {
            this();
            setSequenceNumber(sequenceNumber);
            setUserPrefix(configuration.getAttribute("userprefix", ""));
            setUserSuffix(configuration.getAttribute("usersuffix", ""));
            setRecipientPrefix(configuration.getAttribute("recipientprefix", ""));
            setRecipientSuffix(configuration.getAttribute("recipientsuffix", ""));
            setPassword(configuration.getAttribute("password"));
            setIgnoreRecipientHeader(
                configuration.getAttributeAsBoolean("ignorercpt-header"));
        }                       

        /**
         * Returns the recipientprefix.
         * @return String
         */
        public String getRecipientPrefix()
        {
            return fieldRecipientPrefix;
        }

        /**
         * Returns the recipientsuffix.
         * @return String
         */
        public String getRecipientSuffix()
        {
            return fieldRecipientSuffix;
        }

        /**
         * Returns the userprefix.
         * @return String
         */
        public String getUserPrefix()
        {
            return fieldUserPrefix;
        }

        /**
         * Returns the userSuffix.
         * @return String
         */
        public String getUserSuffix()
        {
            return fieldUserSuffix;
        }

        /**
         * Sets the recipientprefix.
         * @param recipientprefix The recipientprefix to set
         */
        protected void setRecipientPrefix(String recipientprefix)
        {
            fieldRecipientPrefix = recipientprefix;
        }

        /**
         * Sets the recipientsuffix.
         * @param recipientsuffix The recipientsuffix to set
         */
        protected void setRecipientSuffix(String recipientsuffix)
        {
            fieldRecipientSuffix = recipientsuffix;
        }

        /**
         * Sets the userprefix.
         * @param userprefix The userprefix to set
         */
        protected void setUserPrefix(String userprefix)
        {
            fieldUserPrefix = userprefix;
        }

        /**
         * Sets the userSuffix.
         * @param userSuffix The userSuffix to set
         */
        protected void setUserSuffix(String userSuffix)
        {
            fieldUserSuffix = userSuffix;
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
         * Sets the ignoreRecipientHeader.
         * @param ignoreRecipientHeader The ignoreRecipientHeader to set
         */
        protected void setIgnoreRecipientHeader(boolean ignoreRecipientHeader)
        {
            fieldIgnoreRecipientHeader = ignoreRecipientHeader;
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

    }
    /**
     * @see org.apache.avalon.cornerstone.services.scheduler.Target#targetTriggered(String)
     */
    private boolean fieldFetching = false;
    
    /**
     * The Configuration for this task
     */
    private ParsedConfiguration fieldConfiguration;
    
    /**
     * A List of ParsedDynamicAccountParameters, one for every <alllocal> entry
     * in the configuration.
     */
    private List fieldParsedDynamicAccountParameters;    
    
    /**
     * The Static Accounts for this task.
     * These are setup when the task is configured.
     */
    private List fieldStaticAccounts;
    
    /**
     * The Dynamic Accounts for this task.
     * These are setup each time the is run.
     */
    private List fieldDynamicAccounts;        
    
   /**
     * The MailServer service
     */
    private MailServer fieldServer;
    
   /**
     * The Local Users repository
     */
    private UsersRepository fieldLocalUsers;        

    /**
     * Constructor for POP3mail.
     */
    public FetchMail()
    {
        super();
    }

    /**
     * Method configure parses and validates the Configuration data and creates
     * a new <code>ParsedConfiguration</code>, an <code>Account</code> for each
     * configured static account and a <code>ParsedDynamicAccountParameters</code>
     * for each dynamic account.
     * 
     * @see org.apache.avalon.framework.configuration.Configurable#configure(Configuration)
     */
    public void configure(Configuration configuration)
        throws ConfigurationException
    {

        Configuration[] allAccounts = configuration.getChildren("accounts");
        if (allAccounts.length < 1)
            throw new ConfigurationException("Missing <accounts> section.");
        if (allAccounts.length > 1)
            throw new ConfigurationException("Too many <accounts> sections, there must be exactly one");
        Configuration accounts = allAccounts[0];

        // Create an Account for every configured account
        Configuration[] accountsChildren = accounts.getChildren();
        if (accountsChildren.length < 1)
            throw new ConfigurationException("Missing <account> section.");

        for (int i = 0; i < accountsChildren.length; i++)
        {
            Configuration accountsChild = accountsChildren[i];

            if (accountsChild.getName() == "alllocal")
            {
                // <allLocal> is dynamic, save the parameters for accounts to
                // be created when the task is triggered
                getParsedDynamicAccountParameters().add(
                    new ParsedDynamicAccountParameters(i, accountsChild));
                continue;
            }

            if (accountsChild.getName() == "account")
            {
                // Create an Account for the named user and
                // add it to the list of static accounts
                getStaticAccounts().add(
                    new Account(
                        i,
                        accountsChild.getAttribute("user"),
                        accountsChild.getAttribute("password"),
                        accountsChild.getAttribute("recipient"),
                        accountsChild.getAttributeAsBoolean(
                            "ignorercpt-header")));
                continue;
            }

            throw new ConfigurationException(
                "Illegal token: <"
                    + accountsChild.getName()
                    + "> in <accounts>");
        }

        setConfiguration(
            new ParsedConfiguration(
                configuration,
                getLogger(),
                getServer(),
                getLocalUsers()));
    }

    /**
     * Method target triggered fetches mail for each configured account.
     * 
     * @see org.apache.avalon.cornerstone.services.scheduler.Target#targetTriggered(String)
     */
    public void targetTriggered(String arg0)
    {
        // if we are already fetching then just return
        if (isFetching())
            return;

        // Enter Fetching State
        try
        {
            setFetching(true);
            getLogger().info(
                getConfiguration().getFetchTaskName()
                    + " fetcher starting fetches");

            // Reset and get the dynamic accounts,
            // merge with the static accounts and
            // sort the accounts so they are in the order
            // they were entered in config.xml
            resetDynamicAccounts();
            ArrayList mergedAccounts =
                new ArrayList(
                    getDynamicAccounts().size() + getStaticAccounts().size());
            mergedAccounts.addAll(getDynamicAccounts());
            mergedAccounts.addAll(getStaticAccounts());
            Collections.sort(mergedAccounts);

            // Fetch each account
            Iterator accounts = mergedAccounts.iterator();
            while (accounts.hasNext())
            {
                Account account = (Account) accounts.next();
                ParsedConfiguration configuration = getConfiguration();
                configuration.setUser(account.getUser());
                configuration.setPassword(account.getPassword());
                configuration.setRecipient(account.getRecipient());
                configuration.setIgnoreRecipientHeader(
                    account.isIgnoreRecipientHeader());
                try
                {
                    new StoreProcessor(configuration).process();
                }
                catch (MessagingException ex)
                {
                    getLogger().debug(ex.toString());
                }
            }
        }
        catch (ConfigurationException ex)
        {
            getLogger().error(ex.toString());
        }
        finally
        {
            // Ensure the dynamic accounts are thrown away
            resetDynamicAccounts();

            getLogger().info(
                getConfiguration().getFetchTaskName()
                    + " fetcher completed fetches");

            // Exit Fetching State
            setFetching(false);
        }
    }

    /**
     * Returns the fetching.
     * @return boolean
     */
    protected boolean isFetching()
    {
        return fieldFetching;
    }

    /**
     * @see org.apache.avalon.framework.service.Serviceable#service(ServiceManager)
     */
    public void service(final ServiceManager manager) throws ServiceException
    {
        try
        {
            setServer((MailServer) manager.lookup(MailServer.ROLE));
        }
        catch (ClassCastException cce)
        {
            StringBuffer errorBuffer =
                new StringBuffer(128).append("Component ").append(
                    MailServer.ROLE).append(
                    "does not implement the required interface.");
            throw new ServiceException("", errorBuffer.toString());
        }

        UsersStore usersStore =
            (UsersStore) manager.lookup("org.apache.james.services.UsersStore");
        setLocalUsers(usersStore.getRepository("LocalUsers"));
        if (getLocalUsers() == null)
            throw new ServiceException(
                "",
                "The user repository could not be found.");
    }



            


    /**
     * Sets the fetching.
     * @param fetching The fetching to set
     */
    protected void setFetching(boolean fetching)
    {
        fieldFetching = fetching;
    }

    /**
     * Returns the server.
     * @return MailServer
     */
    protected MailServer getServer()
    {
        return fieldServer;
    }

    /**
     * Returns the configuration.
     * @return ParsedConfiguration
     */
    protected ParsedConfiguration getConfiguration()
    {
        return fieldConfiguration;
    }

    /**
     * Sets the configuration.
     * @param configuration The configuration to set
     */
    protected void setConfiguration(ParsedConfiguration configuration)
    {
        fieldConfiguration = configuration;
    }

/**
 * Sets the server.
 * @param server The server to set
 */
protected void setServer(MailServer server)
{
    fieldServer = server;
}

/**
 * Returns the localUsers.
 * @return UsersRepository
 */
protected UsersRepository getLocalUsers()
{
    return fieldLocalUsers;
}

/**
 * Sets the localUsers.
 * @param localUsers The localUsers to set
 */
protected void setLocalUsers(UsersRepository localUsers)
{
    fieldLocalUsers = localUsers;
}

    /**
     * Returns the accounts. Initializes if required.
     * @return List
     */
    protected List getStaticAccounts()
    {
        List accounts = null;
        if (null == (accounts = getStaticAccountsBasic()))
        {
            updateStaticAccounts();
            return getStaticAccounts();
        }   
        return fieldStaticAccounts;
    }
    
    /**
     * Returns the staticAccounts.
     * @return List
     */
    private List getStaticAccountsBasic()
    {
        return fieldStaticAccounts;
    }   

    /**
     * Sets the accounts.
     * @param accounts The accounts to set
     */
    protected void setStaticAccounts(List accounts)
    {
        fieldStaticAccounts = accounts;
    }
    
    /**
     * Updates the staticAccounts.
     */
    protected void updateStaticAccounts()
    {
        setStaticAccounts(computeStaticAccounts());
    }
    
    /**
     * Updates the allLocalParameters.
     */
    protected void updateAllLocalParameters()
    {
        setParsedDynamicAccountParameters(computeAllLocalParameters());
    }   
    
    /**
     * Updates the dynamicAccounts.
     */
    protected void updateDynamicAccounts() throws ConfigurationException
    {
        setDynamicAccounts(computeDynamicAccounts());
    }   
    
    /**
     * Computes the staticAccounts.
     */
    protected List computeStaticAccounts()
    {
        return new ArrayList();
    }
    
    /**
     * Computes the allLocalParameters.
     */
    protected List computeAllLocalParameters()
    {
        return new ArrayList();
    }   
    
    /**
     * Computes the dynamicAccounts.
     */
    protected List computeDynamicAccounts() throws ConfigurationException
    {
        List accounts = new ArrayList(32);
        Iterator parameterIterator = getParsedDynamicAccountParameters().iterator();
        
        // Process each ParsedDynamicParameters
        while (parameterIterator.hasNext())
        {
            ParsedDynamicAccountParameters parameters =
                (ParsedDynamicAccountParameters) parameterIterator.next();
            // Create an Account for each local user
            Iterator usersIterator = getLocalUsers().list();
            while (usersIterator.hasNext())
            {
                String userName = (String) usersIterator.next();
                StringBuffer userBuffer =
                    new StringBuffer(parameters.getUserPrefix());
                userBuffer.append(userName);
                userBuffer.append(parameters.getUserSuffix());
                String user = userBuffer.toString();

                StringBuffer recipientBuffer =
                    new StringBuffer(parameters.getRecipientPrefix());
                recipientBuffer.append(userName);
                recipientBuffer.append(parameters.getRecipientSuffix());
                String recipient = recipientBuffer.toString();

                accounts.add(
                    new Account(
                        parameters.getSequenceNumber(),
                        user,
                        parameters.getPassword(),
                        recipient,
                        parameters.isIgnoreRecipientHeader()));
            }
        }
        return accounts;
    }   
    
    /**
     * Returns the dynamicAccounts. Initializes if required.
     * @return List
     */
    protected List getDynamicAccounts() throws ConfigurationException
    {
        List accounts = null;
        if (null == (accounts = getDynamicAccountsBasic()))
        {
            updateDynamicAccounts();
            return getDynamicAccounts();
        }   
        return fieldDynamicAccounts;
    }
    
    /**
     * Returns the dynamicAccounts.
     * @return List
     */
    private List getDynamicAccountsBasic()
    {
        return fieldDynamicAccounts;
    }   

    /**
     * Sets the dynamicAccounts.
     * @param dynamicAccounts The dynamicAccounts to set
     */
    protected void setDynamicAccounts(List dynamicAccounts)
    {
        fieldDynamicAccounts = dynamicAccounts;
    }
    
    /**
     * Resets the dynamicAccounts.
     */
    protected void resetDynamicAccounts()
    {
        setDynamicAccounts(null);
    }   

    /**
     * Returns the allLocalParameters.
     * @return List
     */
    protected List getParsedDynamicAccountParameters()
    {
        List accounts = null;
        if (null == (accounts = getAllLocalParametersBasic()))
        {
            updateAllLocalParameters();
            return getParsedDynamicAccountParameters();
        }   
        return fieldParsedDynamicAccountParameters;
    }
    
    /**
     * Returns the allLocalParameters.
     * @return List
     */
    private List getAllLocalParametersBasic()
    {
        return fieldParsedDynamicAccountParameters;
    }   

    /**
     * Sets the allLocalParameters.
     * @param allLocalParameters The allLocalParameters to set
     */
    protected void setParsedDynamicAccountParameters(List allLocalParameters)
    {
        fieldParsedDynamicAccountParameters = allLocalParameters;
    }

}
