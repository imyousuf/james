/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.transport;
import java.io.File;
import java.io.IOException;
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
import org.apache.avalon.phoenix.BlockContext;
/**
 * @author <A href="mailto:danny@apache.org">Danny Angus</a>
 * 
 * $Id: Loader.java,v 1.1 2003/01/08 16:53:58 danny Exp $
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
    public void contextualize(final Context context) throws ContextException {
        try {
            baseDirectory = ((BlockContext) context).getBaseDirectory().getCanonicalPath();
        } catch (IOException e) {
            logger.error("cant get base directory for mailet loader");
            throw new ContextException("cant contextualise mailet loader " + e.getMessage(), e);
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
            jarlist.add(new URL("file://" + baseDirectory + "/SAR-INF/lib/classes/"));
        } catch (MalformedURLException e) {
            logger.error(
                "cant add "
                    + "file://"
                    + baseDirectory
                    + "/SAR-INF/classes/ to mailet classloader");
        }
        if (flist != null) {
            for (int i = 0; i < flist.length; i++) {
                try {
                    if (flist[i].indexOf("jar") == flist[i].length() - 3) {
                        jarlist.add(new URL("file://" + flist[i]));
                        logger.debug("added " + flist[i] + " to mailet Classloader");
                    }
                } catch (MalformedURLException e) {
                    logger.error("cant add " + "file://" + flist[i] + " to mailet classloader");
                }
            }
        }
        classPath = (URL[]) jarlist.toArray(new URL[jarlist.size()]);
        mailetClassLoader = new URLClassLoader(classPath, this.getClass().getClassLoader());
    }
}
