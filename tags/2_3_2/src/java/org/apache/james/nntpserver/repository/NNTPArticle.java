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

import java.io.OutputStream;

/** 
 * Contract exposed by a NewsGroup Article
 */
public interface NNTPArticle {

    /**
     * Gets the group containing this article.
     *
     * @return the group
     */
    NNTPGroup getGroup();

    /**
     * Gets the article number for this article.
     *
     * @return the article number
     */
    int getArticleNumber();

    /**
     * Gets the unique message id for this article.
     *
     * @return the message id
     */
    String getUniqueID();

    /**
     * Writes the whole article to a writer.
     *
     * @param wrt the OutputStream to which the article is written.
     */
    void writeArticle(OutputStream wrt);

    /**
     * Writes the article headers to a writer.
     *
     * @param wrt the OutputStream to which the article is written.
     */
    void writeHead(OutputStream wrt);

    /**
     * Writes the article body to a writer.
     *
     * @param wrt the OutputStream to which the article is written.
     */
    void writeBody(OutputStream wrt);

    /**
     * Writes the article overview to a writer.
     *
     * @param wrt the OutputStream to which the article is written.
     */
    void writeOverview(OutputStream wrt);

    /**
     * Gets the header with the specified headerName.  Returns null
     * if the header doesn't exist.
     *
     * @param headerName the name of the header being retrieved.
     */
    String getHeader(String headerName);
}
