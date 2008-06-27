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

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Locale;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.logger.LogEnabled;

/**
  * The SMTPHandlerChain is per service object providing access
  * ConnectHandlers, Commandhandlers and message handlers
  */
public class SMTPHandlerChain extends AbstractLogEnabled {

    private HashMap commandHandlerMap = new HashMap();
    private ArrayList messageHandlers = new ArrayList();
    private ArrayList connectHandlers = new ArrayList();

    private final CommandHandler unknownHandler = new UnknownCmdHandler();

    private final static String[] mandatoryCommands = { "MAIL" , "RCPT", "DATA"};


    /**
     * loads the various handlers from the configuration
     * @param configuration configuration under handlerchain node
     */
    void load(Configuration configuration) throws  ConfigurationException {
        addToMap(UnknownCmdHandler.UNKNOWN_COMMAND, unknownHandler);
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
                            if (handler instanceof LogEnabled) {
                                ((LogEnabled)handler).enableLogging(getLogger());
                            }

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

                            //if it is a message handler add it to list of message handlers
                            if(handler instanceof MessageHandler) {
                                messageHandlers.add((MessageHandler)handler);
                                if (getLogger().isInfoEnabled()) {
                                    getLogger().info("Added MessageHandler: " + className);
                                }
                            }

                        } catch (ClassNotFoundException ex) {
                           if (getLogger().isErrorEnabled()) {
                               getLogger().error("Failed to add Commandhandler: " + className);
                           }
                        } catch (IllegalAccessException ex) {
                           if (getLogger().isErrorEnabled()) {
                               getLogger().error("Failed to add Commandhandler: " + className);
                           }
                        } catch (InstantiationException ex) {
                           if (getLogger().isErrorEnabled()) {
                               getLogger().error("Failed to add Commandhandler: " + className);
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
