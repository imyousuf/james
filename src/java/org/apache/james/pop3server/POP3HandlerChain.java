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

package org.apache.james.pop3server;

import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

/**
  * The POP3HandlerChain is per service object providing access
  * ConnectHandlers, Commandhandlers and message handlers
  */
public class POP3HandlerChain extends AbstractLogEnabled implements Configurable, Serviceable {

    private HashMap commandHandlerMap = new HashMap();
    private ArrayList messageHandlers = new ArrayList();
    private ArrayList connectHandlers = new ArrayList();

    private final CommandHandler unknownHandler = new UnknownCmdHandler();
    private ServiceManager serviceManager;

    private final static String[] mandatoryCommands = { "USER" , "PASS", "LIST"};

    public void service(ServiceManager arg0) throws ServiceException {
        serviceManager = arg0;
    }


    /**
     * loads the various handlers from the configuration
     * @param configuration configuration under handlerchain node
     */
    public void configure(Configuration configuration) throws  ConfigurationException {
        addToMap(UnknownCmdHandler.UNKNOWN_COMMAND, unknownHandler);
        if(configuration == null || configuration.getChildren("handler") == null || configuration.getChildren("handler").length == 0) {
            configuration = new DefaultConfiguration("handlerchain");
            Properties cmds = new Properties();
            cmds.setProperty("USER",UserCmdHandler.class.getName());
            cmds.setProperty("PASS",PassCmdHandler.class.getName());
            cmds.setProperty("LIST",ListCmdHandler.class.getName());
            cmds.setProperty("UIDL",UidlCmdHandler.class.getName());
            cmds.setProperty("RSET",RsetCmdHandler.class.getName());
            cmds.setProperty("DELE",DeleCmdHandler.class.getName());
            cmds.setProperty("NOOP",NoopCmdHandler.class.getName());
            cmds.setProperty("RETR",RetrCmdHandler.class.getName());
            cmds.setProperty("TOP" ,TopCmdHandler.class.getName());
            cmds.setProperty("QUIT",QuitCmdHandler.class.getName());
            Enumeration e = cmds.keys();
            while (e.hasMoreElements()) {
                String cmdName = (String) e.nextElement();
                String className = cmds.getProperty(cmdName);
                DefaultConfiguration cmdConf = new DefaultConfiguration("handler");
                cmdConf.setAttribute("command",cmdName);
                cmdConf.setAttribute("class",className);
                ((DefaultConfiguration) configuration).addChild(cmdConf);
            }
        }
        if(configuration != null) {
            Configuration[] children = configuration.getChildren("handler");
            if ( children != null ) {
                ClassLoader classLoader = getClass().getClassLoader();
                for ( int i = 0 ; i < children.length ; i++ ) {
                    String className = children[i].getAttribute("class");
                    if(className != null) {
                        //load the handler
                        try {
                            Object handler = classLoader.loadClass(className).newInstance();

                            //enable logging
                            ContainerUtil.enableLogging(handler, getLogger());

                            //servicing the handler
                            ContainerUtil.service(handler,serviceManager);

                            //configure the handler
                            ContainerUtil.configure(handler,children[i]);

                            //if it is a connect handler add it to list of connect handlers
                            if(handler instanceof ConnectHandler) {
                                connectHandlers.add((ConnectHandler)handler);
                                if (getLogger().isInfoEnabled()) {
                                    getLogger().info("Added ConnectHandler: " + className);
                                }
                            }

                            //if it is a command handler add it to the map with key as command name
                            if(handler instanceof CommandHandler) {
                                String commandName = children[i].getAttribute("command");
                                commandName = commandName.toUpperCase(Locale.US);
                                addToMap(commandName, (CommandHandler)handler);
                                if (getLogger().isInfoEnabled()) {
                                    getLogger().info("Added Commandhandler: " + className);
                                }

                            }

                        } catch (ClassNotFoundException ex) {
                           if (getLogger().isErrorEnabled()) {
                               getLogger().error("Failed to add Commandhandler: " + className,ex);
                           }
                        } catch (IllegalAccessException ex) {
                           if (getLogger().isErrorEnabled()) {
                               getLogger().error("Failed to add Commandhandler: " + className,ex);
                           }
                        } catch (InstantiationException ex) {
                           if (getLogger().isErrorEnabled()) {
                               getLogger().error("Failed to add Commandhandler: " + className,ex);
                           }
                        } catch (ServiceException e) {
                            if (getLogger().isErrorEnabled()) {
                                getLogger().error("Failed to service Commandhandler: " + className,e);
                            }
                        }
                    }
                }
            }
        }

        //the size must be greater than 1 because we added UnknownCmdHandler to the map
        if(commandHandlerMap.size() < 2) {
            if (getLogger().isErrorEnabled()) {
                getLogger().error("No commandhandlers configured");
            }
            throw new ConfigurationException("No commandhandlers configured");
        } else {
            boolean found = true;
            for (int i = 0; i < mandatoryCommands.length; i++) {
                if(!commandHandlerMap.containsKey(mandatoryCommands[i])) {
                    if (getLogger().isErrorEnabled()) {
                        getLogger().error("No commandhandlers configured for the command:" + mandatoryCommands[i]);
                    }
                    found = false;
                    break;
                }
            }

            if(!found) {
                throw new ConfigurationException("No commandhandlers configured for mandatory commands");
            }
            
        }
    }

    /**
     * Add it to map (key as command name, value is an array list of commandhandlers)
     *
     * @param commandName the command name which will be key
     * @param cmdHandler The commandhandler object
     */
    private void addToMap(String commandName, CommandHandler cmdHandler) {
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
