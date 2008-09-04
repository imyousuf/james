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



package org.apache.james.transport.mailets;

import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.james.Constants;
import org.apache.james.api.user.UsersRepository;
import org.apache.james.api.user.UsersStore;
import org.apache.james.transport.mailets.listservcommands.ErrorCommand;
import org.apache.james.transport.mailets.listservcommands.IListServCommand;
import org.apache.james.util.XMLResources;
import org.apache.mailet.GenericMailet;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;

import javax.mail.MessagingException;
import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/**
 * CommandListservManager is the default implementation of {@link ICommandListservManager}.
 * It loads all the configured {@link IListServCommand}s and delegates to them at runtime.
 * <br />
 *
 * It isn't responsible for procesing messages sent to the main mailing list, but is responsible for
 * individual commands sent by users, such as: info, subscribe, etc...
 * <br />
 *
 * Requests sent to the CommandListservManager take the form of:
 * <pre>
 * &lt;listName&gt;-&lt;commandName&gt;@domain
 * </pre>
 *
 * If the command isn't recognized an error will be sent using {@link #onError}.
 * <br />
 * <br />
 *
 * The configuration for this mailet sould be in the 'root' processor block.
 * <pre>
 * &lt;mailet match="CommandListservMatcher=announce@localhost" class="CommandListservManager"&gt;
 *  &lt;listName&gt;announce&lt;/listName&gt;
 *  &lt;displayName&gt;Announce mailing list&lt;/displayName&gt;
 *  &lt;listOwner&gt;owner@localhost&lt;/listOwner&gt;
 *  &lt;repositoryName&gt;list-announce&lt;/repositoryName&gt;
 *  &lt;listDomain&gt;localhost&lt;/listDomain&gt;
 *
 *  &lt;commandpackages&gt;
 *     &lt;commandpackage&gt;org.apache.james.transport.mailets.listservcommands&lt;/commandpackage&gt;
 *  &lt;/commandpackages&gt;
 *
 *  &lt;commands&gt;
 *     &lt;command name="subscribe" class="Subscribe"/&gt;
 *     &lt;command name="subscribe-confirm" class="SubscribeConfirm"/&gt;
 *     &lt;command name="unsubscribe" class="UnSubscribe"/&gt;
 *     &lt;command name="unsubscribe-confirm" class="UnSubscribeConfirm"/&gt;
 *     &lt;command name="error" class="ErrorCommand"/&gt;
 *     &lt;command name="owner" class="Owner"/&gt;
 *     &lt;command name="info" class="Info"/&gt;
 *  &lt;/commands&gt;
 * &lt;/mailet&gt;
 * </pre>
 *
 * <br />
 * <br />
 * Todo: refine the command matching so we can have more sophistciated commands such as:
 * <pre>
 * &lt;listName&gt;-&lt;commandName&gt;-&lt;optCommandParam&gt;@domain
 * </pre>
 *
 * @version CVS $Revision$ $Date$
 * @since 2.2.0
 */
public class CommandListservManager extends GenericMailet implements ICommandListservManager {

    protected Map commandMap = new HashMap();
    protected List commandPackages = new ArrayList();
    protected UsersRepository usersRepository;
    protected String listName;
    protected String displayName;
    protected String listOwner;
    protected String listDomain;
    protected XMLResources xmlResources;

    /**
     * Get the name of this list specified by the config param: 'listName'.
     * <br />
     * eg: <pre>&lt;listName&gt;announce&lt;/listName&gt;</pre>
     *
     * @param displayFormat is whether you want a display version of this or not
     * @return the official display name of this list
     */
    public String getListName(boolean displayFormat) {
        return displayFormat ? displayName : listName;
    }

    /**
     * Gets the owner of this list specified by the config param: 'listOwner'.
     * <br />
     * eg: <pre>&lt;listOwner&gt;owner@localhost&lt;/listOwner&gt;</pre>
     *
     * @return this is an address like listOwner@localhost
     */
    public String getListOwner() {
        return listOwner;
    }

    /**
     * Get the domain of the list specified by the config param: 'listDomain'.
     * <br />
     * eg: <pre>&lt;listDomain&gt;localhost&lt;/listDomain&gt;</pre>
     *
     * @return a string like localhost
     */
    public String getListDomain() {
        return listDomain;
    }

    /**
     * Get a specific command specified by the 'commands' configuration block.
     * For instance:
     * <pre>
     * &lt;commands&gt;
     *  &lt;command name="subscribe" class="Subscribe"/&gt;
     *  &lt;command name="subscribe-confirm" class="SubscribeConfirm"/&gt;
     *  &lt;command name="unsubscribe" class="UnSubscribe"/&gt;
     *  &lt;command name="unsubscribe-confirm" class="UnSubscribeConfirm"/&gt;
     *  &lt;command name="error" class="ErrorCommand"/&gt;
     *  &lt;command name="owner" class="Owner"/&gt;
     *  &lt;command name="info" class="Info"/&gt;
     * &lt;/commands&gt;
     * </pre>
     * @param name case in-sensitive
     * @return a {@link IListServCommand} if found, null otherwise
     */
    public IListServCommand getCommand(String name) {
        return (IListServCommand) commandMap.get(name.toLowerCase(Locale.US));
    }

    /**
     * Get all the available commands
     * @return a map of {@link IListServCommand}
     * @see #getCommand
     */
    public Map getCommands() {
        return commandMap;
    }

    /**
     * Get the current user repository for this list serv
     * @return an instance of {@link UsersRepository} that is used for the member list of the list serv
     */
    public UsersRepository getUsersRepository() {
        return usersRepository;
    }

    /**
     * An error occurred, send some sort of message
     * @param subject the subject of the message to send
     * @param mail
     * @param errorMessage
     */
    public void onError(Mail mail, String subject, String errorMessage) throws MessagingException {
        ErrorCommand errorCommand = (ErrorCommand) getCommand("error");
        errorCommand.onError(mail, subject, errorMessage);
    }

    /**
     * @return the configuration file for the xml resources
     */
    public String getResourcesFile() {
        return getInitParameter("resources");
    }

    /**
     * Use this to get standard properties for future calls to {@link org.apache.james.util.XMLResources}
     * @return properties with the "LIST_NAME" and the "DOMAIN_NAME" properties
     */
    public Properties getStandardProperties() {
        Properties standardProperties = new Properties();
        standardProperties.put("LIST_NAME", getListName(false));
        standardProperties.put("DISPLAY_NAME", getListName(true));
        standardProperties.put("DOMAIN_NAME", getListDomain());
        return standardProperties;
    }

    /**
     * Initializes an array of resources
     * @param names such as 'header, footer' etc...
     * @return an initialized array of XMLResources
     * @throws ConfigurationException
     */
    public XMLResources[] initXMLResources(String[] names) throws ConfigurationException {
        try {
            File xmlFile = new File(getResourcesFile());

            Properties props = getStandardProperties();
            String listName = props.getProperty("LIST_NAME");

            XMLResources[] xmlResources = new XMLResources[names.length];
            for (int index = 0; index < names.length; index++) {
                xmlResources[index] = new XMLResources();
                xmlResources[index].init(xmlFile, names[index], listName, props);
            }
            return xmlResources;
        } catch (Exception e) {
            log(e.getMessage(), e);
            throw new ConfigurationException("Can't initialize:", e);
        }
    }

    public void init() throws MessagingException {

        try {
            //Well, i want a more complex configuration structure
            //of my mailet, so i have to cheat... and cheat i will...
            Configuration configuration = (Configuration) getField(getMailetConfig(), "configuration");

            //get name
            listName = configuration.getChild("listName").getValue();
            displayName = configuration.getChild("displayName").getValue();
            listOwner = configuration.getChild("listOwner").getValue();
            listDomain = configuration.getChild("listDomain").getValue();

            //initialize resources
            initializeResources();

            //get users store
            initUsersRepository();

            //get command packages
            loadCommandPackages(configuration);

            //load commands
            loadCommands(configuration);

            //register w/context
            getMailetContext().setAttribute(ICommandListservManager.ID + listName, this);
        } catch (Exception e) {
            throw new MessagingException(e.getMessage(), e);
        }
    }

    /**
     * Based on the to address get a valid or command or null
     * @param mailAddress
     * @return IListServCommand or null
     */
    public IListServCommand getCommandTarget(MailAddress mailAddress) {
        String commandName = getCommandName(mailAddress);
        return getCommand(commandName);
    }

    /**
     * <p>Called by the mailet container to allow the mailet to process a
     * message.</p>
     *
     * <p>This method is declared abstract so subclasses must override it.</p>
     *
     * @param mail - the Mail object that contains the MimeMessage and
     *          routing information
     * @throws MessagingException - if an exception occurs that interferes with the mailet's normal operation
     *          occurred
     */
    public void service(Mail mail) throws MessagingException {
        if (mail.getRecipients().size() != 1) {
            getMailetContext().bounce(mail, "You can only send one command at a time to this listserv manager.");
            return;
        }
        MailAddress mailAddress = (MailAddress) mail.getRecipients().iterator().next();
        IListServCommand command = getCommandTarget(mailAddress);

        if (command == null) {
            //don't recognize the command
            Properties props = getStandardProperties();
            props.setProperty("COMMAND", getCommandName(mailAddress));
            onError(mail, "unknown command", xmlResources.getString("command.not.understood", props));
        } else {
            command.onCommand(mail);
        }

        // onError or onCommand would have done the job, so regardless
        // of which get rid of this e-mail.  This is something that we
        // should review, and decide if there is any reason to allow a
        // passthrough.
        mail.setState(Mail.GHOST);
    }

    /**
     * Get the name of the command
     * @param mailAddress
     * @return the name of the command
     */
    protected String getCommandName(MailAddress mailAddress) {
        String user = mailAddress.getUser();
        int index = user.indexOf('-', listName.length());
        String commandName = user.substring(++index);
        return commandName;
    }

    /**
     * initialize the resources
     * @throws Exception
     */
    protected void initializeResources() throws Exception {
        xmlResources = initXMLResources(new String[]{"List Manager"})[0];
    }

    /**
     * Fetch the repository of users
     */
    protected void initUsersRepository() {
        ServiceManager compMgr = (ServiceManager) getMailetContext().getAttribute(Constants.AVALON_COMPONENT_MANAGER);
        try {
            UsersStore usersStore = (UsersStore) compMgr.lookup(UsersStore.ROLE);
            String repName = getInitParameter("repositoryName");

            usersRepository = usersStore.getRepository(repName);
        } catch (Exception e) {
            log("Failed to retrieve Store component:" + e.getMessage());
        }
    }

    /**
     * Load an initialize all of the available commands
     * @param configuration
     * @throws ConfigurationException
     */
    protected void loadCommands(Configuration configuration) throws Exception {
        final Configuration commandConfigurations = configuration.getChild("commands");
        final Configuration[] commandConfs = commandConfigurations.getChildren("command");
        for (int index = 0; index < commandConfs.length; index++) {
            Configuration commandConf = commandConfs[index];
            String commandName = commandConf.getAttribute("name").toLowerCase();
            String className = commandConf.getAttribute("class");
            loadCommand(commandName, className, commandConf);
        }
    }

    /**
     * Loads and initializes a single command
     *
     * @param commandName
     * @param className
     * @param configuration
     * @throws ConfigurationException
     */
    protected void loadCommand(String commandName,
                               String className,
                               Configuration configuration)
            throws ConfigurationException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        ClassLoader theClassLoader = Thread.currentThread().getContextClassLoader();
        for (Iterator it = commandPackages.iterator(); it.hasNext();) {
            String packageName = (String) it.next();

            IListServCommand listServCommand = null;
            try {
                listServCommand = (IListServCommand) theClassLoader.loadClass(packageName + className).newInstance();
            } catch (Exception e) {
                //ignore
                continue;
            }
            listServCommand.init(this, configuration);
            commandMap.put(commandName, listServCommand);
            return;
        }

        throw new ConfigurationException("Unable to load listservcommand: " + commandName);
    }

    /**
     * loads all of the packages for the commands
     *
     * @param configuration
     * @throws ConfigurationException
     */
    protected void loadCommandPackages(Configuration configuration) throws ConfigurationException {
        commandPackages.add("");
        final Configuration packageConfiguration = configuration.getChild("commandpackages");
        final Configuration[] pkgConfs = packageConfiguration.getChildren("commandpackage");
        for (int index = 0; index < pkgConfs.length; index++) {
            Configuration conf = pkgConfs[index];
            String packageName = conf.getValue().trim();
            if (!packageName.endsWith(".")) {
                packageName += ".";
            }
            commandPackages.add(packageName);
        }
    }

    /**
     * Retrieves a data field, potentially defined by a super class.
     * @return null if not found, the object otherwise
     */
    protected static Object getField(Object instance, String name) throws IllegalAccessException {
        Class clazz = instance.getClass();
        Field[] fields;
        while (clazz != null) {
            fields = clazz.getDeclaredFields();
            for (int index = 0; index < fields.length; index++) {
                Field field = fields[index];
                if (field.getName().equals(name)) {
                    field.setAccessible(true);
                    return field.get(instance);
                }
            }
            clazz = clazz.getSuperclass();
        }

        return null;
    }
}
