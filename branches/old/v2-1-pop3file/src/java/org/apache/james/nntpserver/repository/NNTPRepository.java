/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.nntpserver.repository;

import java.util.Date;
import java.util.Iterator;

/**
 * Abstraction of entire NNTP Repository.
 *
 * @author Harmeet Bedi <harmeet@kodemuse.com>
 */
public interface NNTPRepository {

    /**
     * Gets the group with the specified name from within the repository.
     *
     * @param groupName the name of the group to retrieve
     *
     * @return the group
     */
    NNTPGroup getGroup(String groupName);

    /**
     * Gets the article with the specified id from within the repository.
     *
     * @param id the id of the article to retrieve
     *
     * @return the article
     */
    NNTPArticle getArticleFromID(String id);

    /**
     * Creates an article in the repository from the data in the reader.
     *
     * @param reader the reader that serves as a source for the article data
     */
    void createArticle(NNTPLineReader reader);

    /**
     * Gets all groups that match the wildmat string
     *
     * @param wildmat the wildmat parameter
     *
     * @return an iterator containing the groups retrieved
     */
    Iterator getMatchedGroups(String wildmat);

    /**
     * Gets all groups added since the specified date
     *
     * @param dt the Date that serves as a lower bound
     *
     * @return an iterator containing the groups retrieved
     */
    Iterator getGroupsSince(Date dt);

    /**
     * Gets all articles posted since the specified date
     *
     * @param dt the Date that serves as a lower bound
     *
     * @return an iterator containing the articles retrieved
     */
    Iterator getArticlesSince(Date dt);

    /**
     * Returns whether this repository is read only.
     *
     * @return whether this repository is read only
     */
    boolean isReadOnly();

    /**
     * Returns the ordered array of header names (including the trailing colon on each)
     * returned in overview format for articles stored in this repository.
     */
    String[] getOverviewFormat();

}
