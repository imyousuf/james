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

/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.james.mailrepository.filepair;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import org.apache.avalon.cornerstone.services.store.Repository;
import org.apache.avalon.cornerstone.services.store.Store;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.component.Composable;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.Contextualizable;
import org.apache.avalon.framework.logger.AbstractLogEnabled;

/**
 * @phoenix:block
 * @phoenix:service name="org.apache.avalon.cornerstone.services.store.Store"
 *
 */
public class RepositoryManager
    extends AbstractLogEnabled
    implements Store, Contextualizable, Serviceable, Configurable
{
    private static final String REPOSITORY_NAME = "Repository";
    private static long id = 0;

    protected HashMap m_repositories = new HashMap();
    protected HashMap m_models = new HashMap();
    protected HashMap m_classes = new HashMap();
    protected ServiceManager m_componentManager;
    protected Context m_context;

    public void contextualize( final Context context )
    {
        m_context = context;
    }

    public void service( final ServiceManager componentManager )
        throws ServiceException
    {
        m_componentManager = componentManager;
    }

    public void configure( final Configuration configuration )
        throws ConfigurationException
    {
        final Configuration[] registeredClasses =
            configuration.getChild( "repositories" ).getChildren( "repository" );

        for( int i = 0; i < registeredClasses.length; i++ )
        {
            registerRepository( registeredClasses[ i ] );
        }
    }

    public void registerRepository( final Configuration repConf )
        throws ConfigurationException
    {
        final String className = repConf.getAttribute( "class" );
        getLogger().info( "Registering Repository " + className );

        final Configuration[] protocols =
            repConf.getChild( "protocols" ).getChildren( "protocol" );
        final Configuration[] types = repConf.getChild( "types" ).getChildren( "type" );
        final Configuration[] modelIterator =
            repConf.getChild( "models" ).getChildren( "model" );

        for( int i = 0; i < protocols.length; i++ )
        {
            final String protocol = protocols[ i ].getValue();

            for( int j = 0; j < types.length; j++ )
            {
                final String type = types[ j ].getValue();

                for( int k = 0; k < modelIterator.length; k++ )
                {
                    final String model = modelIterator[ k ].getValue();
                    m_classes.put( protocol + type + model, className );
                    getLogger().info( "   for " + protocol + "," + type + "," + model );
                }
            }
        }
    }

    public void release( final Object component )
    {
    }

    public boolean isSelectable( final Object hint )
    {
        if( hint instanceof Configuration )
            return true;
        else
            return false;
    }

    public Object select( final Object hint )
        throws ServiceException
    {
        Configuration repConf = null;
        try
        {
            repConf = (Configuration)hint;
        }
        catch( final ClassCastException cce )
        {
            throw new ServiceException("", "Hint is of the wrong type. " +
                                          "Must be a Configuration", cce );
        }

        URL destination = null;
        try
        {
            destination = new URL( repConf.getAttribute( "destinationURL" ) );
        }
        catch( final ConfigurationException ce )
        {
            throw new ServiceException("","Malformed configuration has no " +
                                          "destinationURL attribute", ce );
        }
        catch( final MalformedURLException mue )
        {
            throw new ServiceException("", "destination is malformed. " +
                                          "Must be a valid URL", mue );
        }

        try
        {
            final String type = repConf.getAttribute( "type" );
            final String repID = destination + type;
            Repository reply = (Repository)m_repositories.get( repID );
            final String model = (String)repConf.getAttribute( "model" );

            if( null != reply )
            {
                if( m_models.get( repID ).equals( model ) )
                {
                    return reply;
                }
                else
                {
                    final String message = "There is already another repository with the " +
                        "same destination and type but with different model";
                    throw new ServiceException("", message );
                }
            }
            else
            {
                final String protocol = destination.getProtocol();
                final String repClass = (String)m_classes.get( protocol + type + model );

                getLogger().debug( "Need instance of " + repClass + " to handle: " +
                                   protocol + type + model );

                try
                {
                    reply = (Repository)Class.forName( repClass ).newInstance();
                    setupLogger( reply, "repository" );

                    if( reply instanceof Contextualizable )
                    {
                        ( (Contextualizable)reply ).contextualize( m_context );
                    }

                    if( reply instanceof Serviceable )
                    {
                        ( (Serviceable)reply ).service( m_componentManager );
                    }

                    if (reply instanceof Composable) {
                        final String error = "no implementation in place to support Coposable";
                        getLogger().error( error );
                        throw new IllegalArgumentException( error );
                    }

                    if( reply instanceof Configurable )
                    {
                        ( (Configurable)reply ).configure( repConf );
                    }

                    if( reply instanceof Initializable )
                    {
                        ( (Initializable)reply ).initialize();
                    }

                    m_repositories.put( repID, reply );
                    m_models.put( repID, model );
                    getLogger().info( "New instance of " + repClass + " created for " +
                                      destination );
                    return reply;
                }
                catch( final Exception e )
                {
                    final String message = "Cannot find or init repository: " + e.getMessage();
                    getLogger().warn( message, e );

                    throw new ServiceException("", message, e );
                }
            }
        }
        catch( final ConfigurationException ce )
        {
            throw new ServiceException("", "Malformed configuration", ce );
        }
    }

    public static final String getName()
    {
        return REPOSITORY_NAME;
    }
}
