/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
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
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE file.
 */
package org.apache.james.core;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.component.Component;
import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.component.Composable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.ContextException;
import org.apache.avalon.framework.context.Contextualizable;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.james.services.UsersRepository;
import org.apache.james.services.UsersStore;
import org.apache.avalon.phoenix.Block;

/**
 *
 * @author <a href="mailto:fede@apache.org">Federico Barbieri</a>
 */
public class AvalonUsersStore
    extends AbstractLogEnabled
    implements Block, Contextualizable, Composable, Configurable, Initializable, UsersStore {

    private HashMap repositories;
    protected Context                context;
    protected Configuration          configuration;
    protected ComponentManager       componentManager;


    public void contextualize(final Context context)
            throws ContextException {
        this.context = context;
    }

    public void configure( final Configuration configuration )
        throws ConfigurationException {
        this.configuration = configuration;
    }

    public void compose( final ComponentManager componentManager )
        throws ComponentException {
        this.componentManager = componentManager;
    }

    public void initialize()
        throws Exception {

        getLogger().info("AvalonUsersStore init...");
        repositories = new HashMap();

        Configuration[] repConfs = configuration.getChildren("repository");
        for ( int i = 0; i < repConfs.length; i++ )
        {
            Configuration repConf = repConfs[i];
            String repName = repConf.getAttribute("name");
            String repClass = repConf.getAttribute("class");

	    getLogger().debug("Starting " + repClass);
            UsersRepository rep = (UsersRepository) Class.forName(repClass).newInstance();

            setupLogger((Component)rep);

            if (rep instanceof Contextualizable) {
                ((Contextualizable) rep).contextualize(context);
            }
            if (rep instanceof Composable) {
                ((Composable) rep).compose( componentManager );
            }
            if (rep instanceof Configurable) {
                ((Configurable) rep).configure(repConf);
            }
            if (rep instanceof Initializable) {
                ((Initializable) rep).initialize();
            }
            repositories.put(repName, rep);
            getLogger().info("UsersRepository " + repName + " started.");
        }
        getLogger().info("AvalonUsersStore ...init");
    }


    public UsersRepository getRepository(String request) {
        UsersRepository response = (UsersRepository) repositories.get(request);
        if (response == null) {
            getLogger().warn("No users repository called: " + request);
        }
        return response;
    }

    public Iterator getRepositoryNames() {
        return this.repositories.keySet().iterator();
    }
    
}
