/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache", "Jakarta", "JAMES" and "Apache Software Foundation"
 *    must not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 * Portions of this software are based upon public domain software
 * originally written at the National Center for Supercomputing Applications,
 * University of Illinois, Urbana-Champaign.
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
            int artNum;
            if (numOfArticles < 0)
                throw new IllegalStateException("NNTP Group is corrupt (articles < 0).");
            else if (numOfArticles == 0) {
                firstArticle = 1;
                artNum = 1;
            } else {
                artNum = getLastArticleNumber() + 1;
            }
            
            articleFile = new File(root, artNum + "");
            articleFile.createNewFile();
            lastArticle = artNum;
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
            IOUtil.shutdownStream(fout);
        }
        return new NNTPArticleImpl(this, articleFile);
    }
}
