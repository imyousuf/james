/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.nntpserver.repository;

import org.apache.avalon.excalibur.io.AndFileFilter;
import org.apache.avalon.excalibur.io.DirectoryFileFilter;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.ContextException;
import org.apache.avalon.framework.context.Contextualizable;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.logger.LogEnabled;
import org.apache.james.context.AvalonContextUtilities;
import org.apache.james.nntpserver.DateSinceFileFilter;
import org.apache.james.nntpserver.NNTPException;
import org.apache.oro.io.GlobFilenameFilter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * NNTP Repository implementation.
 *
 * @author Harmeet Bedi <harmeet@kodemuse.com>
 */
public class NNTPRepositoryImpl extends AbstractLogEnabled 
    implements NNTPRepository, Contextualizable, Configurable, Initializable
{
    /**
     * The context employed by this repository
     */
    private Context context;

    /**
     * The configuration employed by this repository
     */
    private Configuration configuration;

    /**
     * Whether the repository is read only
     */
    private boolean readOnly;

    /**
     * The groups are located under this path.
     */
    private File rootPath;

    /**
     * Articles are temporarily written here and then sent to the spooler.
     */
    private File tempPath;

    /**
     * The spooler for this repository.
     */
    private NNTPSpooler spool;

    /**
     * The article ID repository associated with this NNTP repository.
     */
    private ArticleIDRepository articleIDRepo;

    /**
     * The list of groups stored in this repository
     */
    private String[] addGroups = null;

    /**
     * The root path as a String.
     */
    private String rootPathString = null;

    /**
     * The temp path as a String.
     */
    private String tempPathString = null;

    /**
     * The article ID path as a String.
     */
    private String articleIdPathString = null;

    /**
     * The domain suffix used for files in the article ID repository.
     */
    private String articleIDDomainSuffix = null;

    /**
     * The ordered list of fields returned in the overview format for
     * articles stored in this repository.
     */
    private String[] overviewFormat = { "Subject:",
                                        "From:",
                                        "Date:",
                                        "Message-ID:",
                                        "References:",
                                        "Bytes:",
                                        "Lines:"
                                      };

    /**
     * This is a mapping of group names to NNTP group objects.
     *
     * TODO: This needs to be addressed so it scales better
     */
    private HashMap repositoryGroups = new HashMap();

    /**
     * @see org.apache.avalon.framework.context.Contextualizable#contextualize(Context)
     */
    public void contextualize(Context context)
            throws ContextException {
        this.context = context;
    }

    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(Configuration)
     */
    public void configure( Configuration aConfiguration ) throws ConfigurationException {
        configuration = aConfiguration;
        readOnly = configuration.getChild("readOnly").getValueAsBoolean(false);
        Configuration newsgroupConfiguration = configuration.getChild("newsgroups");
        List addGroupsList = new ArrayList();
        if ( configuration != null ) {
            Configuration[] children = newsgroupConfiguration.getChildren("newsgroup");
            if ( children != null ) {
                for ( int i = 0 ; i < children.length ; i++ ) {
                    addGroupsList.add(children[i].getValue());
                }
            }
        }
        articleIDDomainSuffix = configuration.getChild("articleIDDomainSuffix")
            .getValue("foo.bar.sho.boo");
        addGroups = (String[])addGroupsList.toArray(new String[0]);
        rootPathString = configuration.getChild("rootPath").getValue();
        tempPathString = configuration.getChild("tempPath").getValue();
        articleIdPathString = configuration.getChild("articleIDPath").getValue();
        getLogger().debug("Repository configuration done");
    }

    /**
     * @see org.apache.avalon.framework.activity.Initializable#initialize()
     */
    public void initialize() throws Exception {

        File articleIDPath = null;

        try {
            rootPath = AvalonContextUtilities.getFile(context, rootPathString);
            tempPath = AvalonContextUtilities.getFile(context, tempPathString);
            articleIDPath = AvalonContextUtilities.getFile(context, articleIdPathString);
        } catch (Exception e) {
            getLogger().fatalError(e.getMessage(), e);
            throw e;
        }

        if ( articleIDPath.exists() == false ) {
            articleIDPath.mkdirs();
        }

        articleIDRepo = new ArticleIDRepository(articleIDPath, articleIDDomainSuffix);
        spool = (NNTPSpooler)createSpooler();
        spool.setRepository(this);
        spool.setArticleIDRepository(articleIDRepo);
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("repository:readOnly=" + readOnly);
            getLogger().debug("repository:rootPath=" + rootPath.getAbsolutePath());
            getLogger().debug("repository:tempPath=" + tempPath.getAbsolutePath());
        }

        if ( rootPath.exists() == false ) {
            rootPath.mkdirs();
        } else if (!(rootPath.isDirectory())) {
            StringBuffer errorBuffer =
                new StringBuffer(128)
                    .append("NNTP repository root directory is improperly configured.  The specified path ")
                    .append(rootPathString)
                    .append(" is not a directory.");
            throw new ConfigurationException(errorBuffer.toString());
        }

        for ( int i = 0 ; i < addGroups.length ; i++ ) {
            File groupFile = new File(rootPath,addGroups[i]);
            if ( groupFile.exists() == false ) {
                groupFile.mkdirs();
            } else if (!(groupFile.isDirectory())) {
                StringBuffer errorBuffer =
                    new StringBuffer(128)
                        .append("A file exists in the NNTP root directory with the same name as a newsgroup.  File ")
                        .append(addGroups[i])
                        .append("in directory ")
                        .append(rootPathString)
                        .append(" is not a directory.");
                throw new ConfigurationException(errorBuffer.toString());
            }
        }
        if ( tempPath.exists() == false ) {
            tempPath.mkdirs();
        } else if (!(tempPath.isDirectory())) {
            StringBuffer errorBuffer =
                new StringBuffer(128)
                    .append("NNTP repository temp directory is improperly configured.  The specified path ")
                    .append(tempPathString)
                    .append(" is not a directory.");
            throw new ConfigurationException(errorBuffer.toString());
        }

        getLogger().debug("repository initialization done");
    }

    /**
     * @see org.apache.james.nntpserver.repository.NNTPRepository#isReadOnly()
     */
    public boolean isReadOnly() {
        return readOnly;
    }

    /**
     * @see org.apache.james.nntpserver.repository.NNTPRepository#getGroup(String)
     */
    public NNTPGroup getGroup(String groupName) {
        File groupFile = new File(rootPath,groupName);
        NNTPGroup groupToReturn = null;
        synchronized(this) {
            groupToReturn = (NNTPGroup)repositoryGroups.get(groupName);
            if ((groupToReturn == null) && groupFile.exists() && groupFile.isDirectory() ) {
                groupToReturn = new NNTPGroupImpl(groupFile);
                ((NNTPGroupImpl)groupToReturn).enableLogging(getLogger());
                repositoryGroups.put(groupName, groupToReturn);
            }
        }
        return groupToReturn;
    }

    /**
     * @see org.apache.james.nntpserver.repository.NNTPRepository#getArticleFromID(String)
     */
    public NNTPArticle getArticleFromID(String id) {
        try {
            return articleIDRepo.getArticle(this,id);
        } catch(Exception ex) {
            ex.printStackTrace();
            return null;
        }
//         int idx = id.indexOf('@');
//         String name = id.substring(0,idx);
//         String groupname = id.substring(idx+1);
//         NNTPGroup group = getGroup(groupname);
//         return ( group == null ) ? null : group.getArticleFromID(name);
    }

    /**
     * @see org.apache.james.nntpserver.repository.NNTPRepository#createArticle(NNTPLineReader)
     */
    public void createArticle(NNTPLineReader reader) {
        StringBuffer fileBuffer =
            new StringBuffer(32)
                    .append(System.currentTimeMillis())
                    .append(".")
                    .append(Math.random());
        File f = new File(tempPath, fileBuffer.toString());
        try {
            FileOutputStream fout = new FileOutputStream(f);
            PrintStream prt = new PrintStream(fout,true);
            String line;
            while ( ( line = reader.readLine() ) != null ) {
                prt.println(line);
            }
            prt.close();
            f.renameTo(new File(spool.getSpoolPath(),f.getName()));
        } catch(IOException ex) {
            throw new NNTPException("create article failed",ex);
        }
    }

    /**
     * @see org.apache.james.nntpserver.repository.NNTPRepository#getMatchedGroups(String)
     */
    public Iterator getMatchedGroups(String wildmat) {
        File[] f = rootPath.listFiles(new AndFileFilter
            (new DirectoryFileFilter(),new GlobFilenameFilter(wildmat)));
        return getGroups(f);
    }

    /**
     * Gets an iterator of all news groups represented by the files
     * in the parameter array.
     *
     * @param f the array of files that correspond to news groups
     *
     * @return an iterator of news groups
     */
    private Iterator getGroups(File[] f) {
        List list = new ArrayList();
        for ( int i = 0 ; i < f.length ; i++ ) {
            if (f[i] != null) {
                list.add(getGroup(f[i].getName()));
            }
        }
        return list.iterator();
    }

    /**
     * @see org.apache.james.nntpserver.repository.NNTPRepository#getGroupsSince(Date)
     */
    public Iterator getGroupsSince(Date dt) {
        File[] f = rootPath.listFiles(new AndFileFilter
            (new DirectoryFileFilter(),new DateSinceFileFilter(dt.getTime())));
        return getGroups(f);
    }

    // gets the list of groups.
    // creates iterator that concatenates the article iterators in the list of groups.
    // there is at most one article iterator reference for all the groups

    /**
     * @see org.apache.james.nntpserver.repository.NNTPRepository#getArticlesSince(Date)
     */
    public Iterator getArticlesSince(final Date dt) {
        final Iterator giter = getGroupsSince(dt);
        return new Iterator() {

                private Iterator iter = null;

                public boolean hasNext() {
                    if ( iter == null ) {
                        if ( giter.hasNext() ) {
                            NNTPGroup group = (NNTPGroup)giter.next();
                            iter = group.getArticlesSince(dt);
                        }
                        else {
                            return false;
                        }
                    }
                    if ( iter.hasNext() ) {
                        return true;
                    } else {
                        iter = null;
                        return hasNext();
                    }
                }

                public Object next() {
                    return iter.next();
                }

                public void remove() {
                    throw new UnsupportedOperationException("remove not supported");
                }
            };
    }

    /**
     * @see org.apache.james.nntpserver.repository.NNTPRepository#getOverviewFormat()
     */
    public String[] getOverviewFormat() {
        return overviewFormat;
    }

    /**
     * Creates an instance of the spooler class.
     *
     * TODO: This method doesn't properly implement the Avalon lifecycle.
     */
    private NNTPSpooler createSpooler() 
            throws ConfigurationException {
        String className = "org.apache.james.nntpserver.repository.NNTPSpooler";
        Configuration spoolerConfiguration = configuration.getChild("spool");
        try {
            // Must be a subclass of org.apache.james.nntpserver.repository.NNTPSpooler
            className = spoolerConfiguration.getAttribute("class");
        } catch(ConfigurationException ce) {
            // Use the default class.
        }
        try {
            Object obj = getClass().getClassLoader().loadClass(className).newInstance();
            // TODO: Need to support compose
            if ( obj instanceof LogEnabled ) {
                ((LogEnabled)obj).enableLogging( getLogger() );
            }
            if (obj instanceof Contextualizable) {
                ((Contextualizable)obj).contextualize(context);
            }
            if ( obj instanceof Configurable ) {
                ((Configurable)obj).configure(spoolerConfiguration.getChild("configuration"));
            }
            if ( obj instanceof Initializable ) {
                ((Initializable)obj).initialize();
            }
            return (NNTPSpooler)obj;
        } catch(ClassCastException cce) {
            StringBuffer errorBuffer =
                new StringBuffer(128)
                    .append("Spooler initialization failed because the spooler class ")
                    .append(className)
                    .append(" was not a subclass of org.apache.james.nntpserver.repository.NNTPSpooler");
            String errorString = errorBuffer.toString();
            getLogger().error(errorString, cce);
            throw new ConfigurationException(errorString, cce);
        } catch(Exception ex) {
            getLogger().error("Spooler initialization failed",ex);
            throw new ConfigurationException("Spooler initialization failed",ex);
        }
    }
}
