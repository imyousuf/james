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

import java.util.Date;
import java.util.Iterator;

/**
 * Abstraction of entire NNTP Repository.
 *
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
