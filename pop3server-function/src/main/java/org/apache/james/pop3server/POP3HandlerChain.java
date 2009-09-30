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



package org.apache.james.pop3server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.james.socket.LogEnabled;

/**
  * The POP3HandlerChain is per service object providing access
  * ConnectHandlers, Commandhandlers and message handlers
  */
public class POP3HandlerChain implements Configurable, Serviceable, Initializable {

    /** This log is the fall back shared by all instances */
    private static final Log FALLBACK_LOG = LogFactory.getLog(POP3HandlerChain.class);
    
    /** Non context specific log should only be used when no context specific log is available */
    private Log log = FALLBACK_LOG;
    
    private Map<String, List<CommandHandler>> commandHandlerMap = new HashMap<String, List<CommandHandler>>();
    private List<ConnectHandler> connectHandlers = new ArrayList<ConnectHandler>();
    private List<CapaCapability> capaList = new ArrayList<CapaCapability>();
    private final CommandHandler unknownHandler = new UnknownCmdHandler();
    private ServiceManager serviceManager;

    private final static String[] mandatoryCommands = { "USER" , "PASS", "LIST"};

    /**
     * Sets the service log.
     * Where available, a context sensitive log should be used.
     * @param Log not null
     */
    public void setLog(Log log) {
        this.log = log;
    }
    
    public void service(ServiceManager serviceManager) throws ServiceException {
        this.serviceManager = serviceManager;
    }


    /**
     * loads the various handlers from the configuration
     * @param configuration configuration under handlerchain node
     */
    public void configure(Configuration configuration) throws  ConfigurationException {
        addToMap(unknownHandler);
        if(configuration == null || configuration.getChildren("handler") == null || configuration.getChildren("handler").length == 0) {
            configuration = createDefaultConfiguration();
        }
        if(configuration != null) {
            Configuration[] children = configuration.getChildren("handler");
            if ( children != null ) {
                configureHandlers(children);
            }
        }
        verifyCommandConfiguration();
    }

    private void verifyCommandConfiguration() throws ConfigurationException {
        //the size must be greater than 1 because we added UnknownCmdHandler to the map
        if(commandHandlerMap.size() < 2) {
            if (log.isErrorEnabled()) {
                log.error("No commandhandlers configured");
            }
            throw new ConfigurationException("No commandhandlers configured");
        } else {
            boolean found = true;
            for (int i = 0; i < mandatoryCommands.length; i++) {
                if(!commandHandlerMap.containsKey(mandatoryCommands[i])) {
                    if (log.isErrorEnabled()) {
                        log.error("No commandhandlers configured for the command:" + mandatoryCommands[i]);
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

    private void configureHandlers(Configuration[] children)
            throws ConfigurationException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        for ( int i = 0 ; i < children.length ; i++ ) {
            String className = children[i].getAttribute("class");
            if(className != null) {
                //load the handler
                try {
                    Object handler = classLoader.loadClass(className).newInstance();
                    
                    if (handler instanceof LogEnabled) {
                        ((LogEnabled) handler).setLog(log);
                    }

                    //servicing the handler
                    ContainerUtil.service(handler,serviceManager);

                    //configure the handler
                    ContainerUtil.configure(handler,children[i]);

                    //if it is a connect handler add it to list of connect handlers
                    if(handler instanceof ConnectHandler) {
                        connectHandlers.add((ConnectHandler)handler);
                        if (log.isInfoEnabled()) {
                            log.info("Added ConnectHandler: " + className);
                        }
                    }

                    //if it is a command handler add it to the map with key as command name
                    if(handler instanceof CommandHandler) {
                        addToMap((CommandHandler)handler);
                        if (log.isInfoEnabled()) {
                            log.info("Added Commandhandler: " + className);
                        }
                    }
                    
                    if (handler instanceof CapaCapability) {
                    	capaList.add((CapaCapability) handler);
                    }

                } catch (ClassNotFoundException ex) {
                   if (log.isErrorEnabled()) {
                       log.error("Failed to add Commandhandler: " + className,ex);
                   }
                } catch (IllegalAccessException ex) {
                   if (log.isErrorEnabled()) {
                       log.error("Failed to add Commandhandler: " + className,ex);
                   }
                } catch (InstantiationException ex) {
                   if (log.isErrorEnabled()) {
                       log.error("Failed to add Commandhandler: " + className,ex);
                   }
                } catch (ServiceException e) {
                    if (log.isErrorEnabled()) {
                        log.error("Failed to service Commandhandler: " + className,e);
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Configuration createDefaultConfiguration() {
        Configuration configuration;
        configuration = new DefaultConfiguration("handlerchain");
        List<String> defaultCommands = new ArrayList<String>();
        defaultCommands.add(CapaCmdHandler.class.getName());
        defaultCommands.add(UserCmdHandler.class.getName());
        defaultCommands.add(PassCmdHandler.class.getName());
        defaultCommands.add(StlsCmdHandler.class.getName());
        defaultCommands.add(ListCmdHandler.class.getName());
        defaultCommands.add(UidlCmdHandler.class.getName());
        defaultCommands.add(RsetCmdHandler.class.getName());
        defaultCommands.add(DeleCmdHandler.class.getName());
        defaultCommands.add(NoopCmdHandler.class.getName());
        defaultCommands.add(RetrCmdHandler.class.getName());
        defaultCommands.add(TopCmdHandler.class.getName());
        defaultCommands.add(StatCmdHandler.class.getName());
        defaultCommands.add(QuitCmdHandler.class.getName());
        for (int i = 0; i < defaultCommands.size(); i++) {
            String className = defaultCommands.get(i);
            DefaultConfiguration cmdConf = new DefaultConfiguration("handler");
            cmdConf.setAttribute("class",className);
            ((DefaultConfiguration) configuration).addChild(cmdConf);
        }
        return configuration;
    }

    /**
     * Add it to map (key as command name, value is an array list of commandhandlers)
     *
     * @param commandName the command name which will be key
     * @param cmdHandler The commandhandler object
     */
    private void addToMap(CommandHandler cmdHandler) {
    	List<String> cmds = cmdHandler.getCommands();
    	for (int i = 0 ; i< cmds.size(); i++) {
    		String commandName = cmds.get(i);
    		List<CommandHandler> handlers = commandHandlerMap.get(commandName);
    		if(handlers == null) {
    			handlers = new ArrayList<CommandHandler>();
    			commandHandlerMap.put(commandName, handlers);
       	 	}
    		handlers.add(cmdHandler);
    	}
    }

    /**
     * Returns all the configured commandhandlers for the specified command
     *
     * @param command the command name which will be key
     * @return List of commandhandlers
     */
    List<CommandHandler> getCommandHandlers(String command) {
        if (command == null) {
            return null;
        }
        if (log.isDebugEnabled()) {
            log.debug("Lookup command handler for command: " + command);
        }
        List<CommandHandler> handlers =  commandHandlerMap.get(command);
        if(handlers == null) {
            handlers = commandHandlerMap.get(UnknownCmdHandler.UNKNOWN_COMMAND);
        }

        return handlers;
    }

    /**
     * Returns all the configured connect handlers
     *
     * @return List of connect handlers
     */
    List<ConnectHandler> getConnectHandlers() {
        return connectHandlers;
    }

    /**
     * @see org.apache.avalon.framework.activity.Initializable#initialize()
     */
	public void initialize() throws Exception {
		// wire the capa stuff
		List<CommandHandler> handlers = getCommandHandlers(CapaCmdHandler.COMMAND_NAME);
		if (handlers != null) {
			for (int i = 0; i < handlers.size(); i++) {
				CommandHandler cHandler = handlers.get(i);
			
				// TODO: Maybe an interface ?
				if (cHandler instanceof CapaCmdHandler) {
					for (int a =0; a < capaList.size(); a++) {
						((CapaCmdHandler) cHandler).wireHandler(capaList.get(a));
					}
				}
			}
		}
	}

}
