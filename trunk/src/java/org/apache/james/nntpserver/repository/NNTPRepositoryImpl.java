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
import org.apache.james.nntpserver.DateSinceFileFilter;
import org.apache.james.nntpserver.NNTPException;
import org.apache.oro.io.GlobFilenameFilter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Date;
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
     * @see org.apache.avalon.framework.context.Contextualizable#contextualize(Context)
     */
    public void contextualize(Context context)
            throws ContextException {
        this.context = context;
    }

    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(Configuration)
     */
    public void configure( Configuration configuration ) throws ConfigurationException {
        //System.out.println(getClass().getName() + ": configure");
        //NNTPUtil.show(configuration,System.out);
        readOnly = configuration.getChild("readOnly").getValueAsBoolean(false);
        rootPath = NNTPUtil.getDirectory(context, configuration, "rootPath");
        tempPath = NNTPUtil.getDirectory(context, configuration, "tempPath");
        File articleIDPath = NNTPUtil.getDirectory(context, configuration, "articleIDPath");
        String articleIDDomainSuffix = configuration.getChild("articleIDDomainSuffix")
            .getValue("foo.bar.sho.boo");
        articleIDRepo = new ArticleIDRepository(articleIDPath,articleIDDomainSuffix);
        spool = (NNTPSpooler)NNTPUtil.createInstance
            (context,configuration.getChild("spool"),getLogger(),
             "org.apache.james.nntpserver.repository.NNTPSpooler");
        spool.setRepository(this);
        spool.setArticleIDRepository(articleIDRepo);
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("repository:readOnly=" + readOnly);
            getLogger().debug("repository:rootPath=" + rootPath.getAbsolutePath());
            getLogger().debug("repository:tempPath=" + tempPath.getAbsolutePath());
        }
        configuration = configuration.getChild("newsgroups");
        List addGroupsList = new ArrayList();
        if ( configuration != null ) {
            Configuration[] children = configuration.getChildren("newsgroup");
            if ( children != null )
                for ( int i = 0 ; i < children.length ; i++ )
                    addGroupsList.add(children[i].getValue());
        }
        addGroups = (String[])addGroupsList.toArray(new String[0]);
        getLogger().debug("repository configuration done");
    }

    /**
     * @see org.apache.avalon.framework.activity.Initializable#initialize()
     */
    public void initialize() throws Exception {
        //System.out.println(getClass().getName() + ": init");
        if ( rootPath.exists() == false ) {
            rootPath.mkdirs();
        }
        for ( int i = 0 ; i < addGroups.length ; i++ ) {
            File groupF = new File(rootPath,addGroups[i]);
            if ( groupF.exists() == false ) {
                groupF.mkdirs();
            }
        }
        if ( tempPath.exists() == false ) {
            tempPath.mkdirs();
        }
        File articleIDPath = articleIDRepo.getPath();
        if ( articleIDPath.exists() == false ) {
            articleIDPath.mkdirs();
        }
        if ( spool instanceof Initializable ) {
            ((Initializable)spool).initialize();
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
        File f = new File(rootPath,groupName);
        return ( f.exists() && f.isDirectory() ) ? new NNTPGroupImpl(f) : null;
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
            list.add(new NNTPGroupImpl(f[i]));
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
}
