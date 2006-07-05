/***********************************************************************
 * Copyright (c) 1999-2006 The Apache Software Foundation.             *
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

package org.apache.james.smtpserver;

import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.ContextException;
import org.apache.avalon.framework.context.Contextualizable;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.james.smtpserver.core.BaseCmdHandler;
import org.apache.james.smtpserver.core.BaseFilterCmdHandler;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/**
  * The SMTPHandlerChain is per service object providing access
  * ConnectHandlers, Commandhandlers and message handlers
  */
public class SMTPHandlerChain extends AbstractLogEnabled implements Configurable, Serviceable, Contextualizable, Initializable {

    private HashMap commandHandlerMap = new HashMap();
    private ArrayList messageHandlers = new ArrayList();
    private ArrayList connectHandlers = new ArrayList();

    private final AbstractCommandHandler unknownHandler = new UnknownCmdHandler();
    private ServiceManager serviceManager;
    private Context context;
    
    private final static String[] mandatoryCommands = { "MAIL" , "RCPT", "DATA"};

    public void service(ServiceManager arg0) throws ServiceException {
        serviceManager = arg0;
    }
    
    public void contextualize(Context arg0) throws ContextException {
        context = arg0;
    }



    /**
     * loads the various handlers from the configuration
     * 
     * @param configuration
     *            configuration under handlerchain node
     */
    public void configure(Configuration configuration)
            throws ConfigurationException {
        addToMap(UnknownCmdHandler.UNKNOWN_COMMAND, unknownHandler);
        if (configuration == null
                || configuration.getChildren("handler") == null
                || configuration.getChildren("handler").length == 0) {
            configuration = new DefaultConfiguration("handlerchain");
            Properties cmds = new Properties();
            cmds.setProperty("Default BaseFilterHandler",
                    BaseFilterCmdHandler.class.getName());
            cmds.setProperty("Default BaseHandler", BaseCmdHandler.class
                    .getName());
            cmds.setProperty("Default SendMailHandler", SendMailHandler.class
                    .getName());
            Enumeration e = cmds.keys();
            while (e.hasMoreElements()) {
                String cmdName = (String) e.nextElement();
                String className = cmds.getProperty(cmdName);
                ((DefaultConfiguration) configuration).addChild(addHandler(
                        cmdName, className));
            }
        }
        if (configuration != null) {
            Configuration[] children = configuration.getChildren("handler");
            ClassLoader classLoader = getClass().getClassLoader();

            // load the BaseFilterCmdHandler
            loadClass(classLoader, BaseFilterCmdHandler.class.getName(),
                    addHandler(null, BaseFilterCmdHandler.class.getName()));

            // load the configured handlers
            if (children != null) {
                for (int i = 0; i < children.length; i++) {
                    String className = children[i].getAttribute("class");
                    if (className != null) {

                        // ignore base handlers.
                        if (!className.equals(BaseFilterCmdHandler.class
                                .getName())
                                && !className.equals(BaseCmdHandler.class
                                        .getName())
                                && !className.equals(SendMailHandler.class
                                        .getName())) {

                            // load the handler
                            loadClass(classLoader, className, children[i]);
                        }
                    }
                }

                // load the BaseCmdHandler and SendMailHandler
                loadClass(classLoader, BaseCmdHandler.class.getName(),
                        addHandler(null, BaseCmdHandler.class.getName()));
                loadClass(classLoader, SendMailHandler.class.getName(),
                        addHandler(null, SendMailHandler.class.getName()));
            }
        }

        // the size must be greater than 1 because we added UnknownCmdHandler to
        // the map

        if (commandHandlerMap.size() < 2) {
            if (getLogger().isErrorEnabled()) {
                getLogger().error("No commandhandlers configured");
            }
            throw new ConfigurationException("No commandhandlers configured");
        } else {
            boolean found = true;
            for (int i = 0; i < mandatoryCommands.length; i++) {
                if (!commandHandlerMap.containsKey(mandatoryCommands[i])) {
                    if (getLogger().isErrorEnabled()) {
                        getLogger().error(
                                "No commandhandlers configured for the command:"
                                        + mandatoryCommands[i]);
                    }
                    found = false;
                    break;
                }
            }

            if (!found) {
                throw new ConfigurationException(
                        "No commandhandlers configured for mandatory commands");
            }

            if (messageHandlers.size() == 0) {
                if (getLogger().isErrorEnabled()) {
                    getLogger()
                            .error(
                                    "No messageHandler configured. Check that SendMailHandler is configured in the SMTPHandlerChain");
                }
                throw new ConfigurationException("No messageHandler configured");
            }

        }

    }
    
    /**
     * @see org.apache.avalon.framework.activity.Initializable#initialize()
     */
    public void initialize() throws Exception {
        Iterator h = commandHandlerMap.keySet().iterator();
    
        while(h.hasNext()) {
            List handlers = (List) commandHandlerMap.get(h.next());
            Iterator h2 = handlers.iterator();
            while (h2.hasNext()) {
              ContainerUtil.initialize(h2.next());
            }
        }
    }

    /**
     * Load and add the classes to the handler map
     * 
     * @param classLoader The classLoader to use
     * @param className The class name 
     * @param config The configuration 
     * @throws ConfigurationException Get thrown on error
     */
    private void loadClass(ClassLoader classLoader, String className,
            Configuration config) throws ConfigurationException {
        try {
            Object handler = classLoader.loadClass(className).newInstance();

            // enable logging
            ContainerUtil.enableLogging(handler, getLogger());

            ContainerUtil.contextualize(handler, context);

            // servicing the handler
            ContainerUtil.service(handler, serviceManager);

            // configure the handler
            ContainerUtil.configure(handler, config);

            // if it is a connect handler add it to list of connect handlers
            if (handler instanceof AbstractConnectHandler) {
                connectHandlers.add((AbstractConnectHandler) handler);
                if (getLogger().isInfoEnabled()) {
                    getLogger().info("Added ConnectHandler: " + className);
                }
            }

            // if it is a commands handler add it to the map with key as command
            // name
            if (handler instanceof CommandsHandler) {
                Map c = ((CommandsHandler) handler).getCommands();

                Iterator cmdKeys = c.keySet().iterator();

                while (cmdKeys.hasNext()) {
                    String commandName = cmdKeys.next().toString();
                    String cName = c.get(commandName).toString();

                    DefaultConfiguration cmdConf = new DefaultConfiguration(
                            "handler");
                    cmdConf.setAttribute("command", commandName);
                    cmdConf.setAttribute("class", cName);

                    loadClass(classLoader, cName, cmdConf);
                }

            }

            // if it is a command handler add it to the map with key as command
            // name
            if (handler instanceof AbstractCommandHandler) {
                String commandName = config.getAttribute("command");
                String cmds[] = commandName.split(",");
                List implCmds = ((AbstractCommandHandler) handler).getImplCommands();

                for (int i = 0; i < cmds.length; i++) {
                    commandName = cmds[i].trim().toUpperCase(Locale.US);

                    // Check if the commandHandler implement the configured command
                    if (implCmds.contains(commandName)) {
                        addToMap(commandName, (AbstractCommandHandler) handler);
                        if (getLogger().isInfoEnabled()) {
                            getLogger().info(
                                    "Added Commandhandler: " + className);
                        }
                    } else {
                        // The Configured command is not implemented. Throw an exception
                        throw new ConfigurationException("Commandhandler "
                                + className + " not implement the command "
                                + commandName);
                    }

                }

            }

            // if it is a message handler add it to list of message handlers
            if (handler instanceof AbstractMessageHandler) {
                messageHandlers.add((AbstractMessageHandler) handler);
                if (getLogger().isInfoEnabled()) {
                    getLogger().info("Added MessageHandler: " + className);
                }
            }
        } catch (ClassNotFoundException ex) {
            if (getLogger().isErrorEnabled()) {
                getLogger().error("Failed to add Commandhandler: " + className,
                        ex);
            }
            throw new ConfigurationException("Failed to add Commandhandler: "
                    + className, ex);
        } catch (IllegalAccessException ex) {
            if (getLogger().isErrorEnabled()) {
                getLogger().error("Failed to add Commandhandler: " + className,
                        ex);
            }
            throw new ConfigurationException("Failed to add Commandhandler: "
                    + className, ex);
        } catch (InstantiationException ex) {
            if (getLogger().isErrorEnabled()) {
                getLogger().error("Failed to add Commandhandler: " + className,
                        ex);
            }
            throw new ConfigurationException("Failed to add Commandhandler: "
                    + className, ex);
        } catch (ServiceException e) {
            if (getLogger().isErrorEnabled()) {
                getLogger().error(
                        "Failed to service Commandhandler: " + className, e);
            }
            throw new ConfigurationException("Failed to add Commandhandler: "
                    + className, e);
        } catch (ContextException e) {
            if (getLogger().isErrorEnabled()) {
                getLogger().error(
                        "Failed to service Commandhandler: " + className, e);
            }
            throw new ConfigurationException("Failed to add Commandhandler: "
                    + className, e);
        }
    }

    /**
     * Return a DefaultConfiguration build on the given command name and classname
     * 
     * @param cmdName The command name
     * @param className The class name
     * @return DefaultConfiguration
     */
    private DefaultConfiguration addHandler(String cmdName, String className) {
        DefaultConfiguration cmdConf = new DefaultConfiguration("handler");
        cmdConf.setAttribute("command",cmdName);
        cmdConf.setAttribute("class",className);
        return cmdConf;
    }

    /**
     * Add it to map (key as command name, value is an array list of commandhandlers)
     *
     * @param commandName the command name which will be key
     * @param cmdHandler The commandhandler object
     */
    private void addToMap(String commandName, AbstractCommandHandler cmdHandler) {
        ArrayList handlers = (ArrayList)commandHandlerMap.get(commandName);
        if(handlers == null) {
            handlers = new ArrayList();
            commandHandlerMap.put(commandName, handlers);
        }
        handlers.add(cmdHandler);
    }

    /**
     * Returns all the configured commandhandlers for the specified command
     *
     * @param commandName the command name which will be key
     * @return List of commandhandlers
     */
    List getCommandHandlers(String command) {
        if (command == null) {
            return null;
        }
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("Lookup command handler for command: " + command);
        }
        List handlers =  (List)commandHandlerMap.get(command);
        if(handlers == null) {
            handlers = (List)commandHandlerMap.get(UnknownCmdHandler.UNKNOWN_COMMAND);
        }

        return handlers;
    }

    /**
     * Returns all the configured message handlers
     *
     * @return List of message handlers
     */
    List getMessageHandlers() {
        return messageHandlers;
    }

    /**
     * Returns all the configured connect handlers
     *
     * @return List of connect handlers
     */
    List getConnectHandlers() {
        return connectHandlers;
    }

}
