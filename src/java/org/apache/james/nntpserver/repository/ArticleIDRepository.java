/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2003 The Apache Software Foundation.  All rights
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

import org.apache.avalon.excalibur.io.IOUtil;
import org.apache.james.util.Base64;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;

/**
 * ArticleIDRepository: contains one file for each article.
 * the file name is Base64 encoded article ID
 * The first line of the file is '# <create date of file>
 * the rest of line have <newsgroup name>=<article number>
 * Allows fast lookup of a message by message id.
 *
 * This class allows a process to iterate and synchronize messages with other NNTP Servers.
 * This may be inefficient. It may be better to use an alternate, more
 * efficient process for synchronization and this class for sanity check.
 *
 */
public class ArticleIDRepository {

    /**
     * The root of the repository in the file system
     */
    private final File root;

    /**
     * The suffix appended to the articleIDs
     */
    private final String articleIDDomainSuffix;

    /**
     * A counter of the number of article IDs
     *
     * TODO: Potentially serious threading problem here
     */
    private int counter = 0;

    ArticleIDRepository(File root,String articleIDDomainSuffix) {
        this.root = root;
        this.articleIDDomainSuffix = articleIDDomainSuffix;
    }

    /**
     * Generate a new article ID for use in the repository.
     */
    String generateArticleID() {
        int idx = Math.abs(counter++);
        StringBuffer idBuffer =
            new StringBuffer(256)
                    .append("<")
                    .append(Thread.currentThread().hashCode())
                    .append(".")
                    .append(System.currentTimeMillis())
                    .append(".")
                    .append(idx)
                    .append("@")
                    .append(articleIDDomainSuffix)
                    .append(">");
        return idBuffer.toString();
    }

    /**
     * Add the article information to the repository.
     *
     * @param prop contains the newsgroup name and article number.
     */
    void addArticle(String articleID,Properties prop) throws IOException {
        if ( articleID == null ) {
            articleID = generateArticleID();
        }
        FileOutputStream fout = null;
        try {
            fout = new FileOutputStream(getFileFromID(articleID));
            prop.store(fout,new Date().toString());
        } finally {
            IOUtil.shutdownStream(fout);
        }
    }

    /**
     * Returns the file in the repository corresponding to the specified
     * article ID.
     *
     * @param articleID the article ID
     *
     * @return the repository file
     */
    File getFileFromID(String articleID) {
        String b64Id;
        try {
            b64Id = Base64.encodeAsString(articleID);
        } catch (Exception e) {
            throw new RuntimeException("This shouldn't happen: " + e);
        }
        return new File(root, b64Id);
    }

    /**
     * Returns whether the article ID is in the repository
     *
     * @param articleID the article ID
     *
     * @return whether the article ID is in the repository
     */
    boolean isExists(String articleID) {
        return ( articleID == null ) ? false : getFileFromID(articleID).exists();
    }

    /**
     * Get the article from the NNTP respository with the specified id.
     *
     * @param repo the NNTP repository where the article is stored
     * @param id the id of the article to retrieve
     *
     * @return the article
     *
     * @throws IOException if the ID information cannot be loaded
     */
    NNTPArticle getArticle(NNTPRepository repo,String id) throws IOException {
        File f = getFileFromID(id);
        if ( f.exists() == false ) {
            return null;
        }
        FileInputStream fin = null;
        Properties prop = new Properties();
        try {
            fin = new FileInputStream(f);
            prop.load(fin);
        } finally {
            IOUtil.shutdownStream(fin);
        }
        Enumeration enum = prop.keys();
        NNTPArticle article = null;
        while ( article == null && enum.hasMoreElements() ) {
            String groupName = (String)enum.nextElement();
            int number = Integer.parseInt(prop.getProperty(groupName));
            NNTPGroup group = repo.getGroup(groupName);
            if ( group != null ) {
                article = group.getArticle(number);
            }
        }
        return article;
    }
}
