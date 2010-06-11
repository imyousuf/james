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

package org.apache.mailet;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.sql.DataSource;

/**
 * 
 * Utility class to register and de-register services and datasources in JNDI contexts per Mailet API v3X (experimental)
 * Data sources are registered to mailet:service/datasources/name
 * Services are registered to mailet:service/services/name
 * 
 */
public class MailetServiceJNDIRegistration {

    private static final String ENV = "env";
    private static final String DATASOURCES = "datasources";
    private static final String SERVICES = "services";
    private static final String MAILET_SERVICE = "mailet:service";

    /**
     *  <code>DATA_SOURCE_CONTEXT</code>
     */
    public static final String DATA_SOURCE_CONTEXT=MAILET_SERVICE+"/"+ENV+"/"+DATASOURCES; 
    /**
     *  <code>SERVICE_CONTEXT</code>
     */
    public static final String SERVICE_CONTEXT=MAILET_SERVICE+"/"+ENV+"/"+SERVICES;

    /**
     * @param dsName
     * @param ds
     * @throws MailetException
     */
    public static void registerDataSource(String dsName, DataSource ds) throws MailetException {

        try{
            Context dsContext = getDatasourceContext();
            try{
                dsContext.bind(dsName, ds);
            }catch (NameAlreadyBoundException e){
                dsContext.rebind(dsName, ds);
                System.out.println("Overrode registered datasource "+dsName+" in naming context ");
            }
        }catch (NamingException e){
            throw new MailetException("Error registering datasource "+dsName+" in naming context ", e);
        }
        System.out.println("Registered datasource "+dsName+" in naming context ");
    }
    
    /**
     * @param serviceName
     * @param service
     * @throws MailetException
     */
    public static void registerService(String serviceName, MailetService service) throws MailetException {
        
        try{
            Context serviceContext = getServicesContext();
            try{
                serviceContext.bind(serviceName, service);
                System.out.println("Registered service "+serviceName+" in naming context ");
            }catch (NameAlreadyBoundException e){
                serviceContext.rebind(serviceName, service);
                System.out.println("Overrode registered service "+serviceName+" in naming context ");
            }
        }catch (NamingException e){
            throw new MailetException("Error registering service "+serviceName+" in naming context ", e);
        }
    }

    private static Context getDatasourceContext() throws NamingException {

        Context context;
        context = new InitialContext();
        Context subContext = getContext(MAILET_SERVICE, context);
        Context envContext = getContext(ENV, subContext);
        Context dsContext = getContext(DATASOURCES, envContext);
        return dsContext;
    }
    
    private static Context getServicesContext() throws NamingException {

        Context context;
        context = new InitialContext();
        Context subContext = getContext(MAILET_SERVICE, context);
        Context envContext = getContext(ENV, subContext);
        Context serviceContext = getContext(SERVICES, envContext);
        return serviceContext;
    }

    /**
     * @param key
     * @param context
     * @return
     * @throws NamingException 
     */
    private static Context getContext(String key, Context context) throws NamingException {

        Context subContext;
        try{
            subContext = (Context) context.lookup(key);
        }catch (NameNotFoundException e){
            subContext = context.createSubcontext(key);
        }
        return subContext;
    }

    /**
     * @param dsName
     * @throws MailetException
     */
    public static void deRegisterDataSource(String dsName) throws MailetException {
        System.out.println("DeRegistering datasource "+dsName+" in naming context ");
        try{
            Context dsContext = getDatasourceContext();
            dsContext.unbind(dsName);
        }catch (NamingException e){
            throw new MailetException("Error unbinding datasource "+dsName+" in naming context ", e);
        }
    }
    
    /**
     * @param serviceName
     * @throws MailetException
     */
    public static void deRegisterService(String serviceName) throws MailetException {
        System.out.println("DeRegistering service "+serviceName+" in naming context ");
        try{
            Context serviceContext = getServicesContext();
            serviceContext.unbind(serviceName);
        }catch (NamingException e){
            throw new MailetException("Error unbinding service "+serviceName+" in naming context ", e);
        }
    }
}
