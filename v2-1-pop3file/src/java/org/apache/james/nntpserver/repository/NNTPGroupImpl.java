/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.nntpserver.repository;

import org.apache.avalon.excalibur.io.AndFileFilter;
import org.apache.avalon.excalibur.io.ExtensionFileFilter;
import org.apache.avalon.excalibur.io.InvertedFileFilter;
import org.apache.avalon.excalibur.io.IOUtil;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.james.nntpserver.DateSinceFileFilter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * Group is represented by a directory.
 * Articles are stored in files with the name of file == article number
 *
 * @author Harmeet Bedi <harmeet@kodemuse.com>
 */
class NNTPGroupImpl extends AbstractLogEnabled implements NNTPGroup {

    /**
     * The directory to which this group maps.
     */
    private final File root;

    /**
     * The last article number in the group
     */
    private int lastArticle;

    /**
     * The last article number in the group
     */
    private int firstArticle;

    /**
     * The number of articles in the group.
     */
    private int numOfArticles;

    /**
     * Whether the first, last, and total number of articles in the
     * group have been read from disk.
     * An instance may collect range info once. This involves disk I/O
     */
    private boolean articleRangeInfoCollected = false;

    /**
     * The sole constructor for this particular NNTPGroupImpl.
     *
     * @param root the directory containing the articles
     */
    NNTPGroupImpl(File root) {
        this.root = root;
    }

    /**
     * @see org.apache.james.nntpserver.NNTPGroup#getName()
     */
    public String getName() {
        return root.getName();
    }

    /**
     * @see org.apache.james.nntpserver.NNTPGroup#getDescription()
     */
    public String getDescription() {
        return getName();
    }

    /**
     * @see org.apache.james.nntpserver.NNTPGroup#isPostAllowed()
     */
    public boolean isPostAllowed() {
        return true;
    }

    /**
     * Generates the first, last, and number of articles from the
     * information in the group directory.
     */
    private void collectArticleRangeInfo() {
        if ( articleRangeInfoCollected ) {
            return;
        }
        String[] list = root.list();
        int first = -1;
        int last = -1;
        for ( int i = 0 ; i < list.length ; i++ ) {
            int num = Integer.parseInt(list[i]);
            if ( first == -1 || num < first ) {
                first = num;
            }
            if ( num > last ) {
                last = num;
            }
        }
        numOfArticles = list.length;
        firstArticle = Math.max(first,0);
        lastArticle = Math.max(last,0);
        articleRangeInfoCollected = true;
    }

    /**
     * @see org.apache.james.nntpserver.NNTPGroup#getNumberOfArticles()
     */
    public int getNumberOfArticles() {
        collectArticleRangeInfo();
        return numOfArticles;
    }

    /**
     * @see org.apache.james.nntpserver.NNTPGroup#getFirstArticleNumber()
     */
    public int getFirstArticleNumber() {
        collectArticleRangeInfo();
        return firstArticle;
    }

    /**
     * @see org.apache.james.nntpserver.NNTPGroup#getLastArticleNumber()
     */
    public int getLastArticleNumber() {
        collectArticleRangeInfo();
        return lastArticle;
    }

    /**
     * @see org.apache.james.nntpserver.NNTPGroup#getArticle(int)
     */
    public NNTPArticle getArticle(int number) {
        File f = new File(root,number + "");
        return f.exists() ? new NNTPArticleImpl(this, f) : null;
    }

    /**
     * @see org.apache.james.nntpserver.NNTPGroup#getArticlesSince(Date)
     */
    public Iterator getArticlesSince(Date dt) {
        File[] f = root.listFiles(new AndFileFilter
            (new DateSinceFileFilter(dt.getTime()),
             new InvertedFileFilter(new ExtensionFileFilter(".id"))));
        List list = new ArrayList();
        for ( int i = 0 ; i < f.length ; i++ ) {
            list.add(new NNTPArticleImpl(this, f[i]));
        }
        return list.iterator();
    }

    /**
     * @see org.apache.james.nntpserver.NNTPGroup#getArticles()
     */
    public Iterator getArticles() {
        File[] f = root.listFiles();
        List list = new ArrayList();
        for ( int i = 0 ; i < f.length ; i++ )
            list.add(new NNTPArticleImpl(this, f[i]));
        return list.iterator();
    }

    /**
     * @see org.apache.james.nntpserver.repository.NNTPGroup#getListFormat()
     */
    public String getListFormat() {
        StringBuffer showBuffer =
            new StringBuffer(128)
                .append(getName())
                .append(" ")
                .append(getLastArticleNumber())
                .append(" ")
                .append(getFirstArticleNumber())
                .append(" ")
                .append((isPostAllowed() ? "y":"n"));
        return showBuffer.toString();
    }

    /**
     * @see org.apache.james.nntpserver.repository.NNTPGroup#getListNewsgroupsFormat()
     */
    public String getListNewsgroupsFormat() {
        StringBuffer showBuffer =
            new StringBuffer(128)
                .append(getName())
                .append(" ")
                .append(getDescription());
         return showBuffer.toString();
    }

    /**
     * @see org.apache.james.nntpserver.repository.NNTPGroup#addArticle(InputStream)
     */
    public NNTPArticle addArticle(InputStream newsStream)
            throws IOException {
        File articleFile = null;
        synchronized (this) {
            int artNum = getLastArticleNumber();
            articleFile = new File(root,(artNum + 1)+"");
            articleFile.createNewFile();
            lastArticle++;
            numOfArticles++;
        }
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("Copying message to: "+articleFile.getAbsolutePath());
        }
        FileOutputStream fout = null;
        try {
            fout = new FileOutputStream(articleFile);
            IOUtil.copy(newsStream,fout);
            fout.flush();
        } finally {
            try {
                if (fout != null) {
                    fout.close();
                }
            } catch (IOException ioe) {
                // Ignore this exception so we don't
                // trash any "real" exceptions
            }
        }
        return new NNTPArticleImpl(this, articleFile);
    }

//     public NNTPArticle getArticleFromID(String id) {
//         if ( id == null )
//             return null;
//         int idx = id.indexOf('@');
//         if ( idx != -1 )
//             id = id.substring(0,idx);
//         File f = new File(root,id + ".id");
//         if ( f.exists() == false )
//             return null;
//         try {
//             FileInputStream fin = new FileInputStream(f);
//             int count = fin.available();
//             byte[] ba = new byte[count];
//             fin.read(ba);
//             fin.close();
//             String str = new String(ba);
//             int num = Integer.parseInt(str);
//             return getArticle(num);
//         } catch(IOException ioe) {
//             throw new NNTPException("could not fectch article: "+id,ioe);
//         }
//     }

}
