/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.nntpserver.repository;

import java.io.PrintWriter;

/** 
 * Contract exposed by a NewsGroup Article
 *
 * @author Harmeet Bedi <harmeet@kodemuse.com>
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
     * @param wrt the PrintWriter to which the article is written.
     */
    void writeArticle(PrintWriter wrt);

    /**
     * Writes the article headers to a writer.
     *
     * @param wrt the PrintWriter to which the article is written.
     */
    void writeHead(PrintWriter wrt);

    /**
     * Writes the article body to a writer.
     *
     * @param wrt the PrintWriter to which the article is written.
     */
    void writeBody(PrintWriter wrt);

    /**
     * Writes the article overview to a writer.
     *
     * @param wrt the PrintWriter to which the article is written.
     */
    void writeOverview(PrintWriter wrt);

    /**
     * Gets the header with the specified headerName.  Returns null
     * if the header doesn't exist.
     *
     * @param headerName the name of the header being retrieved.
     */
    String getHeader(String headerName);
}
