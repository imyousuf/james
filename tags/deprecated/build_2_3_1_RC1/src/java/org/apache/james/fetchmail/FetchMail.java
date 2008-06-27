/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.fetchmail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.Session;

import org.apache.avalon.cornerstone.services.scheduler.Target;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.james.services.MailServer;
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
public class FetchMail extends AbstractLogEnabled implements Configurable, Target, Serviceable
{
    /**
     * Key fields for DynamicAccounts.
     */
    private class DynamicAccountKey
    {
        /**
         * The base user name without prfix or suffix
         */
        private String fieldUserName;
        
        /**
         * The sequence number of the parameters used to construct the Account
         */
        private int fieldSequenceNumber;                

        /**
         * Constructor for DynamicAccountKey.
         */
        private DynamicAccountKey()
        {
            super();
        }
        
        /**
         * Constructor for DynamicAccountKey.
         */
        public DynamicAccountKey(String userName, int sequenceNumber)
        {
            this();
            setUserName(userName);
            setSequenceNumber(sequenceNumber);
        }        

        /**
         * @see java.lang.Object#equals(Object)
         */
        public boolean equals(Object obj)
        {
            if (null == obj)
                return false;
            if (!(obj.getClass() == getClass()))
                return false;
            return (
                getUserName().equals(((DynamicAccountKey) obj).getUserName())
                    && getSequenceNumber()
                        == ((DynamicAccountKey) obj).getSequenceNumber());
        }

        /**
         * @see java.lang.Object#hashCode()
         */
        public int hashCode()
        {
            return getUserName().hashCode() ^ getSequenceNumber();
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
         * Returns the userName.
         * @return String
         */
        public String getUserName()
        {
            return fieldUserName;
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
         * Sets the userName.
         * @param userName The userName to set
         */
        protected void setUserName(String userName)
        {
            fieldUserName = userName;
        }

    }
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
        private String customRecipientHeader;

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
            setCustomRecipientHeader(configuration.getAttribute("customrcpt-header", ""));
        }                       

        /**
         * Returns the custom recipient header.
         * @return String
         */
        public String getCustomRecipientHeader() {
            return this.customRecipientHeader;
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
         * Sets the custom recipient header.
         * @param customRecipientHeader The header to be used
         */
        public void setCustomRecipientHeader(String customRecipientHeader) {
            this.customRecipientHeader = customRecipientHeader;
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
     * The JavaMail Session for this fetch task.
     */ 

    private Session fieldSession;
    
    /**
     * The Dynamic Accounts for this task.
     * These are setup each time the fetchtask is run.
     */
    private Map fieldDynamicAccounts;        
    
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
        // Set any Session parameters passed in the Configuration
        setSessionParameters(configuration);

        // Create the ParsedConfiguration used in the delegation chain
        ParsedConfiguration parsedConfiguration =
            new ParsedConfiguration(
                configuration,
                getLogger(),
                getServer(),
                getLocalUsers());
        setConfiguration(parsedConfiguration);

        // Setup the Accounts
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

            if ("alllocal".equals(accountsChild.getName()))
            {
                // <allLocal> is dynamic, save the parameters for accounts to
                // be created when the task is triggered
                getParsedDynamicAccountParameters().add(
                    new ParsedDynamicAccountParameters(i, accountsChild));
                continue;
            }

            if ("account".equals(accountsChild.getName()))
            {
                // Create an Account for the named user and
                // add it to the list of static accounts
                getStaticAccounts().add(
                    new Account(
                        i,
                        parsedConfiguration,
                        accountsChild.getAttribute("user"),
                        accountsChild.getAttribute("password"),
                        accountsChild.getAttribute("recipient"),
                        accountsChild.getAttributeAsBoolean(
                            "ignorercpt-header"),
                        accountsChild.getAttribute("customrcpt-header",""),
                        getSession()));
                continue;
            }

            throw new ConfigurationException(
                "Illegal token: <"
                    + accountsChild.getName()
                    + "> in <accounts>");
        }
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
        {
            getLogger().info(
                "Triggered fetch cancelled. A fetch is already in progress.");
            return;
        }

        // Enter Fetching State
        try
        {
            setFetching(true);
            getLogger().info("Fetcher starting fetches");

            // if debugging, list the JavaMail property key/value pairs
            // for this Session
            if (getLogger().isDebugEnabled())
            {
                getLogger().debug("Session properties:");
                Properties properties = getSession().getProperties();
                Enumeration e = properties.keys();
                while (e.hasMoreElements())
                {
                    String key = (String) e.nextElement();
                    String val = (String) properties.get(key);
                    if (val.length() > 40)
                    {
                        val = val.substring(0, 37) + "...";
                    }
                    getLogger().debug(key + "=" + val);

                }
            }

            // Update the dynamic accounts,
            // merge with the static accounts and
            // sort the accounts so they are in the order
            // they were entered in config.xml
            updateDynamicAccounts();
            ArrayList mergedAccounts =
                new ArrayList(
                    getDynamicAccounts().size() + getStaticAccounts().size());
            mergedAccounts.addAll(getDynamicAccounts().values());
            mergedAccounts.addAll(getStaticAccounts());
            Collections.sort(mergedAccounts);

            StringBuffer logMessage = new StringBuffer(64);
            logMessage.append("Processing ");
            logMessage.append(getStaticAccounts().size());
            logMessage.append(" static accounts and ");
            logMessage.append(getDynamicAccounts().size());
            logMessage.append(" dynamic accounts.");
            getLogger().info(logMessage.toString());

            // Fetch each account
            Iterator accounts = mergedAccounts.iterator();
            while (accounts.hasNext())
            {
                try
                {
                    new StoreProcessor((Account) accounts.next()).process();
                }
                catch (MessagingException ex)
                {
                    getLogger().error(
                        "A MessagingException has terminated processing of this Account",
                        ex);
                }
            }
        }
        catch (Exception ex)
        {
            getLogger().error("An Exception has terminated this fetch.", ex);
        }
        finally
        {
            getLogger().info("Fetcher completed fetches");

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

        UsersRepository usersRepository =
            (UsersRepository) manager.lookup(UsersRepository.ROLE);
        setLocalUsers(usersRepository);
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
        if (null == getStaticAccountsBasic())
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
     * Updates the ParsedDynamicAccountParameters.
     */
    protected void updateParsedDynamicAccountParameters()
    {
        setParsedDynamicAccountParameters(computeParsedDynamicAccountParameters());
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
     * Computes the ParsedDynamicAccountParameters.
     */
    protected List computeParsedDynamicAccountParameters()
    {
        return new ArrayList();
    }   
    
    /**
     * Computes the dynamicAccounts.
     */
    protected Map computeDynamicAccounts() throws ConfigurationException
    {
        Map newAccounts =
            new HashMap(
                getLocalUsers().countUsers()
                    * getParsedDynamicAccountParameters().size());
        Map oldAccounts = getDynamicAccountsBasic();
        if (null == oldAccounts)
            oldAccounts = new HashMap(0);

        Iterator parameterIterator =
            getParsedDynamicAccountParameters().iterator();

        // Process each ParsedDynamicParameters
        while (parameterIterator.hasNext())
        {
            Map accounts =
                computeDynamicAccounts(
                    oldAccounts,
                    (ParsedDynamicAccountParameters) parameterIterator.next());
            // Remove accounts from oldAccounts.
            // This avoids an average 2*N increase in heapspace used as the 
            // newAccounts are created. 
            Iterator oldAccountsIterator = oldAccounts.keySet().iterator();
            while (oldAccountsIterator.hasNext())
            {
                if (accounts.containsKey(oldAccountsIterator.next()))
                    oldAccountsIterator.remove();
            }
            // Add this parameter's accounts to newAccounts
            newAccounts.putAll(accounts);
        }
        return newAccounts;
    }
    
    /**
     * Returns the dynamicAccounts. Initializes if required.
     * @return Map
     */
    protected Map getDynamicAccounts() throws ConfigurationException
    {
        if (null == getDynamicAccountsBasic())
        {
            updateDynamicAccounts();
            return getDynamicAccounts();
        }   
        return fieldDynamicAccounts;
    }
    
    /**
     * Returns the dynamicAccounts.
     * @return Map
     */
    private Map getDynamicAccountsBasic()
    {
        return fieldDynamicAccounts;
    }   

    /**
     * Sets the dynamicAccounts.
     * @param dynamicAccounts The dynamicAccounts to set
     */
    protected void setDynamicAccounts(Map dynamicAccounts)
    {
        fieldDynamicAccounts = dynamicAccounts;
    }
    
    /**
     * Compute the dynamicAccounts for the passed parameters.
     * Accounts for existing users are copied and accounts for new users are 
     * created.
     * @param oldAccounts
     * @param parameters
     * @return Map - The current Accounts
     * @throws ConfigurationException
     */
    protected Map computeDynamicAccounts(
        Map oldAccounts,
        ParsedDynamicAccountParameters parameters)
        throws ConfigurationException
    {
        Map accounts = new HashMap(getLocalUsers().countUsers());
        Iterator usersIterator = getLocalUsers().list();
        while (usersIterator.hasNext())
        {
            String userName = (String) usersIterator.next();
            DynamicAccountKey key =
                new DynamicAccountKey(userName, parameters.getSequenceNumber());
            Account account = (Account) oldAccounts.get(key);
            if (null == account)
            {
                // Create a new DynamicAccount
                account =
                    new DynamicAccount(
                        parameters.getSequenceNumber(),
                        getConfiguration(),
                        userName,
                        parameters.getUserPrefix(),
                        parameters.getUserSuffix(),
                        parameters.getPassword(),
                        parameters.getRecipientPrefix(),
                        parameters.getRecipientSuffix(),
                        parameters.isIgnoreRecipientHeader(),
                        parameters.getCustomRecipientHeader(),
                        getSession());
            }
            accounts.put(key, account);
        }
        return accounts;
    }
    
    /**
     * Resets the dynamicAccounts.
     */
    protected void resetDynamicAccounts()
    {
        setDynamicAccounts(null);
    }   

    /**
     * Returns the ParsedDynamicAccountParameters.
     * @return List
     */
    protected List getParsedDynamicAccountParameters()
    {
        if (null == getParsedDynamicAccountParametersBasic())
        {
            updateParsedDynamicAccountParameters();
            return getParsedDynamicAccountParameters();
        }   
        return fieldParsedDynamicAccountParameters;
    }
    
    /**
     * Returns the ParsedDynamicAccountParameters.
     * @return List
     */
    private List getParsedDynamicAccountParametersBasic()
    {
        return fieldParsedDynamicAccountParameters;
    }   

    /**
     * Sets the ParsedDynamicAccountParameters.
     * @param ParsedDynamicAccountParameters The ParsedDynamicAccountParametersto set
     */
    protected void setParsedDynamicAccountParameters(List parsedDynamicAccountParameters)
    {
        fieldParsedDynamicAccountParameters = parsedDynamicAccountParameters;
    }

    /**
     * Returns the session, lazily initialized if required.
     * @return Session
     */
    protected Session getSession()
    {
        Session session = null;
        if (null == (session = getSessionBasic()))
        {
            updateSession();
            return getSession();
        }    
        return session;
    }
    
    /**
     * Returns the session.
     * @return Session
     */
    private Session getSessionBasic()
    {
        return fieldSession;
    }    

    /**
     * Answers a new Session.
     * @return Session
     */
    protected Session computeSession()
    {
        return Session.getInstance(System.getProperties());
    }
    
    /**
     * Updates the current Session.
     */
    protected void updateSession()
    {
        setSession(computeSession());
    }    

    /**
     * Sets the session.
     * @param session The session to set
     */
    protected void setSession(Session session)
    {
        fieldSession = session;
    }
    
    
    /**
     * Propogate any Session parameters in the configuration to the Session.
     * @param configuration The configuration containing the parameters
     * @throws ConfigurationException
     */
    protected void setSessionParameters(Configuration configuration)
        throws ConfigurationException
    {
        Configuration javaMailProperties =
            configuration.getChild("javaMailProperties", false);
        if (null != javaMailProperties)
        {
            Properties properties = getSession().getProperties();
            Configuration[] allProperties =
                javaMailProperties.getChildren("property");
            for (int i = 0; i < allProperties.length; i++)
            {
                properties.setProperty(
                    allProperties[i].getAttribute("name"),
                    allProperties[i].getAttribute("value"));
                if (getLogger().isDebugEnabled())
                {
                    StringBuffer messageBuffer =
                        new StringBuffer("Set property name: ");
                    messageBuffer.append(allProperties[i].getAttribute("name"));
                    messageBuffer.append(" to: ");
                    messageBuffer.append(
                        allProperties[i].getAttribute("value"));
                    getLogger().debug(messageBuffer.toString());
                }
            }
        }
    }    

}
