/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.nntpserver.repository;

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
 * @author Harmeet Bedi <harmeet@kodemuse.com>
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
            if (fout != null) {
                fout.close();
            }
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
            if (fin != null) {
                fin.close();
            }
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
