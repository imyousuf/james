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
import javax.mail.MessagingException;
import org.apache.avalon.framework.component.Component;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.ContextException;
import org.apache.avalon.framework.context.Contextualizable;
import org.apache.avalon.framework.logger.Logger;
import org.apache.avalon.phoenix.BlockContext;
import org.apache.james.core.MailetConfigImpl;
import org.apache.mailet.Mailet;
import org.apache.mailet.MailetContext;
import org.apache.mailet.MailetException;
/**
 * Loads Mailets for use inside James.
 *
 * @author Serge Knystautas <sergek@lokitech.com>
 * @author Federico Barbieri <scoobie@systemy.it>
 */
public class MailetLoader implements Component, Configurable, Contextualizable {
    private ClassLoader theClassLoader = null;
    private String baseDirectory = null;
    private Logger logger;
    /**
     * The list of packages that may contain Mailets
     */
    private Vector mailetPackages;
    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(Configuration)
     */
    public void configure(Configuration conf) throws ConfigurationException {
        mailetPackages = new Vector();
        mailetPackages.addElement("");
        final Configuration[] pkgConfs = conf.getChildren("mailetpackage");
        for (int i = 0; i < pkgConfs.length; i++) {
            Configuration c = pkgConfs[i];
            String packageName = c.getValue();
            if (!packageName.endsWith(".")) {
                packageName += ".";
            }
            mailetPackages.addElement(packageName);
        }
        File base = new File(baseDirectory + "/SAR-INF/lib");
        String[] flist = base.list();
        Vector jarlist = new Vector();
        URL[] classPath = null;
        try {
            jarlist.add(new URL("file://" + baseDirectory + "/SAR-INF/classes/"));
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
                        logger.debug("added " + flist[i] + " to Mailet Classloader");
                    }
                } catch (MalformedURLException e) {
                    logger.error("cant add " + "file://" + flist[i] + " to mailet classloader");
                }
            }
        }
        classPath = (URL[]) jarlist.toArray(new URL[jarlist.size()]);
        theClassLoader = new URLClassLoader(classPath, this.getClass().getClassLoader());
    }
    /**
     * Get a new Mailet with the specified name acting
     * in the specified context.
     *
     * @param matchName the name of the mailet to be loaded
     * @param context the MailetContext to be passed to the new
     *                mailet
     * @throws MessagingException if an error occurs
     */
    public Mailet getMailet(String mailetName, MailetContext context, Configuration configuration)
        throws MessagingException {
        try {
            for (int i = 0; i < mailetPackages.size(); i++) {
                String className = (String) mailetPackages.elementAt(i) + mailetName;
                try {
                    MailetConfigImpl configImpl = new MailetConfigImpl();
                    configImpl.setMailetName(mailetName);
                    configImpl.setConfiguration(configuration);
                    configImpl.setMailetContext(context);
                    if (theClassLoader == null) {
                        theClassLoader = this.getClass().getClassLoader();
                    }
                    Mailet mailet = (Mailet) theClassLoader.loadClass(className).newInstance();
                    mailet.init(configImpl);
                    return mailet;
                } catch (ClassNotFoundException cnfe) {
                    //do this so we loop through all the packages
                }
            }
            StringBuffer exceptionBuffer =
                new StringBuffer(128)
                    .append("Requested mailet not found: ")
                    .append(mailetName)
                    .append(".  looked in ")
                    .append(mailetPackages.toString());
            throw new ClassNotFoundException(exceptionBuffer.toString());
        } catch (MessagingException me) {
            throw me;
        } catch (Exception e) {
            StringBuffer exceptionBuffer =
                new StringBuffer(128).append("Could not load mailet (").append(mailetName).append(
                    ")");
            throw new MailetException(exceptionBuffer.toString(), e);
        }
    }
    /**
         * @see org.apache.avalon.framework.context.Contextualizable#contextualize(Context)
         */
    public void contextualize(final Context context) throws ContextException {
        try {
            baseDirectory = ((BlockContext) context).getBaseDirectory().getCanonicalPath();
        } catch (IOException e) {
            logger.error("cant get base directory for mailet loader");
            throw new ContextException("cant contextualise loader " + e.getMessage(), e);
        }
    }
    public void setLogger(Logger logger) {
        this.logger = logger;
    }
}
