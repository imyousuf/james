/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.nntpserver.repository;

import org.apache.avalon.excalibur.io.IOUtil;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.ContextException;
import org.apache.avalon.framework.context.Contextualizable;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.logger.LogEnabled;
import org.apache.james.util.Lock;

import javax.mail.internet.MimeMessage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

/**
 * Processes entries and sends to appropriate groups.
 * Eats up inappropriate entries.
 *
 * @author Harmeet Bedi <harmeet@kodemuse.com>
 */
class NNTPSpooler extends AbstractLogEnabled 
        implements Contextualizable, Configurable, Initializable {

    /**
     * The spooler context
     */
    private Context context;

    /**
     * The array of spooler runnables, each associated with a Worker thread
     */
    private SpoolerRunnable[] worker;

    /**
     * The directory containing entries to be spooled.
     */
    private File spoolPath;

    /**
     * @see org.apache.avalon.framework.context.Contextualizable#contextualize(Context)
     */
    public void contextualize(final Context context) 
            throws ContextException {
        this.context = context;
    }

    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(Configuration)
     */
    public void configure( Configuration configuration ) throws ConfigurationException {
        spoolPath = NNTPUtil.getDirectory(context, configuration, "spoolPath");
        int threadCount = configuration.getChild("threadCount").getValueAsInteger(1);
        int threadIdleTime = configuration.getChild("threadIdleTime").getValueAsInteger(1000);
        //String tgName=configuration.getChild("threadGroupName").getValue("NNTPSpooler");
        worker = new SpoolerRunnable[threadCount];
        for ( int i = 0 ; i < worker.length ; i++ ) {
            worker[i] = new SpoolerRunnable(threadIdleTime,spoolPath);
            if ( worker[i] instanceof LogEnabled ) {
                ((LogEnabled)worker[i]).enableLogging(getLogger());
            }
        }
    }

    /**
     * Sets the repository used by this spooler.
     *
     * @param repo the repository to be used
     */
    void setRepository(NNTPRepository repo) {
        for ( int i = 0 ; i < worker.length ; i++ ) {
            worker[i].setRepository(repo);
        }
    }

    /**
     * Sets the article id repository used by this spooler.
     *
     * @param articleIDRepo the article id repository to be used
     */
    void setArticleIDRepository(ArticleIDRepository articleIDRepo) {
        for ( int i = 0 ; i < worker.length ; i++ ) {
            worker[i].setArticleIDRepository(articleIDRepo);
        }
    }

    /**
     * Returns (and creates, if the directory doesn't already exist) the
     * spool directory
     *
     * @return the spool directory
     */
    File getSpoolPath() {
        if ( spoolPath.exists() == false ) {
            spoolPath.mkdirs();
        }
        return spoolPath;
    }

    /**
     * @see org.apache.avalon.framework.activity.Initializable#initialize()
     */
    public void initialize() throws Exception {
        //System.out.println(getClass().getName()+": init");
        // TODO: Replace this with a standard Avalon thread pool
        for ( int i = 0 ; i < worker.length ; i++ ) {
            new Thread(worker[i],"NNTPSpool-"+i).start();
        }
    }

    /**
     * A static inner class that provides the body for the spool
     * threads.
     */
    static class SpoolerRunnable extends AbstractLogEnabled implements Runnable {

        private static final Lock lock = new Lock();

        /**
         * The directory containing entries to be spooled.
         */
        private final File spoolPath;

        /**
         * The time the spooler thread sleeps between processing
         */
        private final int threadIdleTime;

        /**
         * The article ID repository used by this spooler thread
         */
        private ArticleIDRepository articleIDRepo;

        /**
         * The NNTP repository used by this spooler thread
         */
        private NNTPRepository repo;

        SpoolerRunnable(int threadIdleTime,File spoolPath) {
            this.threadIdleTime = threadIdleTime;
            this.spoolPath = spoolPath;
        }

        /**
         * Sets the article id repository used by this spooler thread.
         *
         * @param articleIDRepo the article id repository to be used
         */
        void setArticleIDRepository(ArticleIDRepository articleIDRepo) {
            this.articleIDRepo = articleIDRepo;
        }

        /**
         * Sets the repository used by this spooler thread.
         *
         * @param repo the repository to be used
         */
        void setRepository(NNTPRepository repo) {
            this.repo = repo;
        }

        /**
         * The threads race to grab a lock. if a thread wins it processes the article,
         * if it loses it tries to lock and process the next article.
         */
        public void run() {
            getLogger().debug("in spool thread");
            while ( Thread.currentThread().isInterrupted() == false ) {
                String[] list = spoolPath.list();
                for ( int i = 0 ; i < list.length ; i++ ) {
                    getLogger().debug("Files to process: "+list.length);
                    if ( lock.lock(list[i]) ) {
                        File f = new File(spoolPath,list[i]).getAbsoluteFile();
                        getLogger().debug("Processing file: "+f.getAbsolutePath());
                        try {
                            process(f);
                        } catch(Exception ex) {
                            getLogger().debug("Exception occured while processing file: "+
                                              f.getAbsolutePath(),ex);
                        } finally {
                            lock.unlock(list[i]);
                        }
                    }
                }
                // this is good for other non idle threads
                try {
                    Thread.currentThread().sleep(threadIdleTime);
                } catch(InterruptedException ex) {  }
            }
        }

        /**
         * Process a file stored in the spool.
         *
         * @param f the spool file being processed
         */
        private void process(File f) throws Exception {
            StringBuffer logBuffer =
                new StringBuffer(160)
                        .append("process: ")
                        .append(f.getAbsolutePath())
                        .append(",")
                        .append(f.getCanonicalPath());
            getLogger().debug(logBuffer.toString());
            final MimeMessage msg;
            String articleID;
            // TODO: Why is this a block?
            {   // get the message for copying to destination groups.
                FileInputStream fin = new FileInputStream(f);
                msg = new MimeMessage(null,fin);
                fin.close();

                // ensure no duplicates exist.
                String[] idheader = msg.getHeader("Message-Id");
                articleID = (idheader!=null && idheader.length>0?idheader[0]:null);
                if ( articleIDRepo.isExists(articleID) ) {
                    getLogger().debug("Message already exists: "+articleID);
                    f.delete();
                    return;
                }
                if ( articleID == null ) {
                    articleID = articleIDRepo.generateArticleID();
                    msg.setHeader("Message-Id", articleID);
                    FileOutputStream fout = new FileOutputStream(f);
                    msg.writeTo(fout);
                    fout.close();
                }
            }

            String[] headers = msg.getHeader("Newsgroups");
            Properties prop = new Properties();
            for ( int i = 0 ; i < headers.length ; i++ ) {
                getLogger().debug("Copying message to group: "+headers[i]);
                NNTPGroup group = repo.getGroup(headers[i]);
                if ( group == null ) {
                    getLogger().debug("Group not found: "+headers[i]);
                    continue;
                }
                int artNum = group.getLastArticleNumber();

                // TODO: Encapsulate this in the NNTP group.
                File root = (File)group.getPath();
                File articleFile = null;
                // this ensures that different threads do not create articles with
                // same number
                while( true ) {
                    articleFile = new File(root,(artNum+1)+"");
                    if (articleFile.createNewFile()) {
                        break;
                    }
                }
                getLogger().debug("Copying message to: "+articleFile.getAbsolutePath());
                prop.setProperty(group.getName(),articleFile.getName());
                FileInputStream fin = new FileInputStream(f);
                FileOutputStream fout = new FileOutputStream(articleFile);
                IOUtil.copy(fin,fout);
                fin.close();
                fout.close();
            }
            articleIDRepo.addArticle(articleID,prop);
            boolean delSuccess = f.delete();
            if ( delSuccess == false ) {
                getLogger().error("Could not delete file: "+f.getAbsolutePath());
            }
        }
    } // class SpoolerRunnable
}
