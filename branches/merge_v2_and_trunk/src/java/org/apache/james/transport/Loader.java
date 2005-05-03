/***********************************************************************
 * Copyright (c) 2000-2005 The Apache Software Foundation.             *
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

package org.apache.james.transport;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Vector;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.ContextException;
import org.apache.avalon.framework.context.Contextualizable;
import org.apache.avalon.framework.logger.Logger;

/**
 *
 * $Id$
 */
public class Loader implements Contextualizable {
    protected ClassLoader mailetClassLoader = null;
    protected String baseDirectory = null;
    protected Logger logger;
    protected final String MAILET_PACKAGE = "mailetpackage";
    protected final String MATCHER_PACKAGE = "matcherpackage";
      /**
     * The list of packages that may contain Mailets or matchers
     */
    protected Vector packages;

    /**
     * @see org.apache.avalon.framework.context.Contextualizable#contextualize(Context)
     */
    public void contextualize(final Context context) throws ContextException 
    {
        try 
        {
            baseDirectory = ((File)context.get( "app.home") ).getCanonicalPath();
        } 
        catch (Throwable e) 
        {
            logger.error( "can't get base directory for mailet loader" );
            throw new ContextException("can't contextualise mailet loader " + e.getMessage(), e);
        }
    }

    /**
     * Method setLogger.
     * @param logger
     */
    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    protected void getPackages(Configuration conf, String packageType)
        throws ConfigurationException {
        packages = new Vector();
        packages.addElement("");
        final Configuration[] pkgConfs = conf.getChildren(packageType);
        for (int i = 0; i < pkgConfs.length; i++) {
            Configuration c = pkgConfs[i];
            String packageName = c.getValue();
            if (!packageName.endsWith(".")) {
                packageName += ".";
            }
            packages.addElement(packageName);
        }
    }
    /**
     * Method getMailetClassLoader.
     */
    protected void configureMailetClassLoader() {
        File base = new File(baseDirectory + "/SAR-INF/lib");
        String[] flist = base.list();
        Vector jarlist = new Vector();
        URL[] classPath = null;
        try {
            jarlist.add(new URL("file:///" + baseDirectory + "/SAR-INF/classes/"));
        } catch (MalformedURLException e) {
            logger.error(
                "can't add "
                    + "file:///"
                    + baseDirectory
                    + "/SAR-INF/classes/ to mailet classloader");
        }
        if (flist != null) {
            for (int i = 0; i < flist.length; i++) {
                try {
                    if (flist[i].indexOf("jar") == flist[i].length() - 3) {
                        jarlist.add(new URL("file:///" + baseDirectory +"/SAR-INF/lib/"+ flist[i]));
                        logger.debug("added file:///" + baseDirectory +"/SAR-INF/lib/" + flist[i] + " to mailet Classloader");
                    }
                } catch (MalformedURLException e) {
                    logger.error("can't add file:///" + baseDirectory +"/SAR-INF/lib/"+ flist[i] + " to mailet classloader");
                }
            }
        }
        classPath = (URL[]) jarlist.toArray(new URL[jarlist.size()]);
        mailetClassLoader = new URLClassLoader(classPath, this.getClass().getClassLoader());
    }
}
