/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.remotemanager;

import java.rmi.Naming;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import org.apache.avalon.phoenix.Block;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.logger.LogEnabled;
import org.apache.avalon.framework.context.Contextualizable;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.ContextException;
import org.apache.avalon.framework.context.DefaultContext;
import org.apache.avalon.framework.component.Composable;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.activity.Initializable;


/**
 * This is generic RMI Service which register remote object in a RMI registry.
 * 
 * @author <a href="mailto:buchi@email.com">Gabriel Bucher</a>
 */
public class RMIRemoteManager extends AbstractLogEnabled
        implements Block, Contextualizable, Composable, Configurable, Initializable {
    
    // keywords for configuration
    private static final String RMIREGISTRY = "rmiregistry";
    private static final String RMIREGISTRY_HOST = "host";
    private static final String RMIREGISTRY_PORT = "port";
    private static final String RMIREGISTRY_NEW = "new";
    private static final String RMI_OBJECTS = "objects";
    private static final String RMI_OBJECT = "object";
    private static final String RMI_OBJECT_INTERFACE = "interface";
    private static final String RMI_OBJECT_CLASS = "class";
    private static final String RMI_OBJECT_BINDNAME = "bindname";
    private static final String RMI_OBJECT_BIND = "bind";
    private static final String RMI_OBJECT_CONFIGURATION = "configuration";

    private DefaultContext context;
    private ComponentManager componentManager;
    
    // helper variable for configuration
    private int rmiRegistryPort = 1099;
    private String rmiRegistryHost = "localhost";
    /* important notes! It is not possible to create a second rmi registry
       inside the same jvm. Phoenix is already using a rmi registry for 
       jmx at port 1111 */
    private boolean createNewRmiRegistry = false;
    private Configuration rmiObjects;


    public void contextualize(Context context)
            throws ContextException {
        this.context = new DefaultContext(context);
    }

    public void compose(ComponentManager componentManager)
            throws ComponentException {
        this.componentManager = componentManager;
    }

    public void configure(Configuration configuration)
            throws ConfigurationException {
        final Configuration rmiRegistryConf = configuration.getChild(this.RMIREGISTRY);
        this.rmiRegistryHost = rmiRegistryConf.getAttribute(this.RMIREGISTRY_HOST, "localhost");
        this.rmiRegistryPort = rmiRegistryConf.getAttributeAsInteger(this.RMIREGISTRY_PORT, 1099);
        this.createNewRmiRegistry = rmiRegistryConf.getAttributeAsBoolean(this.RMIREGISTRY_NEW, false);

        this.rmiObjects = configuration.getChild(this.RMI_OBJECTS);
    }

    /**
     * Create or get a rmi registry. Bounds all remote objects to the specified
     * rmi registry.
     * 
     * @exception ConfigurationException throw any exceptions as a ConfigurationException
     */
    public void initialize() 
            throws ConfigurationException {
        final Configuration[] objects = this.rmiObjects.getChildren(this.RMI_OBJECT);
        if (this.createNewRmiRegistry) {
            // TODO: check out if its posible to create a second rmi registry within the same jvm.
            try {
                LocateRegistry.createRegistry(this.rmiRegistryPort);
                this.getLogger().info("new rmi registry at port " + this.rmiRegistryPort + " created!");
            } catch (RemoteException re) {
                getLogger().error("Couldn't create a RMI Registry at port " + this.rmiRegistryPort);
                throw new ConfigurationException("Please check your RMI Registry settings!!!");
            }
        } else {
            // check if the specified rmi registry is running!
            try {
                Registry registry = LocateRegistry.getRegistry(this.rmiRegistryHost, this.rmiRegistryPort);
            } catch (RemoteException re) {
                getLogger().error("Couldn't find RMI Registry [" + this.rmiRegistryHost + ":" +
                                  this.rmiRegistryPort);
                throw new ConfigurationException("Please check your RMI Registry settings!!!");
            }
        }

        // create rmi objects and bounds to the rmi registry
        for (int i = 0; i < objects.length; i++) {
            final String rmiInterface = objects[i].getAttribute(this.RMI_OBJECT_INTERFACE);
            final String rmiClass = objects[i].getAttribute(this.RMI_OBJECT_CLASS);
            final String rmiBindname = objects[i].getAttribute(this.RMI_OBJECT_BINDNAME);
            final boolean rmiBind = objects[i].getAttributeAsBoolean(this.RMI_OBJECT_BIND, false);
            final Configuration rmiObjectConf = objects[i].getChild(this.RMI_OBJECT_CONFIGURATION);
            try {
                Class classObject = Class.forName(rmiClass);
                Remote remote = (Remote)classObject.newInstance();
                // first of all, set logger
                if (remote instanceof LogEnabled) {
                    ((LogEnabled)remote).enableLogging(getLogger().getChildLogger(rmiBindname));
                }
                // Contextualizable, Composable, Configurable, Initializable
                if (remote instanceof Contextualizable) {
                    ((Contextualizable)remote).contextualize(this.context);
                }
                if (remote instanceof Composable) {
                    ((Composable)remote).compose(this.componentManager);
                }
                if (remote instanceof Configurable) {
                    ((Configurable)remote).configure(rmiObjectConf);
                }
                if (remote instanceof Initializable) {
                    ((Initializable)remote).initialize();
                }
                // export remote object
                UnicastRemoteObject.exportObject(remote);
                this.getLogger().info("Export RMI Object " + rmiClass);
                // add instance to the context
                this.context.put(rmiBindname, remote);
                if (rmiBind) {
                    String bindName = "//" + this.rmiRegistryHost + 
                                      ":" + this.rmiRegistryPort + 
                                      "/" + rmiBindname;
                    Naming.rebind(bindName, remote);
                    this.getLogger().info("Rebind '" + rmiClass + "' to " + bindName);
                }
            } catch (ClassNotFoundException cnfe) {
                this.getLogger().error("Could not find RMI Class object!", cnfe);
            } catch (ContextException ce) {
                this.getLogger().error(ce.getMessage(), ce);
            } catch (ComponentException cpe) {
                this.getLogger().error(cpe.getMessage(), cpe);
            } catch (ConfigurationException cfe) {
                this.getLogger().error(cfe.getMessage(), cfe);
            } catch (Exception e) {
                this.getLogger().error(e.getMessage(), e);
            }
        }
    }

}

