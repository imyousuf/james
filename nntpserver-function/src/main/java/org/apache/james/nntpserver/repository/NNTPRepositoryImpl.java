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

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.james.api.kernel.LoaderService;
import org.apache.james.api.protocol.Configurable;
import org.apache.james.api.protocol.LogEnabled;
import org.apache.james.nntpserver.DateSinceFileFilter;
import org.apache.james.nntpserver.NNTPException;
import org.apache.james.services.FileSystem;
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

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * NNTP Repository implementation.
 */
public class NNTPRepositoryImpl implements NNTPRepository {

    /**
     * The configuration employed by this repository
     */
    private HierarchicalConfiguration configuration;

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
    private HashMap<String,String> groupNameMap = null;

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
    private HashMap<String,NNTPGroup> repositoryGroups = new HashMap<String,NNTPGroup>();

    /**
     * The fileSystem service
     */
    private FileSystem fileSystem;

    private Log logger;

    private LoaderService loader;

    @Resource(name="org.apache.commons.configuration.Configuration")
    public void setConfiguration(HierarchicalConfiguration configuration) {
        this.configuration = configuration;
    }
    
    @Resource(name="org.apache.commons.logging.Log")
    public void setLogger(Log logger) {
        this.logger = logger;
    }

    /**
     * Setter for the FileSystem dependency
     * 
     * @param system filesystem service
     */
    @Resource(name="org.apache.james.services.FileSystem")
    public void setFileSystem(FileSystem system) {
        this.fileSystem = system;
    }
    
    @Resource(name="org.apache.james.LoaderService")
    public void setLoaderService(LoaderService loader) {
        this.loader = loader;
    }
    
    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(Configuration)
     */
    @SuppressWarnings("unchecked")
    private void configure() throws ConfigurationException {
        readOnly = configuration.getBoolean("readOnly", false);
        articleIDDomainSuffix = configuration.getString("articleIDDomainSuffix", "foo.bar.sho.boo");
        rootPathString = configuration.getString("rootPath", null);
        if (rootPathString == null) {
            throw new ConfigurationException("Root path URL is required.");
        }
        tempPathString = configuration.getString("tempPath", null);
        if (tempPathString == null) {
            throw new ConfigurationException("Temp path URL is required.");
        }
        articleIdPathString = configuration.getString("articleIDPath", null);
        if (articleIdPathString == null) {
            throw new ConfigurationException("Article ID path URL is required.");
        }
        if (logger.isDebugEnabled()) {
            if (readOnly) {
                logger.debug("NNTP repository is read only.");
            } else {
                logger.debug("NNTP repository is writeable.");
            }
            logger.debug("NNTP repository root path URL is " + rootPathString);
            logger.debug("NNTP repository temp path URL is " + tempPathString);
            logger.debug("NNTP repository article ID path URL is " + articleIdPathString);
        }
        definedGroupsOnly = configuration.getBoolean("newsgroups.[@only]", false);
        groupNameMap = new HashMap<String,String>();
        if ( configuration.getKeys("newsgroups").hasNext()) {
            List<String> children = configuration.getList("newsgroups.newsgroup");
            if ( children != null ) {
                for ( int i = 0 ; i < children.size() ; i++ ) {
                    String groupName = children.get(i);
                    groupNameMap.put(groupName, groupName);
                }
            }
        }
        logger.debug("Repository configuration done");
    }

    @PostConstruct
    public void init() throws Exception {

        logger.debug("Starting initialize");
        configure();
        File articleIDPath = null;

        try {
            rootPath = fileSystem.getFile(rootPathString);
            tempPath = fileSystem.getFile(tempPathString);
            articleIDPath = fileSystem.getFile(articleIdPathString);
        } catch (Exception e) {
            logger.fatal(e.getMessage(), e);
            throw e;
        }

        if ( articleIDPath.exists() == false ) {
            articleIDPath.mkdirs();
        }

        articleIDRepo = new ArticleIDRepository(articleIDPath, articleIDDomainSuffix);
        spool = createSpooler();
        spool.setRepository(this);
        spool.setArticleIDRepository(articleIDRepo);
        if (logger.isDebugEnabled()) {
            logger.debug("repository:readOnly=" + readOnly);
            logger.debug("repository:rootPath=" + rootPath.getAbsolutePath());
            logger.debug("repository:tempPath=" + tempPath.getAbsolutePath());
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

        Set<String> groups = groupNameMap.keySet();
        Iterator<String> groupIterator = groups.iterator();
        while( groupIterator.hasNext() ) {
            String groupName = groupIterator.next();
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

        logger.debug("repository initialization done");
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
            if (logger.isDebugEnabled()) {
                logger.debug(groupName + " is not a newsgroup hosted on this server.");
            }
            return null;
        }
        File groupFile = new File(rootPath,groupName);
        NNTPGroup groupToReturn = null;
        synchronized(this) {
            groupToReturn = repositoryGroups.get(groupName);
            if ((groupToReturn == null) && groupFile.exists() && groupFile.isDirectory() ) {
                try {
                    groupToReturn = new NNTPGroupImpl(groupFile, logger);
                    repositoryGroups.put(groupName, groupToReturn);
                } catch (Exception e) {
                    logger.error("Couldn't create group object.", e);
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
            logger.error("Couldn't get article " + id + ": ", ex);
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
            if (logger.isDebugEnabled()) {
                logger.debug(((definedGroupsOnly ? groupNameMap.containsKey(name) : true) ? "Accepting ": "Rejecting") + name);
            }

            return definedGroupsOnly ? groupNameMap.containsKey(name) : true;
        }
    }

    /**
     * @see org.apache.james.nntpserver.repository.NNTPRepository#getMatchedGroups(String)
     */
    public Iterator<NNTPGroup> getMatchedGroups(String wildmat) {
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
    private Iterator<NNTPGroup> getGroups(File[] f) {
        List<NNTPGroup> list = new ArrayList<NNTPGroup>();
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
    public Iterator<NNTPGroup> getGroupsSince(Date dt) {
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
    public Iterator<NNTPArticle> getArticlesSince(final Date dt) {
        final Iterator<NNTPGroup> giter = getGroupsSince(dt);
        return new Iterator<NNTPArticle>() {

                private Iterator<NNTPArticle> iter = null;

                public boolean hasNext() {
                    if ( iter == null ) {
                        if ( giter.hasNext() ) {
                            NNTPGroup group = giter.next();
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

                public NNTPArticle next() {
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
     */
    private NNTPSpooler createSpooler() 
            throws ConfigurationException {
        String className = NNTPSpooler.class.getName();
        HierarchicalConfiguration spoolerConfiguration = configuration.configurationAt("spool");
        // Must be a subclass of org.apache.james.nntpserver.repository.NNTPSpooler
        className = spoolerConfiguration.getString("[@class]");

        try {
            Class<?> myClass = Thread.currentThread().getContextClassLoader().loadClass(className);
            Object obj = loader.load(myClass);
            if (obj instanceof LogEnabled) {
                ((LogEnabled) obj).setLog(logger);
            }
            
            if (obj instanceof Configurable) {
                ((Configurable) obj).configure(spoolerConfiguration.configurationAt("configuration"));
            }
            return (NNTPSpooler)obj;
        } catch(ClassCastException cce) {
            StringBuffer errorBuffer =
                new StringBuffer(128)
                    .append("Spooler initialization failed because the spooler class ")
                    .append(className)
                    .append(" was not a subclass of org.apache.james.nntpserver.repository.NNTPSpooler");
            String errorString = errorBuffer.toString();
            logger.error(errorString, cce);
            throw new ConfigurationException(errorString, cce);
        } catch(Exception ex) {
            logger.error("Spooler initialization failed",ex);
            throw new ConfigurationException("Spooler initialization failed",ex);
        }
    }
}
