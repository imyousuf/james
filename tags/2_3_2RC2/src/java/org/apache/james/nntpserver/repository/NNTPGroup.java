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

import java.io.InputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;

/** 
 * Contract exposed by a NewsGroup
 *
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
