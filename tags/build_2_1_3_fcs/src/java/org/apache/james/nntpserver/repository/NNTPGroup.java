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
