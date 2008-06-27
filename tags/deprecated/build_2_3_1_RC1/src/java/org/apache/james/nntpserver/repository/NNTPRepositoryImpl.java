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

package org.apache.james.nntpserver.repository;

import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.ContextException;
import org.apache.avalon.framework.context.Contextualizable;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.james.context.AvalonContextUtilities;
import org.apache.james.nntpserver.DateSinceFileFilter;
import org.apache.james.nntpserver.NNTPException;
import org.apache.james.util.io.AndFileFilter;
import org.apache.james.util.io.DirectoryFileFilter;
import org.apache.oro.io.GlobFilenameFilter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * NNTP Repository implementation.
 */
public class NNTPRepositoryImpl extends AbstractLogEnabled 
    implements NNTPRepository, Contextualizable, Configurable, Initializable {

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
     * A map to allow lookup of valid newsgroup names
     */
    private HashMap groupNameMap = null;

    /**
     * Restrict use to newsgroups specified in config only
     */
    private boolean definedGroupsOnly = false;

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
        articleIDDomainSuffix = configuration.getChild("articleIDDomainSuffix")
            .getValue("foo.bar.sho.boo");
        rootPathString = configuration.getChild("rootPath").getValue(null);
        if (rootPathString == null) {
            throw new ConfigurationException("Root path URL is required.");
        }
        tempPathString = configuration.getChild("tempPath").getValue(null);
        if (tempPathString == null) {
            throw new ConfigurationException("Temp path URL is required.");
        }
        articleIdPathString = configuration.getChild("articleIDPath").getValue(null);
        if (articleIdPathString == null) {
            throw new ConfigurationException("Article ID path URL is required.");
        }
        if (getLogger().isDebugEnabled()) {
            if (readOnly) {
                getLogger().debug("NNTP repository is read only.");
            } else {
                getLogger().debug("NNTP repository is writeable.");
            }
            getLogger().debug("NNTP repository root path URL is " + rootPathString);
            getLogger().debug("NNTP repository temp path URL is " + tempPathString);
            getLogger().debug("NNTP repository article ID path URL is " + articleIdPathString);
        }
        Configuration newsgroupConfiguration = configuration.getChild("newsgroups");
        definedGroupsOnly = newsgroupConfiguration.getAttributeAsBoolean("only", false);
        groupNameMap = new HashMap();
        if ( newsgroupConfiguration != null ) {
            Configuration[] children = newsgroupConfiguration.getChildren("newsgroup");
            if ( children != null ) {
                for ( int i = 0 ; i < children.length ; i++ ) {
                    String groupName = children[i].getValue();
                    groupNameMap.put(groupName, groupName);
                }
            }
        }
        getLogger().debug("Repository configuration done");
    }

    /**
     * @see org.apache.avalon.framework.activity.Initializable#initialize()
     */
    public void initialize() throws Exception {

        getLogger().debug("Starting initialize");
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

        Set groups = groupNameMap.keySet();
        Iterator groupIterator = groups.iterator();
        while( groupIterator.hasNext() ) {
            String groupName = (String)groupIterator.next();
            File groupFile = new File(rootPath,groupName);
            if ( groupFile.exists() == false ) {
                groupFile.mkdirs();
            } else if (!(groupFile.isDirectory())) {
                StringBuffer errorBuffer =
                    new StringBuffer(128)
                        .append("A file exists in the NNTP root directory with the same name as a newsgroup.  File ")
                        .append(groupName)
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
        if (definedGroupsOnly && groupNameMap.get(groupName) == null) {
            if (getLogger().isDebugEnabled()) {
                getLogger().debug(groupName + " is not a newsgroup hosted on this server.");
            }
            return null;
        }
        File groupFile = new File(rootPath,groupName);
        NNTPGroup groupToReturn = null;
        synchronized(this) {
            groupToReturn = (NNTPGroup)repositoryGroups.get(groupName);
            if ((groupToReturn == null) && groupFile.exists() && groupFile.isDirectory() ) {
                try {
                    groupToReturn = new NNTPGroupImpl(groupFile);
                    ContainerUtil.enableLogging(groupToReturn, getLogger());
                    ContainerUtil.contextualize(groupToReturn, context);
                    ContainerUtil.initialize(groupToReturn);
                    repositoryGroups.put(groupName, groupToReturn);
                } catch (Exception e) {
                    getLogger().error("Couldn't create group object.", e);
                    groupToReturn = null;
                }
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
            getLogger().error("Couldn't get article " + id + ": ", ex);
            return null;
        }
    }

    /**
     * @see org.apache.james.nntpserver.repository.NNTPRepository#createArticle(InputStream)
     */
    public void createArticle(InputStream in) {
        StringBuffer fileBuffer =
            new StringBuffer(32)
                    .append(System.currentTimeMillis())
                    .append(".")
                    .append(Math.random());
        File f = new File(tempPath, fileBuffer.toString());
        FileOutputStream fout = null;
        try {
            fout = new FileOutputStream(f);
            byte[] readBuffer = new byte[1024];
            int bytesRead = 0;
            while ( ( bytesRead = in.read(readBuffer, 0, 1024) ) > 0 ) {
                fout.write(readBuffer, 0, bytesRead);
            }
            fout.flush();
            fout.close();
            fout = null;
            boolean renamed = f.renameTo(new File(spool.getSpoolPath(),f.getName()));
            if (!renamed) {
                throw new IOException("Could not create article on the spool.");
            }
        } catch(IOException ex) {
            throw new NNTPException("create article failed",ex);
        } finally {
            if (fout != null) {
                try {
                    fout.close();
                } catch (IOException ioe) {
                    // Ignored
                }
            }
        }
    }

    class GroupFilter implements java.io.FilenameFilter {
        public boolean accept(java.io.File dir, String name) {
            if (getLogger().isDebugEnabled()) {
                getLogger().debug(((definedGroupsOnly ? groupNameMap.containsKey(name) : true) ? "Accepting ": "Rejecting") + name);
            }

            return definedGroupsOnly ? groupNameMap.containsKey(name) : true;
        }
    }

    /**
     * @see org.apache.james.nntpserver.repository.NNTPRepository#getMatchedGroups(String)
     */
    public Iterator getMatchedGroups(String wildmat) {
        File[] f = rootPath.listFiles(new AndFileFilter(new GroupFilter(), new AndFileFilter
            (new DirectoryFileFilter(),new GlobFilenameFilter(wildmat))));
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
        File[] f = rootPath.listFiles(new AndFileFilter(new GroupFilter(), new AndFileFilter
            (new DirectoryFileFilter(),new DateSinceFileFilter(dt.getTime()))));
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
            // TODO: Need to support service
            ContainerUtil.enableLogging(obj, getLogger());
            ContainerUtil.contextualize(obj, context);
            ContainerUtil.configure(obj, spoolerConfiguration.getChild("configuration"));
            ContainerUtil.initialize(obj);
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
