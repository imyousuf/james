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
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.ContextException;
import org.apache.avalon.framework.context.Contextualizable;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.logger.LogEnabled;
import org.apache.james.context.AvalonContextUtilities;
import org.apache.james.util.Lock;
import org.apache.james.util.io.IOUtil;

import javax.mail.internet.MimeMessage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 * Processes entries and sends to appropriate groups.
 * Eats up inappropriate entries.
 *
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
     * The String form of the spool directory.
     */
    private String spoolPathString;

    /**
     * The time the spooler threads sleep between processing
     */
    private int threadIdleTime = 0;

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
        int threadCount = configuration.getChild("threadCount").getValueAsInteger(1);
        threadIdleTime = configuration.getChild("threadIdleTime").getValueAsInteger(60 * 1000);
        spoolPathString = configuration.getChild("spoolPath").getValue();
        worker = new SpoolerRunnable[threadCount];
    }

    /**
     * @see org.apache.avalon.framework.activity.Initializable#initialize()
     */
    public void initialize() throws Exception {
        //System.out.println(getClass().getName()+": init");

        try {
            spoolPath = AvalonContextUtilities.getFile(context, spoolPathString);
            if ( spoolPath.exists() == false ) {
                spoolPath.mkdirs();
            } else if (!(spoolPath.isDirectory())) {
                StringBuffer errorBuffer =
                    new StringBuffer(128)
                        .append("Spool directory is improperly configured.  The specified path ")
                        .append(spoolPathString)
                        .append(" is not a directory.");
                throw new ConfigurationException(errorBuffer.toString());
            }
        } catch (Exception e) {
            getLogger().fatalError(e.getMessage(), e);
            throw e;
        }

        for ( int i = 0 ; i < worker.length ; i++ ) {
            worker[i] = new SpoolerRunnable(threadIdleTime,spoolPath);
            if ( worker[i] instanceof LogEnabled ) {
                ((LogEnabled)worker[i]).enableLogging(getLogger());
            }
        }

        // TODO: Replace this with a standard Avalon thread pool
        for ( int i = 0 ; i < worker.length ; i++ ) {
            new Thread(worker[i],"NNTPSpool-"+i).start();
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
        return spoolPath;
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
            getLogger().debug(Thread.currentThread().getName() + " is the NNTP spooler thread.");
            try {
                while ( Thread.currentThread().interrupted() == false ) {
                    String[] list = spoolPath.list();
                    if (list.length > 0) getLogger().debug("Files to process: "+list.length);
                    for ( int i = 0 ; i < list.length ; i++ ) {
                        if ( lock.lock(list[i]) ) {
                            File f = new File(spoolPath,list[i]).getAbsoluteFile();
                            getLogger().debug("Processing file: "+f.getAbsolutePath());
                            try {
                                process(f);
                            } catch(Throwable ex) {
                                getLogger().debug("Exception occured while processing file: "+
                                                  f.getAbsolutePath(),ex);
                            } finally {
                                lock.unlock(list[i]);
                            }
                        }
                        list[i] = null; // release the string entry;
                    }
                    list = null; // release the array;
                    // this is good for other non idle threads
                    try {
                        Thread.currentThread().sleep(threadIdleTime);
                    } catch(InterruptedException ex) {
                        // Ignore and continue
                    }
                }
            } finally {
                Thread.currentThread().interrupted();
            }
        }

        /**
         * Process a file stored in the spool.
         *
         * @param f the spool file being processed
         */
        private void process(File spoolFile) throws Exception {
            StringBuffer logBuffer =
                new StringBuffer(160)
                        .append("process: ")
                        .append(spoolFile.getAbsolutePath())
                        .append(",")
                        .append(spoolFile.getCanonicalPath());
            getLogger().debug(logBuffer.toString());
            final MimeMessage msg;
            String articleID;
            // TODO: Why is this a block?
            {   // Get the message for copying to destination groups.
                FileInputStream fin = new FileInputStream(spoolFile);
                try {
                    msg = new MimeMessage(null,fin);
                } finally {
                    IOUtil.shutdownStream(fin);
                }

                String lineCount = null;
                String[] lineCountHeader = msg.getHeader("Lines");
                if (lineCountHeader == null || lineCountHeader.length == 0) {
                    BufferedReader rdr = new BufferedReader(new InputStreamReader(msg.getDataHandler().getInputStream()));
                    int lines = 0;
                    while (rdr.readLine() != null) {
                        lines++;
                    }

                    lineCount = Integer.toString(lines);
                    rdr.close();

                    msg.setHeader("Lines", lineCount);
                }

                // ensure no duplicates exist.
                String[] idheader = msg.getHeader("Message-Id");
                articleID = ((idheader != null && (idheader.length > 0))? idheader[0] : null);
                if ((articleID != null) && ( articleIDRepo.isExists(articleID))) {
                    getLogger().debug("Message already exists: "+articleID);
                    if (spoolFile.delete() == false)
                        getLogger().error("Could not delete duplicate message from spool: " + spoolFile.getAbsolutePath());
                    return;
                }
                if ( articleID == null || lineCount != null) {
                    if (articleID == null) {
                        articleID = articleIDRepo.generateArticleID();
                        msg.setHeader("Message-Id", articleID);
                    }
                    FileOutputStream fout = new FileOutputStream(spoolFile);
                    try {
                        msg.writeTo(fout);
                    } finally {
                        IOUtil.shutdownStream(fout);
                    }
                }
            }

            String[] headers = msg.getHeader("Newsgroups");
            Properties prop = new Properties();
            if (headers != null) {
                for ( int i = 0 ; i < headers.length ; i++ ) {
                    StringTokenizer tokenizer = new StringTokenizer(headers[i],",");
                    while ( tokenizer.hasMoreTokens() ) {
                        String groupName = tokenizer.nextToken().trim();
                        getLogger().debug("Copying message to group: "+groupName);
                        NNTPGroup group = repo.getGroup(groupName);
                        if ( group == null ) {
                            getLogger().error("Couldn't add article with article ID " + articleID + " to group " + groupName + " - group not found.");
                            continue;
                        }

                        FileInputStream newsStream = new FileInputStream(spoolFile);
                        try {
                            NNTPArticle article = group.addArticle(newsStream);
                            prop.setProperty(group.getName(),article.getArticleNumber() + "");
                        } finally {
                            IOUtil.shutdownStream(newsStream);
                        }
                    }
                }
            }
            articleIDRepo.addArticle(articleID,prop);
            boolean delSuccess = spoolFile.delete();
            if ( delSuccess == false ) {
                getLogger().error("Could not delete file: " + spoolFile.getAbsolutePath());
            }
        }
    } // class SpoolerRunnable
}
