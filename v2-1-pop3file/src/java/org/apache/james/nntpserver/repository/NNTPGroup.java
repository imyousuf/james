/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.nntpserver.repository;

import java.io.InputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;

/** 
 * Contract exposed by a NewsGroup
 *
 * @author Harmeet Bedi <harmeet@kodemuse.com>
 */
public interface NNTPGroup {

    /**
     * Gets the name of the newsgroup
     *
     * @return the newsgroup name
     */
    String getName();

    /**
     * Gets the description of the newsgroup
     *
     * @return the newsgroup description
     */
    String getDescription();

    /**
     * Returns whether posting is allowed to this newsgroup
     *
     * @return whether posting is allowed to this newsgroup
     */
    boolean isPostAllowed();

    /**
     * Gets the number of articles in the group.
     *
     * @return the number of articles in the group.
     */
    int getNumberOfArticles();

    /**
     * Gets the first article number in the group.
     *
     * @return the first article number in the group.
     */
    int getFirstArticleNumber();

    /**
     * Gets the last article number in the group.
     *
     * @return the last article number in the group.
     */
    int getLastArticleNumber();

    /**
     * Gets the article with the specified article number.
     *
     * @param number the article number
     *
     * @return the article
     */
    NNTPArticle getArticle(int number);

    /**
     * Retrieves an iterator of articles in this newsgroup that were
     * posted on or after the specified date.
     *
     * @param dt the Date that acts as a lower bound for the list of
     *           articles
     *
     * @return the article iterator
     */
    Iterator getArticlesSince(Date dt);

    /**
     * Retrieves an iterator of all articles in this newsgroup
     *
     * @return the article iterator
     */
    Iterator getArticles();

    /**
     * Retrieves the group information in a format consistent with
     * a LIST or LIST ACTIVE return line
     *
     * @return the properly formatted string
     */
    String getListFormat();

    /**
     * Retrieves the group information in a format consistent with
     * a LIST NEWSGROUPS return line
     *
     * @return the properly formatted string
     */
    String getListNewsgroupsFormat();

    /**
     * Adds an article to the group based on the data in the
     * stream.
     *
     * @param newsStream the InputStream containing the article data
     *
     * @return the newly created article
     */
    NNTPArticle addArticle(InputStream newsStream) throws IOException;
}
