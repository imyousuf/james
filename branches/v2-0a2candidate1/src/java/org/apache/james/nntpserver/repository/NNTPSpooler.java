/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.nntpserver.repository;

import java.io.*;
import java.util.*;
import javax.mail.internet.MimeMessage;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.ContextException;
import org.apache.avalon.framework.context.Contextualizable;
import org.apache.avalon.framework.logger.AbstractLoggable;
import org.apache.avalon.framework.logger.Loggable;
import org.apache.avalon.excalibur.io.IOUtil;
import org.apache.james.util.Lock;

/**
 * Processes entries and sends to appropriate groups.
 * Eats up inappropriate entries.
 *
 * @author Harmeet Bedi <harmeet@kodemuse.com>
 */
class NNTPSpooler extends AbstractLoggable 
        implements Contextualizable, Configurable, Initializable {

    private Context context;
    private Worker[] worker;
    private File spoolPath;

    public void contextualize(final Context context) 
            throws ContextException {
        this.context = context;
    }

    public void configure( Configuration configuration ) throws ConfigurationException {
        //System.out.println(getClass().getName()+": configure");
        //NNTPUtil.show(configuration,System.out);
        spoolPath = NNTPUtil.getDirectory(context, configuration, "spoolPath");
        int threadCount = configuration.getChild("threadCount").getValueAsInteger(1);
        int threadIdleTime = configuration.getChild("threadIdleTime").getValueAsInteger(1000);
        //String tgName=configuration.getChild("threadGroupName").getValue("NNTPSpooler");
        worker = new Worker[threadCount];
        for ( int i = 0 ; i < worker.length ; i++ ) {
            worker[i] = new Worker(threadIdleTime,spoolPath);
            if ( worker[i] instanceof Loggable )
                ((Loggable)worker[i]).setLogger(getLogger());
        }
    }
    void setRepository(NNTPRepository repo) {
        for ( int i = 0 ; i < worker.length ; i++ )
            worker[i].setRepository(repo);
    }
    void setArticleIDRepository(ArticleIDRepository articleIDRepo) {
        for ( int i = 0 ; i < worker.length ; i++ )
            worker[i].setArticleIDRepository(articleIDRepo);
    }
    File getSpoolPath() {
        if ( spoolPath.exists() == false )
            spoolPath.mkdirs();
        return spoolPath;
    }
    public void initialize() throws Exception {
        //System.out.println(getClass().getName()+": init");
        for ( int i = 0 ; i < worker.length ; i++ )
            new Thread(worker[i],"NNTPSpool-"+i).start();
    }
    static class Worker extends AbstractLoggable implements Runnable {
        private static final Lock lock = new Lock();
        private final File spoolPath;
        private final int threadIdleTime;
        private ArticleIDRepository articleIDRepo;
        private NNTPRepository repo;
        Worker(int threadIdleTime,File spoolPath) {
            this.threadIdleTime = threadIdleTime;
            this.spoolPath = spoolPath;
        }
        void setArticleIDRepository(ArticleIDRepository articleIDRepo) {
            this.articleIDRepo = articleIDRepo;
        }
        void setRepository(NNTPRepository repo) {
            this.repo = repo;
        }
        // the threads race to grab a lock. if a thread wins it processes the article,
        // if it loses it tries to lock and process the next article
        public void run() {
            getLogger().debug("in spool thread");
            while ( Thread.currentThread().isInterrupted() == false ) {
                String[] list = spoolPath.list();
                for ( int i = 0 ; i < list.length ; i++ ) {
                    getLogger().debug("Files to process: "+list.length);
                    if ( lock.lock(list[i]) ) {
                        File f = new File(spoolPath,list[i]).getAbsoluteFile();
                        getLogger().debug("processing file: "+f.getAbsolutePath());
                        try {
                            process(f);
                        } catch(Exception ex) {
                            getLogger().debug("exception occured in processing file: "+
                                              f.getAbsolutePath(),ex);
                        } finally {
                            lock.unlock(list[i]);
                        }
                    }
                }
                // this is good for other non idle threads
                try {  Thread.currentThread().sleep(threadIdleTime);
                } catch(InterruptedException ex) {  }
            }
        }
        private void process(File f) throws Exception {
            getLogger().debug("process: "+f.getAbsolutePath()+","+f.getCanonicalPath());
            final MimeMessage msg;
            String articleID;
            {   // get the message for copying to destination groups.
                FileInputStream fin = new FileInputStream(f);
                msg = new MimeMessage(null,fin);
                fin.close();

                // ensure no duplicates exist.
                String[] idheader = msg.getHeader("Message-Id");
                articleID = (idheader!=null && idheader.length>0?idheader[0]:null);
                if ( articleIDRepo.isExists(articleID) ) {
                    getLogger().debug("message already exists: "+articleID);
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
                getLogger().debug("copying message to group: "+headers[i]);
                NNTPGroup group = repo.getGroup(headers[i]);
                if ( group == null ) {
                    getLogger().debug("group not found: "+headers[i]);
                    continue;
                }
                int artNum = group.getLastArticleNumber();
                File root = (File)group.getPath();
                File articleFile = null;
                // this ensures that different threads do not create articles with
                // same number
                while( true ) {
                    articleFile = new File(root,(artNum+1)+"");
                    if (articleFile.createNewFile())
                        break;
                }
                getLogger().debug("copying message to: "+articleFile.getAbsolutePath());
                prop.setProperty(group.getName(),articleFile.getName());
                FileInputStream fin = new FileInputStream(f);
                FileOutputStream fout = new FileOutputStream(articleFile);
                IOUtil.copy(fin,fout);
                fin.close();
                fout.close();
            }
            articleIDRepo.addArticle(articleID,prop);
            boolean delSuccess = f.delete();
            if ( delSuccess == false )
                getLogger().error("could not delete file: "+f.getAbsolutePath());
        }
    } // class Worker
}
