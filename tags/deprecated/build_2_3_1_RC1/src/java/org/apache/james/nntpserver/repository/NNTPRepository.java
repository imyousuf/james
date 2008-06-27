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
import java.util.Date;
import java.util.Iterator;

/**
 * Abstraction of entire NNTP Repository.
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
     * TODO: Change this to be more OO and pass in a MimeMessage
     *
     * @param in the InputStream that serves as a source for the message data.
     */
    void createArticle(InputStream in);

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
