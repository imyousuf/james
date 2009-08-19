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


package org.apache.james.transport;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.annotation.Resource;
import javax.mail.MessagingException;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.james.api.kernel.ServiceLocator;
import org.apache.mailet.Mailet;
import org.apache.mailet.MailetException;
/**
 * Loads Mailets for use inside James.
 *
 */
public class JamesMailetLoader extends AbstractLoader implements MailetLoader {
    
    private ServiceLocator serviceLocator;
     
    /**
     * Gets the service locator.
     * @return the serviceLocator, not null after initialisation
     */
    public final ServiceLocator getServiceLocator() {
        return serviceLocator;
    }

    /**
     * Sets the service locator.
     * @param serviceLocator the serviceLocator to set
     */
    @Resource(name="org.apache.james.ServiceLocator")
    public final void setServiceLocator(ServiceLocator serviceLocator) {
        this.serviceLocator = serviceLocator;
    }

    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(Configuration)
     */
    public void configure(Configuration conf) throws ConfigurationException {
        getPackages(conf,MAILET_PACKAGE);
    }

    /**
     * @see org.apache.james.transport.MailetLoader#getMailet(java.lang.String, org.apache.avalon.framework.configuration.Configuration)
     */
    public Mailet getMailet(String mailetName, Configuration configuration)
        throws MessagingException {
        try {
            for (int i = 0; i < packages.size(); i++) {
                String className = (String) packages.elementAt(i) + mailetName;
                try {
                    Mailet mailet = (Mailet) Thread.currentThread().getContextClassLoader().loadClass(className).newInstance();
                    MailetConfigImpl configImpl = new MailetConfigImpl();
                    configImpl.setMailetName(mailetName);
                    configImpl.setConfiguration(configuration);
                    configImpl.setMailetContext(new MailetContextWrapper(mailetContext, getLogger().getChildLogger(mailetName))); 

                    injectServices(mailet);

                    mailet.init(configImpl);
                    return mailet;
                } catch (ClassNotFoundException cnfe) {
                    //do this so we loop through all the packages
                }
            }
            StringBuilder exceptionBuffer =
                new StringBuilder(128)
                    .append("Requested mailet not found: ")
                    .append(mailetName)
                    .append(".  looked in ")
                    .append(packages.toString());
            throw new ClassNotFoundException(exceptionBuffer.toString());
        } catch (MessagingException me) {
            throw me;
        } catch (Exception e) {
            StringBuilder exceptionBuffer =
                new StringBuilder(128).append("Could not load mailet (").append(mailetName).append(
                    ")");
            throw new MailetException(exceptionBuffer.toString(), e);
        }
    }

    private void injectServices(Mailet mailet) throws IllegalArgumentException, IllegalAccessException, 
                                                        InvocationTargetException {
        if (serviceLocator == null) {
           getLogger().warn("Service locator not set. Cannot load services.");
        } else {
            Method[] methods = mailet.getClass().getMethods();
            for (Method method : methods) {
                Resource resourceAnnotation = method.getAnnotation(Resource.class);
                if (resourceAnnotation != null) {
                    final String name = resourceAnnotation.name();
                    final Object resource = serviceLocator.get(name);
                    if (resource == null) {
                        if (getLogger().isWarnEnabled()) {
                            getLogger().warn("Unknown service: "  + name);
                        }
                   } else {
                        Object[] args = {resource};
                        method.invoke(mailet, args);
                    }
                }
            }
        }
    }
}
